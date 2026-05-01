<template>
  <section class="knowledge-grid">
    <article class="card panel">
      <p class="tag">天眼审查</p>
      <h2 class="section-title">上传待审查文档</h2>
      <div class="grid form-grid">
        <div>
          <label>标题</label>
          <input v-model="title" placeholder="可选，不填则使用文件名" />
        </div>
        <div>
          <label>来源</label>
          <input v-model="source" placeholder="可选，例如：合同中心 / 邮件附件" />
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
        <button class="primary-btn" @click="importDocument">导入审查文档</button>
      </div>
      <p class="note">导入后可直接发起天眼审查，系统会从文档切片中抽取字段并执行风险规则校验。</p>
    </article>

    <article class="card panel">
      <div class="header-row">
        <h3>待审查文档列表</h3>
        <button class="ghost-btn" @click="refresh">刷新</button>
      </div>

      <div class="doc-list">
        <div v-for="item in documents" :key="item.documentId" class="doc-item">
          <h4>{{ item.title }}</h4>
          <p>来源：{{ item.source }}</p>
          <p>状态：{{ item.status }} / {{ item.indexStatus }}</p>
          <div class="doc-actions">
            <button class="primary-btn" @click="openReview(item)">进入审查</button>
            <button class="warn-btn" @click="remove(item.documentId)">删除</button>
          </div>
        </div>
      </div>
      <p v-if="!documents.length" class="note">暂无待审查文档，请先上传。</p>
      <p v-if="notice" class="notice">{{ notice }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { apiDelete, apiGet, apiPostForm, randomRequestId } from '../api/client';

interface DocumentItem {
  documentId: number;
  title: string;
  source: string;
  status: string;
  indexStatus: string;
  bizType?: string;
}

const router = useRouter();
const title = ref('');
const source = ref('');
const selectedFile = ref<File | null>(null);
const documents = ref<DocumentItem[]>([]);
const notice = ref('');

onMounted(refresh);

async function refresh() {
  documents.value = await apiGet<DocumentItem[]>('/api/tianyan/documents');
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
  formData.append('requestId', randomRequestId('tianyan-import'));
  formData.append('file', selectedFile.value);
  if (title.value.trim()) {
    formData.append('title', title.value.trim());
  }
  if (source.value.trim()) {
    formData.append('source', source.value.trim());
  }
  await apiPostForm('/api/tianyan/documents/import', formData);
  title.value = '';
  source.value = '';
  selectedFile.value = null;
  notice.value = '审查文档已导入，可立即发起天眼审查';
  await refresh();
}

async function remove(documentId: number) {
  await apiDelete(`/api/tianyan/documents/${documentId}?requestId=${encodeURIComponent(randomRequestId('tianyan-delete'))}`);
  notice.value = `审查文档 ${documentId} 已标记删除`;
  await refresh();
}

function openReview(item: DocumentItem) {
  router.push({
    path: `/tianyan/reviews/${item.documentId}`,
    query: {
      title: item.title,
    },
  });
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
