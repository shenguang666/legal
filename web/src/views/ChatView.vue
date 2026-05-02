<template>
  <section class="chat-layout">
    <div class="left card panel">
      <div class="head-row">
        <div>
          <p class="tag">对话工作台</p>
          <h2 class="section-title">法律问答会话</h2>
          <p class="subtle">以企业制度、法律知识库与会话上下文为基础，生成可追溯答案。</p>
        </div>
        <button class="ghost-btn" type="button" @click="newSession">新建会话</button>
      </div>

      <div v-if="!messages.length" class="empty-state chat-empty">
        <div>
          <strong>还没有消息</strong>
          <p>选择下方快捷问题，或直接输入你的法律咨询。</p>
        </div>
      </div>

      <div v-else class="messages" aria-live="polite">
        <article
          v-for="item in messages"
          :key="item.messageId"
          class="bubble"
          :class="item.role === 'assistant' ? 'assistant' : 'user'"
        >
          <p class="meta">{{ item.role === 'assistant' ? '助手' : '用户' }} · {{ formatTime(item.createdAt) }}</p>
          <details v-if="item.role === 'assistant' && item.thinking" class="thinking">
            <summary>思考过程</summary>
            <pre class="thinking-text">{{ item.thinking }}</pre>
          </details>
          <p class="text">{{ item.content }}</p>
          <div v-if="item.role === 'assistant'" class="feedback-line">
            <button class="ghost-btn" type="button" @click="submitFeedback(item.messageId, true)">有帮助</button>
            <button class="warn-btn" type="button" @click="submitFeedback(item.messageId, false)">没帮助</button>
          </div>
        </article>
      </div>

      <div class="prompt-rail" aria-label="快捷问题">
        <div class="prompt-rail-head">
          <span>热词推荐</span>
          <button class="ghost-btn" type="button" :disabled="hotwordLoading" @click="loadQuickPrompts">
            {{ hotwordLoading ? '刷新中…' : '换一组' }}
          </button>
        </div>
        <div v-if="quickPrompts.length" class="prompt-list">
          <button v-for="prompt in quickPrompts" :key="prompt.hotwordId" type="button" class="prompt-chip" @click="usePrompt(prompt)">
            {{ prompt.content }}
          </button>
        </div>
        <p v-else class="prompt-empty">暂无可用热词，可直接输入你的法律咨询。</p>
      </div>

      <div class="composer">
        <label for="chat-question">咨询内容</label>
        <textarea
          id="chat-question"
          v-model="question"
          name="question"
          rows="4"
          autocomplete="off"
          placeholder="输入你的法律问题，例如：劳动合同到期未续签是否有补偿？…"
          @input="clearSelectedHotword"
        />
        <div class="composer-actions">
          <span>{{ question.trim().length }} 字</span>
          <button v-if="loading" class="ghost-btn" type="button" @click="stopStream">停止生成</button>
          <button class="primary-btn" type="button" :disabled="loading || !question.trim()" @click="askStream">{{ loading ? '生成中…' : '发送问题' }}</button>
        </div>
      </div>
      <p class="note">当前 sessionId：{{ sessionId || '尚未创建' }}</p>
    </div>

    <aside class="right card panel">
      <p class="tag">工作流</p>
      <h3>系统提示</h3>
      <div class="insight-list">
        <div>
          <strong>混合检索</strong>
          <span>向量 + BM25 联合召回</span>
        </div>
        <div>
          <strong>引用追溯</strong>
          <span>本轮命中片段实时展示</span>
        </div>
        <div>
          <strong>幂等写入</strong>
          <span>写接口携带 requestId</span>
        </div>
      </div>
      <div v-if="latestCitations.length" class="citation-panel">
        <h4>本轮引用</h4>
        <article v-for="(item, idx) in latestCitations" :key="`${item.documentId}-${idx}`" class="citation-item">
          <p class="citation-source">{{ item.source }}</p>
          <p class="citation-fragment">{{ item.fragment }}</p>
        </article>
      </div>
      <p v-if="notice" class="notice" aria-live="polite">{{ notice }}</p>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { apiGet, apiPost, apiPostSse, randomRequestId } from '../api/client';

interface ChatMessage {
  messageId: number;
  role: string;
  content: string;
  createdAt: string;
  thinking?: string;
}

interface CreateSessionResponse {
  sessionId: string;
  title: string;
  createdAt: string;
}

interface AskResponse {
  answer: string;
  citations: Citation[];
  confidence: number;
  warning: string;
}

interface Citation {
  documentId: number;
  source: string;
  fragment: string;
}

interface HotwordPrompt {
  hotwordId: number;
  hotwordKey: string;
  content: string;
  category?: string;
}

