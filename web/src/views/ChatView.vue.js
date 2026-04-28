import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { apiGet, apiPost, apiPostSse, randomRequestId } from '../api/client';
const route = useRoute();
const sessionId = ref('');
const messages = ref([]);
const question = ref('');
const notice = ref('');
const loading = ref(false);
const latestCitations = ref([]);
let cancelStream = null;
onMounted(async () => {
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
            cancelStream = null;
        }
        else if (name === 'error') {
            loading.value = false;
            notice.value = evt.data?.message || '流式请求失败';
            cancelStream = null;
        }
    });
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
    return new Date(input).toLocaleString();
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.newSession) },
    ...{ class: "ghost-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "messages" },
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
                    if (!(item.role === 'assistant'))
                        return;
                    __VLS_ctx.submitFeedback(item.messageId, true);
                } },
            ...{ class: "ghost-btn" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(item.role === 'assistant'))
                        return;
                    __VLS_ctx.submitFeedback(item.messageId, false);
                } },
            ...{ class: "warn-btn" },
        });
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "composer" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.question),
    rows: "4",
    placeholder: "输入你的法律问题，例如：劳动合同到期未续签是否有补偿？",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.askStream) },
    ...{ class: "primary-btn" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? '生成中...' : '发送问题(流式)');
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "note" },
});
(__VLS_ctx.sessionId || '尚未创建');
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "right card panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({});
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
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['messages']} */ ;
/** @type {__VLS_StyleScopedClasses['bubble']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking']} */ ;
/** @type {__VLS_StyleScopedClasses['thinking-text']} */ ;
/** @type {__VLS_StyleScopedClasses['text']} */ ;
/** @type {__VLS_StyleScopedClasses['feedback-line']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['warn-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['composer']} */ ;
/** @type {__VLS_StyleScopedClasses['primary-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['note']} */ ;
/** @type {__VLS_StyleScopedClasses['right']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
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
            latestCitations: latestCitations,
            newSession: newSession,
            askStream: askStream,
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
