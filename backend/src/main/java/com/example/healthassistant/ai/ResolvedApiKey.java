package com.example.healthassistant.ai;

/**
 * 解析后的 API Key：区分用户自备与平台试用代付。
 */
public record ResolvedApiKey(String key, boolean platformTrial) {

    public boolean isPresent() {
        return key != null && !key.isBlank();
    }
}
