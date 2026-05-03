<template>
  <section class="token-metrics-page">
    <article class="metric-hero card">
      <div>
        <p class="tag">TOKEN COST</p>
        <h2 class="section-title">Token 消耗统计</h2>
        <p class="note">按日期范围查询每日 Token 消耗趋势，并查看每个指标日消耗最高的用户。</p>
      </div>
      <div class="filter-row">
        <div>
          <label>开始日期</label>
          <input v-model="startDate" type="date" />
        </div>
        <div>
          <label>结束日期</label>
          <input v-model="endDate" type="date" :max="maxManualEndDate" />
        </div>
        <button class="primary-btn" type="button" @click="loadAll">查询统计</button>
        <button class="ghost-btn" type="button" :disabled="endDate > maxManualEndDate" @click="runManual">手动统计日期范围</button>
      </div>
    </article>

    <div class="metric-grid">
      <article class="metric-card card">
        <p>Token 总消耗</p>
        <strong>{{ number(summary?.totalTokenUsage) }}</strong>
        <span>统计版本 {{ summary?.metricVersion || '-' }}</span>
      </article>
      <article class="metric-card card">
        <p>助手消息数</p>
        <strong>{{ number(summary?.messageCount) }}</strong>
        <span>仅统计 assistant 消息</span>
      </article>
      <article class="metric-card card">
        <p>活跃用户累计</p>
        <strong>{{ number(summary?.activeUserCount) }}</strong>
        <span>按日汇总累计</span>
      </article>
      <article class="metric-card card">
        <p>平均 Token / 消息</p>
        <strong>{{ decimal(summary?.averageTokensPerMessage) }}</strong>
        <span>总 Token / 助手消息数</span>
      </article>
    </div>

    <article class="card panel trend-panel">
      <div class="header-row">
        <div>
          <p class="tag">趋势</p>
          <h3>日期维度消耗趋势</h3>
        </div>
        <span v-if="notice" class="notice">{{ notice }}</span>
      </div>
      <div v-if="summary?.trends?.length" ref="trendChartRef" class="trend-chart"></div>
      <p v-else class="note">当前日期范围暂无 Token 消耗趋势。</p>
    </article>

    <article class="card panel">
      <div class="header-row">
        <div>
          <p class="tag">{{ viewMode === 'summary' ? '日汇总' : 'Top 用户' }}</p>
          <h3>{{ viewMode === 'summary' ? '日汇总结果分页' : `${selectedDailySummary?.metricDate || ''} Token 消耗排行` }}</h3>
        </div>
        <div class="header-actions">
          <span v-if="viewMode === 'summary'" class="note">点击某条日汇总进入 Top 用户</span>
          <button v-if="viewMode === 'topUsers'" class="ghost-btn" type="button" @click="backToSummaries">返回日汇总</button>
          <button class="ghost-btn" type="button" @click="refreshCurrentPanel">{{ viewMode === 'summary' ? '刷新日汇总' : '刷新排行' }}</button>
        </div>
      </div>

      <template v-if="viewMode === 'summary'">
        <div class="summary-table">
          <button
            v-for="item in dailySummaries"
            :key="item.id"
            class="summary-row"
            type="button"
            @click="openDailySummary(item)"
          >
            <span>{{ item.metricDate }}</span>
            <span>{{ item.status }}</span>
            <span>Token {{ number(item.totalTokenUsage) }}</span>
            <span>消息 {{ number(item.messageCount) }}</span>
            <span>用户 {{ number(item.activeUserCount) }}</span>
            <span>均值 {{ decimal(item.averageTokensPerMessage) }}</span>
            <span>Top {{ item.topUserCount }}/{{ item.topUserLimit }}</span>
          </button>
          <p v-if="!dailySummaries.length" class="note">当前日期范围暂无日汇总结果。</p>
        </div>
        <div class="pager">
          <button class="ghost-btn" type="button" :disabled="summaryPageNo <= 1" @click="prevSummaryPage">上一页</button>
          <span>第 {{ summaryPageNo }} 页 / 共 {{ summaryTotal }} 条</span>
          <button class="ghost-btn" type="button" :disabled="summaryPageNo * summaryPageSize >= summaryTotal" @click="nextSummaryPage">下一页</button>
        </div>
      </template>

      <template v-else>
        <div class="top-user-list">
          <div v-for="item in topUsers" :key="item.id" class="top-user-card">
            <div>
              <p class="rank">#{{ item.rankNo }}</p>
              <h4>{{ item.displayName || item.username }}</h4>
              <p class="note">U{{ item.userId }} | {{ item.username || '-' }}</p>
            </div>
            <div class="usage-block">
              <strong>{{ number(item.tokenUsage) }}</strong>
              <span>{{ number(item.messageCount) }} 条消息 · {{ percent(item.usageRatio) }}</span>
            </div>
          </div>
          <p v-if="!topUsers.length" class="note">暂无 Top 用户明细。</p>
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
  totalTokenUsage: number;
  messageCount: number;
  activeUserCount: number;
  averageTokensPerMessage: number;
  status: string;
  dailySummaryId: number;
}

