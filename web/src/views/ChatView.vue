<template>
  <section class="chat-layout">
    <div class="left card panel">
      <div class="head-row">
        <div>
          <p class="tag">对话工作台</p>
          <h2 class="section-title">法律问答会话</h2>
        </div>
        <button class="ghost-btn" @click="newSession">新建会话</button>
      </div>

      <div class="messages">
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
            <button class="ghost-btn" @click="submitFeedback(item.messageId, true)">有帮助</button>
            <button class="warn-btn" @click="submitFeedback(item.messageId, false)">没帮助</button>
          </div>
        </article>
      </div>

      <div class="composer">
        <textarea
          v-model="question"
          rows="4"
          placeholder="输入你的法律问题，例如：劳动合同到期未续签是否有补偿？"
        />
        <button class="primary-btn" :disabled="loading" @click="askStream">{{ loading ? '生成中...' : '发送问题(流式)' }}</button>
      </div>
      <p class="note">当前 sessionId：{{ sessionId || '尚未创建' }}</p>
    </div>

    <aside class="right card panel">
      <h3>系统提示</h3>
      <ul>
        <li>当前问答已接入混合检索（向量 + BM25）。</li>
        <li>答案下方可查看命中的知识库引用片段。</li>
        <li>所有写接口都带 requestId，支持幂等。</li>
      </ul>
      <div v-if="latestCitations.length" class="citation-panel">
        <h4>本轮引用</h4>
        <article v-for="(item, idx) in latestCitations" :key="`${item.documentId}-${idx}`" class="citation-item">
          <p class="citation-source">{{ item.source }}</p>
          <p class="citation-fragment">{{ item.fragment }}</p>
        </article>
      </div>
      <p v-if="notice" class="notice">{{ notice }}</p>
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

const route = useRoute();
const sessionId = ref<string>('');
const messages = ref<ChatMessage[]>([]);
const question = ref('');
const notice = ref('');
const loading = ref(false);
const latestCitations = ref<Citation[]>([]);
let cancelStream: null | (() => void) = null;

onMounted(async () => {
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
      cancelStream = null;
    } else if (name === 'error') {
      loading.value = false;
      notice.value = evt.data?.message || '流式请求失败';
      cancelStream = null;
    }
  });
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
  return new Date(input).toLocaleString();
}
</script>

<style scoped>
.chat-layout {
  display: grid;
  grid-template-columns: 1.45fr minmax(240px, 0.75fr);
  gap: 1rem;
}

.panel {
  padding: 1rem;
}

.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.messages {
  margin-top: 0.9rem;
  display: grid;
  gap: 0.65rem;
  max-height: 430px;
  overflow: auto;
  padding-right: 0.25rem;
}

.bubble {
  border-radius: 14px;
  padding: 0.8rem;
  border: 1px solid var(--line);
}

.user {
  background: rgba(50, 92, 73, 0.15);
}

.assistant {
  background: rgba(192, 147, 71, 0.13);
}

.meta {
  margin: 0;
  font-size: 0.76rem;
  color: var(--ink-soft);
}

.text {
  margin: 0.45rem 0 0;
  line-height: 1.58;
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

.right ul {
  margin-top: 0.25rem;
  margin-bottom: 0.8rem;
  padding-left: 1rem;
  line-height: 1.6;
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
