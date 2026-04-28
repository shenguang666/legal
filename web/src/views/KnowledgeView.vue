<template>
  <section class="knowledge-grid">
    <article class="card panel">
      <p class="tag">知识库录入</p>
      <h2 class="section-title">上传文档并向量化</h2>
      <div class="grid form-grid">
        <div>
          <label>标题</label>
          <input v-model="title" placeholder="可选，不填则使用文件名" />
        </div>
        <div>
          <label>来源</label>
          <input v-model="source" placeholder="可选，例如：国家法规库" />
        </div>
        <div>
          <label>文件</label>
          <input
            type="file"
            accept=".pdf,.doc,.docx,.txt,.md"
            @change="onSelectFile"
          />
        </div>
      </div>
      <div class="actions">
        <button class="primary-btn" @click="importDocument">导入并索引</button>
      </div>
      <p class="note">支持 pdf/doc/docx/txt/md，导入后由后台异步完成向量索引。</p>
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
import { apiDelete, apiGet, apiPost, apiPostForm, randomRequestId } from '../api/client';

interface DocumentItem {
  documentId: number;
  title: string;
  source: string;
  status: string;
  indexStatus: string;
}

const title = ref('');
const source = ref('');
const selectedFile = ref<File | null>(null);
const documents = ref<DocumentItem[]>([]);
const notice = ref('');

onMounted(refresh);

async function refresh() {
  documents.value = await apiGet<DocumentItem[]>('/api/knowledge/documents');
}

function onSelectFile(event: Event) {
  const input = event.target as HTMLInputElement;
  selectedFile.value = input.files?.[0] || null;
}

async function importDocument() {
  if (!selectedFile.value) {
    notice.value = '请先选择文件';
    return;
  }
  const formData = new FormData();
  formData.append('requestId', randomRequestId('kb-import'));
  formData.append('file', selectedFile.value);
  if (title.value.trim()) {
    formData.append('title', title.value.trim());
  }
  if (source.value.trim()) {
    formData.append('source', source.value.trim());
  }
  await apiPostForm('/api/knowledge/documents/import', formData);
  title.value = '';
  source.value = '';
  selectedFile.value = null;
  notice.value = '文档已导入，正在后台建立向量索引';
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