interface DailySummary {
  id: number;
  metricDate: string;
  metricVersion: string;
  totalTokenUsage: number;
  messageCount: number;
  activeUserCount: number;
  averageTokensPerMessage: number;
  topUserLimit: number;
  topUserCount: number;
  status: string;
  errorMessage: string;
  startedAt: string;
  completedAt: string;
}

interface Summary {
  totalTokenUsage: number;
  messageCount: number;
  activeUserCount: number;
  averageTokensPerMessage: number;
  metricVersion: string;
  trends: TrendPoint[];
  dailySummaries: DailySummary[];
}

interface DailySummaryPage {
  total: number;
  pageNo: number;
  pageSize: number;
  items: DailySummary[];
}

interface TopUser {
  id: number;
  dailySummaryId: number;
  metricDate: string;
  userId: number;
  username: string;
  displayName: string;
  rankNo: number;
  tokenUsage: number;
  messageCount: number;
  usageRatio: number;
}

interface RunResult {
  metricDate?: string;
  startDate?: string;
  endDate?: string;
  selected: number;
  success: number;
  failed: number;
  skipped: number;
  alreadyProcessedDates: number;
  alreadyProcessed: boolean;
  message?: string;
  skippedReason?: string;
}

const today = new Date();
const start = new Date(today);
start.setDate(today.getDate() - 7);
const yesterday = new Date(today);
yesterday.setDate(today.getDate() - 1);

const startDate = ref(toDateInput(start));
const endDate = ref(toDateInput(today));
const maxManualEndDate = toDateInput(yesterday);
const summary = ref<Summary | null>(null);
const dailySummaries = ref<DailySummary[]>([]);
const selectedDailySummary = ref<DailySummary | null>(null);
const topUsers = ref<TopUser[]>([]);
const viewMode = ref<'summary' | 'topUsers'>('summary');
const summaryPageNo = ref(1);
const summaryPageSize = ref(10);
const summaryTotal = ref(0);
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
  topUsers.value = [];
  viewMode.value = 'summary';
  summaryPageNo.value = 1;
  await Promise.all([loadSummary(), loadDailySummaries()]);
}

async function loadSummary() {
  summary.value = await apiGet<Summary>(`/api/token-usage-metrics/summary?startDate=${startDate.value}&endDate=${endDate.value}`);
  await nextTick();
  renderTrendChart();
}

async function loadDailySummaries() {
  const page = await apiGet<DailySummaryPage>(`/api/token-usage-metrics/daily-summaries?startDate=${startDate.value}&endDate=${endDate.value}&pageNo=${summaryPageNo.value}&pageSize=${summaryPageSize.value}`);
  dailySummaries.value = page.items || [];
  summaryTotal.value = page.total;
  summaryPageNo.value = page.pageNo;
  summaryPageSize.value = page.pageSize;
}

