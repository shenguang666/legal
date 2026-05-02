import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { apiGet, apiPost, apiPostSse, randomRequestId } from '../api/client';
const route = useRoute();
const sessionId = ref('');
const messages = ref([]);
const question = ref('');
const notice = ref('');
const loading = ref(false);
const hotwordLoading = ref(false);
const latestCitations = ref([]);
const quickPrompts = ref([]);
const selectedHotwordKey = ref('');
let cancelStream = null;
onMounted(async () => {
    await loadQuickPrompts();
    const querySessionId = route.query.sessionId;
    const cacheSessionId = localStorage.getItem('legal.activeSessionId');
    sessionId.value = querySessionId || cacheSessionId || '';
    if (!sessionId.value) {
        await newSession();
        return;
    }
    await loadMessages();
});
async function newSession() {
    const data = await apiPost('/api/chat/session', {
        requestId: randomRequestId('chat-session'),
        title: `法律咨询-${new Date().toLocaleDateString()}`,
    });
    sessionId.value = data.sessionId;
    localStorage.setItem('legal.activeSessionId', data.sessionId);
    messages.value = [];
    notice.value = `新会话已创建：${data.sessionId}`;
}
async function loadMessages() {
    if (!sessionId.value) {
        return;
    }
    messages.value = await apiGet(`/api/chat/session/${sessionId.value}/messages`);
}
async function ask() {
    if (!question.value.trim() || !sessionId.value) {
        return;
    }
    loading.value = true;
    try {
        const payload = {
            sessionId: sessionId.value,
            question: question.value.trim(),
            requestId: randomRequestId('chat-ask'),
            hotwordKey: selectedHotwordKey.value || undefined,
        };
        const result = await apiPost('/api/chat/ask', payload);
        await loadMessages();
        question.value = '';
        latestCitations.value = result.citations || [];
        notice.value = `置信度：${result.confidence.toFixed(2)} ｜ ${result.warning}`;
    }
    catch (error) {
        notice.value = error instanceof Error ? error.message : '发送失败';
    }
    finally {
        loading.value = false;
    }
}
// 保留旧 ask()，新增流式按钮已指向 askStream()
async function askStream() {
    if (!question.value.trim() || !sessionId.value) {
        return;
    }
    if (cancelStream) {
        cancelStream();
        cancelStream = null;
    }
    loading.value = true;
    latestCitations.value = [];
    notice.value = '';
    const payload = {
        sessionId: sessionId.value,
        question: question.value.trim(),
        requestId: randomRequestId('chat-ask-stream'),
        hotwordKey: selectedHotwordKey.value || undefined,
    };
    const userMsg = {
        messageId: Date.now() - 1,
        role: 'user',
        content: payload.question,
        createdAt: new Date().toISOString(),
    };
    const assistantMsg = {
        messageId: Date.now(),
        role: 'assistant',
        content: '',
        thinking: '',
        createdAt: new Date().toISOString(),
    };
    messages.value = [...messages.value, userMsg, assistantMsg];
    const patchAssistant = () => {
        messages.value = messages.value.map((item) => (item.messageId === assistantMsg.messageId ? { ...assistantMsg } : item));
    };
    cancelStream = apiPostSse('/api/chat/ask/stream', payload, (evt) => {
        const name = evt.name;
        const data = evt.data?.data ?? evt.data;
        if (name === 'thinking') {
            assistantMsg.thinking = (assistantMsg.thinking || '') + String(data || '');
            patchAssistant();
        }
        else if (name === 'answer') {
            assistantMsg.content = (assistantMsg.content || '') + String(data || '');
            patchAssistant();
        }
        else if (name === 'citations') {
            try {
                latestCitations.value = typeof data === 'string' ? JSON.parse(data) : data;
            }
            catch {
                // ignore
            }
        }
        else if (name === 'done') {
            patchAssistant();
            loading.value = false;
            question.value = '';
            selectedHotwordKey.value = '';
            cancelStream = null;
        }
        else if (name === 'error') {
            loading.value = false;
            notice.value = evt.data?.message || '流式请求失败';
            cancelStream = null;
        }
    });
}
function stopStream() {
    if (cancelStream) {
        cancelStream();
        cancelStream = null;
        loading.value = false;
        notice.value = '已停止本次生成';
    }
}
function usePrompt(prompt) {
    question.value = prompt.content;
    selectedHotwordKey.value = prompt.hotwordKey;
}
function clearSelectedHotword() {
    selectedHotwordKey.value = '';
}
async function loadQuickPrompts() {
    hotwordLoading.value = true;
    try {
        quickPrompts.value = await apiGet('/api/hotwords/random?limit=3');
    }
    catch (error) {
        quickPrompts.value = [];
        notice.value = error instanceof Error ? `热词加载失败：${error.message}` : '热词加载失败，可手动输入问题';
    }
    finally {
        hotwordLoading.value = false;
    }
}
async function submitFeedback(messageId, helpful) {
    try {
        await apiPost('/api/feedback', {
            requestId: randomRequestId('feedback'),
            messageId,
            helpful,
            comment: helpful ? '回答清晰' : '需要更准确的依据',
        });
        notice.value = '反馈已记录';
    }
    catch (error) {
        notice.value = error instanceof Error ? error.message : '反馈失败';
    }
}
function formatTime(input) {
    if (!input) {
        return '';
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
/** @type {__VLS_StyleScopedClasses['chat-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-rail-head']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-rail-head']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-list']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['composer-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['insight-list']} */ ;
/** @type {__VLS_StyleScopedClasses['insight-list']} */ ;
/** @type {__VLS_StyleScopedClasses['insight-list']} */ ;
/** @type {__VLS_StyleScopedClasses['insight-list']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-layout']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "chat-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "left card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "head-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "subtle" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.newSession) },
    ...{ class: "ghost-btn" },
    type: "button",
});
if (!__VLS_ctx.messages.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty-state chat-empty" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "messages" },
        'aria-live': "polite",
    });
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.messages))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
            key: (item.messageId),
            ...{ class: "bubble" },
            ...{ class: (item.role === 'assistant' ? 'assistant' : 'user') },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "meta" },
        });
        (item.role === 'assistant' ? '助手' : '用户');
        (__VLS_ctx.formatTime(item.createdAt));
        if (item.role === 'assistant' && item.thinking) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.details, __VLS_intrinsicElements.details)({
                ...{ class: "thinking" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.summary, __VLS_intrinsicElements.summary)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({
                ...{ class: "thinking-text" },
            });
            (item.thinking);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "text" },
        });
        (item.content);
        if (item.role === 'assistant') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "feedback-line" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(!__VLS_ctx.messages.length))
                            return;
                        if (!(item.role === 'assistant'))
                            return;
                        __VLS_ctx.submitFeedback(item.messageId, true);
                    } },
                ...{ class: "ghost-btn" },
                type: "button",
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(!__VLS_ctx.messages.length))
                            return;
                        if (!(item.role === 'assistant'))
                            return;
                        __VLS_ctx.submitFeedback(item.messageId, false);
                    } },
                ...{ class: "warn-btn" },
                type: "button",
            });
        }
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "prompt-rail" },
    'aria-label': "快捷问题",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "prompt-rail-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.loadQuickPrompts) },
    ...{ class: "ghost-btn" },
    type: "button",
    disabled: (__VLS_ctx.hotwordLoading),
});
(__VLS_ctx.hotwordLoading ? '刷新中…' : '换一组');
if (__VLS_ctx.quickPrompts.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "prompt-list" },
    });
    for (const [prompt] of __VLS_getVForSourceType((__VLS_ctx.quickPrompts))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.quickPrompts.length))
                        return;
                    __VLS_ctx.usePrompt(prompt);
                } },
            key: (prompt.hotwordId),
            type: "button",
            ...{ class: "prompt-chip" },
        });
        (prompt.content);
    }
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "prompt-empty" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "composer" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    for: "chat-question",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
    ...{ onInput: (__VLS_ctx.clearSelectedHotword) },
    id: "chat-question",
    value: (__VLS_ctx.question),
    name: "question",
    rows: "4",
    autocomplete: "off",
    placeholder: "输入你的法律问题，例如：劳动合同到期未续签是否有补偿？…",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "composer-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.question.trim().length);
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.stopStream) },
        ...{ class: "ghost-btn" },
        type: "button",
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.askStream) },
    ...{ class: "primary-btn" },
    type: "button",
    disabled: (__VLS_ctx.loading || !__VLS_ctx.question.trim()),
});
(__VLS_ctx.loading ? '生成中…' : '发送问题');
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "note" },
});
(__VLS_ctx.sessionId || '尚未创建');
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "right card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "tag" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "insight-list" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
if (__VLS_ctx.latestCitations.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "citation-panel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    for (const [item, idx] of __VLS_getVForSourceType((__VLS_ctx.latestCitations))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
            key: (`${item.documentId}-${idx}`),
            ...{ class: "citation-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "citation-source" },
        });
        (item.source);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "citation-fragment" },
        });
        (item.fragment);
    }
}
if (__VLS_ctx.notice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "notice" },
        'aria-live': "polite",
    });
    (__VLS_ctx.notice);
}
/** @type {__VLS_StyleScopedClasses['chat-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['left']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['head-row']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['subtle']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-state']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['messages']} */ ;
/** @type {__VLS_StyleScopedClasses['bubble']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-text']} */ ;
/** @type {__VLS_StyleScopedClasses['text']} */ ;
/** @type {__VLS_StyleScopedClasses['feedback-line']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['warn-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-rail']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-rail-head']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-list']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-chip']} */ ;
/** @type {__VLS_StyleScopedClasses['prompt-empty']} */ ;
/** @type {__VLS_StyleScopedClasses['composer']} */ ;
/** @type {__VLS_StyleScopedClasses['composer-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['right']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['insight-list']} */ ;
/** @type {__VLS_StyleScopedClasses['citation-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['citation-item']} */ ;
/** @type {__VLS_StyleScopedClasses['citation-source']} */ ;
/** @type {__VLS_StyleScopedClasses['citation-fragment']} */ ;
/** @type {__VLS_StyleScopedClasses['notice']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            sessionId: sessionId,
            messages: messages,
            question: question,
            notice: notice,
            loading: loading,
            hotwordLoading: hotwordLoading,
            latestCitations: latestCitations,
            quickPrompts: quickPrompts,
            newSession: newSession,
            askStream: askStream,
            stopStream: stopStream,
            usePrompt: usePrompt,
            clearSelectedHotword: clearSelectedHotword,
            loadQuickPrompts: loadQuickPrompts,
            submitFeedback: submitFeedback,
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