const route = useRoute();
const sessionId = ref<string>('');
const messages = ref<ChatMessage[]>([]);
const question = ref('');
const notice = ref('');
const loading = ref(false);
const hotwordLoading = ref(false);
const latestCitations = ref<Citation[]>([]);
const quickPrompts = ref<HotwordPrompt[]>([]);
const selectedHotwordKey = ref('');
let cancelStream: null | (() => void) = null;

onMounted(async () => {
  await loadQuickPrompts();
  const querySessionId = route.query.sessionId as string | undefined;
  const cacheSessionId = localStorage.getItem('legal.activeSessionId');
  sessionId.value = querySessionId || cacheSessionId || '';
  if (!sessionId.value) {
    await newSession();
    return;
  }
  await loadMessages();
});

async function newSession() {
  const data = await apiPost<CreateSessionResponse>('/api/chat/session', {
    requestId: randomRequestId('chat-session'),
    title: `法律咨询-${new Date().toLocaleDateString()}`,
  });
  sessionId.value = data.sessionId;
  localStorage.setItem('legal.activeSessionId', data.sessionId);
  messages.value = [];
  notice.value = `新会话已创建：${data.sessionId}`;
}

async function loadMessages() {
  if (!sessionId.value) {
    return;
  }
  messages.value = await apiGet<ChatMessage[]>(`/api/chat/session/${sessionId.value}/messages`);
}

async function ask() {
  if (!question.value.trim() || !sessionId.value) {
    return;
  }
  loading.value = true;
  try {
    const payload = {
      sessionId: sessionId.value,
      question: question.value.trim(),
      requestId: randomRequestId('chat-ask'),
      hotwordKey: selectedHotwordKey.value || undefined,
    };
    const result = await apiPost<AskResponse>('/api/chat/ask', payload);
    await loadMessages();
    question.value = '';
    latestCitations.value = result.citations || [];
    notice.value = `置信度：${result.confidence.toFixed(2)} ｜ ${result.warning}`;
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '发送失败';
  } finally {
    loading.value = false;
  }
}

// 保留旧 ask()，新增流式按钮已指向 askStream()

async function askStream() {
  if (!question.value.trim() || !sessionId.value) {
    return;
  }
  if (cancelStream) {
    cancelStream();
    cancelStream = null;
  }
  loading.value = true;
  latestCitations.value = [];
  notice.value = '';

  const payload = {
    sessionId: sessionId.value,
    question: question.value.trim(),
    requestId: randomRequestId('chat-ask-stream'),
    hotwordKey: selectedHotwordKey.value || undefined,
  };

  const userMsg: ChatMessage = {
    messageId: Date.now() - 1,
    role: 'user',
    content: payload.question,
    createdAt: new Date().toISOString(),
  };
  const assistantMsg: ChatMessage = {
    messageId: Date.now(),
    role: 'assistant',
    content: '',
    thinking: '',
    createdAt: new Date().toISOString(),
  };
  messages.value = [...messages.value, userMsg, assistantMsg];

  const patchAssistant = () => {
    messages.value = messages.value.map((item) => (item.messageId === assistantMsg.messageId ? { ...assistantMsg } : item));
  };

  cancelStream = apiPostSse('/api/chat/ask/stream', payload, (evt) => {
    const name = evt.name;
    const data = evt.data?.data ?? evt.data;
    if (name === 'thinking') {
      assistantMsg.thinking = (assistantMsg.thinking || '') + String(data || '');
      patchAssistant();
    } else if (name === 'answer') {
      assistantMsg.content = (assistantMsg.content || '') + String(data || '');
      patchAssistant();
    } else if (name === 'citations') {
      try {
        latestCitations.value = typeof data === 'string' ? (JSON.parse(data) as Citation[]) : (data as Citation[]);
      } catch {
        // ignore
      }
    } else if (name === 'done') {
      patchAssistant();
      loading.value = false;
      question.value = '';
      selectedHotwordKey.value = '';
      cancelStream = null;
    } else if (name === 'error') {
      loading.value = false;
      notice.value = evt.data?.message || '流式请求失败';
      cancelStream = null;
    }
  });
}

function stopStream() {
  if (cancelStream) {
    cancelStream();
    cancelStream = null;
    loading.value = false;
    notice.value = '已停止本次生成';
  }
}

function usePrompt(prompt: HotwordPrompt) {
  question.value = prompt.content;
  selectedHotwordKey.value = prompt.hotwordKey;
}

function clearSelectedHotword() {
  selectedHotwordKey.value = '';
}

async function loadQuickPrompts() {
  hotwordLoading.value = true;
  try {
    quickPrompts.value = await apiGet<HotwordPrompt[]>('/api/hotwords/random?limit=3');
  } catch (error) {
    quickPrompts.value = [];
    notice.value = error instanceof Error ? `热词加载失败：${error.message}` : '热词加载失败，可手动输入问题';
  } finally {
    hotwordLoading.value = false;
  }
}

