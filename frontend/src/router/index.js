// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '../views/DashboardView.vue';
import RecipesView from '../views/RecipesView.vue';
import DiaryView from '../views/DiaryView.vue';
import ProfileView from '../views/ProfileView.vue';
import AIConsultView from '../views/AIConsultView.vue';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
// 添加FitnessView导入
import FitnessView from '../views/FitnessView.vue';
// 添加MentalHealthView导入
import MentalHealthView from '../views/MentalHealthView.vue';
// 添加 CalendarView导入
import CalendarView from '../views/CalendarView.vue';
// 添加 AIRecipeResultView导入
import AIRecipeResultView from '../views/AIRecipeResultView.vue';
import { useUserStore } from '../stores/userStore';

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
    {
        path: '/ai-consult',
        name: 'AIConsult',
        component: AIConsultView,
        meta: { requiresAuth: true }
    },
    // 添加FitnessView路由
    {
        path: '/fitness',
        name: 'Fitness',
        component: FitnessView,
        meta: { requiresAuth: true }
    },
    // 添加MentalHealthView 路由
    {
        path: '/mental-health',
        name: 'MentalHealth',
        component: MentalHealthView,
        meta: { requiresAuth: true }
    },
    // 添加 CalendarView 路由
    {
        path: '/calendar',
        name: 'Calendar',
        component: CalendarView,
        meta: { requiresAuth: true }
    },
    // AI 食谱推荐结果页面（新标签页打开）
    {
        path: '/ai-recipe-result',
        name: 'AIRecipeResult',
        component: AIRecipeResultView,
        meta: { requiresAuth: true } // 需要认证，因为要调用 API
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// 添加路由守卫
router.beforeEach((to, from, next) => {
    const userStore = useUserStore();

    // 检查 localStorage 中的登录状态
    const storedLoginStatus = localStorage.getItem('isLoggedIn') === 'true';
    const storedUserData = localStorage.getItem('userProfile');
    
    // 如果 store 中没有登录状态但 localStorage 中有，则恢复状态
    if (!userStore.isLoggedIn && storedLoginStatus && storedUserData) {
        try {
            const userData = JSON.parse(storedUserData);
            userStore.setUserData(userData);
        } catch (e) {
            console.error('恢复用户状态失败:', e);
            // 清除损坏的数据
            localStorage.removeItem('isLoggedIn');
            localStorage.removeItem('userProfile');
        }
    }
    
    // 检查是否需要认证
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
        next('/login');
    }
    // 检查是否是游客页面（登录/注册页）
    else if (to.meta.requiresGuest && userStore.isLoggedIn) {
        next('/');
    }
    // 其他情况正常放行
    else {
        next();
    }
});

export default router;