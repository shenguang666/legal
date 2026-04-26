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
        <button class="primary-btn" :disabled="loading" @click="ask">{{ loading ? '发送中...' : '发送问题' }}</button>
      </div>
      <p class="note">当前 sessionId：{{ sessionId || '尚未创建' }}</p>
    </div>

    <aside class="right card panel">
      <h3>系统提示</h3>
      <ul>
        <li>基础版已接通会话、知识库、反馈接口。</li>
        <li>当前问答结果为占位模型回复，可继续接入 LangChain4j 与向量检索。</li>
        <li>所有写接口都带 requestId，支持幂等。</li>
      </ul>
      <p v-if="notice" class="notice">{{ notice }}</p>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { apiGet, apiPost, randomRequestId } from '../api/client';

interface ChatMessage {
  messageId: number;
  role: string;
  content: string;
  createdAt: string;
}

interface CreateSessionResponse {
  sessionId: string;
  title: string;
  createdAt: string;
}

interface AskResponse {
  answer: string;
  confidence: number;
  warning: string;
}

const route = useRoute();
const sessionId = ref<string>('');
const messages = ref<ChatMessage[]>([]);
const question = ref('');
const notice = ref('');
const loading = ref(false);

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
    notice.value = `置信度：${result.confidence.toFixed(2)} ｜ ${result.warning}`;
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '发送失败';
  } finally {
    loading.value = false;
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

@media (max-width: 980px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }
}
</style>
