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
        <div v-if="cleaningAvailable" class="selection-row">
          <label class="selection-chip selection-chip--soft" :class="{ 'selection-chip--active': cleaningEnabled }">
            <input v-model="cleaningEnabled" type="checkbox" />
            <span>启用文档清洗</span>
          </label>
        </div>
      </div>
      <div class="actions">
        <button class="primary-btn" type="button" :disabled="busy" @click="importDocument">导入并索引</button>
      </div>
      <p class="note">支持 pdf/doc/docx/txt/md，单次最多上传 {{ maxUploadDocuments }} 个文档，导入后由后台异步完成解析与向量索引。</p>
    </article>

    <article class="card panel">
      <p class="tag">智能问答配置</p>
      <h3>知识库检索索引范围</h3>
      <div class="grid form-grid">
        <div>
          <label for="qa-index-scope">问答检索范围</label>
          <select id="qa-index-scope" v-model="qaIndexScope" class="console-select">
            <option v-for="scope in qaIndexScopes" :key="scope" :value="scope">{{ qaIndexScopeLabel(scope) }}</option>
          </select>
        </div>
      </div>
      <p class="note">原生索引：{{ nativeIndexName || '-' }}；MinerU 索引：{{ mineruIndexName || '-' }}</p>
      <div class="actions">
        <button class="primary-btn" type="button" :disabled="busy" @click="saveQaIndexConfig">保存问答检索范围</button>
      </div>
    </article>

    <article class="card panel">
      <div class="header-row">
        <div>
          <p class="tag">知识资产</p>
          <h3>文档列表</h3>
        </div>
        <button class="ghost-btn" type="button" :disabled="busy" @click="refresh">刷新</button>
      </div>
      <div class="filter-row">
        <label for="knowledge-list-parse-method">解析类型</label>
        <select id="knowledge-list-parse-method" v-model="documentParseMethodFilter" class="console-select" @change="refresh">
          <option value="ALL">全部解析类型</option>
          <option v-for="method in parseMethods" :key="method" :value="method">{{ parseMethodLabel(method) }}</option>
        </select>
      </div>

      <div class="doc-list">
        <div v-for="item in documents" :key="item.documentId" class="doc-item">
          <span class="doc-status">{{ item.status }} / {{ item.indexStatus }} / {{ item.parseStatus || 'COMPLETED' }}</span>
          <h4>{{ item.title }}</h4>
          <p>来源：{{ item.source }}</p>
          <p>解析方式：{{ parseMethodLabel(item.parseMethod || 'NATIVE') }}</p>
          <p>文档清洗：{{ item.cleaningEnabled ? '已启用' : '未启用' }}</p>
          <div class="doc-actions">
            <button class="ghost-btn" type="button" :disabled="busy" @click="openDetail(item)">查看详情</button>
            <button class="ghost-btn" type="button" :disabled="busy" @click="triggerIndex(item)">触发索引</button>
            <button v-if="item.parseStatus === 'FAILED'" class="ghost-btn" type="button" :disabled="busy" @click="retryParse(item)">重试解析</button>
            <button class="warn-btn" type="button" :disabled="busy" @click="remove(item)">删除</button>
          </div>
          <p v-if="item.parseFailureReason" class="note">解析失败：{{ item.parseFailureReason }}</p>
        </div>
      </div>
      <p v-if="!documents.length" class="note">暂无文档，请先创建一条记录。</p>
      <p v-if="notice" class="notice" aria-live="polite">{{ notice }}</p>
    </article>

    <Teleport to="body">
      <div v-if="selectedDocument" class="detail-panel" role="dialog" aria-modal="true">
        <div class="detail-modal card">
          <div class="header-row">
            <div>
              <p class="tag">文档详情</p>
              <h3>{{ selectedDocument.title }}</h3>
            </div>
            <button class="ghost-btn" type="button" @click="closeDetail">关闭</button>
          </div>
          <div class="detail-grid">
            <p><strong>来源</strong><span>{{ selectedDocument.source }}</span></p>
            <p><strong>导入人</strong><span>{{ importerLabel(selectedDocument) }}</span></p>
            <p><strong>导入时间</strong><span>{{ formatDate(selectedDocument.createdAt) }}</span></p>
            <p><strong>解析方式</strong><span>{{ parseMethodLabel(selectedDocument.parseMethod || 'NATIVE') }}</span></p>
            <p><strong>状态</strong><span>{{ selectedDocument.status }} / {{ selectedDocument.indexStatus }} / {{ selectedDocument.parseStatus || 'COMPLETED' }}</span></p>
          </div>
          <div class="actions">
            <button class="primary-btn" type="button" :disabled="!selectedDocument.documentUrl" @click="openDocumentAsset(selectedDocument, 'origin')">下载原文档</button>
            <button v-if="selectedDocument.parseMethod !== 'NATIVE'" class="ghost-btn" type="button" :disabled="!selectedDocument.documentUrl" @click="openDocumentAsset(selectedDocument, 'full.md')">下载 Markdown 解析结果</button>
            <button v-if="selectedDocument.parseMethod !== 'NATIVE'" class="ghost-btn" type="button" :disabled="!selectedDocument.documentUrl" @click="openDocumentAsset(selectedDocument, 'content_list_v2.json')">下载 JSON 解析结果</button>
          </div>
          <p v-if="!selectedDocument.documentUrl" class="note">当前文档暂未记录可访问原文档地址。</p>
        </div>
      </div>
    </Teleport>

    <ConfirmDialog
      :model-value="confirmState.visible"
      :title="confirmState.title"
      :message="confirmState.message"
      :target-name="confirmState.targetName"
      :target-label="confirmState.targetLabel"
      :confirm-text="confirmState.confirmText"
      :eyebrow="confirmState.eyebrow"
      :danger="confirmState.danger"
      @cancel="closeConfirm"
      @confirm="confirmAction"
    />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { apiDelete, apiGet, apiPost, apiPostForm, randomRequestId } from '../api/client';
