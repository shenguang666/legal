import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { apiDelete, apiGet, apiPostForm, randomRequestId } from '../api/client';
const router = useRouter();
const title = ref('');
const source = ref('');
const selectedFile = ref(null);
const documents = ref([]);
const notice = ref('');
onMounted(refresh);
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
    const formData = new FormData();
    formData.append('requestId', randomRequestId('tianyan-import'));
    formData.append('file', selectedFile.value);
    if (title.value.trim()) {
        formData.append('title', title.value.trim());
    }
    if (source.value.trim()) {
        formData.append('source', source.value.trim());
    }
    await apiPostForm('/api/tianyan/documents/import', formData);
    title.value = '';
    source.value = '';
    selectedFile.value = null;
    notice.value = '审查文档已导入，可立即发起天眼审查';
    await refresh();
}
async function remove(documentId) {
    if (!window.confirm(`确认删除审查文档 ${documentId}？`)) {
        return;
    }
    await apiDelete(`/api/tianyan/documents/${documentId}?requestId=${encodeURIComponent(randomRequestId('tianyan-delete'))}`);
    notice.value = `审查文档 ${documentId} 已标记删除`;
    await refresh();
}
function openReview(item) {
    router.push({
        path: `/tianyan/reviews/${item.documentId}`,
        query: {
            title: item.title,
        },
    });
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['doc-item']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-item']} */ ;
/** @type {__VLS_StyleScopedClasses['doc-item']} */ ;
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    (item.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (item.source);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "doc-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openReview(item);
            } },
        ...{ class: "primary-btn" },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.remove(item.documentId);
            } },
        ...{ class: "warn-btn" },
        type: "button",
    });
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
/** @type {__VLS_StyleScopedClasses['knowledge-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
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
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['warn-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            title: title,
            source: source,
            documents: documents,
            notice: notice,
            refresh: refresh,
            onSelectFile: onSelectFile,
            importDocument: importDocument,
            remove: remove,
            openReview: openReview,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
