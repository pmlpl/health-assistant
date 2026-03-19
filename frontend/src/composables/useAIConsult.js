// src/composables/useAIConsult.js
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useUserStore } from '../stores/userStore';

export function useAIConsult() {
    const userStore = useUserStore();
    const currentMessage = ref('');
    const chatMessages = ref([]);
    const isLoading = ref(false);
    
    // 为每个用户生成独立的存储键名
    const getStorageKey = () => {
        return `ai_chat_history_${userStore.userData.userId}`;
    };

    // 保存聊天记录到本地存储（按用户区分）
    const saveChatHistory = () => {
        try {
            const storageKey = getStorageKey();
            const chatData = {
                userId: userStore.userData.userId,
                messages: chatMessages.value,
                lastUpdated: Date.now()
            };
            localStorage.setItem(storageKey, JSON.stringify(chatData));
            console.log(`💾 已保存 ${chatMessages.value.length} 条聊天记录到本地存储`);
        } catch (error) {
            console.warn('保存聊天记录失败:', error);
        }
    };

    // 从本地存储加载聊天记录（按用户区分）
    const loadChatHistory = () => {
        try {
            const storageKey = getStorageKey();
            const savedData = localStorage.getItem(storageKey);
            if (savedData) {
                const chatData = JSON.parse(savedData);
                // 验证是否为当前用户的数据
                if (chatData.userId === userStore.userData.userId) {
                    // 可以添加过期检查逻辑，比如超过 24 小时的数据可以清理
                    chatMessages.value = chatData.messages || [];
                } else {
                    // 不是当前用户的数据，清空聊天记录
                    chatMessages.value = [];
                    console.log('⚠️ 检测到不同用户数据，已清空聊天记录');
                }
            } else {
                // 没有找到该用户的聊天记录
                chatMessages.value = [];
            }
        } catch (error) {
            console.warn('加载聊天记录失败:', error);
            chatMessages.value = [];
        }
    };

    // 发送消息
    const sendMessage = async () => {
        if (!currentMessage.value.trim()) {
            return;
        }

        const userMessage = currentMessage.value.trim();
        
        // 添加用户消息到聊天记录
        const newUserMessage = {
            type: 'user',
            content: userMessage,
            timestamp: Date.now()
        };
        chatMessages.value.push(newUserMessage);
        saveChatHistory(); // 保存到本地存储

        const tempMessage = currentMessage.value;
        currentMessage.value = '';

        try {
            isLoading.value = true;
            userStore.clearError();

            // 使用相对路径，通过Vite代理转发到后端
            const response = await fetch('/api/ai/nutrition-advice', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: userStore.userData.userId,
                    query: userMessage
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            
            // 添加AI回复到聊天记录
            const aiResponse = {
                type: 'assistant',
                content: data.advice || '未收到回复',
                timestamp: Date.now()
            };
            chatMessages.value.push(aiResponse);
            saveChatHistory(); // 保存到本地存储

        } catch (err) {
            console.error('AI咨询失败:', err);
            userStore.setError(`AI咨询失败: ${err.message}`);
            
            // 添加错误消息到聊天记录
            const errorMessage = {
                type: 'assistant',
                content: '抱歉，服务暂时不可用，请稍后再试。',
                timestamp: Date.now()
            };
            chatMessages.value.push(errorMessage);
            saveChatHistory(); // 保存到本地存储
        } finally {
            isLoading.value = false;
        }
    };

    // 清除聊天历史
    const clearChatHistory = async () => {
        if (confirm('确定要清除所有对话历史吗？这将删除本地保存的所有聊天记录。')) {
            try {
                // 清除后端会话
                const response = await fetch(`/api/ai/session/${userStore.userData.userId}`, {
                    method: 'DELETE'
                });

                // 清除前端数据
                chatMessages.value = [];
                currentMessage.value = '';
                localStorage.removeItem(getStorageKey());
                
                if (response.ok) {
                    console.log('聊天历史已清除');
                }
            } catch (err) {
                console.error('清除聊天历史失败:', err);
                // 即使后端清除失败，也清空前段显示和本地存储
                chatMessages.value = [];
                currentMessage.value = '';
                localStorage.removeItem(getStorageKey());
            }
        }
    };

    // 添加清理旧数据的方法
    const cleanupOldChatHistory = () => {
        try {
            const now = Date.now();
            const ONE_DAY = 24 * 60 * 60 * 1000; // 24小时
            
            // 遍历所有localStorage项
            for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key && key.startsWith('ai_chat_history_')) {
                    const chatData = JSON.parse(localStorage.getItem(key));
                    // 如果超过24小时未更新，删除该记录
                    if (now - chatData.lastUpdated > ONE_DAY) {
                        localStorage.removeItem(key);
                        console.log(`已清理过期聊天记录: ${key}`);
                    }
                }
            }
        } catch (error) {
            console.warn('清理旧聊天记录失败:', error);
        }
    };

    // 在组件挂载时执行清理
    onMounted(() => {
        cleanupOldChatHistory();
        
        // 如果用户数据已经存在，立即加载聊天记录
        if (userStore.userData && userStore.userData.userId) {
            loadChatHistory();
        }
    });

    // 监听用户数据变化，确保用户登录后加载聊天记录
    watch(() => userStore.userData, (newUserData) => {
        if (newUserData && newUserData.userId) {
            loadChatHistory();
        }
    }, { immediate: true });

    // 组件卸载前保存聊天记录
    onUnmounted(() => {
        saveChatHistory();
    });

    return {
        currentMessage,
        chatMessages,
        isLoading,
        sendMessage,
        clearChatHistory,
        loadChatHistory
    };
}
