<template>
  <section class="rule-page">
    <div class="top-grid">
      <article class="card panel">
        <p class="tag">风险规则管理</p>
        <h2 class="section-title">手工录入与文件导入</h2>

        <div class="section-block">
          <h3>手工录入风险规则</h3>
          <div class="grid form-grid">
            <div>
              <label>规则名称</label>
              <input v-model="manualRuleName" placeholder="例如：报销缺少发票风险" />
            </div>
            <div>
              <label>规则编码</label>
              <input v-model="manualRuleCode" placeholder="可选，不填则自动生成" />
            </div>
            <div>
              <label>来源</label>
              <input v-model="manualSource" placeholder="例如：制度专员手工录入" />
            </div>
            <div>
              <label>风险级别</label>
              <select v-model="manualSeverity" class="console-select">
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>
            <div>
              <label>命中阈值</label>
              <input v-model="manualHitThreshold" type="number" min="0" max="1" step="0.01" />
            </div>
          </div>
          <div>
            <label>规则内容</label>
            <textarea v-model="manualRuleContent" rows="6" placeholder="请输入风险规则原文，保存后会同时写入 MySQL 与 ES。"></textarea>
          </div>
          <div class="actions">
            <button class="primary-btn" type="button" @click="createManualRule">保存手工规则</button>
          </div>
        </div>

        <div class="section-block">
          <h3>文件导入风险规则</h3>
          <div class="grid form-grid">
            <div>
              <label>文档标题</label>
              <input v-model="importTitle" placeholder="可选，不填则使用文件名" />
            </div>
            <div>
              <label>规则名称</label>
              <input v-model="importRuleName" placeholder="可选，不填则使用文档标题" />
            </div>
            <div>
              <label>规则编码</label>
              <input v-model="importRuleCode" placeholder="可选，不填则自动生成" />
            </div>
            <div>
              <label>来源</label>
              <input v-model="importSource" placeholder="例如：制度库 / 扫描件上传" />
            </div>
            <div>
              <label>风险级别</label>
              <select v-model="importSeverity" class="console-select">
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>
            <div>
              <label>命中阈值</label>
              <input v-model="importHitThreshold" type="number" min="0" max="1" step="0.01" />
            </div>
            <div>
              <label>文件</label>
              <input type="file" accept=".pdf,.doc,.docx,.txt,.md" @change="onSelectFile" />
            </div>
            <div>
              <label>解析方式</label>
              <select v-model="parseMethod" class="console-select">
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
          <p class="note">单次最多上传 {{ maxUploadDocuments }} 个文档。MinerU 精准解析会在后台完成后再投递风险规则索引。</p>
          <div class="actions">
            <button class="primary-btn" type="button" @click="importRuleDocument">导入规则文件</button>
          </div>
        </div>
      </article>

      <article class="card panel">
        <div class="header-row">
          <div>
            <p class="tag">风险规则列表</p>
            <h3>已入库规则</h3>
          </div>
          <button class="ghost-btn" type="button" @click="refreshAll">刷新</button>
        </div>

        <div class="item-list">
          <div v-for="item in rules" :key="item.ruleId" class="item-card">
            <div class="item-top">
              <div>
                <h4>{{ item.ruleName }}</h4>
                <p class="note">{{ item.ruleCode }} | {{ item.ruleSourceType }} | {{ item.severity }}</p>
              </div>
              <span class="status-pill" :data-status="item.enabled ? 'ENABLED' : 'DISABLED'">{{ item.enabled ? 'ENABLED' : 'DISABLED' }}</span>
            </div>
            <p class="note">文档：{{ item.documentTitle || '-' }} / {{ item.documentStatus || '-' }} / {{ item.documentIndexStatus || '-' }} / {{ item.documentParseStatus || 'COMPLETED' }}</p>
            <p class="note">来源：{{ item.documentSource || '-' }} | 阈值：{{ item.hitThreshold ?? '-' }}</p>
            <p v-if="item.documentParseFailureReason" class="note">解析失败：{{ item.documentParseFailureReason }}</p>
            <p v-if="item.ruleContent" class="evidence">{{ item.ruleContent }}</p>
            <div class="item-actions">
              <button class="ghost-btn" type="button" :disabled="!item.documentId" @click="openDetail(item)">查看详情</button>
              <button class="ghost-btn" type="button" @click="toggleRule(item)">{{ item.enabled ? '停用' : '启用' }}</button>
              <button class="ghost-btn" type="button" :disabled="!item.documentId" @click="reindexRule(item)">重建索引</button>
              <button class="warn-btn" type="button" @click="deleteRule(item)">删除</button>
            </div>
          </div>
        </div>
        <p v-if="!rules.length" class="note">暂无风险规则，请先手工录入或上传文档。</p>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="selectedRule" class="detail-panel" role="dialog" aria-modal="true">
        <div class="detail-modal card">
          <div class="header-row">
            <div>
              <p class="tag">文档详情</p>
              <h3>{{ selectedRule.documentTitle || selectedRule.ruleName }}</h3>
            </div>
            <button class="ghost-btn" type="button" @click="closeDetail">关闭</button>
          </div>
          <div class="detail-grid">
            <p><strong>来源</strong><span>{{ selectedRule.documentSource || '-' }}</span></p>
            <p><strong>导入人</strong><span>{{ importerLabel(selectedRule) }}</span></p>
            <p><strong>导入时间</strong><span>{{ formatDate(selectedRule.documentCreatedAt || selectedRule.createdAt) }}</span></p>
            <p><strong>解析方式</strong><span>{{ parseMethodLabel(selectedRule.documentParseMethod || 'NATIVE') }}</span></p>
            <p><strong>状态</strong><span>{{ selectedRule.documentStatus || '-' }} / {{ selectedRule.documentIndexStatus || '-' }} / {{ selectedRule.documentParseStatus || 'COMPLETED' }}</span></p>
          </div>
          <div class="actions">
            <button class="primary-btn" type="button" :disabled="!selectedRule.documentUrl" @click="openDocumentAsset(selectedRule, 'origin')">查看原文档</button>
            <button class="ghost-btn" type="button" :disabled="!selectedRule.documentUrl" @click="openDocumentAsset(selectedRule, 'full.md')">下载 Markdown 解析结果</button>
            <button class="ghost-btn" type="button" :disabled="!selectedRule.documentUrl" @click="openDocumentAsset(selectedRule, 'content_list_v2.json')">下载 JSON 解析结果</button>
          </div>
          <p v-if="!selectedRule.documentUrl" class="note">当前文档暂未记录可访问原文档地址。</p>
        </div>
      </div>
    </Teleport>

    <article class="card panel">
      <div class="header-row">
        <div>
          <p class="tag">抽取字段管理</p>
          <h3>配置天眼审查字段定义</h3>
        </div>
        <button class="ghost-btn" type="button" @click="resetFieldForm">清空表单</button>
      </div>

      <div class="grid field-grid">
        <div>
          <label>字段编码</label>
          <input v-model="fieldCode" placeholder="例如：party_a" />
        </div>
        <div>
          <label>字段名称</label>
          <input v-model="fieldName" placeholder="例如：甲方" />
        </div>
        <div>
          <label>抽取器类型</label>
          <select v-model="extractorKind" class="console-select">
            <option value="PARTY_PATTERN">PARTY_PATTERN</option>
            <option value="AMOUNT_PATTERN">AMOUNT_PATTERN</option>
            <option value="DATE_KEYWORD">DATE_KEYWORD</option>
            <option value="KEYWORD_LINE">KEYWORD_LINE</option>
          </select>
        </div>
        <div>
          <label>排序</label>
          <input v-model="fieldSortOrder" type="number" min="0" step="1" />
        </div>
        <div class="field-switches selection-row">
          <label class="selection-chip selection-chip--soft" :class="{ 'selection-chip--active': fieldRepeatable }">
            <input v-model="fieldRepeatable" type="checkbox" />
            <span>多值字段</span>
          </label>
          <label class="selection-chip selection-chip--soft" :class="{ 'selection-chip--active': fieldDeduplicate }">
            <input v-model="fieldDeduplicate" type="checkbox" />
            <span>归一值去重</span>
          </label>
          <label class="selection-chip selection-chip--soft" :class="{ 'selection-chip--active': fieldEnabled }">
            <input v-model="fieldEnabled" type="checkbox" />
            <span>启用</span>
          </label>
        </div>
      </div>
      <div>
        <label>正则表达式</label>
        <textarea v-model="fieldPatternExpr" rows="3" placeholder="适用于 PARTY_PATTERN / AMOUNT_PATTERN / DATE_KEYWORD"></textarea>
      </div>
      <div>
        <label>关键字配置</label>
        <textarea v-model="fieldKeywordConfig" rows="3" placeholder="支持换行、逗号或分号分隔"></textarea>
      </div>
      <div>
        <label>说明</label>
        <textarea v-model="fieldDescription" rows="2" placeholder="说明该字段的业务用途"></textarea>
      </div>
      <div class="actions">
        <button class="primary-btn" type="button" @click="saveFieldDefinition">{{ editingFieldId ? '更新字段定义' : '新增字段定义' }}</button>
      </div>

      <div class="item-list field-list">
        <div v-for="item in fieldDefinitions" :key="item.fieldDefinitionId" class="item-card">
          <div class="item-top">
            <div>
              <h4>{{ item.fieldName }}</h4>
              <p class="note">{{ item.fieldCode }} | {{ item.extractorKind }} | sort={{ item.sortOrder }}</p>
            </div>
            <span class="status-pill" :data-status="item.enabled ? 'ENABLED' : 'DISABLED'">{{ item.enabled ? 'ENABLED' : 'DISABLED' }}</span>
          </div>
          <p class="note">repeatable={{ item.repeatable }} | deduplicate={{ item.deduplicateByNormalized }} | systemDefault={{ item.systemDefault }}</p>
          <p v-if="item.patternExpr" class="evidence">{{ item.patternExpr }}</p>
          <p v-if="item.keywordConfig" class="note">关键字：{{ item.keywordConfig }}</p>
          <p v-if="item.description" class="note">说明：{{ item.description }}</p>
          <div class="item-actions">
            <button class="ghost-btn" type="button" @click="beginEditField(item)" :disabled="item.systemDefault">编辑</button>
            <button class="ghost-btn" type="button" @click="toggleField(item)" :disabled="item.systemDefault">{{ item.enabled ? '停用' : '启用' }}</button>
            <button class="warn-btn" type="button" @click="deleteField(item)" :disabled="item.systemDefault">删除</button>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { apiDelete, apiGet, apiPost, apiPostForm, randomRequestId } from '../api/client';

