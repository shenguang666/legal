<template>
  <section class="review-page">
    <article class="card panel hero">
      <div>
        <p class="tag">天眼审查</p>
        <h2 class="section-title">{{ pageTitle }}</h2>
        <p class="note">对审查文档中的核心字段、金额、日期、责任条款与风险点进行结构化识别和规则校验。</p>
      </div>
      <div class="hero-actions">
        <button class="ghost-btn" @click="goBack">返回天眼审查</button>
        <button class="ghost-btn" @click="refresh">刷新</button>
        <button class="primary-btn" @click="startReview">开始审查</button>
        <button v-if="review" class="warn-btn" @click="rerunReview">重新审查</button>
      </div>
    </article>

    <article class="card panel status-panel">
      <div class="status-header">
        <div>
          <h3>当前审查状态</h3>
          <p class="note">文档 ID：{{ documentId }} <span v-if="review">| 版本：{{ review.docVersion }}</span></p>
        </div>
        <span class="status-pill" :data-status="review?.status || 'EMPTY'">{{ review?.status || 'NOT_STARTED' }}</span>
      </div>
      <p v-if="notice" class="notice">{{ notice }}</p>
      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <p v-if="review?.summaryText" class="summary-text">{{ review.summaryText }}</p>
      <p v-if="review?.failureReason" class="error-text">失败原因：{{ review.failureReason }}</p>

      <div class="summary-grid" v-if="review">
        <div class="metric-card">
          <span>风险等级</span>
          <strong>{{ review.riskLevel || 'LOW' }}</strong>
        </div>
        <div class="metric-card">
          <span>风险数量</span>
          <strong>{{ review.riskCount ?? 0 }}</strong>
        </div>
        <div class="metric-card">
          <span>命中规则</span>
          <strong>{{ review.hitRuleCount ?? 0 }}</strong>
        </div>
        <div class="metric-card">
          <span>字段覆盖</span>
          <strong>{{ review.extractedFieldCount ?? 0 }}/{{ review.totalFieldCount ?? 0 }}</strong>
        </div>
      </div>
    </article>

    <article v-if="review" class="card panel filter-panel">
      <div class="filter-layout">
        <div class="filter-group">
          <span class="filter-label">查看范围</span>
          <div class="selection-row">
            <label class="selection-chip" :class="{ 'selection-chip--active': panelMode === 'all' }">
              <input v-model="panelMode" type="radio" name="review-panel-mode" value="all" />
              <span>全部结果</span>
            </label>
            <label class="selection-chip" :class="{ 'selection-chip--active': panelMode === 'fields' }">
              <input v-model="panelMode" type="radio" name="review-panel-mode" value="fields" />
              <span>仅看字段</span>
            </label>
            <label class="selection-chip" :class="{ 'selection-chip--active': panelMode === 'risks' }">
              <input v-model="panelMode" type="radio" name="review-panel-mode" value="risks" />
              <span>仅看风险</span>
            </label>
          </div>
        </div>
        <div class="filter-group">
          <span class="filter-label">聚焦重点</span>
          <div class="selection-row">
            <label class="selection-chip selection-chip--soft" :class="{ 'selection-chip--active': showMissingOnly }">
              <input v-model="showMissingOnly" type="checkbox" />
              <span>仅看缺失字段</span>
            </label>
            <label class="selection-chip selection-chip--soft" :class="{ 'selection-chip--active': showHitsOnly }">
              <input v-model="showHitsOnly" type="checkbox" />
              <span>仅看命中风险</span>
            </label>
          </div>
        </div>
      </div>
    </article>

    <div class="review-grid" :class="{ 'single-column': panelMode !== 'all' }">
      <article v-if="displayFieldsPanel" class="card panel">
        <div class="header-row">
          <h3>抽取字段</h3>
          <span class="note">{{ filteredFields.length }}/{{ review?.fields?.length || 0 }} 项</span>
        </div>
        <div v-if="filteredFields.length" class="field-list">
          <div v-for="field in filteredFields" :key="field.fieldId ?? `${field.fieldCode}-${field.fieldOrder}`" class="field-item">
            <div class="field-top">
              <strong>{{ field.fieldName }}</strong>
              <span class="status-pill" :data-status="field.status">{{ field.status }}</span>
            </div>
            <p class="field-value">{{ field.normalizedValue || field.rawValue || '未识别' }}</p>
            <p class="note">字段编码：{{ field.fieldCode }} | 置信度：{{ formatConfidence(field.confidence) }}</p>
            <p v-if="field.explanation" class="note">说明：{{ field.explanation }}</p>
            <p v-if="field.evidenceText" class="evidence">证据：{{ field.evidenceText }}</p>
            <p v-if="field.sourceChunkRef" class="note">来源：{{ field.sourceChunkRef }} / {{ field.extractorType }}</p>
          </div>
        </div>
        <p v-else class="note">暂无审查字段，点击“开始审查”后查看结果。</p>
      </article>

      <article v-if="displayRisksPanel" class="card panel">
        <div class="header-row">
          <h3>规则命中与风险项</h3>
          <span class="note">{{ filteredRiskItems.length }}/{{ review?.riskItems?.length || 0 }} 项</span>
        </div>
        <div v-if="filteredRiskItems.length" class="risk-list">
          <div v-for="item in filteredRiskItems" :key="item.riskId ?? item.ruleCode" class="risk-item">
            <div class="field-top">
              <strong>{{ item.ruleName }}</strong>
              <span class="status-pill" :data-status="item.executionStatus">{{ item.executionStatus }}</span>
            </div>
            <p class="field-value">{{ item.message }}</p>
            <p class="note">规则：{{ item.ruleCode }} / {{ item.ruleType }} / {{ item.severity }}</p>
            <p v-if="item.affectedFieldCodes" class="note">关联字段：{{ item.affectedFieldCodes }}</p>
            <p v-if="item.evidenceText" class="evidence">证据：{{ item.evidenceText }}</p>
          </div>
        </div>
        <p v-else class="note">暂无规则结果。</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { apiGet, apiPost, randomRequestId } from '../api/client';

