<template>
  <section class="knowledge-grid">
    <article class="card panel">
      <p class="tag">知识库录入</p>
      <h2 class="section-title">上传文档并向量化</h2>
      <div class="grid form-grid">
        <div>
          <label for="knowledge-title">标题</label>
          <input id="knowledge-title" v-model="title" name="knowledgeTitle" autocomplete="off" placeholder="可选，不填则使用文件名…" />
        </div>
        <div>
          <label for="knowledge-source">来源</label>
          <input id="knowledge-source" v-model="source" name="knowledgeSource" autocomplete="off" placeholder="可选，例如：国家法规库…" />
        </div>
        <div>
          <label for="knowledge-file">文件</label>
          <input
            id="knowledge-file"
            name="knowledgeFile"
            type="file"
            multiple
            accept=".pdf,.doc,.docx,.txt,.md"
            @change="onSelectFile"
          />
        </div>
        <div>
          <label for="knowledge-parse-method">解析方式</label>
          <select id="knowledge-parse-method" v-model="parseMethod" class="console-select">
            <option v-for="method in parseMethods" :key="method" :value="method">{{ parseMethodLabel(method) }}</option>
          </select>
        </div>
      </div>
      <div class="actions">
        <button class="primary-btn" type="button" @click="importDocument">导入并索引</button>
      </div>
      <p class="note">支持 pdf/doc/docx/txt/md，单次最多上传 {{ maxUploadDocuments }} 个文档，导入后由后台异步完成解析与向量索引。</p>
    </article>

    <article class="card panel">
      <div class="header-row">
        <div>
          <p class="tag">知识资产</p>
          <h3>文档列表</h3>
        </div>
        <button class="ghost-btn" type="button" @click="refresh">刷新</button>
      </div>

      <div class="doc-list">
        <div v-for="item in documents" :key="item.documentId" class="doc-item">
          <span class="doc-status">{{ item.status }} / {{ item.indexStatus }} / {{ item.parseStatus || 'COMPLETED' }}</span>
          <h4>{{ item.title }}</h4>
          <p>来源：{{ item.source }}</p>
          <div class="doc-actions">
            <button class="ghost-btn" type="button" @click="triggerIndex(item.documentId)">触发索引</button>
            <button v-if="item.parseStatus === 'FAILED'" class="ghost-btn" type="button" @click="retryParse(item.documentId)">重试解析</button>
            <button class="warn-btn" type="button" @click="remove(item.documentId)">删除</button>
          </div>
          <p v-if="item.parseFailureReason" class="note">解析失败：{{ item.parseFailureReason }}</p>
        </div>
      </div>
      <p v-if="!documents.length" class="note">暂无文档，请先创建一条记录。</p>
      <p v-if="notice" class="notice" aria-live="polite">{{ notice }}</p>
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
  parseMethod?: string;
  parseStatus?: string;
  parseFailureReason?: string;
}

interface DocumentProcessingCapabilities {
  defaultParseMethod: string;
  maxUploadDocuments: number;
  availableParseMethods: string[];
}

const title = ref('');
const source = ref('');
const selectedFiles = ref<File[]>([]);
const documents = ref<DocumentItem[]>([]);
const notice = ref('');
const parseMethod = ref('NATIVE');
const parseMethods = ref<string[]>(['NATIVE']);
const maxUploadDocuments = ref(1);

onMounted(async () => {
  await loadCapabilities();
  await refresh();
});

async function loadCapabilities() {
  const capabilities = await apiGet<DocumentProcessingCapabilities>('/api/document-processing/capabilities');
  parseMethods.value = capabilities.availableParseMethods?.length ? capabilities.availableParseMethods : ['NATIVE'];
  parseMethod.value = capabilities.defaultParseMethod || parseMethods.value[0];
  maxUploadDocuments.value = capabilities.maxUploadDocuments || 1;
}

async function refresh() {
  documents.value = await apiGet<DocumentItem[]>('/api/knowledge/documents');
}

function onSelectFile(event: Event) {
  const input = event.target as HTMLInputElement;
  selectedFiles.value = Array.from(input.files || []);
}

async function importDocument() {
  if (!selectedFiles.value.length) {
    notice.value = '请先选择文件';
    return;
  }
  if (selectedFiles.value.length > maxUploadDocuments.value) {
    notice.value = `单次最多上传 ${maxUploadDocuments.value} 个文档`;
    return;
  }
  for (const file of selectedFiles.value) {
    const formData = new FormData();
    formData.append('requestId', randomRequestId('kb-import'));
    formData.append('file', file);
    if (title.value.trim() && selectedFiles.value.length === 1) {
      formData.append('title', title.value.trim());
    }
    if (source.value.trim()) {
      formData.append('source', source.value.trim());
    }
    formData.append('parseMethod', parseMethod.value);
    await apiPostForm('/api/knowledge/documents/import', formData);
  }
  title.value = '';
  source.value = '';
  selectedFiles.value = [];
  notice.value = parseMethod.value === 'MINERU_PRECISE' ? '文档已导入，正在后台进行 MinerU 精准解析' : '文档已导入，正在后台建立向量索引';
  await refresh();
}

async function triggerIndex(documentId: number) {
  await apiPost(`/api/knowledge/documents/${documentId}/index?requestId=${encodeURIComponent(randomRequestId('kb-index'))}`);
  notice.value = `文档 ${documentId} 已投递索引任务`;
  await refresh();
}

async function retryParse(documentId: number) {
  await apiPost(`/api/knowledge/documents/${documentId}/retry-parse?requestId=${encodeURIComponent(randomRequestId('kb-retry-parse'))}`);
  notice.value = `文档 ${documentId} 已重新提交解析`;
  await refresh();
}

function parseMethodLabel(method: string) {
  if (method === 'MINERU_PRECISE') {
    return 'MinerU 精准解析';
  }
  return '原生解析';
}

async function remove(documentId: number) {
  if (!window.confirm(`确认删除知识库文档 ${documentId}？`)) {
    return;
  }
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
  padding: clamp(1rem, 2vw, 1.25rem);
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
  position: relative;
  border-radius: 14px;
  border: 1px solid var(--line);
  background:
    radial-gradient(circle at 100% 0%, rgba(50, 92, 73, 0.12), transparent 36%),
    rgba(255, 255, 255, 0.6);
  padding: 0.8rem;
  min-width: 0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.52);
}

.doc-item:hover {
  border-color: rgba(192, 147, 71, 0.42);
  transform: translateY(-2px);
  box-shadow: var(--lift-shadow);
}

.doc-status {
  display: inline-flex;
  margin-bottom: 0.5rem;
  border-radius: 999px;
  background: var(--forest-soft);
  color: var(--forest-deep);
  padding: 0.24rem 0.52rem;
  font-size: 0.72rem;
  font-variant-numeric: tabular-nums;
}

.doc-item h4 {
  margin: 0 0 0.35rem;
  font-size: 1.2rem;
}

.doc-item p {
  margin: 0.25rem 0;
  color: var(--ink-soft);
  font-size: 0.86rem;
  overflow-wrap: anywhere;
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
