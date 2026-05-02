import { createRouter, createWebHistory } from 'vue-router';
import { currentRole, hasToken } from '../api/client';
import LoginView from '../views/LoginView.vue';
import ChatView from '../views/ChatView.vue';
import SessionsView from '../views/SessionsView.vue';
import KnowledgeView from '../views/KnowledgeView.vue';
import RiskRuleView from '../views/RiskRuleView.vue';
import RagMetricsView from '../views/RagMetricsView.vue';
import TianyanView from '../views/TianyanView.vue';
import ContractReviewView from '../views/ContractReviewView.vue';
const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            redirect: () => {
                return hasToken() ? '/chat' : '/login';
            },
        },
        { path: '/login', component: LoginView, meta: { publicOnly: true } },
        { path: '/chat', component: ChatView },
        { path: '/sessions', component: SessionsView },
        { path: '/knowledge', component: KnowledgeView, meta: { adminOnly: true } },
        { path: '/risk-rules', component: RiskRuleView, meta: { adminOnly: true } },
        { path: '/rag-metrics', component: RagMetricsView, meta: { adminOnly: true } },
        { path: '/tianyan', component: TianyanView, meta: { adminOnly: true } },
        { path: '/tianyan/reviews/:documentId', component: ContractReviewView, meta: { adminOnly: true } },
    ],
    scrollBehavior() {
        return { top: 0 };
    },
});
router.beforeEach((to) => {
    if (to.meta.publicOnly && hasToken()) {
        return '/chat';
    }
    if (!to.meta.publicOnly && !hasToken()) {
        return '/login';
    }
    if (to.meta.adminOnly && currentRole() !== 'ADMIN') {
        return '/chat';
    }
    return true;
});
export default router;
