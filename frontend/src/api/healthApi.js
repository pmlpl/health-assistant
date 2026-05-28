// src/api/healthApi.js
import axios from 'axios';

// 使用环境变量配置 API 基础 URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const TOKEN_KEY = 'authToken';

// 读取/保存 JWT，供拦截器与登录流程使用
export const getAuthToken = () => localStorage.getItem(TOKEN_KEY);
export const setAuthToken = (token) => {
    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
    } else {
        localStorage.removeItem(TOKEN_KEY);
    }
};

const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 300000,
    withCredentials: false,
});

// 请求拦截器：自动附加 Bearer Token
api.interceptors.request.use(
    config => {
        const token = getAuthToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

// 响应拦截器：401 跳转登录；自动解包 ApiResponse 格式
api.interceptors.response.use(
    response => {
        const body = response.data;
        // 统一解包 { code, message, data } 为扁平结构，兼容旧版 success 字段
        if (body && typeof body.code === 'number' && body.code === 200) {
            const payload = body.data;
            if (payload === null || payload === undefined) {
                response.data = { success: true, message: body.message };
            } else if (Array.isArray(payload)) {
                response.data = payload;
            } else if (typeof payload === 'object') {
                response.data = { ...payload, success: true, message: body.message };
            } else {
                response.data = payload;
            }
        }
        return response;
    },
    error => {
        if (error.response?.status === 401) {
            setAuthToken(null);
            localStorage.removeItem('isLoggedIn');
            localStorage.removeItem('userProfile');
            if (!window.location.pathname.includes('/login')) {
                window.location.href = '/login';
            }
        } else if (error.response?.data?.message) {
            error.message = error.response.data.message;
        } else if (error.response && error.response.status !== 403 && error.response.status !== 404) {
            console.error('API 请求错误:', error);
        }
        return Promise.reject(error);
    }
);

// 导出带 JWT 拦截器的 axios 实例，供各页面替代裸 axios
export const apiClient = api;

export const healthApi = {
    getUserProfile: async (userId) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.get(`/users/profile/${encodedUserId}`);
            return response.data;
        } catch (error) {
            console.error('获取用户档案失败:', error);
            throw error;
        }
    },

    createUserProfile: async (profileData) => {
        try {
            const response = await api.post('/users/profile', profileData);
            return response.data;
        } catch (error) {
            console.error('创建用户档案失败:', error);
            throw error;
        }
    },

    updateUserProfile: async (userId, profileData) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.put(`/users/profile/${encodedUserId}`, profileData);
            return response.data;
        } catch (error) {
            console.error('更新用户档案失败:', error);
            throw error;
        }
    },

    register: async (userData) => {
        try {
            const response = await api.post('/users/register', userData);
            return response.data;
        } catch (error) {
            console.error('用户注册失败:', error);
            throw error;
        }
    },

    login: async (credentials) => {
        try {
            const response = await api.post('/users/login', credentials);
            return response.data;
        } catch (error) {
            console.error('用户登录失败:', error);
            throw error;
        }
    },

    forgotPassword: async (data) => {
        try {
            const response = await api.post('/users/forgot-password', data);
            return response.data;
        } catch (error) {
            console.error('忘记密码请求失败:', error);
            throw error;
        }
    },

    getAllRecipes: async () => {
        try {
            const response = await api.get('/recipes');
            return response.data;
        } catch (error) {
            console.error('获取食谱失败:', error);
            throw error;
        }
    },

    getRecipeById: async (id) => {
        try {
            const response = await api.get(`/recipes/${id}`);
            return response.data;
        } catch (error) {
            console.error('获取食谱详情失败:', error);
            throw error;
        }
    },

    recordDiet: async (dietData) => {
        try {
            const response = await api.post('/diet/record', dietData);
            return response.data;
        } catch (error) {
            console.error('记录饮食失败:', error);
            throw error;
        }
    },

    getDailyDiet: async (userId, date) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.get(`/diet/daily/${encodedUserId}/${date}`);
            return response.data;
        } catch (error) {
            console.error('获取日常饮食记录失败:', error);
            throw error;
        }
    },

    // 获取指定日期范围的饮食记录
    getDietRecordsInRange: async (userId, startDate, endDate) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.get(`/diet/range/${encodedUserId}`, {
                params: { startDate, endDate }
            });
            return response.data;
        } catch (error) {
            console.error('获取日期范围饮食记录失败:', error);
            throw error;
        }
    },

    getMonthlyDietSummary: async (userId, year, month) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.get(`/diet/monthly-summary/${encodedUserId}/${year}/${month}`);
            return response.data;
        } catch (error) {
            console.error('获取月度饮食统计失败:', error);
            throw error;
        }
    },

    // 删除饮食记录
    deleteDietRecord: async (recordId) => {
        try {
            const response = await api.delete(`/diet/${recordId}`);
            return response.data;
        } catch (error) {
            console.error('删除饮食记录失败:', error);
            throw error;
        }
    },

    // 批量删除饮食记录
    deleteBatchDietRecords: async (recordIds) => {
        try {
            const response = await api.delete('/diet/batch', {
                data: recordIds
            });
            return response.data;
        } catch (error) {
            console.error('批量删除饮食记录失败:', error);
            throw error;
        }
    },

    // 喝水记录相关
    getDailyWaterIntake: async (userId, date) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.get(`/diet/daily/${encodedUserId}/${date}`);
            const records = response.data;
            return records.filter(record =>
                record.foodDescription &&
                (record.foodDescription.includes('水') || record.foodDescription.toLowerCase().includes('water'))
            );
        } catch (error) {
            console.error('获取每日喝水记录失败:', error);
            throw error;
        }
    },

    addWaterIntake: async (userId, waterData) => {
        try {
            // 创建一个特殊的饮食记录来表示喝水
            const dietData = {
                userId: userId,
                mealType: 'SNACK', // 使用零食餐别
                foodDescription: `💧 喝水打卡 - ${waterData.time}`, // 在食物描述中包含喝水信息
                calories: 0, // 水没有热量
                protein: 0,
                carbs: 0,
                fat: 0,
                fiber: 0
            };

            // 调用现有的记录饮食API
            const response = await api.post('/diet/record', dietData);
            return response.data;
        } catch (error) {
            console.error('添加喝水记录失败:', error);
            throw error;
        }
    },

    // 健康提示相关
    getPersonalizedTips: async (userId) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.get(`/health-tips/personalized/${encodedUserId}`);
            return response.data;
        } catch (error) {
            console.error('获取健康提示失败:', error);
            throw error;
        }
    },

    healthCheck: async () => {
        try {
            const response = await api.get('/test/health');
            return response.data;
        } catch (error) {
            console.error('健康检查失败:', error);
            throw error;
        }
    },

    logout: async (userId) => {
        try {
            const response = await api.post('/users/logout', { userId });
            return response.data;
        } catch (error) {
            console.error('用户注销失败:', error);
            throw error;
        }
    },

    deleteUser: async (userId) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.delete(`/users/delete/${encodedUserId}`);
            return response.data;
        } catch (error) {
            console.error('删除用户失败:', error);
            throw error;
        }
    },

    updateUsername: async (userId, newUsername) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.put(`/users/update-username/${encodedUserId}`, { newUsername });
            return response.data;
        } catch (error) {
            console.error('修改用户名失败:', error);
            throw error;
        }
    },

    updatePassword: async (userId, oldPassword, newPassword) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.put(`/users/update-password/${encodedUserId}`, {
                oldPassword,
                newPassword
            });
            return response.data;
        } catch (error) {
            console.error('修改密码失败:', error);
            throw error;
        }
    },

    // 新增：健身分析相关
    analyzeFitness: async (fitnessData) => {
        try {
            const response = await api.post('/ai/analyze-fitness', fitnessData);
            return response.data;
        } catch (error) {
            console.error('健身分析失败:', error);
            throw error;
        }
    },

    // 健身记录相关
    batchSaveFitnessRecords: async (records) => {
        try {
            const response = await api.post('/diet/fitness/batch-save', records);
            return response.data;
        } catch (error) {
            console.error('批量保存健身记录失败:', error);
            throw error;
        }
    },

    getDailyFitnessRecords: async (userId, date) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.get(`/diet/fitness/daily/${encodedUserId}/${date}`);
            return response.data;
        } catch (error) {
            console.error('获取每日健身记录失败:', error);
            throw error;
        }
    },

    getMonthlyFitnessSummary: async (userId, year, month) => {
        try {
            const encodedUserId = encodeURIComponent(userId);
            const response = await api.get(`/diet/fitness/monthly-summary/${encodedUserId}/${year}/${month}`);
            return response.data;
        } catch (error) {
            console.error('获取月度健身统计失败:', error);
            throw error;
        }
    },

    // 心理健康咨询相关
    getMentalHealthAdvice: async (requestData) => {
        const response = await api.post('/ai/mental-health', requestData, {
            timeout: 600000,
        });
        return response;
    },

    getApiStatus: async () => {
        const response = await api.get('/status/api-availability');
        return response.data;
    },

    getAiSettings: async () => {
        const response = await api.get('/users/me/ai-settings');
        return response.data?.data ?? response.data;
    },

    updateAiSettings: async (payload) => {
        const response = await api.put('/users/me/ai-settings', payload);
        return response.data?.data ?? response.data;
    },

    testAiConnection: async (type) => {
        const response = await api.post('/users/me/ai-settings/test', { type });
        return response.data?.data ?? response.data;
    }
};