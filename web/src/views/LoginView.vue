<template>
  <section class="login-layout">
    <article class="login-hero card panel">
      <div class="hero-copy">
        <p class="tag">Sa-Token 登录入口</p>
        <h2 class="section-title">登录律典问答台</h2>
        <p class="intro">
          当前系统已切换为账号密码登录。普通用户仅可使用问答与会话功能，管理员可额外管理知识库。
        </p>
        <div class="feature-strip" aria-label="系统能力">
          <span>混合检索</span>
          <span>合同天眼</span>
          <span>RAG 指标</span>
        </div>
      </div>

      <form class="login-form" @submit.prevent="login">
        <div class="grid form-grid">
          <div>
            <label for="tenantId">租户 ID</label>
            <input id="tenantId" v-model="tenantId" name="tenantId" type="number" inputmode="numeric" autocomplete="off" placeholder="例如 1001…" />
          </div>
          <div>
            <label for="username">用户名</label>
            <input id="username" v-model="username" name="username" autocomplete="username" spellcheck="false" placeholder="admin / user…" />
          </div>
          <div>
            <label for="password">密码</label>
            <input id="password" v-model="password" name="password" type="password" autocomplete="current-password" placeholder="请输入密码…" />
          </div>
        </div>

        <div class="actions">
          <button class="primary-btn" type="submit" :disabled="loading">
            {{ loading ? '登录中…' : '进入系统' }}
          </button>
        </div>
      </form>

      <div class="credential-grid" aria-label="演示账号">
        <div>
          <span>默认租户</span>
          <strong>1001</strong>
        </div>
        <div>
          <span>管理员</span>
          <strong>admin / Admin@123</strong>
        </div>
        <div>
          <span>普通用户</span>
          <strong>user / User@123</strong>
        </div>
      </div>
      <p v-if="notice" class="notice" aria-live="polite">{{ notice }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { apiLogin } from '../api/client';

const router = useRouter();
const tenantId = ref(localStorage.getItem('legal.tenantId') || '1001');
const username = ref(localStorage.getItem('legal.username') || 'user');
const password = ref('');
const notice = ref('');
const loading = ref(false);

async function login() {
  const tenantIdValue = String(tenantId.value ?? '').trim();
  const usernameValue = username.value.trim();
  const passwordValue = password.value.trim();
  if (!tenantIdValue || !usernameValue || !passwordValue) {
    notice.value = '请完整填写租户、用户名和密码';
    return;
  }
  loading.value = true;
  notice.value = '';
  try {
    await apiLogin({
      tenantId: Number(tenantIdValue),
      username: usernameValue,
      password: passwordValue,
    });
    router.push('/chat');
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '登录失败';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-layout {
  max-width: 980px;
  min-height: calc(100vh - 250px);
  display: grid;
  align-items: center;
}

.panel {
  padding: clamp(1.2rem, 3vw, 2rem);
}

.login-hero {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(280px, 1fr);
  gap: clamp(1rem, 4vw, 2.4rem);
  align-items: center;
}

.hero-copy {
  min-width: 0;
}

.intro {
  margin: 0 0 1rem;
  color: var(--ink-soft);
  line-height: 1.6;
}

.feature-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 1.2rem;
}

.feature-strip span,
.credential-grid div {
  border: 1px solid rgba(29, 43, 35, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.38);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.54);
}

.feature-strip span {
  padding: 0.48rem 0.68rem;
  color: var(--ink-soft);
  font-size: 0.86rem;
}

.login-form {
  min-width: 0;
}

.form-grid {
  grid-template-columns: 1fr;
}

.actions {
  margin-top: 1rem;
}

.actions .primary-btn {
  width: 100%;
}

.credential-grid {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
  margin-top: 1.2rem;
}

.credential-grid div {
  padding: 0.82rem;
}

.credential-grid span {
  display: block;
  color: var(--ink-soft);
  font-size: 0.78rem;
}

.credential-grid strong {
  display: block;
  margin-top: 0.35rem;
  overflow-wrap: anywhere;
  font-size: 0.95rem;
}

.notice {
  margin-top: 0.8rem;
  color: var(--ink-soft);
  border-left: 3px solid var(--brass);
  padding-left: 0.6rem;
}

@media (max-width: 860px) {
  .login-hero,
  .credential-grid {
    grid-template-columns: 1fr;
  }
}
</style>
