<template>
  <section class="hotword-page">
    <article class="card panel hero-panel">
      <div>
        <p class="tag">热词管理</p>
        <h2 class="section-title">聊天快捷问题运营台</h2>
        <p class="note">维护聊天页随机展示的快捷问题热词，保存后会自动刷新 Redis 热词池缓存。</p>
      </div>
      <button class="ghost-btn" type="button" @click="loadHotwords">刷新列表</button>
    </article>

    <div class="hotword-grid">
      <article class="card panel form-panel">
        <div class="header-row">
          <div>
            <p class="tag">{{ editingId ? '编辑热词' : '新增热词' }}</p>
            <h3>{{ editingId ? '更新快捷问题' : '创建快捷问题' }}</h3>
          </div>
          <button class="ghost-btn" type="button" @click="resetForm">清空</button>
        </div>

        <div class="form-stack">
          <div>
            <label for="hotword-content">热词内容</label>
            <textarea
              id="hotword-content"
              v-model="form.content"
              name="content"
              rows="4"
              maxlength="255"
              placeholder="例如：劳动合同到期未续签是否有经济补偿？"
            />
          </div>
          <div>
            <label for="hotword-key">热词标记</label>
            <input
              id="hotword-key"
              v-model="form.hotwordKey"
              name="hotwordKey"
              maxlength="64"
              placeholder="例如：labor_contract_expire_compensation"
              autocomplete="off"
            />
          </div>
          <div>
            <label for="hotword-preset-answer">预设答案</label>
            <textarea
              id="hotword-preset-answer"
              v-model="form.presetAnswer"
              name="presetAnswer"
              rows="6"
              maxlength="4000"
              placeholder="命中该热词标记时，askStream 会直接返回这里配置的答案。"
            />
          </div>
          <div class="grid form-grid">
            <div>
              <label for="hotword-category">分类</label>
              <input id="hotword-category" v-model="form.category" name="category" placeholder="劳动合同" autocomplete="off" />
            </div>
            <div>
              <label for="hotword-weight">权重</label>
              <input id="hotword-weight" v-model.number="form.weight" name="weight" type="number" min="0" step="1" />
            </div>
            <div>
              <label for="hotword-sort-order">排序号</label>
              <input id="hotword-sort-order" v-model.number="form.sortOrder" name="sortOrder" type="number" min="0" step="1" />
            </div>
            <label class="selection-chip hotword-toggle">
              <input v-model="form.enabled" name="enabled" type="checkbox" />
              <span>启用展示</span>
            </label>
          </div>
          <div class="actions">
            <button class="primary-btn" type="button" @click="submitHotword">{{ editingId ? '保存修改' : '新增热词' }}</button>
          </div>
        </div>
        <p v-if="notice" class="notice" aria-live="polite">{{ notice }}</p>
        <p v-if="error" class="error-text" aria-live="assertive">{{ error }}</p>
      </article>

      <article class="card panel list-panel">
        <div class="header-row">
          <div>
            <p class="tag">热词池</p>
            <h3>当前租户热词</h3>
          </div>
          <span class="status-pill">{{ page.total }} 条</span>
        </div>

        <div class="item-list">
          <div v-for="item in hotwords" :key="item.hotwordId" class="item-card hotword-card">
            <div class="item-top">
              <div>
                <h4>{{ item.content }}</h4>
                <p class="note">{{ item.category || '未分类' }} | 标记 {{ item.hotwordKey }} | 权重 {{ item.weight }} | 排序 {{ item.sortOrder }}</p>
              </div>
              <span class="status-pill" :data-status="item.enabled ? 'ENABLED' : 'DISABLED'">{{ item.enabled ? 'ENABLED' : 'DISABLED' }}</span>
            </div>
            <p class="note">更新：{{ formatTime(item.updatedAt) }}</p>
            <div class="item-actions">
              <button class="ghost-btn" type="button" @click="beginEdit(item)">编辑</button>
              <button class="ghost-btn" type="button" @click="toggleHotword(item)">{{ item.enabled ? '停用' : '启用' }}</button>
              <button class="warn-btn" type="button" @click="removeHotword(item)">删除</button>
            </div>
          </div>
          <div v-if="!hotwords.length" class="empty-state">
            <strong>暂无热词</strong>
            <p>新增 3 条以上启用热词后，聊天页会随机展示快捷问题。</p>
          </div>
        </div>

        <div class="pager">
          <button class="ghost-btn" type="button" :disabled="page.pageNo <= 1" @click="prevPage">上一页</button>
          <span>第 {{ page.pageNo }} 页 / 共 {{ page.total }} 条</span>
          <button class="ghost-btn" type="button" :disabled="page.pageNo * page.pageSize >= page.total" @click="nextPage">下一页</button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { apiDelete, apiGet, apiPost, randomRequestId } from '../api/client';

