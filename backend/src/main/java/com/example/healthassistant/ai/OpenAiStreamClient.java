package com.example.healthassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容接口的 SSE 流式解析（LM Studio、DeepSeek、自定义端点等）。
 */
public final class OpenAiStreamClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile HttpClient HTTP = buildHttpClient(30);

    private OpenAiStreamClient() {
    }

    public static void setConnectTimeoutSeconds(int seconds) {
        int safe = seconds > 0 ? seconds : 30;
        HTTP = buildHttpClient(safe);
    }

    private static HttpClient buildHttpClient(int connectTimeoutSeconds) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
    }

    public static void streamChat(
            String baseUrl,
            String apiKey,
            String model,
            List<ChatMessage> messages,
            int maxTokens,
            AiStreamChunkConsumer consumer) throws Exception {
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
        body.put("stream", true);
        if (maxTokens > 0) {
            body.put("max_tokens", maxTokens);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        } else {
            builder.header("Authorization", "Bearer lm-studio");
        }

        final HttpResponse<java.io.InputStream> response;
        try {
            response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new IllegalStateException(formatConnectError(baseUrl, e), e);
        }

        if (response.statusCode() >= 400) {
            throw new IllegalStateException("流式请求失败 HTTP " + response.statusCode());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                JsonNode root = MAPPER.readTree(data);
                JsonNode delta = root.path("choices").path(0).path("delta").path("content");
                if (!delta.isMissingNode() && !delta.isNull()) {
                    String chunk = delta.asText();
                    if (chunk != null && !chunk.isEmpty()) {
                        consumer.onChunk(chunk);
                    }
                }
            }
        }
    }

    public static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("API 地址不能为空");
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.endsWith("/v1")) {
            trimmed = trimmed + "/v1";
        }
        return trimmed;
    }

    private static String formatConnectError(String baseUrl, Exception e) {
        String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return "无法连接 LM Studio（" + baseUrl + "）：" + detail
                + "。请确认：① LM Studio 已开启 Local Server；② 地址须为运行本后端的机器能访问的地址"
                + "（不是浏览器地址栏）；③ 若 LM 与后端在同一台电脑请用 http://127.0.0.1:1234/v1；"
                + "④ 远程 IP 需在 LM Studio 中允许局域网并放行防火墙 1234 端口。";
    }
}
