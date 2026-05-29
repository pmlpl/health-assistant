// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '../views/DashboardView.vue';
import RecipesView from '../views/RecipesView.vue';
import DiaryView from '../views/DiaryView.vue';
import ProfileView from '../views/ProfileView.vue';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
// 添加FitnessView导入
import FitnessView from '../views/FitnessView.vue';
// 添加 CalendarView导入
import CalendarView from '../views/CalendarView.vue';
import HealthReportView from '../views/HealthReportView.vue';
import AIRecipeResultView from '../views/AIRecipeResultView.vue';
import UserGuideView from '../views/UserGuideView.vue';
import { useUserStore } from '../stores/userStore';
import { getAuthToken } from '../api/healthApi';

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: LoginView,
        meta: { requiresGuest: true }
    },
    {
        path: '/register',
        name: 'Register',
        component: RegisterView,
        meta: { requiresGuest: true }
    },
    {
        path: '/',
        name: 'Dashboard',
        component: DashboardView,
        meta: { requiresAuth: true }
    },
    {
        path: '/recipes',
        name: 'Recipes',
        component: RecipesView,
        meta: { requiresAuth: true }
    },
    {
        path: '/diary',
        name: 'Diary',
        component: DiaryView,
        meta: { requiresAuth: true }
    },
    {
        path: '/profile',
        name: 'Profile',
        component: ProfileView,
        meta: { requiresAuth: true }
    },
    // 添加FitnessView 路由
    {
        path: '/fitness',
        name: 'Fitness',
        component: FitnessView,
        meta: { requiresAuth: true }
    },
    {
        path: '/calendar',
        name: 'Calendar',
        component: CalendarView,
        meta: { requiresAuth: true }
    },
    {
        path: '/health-report',
        name: 'HealthReport',
        component: HealthReportView,
        meta: { requiresAuth: true }
    },
    // AI 食谱推荐结果页面（新标签页打开）
    {
        path: '/ai-recipe-result',
        name: 'AIRecipeResult',
        component: AIRecipeResultView,
        meta: { requiresAuth: true }
    },
    {
        path: '/guide',
        name: 'UserGuide',
        component: UserGuideView,
        meta: { requiresAuth: true }
    },
    {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('../views/NotFoundView.vue'),
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// 添加路由守卫
router.beforeEach((to, from) => {
    const userStore = useUserStore();

    // 检查 localStorage 中的登录状态
    const storedLoginStatus = localStorage.getItem('isLoggedIn') === 'true';
    const storedUserData = localStorage.getItem('userProfile');
    const hasToken = !!getAuthToken();

    if (!userStore.isLoggedIn && storedLoginStatus && storedUserData && hasToken) {
        try {
            const userData = JSON.parse(storedUserData);
            userStore.setUserData(userData);
        } catch (e) {
            console.error('恢复用户状态失败:', e);
            localStorage.removeItem('isLoggedIn');
            localStorage.removeItem('userProfile');
        }
    } else if (storedLoginStatus && !hasToken) {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userProfile');
    }

    if (to.meta.requiresAuth && (!userStore.isLoggedIn || !getAuthToken())) {
        return '/login';
    }
    // 检查是否是游客页面（登录/注册页）
    else if (to.meta.requiresGuest && userStore.isLoggedIn) {
        return '/';
    }
    // 其他情况正常放行
    else {
        return undefined;
    }
});

export default router;