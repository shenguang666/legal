<template>
  <section class="rag-metrics-page">
    <article class="metric-hero card">
      <div>
        <p class="tag">RAG QUALITY</p>
        <h2 class="section-title">召回率与精确率统计</h2>
        <p class="note">按日期范围查询日汇总结果，底部分页展示日汇总；点击某日汇总可查看该日期内每条消息的 Recall / Precision。</p>
      </div>
      <div class="filter-row">
        <div>
          <label>开始日期</label>
          <input v-model="startDate" type="date" />
        </div>
        <div>
          <label>结束日期</label>
          <input v-model="endDate" type="date" />
        </div>
        <button class="primary-btn" @click="loadAll">查询统计</button>
        <button class="ghost-btn" @click="runToday">手动评估结束日期</button>
      </div>
    </article>

    <div class="metric-grid">
      <article class="metric-card card">
        <p>平均召回率 Recall</p>
        <strong>{{ percent(summary?.averageRecall) }}</strong>
        <span>TP / (TP + FN)</span>
      </article>
      <article class="metric-card card">
        <p>平均精确率 Precision</p>
        <strong>{{ percent(summary?.averagePrecision) }}</strong>
        <span>TP / (TP + FP)</span>
      </article>
      <article class="metric-card card">
        <p>RAG 消息 / 有效消息</p>
        <strong>{{ summary?.evaluatedCount ?? 0 }}</strong>
        <span>有效 {{ summary?.validMessageCount ?? 0 }}</span>
      </article>
      <article class="metric-card card">
        <p>成功 / 失败 / 过滤</p>
        <strong>{{ summary?.successCount ?? 0 }}</strong>
        <span>失败 {{ summary?.failedCount ?? 0 }} | 过滤 {{ summary?.filteredCount ?? 0 }}</span>
      </article>
    </div>

    <article class="card panel trend-panel">
      <div class="header-row">
        <div>
          <p class="tag">趋势</p>
          <h3>日期维度趋势</h3>
        </div>
        <span v-if="notice" class="notice">{{ notice }}</span>
      </div>
      <div v-if="summary?.trends?.length" ref="trendChartRef" class="trend-chart"></div>
      <p v-else class="note">当前日期范围暂无评估趋势。</p>
    </article>

    <article class="card panel">
      <div class="header-row">
        <div>
          <p class="tag">{{ viewMode === 'summary' ? '日汇总' : '消息明细' }}</p>
          <h3>{{ viewMode === 'summary' ? '日汇总结果分页' : `${selectedDailySummary?.metricDate || ''} 消息级评估结果` }}</h3>
        </div>
        <div class="header-actions">
          <span v-if="viewMode === 'summary'" class="note">点击某条日汇总进入详情</span>
          <button v-if="viewMode === 'detail'" class="ghost-btn" @click="backToSummaries">返回日汇总</button>
          <button class="ghost-btn" @click="refreshCurrentPanel">{{ viewMode === 'summary' ? '刷新日汇总' : '刷新明细' }}</button>
        </div>
      </div>

      <template v-if="viewMode === 'summary'">
        <div class="summary-table">
          <button
            v-for="item in dailySummaries"
            :key="item.id"
            class="summary-row"
            @click="openDailySummary(item)"
          >
            <span>{{ item.metricDate }}</span>
            <span>{{ item.status }}</span>
            <span>R {{ percent(item.averageRecall) }}</span>
            <span>P {{ percent(item.averagePrecision) }}</span>
            <span>RAG {{ item.totalRagMessageCount }}</span>
            <span>有效 {{ item.validMessageCount }}</span>
            <span>过滤 {{ item.filteredMessageCount }}</span>
            <span>失败 {{ item.failedCount }}</span>
          </button>
          <p v-if="!dailySummaries.length" class="note">当前日期范围暂无日汇总结果。</p>
        </div>
        <div class="pager">
          <button class="ghost-btn" :disabled="summaryPageNo <= 1" @click="prevSummaryPage">上一页</button>
          <span>第 {{ summaryPageNo }} 页 / 共 {{ summaryTotal }} 条</span>
          <button class="ghost-btn" :disabled="summaryPageNo * summaryPageSize >= summaryTotal" @click="nextSummaryPage">下一页</button>
        </div>
      </template>

      <template v-else>
        <div class="detail-list">
          <div v-for="item in details" :key="item.id" class="detail-card">
            <div class="item-top">
              <div>
                <h4>{{ item.queryText }}</h4>
                <p class="note">summary#{{ item.dailySummaryId || '-' }} | log#{{ item.retrievalLogId }} | {{ item.status }} | {{ formatTime(item.evaluatedAt) }}</p>
              </div>
              <span class="status-pill" :data-status="item.status">{{ item.status }}</span>
            </div>
            <div class="chip-row">
              <span>TP {{ item.truePositive ?? 0 }}</span>
              <span>FN {{ item.falseNegative ?? 0 }}</span>
              <span>FP {{ item.falsePositive ?? 0 }}</span>
              <span>Recall {{ percent(item.recallScore) }}</span>
              <span>Precision {{ percent(item.precisionScore) }}</span>
            </div>
            <p class="note">原始命中：{{ ids(item.originalHitChunkIds) }}</p>
            <p class="note">相关命中：{{ ids(item.relevantOriginalChunkIds) }}</p>
            <p class="note">不相关命中：{{ ids(item.irrelevantOriginalChunkIds) }}</p>
            <p class="note">漏召回：{{ ids(item.missedRelevantChunkIds) }}</p>
            <p v-if="item.explanation" class="evidence">{{ item.explanation }}</p>
            <p v-if="item.errorMessage" class="error-text">{{ item.errorMessage }}</p>
          </div>
          <p v-if="!details.length" class="note">暂无消息级评估明细。</p>
        </div>
        <div class="pager">
          <button class="ghost-btn" :disabled="detailPageNo <= 1" @click="prevDetailPage">上一页</button>
          <span>第 {{ detailPageNo }} 页 / 共 {{ detailTotal }} 条</span>
          <button class="ghost-btn" :disabled="detailPageNo * detailPageSize >= detailTotal" @click="nextDetailPage">下一页</button>
        </div>
      </template>
    </article>
  </section>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { apiGet, apiPost } from '../api/client';

