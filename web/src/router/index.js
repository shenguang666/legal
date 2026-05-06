import { createRouter, createWebHistory } from 'vue-router';
import { currentRole, hasToken } from '../api/client';
import LoginView from '../views/LoginView.vue';
import ChatView from '../views/ChatView.vue';
import SessionsView from '../views/SessionsView.vue';
import KnowledgeView from '../views/KnowledgeView.vue';
import HotwordView from '../views/HotwordView.vue';
import RiskRuleView from '../views/RiskRuleView.vue';
import RagMetricsView from '../views/RagMetricsView.vue';
import TokenUsageMetricsView from '../views/TokenUsageMetricsView.vue';
import TianyanView from '../views/TianyanView.vue';
import ContractReviewView from '../views/ContractReviewView.vue';
import SmartCourtView from '../views/SmartCourtView.vue';
const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: '/login' },
        { path: '/login', component: LoginView, meta: { publicOnly: true } },
        { path: '/chat', component: ChatView },
        { path: '/sessions', component: SessionsView },
        { path: '/knowledge', component: KnowledgeView, meta: { adminOnly: true } },
        { path: '/hotwords', component: HotwordView, meta: { adminOnly: true } },
        { path: '/risk-rules', component: RiskRuleView, meta: { adminOnly: true } },
        { path: '/rag-metrics', component: RagMetricsView, meta: { adminOnly: true } },
        { path: '/token-usage-metrics', component: TokenUsageMetricsView, meta: { adminOnly: true } },
        { path: '/tianyan', component: TianyanView },
        { path: '/tianyan/reviews/:documentId', component: ContractReviewView },
        { path: '/smart-court', component: SmartCourtView },
    ],
    scrollBehavior() {
        return { top: 0 };
    },
});
router.beforeEach((to) => {
    if (!to.meta.publicOnly && !hasToken()) {
        return '/login';
    }
    if (to.meta.adminOnly && currentRole() !== 'ADMIN') {
        return '/chat';
    }
    return true;
});
export default router;
