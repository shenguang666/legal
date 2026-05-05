import { onMounted, ref } from 'vue';
import { apiDelete, apiGet, apiPost, apiPostForm, randomRequestId } from '../api/client';
import ConfirmDialog from '../components/ConfirmDialog.vue';
const title = ref('');
const source = ref('');
const selectedFiles = ref([]);
const documents = ref([]);
const notice = ref('');
const parseMethod = ref('MINERU_PRECISE');
const parseMethods = ref(['NATIVE']);
const documentParseMethodFilter = ref('ALL');
const maxUploadDocuments = ref(1);
const cleaningAvailable = ref(false);
const cleaningEnabled = ref(false);
const busy = ref(false);
const qaIndexScope = ref('NATIVE_ONLY');
const qaIndexScopes = ref(['NATIVE_ONLY', 'MINERU_ONLY', 'BOTH']);
const nativeIndexName = ref('');
const mineruIndexName = ref('');
const selectedDocument = ref(null);
const confirmState = ref({
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
    const capabilities = await apiGet('/api/document-processing/capabilities');
    parseMethods.value = orderedParseMethods(capabilities.availableParseMethods?.length ? capabilities.availableParseMethods : ['NATIVE']);
    parseMethod.value = preferredParseMethod(parseMethods.value, capabilities.defaultParseMethod);
    maxUploadDocuments.value = capabilities.maxUploadDocuments || 1;
    cleaningAvailable.value = Boolean(capabilities.cleaningAvailable);
    cleaningEnabled.value = false;
}
function orderedParseMethods(methods) {
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
function preferredParseMethod(methods, defaultMethod) {
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
    documents.value = await apiGet(`/api/knowledge/documents${query}`);
}
async function loadQaIndexConfig() {
    const config = await apiGet('/api/knowledge/admin/qa-index-config');
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
        await apiPost('/api/knowledge/admin/qa-index-config', { indexScope: qaIndexScope.value });
        notice.value = `智能问答知识库检索范围已切换为：${qaIndexScopeLabel(qaIndexScope.value)}`;
        await loadQaIndexConfig();
    }
    finally {
        busy.value = false;
    }
}
function onSelectFile(event) {
    const input = event.target;
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
    }
    finally {
        busy.value = false;
    }
}
async function triggerIndex(item) {
    openConfirm({
        title: '重新触发向量索引',
        message: '确认后会重新投递该文档的向量索引任务。',
        targetName: item.title,
        targetLabel: '文档名称',
        confirmText: '触发索引',
        action: () => executeTriggerIndex(item),
    });
}
async function executeTriggerIndex(item) {
    busy.value = true;
    notice.value = `正在投递文档“${item.title}”的索引任务…`;
    try {
        await apiPost(`/api/knowledge/documents/${item.documentId}/index?requestId=${encodeURIComponent(randomRequestId('kb-index'))}`);
        notice.value = `文档“${item.title}”已投递索引任务`;
        await refresh();
    }
    finally {
        busy.value = false;
    }
}
async function retryParse(item) {
    openConfirm({
        title: '重新提交解析任务',
        message: '确认后会重新提交该文档的解析任务。',
        targetName: item.title,
        targetLabel: '文档名称',
        confirmText: '重试解析',
        action: () => executeRetryParse(item),
    });
}
async function executeRetryParse(item) {
    busy.value = true;
    notice.value = `正在重新提交文档“${item.title}”的解析任务…`;
    try {
        await apiPost(`/api/knowledge/documents/${item.documentId}/retry-parse?requestId=${encodeURIComponent(randomRequestId('kb-retry-parse'))}`);
        notice.value = `文档“${item.title}”已重新提交解析`;
        await refresh();
    }
    finally {
        busy.value = false;
    }
}
function parseMethodLabel(method) {
    if (method === 'MINERU_PRECISE') {
        return 'MinerU 精准解析';
    }
    return '原生解析';
}
function qaIndexScopeLabel(scope) {
    if (scope === 'MINERU_ONLY') {
        return '仅查询 MinerU 精准解析索引';
    }
    if (scope === 'BOTH') {
        return '同时查询原生索引和 MinerU 索引';
    }
    return '仅查询原生解析索引';
}
async function remove(item) {
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
async function executeRemove(item) {
    busy.value = true;
    notice.value = `正在删除文档“${item.title}”及其切片…`;
    try {
        await apiDelete(`/api/knowledge/documents/${item.documentId}?requestId=${encodeURIComponent(randomRequestId('kb-delete'))}`);
        notice.value = `文档“${item.title}”已删除，数据库切片和 Elasticsearch 切片已同步清理`;
        await refresh();
    }
    finally {
        busy.value = false;
    }
}
function openDetail(item) {
    selectedDocument.value = item;
}
function closeDetail() {
    selectedDocument.value = null;
}
async function openDocumentAsset(item, fileName) {
    if (!item.documentUrl) {
        notice.value = '当前文档暂未记录可访问原文档地址';
        return;
    }
    try {
        const signedUrl = await apiGet(`/api/document-assets/${item.documentId}/${encodeURIComponent(fileName)}`);
        window.open(signedUrl, '_blank', 'noopener,noreferrer');
    }
    catch (err) {
        notice.value = err?.message || '获取文档下载地址失败';
    }
}
function importerLabel(item) {
    return item.ownerUsername || (item.ownerUserId ? `用户-${item.ownerUserId}` : '-');
}
function formatDate(value) {
    if (!value) {
        return '-';
    }
    return new Date(value).toLocaleString();
}
function openConfirm(options) {
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['doc-item']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-item']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-item']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['knowledge-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "knowledge-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "grid form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "knowledge-title",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "knowledge-title",
    name: "knowledgeTitle",
    autocomplete: "off",
    placeholder: "可选，不填则使用文件名…",
});
(__VLS_ctx.title);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "knowledge-source",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "knowledge-source",
    name: "knowledgeSource",
    autocomplete: "off",
    placeholder: "可选，例如：国家法规库…",
});
(__VLS_ctx.source);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "knowledge-file",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.onSelectFile) },
    id: "knowledge-file",
    name: "knowledgeFile",
    type: "file",
    multiple: true,
    accept: ".pdf,.doc,.docx,.txt,.md",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "knowledge-parse-method",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    id: "knowledge-parse-method",
    value: (__VLS_ctx.parseMethod),
    ...{ class: "console-select" },
});
for (const [method] of __VLS_getVForSourceType((__VLS_ctx.parseMethods))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (method),
        value: (method),
    });
    (__VLS_ctx.parseMethodLabel(method));
}
if (__VLS_ctx.cleaningAvailable) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "selection-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "selection-chip selection-chip--soft" },
        ...{ class: ({ 'selection-chip--active': __VLS_ctx.cleaningEnabled }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        type: "checkbox",
    });
    (__VLS_ctx.cleaningEnabled);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.importDocument) },
    ...{ class: "primary-btn" },
    type: "button",
    disabled: (__VLS_ctx.busy),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "note" },
});
(__VLS_ctx.maxUploadDocuments);
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "grid form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "qa-index-scope",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    id: "qa-index-scope",
    value: (__VLS_ctx.qaIndexScope),
    ...{ class: "console-select" },
});
for (const [scope] of __VLS_getVForSourceType((__VLS_ctx.qaIndexScopes))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (scope),
        value: (scope),
    });
    (__VLS_ctx.qaIndexScopeLabel(scope));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "note" },
});
(__VLS_ctx.nativeIndexName || '-');
(__VLS_ctx.mineruIndexName || '-');
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.saveQaIndexConfig) },
    ...{ class: "primary-btn" },
    type: "button",
    disabled: (__VLS_ctx.busy),
});
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refresh) },
    ...{ class: "ghost-btn" },
    type: "button",
    disabled: (__VLS_ctx.busy),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "filter-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "knowledge-list-parse-method",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    ...{ onChange: (__VLS_ctx.refresh) },
    id: "knowledge-list-parse-method",
    value: (__VLS_ctx.documentParseMethodFilter),
    ...{ class: "console-select" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "ALL",
});
for (const [method] of __VLS_getVForSourceType((__VLS_ctx.parseMethods))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (method),
        value: (method),
    });
    (__VLS_ctx.parseMethodLabel(method));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "doc-list" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.documents))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (item.documentId),
        ...{ class: "doc-item" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "doc-status" },
    });
    (item.status);
    (item.indexStatus);
    (item.parseStatus || 'COMPLETED');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    (item.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (item.source);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.parseMethodLabel(item.parseMethod || 'NATIVE'));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (item.cleaningEnabled ? '已启用' : '未启用');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "doc-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openDetail(item);
            } },
        ...{ class: "ghost-btn" },
        type: "button",
        disabled: (__VLS_ctx.busy),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.triggerIndex(item);
            } },
        ...{ class: "ghost-btn" },
        type: "button",
        disabled: (__VLS_ctx.busy),
    });
    if (item.parseStatus === 'FAILED') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(item.parseStatus === 'FAILED'))
                        return;
                    __VLS_ctx.retryParse(item);
                } },
            ...{ class: "ghost-btn" },
            type: "button",
            disabled: (__VLS_ctx.busy),
        });
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.remove(item);
            } },
        ...{ class: "warn-btn" },
        type: "button",
        disabled: (__VLS_ctx.busy),
    });
    if (item.parseFailureReason) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
        (item.parseFailureReason);
    }
}
if (!__VLS_ctx.documents.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
}
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
        'aria-live': "polite",
    });
    (__VLS_ctx.notice);
}
const __VLS_0 = {}.Teleport;
/** @type {[typeof __VLS_components.Teleport, typeof __VLS_components.Teleport, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    to: "body",
}));
const __VLS_2 = __VLS_1({
    to: "body",
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
if (__VLS_ctx.selectedDocument) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-panel" },
        role: "dialog",
        'aria-modal': "true",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-modal card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "header-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "tag" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.selectedDocument.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.closeDetail) },
        ...{ class: "ghost-btn" },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-grid" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.selectedDocument.source);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.importerLabel(__VLS_ctx.selectedDocument));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.formatDate(__VLS_ctx.selectedDocument.createdAt));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.parseMethodLabel(__VLS_ctx.selectedDocument.parseMethod || 'NATIVE'));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.selectedDocument.status);
    (__VLS_ctx.selectedDocument.indexStatus);
    (__VLS_ctx.selectedDocument.parseStatus || 'COMPLETED');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.selectedDocument))
                    return;
                __VLS_ctx.openDocumentAsset(__VLS_ctx.selectedDocument, 'origin');
            } },
        ...{ class: "primary-btn" },
        type: "button",
        disabled: (!__VLS_ctx.selectedDocument.documentUrl),
    });
    if (__VLS_ctx.selectedDocument.parseMethod !== 'NATIVE') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.selectedDocument))
                        return;
                    if (!(__VLS_ctx.selectedDocument.parseMethod !== 'NATIVE'))
                        return;
                    __VLS_ctx.openDocumentAsset(__VLS_ctx.selectedDocument, 'full.md');
                } },
            ...{ class: "ghost-btn" },
            type: "button",
            disabled: (!__VLS_ctx.selectedDocument.documentUrl),
        });
    }
    if (__VLS_ctx.selectedDocument.parseMethod !== 'NATIVE') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.selectedDocument))
                        return;
                    if (!(__VLS_ctx.selectedDocument.parseMethod !== 'NATIVE'))
                        return;
                    __VLS_ctx.openDocumentAsset(__VLS_ctx.selectedDocument, 'content_list_v2.json');
                } },
            ...{ class: "ghost-btn" },
            type: "button",
            disabled: (!__VLS_ctx.selectedDocument.documentUrl),
        });
    }
    if (!__VLS_ctx.selectedDocument.documentUrl) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "note" },
        });
    }
}
var __VLS_3;
/** @type {[typeof ConfirmDialog, ]} */ ;
// @ts-ignore
const __VLS_4 = __VLS_asFunctionalComponent(ConfirmDialog, new ConfirmDialog({
    ...{ 'onCancel': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.confirmState.visible),
    title: (__VLS_ctx.confirmState.title),
    message: (__VLS_ctx.confirmState.message),
    targetName: (__VLS_ctx.confirmState.targetName),
    targetLabel: (__VLS_ctx.confirmState.targetLabel),
    confirmText: (__VLS_ctx.confirmState.confirmText),
    eyebrow: (__VLS_ctx.confirmState.eyebrow),
    danger: (__VLS_ctx.confirmState.danger),
}));
const __VLS_5 = __VLS_4({
    ...{ 'onCancel': {} },
    ...{ 'onConfirm': {} },
    modelValue: (__VLS_ctx.confirmState.visible),
    title: (__VLS_ctx.confirmState.title),
    message: (__VLS_ctx.confirmState.message),
    targetName: (__VLS_ctx.confirmState.targetName),
    targetLabel: (__VLS_ctx.confirmState.targetLabel),
    confirmText: (__VLS_ctx.confirmState.confirmText),
    eyebrow: (__VLS_ctx.confirmState.eyebrow),
    danger: (__VLS_ctx.confirmState.danger),
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
let __VLS_7;
let __VLS_8;
let __VLS_9;
const __VLS_10 = {
    onCancel: (__VLS_ctx.closeConfirm)
};
const __VLS_11 = {
    onConfirm: (__VLS_ctx.confirmAction)
};
var __VLS_6;
/** @type {__VLS_StyleScopedClasses['knowledge-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['console-select']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-row']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip--soft']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['console-select']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-row']} */ ;
/** @type {__VLS_StyleScopedClasses['console-select']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-list']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-item']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-status']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['warn-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ConfirmDialog: ConfirmDialog,
            title: title,
            source: source,
            documents: documents,
            notice: notice,
            parseMethod: parseMethod,
            parseMethods: parseMethods,
            documentParseMethodFilter: documentParseMethodFilter,
            maxUploadDocuments: maxUploadDocuments,
            cleaningAvailable: cleaningAvailable,
            cleaningEnabled: cleaningEnabled,
            busy: busy,
            qaIndexScope: qaIndexScope,
            qaIndexScopes: qaIndexScopes,
            nativeIndexName: nativeIndexName,
            mineruIndexName: mineruIndexName,
            selectedDocument: selectedDocument,
            confirmState: confirmState,
            refresh: refresh,
            saveQaIndexConfig: saveQaIndexConfig,
            onSelectFile: onSelectFile,
            importDocument: importDocument,
            triggerIndex: triggerIndex,
            retryParse: retryParse,
            parseMethodLabel: parseMethodLabel,
            qaIndexScopeLabel: qaIndexScopeLabel,
            remove: remove,
            openDetail: openDetail,
            closeDetail: closeDetail,
            openDocumentAsset: openDocumentAsset,
            importerLabel: importerLabel,
            formatDate: formatDate,
            closeConfirm: closeConfirm,
            confirmAction: confirmAction,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
