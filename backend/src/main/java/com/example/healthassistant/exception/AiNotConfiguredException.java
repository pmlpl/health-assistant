package com.example.healthassistant.exception;

/**
 * 用户未配置 AI（prod BYOK），引导前往 AI 设置页。
 */
public class AiNotConfiguredException extends RuntimeException {

    public AiNotConfiguredException() {
        super("请先在「AI 设置」中配置您的大模型或 API Key");
    }

    public AiNotConfiguredException(String message) {
        super(message);
    }
}