interface TrendPoint {
  date: string;
  averageRecall: number;
  averagePrecision: number;
  totalRagMessageCount: number;
  validMessageCount: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  filteredCount: number;
  status: string;
  dailySummaryId: number;
}

interface DailySummary {
  id: number;
  metricDate: string;
  evaluationVersion: string;
  totalRagMessageCount: number;
  validMessageCount: number;
  filteredMessageCount: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  averageRecall: number;
  averagePrecision: number;
  candidateTopN: number;
  status: string;
  errorMessage: string;
  startedAt: string;
  completedAt: string;
}

interface Summary {
  averageRecall: number;
  averagePrecision: number;
  evaluatedCount: number;
  validMessageCount: number;
  filteredCount: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  candidateTopN: number;
  evaluationVersion: string;
  trends: TrendPoint[];
  dailySummaries: DailySummary[];
}

interface Detail {
  id: number;
  dailySummaryId: number;
  retrievalLogId: number;
  queryText: string;
  originalHitChunkIds: number[];
  relevantOriginalChunkIds: number[];
  irrelevantOriginalChunkIds: number[];
  missedRelevantChunkIds: number[];
  truePositive: number;
  falseNegative: number;
  falsePositive: number;
  recallScore: number;
  precisionScore: number;
  evaluatedCandidateCount: number;
  evaluatedTopN: number;
  modelName: string;
  promptVersion: string;
  explanation: string;
  status: string;
  errorMessage: string;
  evaluatedAt: string;
}

interface DetailPage {
  total: number;
  pageNo: number;
  pageSize: number;
  items: Detail[];
}

