const __VLS_props = withDefaults(defineProps(), {
    targetName: '',
    targetLabel: '对象',
    confirmText: '确认',
    eyebrow: '操作确认',
    danger: false,
});
const emit = defineEmits();
const titleId = `confirm-${Math.random().toString(36).slice(2)}`;
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({
    targetName: '',
    targetLabel: '对象',
    confirmText: '确认',
    eyebrow: '操作确认',
    danger: false,
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['confirm-head']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-target']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-target']} */ ;
// CSS variable injection 
// CSS variable injection end 
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
if (__VLS_ctx.modelValue) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "confirm-panel" },
        role: "dialog",
        'aria-modal': "true",
        'aria-labelledby': (__VLS_ctx.titleId),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "confirm-modal card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "confirm-head" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "tag" },
    });
    (__VLS_ctx.eyebrow);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
        id: (__VLS_ctx.titleId),
    });
    (__VLS_ctx.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.modelValue))
                    return;
                __VLS_ctx.emit('cancel');
            } },
        ...{ class: "ghost-btn" },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "confirm-message" },
    });
    (__VLS_ctx.message);
    if (__VLS_ctx.targetName) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "confirm-target" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.targetLabel);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (__VLS_ctx.targetName);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "confirm-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.modelValue))
                    return;
                __VLS_ctx.emit('cancel');
            } },
        ...{ class: "ghost-btn" },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.modelValue))
                    return;
                __VLS_ctx.emit('confirm');
            } },
        ...{ class: (__VLS_ctx.danger ? 'warn-btn' : 'primary-btn') },
        type: "button",
    });
    (__VLS_ctx.confirmText);
}
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['confirm-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-modal']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-head']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-message']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-target']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            emit: emit,
            titleId: titleId,
        };
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
; /* PartiallyEnd: #4569/main.vue */
