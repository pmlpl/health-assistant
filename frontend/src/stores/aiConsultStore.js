// AI 精灵营养咨询：聊天记录与流式状态（与路由解耦）
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useAiConsultStore = defineStore('aiConsult', () => {
    const chatMessages = ref([]);
    const streaming = ref(false);
    const currentAssistantDraft = ref('');
    // 默认先走稳定同步接口；后端开启且流式可用时再启用 SSE
    const streamEnabled = ref(false);

    const isLoading = computed(() => streaming.value);

    function appendUserMessage(content) {
        chatMessages.value.push({
            type: 'user',
            content,
            timestamp: Date.now(),
        });
    }

    function beginAssistantStream() {
        // 仅进入 loading，不插入空气泡（由 AISpirit 的 loading 动画展示等待）
        streaming.value = true;
        currentAssistantDraft.value = '';
    }

    function appendAssistantChunk(chunk) {
        if (!chunk) {
            return;
        }
        currentAssistantDraft.value += chunk;
        const last = chatMessages.value[chatMessages.value.length - 1];
        if (last?.type === 'assistant' && last.streaming) {
            last.content = currentAssistantDraft.value;
        } else {
            chatMessages.value.push({
                type: 'assistant',
                content: currentAssistantDraft.value,
                timestamp: Date.now(),
                streaming: true,
            });
        }
    }

    function finishAssistantStream() {
        streaming.value = false;
        const last = chatMessages.value[chatMessages.value.length - 1];
        const text = (currentAssistantDraft.value || last?.content || '').trim();
        if (last?.type === 'assistant' && last.streaming) {
            if (!text) {
                chatMessages.value.pop();
            } else {
                last.streaming = false;
                last.content = text;
            }
        }
        currentAssistantDraft.value = '';
    }

    function setAssistantError(text) {
        streaming.value = false;
        const last = chatMessages.value[chatMessages.value.length - 1];
        if (last?.type === 'assistant' && last.streaming) {
            last.content = text;
            last.streaming = false;
        } else {
            chatMessages.value.push({
                type: 'assistant',
                content: text,
                timestamp: Date.now(),
                streaming: false,
            });
        }
        currentAssistantDraft.value = '';
    }

    function setMessages(messages) {
        chatMessages.value = messages || [];
    }

    function clearAll() {
        chatMessages.value = [];
        streaming.value = false;
        currentAssistantDraft.value = '';
    }

    function setStreamEnabled(enabled) {
        streamEnabled.value = enabled;
    }

    /** 用户取消或异常中断时解除 loading，不插入错误气泡 */
    function cancelStreaming() {
        streaming.value = false;
        currentAssistantDraft.value = '';
        const last = chatMessages.value[chatMessages.value.length - 1];
        if (last?.type === 'assistant' && last.streaming && !String(last.content || '').trim()) {
            chatMessages.value.pop();
        }
    }

    return {
        chatMessages,
        streaming,
        currentAssistantDraft,
        streamEnabled,
        isLoading,
        appendUserMessage,
        beginAssistantStream,
        appendAssistantChunk,
        finishAssistantStream,
        setAssistantError,
        setMessages,
        clearAll,
        setStreamEnabled,
        cancelStreaming,
    };
});