interface DailySummaryPage {
  total: number;
  pageNo: number;
  pageSize: number;
  items: DailySummary[];
}

const today = new Date();
const start = new Date(today);
start.setDate(today.getDate() - 7);

const startDate = ref(toDateInput(start));
const endDate = ref(toDateInput(today));
const summary = ref<Summary | null>(null);
const dailySummaries = ref<DailySummary[]>([]);
const details = ref<Detail[]>([]);
const selectedDailySummary = ref<DailySummary | null>(null);
const viewMode = ref<'summary' | 'detail'>('summary');
const summaryPageNo = ref(1);
const summaryPageSize = ref(10);
const summaryTotal = ref(0);
const detailPageNo = ref(1);
const detailPageSize = ref(20);
const detailTotal = ref(0);
const notice = ref('');
const trendChartRef = ref<HTMLElement | null>(null);
let trendChart: echarts.ECharts | null = null;

onMounted(() => {
  loadAll();
  window.addEventListener('resize', resizeChart);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart);
  trendChart?.dispose();
});

async function loadAll() {
  selectedDailySummary.value = null;
  viewMode.value = 'summary';
  summaryPageNo.value = 1;
  await Promise.all([loadSummary(), loadDailySummaries()]);
}

async function loadSummary() {
  summary.value = await apiGet<Summary>(`/api/rag-metrics/summary?startDate=${startDate.value}&endDate=${endDate.value}`);
  await nextTick();
  renderTrendChart();
}

async function loadDailySummaries() {
  const page = await apiGet<DailySummaryPage>(`/api/rag-metrics/daily-summaries?startDate=${startDate.value}&endDate=${endDate.value}&pageNo=${summaryPageNo.value}&pageSize=${summaryPageSize.value}`);
  dailySummaries.value = page.items || [];
  summaryTotal.value = page.total;
  summaryPageNo.value = page.pageNo;
  summaryPageSize.value = page.pageSize;
}

async function loadDetails() {
  if (!selectedDailySummary.value) {
    details.value = [];
    detailTotal.value = 0;
    return;
  }
  const page = await apiGet<DetailPage>(`/api/rag-metrics/details?startDate=${startDate.value}&endDate=${endDate.value}&dailySummaryId=${selectedDailySummary.value.id}&pageNo=${detailPageNo.value}&pageSize=${detailPageSize.value}`);
  details.value = page.items || [];
  detailTotal.value = page.total;
  detailPageNo.value = page.pageNo;
  detailPageSize.value = page.pageSize;
}

async function runToday() {
  notice.value = '正在提交评估任务...';
  const result = await apiPost<{ selected: number; success: number; failed: number; skipped: number }>(`/api/rag-metrics/run?date=${endDate.value}`);
  notice.value = `评估完成：选中 ${result.selected}，成功 ${result.success}，失败 ${result.failed}，跳过 ${result.skipped}`;
  await loadAll();
}

function openDailySummary(item: DailySummary) {
  selectedDailySummary.value = item;
  viewMode.value = 'detail';
  detailPageNo.value = 1;
  loadDetails();
}

function backToSummaries() {
  selectedDailySummary.value = null;
  viewMode.value = 'summary';
  details.value = [];
  detailTotal.value = 0;
}

function refreshCurrentPanel() {
  if (viewMode.value === 'summary') {
    loadDailySummaries();
  } else {
    loadDetails();
  }
}

function prevSummaryPage() {
  summaryPageNo.value -= 1;
  loadDailySummaries();
}

function nextSummaryPage() {
  summaryPageNo.value += 1;
  loadDailySummaries();
}

function prevDetailPage() {
  detailPageNo.value -= 1;
  loadDetails();
}

function nextDetailPage() {
  detailPageNo.value += 1;
  loadDetails();
}

function percent(value?: number | null) {
  const safe = Number(value || 0);
  return `${Math.round(safe * 10000) / 100}%`;
}

