<template>
  <section class="login-layout">
    <article class="card panel">
      <p class="tag">Sa-Token 登录入口</p>
      <h2 class="section-title">登录律典问答台</h2>
      <p class="intro">
        当前系统已切换为账号密码登录。普通用户仅可使用问答与会话功能，管理员可额外管理知识库。
      </p>

      <div class="grid form-grid">
        <div>
          <label>租户 ID</label>
          <input v-model="tenantId" type="number" placeholder="例如 1001" />
        </div>
        <div>
          <label>用户名</label>
          <input v-model="username" placeholder="admin / user" />
        </div>
        <div>
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" @keyup.enter="login" />
        </div>
      </div>

      <div class="actions">
        <button class="primary-btn" :disabled="loading" @click="login">
          {{ loading ? '登录中...' : '进入系统' }}
        </button>
      </div>
      <p class="note">默认租户：1001</p>
      <p class="note">管理员：admin / Admin@123</p>
      <p class="note">普通用户：user / User@123</p>
      <p v-if="notice" class="notice">{{ notice }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { apiLogin, hasToken } from '../api/client';

const router = useRouter();
const tenantId = ref(localStorage.getItem('legal.tenantId') || '1001');
const username = ref(localStorage.getItem('legal.username') || 'user');
const password = ref('');
const notice = ref('');
const loading = ref(false);

if (hasToken()) {
  router.replace('/chat');
}

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
  max-width: 780px;
}

.panel {
  padding: 1.3rem 1.4rem;
}

.intro {
  margin: 0 0 1rem;
  color: var(--ink-soft);
  line-height: 1.6;
}

.form-grid {
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}

.actions {
  margin-top: 0.9rem;
}

.notice {
  margin-top: 0.8rem;
  color: var(--ink-soft);
  border-left: 3px solid var(--brass);
  padding-left: 0.6rem;
}
</style>
