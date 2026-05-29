package com.example.healthassistant.ai;

/**
 * 流式输出时按片段回调（供 SSE 推送给前端）。
 */
@FunctionalInterface
public interface AiStreamChunkConsumer {
    void onChunk(String text);
}