import ConfirmDialog from '../components/ConfirmDialog.vue';

interface DocumentItem {
  documentId: number;
  ownerUserId?: number;
  ownerUsername?: string | null;
  title: string;
  source: string;
  status: string;
  indexStatus: string;
  parseMethod?: string;
  parseStatus?: string;
  parseFailureReason?: string;
  cleaningEnabled?: boolean;
  documentUrl?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

interface DocumentProcessingCapabilities {
  defaultParseMethod: string;
  maxUploadDocuments: number;
  availableParseMethods: string[];
  cleaningAvailable: boolean;
}

interface QaIndexConfig {
  indexScope: string;
  availableScopes: string[];
  nativeIndexName: string;
  mineruIndexName: string;
}

interface ConfirmState {
  visible: boolean;
  title: string;
  message: string;
  targetName: string;
  targetLabel: string;
  confirmText: string;
  eyebrow: string;
  danger: boolean;
  action: null | (() => Promise<void>);
}

const title = ref('');
const source = ref('');
const selectedFiles = ref<File[]>([]);
const documents = ref<DocumentItem[]>([]);
const notice = ref('');
const parseMethod = ref('MINERU_PRECISE');
const parseMethods = ref<string[]>(['NATIVE']);
const documentParseMethodFilter = ref('ALL');
const maxUploadDocuments = ref(1);
const cleaningAvailable = ref(false);
const cleaningEnabled = ref(false);
const busy = ref(false);
const qaIndexScope = ref('NATIVE_ONLY');
const qaIndexScopes = ref<string[]>(['NATIVE_ONLY', 'MINERU_ONLY', 'BOTH']);
const nativeIndexName = ref('');
const mineruIndexName = ref('');
const selectedDocument = ref<DocumentItem | null>(null);
const confirmState = ref<ConfirmState>({
  visible: false,
  title: '',
  message: '',
  targetName: '',
  targetLabel: '对象',
  confirmText: '确认',
  eyebrow: '操作确认',
  danger: false,
  action: null,
});

onMounted(async () => {
  await loadCapabilities();
  await loadQaIndexConfig();
  await refresh();
});

async function loadCapabilities() {
  const capabilities = await apiGet<DocumentProcessingCapabilities>('/api/document-processing/capabilities');
  parseMethods.value = orderedParseMethods(capabilities.availableParseMethods?.length ? capabilities.availableParseMethods : ['NATIVE']);
  parseMethod.value = preferredParseMethod(parseMethods.value, capabilities.defaultParseMethod);
  maxUploadDocuments.value = capabilities.maxUploadDocuments || 1;
  cleaningAvailable.value = Boolean(capabilities.cleaningAvailable);
  cleaningEnabled.value = false;
}

function orderedParseMethods(methods: string[]) {
  return [...methods].sort((left, right) => {
    if (left === 'MINERU_PRECISE') {
      return -1;
    }
    if (right === 'MINERU_PRECISE') {
      return 1;
    }
    return 0;
  });
}

function preferredParseMethod(methods: string[], defaultMethod?: string) {
  if (methods.includes('MINERU_PRECISE')) {
    return 'MINERU_PRECISE';
  }
  if (defaultMethod && methods.includes(defaultMethod)) {
    return defaultMethod;
  }
  return methods[0] || 'NATIVE';
}

async function refresh() {
  const query = documentParseMethodFilter.value && documentParseMethodFilter.value !== 'ALL'
    ? `?parseMethod=${encodeURIComponent(documentParseMethodFilter.value)}`
    : '';
  documents.value = await apiGet<DocumentItem[]>(`/api/knowledge/documents${query}`);
}

async function loadQaIndexConfig() {
  const config = await apiGet<QaIndexConfig>('/api/knowledge/admin/qa-index-config');
  qaIndexScope.value = config.indexScope || 'NATIVE_ONLY';
  qaIndexScopes.value = config.availableScopes?.length ? config.availableScopes : ['NATIVE_ONLY', 'MINERU_ONLY', 'BOTH'];
  nativeIndexName.value = config.nativeIndexName || '';
  mineruIndexName.value = config.mineruIndexName || '';
}

async function saveQaIndexConfig() {
  openConfirm({
    title: '切换问答检索范围',
    message: '确认后，智能问答会按新的知识库索引范围进行检索。',
    targetName: qaIndexScopeLabel(qaIndexScope.value),
    targetLabel: '检索范围',
    confirmText: '确认切换',
    action: executeSaveQaIndexConfig,
  });
}

async function executeSaveQaIndexConfig() {
  busy.value = true;
  notice.value = '正在保存智能问答检索范围…';
  try {
    await apiPost<QaIndexConfig>('/api/knowledge/admin/qa-index-config', { indexScope: qaIndexScope.value });
    notice.value = `智能问答知识库检索范围已切换为：${qaIndexScopeLabel(qaIndexScope.value)}`;
    await loadQaIndexConfig();
  } finally {
    busy.value = false;
  }
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
  const methodLabel = parseMethodLabel(parseMethod.value);
  openConfirm({
    title: '导入知识库文档',
    message: `确认使用“${methodLabel}”导入 ${selectedFiles.value.length} 个文档。`,
    targetName: selectedFiles.value.map((file) => file.name).join('、'),
    targetLabel: '文档名称',
    confirmText: '确认导入',
    action: executeImportDocument,
  });
}

async function executeImportDocument() {
  busy.value = true;
  notice.value = `正在导入 ${selectedFiles.value.length} 个文档…`;
  try {
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
      formData.append('cleaningEnabled', String(cleaningAvailable.value && cleaningEnabled.value));
      await apiPostForm('/api/knowledge/documents/import', formData);
    }
    title.value = '';
    source.value = '';
    selectedFiles.value = [];
    cleaningEnabled.value = false;
    notice.value = parseMethod.value === 'MINERU_PRECISE' ? '文档已导入，正在后台进行 MinerU 精准解析' : '文档已导入，正在后台建立向量索引';
    await refresh();
  } finally {
    busy.value = false;
  }
}