async function submitFeedback(messageId: number, helpful: boolean) {
  try {
    await apiPost('/api/feedback', {
      requestId: randomRequestId('feedback'),
      messageId,
      helpful,
      comment: helpful ? '回答清晰' : '需要更准确的依据',
    });
    notice.value = '反馈已记录';
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '反馈失败';
  }
}

function formatTime(input: string) {
  if (!input) {
    return '';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(input));
}
</script>

<style scoped>
.chat-layout {
  display: grid;
  grid-template-columns: 1.45fr minmax(240px, 0.75fr);
  gap: 1rem;
}

.panel {
  padding: clamp(1rem, 2vw, 1.25rem);
}

.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.subtle {
  margin: -0.25rem 0 0;
  color: var(--ink-soft);
  line-height: 1.55;
}

.messages {
  margin-top: 0.9rem;
  display: grid;
  gap: 0.65rem;
  max-height: 430px;
  overflow: auto;
  padding-right: 0.25rem;
}

.chat-empty {
  margin-top: 0.9rem;
}

.chat-empty strong {
  display: block;
  color: var(--ink);
  font-family: 'Cormorant Garamond', serif;
  font-size: 1.5rem;
}

.chat-empty p {
  margin: 0.3rem 0 0;
}

.bubble {
  border-radius: 14px;
  padding: 0.8rem;
  border: 1px solid var(--line);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.46);
  animation: rise 0.28s ease both;
}

.user {
  margin-left: clamp(0rem, 8vw, 5rem);
  background:
    radial-gradient(circle at 0% 0%, rgba(255, 255, 255, 0.24), transparent 36%),
    rgba(50, 92, 73, 0.15);
}

.assistant {
  margin-right: clamp(0rem, 8vw, 5rem);
  background:
    radial-gradient(circle at 0% 0%, rgba(255, 255, 255, 0.3), transparent 38%),
    rgba(192, 147, 71, 0.13);
}

.meta {
  margin: 0;
  font-size: 0.76rem;
  color: var(--ink-soft);
}

.text {
  margin: 0.45rem 0 0;
  line-height: 1.58;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.feedback-line {
  margin-top: 0.7rem;
  display: flex;
  gap: 0.5rem;
}

.thinking {
  margin: 0.55rem 0;
  border: 1px dashed var(--line);
  border-radius: 10px;
  padding: 0.5rem 0.75rem;
  background: rgba(0, 0, 0, 0.02);
}

.thinking-text {
  white-space: pre-wrap;
  margin: 0.4rem 0 0;
  font-size: 0.9rem;
  line-height: 1.35;
  color: rgba(0, 0, 0, 0.72);
}

.composer {
  margin-top: 0.9rem;
  display: grid;
  gap: 0.65rem;
}

.prompt-rail {
  margin-top: 0.9rem;
  border: 1px solid rgba(29, 43, 35, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.34);
  padding: 0.72rem;
}

.prompt-rail-head,
.prompt-list {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.prompt-rail-head {
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.6rem;
}

.prompt-rail-head span {
  color: var(--ink);
  font-weight: 700;
}

.prompt-list {
  align-items: center;
}

.prompt-chip {
  border: 1px solid rgba(29, 43, 35, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.4);
  color: var(--ink-soft);
  padding: 0.52rem 0.74rem;
  cursor: pointer;
}

.prompt-chip:hover {
  border-color: rgba(192, 147, 71, 0.45);
  background: rgba(255, 250, 239, 0.76);
  color: var(--ink);
  transform: translateY(-1px);
}

.prompt-empty {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.88rem;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.composer-actions span {
  margin-right: auto;
  color: var(--ink-soft);
  font-size: 0.82rem;
}

.insight-list {
  display: grid;
  gap: 0.7rem;
  margin: 0.7rem 0 1rem;
}

.insight-list div {
  border: 1px solid rgba(29, 43, 35, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.42);
  padding: 0.78rem;
}

.insight-list strong,
.insight-list span {
  display: block;
}

.insight-list span {
  margin-top: 0.24rem;
  color: var(--ink-soft);
  font-size: 0.84rem;
}

.notice {
  border-left: 3px solid var(--brass);
  padding-left: 0.6rem;
  color: var(--ink-soft);
}

.citation-panel {
  margin: 0.8rem 0;
  display: grid;
  gap: 0.55rem;
}

.citation-item {
  border-radius: 10px;
  border: 1px solid var(--line);
  padding: 0.55rem;
  background: rgba(255, 255, 255, 0.55);
}

.citation-source {
  margin: 0 0 0.3rem;
  font-weight: 600;
  font-size: 0.8rem;
}

.citation-fragment {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.82rem;
  line-height: 1.5;
}

@media (max-width: 980px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }
}
</style>