async function loadTopUsers() {
  if (!selectedDailySummary.value) {
    topUsers.value = [];
    return;
  }
  topUsers.value = await apiGet<TopUser[]>(`/api/token-usage-metrics/top-users?dailySummaryId=${selectedDailySummary.value.id}`);
}

async function runManual() {
  if (endDate.value >= toDateInput(today)) {
    notice.value = '手动统计结束日期只能选择今天之前的历史日期。';
    return;
  }
  notice.value = '正在提交统计任务…';
  const result = await apiPost<RunResult>(`/api/token-usage-metrics/run?date=${startDate.value}&endDate=${endDate.value}`);
  notice.value = result.message || `统计完成：选中 ${result.selected}，成功 ${result.success}，失败 ${result.failed}，跳过 ${result.skipped}`;
  await loadAll();
}

function openDailySummary(item: DailySummary) {
  selectedDailySummary.value = item;
  viewMode.value = 'topUsers';
  loadTopUsers();
}

function backToSummaries() {
  selectedDailySummary.value = null;
  topUsers.value = [];
  viewMode.value = 'summary';
}

function refreshCurrentPanel() {
  if (viewMode.value === 'summary') {
    loadDailySummaries();
  } else {
    loadTopUsers();
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

function number(value?: number | null) {
  return Number(value || 0).toLocaleString();
}

function decimal(value?: number | null) {
  return Number(value || 0).toFixed(2);
}

function percent(value?: number | null) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`;
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
    legend: { data: ['Token消耗', '助手消息', '活跃用户'], top: 0 },
    grid: { left: 64, right: 48, top: 48, bottom: 36 },
    xAxis: { type: 'category', data: trends.map((item) => item.date) },
    yAxis: [
      { type: 'value', minInterval: 1 },
      { type: 'value', minInterval: 1 }
    ],
    series: [
      { name: 'Token消耗', type: 'line', smooth: true, yAxisIndex: 0, data: trends.map((item) => item.totalTokenUsage || 0) },
      { name: '助手消息', type: 'bar', yAxisIndex: 1, data: trends.map((item) => item.messageCount || 0) },
      { name: '活跃用户', type: 'bar', yAxisIndex: 1, data: trends.map((item) => item.activeUserCount || 0) }
    ]
  });
}

function resizeChart() {
  trendChart?.resize();
}
</script>

<style scoped>
.token-metrics-page {
  display: grid;
  gap: 1rem;
}

.metric-hero,
.header-row,
.filter-row,
.header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.filter-row label {
  display: block;
  margin-bottom: 0.35rem;
  color: var(--ink-soft);
  font-size: 0.84rem;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.metric-card {
  padding: 1rem;
}

.metric-card p,
.metric-card span {
  margin: 0;
  color: var(--ink-soft);
}

.metric-card strong {
  display: block;
  margin: 0.45rem 0;
  font-size: clamp(1.5rem, 3vw, 2.35rem);
}

.panel {
  padding: 1rem;
}

.trend-chart {
  width: 100%;
  height: 320px;
}

.summary-table,
.top-user-list {
  display: grid;
  gap: 0.65rem;
  margin-top: 1rem;
}

.summary-row,
.top-user-card {
  display: grid;
  grid-template-columns: 1fr 0.8fr 1fr 1fr 1fr 1fr 0.9fr;
  gap: 0.65rem;
  align-items: center;
  width: 100%;
  border: 1px solid rgba(29, 43, 35, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.38);
  color: var(--ink);
  padding: 0.85rem;
  text-align: left;
}

.summary-row:hover {
  border-color: rgba(50, 92, 73, 0.38);
  transform: translateY(-1px);
}

.top-user-card {
  grid-template-columns: 1fr auto;
}

.top-user-card h4,
.rank {
  margin: 0;
}

.rank {
  color: var(--brass);
  font-weight: 700;
}

.usage-block {
  text-align: right;
}

.usage-block strong {
  display: block;
  font-size: 1.35rem;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}

.notice {
  color: var(--forest);
  font-weight: 700;
}

@media (max-width: 1080px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
