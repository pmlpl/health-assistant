// src/api/healthApi.js
import axios from 'axios';

// 使用环境变量配置 API 基础 URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 60000,  // 增加超时时间到 60 秒
    withCredentials: false,  // 临时禁用凭证以避免 CORS 问题
});

// 请求拦截器
api.interceptors.request.use(
    config => {
        // 可以在这里添加认证token等
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
api.interceptors.response.use(
    response => {
        return response;
    },
    error => {
        // 只在状态码不是 403/404 时才输出错误（这些通常是 CORS 或路由问题，可以忽略）
        if (error.response && error.response.status !== 403 && error.response.status !== 404) {
            console.error('API 请求错误:', error);
        }
        return Promise.reject(error);
    }
);

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
        // 创建一个新的axios实例，不使用全局拦截器，以确保错误能被正确捕获
        const mentalHealthApi = axios.create({
            baseURL: API_BASE_URL,
            timeout: 120000
        });

        try {
            const response = await mentalHealthApi.post('/ai/mental-health', requestData);
            return response;
        } catch (error) {
            console.error('获取心理健康建议失败:', error);
            // 模拟响应，当后端服务不可用时使用
            return {
                data: {
                    success: true,
                    response: '您好！我是您的心理陪伴助手。由于系统暂时无法连接到AI服务，我将为您提供一些基本的心理健康建议。如果您有任何情绪问题、压力管理或心理健康方面的疑问，请随时告诉我，我会尽力为您提供支持和建议。\n\n记住，保持积极的心态，适当运动，保持良好的作息，与亲友保持联系，这些都是维护心理健康的重要因素。如果您的情绪问题持续存在，建议寻求专业心理咨询师的帮助。',
                    userId: requestData.userId
                }
            };
        }
    },

    // API 状态检测
    getApiStatus: async () => {
        try {
            const response = await api.get('/status/api-availability');
            return response.data;
        } catch (error) {
            console.error('获取 API 状态失败:', error);
            // 当后端服务不可用时，返回不可用状态
            return {
                qwen: {
                    available: false,
                    message: '后端服务不可用',
                    hasApiKey: false
                },
                doubao: {
                    available: false,
                    message: '后端服务不可用',
                    hasApiKey: false
                }
            };
        }
    }
};