async function triggerIndex(item: DocumentItem) {
  openConfirm({
    title: '重新触发向量索引',
    message: '确认后会重新投递该文档的向量索引任务。',
    targetName: item.title,
    targetLabel: '文档名称',
    confirmText: '触发索引',
    action: () => executeTriggerIndex(item),
  });
}

async function executeTriggerIndex(item: DocumentItem) {
  busy.value = true;
  notice.value = `正在投递文档“${item.title}”的索引任务…`;
  try {
    await apiPost(`/api/knowledge/documents/${item.documentId}/index?requestId=${encodeURIComponent(randomRequestId('kb-index'))}`);
    notice.value = `文档“${item.title}”已投递索引任务`;
    await refresh();
  } finally {
    busy.value = false;
  }
}

async function retryParse(item: DocumentItem) {
  openConfirm({
    title: '重新提交解析任务',
    message: '确认后会重新提交该文档的解析任务。',
    targetName: item.title,
    targetLabel: '文档名称',
    confirmText: '重试解析',
    action: () => executeRetryParse(item),
  });
}

async function executeRetryParse(item: DocumentItem) {
  busy.value = true;
  notice.value = `正在重新提交文档“${item.title}”的解析任务…`;
  try {
    await apiPost(`/api/knowledge/documents/${item.documentId}/retry-parse?requestId=${encodeURIComponent(randomRequestId('kb-retry-parse'))}`);
    notice.value = `文档“${item.title}”已重新提交解析`;
    await refresh();
  } finally {
    busy.value = false;
  }
}

