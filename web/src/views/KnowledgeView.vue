<template>
  <section class="knowledge-grid">
    <article class="card panel">
      <p class="tag">知识库录入</p>
      <h2 class="section-title">新增文档元数据</h2>
      <div class="grid form-grid">
        <div>
          <label>标题</label>
          <input v-model="title" placeholder="例如：劳动合同法-第二次修订" />
        </div>
        <div>
          <label>来源</label>
          <input v-model="source" placeholder="例如：国家法规库" />
        </div>
      </div>
      <div class="actions">
        <button class="primary-btn" @click="createDocument">创建文档</button>
      </div>
      <p class="note">基础版先接元数据，后续可替换为真实文件上传。</p>
    </article>

    <article class="card panel">
      <div class="header-row">
        <h3>文档列表</h3>
        <button class="ghost-btn" @click="refresh">刷新</button>
      </div>

      <div class="doc-list">
        <div v-for="item in documents" :key="item.documentId" class="doc-item">
          <h4>{{ item.title }}</h4>
          <p>来源：{{ item.source }}</p>
          <p>状态：{{ item.status }} / {{ item.indexStatus }}</p>
          <div class="doc-actions">
            <button class="ghost-btn" @click="triggerIndex(item.documentId)">触发索引</button>
            <button class="warn-btn" @click="remove(item.documentId)">删除</button>
          </div>
        </div>
      </div>
      <p v-if="!documents.length" class="note">暂无文档，请先创建一条记录。</p>
      <p v-if="notice" class="notice">{{ notice }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { apiDelete, apiGet, apiPost, randomRequestId } from '../api/client';

interface DocumentItem {
  documentId: number;
  title: string;
  source: string;
  status: string;
  indexStatus: string;
}

const title = ref('');
const source = ref('');
const documents = ref<DocumentItem[]>([]);
const notice = ref('');

onMounted(refresh);

async function refresh() {
  documents.value = await apiGet<DocumentItem[]>('/api/knowledge/documents');
}

async function createDocument() {
  if (!title.value.trim() || !source.value.trim()) {
    notice.value = '请填写标题和来源';
    return;
  }
  await apiPost('/api/knowledge/documents', {
    requestId: randomRequestId('kb-create'),
    title: title.value.trim(),
    source: source.value.trim(),
  });
  title.value = '';
  source.value = '';
  notice.value = '文档已创建';
  await refresh();
}

async function triggerIndex(documentId: number) {
  await apiPost(`/api/knowledge/documents/${documentId}/index?requestId=${encodeURIComponent(randomRequestId('kb-index'))}`);
  notice.value = `文档 ${documentId} 已投递索引任务`;
  await refresh();
}

async function remove(documentId: number) {
  await apiDelete(`/api/knowledge/documents/${documentId}?requestId=${encodeURIComponent(randomRequestId('kb-delete'))}`);
  notice.value = `文档 ${documentId} 已标记删除`;
  await refresh();
}
</script>

<style scoped>
.knowledge-grid {
  display: grid;
  grid-template-columns: minmax(250px, 0.95fr) 1.45fr;
  gap: 1rem;
}

.panel {
  padding: 1rem;
}

.form-grid {
  grid-template-columns: 1fr;
}

.actions {
  margin-top: 0.8rem;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.doc-list {
  margin-top: 0.8rem;
  display: grid;
  gap: 0.7rem;
}

.doc-item {
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
  padding: 0.8rem;
}

.doc-item h4 {
  margin: 0 0 0.35rem;
  font-size: 1.2rem;
}

.doc-item p {
  margin: 0.25rem 0;
  color: var(--ink-soft);
  font-size: 0.86rem;
}

.doc-actions {
  margin-top: 0.55rem;
  display: flex;
  gap: 0.45rem;
}

.notice {
  margin-top: 0.7rem;
  color: var(--ink-soft);
  border-left: 3px solid var(--brass);
  padding-left: 0.6rem;
}

@media (max-width: 980px) {
  .knowledge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
