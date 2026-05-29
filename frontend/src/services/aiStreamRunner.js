// AI 精灵咨询：优先走 healthApi（自动解包 ApiResponse），流式失败则降级
import { healthApi, getAuthToken } from '../api/healthApi';
import { useAiConsultStore } from '../stores/aiConsultStore';
import { messageFromError, notifyWarning } from '../utils/notify';

const BACKEND_LABELS = {
    lmstudio: '本地 LM Studio',
    dashscope: '通义千问',
    deepseek: 'DeepSeek',
    doubao: '豆包',
    other: '其他 OpenAI 兼容',
    auto: '自动',
};

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';
let abortController = null;

function getStreamUrl() {
    const base = API_BASE.endsWith('/') ? API_BASE.slice(0, -1) : API_BASE;
    return `${base}/ai/nutrition-advice/stream`;
}

/** 解包后端 { code, message, data } 或兼容旧版扁平 JSON */
function unwrapApiJson(json) {
    if (!json || typeof json !== 'object') {
        return json;
    }
    if (typeof json.code === 'number' && json.code === 200 && json.data != null) {
        return json.data;
    }
    return json;
}

function parseSseChunk(data, store) {
    if (!data || data === '[DONE]') {
        return;
    }
    if (data.startsWith('{')) {
        try {
            const obj = JSON.parse(data);
            const err = obj.error || obj.message;
            if (err) {
                throw new Error(String(err));
            }
        } catch (e) {
            if (e instanceof Error && e.message && !(e instanceof SyntaxError)) {
                throw e;
            }
        }
        return;
    }
    store.appendAssistantChunk(data);
}

export const aiStreamRunner = {
    cancel() {
        if (abortController) {
            abortController.abort();
            abortController = null;
        }
    },

    async send(userId, query, { useStream = true } = {}) {
        this.cancel();
        abortController = new AbortController();
        const signal = abortController.signal;
        const store = useAiConsultStore();

        store.beginAssistantStream();

        try {
            if (useStream && store.streamEnabled) {
                try {
                    await this._sendStream(userId, query, signal, store);
                    const text = store.currentAssistantDraft || '';
                    if (text.trim()) {
                        store.finishAssistantStream();
                        return text;
                    }
                } catch (streamErr) {
                    if (streamErr.name === 'AbortError') {
                        throw streamErr;
                    }
                    console.warn('AI 流式失败，降级为普通请求:', streamErr);
                    // 保持 loading，不重复插入空气泡
                }
            }
            return await this._sendViaHealthApi(userId, query, store);
        } catch (e) {
            if (e.name !== 'AbortError') {
                const hint = messageFromError(e, '服务暂时不可用');
                store.setAssistantError(
                    e.response?.status === 401
                        ? '请先登录后再咨询。'
                        : e.response?.status === 428
                          ? '请先在 AI 设置中选择服务商并保存 Key/地址。'
                          : `抱歉，${hint}`
                );
            }
            throw e;
        } finally {
            abortController = null;
        }
    },

    /** 与原先 useAIConsult 一致：走 axios + JWT + ApiResponse 解包 */
    async _sendViaHealthApi(userId, query, store) {
        const data = await healthApi.getNutritionAdvice(userId, query);
        const text = data?.advice || '';
        if (!text && data?.error) {
            throw new Error(data.error);
        }
        if (!text) {
            throw new Error('未收到 AI 回复');
        }
        if (data?.aiBackend && data.aiBackend !== 'lmstudio') {
            const label = BACKEND_LABELS[data.aiBackend] || data.aiBackend;
            notifyWarning(
                `本次未使用本地模型，实际调用：${label}${data.aiModel ? `（${data.aiModel}）` : ''}。请在右上角 AI 设置中选择「本地 LM Studio」并保存。`,
                '路由提示'
            );
        }
        store.appendAssistantChunk(text);
        store.finishAssistantStream();
        return text;
    },

    async _sendStream(userId, query, signal, store) {
        const token = getAuthToken();
        if (!token) {
            throw new Error('未登录');
        }

        const res = await fetch(getStreamUrl(), {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`,
                Accept: 'text/event-stream',
            },
            body: JSON.stringify({ userId, query }),
            signal,
        });

        const contentType = res.headers.get('content-type') || '';
        if (!res.ok) {
            let errMsg = `HTTP ${res.status}`;
            try {
                const errJson = unwrapApiJson(await res.json());
                errMsg = errJson?.error || errJson?.message || errMsg;
            } catch {
                errMsg = (await res.text()) || errMsg;
            }
            throw new Error(errMsg);
        }

        if (contentType.includes('application/json')) {
            const json = unwrapApiJson(await res.json());
            const text = json?.advice || json?.error || '';
            if (json?.error && !json?.advice) {
                throw new Error(json.error);
            }
            if (text) {
                store.appendAssistantChunk(text);
            }
            return;
        }

        if (!res.body) {
            throw new Error('浏览器不支持流式响应');
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const processSseBlock = (block) => {
            let eventName = 'message';
            const dataLines = [];
            for (const line of block.split(/\r?\n/)) {
                if (line.startsWith('event:')) {
                    eventName = line.slice(6).trim();
                } else if (line.startsWith('data:')) {
                    dataLines.push(line.slice(5).trim());
                }
            }
            const payload = dataLines.join('\n');
            if (eventName === 'error' && payload) {
                throw new Error(payload);
            }
            if (eventName === 'done' || payload === '[DONE]') {
                return;
            }
            if (payload) {
                parseSseChunk(payload, store);
            }
        };

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const blocks = buffer.split(/\r?\n\r?\n/);
            buffer = blocks.pop() || '';
            for (const block of blocks) {
                if (block.trim()) {
                    processSseBlock(block);
                }
            }
        }
        if (buffer.trim()) {
            processSseBlock(buffer);
        }
    },
};