function parseMethodLabel(method: string) {
  if (method === 'MINERU_PRECISE') {
    return 'MinerU 精准解析';
  }
  return '原生解析';
}

function qaIndexScopeLabel(scope: string) {
  if (scope === 'MINERU_ONLY') {
    return '仅查询 MinerU 精准解析索引';
  }
  if (scope === 'BOTH') {
    return '同时查询原生索引和 MinerU 索引';
  }
  return '仅查询原生解析索引';
}

async function remove(item: DocumentItem) {
  openConfirm({
    title: '删除知识库文档',
    message: '确认后会删除该文档，并同步清理数据库切片和 Elasticsearch 切片。',
    targetName: item.title,
    targetLabel: '文档名称',
    confirmText: '确认删除',
    danger: true,
    action: () => executeRemove(item),
  });
}

async function executeRemove(item: DocumentItem) {
  busy.value = true;
  notice.value = `正在删除文档“${item.title}”及其切片…`;
  try {
    await apiDelete(`/api/knowledge/documents/${item.documentId}?requestId=${encodeURIComponent(randomRequestId('kb-delete'))}`);
    notice.value = `文档“${item.title}”已删除，数据库切片和 Elasticsearch 切片已同步清理`;
    await refresh();
  } finally {
    busy.value = false;
  }
}

function openDetail(item: DocumentItem) {
  selectedDocument.value = item;
}

function closeDetail() {
  selectedDocument.value = null;
}

async function openDocumentAsset(item: DocumentItem, fileName: string) {
  if (!item.documentUrl) {
    notice.value = '当前文档暂未记录可访问原文档地址';
    return;
  }
  try {
    const signedUrl = await apiGet<string>(`/api/document-assets/${item.documentId}/${encodeURIComponent(fileName)}`);
    window.open(signedUrl, '_blank', 'noopener,noreferrer');
  } catch (err: any) {
    notice.value = err?.message || '获取文档下载地址失败';
  }
}

function importerLabel(item: DocumentItem) {
  return item.ownerUsername || (item.ownerUserId ? `用户-${item.ownerUserId}` : '-');
}

function formatDate(value?: string) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

function openConfirm(options: Partial<ConfirmState> & { action: () => Promise<void> }) {
  confirmState.value = {
    visible: true,
    title: options.title || '操作确认',
    message: options.message || '请确认是否继续执行该操作。',
    targetName: options.targetName || '',
    targetLabel: options.targetLabel || '对象',
    confirmText: options.confirmText || '确认',
    eyebrow: options.eyebrow || '操作确认',
    danger: Boolean(options.danger),
    action: options.action,
  };
}

function closeConfirm() {
  confirmState.value.visible = false;
  confirmState.value.action = null;
}

async function confirmAction() {
  const action = confirmState.value.action;
  closeConfirm();
  if (action) {
    await action();
  }
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
  flex-wrap: wrap;
}

.notice {
  margin-top: 0.7rem;
  color: var(--ink-soft);
  border-left: 3px solid var(--brass);
  padding-left: 0.6rem;
}

.detail-panel {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgba(19, 28, 23, 0.46);
  backdrop-filter: blur(6px);
}

.detail-modal {
  width: min(560px, 100%);
  padding: 1rem;
}

.detail-grid {
  display: grid;
  gap: 0.55rem;
  margin-top: 0.8rem;
}

.detail-grid p {
  display: grid;
  grid-template-columns: 5.5rem 1fr;
  gap: 0.75rem;
  margin: 0;
  color: var(--ink-soft);
  overflow-wrap: anywhere;
}

@media (max-width: 980px) {
  .knowledge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
