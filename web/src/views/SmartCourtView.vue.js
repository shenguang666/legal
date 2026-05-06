import * as echarts from 'echarts';
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { auditCourtReportExport, confirmCourtFacts, createCourtCase as createCourtCaseApi, deleteCourtCase as deleteCourtCaseApi, generateCourtReport, getCourtGraph, ignoreCourtSuggestion, listCourtCases, listCourtSuggestions, resolveCourtSuggestion, startCourtHearing, } from '../api/court';
const loading = ref(false);
const cases = ref([]);
const activeCase = ref(null);
const graph = ref(null);
const suggestions = ref([]);
const report = ref(null);
const graphRef = ref(null);
let chart = null;
const form = ref({ title: '', userSide: 'PLAINTIFF', caseSummary: '', userObjective: '' });
const documentIdsText = ref('');
const judgeJson = ref('');
const activeAction = ref('');
const deleteDialogVisible = ref(false);
const notice = ref(null);
const operationLogs = ref([]);
const highSuggestions = computed(() => suggestions.value.filter((item) => item.severity === 'HIGH' && item.status === 'OPEN'));
onMounted(loadCases);
onBeforeUnmount(() => chart?.dispose());
async function loadCases() {
    loading.value = true;
    try {
        const page = await listCourtCases();
        cases.value = page.records || [];
        if (!activeCase.value && cases.value.length) {
            await selectCase(cases.value[0]);
        }
    }
    finally {
        loading.value = false;
    }
}
async function createCase() {
    loading.value = true;
    try {
        const documentIds = documentIdsText.value.split(/[,，\s]+/).map((item) => Number(item)).filter(Boolean);
        const created = await createCourtCaseApi({ ...form.value, documentIds });
        activeCase.value = created;
        await loadCases();
        await loadGraphAndSuggestions();
        pushLog(`案件「${created.title}」创建成功`);
        showNotice('success', '案件创建成功');
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
    finally {
        loading.value = false;
    }
}
async function selectCase(item) {
    activeCase.value = item;
    report.value = null;
    await loadGraphAndSuggestions();
}
async function confirmFacts() {
    if (!activeCase.value)
        return;
    activeAction.value = 'confirmFacts';
    try {
        activeCase.value = await confirmCourtFacts(activeCase.value.caseId);
        await loadCases();
        await loadGraphAndSuggestions();
        pushLog(`要素确认成功，当前状态：${statusText(activeCase.value.status)}`);
        showNotice('success', '要素已确认');
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
    finally {
        activeAction.value = '';
    }
}
async function startHearing() {
    if (!activeCase.value)
        return;
    activeAction.value = 'startHearing';
    try {
        activeCase.value = await startCourtHearing(activeCase.value.caseId);
        await loadCases();
        await loadGraphAndSuggestions();
        pushLog(`开庭成功，当前状态：${statusText(activeCase.value.status)}`);
        showNotice('success', '已进入开庭状态');
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
    finally {
        activeAction.value = '';
    }
}
function requestDeleteCase() {
    if (!activeCase.value)
        return;
    deleteDialogVisible.value = true;
}
async function deleteCase() {
    if (!activeCase.value)
        return;
    const title = activeCase.value.title;
    activeAction.value = 'deleteCase';
    try {
        await deleteCourtCaseApi(activeCase.value.caseId);
        deleteDialogVisible.value = false;
        activeCase.value = null;
        graph.value = null;
        suggestions.value = [];
        operationLogs.value = [];
        await loadCases();
        showNotice('success', `案件「${title}」已删除`);
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
    finally {
        activeAction.value = '';
    }
}
async function loadGraphAndSuggestions() {
    if (!activeCase.value)
        return;
    activeAction.value = activeAction.value || 'refreshGraph';
    try {
        graph.value = await getCourtGraph(activeCase.value.caseId, { hops: 2, limit: 200 });
        suggestions.value = await listCourtSuggestions(activeCase.value.caseId);
        await nextTick(renderGraph);
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
    finally {
        if (activeAction.value === 'refreshGraph') {
            activeAction.value = '';
        }
    }
}
function renderGraph() {
    const el = graphRef.value;
    if (!el || !graph.value)
        return;
    chart || (chart = echarts.init(el));
    chart.setOption({
        tooltip: {},
        series: [{
                type: 'graph',
                layout: 'force',
                roam: true,
                force: { repulsion: 160, edgeLength: 90 },
                data: graph.value.nodes.map((node) => ({ id: node.businessId, name: node.label || node.businessId, category: node.type, symbolSize: 46 })),
                links: graph.value.edges.map((edge) => ({ source: edge.sourceBusinessId, target: edge.targetBusinessId, name: edge.type })),
                label: { show: true, fontSize: 10 },
                categories: [...new Set(graph.value.nodes.map((node) => node.type))].map((name) => ({ name })),
            }],
    });
}
async function ignoreSuggestion(suggestionId) {
    if (!activeCase.value)
        return;
    activeAction.value = `ignore-${suggestionId}`;
    try {
        await ignoreCourtSuggestion(activeCase.value.caseId, suggestionId);
        await loadGraphAndSuggestions();
        pushLog(`补证建议 #${suggestionId} 已忽略`);
        showNotice('success', '补证建议已忽略');
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
    finally {
        activeAction.value = '';
    }
}
async function resolveSuggestion(suggestionId) {
    if (!activeCase.value)
        return;
    activeAction.value = `resolve-${suggestionId}`;
    try {
        await resolveCourtSuggestion(activeCase.value.caseId, suggestionId);
        await loadGraphAndSuggestions();
        pushLog(`补证建议 #${suggestionId} 已解除`);
        showNotice('success', '补证建议已解除');
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
    finally {
        activeAction.value = '';
    }
}
async function generateReport() {
    if (!activeCase.value)
        return;
    try {
        const output = JSON.parse(judgeJson.value);
        report.value = await generateCourtReport(activeCase.value.caseId, null, output);
        pushLog('模拟裁判报告生成成功');
        showNotice('success', '报告已生成');
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
}
async function auditExport() {
    if (!activeCase.value)
        return;
    try {
        await auditCourtReportExport(activeCase.value.caseId, 'PDF');
        showNotice('success', 'PDF 导出审计已记录');
    }
    catch (err) {
        showNotice('error', errorMessage(err));
    }
}
function parseList(json) {
    try {
        const value = JSON.parse(json || '[]');
        return Array.isArray(value) ? value : [];
    }
    catch {
        return [];
    }
}
function statusText(status) {
    const map = { DRAFT: '草稿', READY: '可开庭', HEARING: '庭审中', JUDGED: '已裁判', ARCHIVED: '已归档' };
    return map[status] || status;
}
function pushLog(text) {
    operationLogs.value.unshift({ id: Date.now(), text: `${new Date().toLocaleTimeString()} ${text}` });
    operationLogs.value = operationLogs.value.slice(0, 6);
}
function showNotice(type, text) {
    notice.value = { type, text };
    window.setTimeout(() => {
        if (notice.value?.text === text) {
            notice.value = null;
        }
    }, 3200);
}
function errorMessage(err) {
    return err instanceof Error ? err.message : String(err || '操作失败');
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['hero-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['case-card']} */ ;
/** @type {__VLS_StyleScopedClasses['case-card']} */ ;
/** @type {__VLS_StyleScopedClasses['case-head']} */ ;
/** @type {__VLS_StyleScopedClasses['graph-head']} */ ;
/** @type {__VLS_StyleScopedClasses['operation-log']} */ ;
/** @type {__VLS_StyleScopedClasses['operation-log']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion']} */ ;
/** @type {__VLS_StyleScopedClasses['mini-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['court-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "court-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "hero-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "eyebrow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.loadCases) },
    ...{ class: "primary-btn" },
    type: "button",
    disabled: (__VLS_ctx.loading),
});
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "notice" },
        ...{ class: (__VLS_ctx.notice.type) },
    });
    (__VLS_ctx.notice.text);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "court-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "panel cases-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.createCase) },
    ...{ class: "case-form" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "案件标题",
});
(__VLS_ctx.form.title);
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.form.userSide),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "PLAINTIFF",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "DEFENDANT",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.form.caseSummary),
    rows: "4",
    placeholder: "案件简述",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.form.userObjective),
    rows: "3",
    placeholder: "核心诉求或抗辩目标",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "证据文档ID，逗号分隔",
});
(__VLS_ctx.documentIdsText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary-btn" },
    type: "submit",
    disabled: (__VLS_ctx.loading),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "case-list" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.cases))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectCase(item);
            } },
        key: (item.caseId),
        ...{ class: "case-card" },
        ...{ class: ({ active: __VLS_ctx.activeCase?.caseId === item.caseId }) },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (item.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.statusText(item.status));
    (item.userSide === 'PLAINTIFF' ? '原告' : '被告');
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "panel hearing-panel" },
});
if (__VLS_ctx.activeCase) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "case-head" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "eyebrow" },
    });
    (__VLS_ctx.activeCase.caseId);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.activeCase.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.activeCase.caseSummary || '暂无案件摘要');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "status-pill" },
    });
    (__VLS_ctx.statusText(__VLS_ctx.activeCase.status));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "action-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.confirmFacts) },
        type: "button",
        disabled: (!!__VLS_ctx.activeAction),
    });
    (__VLS_ctx.activeAction === 'confirmFacts' ? '确认中...' : '确认要素');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.startHearing) },
        type: "button",
        disabled: (!!__VLS_ctx.activeAction),
    });
    (__VLS_ctx.activeAction === 'startHearing' ? '开庭中...' : '开庭');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.loadGraphAndSuggestions) },
        type: "button",
        disabled: (!!__VLS_ctx.activeAction),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.requestDeleteCase) },
        ...{ class: "danger" },
        type: "button",
        disabled: (!!__VLS_ctx.activeAction),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dialogue-box" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "system-line" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.activeCase.userObjective || '未填写');
    if (__VLS_ctx.operationLogs.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "operation-log" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
        for (const [item] of __VLS_getVForSourceType((__VLS_ctx.operationLogs))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                key: (item.id),
            });
            (item.text);
        }
    }
    if (__VLS_ctx.highSuggestions.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "suggestion-box" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
        for (const [item] of __VLS_getVForSourceType((__VLS_ctx.highSuggestions))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
                key: (item.suggestionId),
                ...{ class: "suggestion high" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
            (item.missingDescription);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
            (item.potentialImpact || item.rationale);
        }
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "report-panel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "report-form" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.judgeJson),
        rows: "8",
        placeholder: "粘贴 AI 法官 JSON 输出",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.generateReport) },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.auditExport) },
        type: "button",
        disabled: (!__VLS_ctx.report),
    });
    if (__VLS_ctx.report) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
            ...{ class: "report-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "watermark" },
        });
        (__VLS_ctx.report.watermark);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h5, __VLS_intrinsicElements.h5)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        (__VLS_ctx.parseList(__VLS_ctx.report.focusIssuesJson).join('；'));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h5, __VLS_intrinsicElements.h5)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        (__VLS_ctx.parseList(__VLS_ctx.report.judgmentPointsJson).join('；'));
    }
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty-state" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "panel graph-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "graph-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
(__VLS_ctx.graph?.graphState || '未加载');
(__VLS_ctx.graph?.pendingEventCount ?? 0);
if (__VLS_ctx.graph?.warning) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "warning" },
    });
    (__VLS_ctx.graph.warning);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "graphRef",
    ...{ class: "graph-canvas" },
});
/** @type {typeof __VLS_ctx.graphRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "suggestions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.suggestions))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
        key: (item.suggestionId),
        ...{ class: "suggestion" },
        ...{ class: (item.severity.toLowerCase()) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (item.suggestionType);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (item.severity);
    (item.status);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (item.missingDescription);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "mini-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.ignoreSuggestion(item.suggestionId);
            } },
        type: "button",
        disabled: (!!__VLS_ctx.activeAction),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.resolveSuggestion(item.suggestionId);
            } },
        type: "button",
        disabled: (!!__VLS_ctx.activeAction),
    });
}
if (__VLS_ctx.deleteDialogVisible) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "modal-mask" },
        role: "dialog",
        'aria-modal': "true",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "confirm-dialog" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "eyebrow" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.activeCase?.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "dialog-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.deleteDialogVisible))
                    return;
                __VLS_ctx.deleteDialogVisible = false;
            } },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.deleteCase) },
        ...{ class: "danger" },
        type: "button",
        disabled: (__VLS_ctx.activeAction === 'deleteCase'),
    });
    (__VLS_ctx.activeAction === 'deleteCase' ? '删除中...' : '确认删除');
}
/** @type {__VLS_StyleScopedClasses['court-page']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['court-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['cases-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['case-form']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['case-list']} */ ;
/** @type {__VLS_StyleScopedClasses['case-card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['hearing-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['case-head']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['action-row']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['dialogue-box']} */ ;
/** @type {__VLS_StyleScopedClasses['system-line']} */ ;
/** @type {__VLS_StyleScopedClasses['operation-log']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion-box']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion']} */ ;
/** @type {__VLS_StyleScopedClasses['high']} */ ;
/** @type {__VLS_StyleScopedClasses['report-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['report-form']} */ ;
/** @type {__VLS_StyleScopedClasses['report-card']} */ ;
/** @type {__VLS_StyleScopedClasses['watermark']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-state']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['graph-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['graph-head']} */ ;
/** @type {__VLS_StyleScopedClasses['warning']} */ ;
/** @type {__VLS_StyleScopedClasses['graph-canvas']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestions']} */ ;
/** @type {__VLS_StyleScopedClasses['suggestion']} */ ;
/** @type {__VLS_StyleScopedClasses['mini-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['modal-mask']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['dialog-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            loading: loading,
            cases: cases,
            activeCase: activeCase,
            graph: graph,
            suggestions: suggestions,
            report: report,
            graphRef: graphRef,
            form: form,
            documentIdsText: documentIdsText,
            judgeJson: judgeJson,
            activeAction: activeAction,
            deleteDialogVisible: deleteDialogVisible,
            notice: notice,
            operationLogs: operationLogs,
            highSuggestions: highSuggestions,
            loadCases: loadCases,
            createCase: createCase,
            selectCase: selectCase,
            confirmFacts: confirmFacts,
            startHearing: startHearing,
            requestDeleteCase: requestDeleteCase,
            deleteCase: deleteCase,
            loadGraphAndSuggestions: loadGraphAndSuggestions,
            ignoreSuggestion: ignoreSuggestion,
            resolveSuggestion: resolveSuggestion,
            generateReport: generateReport,
            auditExport: auditExport,
            parseList: parseList,
            statusText: statusText,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
