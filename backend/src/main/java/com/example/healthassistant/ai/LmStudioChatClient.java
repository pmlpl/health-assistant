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

@Component
public class LmStudioChatClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LmStudioChatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String complete(String baseUrl, String model, List<ChatMessage> messages) {
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

    public boolean testConnection(String baseUrl) {
        try {
            String url = normalizeBaseUrl(baseUrl) + "/models";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth("lm-studio");
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return true;
        } catch (Exception e) {
            return false;
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
