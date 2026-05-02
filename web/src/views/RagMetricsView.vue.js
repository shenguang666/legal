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
const details = ref([]);
const selectedDailySummary = ref(null);
const viewMode = ref('summary');
const summaryPageNo = ref(1);
const summaryPageSize = ref(10);
const summaryTotal = ref(0);
const detailPageNo = ref(1);
const detailPageSize = ref(20);
const detailTotal = ref(0);
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
    viewMode.value = 'summary';
    summaryPageNo.value = 1;
    await Promise.all([loadSummary(), loadDailySummaries()]);
}
async function loadSummary() {
    summary.value = await apiGet(`/api/rag-metrics/summary?startDate=${startDate.value}&endDate=${endDate.value}`);
    await nextTick();
    renderTrendChart();
}
async function loadDailySummaries() {
    const page = await apiGet(`/api/rag-metrics/daily-summaries?startDate=${startDate.value}&endDate=${endDate.value}&pageNo=${summaryPageNo.value}&pageSize=${summaryPageSize.value}`);
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
    const page = await apiGet(`/api/rag-metrics/details?startDate=${startDate.value}&endDate=${endDate.value}&dailySummaryId=${selectedDailySummary.value.id}&pageNo=${detailPageNo.value}&pageSize=${detailPageSize.value}`);
    details.value = page.items || [];
    detailTotal.value = page.total;
    detailPageNo.value = page.pageNo;
    detailPageSize.value = page.pageSize;
}
async function runToday() {
    if (endDate.value >= toDateInput(today)) {
        notice.value = '手动评估结束日期只能选择今天之前的历史日期。';
        return;
    }
    notice.value = '正在提交评估任务…';
    const result = await apiPost(`/api/rag-metrics/run?date=${startDate.value}&endDate=${endDate.value}`);
    notice.value = result.message || `评估完成：选中 ${result.selected}，成功 ${result.success}，失败 ${result.failed}，跳过 ${result.skipped}`;
    await loadAll();
}
function openDailySummary(item) {
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
    }
    else {
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
function percent(value) {
    const safe = Number(value || 0);
    return `${Math.round(safe * 10000) / 100}%`;
}
function ids(values) {
    return values?.length ? values.join(', ') : '-';
}
function formatTime(value) {
    return value ? value.replace('T', ' ').slice(0, 19) : '-';
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
        legend: { data: ['召回率', '精确率', 'RAG消息', '有效消息'], top: 0 },
        grid: { left: 48, right: 48, top: 48, bottom: 36 },
        xAxis: { type: 'category', data: trends.map((item) => item.date) },
        yAxis: [
            { type: 'value', min: 0, max: 1, axisLabel: { formatter: (value) => `${Math.round(value * 100)}%` } },
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['metric-hero']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-row']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-card']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['bar-track']} */ ;
/** @type {__VLS_StyleScopedClasses['bar-track']} */ ;
/** @type {__VLS_StyleScopedClasses['chip-row']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-hero']} */ ;
/** @type {__VLS_StyleScopedClasses['metric-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['trend-row']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "rag-metrics-page" },
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
    ...{ onClick: (__VLS_ctx.runToday) },
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
(__VLS_ctx.percent(__VLS_ctx.summary?.averageRecall));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "metric-card card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.percent(__VLS_ctx.summary?.averagePrecision));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "metric-card card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.summary?.evaluatedCount ?? 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.summary?.validMessageCount ?? 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "metric-card card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.summary?.successCount ?? 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.summary?.failedCount ?? 0);
(__VLS_ctx.summary?.filteredCount ?? 0);
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
(__VLS_ctx.viewMode === 'summary' ? '日汇总' : '消息明细');
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.viewMode === 'summary' ? '日汇总结果分页' : `${__VLS_ctx.selectedDailySummary?.metricDate || ''} 消息级评估结果`);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-actions" },
});
if (__VLS_ctx.viewMode === 'summary') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "note" },
    });
}
if (__VLS_ctx.viewMode === 'detail') {
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
(__VLS_ctx.viewMode === 'summary' ? '刷新日汇总' : '刷新明细');
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
        (__VLS_ctx.percent(item.averageRecall));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.percent(item.averagePrecision));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.totalRagMessageCount);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.validMessageCount);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.filteredMessageCount);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.failedCount);
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
        ...{ class: "detail-list" },
    });
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.details))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (item.id),
            ...{ class: "detail-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "item-top" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
        (item.queryText);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (item.dailySummaryId || '-');
        (item.retrievalLogId);
        (item.status);
        (__VLS_ctx.formatTime(item.evaluatedAt));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "status-pill" },
            'data-status': (item.status),
        });
        (item.status);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "chip-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.truePositive ?? 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.falseNegative ?? 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (item.falsePositive ?? 0);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.percent(item.recallScore));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.percent(item.precisionScore));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (__VLS_ctx.ids(item.originalHitChunkIds));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (__VLS_ctx.ids(item.relevantOriginalChunkIds));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (__VLS_ctx.ids(item.irrelevantOriginalChunkIds));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (__VLS_ctx.ids(item.missedRelevantChunkIds));
        if (item.explanation) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "evidence" },
            });
            (item.explanation);
        }
        if (item.errorMessage) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "error-text" },
            });
            (item.errorMessage);
        }
    }
    if (!__VLS_ctx.details.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "pager" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.prevDetailPage) },
        ...{ class: "ghost-btn" },
        type: "button",
        disabled: (__VLS_ctx.detailPageNo <= 1),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.detailPageNo);
    (__VLS_ctx.detailTotal);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.nextDetailPage) },
        ...{ class: "ghost-btn" },
        type: "button",
        disabled: (__VLS_ctx.detailPageNo * __VLS_ctx.detailPageSize >= __VLS_ctx.detailTotal),
    });
}
/** @type {__VLS_StyleScopedClasses['rag-metrics-page']} */ ;
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
/** @type {__VLS_StyleScopedClasses['detail-list']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-card']} */ ;
/** @type {__VLS_StyleScopedClasses['item-top']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['chip-row']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['evidence']} */ ;
/** @type {__VLS_StyleScopedClasses['error-text']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['pager']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            startDate: startDate,
            endDate: endDate,
            maxManualEndDate: maxManualEndDate,
            summary: summary,
            dailySummaries: dailySummaries,
            details: details,
            selectedDailySummary: selectedDailySummary,
            viewMode: viewMode,
            summaryPageNo: summaryPageNo,
            summaryPageSize: summaryPageSize,
            summaryTotal: summaryTotal,
            detailPageNo: detailPageNo,
            detailPageSize: detailPageSize,
            detailTotal: detailTotal,
            notice: notice,
            trendChartRef: trendChartRef,
            loadAll: loadAll,
            runToday: runToday,
            openDailySummary: openDailySummary,
            backToSummaries: backToSummaries,
            refreshCurrentPanel: refreshCurrentPanel,
            prevSummaryPage: prevSummaryPage,
            nextSummaryPage: nextSummaryPage,
            prevDetailPage: prevDetailPage,
            nextDetailPage: nextDetailPage,
            percent: percent,
            ids: ids,
            formatTime: formatTime,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
