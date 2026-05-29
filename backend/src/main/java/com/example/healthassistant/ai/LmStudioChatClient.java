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

import org.springframework.web.client.RestClientException;

@Component
public class LmStudioChatClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LmStudioChatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String complete(String baseUrl, String model, List<ChatMessage> messages, int maxTokens) {
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
        headers.setBearerAuth("lm-studio");

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new IllegalStateException("解析 LM Studio 响应失败: " + e.getMessage(), e);
        }
    }

    /** 探测 LM Studio：先 /models，再用指定模型发一条最小对话 */
    public ConnectionTestResult testConnectionDetailed(String baseUrl, String model) {
        String normalized = normalizeBaseUrl(baseUrl);
        try {
            String modelsUrl = normalized + "/models";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("lm-studio");
            restTemplate.exchange(modelsUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (RestClientException e) {
            return new ConnectionTestResult(false,
                    "无法连接 " + baseUrl + "：" + rootMessage(e)
                            + "。请确认 LM Studio 已开启 Local Server，地址一般为 http://127.0.0.1:1234");
        } catch (Exception e) {
            return new ConnectionTestResult(false, "连接失败：" + rootMessage(e));
        }

        String modelName = (model == null || model.isBlank()) ? "local-model" : model.trim();
        try {
            List<ChatMessage> ping = List.of(new ChatMessage("user", "hi"));
            complete(baseUrl, modelName, ping, 256);
            return new ConnectionTestResult(true,
                    "LM Studio 连接成功，模型「" + modelName + "」可用");
        } catch (Exception e) {
            return new ConnectionTestResult(false,
                    "服务已连通，但模型「" + modelName + "」调用失败：" + rootMessage(e)
                            + "。请在 LM Studio 中加载模型，并将「模型名称」与界面显示的 Model ID 保持一致。");
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

    /** 流式对话（OpenAI SSE） */
    public void stream(String baseUrl, String model, List<ChatMessage> messages, int maxTokens,
                       AiStreamChunkConsumer consumer) {
        try {
            OpenAiStreamClient.streamChat(
                    normalizeBaseUrl(baseUrl),
                    "lm-studio",
                    model,
                    messages,
                    maxTokens,
                    consumer);
        } catch (Exception e) {
            throw new IllegalStateException("LM Studio 流式调用失败: " + e.getMessage(), e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("LM Studio 地址不能为空");
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
}
