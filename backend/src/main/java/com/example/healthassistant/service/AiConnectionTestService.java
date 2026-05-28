package com.example.healthassistant.service;

import com.example.healthassistant.ai.DashScopeChatClient;
import com.example.healthassistant.ai.LmStudioChatClient;
import com.example.healthassistant.ai.ResolvedAiConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiConnectionTestService {

    private final UserAiSettingsService userAiSettingsService;
    private final LmStudioChatClient lmStudioChatClient;
    private final DashScopeChatClient dashScopeChatClient;
    private final RestTemplate restTemplate;

    @Value("${doubao.api.key:}")
    private String serverDoubaoKey;

    public AiConnectionTestService(
            UserAiSettingsService userAiSettingsService,
            LmStudioChatClient lmStudioChatClient,
            DashScopeChatClient dashScopeChatClient,
            RestTemplate restTemplate) {
        this.userAiSettingsService = userAiSettingsService;
        this.lmStudioChatClient = lmStudioChatClient;
        this.dashScopeChatClient = dashScopeChatClient;
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> test(String userId, String type) {
        ResolvedAiConfig config = userAiSettingsService.resolve(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);

        boolean ok = false;
        String message;

        switch (type == null ? "" : type) {
            case "lmstudio" -> {
                ok = config.getLmstudioBaseUrl() != null
                        && lmStudioChatClient.testConnection(config.getLmstudioBaseUrl());
                message = ok ? "LM Studio 连接成功" : "无法连接 LM Studio，请确认 Local Server 已启动";
            }
            case "dashscope" -> {
                ok = config.getDashscopeApiKey() != null
                        && dashScopeChatClient.testConnection(config.getDashscopeApiKey());
                message = ok ? "DashScope 连接成功" : "DashScope Key 无效或未配置";
            }
            case "doubao" -> {
                String key = config.getDoubaoApiKey();
                ok = key != null && !key.isBlank();
                message = ok ? "豆包 Key 已配置" : "请配置豆包 API Key";
            }
            case "pexels" -> {
                ok = testPexels(config.getPexelsApiKey());
                message = ok ? "Pexels 连接成功" : "Pexels Key 无效或未配置";
            }
            default -> message = "未知测试类型: " + type;
        }

        result.put("reachable", ok);
        result.put("message", message);
        return result;
    }

    private boolean testPexels(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            ResponseEntity<String> resp = restTemplate.exchange(
                    "https://api.pexels.com/v1/search?query=food&per_page=1",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
