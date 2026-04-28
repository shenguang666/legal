import { createRouter, createWebHistory } from 'vue-router';
import { currentRole, hasToken } from '../api/client';
import LoginView from '../views/LoginView.vue';
import ChatView from '../views/ChatView.vue';
import SessionsView from '../views/SessionsView.vue';
import KnowledgeView from '../views/KnowledgeView.vue';
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
