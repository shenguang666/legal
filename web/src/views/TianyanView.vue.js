import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { apiDelete, apiGet, apiPost, apiPostForm, randomRequestId } from '../api/client';
import ConfirmDialog from '../components/ConfirmDialog.vue';
const router = useRouter();
const title = ref('');
const source = ref('');
const selectedFile = ref(null);
const documents = ref([]);
const notice = ref('');
const parseMethod = ref('MINERU_PRECISE');
const parseMethods = ref(['NATIVE']);
const maxUploadDocuments = ref(1);
const cleaningAvailable = ref(false);
const cleaningEnabled = ref(false);
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
    documents.value = await apiGet('/api/tianyan/documents');
}
function onSelectFile(event) {
    const input = event.target;
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
async function remove(item) {
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
async function executeRemove(item) {
    await apiDelete(`/api/tianyan/documents/${item.documentId}?requestId=${encodeURIComponent(randomRequestId('tianyan-delete'))}`);
    notice.value = `审查文档“${item.title}”已标记删除`;
    await refresh();
}
function openReview(item) {
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
async function retryParse(item) {
    openConfirm({
        title: '重新提交解析任务',
        message: '确认后会重新提交该审查文档的解析任务。',
        targetName: item.title,
        targetLabel: '文档名称',
        confirmText: '重试解析',
        action: () => executeRetryParse(item),
    });
}
async function executeRetryParse(item) {
    await apiPost(`/api/tianyan/documents/${item.documentId}/retry-parse?requestId=${encodeURIComponent(randomRequestId('tianyan-retry-parse'))}`);
    notice.value = `审查文档“${item.title}”已重新提交解析`;
    await refresh();
}
function parseMethodLabel(method) {
    if (method === 'MINERU_PRECISE') {
        return 'MinerU 精准解析';
    }
    return '原生解析';
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
    for: "tianyan-title",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "tianyan-title",
    name: "tianyanTitle",
    autocomplete: "off",
    placeholder: "可选，不填则使用文件名…",
});
(__VLS_ctx.title);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "tianyan-source",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "tianyan-source",
    name: "tianyanSource",
    autocomplete: "off",
    placeholder: "可选，例如：合同中心 / 邮件附件…",
});
(__VLS_ctx.source);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "tianyan-file",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.onSelectFile) },
    id: "tianyan-file",
    name: "tianyanFile",
    type: "file",
    accept: ".pdf,.doc,.docx,.txt,.md",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "tianyan-parse-method",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    id: "tianyan-parse-method",
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
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "note" },
});
(__VLS_ctx.maxUploadDocuments);
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
});
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
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openReview(item);
            } },
        ...{ class: "primary-btn" },
        type: "button",
        disabled: (Boolean(item.parseStatus && item.parseStatus !== 'COMPLETED')),
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
        });
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.remove(item);
            } },
        ...{ class: "warn-btn" },
        type: "button",
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.selectedDocument))
                    return;
                __VLS_ctx.openDocumentAsset(__VLS_ctx.selectedDocument, 'full.md');
            } },
        ...{ class: "ghost-btn" },
        type: "button",
        disabled: (!__VLS_ctx.selectedDocument.documentUrl),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.selectedDocument))
                    return;
                __VLS_ctx.openDocumentAsset(__VLS_ctx.selectedDocument, 'content_list_v2.json');
            } },
        ...{ class: "ghost-btn" },
        type: "button",
        disabled: (!__VLS_ctx.selectedDocument.documentUrl),
    });
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
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-list']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-item']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-status']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
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
            maxUploadDocuments: maxUploadDocuments,
            cleaningAvailable: cleaningAvailable,
            cleaningEnabled: cleaningEnabled,
            selectedDocument: selectedDocument,
            confirmState: confirmState,
            refresh: refresh,
            onSelectFile: onSelectFile,
            importDocument: importDocument,
            remove: remove,
            openReview: openReview,
            retryParse: retryParse,
            parseMethodLabel: parseMethodLabel,
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