interface ContractReviewFieldItem {
  fieldId?: number;
  fieldCode: string;
  fieldName: string;
  rawValue?: string | null;
  normalizedValue?: string | null;
  status?: string | null;
  confidence?: number | null;
  evidenceText?: string | null;
  sourceChunkRef?: string | null;
  extractorType?: string | null;
  fieldOrder?: number | null;
  groupKey?: string | null;
  explanation?: string | null;
}

interface ContractRiskItem {
  riskId?: number;
  ruleCode: string;
  ruleName: string;
  ruleType?: string | null;
  severity?: string | null;
  executionStatus?: string | null;
  message: string;
  evidenceText?: string | null;
  affectedFieldCodes?: string | null;
}

interface ContractReviewDetail {
  reviewId: number;
  documentId: number;
  docVersion: number;
  status?: string | null;
  riskLevel?: string | null;
  riskCount?: number | null;
  hitRuleCount?: number | null;
  totalFieldCount?: number | null;
  extractedFieldCount?: number | null;
  missingFieldCount?: number | null;
  summaryText?: string | null;
  failureReason?: string | null;
  fields: ContractReviewFieldItem[];
  riskItems: ContractRiskItem[];
}

const route = useRoute();
const router = useRouter();
const review = ref<ContractReviewDetail | null>(null);
const notice = ref('');
const errorMessage = ref('');
let timer: number | null = null;

const panelMode = ref<'all' | 'fields' | 'risks'>('all');
const showMissingOnly = ref(false);
const showHitsOnly = ref(false);

const documentId = computed(() => Number(route.params.documentId || 0));
const pageTitle = computed(() => {
  const title = route.query.title;
  return typeof title === 'string' && title.trim() ? title : `文档 #${documentId.value}`;
});
const filteredFields = computed(() => {
  const items = review.value?.fields ?? [];
  return showMissingOnly.value ? items.filter(field => field.status === 'MISSING') : items;
});
const filteredRiskItems = computed(() => {
  const items = review.value?.riskItems ?? [];
  return showHitsOnly.value ? items.filter(item => item.executionStatus === 'HIT') : items;
});
const displayFieldsPanel = computed(() => panelMode.value !== 'risks');
const displayRisksPanel = computed(() => panelMode.value !== 'fields');

