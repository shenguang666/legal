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
      <div class="menu-shell">
        <button class="menu-arrow" type="button" :disabled="!canScrollMenuLeft" aria-label="向左切换导航" @click="scrollMenu(-1)">‹</button>
        <nav ref="menuRef" class="menu" aria-label="主导航" @scroll="updateMenuScrollState">
          <RouterLink to="/chat" class="menu-link">问答</RouterLink>
          <RouterLink to="/sessions" class="menu-link">会话</RouterLink>
          <RouterLink to="/smart-court" class="menu-link">小法庭</RouterLink>
          <RouterLink v-if="isAdmin" to="/knowledge" class="menu-link">知识库</RouterLink>
          <RouterLink v-if="isAdmin" to="/hotwords" class="menu-link">热词管理</RouterLink>
          <RouterLink v-if="isAdmin" to="/risk-rules" class="menu-link">风险规则</RouterLink>
          <RouterLink v-if="isAdmin" to="/rag-metrics" class="menu-link">RAG指标</RouterLink>
          <RouterLink v-if="isAdmin" to="/token-usage-metrics" class="menu-link">Token指标</RouterLink>
          <RouterLink to="/tianyan" class="menu-link">天眼审查</RouterLink>
        </nav>
        <button class="menu-arrow" type="button" :disabled="!canScrollMenuRight" aria-label="向右切换导航" @click="scrollMenu(1)">›</button>
      </div>
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
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
const menuRef = ref<HTMLElement | null>(null);
const canScrollMenuLeft = ref(false);
const canScrollMenuRight = ref(false);

function updateMenuScrollState() {
  const menu = menuRef.value;
  if (!menu) {
    canScrollMenuLeft.value = false;
    canScrollMenuRight.value = false;
    return;
  }
  const maxScrollLeft = menu.scrollWidth - menu.clientWidth;
  canScrollMenuLeft.value = menu.scrollLeft > 1;
  canScrollMenuRight.value = maxScrollLeft > 1 && menu.scrollLeft < maxScrollLeft - 1;
}

function scrollMenu(direction: -1 | 1) {
  const menu = menuRef.value;
  if (!menu) {
    return;
  }
  menu.scrollBy({
    left: direction * Math.max(menu.clientWidth * 0.72, 180),
    behavior: 'smooth',
  });
  window.setTimeout(updateMenuScrollState, 260);
}

function centerActiveMenuItem() {
  const menu = menuRef.value;
  const activeLink = menu?.querySelector<HTMLElement>('.router-link-active');
  if (!menu || !activeLink) {
    updateMenuScrollState();
    return;
  }
  menu.scrollTo({
    left: activeLink.offsetLeft - (menu.clientWidth - activeLink.offsetWidth) / 2,
    behavior: 'smooth',
  });
  window.setTimeout(updateMenuScrollState, 260);
}

onMounted(() => {
  nextTick(centerActiveMenuItem);
  window.addEventListener('resize', centerActiveMenuItem);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', centerActiveMenuItem);
});

watch(() => route.fullPath, () => nextTick(centerActiveMenuItem));

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

.menu-shell {
  display: grid;
  grid-template-columns: 2.22rem minmax(0, 1fr) 2.22rem;
  align-items: center;
  gap: 0.38rem;
  min-width: 0;
  padding: 0.32rem;
  border: 1px solid rgba(29, 43, 35, 0.1);
  border-radius: 999px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.4), rgba(255, 251, 241, 0.22)),
    rgba(255, 255, 255, 0.24);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.6),
    0 10px 24px rgba(29, 43, 35, 0.06);
}

.menu-arrow {
  display: inline-grid;
  place-items: center;
  width: 2.22rem;
  height: 2.22rem;
  border: 1px solid rgba(29, 43, 35, 0.12);
  border-radius: 999px;
  background:
    radial-gradient(circle at 30% 18%, rgba(255, 255, 255, 0.5), transparent 42%),
    rgba(255, 253, 247, 0.78);
  color: var(--forest-deep);
  cursor: pointer;
  font-size: 1.35rem;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 8px 16px rgba(29, 43, 35, 0.08);
}

.menu-arrow:hover:not(:disabled) {
  border-color: rgba(29, 43, 35, 0.18);
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 255, 255, 0.2), transparent 42%),
    var(--ink);
  color: var(--paper);
  transform: translateY(-1px);
}

.menu-arrow:disabled {
  cursor: default;
  opacity: 0.34;
  box-shadow: none;
}

.menu {
  display: flex;
  gap: 0.5rem;
  flex-wrap: nowrap;
  justify-content: flex-start;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0.08rem;
  border-radius: 999px;
  scroll-behavior: smooth;
  scrollbar-width: none;
}

.menu::-webkit-scrollbar {
  display: none;
}

.menu-link {
  position: relative;
  flex: 0 0 auto;
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

  .menu-shell {
    grid-template-columns: 2rem minmax(0, 1fr) 2rem;
  }

  .menu-arrow {
    width: 2rem;
    height: 2rem;
  }

  .identity {
    justify-content: flex-start;
    min-width: 0;
  }
}
</style>
