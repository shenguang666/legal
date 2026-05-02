import { computed } from 'vue';
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router';
import { apiLogout, currentRole, hasToken } from './api/client';
const router = useRouter();
const route = useRoute();
const authState = computed(() => {
    route.fullPath;
    return {
        tenantId: localStorage.getItem('legal.tenantId') || '1001',
        userId: localStorage.getItem('legal.userId') || '-',
        displayName: localStorage.getItem('legal.displayName') || localStorage.getItem('legal.username') || '未登录',
        roleCode: currentRole(),
    };
});
const tenantId = computed(() => authState.value.tenantId);
const userId = computed(() => authState.value.userId);
const displayName = computed(() => authState.value.displayName);
const roleCode = computed(() => authState.value.roleCode);
const isAdmin = computed(() => roleCode.value === 'ADMIN');
async function logout() {
    if (hasToken()) {
        await apiLogout();
    }
    router.push('/login');
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['skip-link']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['identity']} */ ;
/** @type {__VLS_StyleScopedClasses['identity-avatar']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['menu']} */ ;
/** @type {__VLS_StyleScopedClasses['identity']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "shell-glow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.a, __VLS_intrinsicElements.a)({
    ...{ class: "skip-link" },
    href: "#main-content",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "topbar card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand-block" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "eyebrow" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "brand" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "brand-subtitle" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.nav, __VLS_intrinsicElements.nav)({
    ...{ class: "menu" },
    'aria-label': "主导航",
});
const __VLS_0 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    to: "/chat",
    ...{ class: "menu-link" },
}));
const __VLS_2 = __VLS_1({
    to: "/chat",
    ...{ class: "menu-link" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
var __VLS_3;
const __VLS_4 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    to: "/sessions",
    ...{ class: "menu-link" },
}));
const __VLS_6 = __VLS_5({
    to: "/sessions",
    ...{ class: "menu-link" },
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
var __VLS_7;
if (__VLS_ctx.isAdmin) {
    const __VLS_8 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
        to: "/knowledge",
        ...{ class: "menu-link" },
    }));
    const __VLS_10 = __VLS_9({
        to: "/knowledge",
        ...{ class: "menu-link" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_9));
    __VLS_11.slots.default;
    var __VLS_11;
}
if (__VLS_ctx.isAdmin) {
    const __VLS_12 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
        to: "/hotwords",
        ...{ class: "menu-link" },
    }));
    const __VLS_14 = __VLS_13({
        to: "/hotwords",
        ...{ class: "menu-link" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_13));
    __VLS_15.slots.default;
    var __VLS_15;
}
if (__VLS_ctx.isAdmin) {
    const __VLS_16 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
        to: "/risk-rules",
        ...{ class: "menu-link" },
    }));
    const __VLS_18 = __VLS_17({
        to: "/risk-rules",
        ...{ class: "menu-link" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_17));
    __VLS_19.slots.default;
    var __VLS_19;
}
if (__VLS_ctx.isAdmin) {
    const __VLS_20 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
        to: "/rag-metrics",
        ...{ class: "menu-link" },
    }));
    const __VLS_22 = __VLS_21({
        to: "/rag-metrics",
        ...{ class: "menu-link" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_21));
    __VLS_23.slots.default;
    var __VLS_23;
}
if (__VLS_ctx.isAdmin) {
    const __VLS_24 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
        to: "/tianyan",
        ...{ class: "menu-link" },
    }));
    const __VLS_26 = __VLS_25({
        to: "/tianyan",
        ...{ class: "menu-link" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_25));
    __VLS_27.slots.default;
    var __VLS_27;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "identity" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "identity-avatar" },
});
(__VLS_ctx.displayName.slice(0, 1).toUpperCase());
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.displayName);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.roleCode);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.tenantId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.userId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.logout) },
    ...{ class: "ghost-btn" },
    type: "button",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    id: "main-content",
    ...{ class: "page card" },
});
const __VLS_28 = {}.RouterView;
/** @type {[typeof __VLS_components.RouterView, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({}));
const __VLS_30 = __VLS_29({}, ...__VLS_functionalComponentArgsRest(__VLS_29));
/** @type {__VLS_StyleScopedClasses['shell']} */ ;
/** @type {__VLS_StyleScopedClasses['shell-glow']} */ ;
/** @type {__VLS_StyleScopedClasses['skip-link']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['brand-block']} */ ;
/** @type {__VLS_StyleScopedClasses['eyebrow']} */ ;
/** @type {__VLS_StyleScopedClasses['brand']} */ ;
/** @type {__VLS_StyleScopedClasses['brand-subtitle']} */ ;
/** @type {__VLS_StyleScopedClasses['menu']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-link']} */ ;
/** @type {__VLS_StyleScopedClasses['identity']} */ ;
/** @type {__VLS_StyleScopedClasses['identity-avatar']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['page']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            RouterView: RouterView,
            tenantId: tenantId,
            userId: userId,
            displayName: displayName,
            roleCode: roleCode,
            isAdmin: isAdmin,
            logout: logout,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