interface HotwordItem {
  hotwordId: number;
  hotwordKey: string;
  content: string;
  presetAnswer: string;
  category?: string;
  weight: number;
  sortOrder: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

interface HotwordPage {
  total: number;
  pageNo: number;
  pageSize: number;
  items: HotwordItem[];
}

const hotwords = ref<HotwordItem[]>([]);
const notice = ref('');
const error = ref('');
const editingId = ref<number | null>(null);
const page = reactive({ total: 0, pageNo: 1, pageSize: 10 });
const form = reactive({ hotwordKey: '', content: '', presetAnswer: '', category: '', weight: 0, sortOrder: 0, enabled: true });

onMounted(loadHotwords);

async function loadHotwords() {
  error.value = '';
  try {
    const result = await apiGet<HotwordPage>(`/api/hotwords?pageNo=${page.pageNo}&pageSize=${page.pageSize}`);
    hotwords.value = result.items || [];
    page.total = result.total;
    page.pageNo = result.pageNo;
    page.pageSize = result.pageSize;
  } catch (err) {
    error.value = err instanceof Error ? `热词列表加载失败：${err.message}` : '热词列表加载失败';
  }
}

async function submitHotword() {
  error.value = '';
  if (!form.content.trim()) {
    error.value = '请填写热词内容';
    return;
  }
  if (!form.presetAnswer.trim()) {
    error.value = '请填写预设答案';
    return;
  }
  const payload = {
    requestId: randomRequestId(editingId.value ? 'hotword-update' : 'hotword-create'),
    hotwordKey: form.hotwordKey.trim() || null,
    content: form.content.trim(),
    presetAnswer: form.presetAnswer.trim(),
    category: form.category.trim() || null,
    weight: Number.isFinite(form.weight) ? form.weight : 0,
    sortOrder: Number.isFinite(form.sortOrder) ? form.sortOrder : 0,
    enabled: form.enabled,
  };
  try {
    if (editingId.value) {
      await apiPost<HotwordItem>(`/api/hotwords/${editingId.value}`, payload);
      notice.value = '热词已更新，缓存将在下一次聊天页刷新时生效';
    } else {
      await apiPost<HotwordItem>('/api/hotwords', payload);
      notice.value = '热词已新增，缓存将在下一次聊天页刷新时生效';
    }
    resetForm();
    await loadHotwords();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '热词保存失败';
  }
}

function beginEdit(item: HotwordItem) {
  editingId.value = item.hotwordId;
  form.hotwordKey = item.hotwordKey || '';
  form.content = item.content;
  form.presetAnswer = item.presetAnswer || '';
  form.category = item.category || '';
  form.weight = item.weight || 0;
  form.sortOrder = item.sortOrder || 0;
  form.enabled = item.enabled;
  error.value = '';
}

function resetForm() {
  editingId.value = null;
  form.hotwordKey = '';
  form.content = '';
  form.presetAnswer = '';
  form.category = '';
  form.weight = 0;
  form.sortOrder = 0;
  form.enabled = true;
  error.value = '';
}

async function toggleHotword(item: HotwordItem) {
  error.value = '';
  try {
    await apiPost<HotwordItem>(`/api/hotwords/${item.hotwordId}/status`, {
      requestId: randomRequestId('hotword-status'),
      enabled: !item.enabled,
    });
    notice.value = `热词已${item.enabled ? '停用' : '启用'}`;
    await loadHotwords();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '热词状态更新失败';
  }
}

async function removeHotword(item: HotwordItem) {
  if (!window.confirm(`确认删除热词“${item.content}”？`)) {
    return;
  }
  error.value = '';
  try {
    await apiDelete(`/api/hotwords/${item.hotwordId}?requestId=${encodeURIComponent(randomRequestId('hotword-delete'))}`);
    notice.value = '热词已删除';
    await loadHotwords();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '热词删除失败';
  }
}

function prevPage() {
  if (page.pageNo <= 1) {
    return;
  }
  page.pageNo -= 1;
  loadHotwords();
}

function nextPage() {
  if (page.pageNo * page.pageSize >= page.total) {
    return;
  }
  page.pageNo += 1;
  loadHotwords();
}

function formatTime(input: string) {
  if (!input) {
    return '-';
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
.hotword-page {
  display: grid;
  gap: 1rem;
}

.panel {
  padding: clamp(1rem, 2vw, 1.25rem);
}

.hero-panel,
.header-row,
.item-top,
.item-actions,
.pager {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.hotword-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.78fr) minmax(360px, 1.22fr);
  gap: 1rem;
}

.form-stack,
.item-list {
  display: grid;
  gap: 0.85rem;
}

.hotword-toggle {
  align-self: end;
  min-height: 42px;
}

.hotword-card {
  position: relative;
  overflow: hidden;
}

.hotword-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: linear-gradient(180deg, var(--brass), var(--forest));
  opacity: 0.76;
}

.item-actions,
.pager {
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.pager {
  margin-top: 1rem;
}

.error-text {
  color: #9b2c2c;
}

@media (max-width: 980px) {
  .hotword-grid {
    grid-template-columns: 1fr;
  }

  .hero-panel,
  .header-row,
  .item-top {
    flex-direction: column;
  }
}
</style>
