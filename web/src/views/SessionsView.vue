<template>
  <section class="sessions-layout card panel">
    <div class="header-row">
      <div>
        <p class="tag">会话索引</p>
        <h2 class="section-title">历史会话</h2>
      </div>
      <button class="primary-btn" @click="refresh">刷新列表</button>
    </div>

    <div class="session-grid">
      <article v-for="item in sessions" :key="item.sessionId" class="session-item">
        <h3>{{ item.title }}</h3>
        <p>ID：{{ item.sessionId }}</p>
        <p>创建：{{ formatTime(item.createdAt) }}</p>
        <p>活跃：{{ formatTime(item.lastActiveAt) }}</p>
        <button class="ghost-btn" @click="open(item.sessionId)">打开会话</button>
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
  return new Date(input).toLocaleString();
}
</script>

<style scoped>
.panel {
  padding: 1rem;
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
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
  padding: 0.9rem;
}

.session-item h3 {
  margin: 0 0 0.4rem;
  font-size: 1.3rem;
}

.session-item p {
  margin: 0.2rem 0;
  color: var(--ink-soft);
  font-size: 0.85rem;
}

.session-item button {
  margin-top: 0.6rem;
}
</style>
