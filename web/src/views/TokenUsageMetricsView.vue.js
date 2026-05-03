import * as echarts from 'echarts';
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { apiGet, apiPost } from '../api/client';
const today = new Date();
const start = new Date(today);
start.setDate(today.getDate() - 7);
const yesterday = new Date(today);
yesterday.setDate(today.getDate() - 1);
const startDate = ref(toDateInput(start));
const endDate = ref(toDateInput(today));
const maxManualEndDate = toDateInput(yesterday);
const summary = ref(null);
const dailySummaries = ref([]);
const selectedDailySummary = ref(null);
const topUsers = ref([]);
const viewMode = ref('summary');
const summaryPageNo = ref(1);
const summaryPageSize = ref(10);
const summaryTotal = ref(0);
const notice = ref('');
const trendChartRef = ref(null);
let trendChart = null;
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
    summary.value = await apiGet(`/api/token-usage-metrics/summary?startDate=${startDate.value}&endDate=${endDate.value}`);
    await nextTick();
    renderTrendChart();
}
async function loadDailySummaries() {
    const page = await apiGet(`/api/token-usage-metrics/daily-summaries?startDate=${startDate.value}&endDate=${endDate.value}&pageNo=${summaryPageNo.value}&pageSize=${summaryPageSize.value}`);
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
    topUsers.value = await apiGet(`/api/token-usage-metrics/top-users?dailySummaryId=${selectedDailySummary.value.id}`);
}
async function runManual() {
    if (endDate.value >= toDateInput(today)) {
        notice.value = '手动统计结束日期只能选择今天之前的历史日期。';
        return;
    }
    notice.value = '正在提交统计任务…';
    const result = await apiPost(`/api/token-usage-metrics/run?date=${startDate.value}&endDate=${endDate.value}`);
    notice.value = result.message || `统计完成：选中 ${result.selected}，成功 ${result.success}，失败 ${result.failed}，跳过 ${result.skipped}`;
    await loadAll();
}
function openDailySummary(item) {
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
    }
    else {
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
function number(value) {
    return Number(value || 0).toLocaleString();
}
function decimal(value) {
    return Number(value || 0).toFixed(2);
}
function percent(value) {
    return `${(Number(value || 0) * 100).toFixed(2)}%`;
}
function toDateInput(date) {
    return date.toISOString().slice(0, 10);
}
function renderTrendChart() {
    if (!trendChartRef.value || !summary.value?.trends?.length) {
        return;
    }
    trendChart ?? (trendChart = echarts.init(trendChartRef.value));
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['filter-row']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['top-user-card']} */ ;
/** @type {__VLS_StyleScopedClasses['top-user-card']} */ ;
/** @type {__VLS_StyleScopedClasses['rank']} */ ;
/** @type {__VLS_StyleScopedClasses['usage-block']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "token-metrics-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "metric-hero card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "note" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "filter-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "date",
});
(__VLS_ctx.startDate);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "date",
    max: (__VLS_ctx.maxManualEndDate),
});
(__VLS_ctx.endDate);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.loadAll) },
    ...{ class: "primary-btn" },
    type: "button",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.runManual) },
    ...{ class: "ghost-btn" },
    type: "button",
    disabled: (__VLS_ctx.endDate > __VLS_ctx.maxManualEndDate),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "metric-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "metric-card card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.number(__VLS_ctx.summary?.totalTokenUsage));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.summary?.metricVersion || '-');
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "metric-card card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.number(__VLS_ctx.summary?.messageCount));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "metric-card card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.number(__VLS_ctx.summary?.activeUserCount));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "metric-card card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.decimal(__VLS_ctx.summary?.averageTokensPerMessage));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel trend-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "notice" },
    });
    (__VLS_ctx.notice);
}
if (__VLS_ctx.summary?.trends?.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ref: "trendChartRef",
        ...{ class: "trend-chart" },
    });
    /** @type {typeof __VLS_ctx.trendChartRef} */ ;
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
(__VLS_ctx.viewMode === 'summary' ? '日汇总' : 'Top 用户');
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.viewMode === 'summary' ? '日汇总结果分页' : `${__VLS_ctx.selectedDailySummary?.metricDate || ''} Token 消耗排行`);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-actions" },
});
if (__VLS_ctx.viewMode === 'summary') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "note" },
    });
}
if (__VLS_ctx.viewMode === 'topUsers') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.backToSummaries) },
        ...{ class: "ghost-btn" },
        type: "button",
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refreshCurrentPanel) },
    ...{ class: "ghost-btn" },
    type: "button",
});
(__VLS_ctx.viewMode === 'summary' ? '刷新日汇总' : '刷新排行');
if (__VLS_ctx.viewMode === 'summary') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "summary-table" },
    });
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.dailySummaries))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.viewMode === 'summary'))
                        return;
                    __VLS_ctx.openDailySummary(item);
                } },
            key: (item.id),
            ...{ class: "summary-row" },
            type: "button",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.metricDate);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.status);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.number(item.totalTokenUsage));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.number(item.messageCount));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.number(item.activeUserCount));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.decimal(item.averageTokensPerMessage));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.topUserCount);
        (item.topUserLimit);
    }
    if (!__VLS_ctx.dailySummaries.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "pager" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.prevSummaryPage) },
        ...{ class: "ghost-btn" },
        type: "button",
        disabled: (__VLS_ctx.summaryPageNo <= 1),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.summaryPageNo);
    (__VLS_ctx.summaryTotal);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.nextSummaryPage) },
        ...{ class: "ghost-btn" },
        type: "button",
        disabled: (__VLS_ctx.summaryPageNo * __VLS_ctx.summaryPageSize >= __VLS_ctx.summaryTotal),
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "top-user-list" },
    });
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.topUsers))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (item.id),
            ...{ class: "top-user-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "rank" },
        });
        (item.rankNo);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
        (item.displayName || item.username);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (item.userId);
        (item.username || '-');
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "usage-block" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (__VLS_ctx.number(item.tokenUsage));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.number(item.messageCount));
        (__VLS_ctx.percent(item.usageRatio));
    }
    if (!__VLS_ctx.topUsers.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
    }
}
/** @type {__VLS_StyleScopedClasses['token-metrics-page']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-hero']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-row']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['trend-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['trend-chart']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['header-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-table']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['pager']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['top-user-list']} */ ;
/** @type {__VLS_StyleScopedClasses['top-user-card']} */ ;
/** @type {__VLS_StyleScopedClasses['rank']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['usage-block']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            startDate: startDate,
            endDate: endDate,
            maxManualEndDate: maxManualEndDate,
            summary: summary,
            dailySummaries: dailySummaries,
            selectedDailySummary: selectedDailySummary,
            topUsers: topUsers,
            viewMode: viewMode,
            summaryPageNo: summaryPageNo,
            summaryPageSize: summaryPageSize,
            summaryTotal: summaryTotal,
            notice: notice,
            trendChartRef: trendChartRef,
            loadAll: loadAll,
            runManual: runManual,
            openDailySummary: openDailySummary,
            backToSummaries: backToSummaries,
            refreshCurrentPanel: refreshCurrentPanel,
            prevSummaryPage: prevSummaryPage,
            nextSummaryPage: nextSummaryPage,
            number: number,
            decimal: decimal,
            percent: percent,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
