// src/stores/userStore.js
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useUserStore = defineStore('user', () => {
    // 状态
    const userData = ref({
        userId: '',
        name: '',
        goal: '',
        currentWeight: 0,
        targetWeight: 0
    });

    const isLoggedIn = ref(false);
    const loading = ref(false);
    const errorMessage = ref('');
    const isInitialized = ref(false); // 新增：标记store是否已初始化

    // 初始化store状态
    const initializeStore = () => {
        if (isInitialized.value) return;
        // 从localStorage恢复状态
        const storedLoginStatus = localStorage.getItem('isLoggedIn') === 'true';
        const storedUserData = localStorage.getItem('userProfile');
        
        if (storedLoginStatus && storedUserData) {
            try {
                const userDataObj = JSON.parse(storedUserData);
                userData.value = userDataObj;
                isLoggedIn.value = true;
            } catch (e) {
                console.error('恢复用户状态失败:', e);
                // 清除损坏的数据
                localStorage.removeItem('isLoggedIn');
                localStorage.removeItem('userProfile');
            }
        }
        isInitialized.value = true;
    };

    // 动作
    const setUserData = (data) => {
        userData.value = { ...userData.value, ...data };
        isLoggedIn.value = true;
        isInitialized.value = true;
        // 保存到localStorage
        localStorage.setItem('userProfile', JSON.stringify(userData.value));
        localStorage.setItem('isLoggedIn', 'true');
    };

    const logout = () => {
        // 清除本地状态
        userData.value = {
            userId: '',
            name: '',
            goal: '',
            currentWeight: 0,
            targetWeight: 0
        };
        isLoggedIn.value = false;
        isInitialized.value = false;
        errorMessage.value = '';
        
        // 清除localStorage
        localStorage.removeItem('userProfile');
        localStorage.removeItem('isLoggedIn');

    };

    const setLoading = (status) => {
        loading.value = status;
    };

    const setError = (message) => {
        errorMessage.value = message;
    };

    const clearError = () => {
        errorMessage.value = '';
    };

    // 计算属性
    const isAuthenticated = computed(() => {
        return isLoggedIn.value && userData.value.userId;
    });

    return {
        userData,
        isLoggedIn,
        loading,
        errorMessage,
        isInitialized,
        isAuthenticated,
        initializeStore,
        setUserData,
        logout,
        setLoading,
        setError,
        clearError
    };
});
