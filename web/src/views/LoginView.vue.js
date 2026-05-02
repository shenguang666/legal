import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { apiLogin, hasToken } from '../api/client';
const router = useRouter();
const tenantId = ref(localStorage.getItem('legal.tenantId') || '1001');
const username = ref(localStorage.getItem('legal.username') || 'user');
const password = ref('');
const notice = ref('');
const loading = ref(false);
if (hasToken()) {
    router.replace('/chat');
}
async function login() {
    const tenantIdValue = String(tenantId.value ?? '').trim();
    const usernameValue = username.value.trim();
    const passwordValue = password.value.trim();
    if (!tenantIdValue || !usernameValue || !passwordValue) {
        notice.value = '请完整填写租户、用户名和密码';
        return;
    }
    loading.value = true;
    notice.value = '';
    try {
        await apiLogin({
            tenantId: Number(tenantIdValue),
            username: usernameValue,
            password: passwordValue,
        });
        router.push('/chat');
    }
    catch (error) {
        notice.value = error instanceof Error ? error.message : '登录失败';
    }
    finally {
        loading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['feature-strip']} */ ;
/** @type {__VLS_StyleScopedClasses['feature-strip']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['credential-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['credential-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['credential-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['credential-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['login-hero']} */ ;
/** @type {__VLS_StyleScopedClasses['credential-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "login-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "login-hero card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "hero-copy" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "intro" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "feature-strip" },
    'aria-label': "系统能力",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.login) },
    ...{ class: "login-form" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "grid form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "tenantId",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "tenantId",
    name: "tenantId",
    type: "number",
    inputmode: "numeric",
    autocomplete: "off",
    placeholder: "例如 1001…",
});
(__VLS_ctx.tenantId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "username",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "username",
    name: "username",
    autocomplete: "username",
    spellcheck: "false",
    placeholder: "admin / user…",
});
(__VLS_ctx.username);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "password",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    id: "password",
    name: "password",
    type: "password",
    autocomplete: "current-password",
    placeholder: "请输入密码…",
});
(__VLS_ctx.password);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "primary-btn" },
    type: "submit",
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? '登录中…' : '进入系统');
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "credential-grid" },
    'aria-label': "演示账号",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
        'aria-live': "polite",
    });
    (__VLS_ctx.notice);
}
/** @type {__VLS_StyleScopedClasses['login-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['login-hero']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['hero-copy']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['intro']} */ ;
/** @type {__VLS_StyleScopedClasses['feature-strip']} */ ;
/** @type {__VLS_StyleScopedClasses['login-form']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['credential-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            tenantId: tenantId,
            username: username,
            password: password,
            notice: notice,
            loading: loading,
            login: login,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
