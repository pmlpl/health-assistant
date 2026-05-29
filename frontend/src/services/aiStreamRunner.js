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
/** 等待 SSE 首条业务数据（非 ready）的最大毫秒数 */
const STREAM_FIRST_BYTE_TIMEOUT_MS = Number(import.meta.env.VITE_AI_STREAM_FIRST_BYTE_TIMEOUT_MS) || 90_000;
/** 两次 chunk 之间的最大空闲毫秒数 */
const STREAM_IDLE_TIMEOUT_MS = Number(import.meta.env.VITE_AI_STREAM_IDLE_TIMEOUT_MS) || 120_000;
/** 整次流式请求绝对上限 */
const STREAM_TOTAL_TIMEOUT_MS = Number(import.meta.env.VITE_AI_STREAM_TOTAL_TIMEOUT_MS) || 600_000;
/** 仅收到 ready 心跳后，等待正文的最长时间，超时则降级为同步接口 */
const STREAM_READY_STALL_MS = Number(import.meta.env.VITE_AI_STREAM_READY_STALL_MS) || 25_000;

let abortController = null;

/** 与 axios baseURL 对齐，避免 VITE_API_BASE_URL 漏写 /api 导致流式请求 404 挂起 */
function normalizeApiBase(base) {
    const raw = (base || '/api').trim().replace(/\/+$/, '');
    if (raw.endsWith('/api')) {
        return raw;
    }
    if (/^https?:\/\//i.test(raw)) {
        return `${raw}/api`;
    }
    return raw.startsWith('/') ? raw : `/${raw}`;
}

function getStreamUrl() {
    return `${normalizeApiBase(API_BASE)}/ai/nutrition-advice/stream`;
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
    if (!data || data === '[DONE]' || data === 'ok') {
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

/** 为 fetch + SSE 读取叠加首字节/空闲/总时长超时，避免无限 loading */
function createStreamTimeoutController(parentSignal) {
    const controller = new AbortController();
    let firstByteTimer = null;
    let idleTimer = null;
    let totalTimer = null;

    const clearAll = () => {
        if (firstByteTimer) clearTimeout(firstByteTimer);
        if (idleTimer) clearTimeout(idleTimer);
        if (totalTimer) clearTimeout(totalTimer);
        firstByteTimer = idleTimer = totalTimer = null;
    };

    const abortWithReason = (message) => {
        if (controller.signal.aborted) return;
        clearAll();
        controller.abort(new DOMException(message, 'TimeoutError'));
    };

    if (parentSignal) {
        if (parentSignal.aborted) {
            controller.abort();
        } else {
            parentSignal.addEventListener('abort', () => controller.abort(), { once: true });
        }
    }

    firstByteTimer = setTimeout(
        () => abortWithReason(`AI 流式响应超时：${Math.round(STREAM_FIRST_BYTE_TIMEOUT_MS / 1000)} 秒内未收到数据`),
        STREAM_FIRST_BYTE_TIMEOUT_MS
    );
    totalTimer = setTimeout(
        () => abortWithReason(`AI 咨询超时：已超过 ${Math.round(STREAM_TOTAL_TIMEOUT_MS / 1000)} 秒`),
        STREAM_TOTAL_TIMEOUT_MS
    );

    return {
        signal: controller.signal,
        /** 收到 SSE 业务数据（含 error/done/正文）后调用 */
        markActivity() {
            if (firstByteTimer) {
                clearTimeout(firstByteTimer);
                firstByteTimer = null;
            }
            if (idleTimer) clearTimeout(idleTimer);
            idleTimer = setTimeout(
                () => abortWithReason(`AI 流式响应中断：${Math.round(STREAM_IDLE_TIMEOUT_MS / 1000)} 秒无新内容`),
                STREAM_IDLE_TIMEOUT_MS
            );
        },
        dispose: clearAll,
    };
}

/** 本地 LM 与连接测试一致走同步 complete，避免 SSE 仅 ready 无正文 */
async function shouldUseSseStream(store) {
    if (!store.streamEnabled) {
        return false;
    }
    try {
        const settings = await healthApi.getAiSettings();
        const provider = settings?.textProvider;
        if (provider === 'lmstudio' || provider === 'auto') {
            return false;
        }
    } catch {
        return false;
    }
    return true;
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
            const useSse = useStream && (await shouldUseSseStream(store));
            if (useSse) {
                try {
                    await this._sendStream(userId, query, signal, store);
                    const text = store.currentAssistantDraft || '';
                    if (text.trim()) {
                        store.finishAssistantStream();
                        return text;
                    }
                } catch (streamErr) {
                    const abortMsg =
                        streamErr?.message ||
                        signal?.reason?.message ||
                        (typeof signal?.reason === 'string' ? signal.reason : '');
                    const stallFallback =
                        abortMsg.includes('就绪信号') || abortMsg.includes('切换为普通');
                    if (streamErr.name === 'AbortError' && !stallFallback) {
                        throw streamErr;
                    }
                    console.warn('AI 流式失败，降级为普通请求:', streamErr);
                }
            }
            return await this._sendViaHealthApi(userId, query, store);
        } catch (e) {
            if (e.name === 'AbortError') {
                store.cancelStreaming();
            } else {
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
            if (store.streaming) {
                store.setAssistantError('咨询未完成，请重试');
            }
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

        const timeoutCtl = createStreamTimeoutController(signal);
        let readyStallTimer = null;
        let sawContent = false;

        const clearReadyStall = () => {
            if (readyStallTimer) {
                clearTimeout(readyStallTimer);
                readyStallTimer = null;
            }
        };

        const armReadyStall = () => {
            clearReadyStall();
            readyStallTimer = setTimeout(() => {
                if (!sawContent && abortController) {
                    abortController.abort(
                        new DOMException('仅收到连接就绪信号，模型无输出，已切换为普通请求', 'TimeoutError')
                    );
                }
            }, STREAM_READY_STALL_MS);
        };

        try {
            const res = await fetch(getStreamUrl(), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`,
                    Accept: 'text/event-stream',
                },
                body: JSON.stringify({ userId: String(userId), query }),
                signal: timeoutCtl.signal,
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
                timeoutCtl.markActivity();
                const json = unwrapApiJson(await res.json());
                const text = json?.advice || json?.error || '';
                if (json?.error && !json?.advice) {
                    throw new Error(json.error);
                }
                if (text) {
                    sawContent = true;
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
                if (!payload && eventName !== 'ready') {
                    return;
                }
                if (eventName === 'ready') {
                    armReadyStall();
                    return;
                }
                timeoutCtl.markActivity();
                clearReadyStall();
                if (eventName === 'error' && payload) {
                    throw new Error(payload);
                }
                if (eventName === 'done' || payload === '[DONE]') {
                    sawContent = true;
                    return;
                }
                if (payload) {
                    sawContent = true;
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
        } finally {
            clearReadyStall();
            timeoutCtl.dispose();
        }
    },
};
