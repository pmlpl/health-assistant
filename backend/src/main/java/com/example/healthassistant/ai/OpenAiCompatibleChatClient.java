package com.example.healthassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Chat Completions（DeepSeek、自定义端点等）。
 */
@Component
public class OpenAiCompatibleChatClient {

    public static final String DEEPSEEK_DEFAULT_BASE = "https://api.deepseek.com";
    public static final String DEEPSEEK_DEFAULT_MODEL = "deepseek-v4-flash";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleChatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * OpenAI 兼容多模态识图（LM Studio / 部分云端视觉端点）。
     */
    public String completeWithVisionImage(
            String baseUrl,
            String apiKey,
            String model,
            String imageDataUri,
            String systemPrompt,
            String userText,
            int maxTokens) {
        String url = normalizeBaseUrl(baseUrl) + "/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.3);

        List<Map<String, Object>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "image_url", "image_url", Map.of("url", imageDataUri)));
        userContent.add(Map.of("type", "text", "text", userText));
        messages.add(Map.of("role", "user", "content", userContent));

        body.put("messages", messages);
        if (maxTokens > 0) {
            body.put("max_tokens", maxTokens);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        } else {
            headers.setBearerAuth("lm-studio");
        }

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new IllegalStateException("解析视觉响应失败: " + e.getMessage(), e);
        }
    }

    public String complete(String baseUrl, String apiKey, String model, List<ChatMessage> messages, int maxTokens) {
        String url = normalizeBaseUrl(baseUrl) + "/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        List<Map<String, String>> msgList = new ArrayList<>();
        for (ChatMessage m : messages) {
            Map<String, String> item = new HashMap<>();
            item.put("role", m.role());
            item.put("content", m.content());
            msgList.add(item);
        }
        body.put("messages", msgList);
        body.put("temperature", 0.7);
        if (maxTokens > 0) {
            body.put("max_tokens", maxTokens);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new IllegalStateException("解析 OpenAI 兼容响应失败: " + e.getMessage(), e);
        }
    }

    public void stream(String baseUrl, String apiKey, String model, List<ChatMessage> messages, int maxTokens,
                       AiStreamChunkConsumer consumer) {
        try {
            OpenAiStreamClient.streamChat(
                    normalizeBaseUrl(baseUrl),
                    apiKey,
                    model,
                    messages,
                    maxTokens,
                    consumer);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 兼容流式调用失败: " + e.getMessage(), e);
        }
    }

    public boolean testConnection(String baseUrl, String apiKey, String model) {
        return testConnectionDetailed(baseUrl, apiKey, model).success();
    }

    public ConnectionTestResult testConnectionDetailed(String baseUrl, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            return new ConnectionTestResult(false, "API Key 未配置，请先填写并保存");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return new ConnectionTestResult(false, "API 根地址未填写");
        }
        String modelId = (model != null && !model.isBlank()) ? model : DEEPSEEK_DEFAULT_MODEL;
        try {
            complete(baseUrl, apiKey, modelId, List.of(new ChatMessage("user", "ping")), 256);
            return new ConnectionTestResult(true, "API 连接成功，模型：" + modelId);
        } catch (Exception e) {
            return new ConnectionTestResult(false,
                    "API 连接失败：" + rootMessage(e) + "。请检查地址、Key 与模型名。");
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName();
    }

    public String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("API 地址不能为空");
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        // 火山方舟等已是 /api/v3 的端点，不再追加 /v1
        if (trimmed.contains("/api/v3")) {
            return trimmed;
        }
        if (!trimmed.endsWith("/v1")) {
            trimmed = trimmed + "/v1";
        }
        return trimmed;
    }
}
