// src/stores/userStore.js
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { setAuthToken, getAuthToken } from '../api/healthApi';

export const useUserStore = defineStore('user', () => {
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
    const isInitialized = ref(false);

    const initializeStore = () => {
        if (isInitialized.value) return;
        const storedLoginStatus = localStorage.getItem('isLoggedIn') === 'true';
        const storedUserData = localStorage.getItem('userProfile');
        const storedToken = getAuthToken();

        if (storedLoginStatus && storedUserData && storedToken) {
            try {
                userData.value = JSON.parse(storedUserData);
                isLoggedIn.value = true;
            } catch (e) {
                console.error('恢复用户状态失败:', e);
                localStorage.removeItem('isLoggedIn');
                localStorage.removeItem('userProfile');
                setAuthToken(null);
            }
        } else if (!storedToken) {
            localStorage.removeItem('isLoggedIn');
            localStorage.removeItem('userProfile');
        }
        isInitialized.value = true;
    };

    const setUserData = (data, token) => {
        userData.value = { ...userData.value, ...data };
        isLoggedIn.value = true;
        isInitialized.value = true;
        localStorage.setItem('userProfile', JSON.stringify(userData.value));
        localStorage.setItem('isLoggedIn', 'true');
        if (token) {
            setAuthToken(token);
        }
    };

    const logout = () => {
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
        localStorage.removeItem('userProfile');
        localStorage.removeItem('isLoggedIn');
        setAuthToken(null);
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

    const isAuthenticated = computed(() => {
        return isLoggedIn.value && userData.value.userId && !!getAuthToken();
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
