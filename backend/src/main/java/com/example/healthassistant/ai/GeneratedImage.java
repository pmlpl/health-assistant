package com.example.healthassistant.ai;

/**
 * 豆包 Seedream 生图结果（字节 + MIME）。
 */
public record GeneratedImage(byte[] bytes, String contentType) {
}
