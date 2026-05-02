<template>
  <div class="shell">
    <div class="shell-glow"></div>
    <a class="skip-link" href="#main-content">跳到主内容</a>
    <header class="topbar card">
      <div class="brand-block">
        <p class="eyebrow">LEGAL RAG BASELINE</p>
        <h1 class="brand">律典问答台</h1>
        <p class="brand-subtitle">企业法律知识、合同风险与检索质量的智能控制台</p>
      </div>
      <nav class="menu" aria-label="主导航">
        <RouterLink to="/chat" class="menu-link">问答</RouterLink>
        <RouterLink to="/sessions" class="menu-link">会话</RouterLink>
        <RouterLink v-if="isAdmin" to="/knowledge" class="menu-link">知识库</RouterLink>
        <RouterLink v-if="isAdmin" to="/risk-rules" class="menu-link">风险规则</RouterLink>
        <RouterLink v-if="isAdmin" to="/rag-metrics" class="menu-link">RAG指标</RouterLink>
        <RouterLink v-if="isAdmin" to="/tianyan" class="menu-link">天眼审查</RouterLink>
      </nav>
      <div class="identity">
        <span class="identity-avatar">{{ displayName.slice(0, 1).toUpperCase() }}</span>
        <span>{{ displayName }}</span>
        <span>{{ roleCode }}</span>
        <span>T{{ tenantId }}</span>
        <span>U{{ userId }}</span>
        <button class="ghost-btn" type="button" @click="logout">退出</button>
      </div>
    </header>

    <main id="main-content" class="page card">
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

.skip-link {
  position: fixed;
  left: 1rem;
  top: 1rem;
  z-index: 20;
  transform: translateY(-180%);
  border-radius: 999px;
  background: var(--ink);
  color: var(--paper-strong);
  padding: 0.7rem 1rem;
  text-decoration: none;
}

.skip-link:focus-visible {
  transform: translateY(0);
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
  grid-template-columns: minmax(260px, 0.92fr) minmax(320px, 1.35fr) auto;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1rem;
  padding: 1rem;
  border-radius: 28px;
}

.brand-block {
  min-width: 0;
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

.brand-subtitle {
  margin: 0.2rem 0 0;
  color: var(--ink-soft);
  font-size: 0.84rem;
  line-height: 1.45;
}

.menu {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: center;
  padding: 0.34rem;
  border: 1px solid rgba(29, 43, 35, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.32);
}

.menu-link {
  position: relative;
  text-decoration: none;
  color: var(--ink);
  padding: 0.58rem 0.86rem;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 0.92rem;
  white-space: nowrap;
}

.menu-link:hover,
.menu-link.router-link-active {
  border-color: rgba(29, 43, 35, 0.18);
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 255, 255, 0.18), transparent 42%),
    var(--ink);
  color: var(--paper);
  box-shadow: 0 10px 20px rgba(29, 43, 35, 0.16);
  transform: translateY(-1px);
}

.identity {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.65rem;
  font-size: 0.85rem;
  flex-wrap: wrap;
  min-width: 220px;
}

.identity span:not(.identity-avatar) {
  border: 1px solid rgba(29, 43, 35, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.34);
  padding: 0.4rem 0.62rem;
}

.identity-avatar {
  display: inline-grid;
  place-items: center;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--forest), var(--brass));
  color: var(--paper-strong);
  font-weight: 700;
  box-shadow: 0 12px 22px rgba(50, 92, 73, 0.2);
}

.page {
  min-height: calc(100vh - 170px);
  padding: clamp(0.75rem, 2vw, 1.1rem);
  border-radius: 28px;
}

@media (max-width: 960px) {
  .topbar {
    grid-template-columns: 1fr;
  }

  .menu {
    justify-content: flex-start;
    border-radius: 22px;
  }

  .identity {
    justify-content: flex-start;
    min-width: 0;
  }
}
</style>
