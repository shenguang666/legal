<template>
  <div class="shell">
    <div class="shell-glow"></div>
    <header class="topbar card">
      <div>
        <p class="eyebrow">LEGAL RAG BASELINE</p>
        <h1 class="brand">律典问答台</h1>
      </div>
      <nav class="menu">
        <RouterLink to="/chat" class="menu-link">问答</RouterLink>
        <RouterLink to="/sessions" class="menu-link">会话</RouterLink>
        <RouterLink v-if="isAdmin" to="/knowledge" class="menu-link">知识库</RouterLink>
      </nav>
      <div class="identity">
        <span>{{ displayName }}</span>
        <span>{{ roleCode }}</span>
        <span>T{{ tenantId }}</span>
        <span>U{{ userId }}</span>
        <button class="ghost-btn" @click="logout">退出</button>
      </div>
    </header>

    <main class="page card">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router';
import { apiLogout, currentRole, hasToken } from './api/client';

const router = useRouter();
const route = useRoute();

const authState = computed(() => {
  route.fullPath;
  return {
    tenantId: localStorage.getItem('legal.tenantId') || '1001',
    userId: localStorage.getItem('legal.userId') || '-',
    displayName: localStorage.getItem('legal.displayName') || localStorage.getItem('legal.username') || '未登录',
    roleCode: currentRole(),
  };
});

const tenantId = computed(() => authState.value.tenantId);
const userId = computed(() => authState.value.userId);
const displayName = computed(() => authState.value.displayName);
const roleCode = computed(() => authState.value.roleCode);
const isAdmin = computed(() => roleCode.value === 'ADMIN');

async function logout() {
  if (hasToken()) {
    await apiLogout();
  }
  router.push('/login');
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  padding: 2.2rem clamp(1rem, 2.8vw, 3rem);
  position: relative;
}

.shell-glow {
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 14% 12%, rgba(192, 147, 71, 0.2), transparent 35%),
    radial-gradient(circle at 85% 88%, rgba(50, 92, 73, 0.26), transparent 38%);
}

.topbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto auto;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1rem;
}

.eyebrow {
  margin: 0;
  letter-spacing: 0.24em;
  font-size: 0.67rem;
  color: var(--ink-soft);
}

.brand {
  margin: 0.2rem 0 0;
  font-size: clamp(1.6rem, 3vw, 2.2rem);
}

.menu {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.menu-link {
  text-decoration: none;
  color: var(--ink);
  padding: 0.55rem 0.8rem;
  border: 1px solid rgba(30, 43, 35, 0.2);
  border-radius: 999px;
  font-size: 0.92rem;
  transition: all 0.2s ease;
}

.menu-link:hover,
.menu-link.router-link-active {
  background: var(--ink);
  color: var(--paper);
}

.identity {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.65rem;
  font-size: 0.85rem;
  flex-wrap: wrap;
}

.page {
  min-height: calc(100vh - 170px);
}

@media (max-width: 960px) {
  .topbar {
    grid-template-columns: 1fr;
  }

  .identity {
    justify-content: flex-start;
  }
}
</style>