interface RiskRuleItem {
  ruleId: number;
  ruleCode: string;
  ruleName: string;
  ruleType: string;
  ruleSourceType: string;
  severity: string;
  enabled: boolean;
  hitThreshold?: number | null;
  documentId?: number | null;
  documentOwnerUserId?: number | null;
  documentOwnerUsername?: string | null;
  documentTitle?: string | null;
  documentSource?: string | null;
  documentUrl?: string | null;
  documentStatus?: string | null;
  documentIndexStatus?: string | null;
  documentParseMethod?: string | null;
  documentParseStatus?: string | null;
  documentParseFailureReason?: string | null;
  ruleContent?: string | null;
  documentCreatedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

interface DocumentProcessingCapabilities {
  defaultParseMethod: string;
  maxUploadDocuments: number;
  availableParseMethods: string[];
  cleaningAvailable: boolean;
}

interface FieldDefinitionItem {
  fieldDefinitionId: number;
  tenantId: number;
  systemDefault: boolean;
  fieldCode: string;
  fieldName: string;
  extractorKind: string;
  patternExpr?: string | null;
  keywordConfig?: string | null;
  repeatable: boolean;
  deduplicateByNormalized: boolean;
  enabled: boolean;
  sortOrder: number;
  description?: string | null;
}

const rules = ref<RiskRuleItem[]>([]);
const fieldDefinitions = ref<FieldDefinitionItem[]>([]);
const selectedFile = ref<File | null>(null);
const notice = ref('');
const parseMethod = ref('NATIVE');
const parseMethods = ref<string[]>(['NATIVE']);
const maxUploadDocuments = ref(1);
const cleaningAvailable = ref(false);
const cleaningEnabled = ref(false);
const selectedRule = ref<RiskRuleItem | null>(null);

const manualRuleName = ref('');
const manualRuleCode = ref('');
const manualSource = ref('');
const manualSeverity = ref('MEDIUM');
const manualHitThreshold = ref(0.78);
const manualRuleContent = ref('');

const importTitle = ref('');
const importRuleName = ref('');
const importRuleCode = ref('');
const importSource = ref('');
const importSeverity = ref('MEDIUM');
const importHitThreshold = ref(0.78);

const editingFieldId = ref<number | null>(null);
const fieldCode = ref('');
const fieldName = ref('');
const extractorKind = ref('PARTY_PATTERN');
const fieldPatternExpr = ref('');
const fieldKeywordConfig = ref('');
const fieldRepeatable = ref(false);
const fieldDeduplicate = ref(true);
const fieldEnabled = ref(true);
const fieldSortOrder = ref(10);
const fieldDescription = ref('');

onMounted(async () => {
  await loadCapabilities();
  await refreshAll();
});

async function loadCapabilities() {
  const capabilities = await apiGet<DocumentProcessingCapabilities>('/api/document-processing/capabilities');
  parseMethods.value = capabilities.availableParseMethods?.length ? capabilities.availableParseMethods : ['NATIVE'];
  parseMethod.value = capabilities.defaultParseMethod || parseMethods.value[0];
  maxUploadDocuments.value = capabilities.maxUploadDocuments || 1;
  cleaningAvailable.value = Boolean(capabilities.cleaningAvailable);
  cleaningEnabled.value = false;
}

async function refreshAll() {
  rules.value = await apiGet<RiskRuleItem[]>('/api/risk-rules/entries');
  fieldDefinitions.value = await apiGet<FieldDefinitionItem[]>('/api/risk-rules/fields');
}

function onSelectFile(event: Event) {
  const input = event.target as HTMLInputElement;
  selectedFile.value = input.files?.[0] || null;
}

async function createManualRule() {
  if (!manualRuleName.value.trim() || !manualRuleContent.value.trim()) {
    notice.value = '请填写规则名称和规则内容';
    return;
  }
  await apiPost('/api/risk-rules/manual', {
    requestId: randomRequestId('risk-rule-manual'),
    ruleCode: manualRuleCode.value.trim() || undefined,
    ruleName: manualRuleName.value.trim(),
    source: manualSource.value.trim() || undefined,
    severity: manualSeverity.value,
    hitThreshold: Number(manualHitThreshold.value),
    ruleContent: manualRuleContent.value.trim(),
  });
  manualRuleName.value = '';
  manualRuleCode.value = '';
  manualSource.value = '';
  manualSeverity.value = 'MEDIUM';
  manualHitThreshold.value = 0.78;
  manualRuleContent.value = '';
  notice.value = '手工风险规则已写入 MySQL，并已投递 ES 索引任务';
  await refreshAll();
}

async function importRuleDocument() {
  if (!selectedFile.value) {
    notice.value = '请先选择文件';
    return;
  }
  if (maxUploadDocuments.value < 1) {
    notice.value = '当前配置不允许上传文档';
    return;
  }
  const formData = new FormData();
  formData.append('requestId', randomRequestId('risk-rule-import'));
  formData.append('file', selectedFile.value);
  if (importTitle.value.trim()) formData.append('title', importTitle.value.trim());
  if (importRuleName.value.trim()) formData.append('ruleName', importRuleName.value.trim());
  if (importRuleCode.value.trim()) formData.append('ruleCode', importRuleCode.value.trim());
  if (importSource.value.trim()) formData.append('source', importSource.value.trim());
  formData.append('severity', importSeverity.value);
  formData.append('hitThreshold', String(importHitThreshold.value));
  formData.append('parseMethod', parseMethod.value);
  formData.append('cleaningEnabled', String(cleaningAvailable.value && cleaningEnabled.value));
  await apiPostForm('/api/risk-rules/import', formData);
  importTitle.value = '';
  importRuleName.value = '';
  importRuleCode.value = '';
  importSource.value = '';
  importSeverity.value = 'MEDIUM';
  importHitThreshold.value = 0.78;
  cleaningEnabled.value = false;
  selectedFile.value = null;
  notice.value = parseMethod.value === 'MINERU_PRECISE' ? '风险规则文件已导入，MinerU 精准解析完成后会自动投递索引' : '风险规则文件已导入，规则元数据与 ES 索引已同步创建';
  await refreshAll();
}

function parseMethodLabel(method: string) {
  if (method === 'MINERU_PRECISE') {
    return 'MinerU 精准解析';
  }
  return '原生解析';
}

async function toggleRule(item: RiskRuleItem) {
  await apiPost(`/api/risk-rules/${item.ruleId}/status`, {
    requestId: randomRequestId('risk-rule-status'),
    enabled: !item.enabled,
  });
  notice.value = `风险规则 ${item.ruleCode} 已${item.enabled ? '停用' : '启用'}`;
  await refreshAll();
}

async function reindexRule(item: RiskRuleItem) {
  await apiPost(`/api/risk-rules/${item.ruleId}/reindex?requestId=${encodeURIComponent(randomRequestId('risk-rule-reindex'))}`);
  notice.value = `风险规则 ${item.ruleCode} 已提交重建索引`;
  await refreshAll();
}

async function deleteRule(item: RiskRuleItem) {
  if (!window.confirm(`确认删除风险规则 ${item.ruleCode}？`)) {
    return;
  }
  await apiDelete(`/api/risk-rules/${item.ruleId}?requestId=${encodeURIComponent(randomRequestId('risk-rule-delete'))}`);
  notice.value = `风险规则 ${item.ruleCode} 已删除`;
  await refreshAll();
}

function openDetail(item: RiskRuleItem) {
  selectedRule.value = item;
}

function closeDetail() {
  selectedRule.value = null;
}

async function openDocumentAsset(item: RiskRuleItem, fileName: string) {
  if (!item.documentUrl || !item.documentId) {
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

function importerLabel(item: RiskRuleItem) {
  return item.documentOwnerUsername || (item.documentOwnerUserId ? `用户-${item.documentOwnerUserId}` : '-');
}

function formatDate(value?: string | null) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

function resetFieldForm() {
  editingFieldId.value = null;
  fieldCode.value = '';
  fieldName.value = '';
  extractorKind.value = 'PARTY_PATTERN';
  fieldPatternExpr.value = '';
  fieldKeywordConfig.value = '';
  fieldRepeatable.value = false;
  fieldDeduplicate.value = true;
  fieldEnabled.value = true;
  fieldSortOrder.value = 10;
  fieldDescription.value = '';
}

function beginEditField(item: FieldDefinitionItem) {
  editingFieldId.value = item.fieldDefinitionId;
  fieldCode.value = item.fieldCode;
  fieldName.value = item.fieldName;
  extractorKind.value = item.extractorKind;
  fieldPatternExpr.value = item.patternExpr || '';
  fieldKeywordConfig.value = item.keywordConfig || '';
  fieldRepeatable.value = item.repeatable;
  fieldDeduplicate.value = item.deduplicateByNormalized;
  fieldEnabled.value = item.enabled;
  fieldSortOrder.value = item.sortOrder;
  fieldDescription.value = item.description || '';
}

async function saveFieldDefinition() {
  if (!fieldCode.value.trim() || !fieldName.value.trim()) {
    notice.value = '请填写字段编码和字段名称';
    return;
  }
  const payload = {
    requestId: randomRequestId('field-definition-save'),
    fieldCode: fieldCode.value.trim(),
    fieldName: fieldName.value.trim(),
    extractorKind: extractorKind.value,
    patternExpr: fieldPatternExpr.value.trim() || undefined,
    keywordConfig: fieldKeywordConfig.value.trim() || undefined,
    repeatable: fieldRepeatable.value,
    deduplicateByNormalized: fieldDeduplicate.value,
    enabled: fieldEnabled.value,
    sortOrder: Number(fieldSortOrder.value),
    description: fieldDescription.value.trim() || undefined,
  };
  if (editingFieldId.value) {
    await apiPost(`/api/risk-rules/fields/${editingFieldId.value}`, payload);
    notice.value = `字段定义 ${fieldCode.value} 已更新`;
  } else {
    await apiPost('/api/risk-rules/fields', payload);
    notice.value = `字段定义 ${fieldCode.value} 已创建`;
  }
  resetFieldForm();
  await refreshAll();
}

async function toggleField(item: FieldDefinitionItem) {
  await apiPost(`/api/risk-rules/fields/${item.fieldDefinitionId}/status`, {
    requestId: randomRequestId('field-definition-status'),
    enabled: !item.enabled,
  });
  notice.value = `字段定义 ${item.fieldCode} 已${item.enabled ? '停用' : '启用'}`;
  await refreshAll();
}

async function deleteField(item: FieldDefinitionItem) {
  if (!window.confirm(`确认删除字段定义 ${item.fieldCode}？`)) {
    return;
  }
  await apiDelete(`/api/risk-rules/fields/${item.fieldDefinitionId}?requestId=${encodeURIComponent(randomRequestId('field-definition-delete'))}`);
  notice.value = `字段定义 ${item.fieldCode} 已删除`;
  await refreshAll();
}
</script>

<style scoped>
.rule-page {
  display: grid;
  gap: 1rem;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1.05fr) 1.15fr;
  gap: 1rem;
}

.panel {
  padding: 1rem;
}

.section-block {
  display: grid;
  gap: 0.75rem;
  padding-top: 0.9rem;
  margin-top: 0.9rem;
  border-top: 1px solid var(--line);
}

.form-grid,
.field-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-switches {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  align-items: center;
}

.actions {
  margin-top: 0.2rem;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.item-list {
  margin-top: 0.8rem;
  display: grid;
  gap: 0.75rem;
}

.item-card {
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
  padding: 0.85rem;
}

.item-top {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: flex-start;
}

.item-top h4 {
  margin: 0 0 0.25rem;
  font-size: 1.08rem;
}

.item-actions {
  margin-top: 0.65rem;
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.field-list {
  margin-top: 1rem;
}

.notice {
  margin-top: 0.7rem;
  color: var(--ink-soft);
  border-left: 3px solid var(--brass);
  padding-left: 0.6rem;
}

.evidence {
  margin-top: 0.45rem;
  color: var(--ink-soft);
  font-size: 0.86rem;
  white-space: pre-wrap;
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
  .top-grid,
  .form-grid,
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