onMounted(() => {
  refresh();
});

watch(() => review.value?.status, () => {
  setupPolling();
});

watch(() => documentId.value, () => {
  refresh();
});

onBeforeUnmount(() => {
  clearPolling();
});

async function refresh() {
  errorMessage.value = '';
  if (!documentId.value) {
    return;
  }
  try {
    review.value = await apiGet<ContractReviewDetail | null>(`/api/tianyan/reviews/by-document/${documentId.value}`);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : String(error);
  }
}

async function startReview() {
  errorMessage.value = '';
  notice.value = '';
  try {
    review.value = await apiPost<ContractReviewDetail>('/api/tianyan/reviews', {
      requestId: randomRequestId('tianyan-review'),
      documentId: documentId.value,
    });
    notice.value = '天眼审查任务已提交，后台正在分析文档';
    setupPolling();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : String(error);
  }
}

async function rerunReview() {
  if (!review.value) {
    return;
  }
  errorMessage.value = '';
  notice.value = '';
  try {
    review.value = await apiPost<ContractReviewDetail>(`/api/tianyan/reviews/${review.value.reviewId}/rerun?requestId=${encodeURIComponent(randomRequestId('tianyan-rerun'))}`);
    notice.value = '已重新提交天眼审查任务';
    setupPolling();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : String(error);
  }
}

function setupPolling() {
  clearPolling();
  const status = review.value?.status;
  if (status === 'PENDING' || status === 'PROCESSING') {
    timer = window.setInterval(() => {
      refresh();
    }, 3000);
  }
}

function clearPolling() {
  if (timer) {
    window.clearInterval(timer);
    timer = null;
  }
}

function formatConfidence(value?: number | null) {
  if (value === null || value === undefined) {
    return '-';
  }
  return `${(value * 100).toFixed(1)}%`;
}

function goBack() {
  router.push('/tianyan');
}
</script>

<style scoped>
.review-page {
  display: grid;
  gap: 1rem;
}

.panel {
  padding: 1rem;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem;
}

.status-panel {
  display: grid;
  gap: 0.75rem;
}

.filter-panel {
  padding: 0.95rem 1rem;
}

.status-header,
.header-row,
.field-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.summary-grid,
.review-grid {
  display: grid;
  gap: 1rem;
}

.filter-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.filter-group {
  display: grid;
  gap: 0.55rem;
}

.filter-label {
  font-size: 0.8rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.review-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.review-grid.single-column {
  grid-template-columns: 1fr;
}

.metric-card,
.field-item,
.risk-item {
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.7);
  padding: 0.85rem;
}

.field-list,
.risk-list {
  display: grid;
  gap: 0.75rem;
}

.field-value,
.summary-text {
  margin: 0.35rem 0;
  color: var(--ink);
  font-weight: 600;
}

.evidence {
  margin: 0.35rem 0 0;
  color: var(--ink-soft);
  font-size: 0.86rem;
}

.notice,
.error-text {
  margin: 0;
  padding-left: 0.6rem;
  border-left: 3px solid var(--brass);
}

.error-text {
  border-left-color: #c45f5f;
  color: #8f2b2b;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.25rem 0.6rem;
  border: 1px solid var(--line);
  font-size: 0.78rem;
  background: rgba(255, 255, 255, 0.72);
}

.status-pill[data-status='HIT'],
.status-pill[data-status='FAILED'],
.status-pill[data-status='HIGH'] {
  background: rgba(196, 95, 95, 0.14);
  color: #8f2b2b;
}

.status-pill[data-status='PROCESSING'],
.status-pill[data-status='PENDING'],
.status-pill[data-status='MEDIUM'] {
  background: rgba(192, 147, 71, 0.14);
  color: #855f18;
}

.status-pill[data-status='PASSED'],
.status-pill[data-status='COMPLETED'],
.status-pill[data-status='LOW'] {
  background: rgba(50, 92, 73, 0.14);
  color: #325c49;
}

@media (max-width: 1100px) {
  .filter-layout,
  .summary-grid,
  .review-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
