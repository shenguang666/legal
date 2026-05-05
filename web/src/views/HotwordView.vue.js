import { onMounted, reactive, ref } from 'vue';
import { apiDelete, apiGet, apiPost, randomRequestId } from '../api/client';
import ConfirmDialog from '../components/ConfirmDialog.vue';
const hotwords = ref([]);
const notice = ref('');
const error = ref('');
const editingId = ref(null);
const page = reactive({ total: 0, pageNo: 1, pageSize: 10 });
const form = reactive({ hotwordKey: '', content: '', presetAnswer: '', category: '', weight: 0, sortOrder: 0, enabled: true });
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
onMounted(loadHotwords);
async function loadHotwords() {
    error.value = '';
    try {
        const result = await apiGet(`/api/hotwords?pageNo=${page.pageNo}&pageSize=${page.pageSize}`);
        hotwords.value = result.items || [];
        page.total = result.total;
        page.pageNo = result.pageNo;
        page.pageSize = result.pageSize;
    }
    catch (err) {
        error.value = err instanceof Error ? `热词列表加载失败：${err.message}` : '热词列表加载失败';
    }
}
async function submitHotword() {
    error.value = '';
    if (!form.content.trim()) {
        error.value = '请填写热词内容';
        return;
    }
    if (!form.presetAnswer.trim()) {
        error.value = '请填写预设答案';
        return;
    }
    const payload = {
        requestId: randomRequestId(editingId.value ? 'hotword-update' : 'hotword-create'),
        hotwordKey: form.hotwordKey.trim() || null,
        content: form.content.trim(),
        presetAnswer: form.presetAnswer.trim(),
        category: form.category.trim() || null,
        weight: Number.isFinite(form.weight) ? form.weight : 0,
        sortOrder: Number.isFinite(form.sortOrder) ? form.sortOrder : 0,
        enabled: form.enabled,
    };
    try {
        if (editingId.value) {
            await apiPost(`/api/hotwords/${editingId.value}`, payload);
            notice.value = '热词已更新，缓存将在下一次聊天页刷新时生效';
        }
        else {
            await apiPost('/api/hotwords', payload);
            notice.value = '热词已新增，缓存将在下一次聊天页刷新时生效';
        }
        resetForm();
        await loadHotwords();
    }
    catch (err) {
        error.value = err instanceof Error ? err.message : '热词保存失败';
    }
}
function beginEdit(item) {
    editingId.value = item.hotwordId;
    form.hotwordKey = item.hotwordKey || '';
    form.content = item.content;
    form.presetAnswer = item.presetAnswer || '';
    form.category = item.category || '';
    form.weight = item.weight || 0;
    form.sortOrder = item.sortOrder || 0;
    form.enabled = item.enabled;
    error.value = '';
}
function resetForm() {
    editingId.value = null;
    form.hotwordKey = '';
    form.content = '';
    form.presetAnswer = '';
    form.category = '';
    form.weight = 0;
    form.sortOrder = 0;
    form.enabled = true;
    error.value = '';
}
async function toggleHotword(item) {
    error.value = '';
    try {
        await apiPost(`/api/hotwords/${item.hotwordId}/status`, {
            requestId: randomRequestId('hotword-status'),
            enabled: !item.enabled,
        });
        notice.value = `热词已${item.enabled ? '停用' : '启用'}`;
        await loadHotwords();
    }
    catch (err) {
        error.value = err instanceof Error ? err.message : '热词状态更新失败';
    }
}
async function removeHotword(item) {
    openConfirm({
        title: '删除热词',
        message: '确认后会删除该聊天快捷问题热词。',
        targetName: item.content,
        targetLabel: '热词内容',
        confirmText: '确认删除',
        danger: true,
        action: () => executeRemoveHotword(item),
    });
}
async function executeRemoveHotword(item) {
    error.value = '';
    try {
        await apiDelete(`/api/hotwords/${item.hotwordId}?requestId=${encodeURIComponent(randomRequestId('hotword-delete'))}`);
        notice.value = '热词已删除';
        await loadHotwords();
    }
    catch (err) {
        error.value = err instanceof Error ? err.message : '热词删除失败';
    }
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
function prevPage() {
    if (page.pageNo <= 1) {
        return;
    }
    page.pageNo -= 1;
    loadHotwords();
}
function nextPage() {
    if (page.pageNo * page.pageSize >= page.total) {
        return;
    }
    page.pageNo += 1;
    loadHotwords();
}
function formatTime(input) {
    if (!input) {
        return '-';
    }
    return new Intl.DateTimeFormat('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    }).format(new Date(input));
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['hotword-card']} */ ;
/** @type {__VLS_StyleScopedClasses['item-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['pager']} */ ;
/** @type {__VLS_StyleScopedClasses['pager']} */ ;
/** @type {__VLS_StyleScopedClasses['hotword-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['item-top']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "hotword-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel hero-panel" },
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.loadHotwords) },
    ...{ class: "ghost-btn" },
    type: "button",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "hotword-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel form-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
(__VLS_ctx.editingId ? '编辑热词' : '新增热词');
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.editingId ? '更新快捷问题' : '创建快捷问题');
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.resetForm) },
    ...{ class: "ghost-btn" },
    type: "button",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-stack" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "hotword-content",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
    id: "hotword-content",
    value: (__VLS_ctx.form.content),
    name: "content",
    rows: "4",
    maxlength: "255",
    placeholder: "例如：劳动合同到期未续签是否有经济补偿？",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "hotword-key",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "hotword-key",
    name: "hotwordKey",
    maxlength: "64",
    placeholder: "例如：labor_contract_expire_compensation",
    autocomplete: "off",
});
(__VLS_ctx.form.hotwordKey);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "hotword-preset-answer",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
    id: "hotword-preset-answer",
    value: (__VLS_ctx.form.presetAnswer),
    name: "presetAnswer",
    rows: "6",
    maxlength: "4000",
    placeholder: "命中该热词标记时，askStream 会直接返回这里配置的答案。",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "grid form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "hotword-category",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "hotword-category",
    name: "category",
    placeholder: "劳动合同",
    autocomplete: "off",
});
(__VLS_ctx.form.category);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "hotword-weight",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "hotword-weight",
    name: "weight",
    type: "number",
    min: "0",
    step: "1",
});
(__VLS_ctx.form.weight);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "hotword-sort-order",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "hotword-sort-order",
    name: "sortOrder",
    type: "number",
    min: "0",
    step: "1",
});
(__VLS_ctx.form.sortOrder);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "selection-chip hotword-toggle" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    name: "enabled",
    type: "checkbox",
});
(__VLS_ctx.form.enabled);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.submitHotword) },
    ...{ class: "primary-btn" },
    type: "button",
});
(__VLS_ctx.editingId ? '保存修改' : '新增热词');
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
        'aria-live': "polite",
    });
    (__VLS_ctx.notice);
}
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "error-text" },
        'aria-live': "assertive",
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "card panel list-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "status-pill" },
});
(__VLS_ctx.page.total);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-list" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.hotwords))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (item.hotwordId),
        ...{ class: "item-card hotword-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "item-top" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    (item.content);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
    (item.category || '未分类');
    (item.hotwordKey);
    (item.weight);
    (item.sortOrder);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "status-pill" },
        'data-status': (item.enabled ? 'ENABLED' : 'DISABLED'),
    });
    (item.enabled ? 'ENABLED' : 'DISABLED');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "note" },
    });
    (__VLS_ctx.formatTime(item.updatedAt));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "item-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.beginEdit(item);
            } },
        ...{ class: "ghost-btn" },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.toggleHotword(item);
            } },
        ...{ class: "ghost-btn" },
        type: "button",
    });
    (item.enabled ? '停用' : '启用');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.removeHotword(item);
            } },
        ...{ class: "warn-btn" },
        type: "button",
    });
}
if (!__VLS_ctx.hotwords.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty-state" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "pager" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.prevPage) },
    ...{ class: "ghost-btn" },
    type: "button",
    disabled: (__VLS_ctx.page.pageNo <= 1),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.page.pageNo);
(__VLS_ctx.page.total);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.nextPage) },
    ...{ class: "ghost-btn" },
    type: "button",
    disabled: (__VLS_ctx.page.pageNo * __VLS_ctx.page.pageSize >= __VLS_ctx.page.total),
});
/** @type {[typeof ConfirmDialog, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(ConfirmDialog, new ConfirmDialog({
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
const __VLS_1 = __VLS_0({
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
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onCancel: (__VLS_ctx.closeConfirm)
};
const __VLS_7 = {
    onConfirm: (__VLS_ctx.confirmAction)
};
var __VLS_2;
/** @type {__VLS_StyleScopedClasses['hotword-page']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['hotword-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['form-stack']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['selection-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['hotword-toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
/** @type {__VLS_StyleScopedClasses['error-text']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['list-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['header-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['item-list']} */ ;
/** @type {__VLS_StyleScopedClasses['item-card']} */ ;
/** @type {__VLS_StyleScopedClasses['hotword-card']} */ ;
/** @type {__VLS_StyleScopedClasses['item-top']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['item-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['warn-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-state']} */ ;
/** @type {__VLS_StyleScopedClasses['pager']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ConfirmDialog: ConfirmDialog,
            hotwords: hotwords,
            notice: notice,
            error: error,
            editingId: editingId,
            page: page,
            form: form,
            confirmState: confirmState,
            loadHotwords: loadHotwords,
            submitHotword: submitHotword,
            beginEdit: beginEdit,
            resetForm: resetForm,
            toggleHotword: toggleHotword,
            removeHotword: removeHotword,
            closeConfirm: closeConfirm,
            confirmAction: confirmAction,
            prevPage: prevPage,
            nextPage: nextPage,
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
