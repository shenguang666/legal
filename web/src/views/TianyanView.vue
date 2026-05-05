<template>
  <section class="knowledge-grid">
    <article class="card panel">
      <p class="tag">天眼审查</p>
      <h2 class="section-title">上传待审查文档</h2>
      <div class="grid form-grid">
        <div>
          <label for="tianyan-title">标题</label>
          <input id="tianyan-title" v-model="title" name="tianyanTitle" autocomplete="off" placeholder="可选，不填则使用文件名…" />
        </div>
        <div>
          <label for="tianyan-source">来源</label>
          <input id="tianyan-source" v-model="source" name="tianyanSource" autocomplete="off" placeholder="可选，例如：合同中心 / 邮件附件…" />
        </div>
        <div>
          <label for="tianyan-file">文件</label>
          <input
            id="tianyan-file"
            name="tianyanFile"
            type="file"
            accept=".pdf,.doc,.docx,.txt,.md"
            @change="onSelectFile"
          />
        </div>
        <div>
          <label for="tianyan-parse-method">解析方式</label>
          <select id="tianyan-parse-method" v-model="parseMethod" class="console-select">
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
        <button class="primary-btn" type="button" @click="importDocument">导入审查文档</button>
      </div>
      <p class="note">单次最多上传 {{ maxUploadDocuments }} 个文档。MinerU 精准解析完成后才能发起天眼审查。</p>
    </article>

    <article class="card panel">
      <div class="header-row">
        <div>
          <p class="tag">审查队列</p>
          <h3>待审查文档列表</h3>
        </div>
        <button class="ghost-btn" type="button" @click="refresh">刷新</button>
      </div>

      <div class="doc-list">
        <div v-for="item in documents" :key="item.documentId" class="doc-item">
          <span class="doc-status">{{ item.status }} / {{ item.indexStatus }} / {{ item.parseStatus || 'COMPLETED' }}</span>
          <h4>{{ item.title }}</h4>
          <p>来源：{{ item.source }}</p>
          <p>文档清洗：{{ item.cleaningEnabled ? '已启用' : '未启用' }}</p>
          <div class="doc-actions">
            <button class="ghost-btn" type="button" @click="openDetail(item)">查看详情</button>
            <button class="primary-btn" type="button" :disabled="Boolean(item.parseStatus && item.parseStatus !== 'COMPLETED')" @click="openReview(item)">进入审查</button>
            <button v-if="item.parseStatus === 'FAILED'" class="ghost-btn" type="button" @click="retryParse(item)">重试解析</button>
            <button class="warn-btn" type="button" @click="remove(item)">删除</button>
          </div>
          <p v-if="item.parseFailureReason" class="note">解析失败：{{ item.parseFailureReason }}</p>
        </div>
      </div>
      <p v-if="!documents.length" class="note">暂无待审查文档，请先上传。</p>
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
            <button class="ghost-btn" type="button" :disabled="!selectedDocument.documentUrl" @click="openDocumentAsset(selectedDocument, 'full.md')">下载 Markdown 解析结果</button>
            <button class="ghost-btn" type="button" :disabled="!selectedDocument.documentUrl" @click="openDocumentAsset(selectedDocument, 'content_list_v2.json')">下载 JSON 解析结果</button>
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
import { useRouter } from 'vue-router';
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
  bizType?: string;
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

const router = useRouter();
const title = ref('');
const source = ref('');
const selectedFile = ref<File | null>(null);
const documents = ref<DocumentItem[]>([]);
const notice = ref('');
const parseMethod = ref('NATIVE');
const parseMethods = ref<string[]>(['NATIVE']);
const maxUploadDocuments = ref(1);
const cleaningAvailable = ref(false);
const cleaningEnabled = ref(false);
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
  await refresh();
});

async function loadCapabilities() {
  const capabilities = await apiGet<DocumentProcessingCapabilities>('/api/document-processing/capabilities');
  parseMethods.value = capabilities.availableParseMethods?.length ? capabilities.availableParseMethods : ['NATIVE'];
  parseMethod.value = capabilities.defaultParseMethod || parseMethods.value[0];
  maxUploadDocuments.value = capabilities.maxUploadDocuments || 1;
  cleaningAvailable.value = Boolean(capabilities.cleaningAvailable);
  cleaningEnabled.value = false;
}

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
  openConfirm({
    title: '导入审查文档',
    message: `确认使用“${parseMethodLabel(parseMethod.value)}”导入该审查文档。`,
    targetName: selectedFile.value.name,
    targetLabel: '文件名称',
    confirmText: '确认导入',
    action: executeImportDocument,
  });
}

async function executeImportDocument() {
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
  formData.append('parseMethod', parseMethod.value);
  formData.append('cleaningEnabled', String(cleaningAvailable.value && cleaningEnabled.value));
  await apiPostForm('/api/tianyan/documents/import', formData);
  title.value = '';
  source.value = '';
  selectedFile.value = null;
  cleaningEnabled.value = false;
  notice.value = parseMethod.value === 'MINERU_PRECISE' ? '审查文档已导入，MinerU 精准解析完成后可发起天眼审查' : '审查文档已导入，可立即发起天眼审查';
  await refresh();
}

async function remove(item: DocumentItem) {
  openConfirm({
    title: '删除审查文档',
    message: '确认后会将该审查文档标记删除。',
    targetName: item.title,
    targetLabel: '文档名称',
    confirmText: '确认删除',
    danger: true,
    action: () => executeRemove(item),
  });
}

async function executeRemove(item: DocumentItem) {
  await apiDelete(`/api/tianyan/documents/${item.documentId}?requestId=${encodeURIComponent(randomRequestId('tianyan-delete'))}`);
  notice.value = `审查文档“${item.title}”已标记删除`;
  await refresh();
}

function openReview(item: DocumentItem) {
  if (item.parseStatus && item.parseStatus !== 'COMPLETED') {
    notice.value = '文档解析未完成，暂不能发起天眼审查';
    return;
  }
  router.push({
    path: `/tianyan/reviews/${item.documentId}`,
    query: {
      title: item.title,
    },
  });
}

async function retryParse(item: DocumentItem) {
  openConfirm({
    title: '重新提交解析任务',
    message: '确认后会重新提交该审查文档的解析任务。',
    targetName: item.title,
    targetLabel: '文档名称',
    confirmText: '重试解析',
    action: () => executeRetryParse(item),
  });
}

async function executeRetryParse(item: DocumentItem) {
  await apiPost(`/api/tianyan/documents/${item.documentId}/retry-parse?requestId=${encodeURIComponent(randomRequestId('tianyan-retry-parse'))}`);
  notice.value = `审查文档“${item.title}”已重新提交解析`;
  await refresh();
}

function parseMethodLabel(method: string) {
  if (method === 'MINERU_PRECISE') {
    return 'MinerU 精准解析';
  }
  return '原生解析';
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
    radial-gradient(circle at 100% 0%, rgba(167, 90, 63, 0.12), transparent 36%),
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
  background: var(--brass-soft);
  color: #855f18;
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