function ids(values?: number[]) {
  return values?.length ? values.join(', ') : '-';
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}

function toDateInput(date: Date) {
  return date.toISOString().slice(0, 10);
}

function renderTrendChart() {
  if (!trendChartRef.value || !summary.value?.trends?.length) {
    return;
  }
  trendChart ??= echarts.init(trendChartRef.value);
  const trends = summary.value.trends;
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['召回率', '精确率', 'RAG消息', '有效消息'], top: 0 },
    grid: { left: 48, right: 48, top: 48, bottom: 36 },
    xAxis: { type: 'category', data: trends.map((item) => item.date) },
    yAxis: [
      { type: 'value', min: 0, max: 1, axisLabel: { formatter: (value: number) => `${Math.round(value * 100)}%` } },
      { type: 'value', minInterval: 1 }
    ],
    series: [
      { name: '召回率', type: 'line', smooth: true, yAxisIndex: 0, data: trends.map((item) => Number(item.averageRecall || 0)) },
      { name: '精确率', type: 'line', smooth: true, yAxisIndex: 0, data: trends.map((item) => Number(item.averagePrecision || 0)) },
      { name: 'RAG消息', type: 'bar', yAxisIndex: 1, data: trends.map((item) => item.totalRagMessageCount || 0) },
      { name: '有效消息', type: 'bar', yAxisIndex: 1, data: trends.map((item) => item.validMessageCount || 0) }
    ]
  });
}

function resizeChart() {
  trendChart?.resize();
}
</script>

<style scoped>
.rag-metrics-page {
  display: grid;
  gap: 1rem;
}

.metric-hero,
.panel {
  padding: 1.2rem;
}

.metric-hero {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) auto;
  gap: 1rem;
  align-items: end;
}

.filter-row,
.header-row,
.header-actions,
.item-top,
.pager,
.chip-row {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.filter-row {
  justify-content: flex-end;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.metric-card {
  padding: 1rem;
}

.metric-card p,
.metric-card span,
.note {
  color: var(--ink-soft);
}

.metric-card strong {
  display: block;
  font-size: 2rem;
  margin: 0.45rem 0;
}

.trend-list,
.detail-list,
.summary-table {
  display: grid;
  gap: 0.75rem;
}

.trend-chart {
  width: 100%;
  min-height: 360px;
}

.summary-row {
  display: grid;
  grid-template-columns: 110px 130px repeat(6, minmax(80px, 1fr));
  gap: 0.65rem;
  align-items: center;
  width: 100%;
  padding: 0.85rem 1rem;
  border: 1px solid var(--line);
  border-radius: 14px;
  color: var(--ink);
  background: rgba(255, 255, 255, 0.48);
  text-align: left;
  cursor: pointer;
}

.summary-row.active,
.summary-row:hover {
  border-color: var(--forest);
  background: rgba(50, 92, 73, 0.08);
}

.trend-row {
  display: grid;
  grid-template-columns: 110px 1fr 80px 1fr 80px;
  gap: 0.65rem;
  align-items: center;
}

.bar-track {
  height: 0.65rem;
  border-radius: 999px;
  background: rgba(29, 43, 35, 0.12);
  overflow: hidden;
}

.bar-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--forest), #77a88d);
}

.bar-track.precision i {
  background: linear-gradient(90deg, var(--brass), #e2bf73);
}

.detail-card {
  padding: 1rem;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.42);
}

.chip-row span,
.status-pill {
  border-radius: 999px;
  padding: 0.28rem 0.55rem;
  background: rgba(50, 92, 73, 0.1);
  font-size: 0.85rem;
}

.evidence {
  white-space: pre-wrap;
  padding: 0.75rem;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
}

.error-text {
  color: var(--brick);
}

@media (max-width: 1100px) {
  .metric-hero,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .trend-row {
    grid-template-columns: 1fr;
  }

  .summary-row {
    grid-template-columns: 1fr;
  }
}
</style>
