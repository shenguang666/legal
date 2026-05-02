<template>
  <section class="sessions-layout card panel">
    <div class="header-row">
      <div>
        <p class="tag">会话索引</p>
        <h2 class="section-title">历史会话</h2>
        <p class="note">快速回到历史咨询上下文，继续追问或复核答案引用。</p>
      </div>
      <button class="primary-btn" type="button" @click="refresh">刷新列表</button>
    </div>

    <div class="session-grid">
      <article v-for="item in sessions" :key="item.sessionId" class="session-item">
        <span class="session-badge">SESSION</span>
        <h3>{{ item.title }}</h3>
        <p>ID：{{ item.sessionId }}</p>
        <p>创建：{{ formatTime(item.createdAt) }}</p>
        <p>活跃：{{ formatTime(item.lastActiveAt) }}</p>
        <button class="ghost-btn" type="button" @click="open(item.sessionId)">打开会话</button>
      </article>
    </div>

    <p v-if="!sessions.length" class="note">暂无会话数据，可先到问答页发送一条消息。</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { apiGet } from '../api/client';

interface ChatSession {
  sessionId: string;
  title: string;
  createdAt: string;
  lastActiveAt: string;
}

const router = useRouter();
const sessions = ref<ChatSession[]>([]);

onMounted(refresh);

async function refresh() {
  sessions.value = await apiGet<ChatSession[]>('/api/chat/sessions');
}

function open(sessionId: string) {
  localStorage.setItem('legal.activeSessionId', sessionId);
  router.push({ path: '/chat', query: { sessionId } });
}

function formatTime(input: string) {
  if (!input) {
    return '';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(input));
}
</script>

<style scoped>
.panel {
  padding: clamp(1rem, 2vw, 1.25rem);
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.session-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 0.8rem;
}

.session-item {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid var(--line);
  background:
    radial-gradient(circle at 100% 0%, rgba(192, 147, 71, 0.14), transparent 36%),
    rgba(255, 255, 255, 0.6);
  padding: 0.9rem;
  min-width: 0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.52);
}

.session-item:hover {
  border-color: rgba(192, 147, 71, 0.42);
  box-shadow: var(--lift-shadow);
  transform: translateY(-2px);
}

.session-badge {
  display: inline-flex;
  margin-bottom: 0.55rem;
  border-radius: 999px;
  background: var(--forest-soft);
  color: var(--forest-deep);
  padding: 0.22rem 0.5rem;
  font-size: 0.68rem;
  letter-spacing: 0.12em;
}

.session-item h3 {
  margin: 0 0 0.4rem;
  font-size: 1.3rem;
}

.session-item p {
  margin: 0.2rem 0;
  color: var(--ink-soft);
  font-size: 0.85rem;
  overflow-wrap: anywhere;
}

.session-item button {
  margin-top: 0.6rem;
}
</style>
