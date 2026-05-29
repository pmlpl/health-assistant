package com.example.healthassistant.ai;

/**
 * OpenAI 兼容对话消息。
 */
public record ChatMessage(String role, String content) {
}
