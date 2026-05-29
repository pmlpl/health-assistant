// AI 精灵营养咨询：状态在 Pinia，流式由 aiStreamRunner 持有（切路由不 abort）
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useUserStore } from '../stores/userStore';
import { useAiConsultStore } from '../stores/aiConsultStore';
import { healthApi, getAuthToken } from '../api/healthApi';
import { aiStreamRunner } from '../services/aiStreamRunner';
import { messageFromError } from '../utils/notify';

export function useAIConsult() {
    const userStore = useUserStore();
    const consultStore = useAiConsultStore();
    const { chatMessages, isLoading } = storeToRefs(consultStore);
    const currentMessage = ref('');

    const getStorageKey = () => `ai_chat_history_${userStore.userData.userId}`;

    const saveChatHistory = () => {
        try {
            const storageKey = getStorageKey();
            const chatData = {
                userId: userStore.userData.userId,
                messages: chatMessages.value,
                lastUpdated: Date.now(),
            };
            localStorage.setItem(storageKey, JSON.stringify(chatData));
        } catch (error) {
            console.warn('保存聊天记录失败:', error);
        }
    };

    const loadChatHistory = () => {
        try {
            const storageKey = getStorageKey();
            const savedData = localStorage.getItem(storageKey);
            if (savedData) {
                const chatData = JSON.parse(savedData);
                if (chatData.userId === userStore.userData.userId) {
                    consultStore.setMessages(chatData.messages || []);
                } else {
                    consultStore.setMessages([]);
                }
            } else {
                consultStore.setMessages([]);
            }
        } catch (error) {
            console.warn('加载聊天记录失败:', error);
            consultStore.setMessages([]);
        }
    };

    const sendMessage = async () => {
        if (!currentMessage.value.trim()) {
            return;
        }
        if (!getAuthToken()) {
            userStore.setError('请先登录后再使用 AI 咨询');
            return;
        }
        const userId = userStore.userData?.userId;
        if (!userId) {
            userStore.setError('用户信息缺失，请重新登录');
            return;
        }

        const userMessage = currentMessage.value.trim();
        consultStore.appendUserMessage(userMessage);
        saveChatHistory();
        currentMessage.value = '';

        try {
            userStore.clearError();
            await aiStreamRunner.send(userId, userMessage, {
                useStream: consultStore.streamEnabled,
            });
            saveChatHistory();
        } catch (err) {
            if (err.name === 'AbortError') {
                consultStore.cancelStreaming();
                return;
            }
            console.error('AI咨询失败:', err);
            const hint = messageFromError(err, '服务暂时不可用');
            userStore.setError(`AI咨询失败: ${hint}`);
            saveChatHistory();
            // 错误文案已由 aiStreamRunner 写入 assistant 气泡
        }
    };

    const clearChatHistory = async () => {
        if (!confirm('确定要清除所有对话历史吗？这将删除本地保存的所有聊天记录。')) {
            return;
        }
        try {
            aiStreamRunner.cancel();
            const userId = userStore.userData?.userId;
            if (userId && getAuthToken()) {
                await healthApi.clearAiSession(userId);
            }
            consultStore.clearAll();
            currentMessage.value = '';
            localStorage.removeItem(getStorageKey());
        } catch (err) {
            console.error('清除聊天历史失败:', err);
            consultStore.clearAll();
            currentMessage.value = '';
            localStorage.removeItem(getStorageKey());
        }
    };

    /** 根据后端开关与用户已保存的 textProvider 决定是否走 SSE */
    const refreshStreamPreference = async () => {
        try {
            const [cfg, settings] = await Promise.all([
                healthApi.getConsultStreamEnabled(),
                healthApi.getAiSettings().catch(() => null),
            ]);
            const provider = settings?.textProvider;
            const localLm = provider === 'lmstudio' || provider === 'auto';
            consultStore.setStreamEnabled(cfg?.streamEnabled === true && !localLm);
        } catch {
            consultStore.setStreamEnabled(false);
        }
    };

    const cleanupOldChatHistory = () => {
        try {
            const now = Date.now();
            const ONE_DAY = 24 * 60 * 60 * 1000;
            for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key && key.startsWith('ai_chat_history_')) {
                    const chatData = JSON.parse(localStorage.getItem(key));
                    if (now - chatData.lastUpdated > ONE_DAY) {
                        localStorage.removeItem(key);
                    }
                }
            }
        } catch (error) {
            console.warn('清理旧聊天记录失败:', error);
        }
    };

    onMounted(async () => {
        cleanupOldChatHistory();
        if (userStore.userData?.userId) {
            loadChatHistory();
        }
        await refreshStreamPreference();
    });

    watch(
        () => userStore.userData,
        (newUserData) => {
            if (newUserData?.userId) {
                loadChatHistory();
            }
        },
        { immediate: true }
    );

    watch(chatMessages, () => saveChatHistory(), { deep: true });

    onUnmounted(() => {
        saveChatHistory();
    });

    return {
        currentMessage,
        chatMessages,
        isLoading,
        sendMessage,
        clearChatHistory,
        loadChatHistory,
        refreshStreamPreference,
    };
}
