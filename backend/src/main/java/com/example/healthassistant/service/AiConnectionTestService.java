package com.example.healthassistant.service;

import com.example.healthassistant.ai.*;
import com.example.healthassistant.dto.AiConnectionTestRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiConnectionTestService {

    private final UserAiSettingsService userAiSettingsService;
    private final LmStudioChatClient lmStudioChatClient;
    private final DashScopeChatClient dashScopeChatClient;
    private final OpenAiCompatibleChatClient openAiCompatibleChatClient;
    private final DoubaoChatClient doubaoChatClient;
    private final RestTemplate restTemplate;

    @Value("${ai.dashscope.default-model:qwen-plus}")
    private String defaultDashscopeModel;

    @Value("${ai.deepseek.default-model:deepseek-chat}")
    private String defaultDeepseekModel;

    @Value("${ai.doubao.default-model:doubao-seed-1-8-251228}")
    private String defaultDoubaoModel;

    public AiConnectionTestService(
            UserAiSettingsService userAiSettingsService,
            LmStudioChatClient lmStudioChatClient,
            DashScopeChatClient dashScopeChatClient,
            OpenAiCompatibleChatClient openAiCompatibleChatClient,
            DoubaoChatClient doubaoChatClient,
            RestTemplate restTemplate) {
        this.userAiSettingsService = userAiSettingsService;
        this.lmStudioChatClient = lmStudioChatClient;
        this.dashScopeChatClient = dashScopeChatClient;
        this.openAiCompatibleChatClient = openAiCompatibleChatClient;
        this.doubaoChatClient = doubaoChatClient;
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> test(String userId, AiConnectionTestRequestDto request) {
        ResolvedAiConfig config = userAiSettingsService.resolve(userId);
        String type = request != null && request.getType() != null ? request.getType() : "";

        ConnectionTestResult testResult = switch (type) {
            case "lmstudio" -> testLmStudio(request, config);
            case "dashscope" -> testDashscope(request, config);
            case "deepseek" -> testDeepseek(request, config);
            case "doubao" -> testDoubao(request, config);
            case "other" -> testOther(request, config);
            case "pexels" -> testPexelsDetailed(request, config);
            default -> new ConnectionTestResult(false, "未知测试类型: " + type);
        };

        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("reachable", testResult.success());
        result.put("message", testResult.message());
        return result;
    }

    private ConnectionTestResult testLmStudio(AiConnectionTestRequestDto request, ResolvedAiConfig config) {
        String lmUrl = pick(request != null ? request.getLmstudioBaseUrl() : null, config.getLmstudioBaseUrl());
        String lmModel = pick(request != null ? request.getLmstudioModel() : null, config.getLmstudioModel());
        if (lmUrl == null || lmUrl.isBlank()) {
            return new ConnectionTestResult(false, "请先填写 LM Studio 地址（如 http://127.0.0.1:1234/v1）");
        }
        return lmStudioChatClient.testConnectionDetailed(lmUrl, lmModel);
    }

    private ConnectionTestResult testDashscope(AiConnectionTestRequestDto request, ResolvedAiConfig config) {
        String key = pick(request != null ? request.getDashscopeApiKey() : null, config.getDashscopeApiKey());
        String model = pick(request != null ? request.getCloudModel() : null, config.getCloudModel());
        if (model == null || model.isBlank()) {
            model = defaultDashscopeModel;
        }
        return dashScopeChatClient.testConnectionDetailed(key, model);
    }

    private ConnectionTestResult testDeepseek(AiConnectionTestRequestDto request, ResolvedAiConfig config) {
        String key = pick(request != null ? request.getDeepseekApiKey() : null, config.getDeepseekApiKey());
        String model = pick(request != null ? request.getCloudModel() : null, config.getCloudModel());
        if (model == null || model.isBlank()) {
            model = defaultDeepseekModel;
        }
        ConnectionTestResult result = openAiCompatibleChatClient.testConnectionDetailed(
                OpenAiCompatibleChatClient.DEEPSEEK_DEFAULT_BASE, key, model);
        if (result.success()) {
            return new ConnectionTestResult(true, "DeepSeek 连接成功，模型：" + model);
        }
        return new ConnectionTestResult(false,
                result.message().replace("API 连接失败", "DeepSeek 连接失败"));
    }

    private ConnectionTestResult testDoubao(AiConnectionTestRequestDto request, ResolvedAiConfig config) {
        String key = pick(request != null ? request.getDoubaoApiKey() : null, config.getDoubaoApiKey());
        String model = pick(request != null ? request.getCloudModel() : null, config.getCloudModel());
        if (model == null || model.isBlank()) {
            model = defaultDoubaoModel;
        }
        return doubaoChatClient.testConnectionDetailed(key, model);
    }

    private ConnectionTestResult testOther(AiConnectionTestRequestDto request, ResolvedAiConfig config) {
        String base = pick(request != null ? request.getCustomApiBaseUrl() : null, config.getCustomApiBaseUrl());
        String key = pick(request != null ? request.getCustomApiKey() : null, config.getCustomApiKey());
        String model = pick(request != null ? request.getCloudModel() : null, config.getCloudModel());
        if (model == null || model.isBlank()) {
            model = "gpt-3.5-turbo";
        }
        ConnectionTestResult result = openAiCompatibleChatClient.testConnectionDetailed(base, key, model);
        if (result.success()) {
            return new ConnectionTestResult(true, "自定义 API 连接成功，模型：" + model);
        }
        return new ConnectionTestResult(false,
                result.message().replace("API 连接失败", "自定义 API 连接失败"));
    }

    private ConnectionTestResult testPexelsDetailed(AiConnectionTestRequestDto request, ResolvedAiConfig config) {
        String key = pick(request != null ? request.getPexelsApiKey() : null, config.getPexelsApiKey());
        if (key == null || key.isBlank()) {
            return new ConnectionTestResult(false, "Pexels API Key 未配置，请先填写并保存");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", key.trim());
            ResponseEntity<String> resp = restTemplate.exchange(
                    "https://api.pexels.com/v1/search?query=food&per_page=1",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                return new ConnectionTestResult(true, "Pexels 连接成功");
            }
            return new ConnectionTestResult(false,
                    "Pexels 连接失败：HTTP " + resp.getStatusCode().value());
        } catch (RestClientException e) {
            return new ConnectionTestResult(false,
                    "Pexels 连接失败：" + rootMessage(e) + "。请检查 Key 是否有效。");
        } catch (Exception e) {
            return new ConnectionTestResult(false, "Pexels 连接失败：" + rootMessage(e));
        }
    }

    private static String pick(String preview, String saved) {
        if (preview != null && !preview.isBlank()) {
            return preview.trim();
        }
        return saved;
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName();
    }
}
