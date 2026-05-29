package com.example.healthassistant.service;

import com.example.healthassistant.ai.DashScopeVisionClient;
import com.example.healthassistant.ai.OpenAiCompatibleChatClient;
import com.example.healthassistant.ai.ResolvedAiConfig;
import com.example.healthassistant.ai.ResolvedApiKey;
import com.example.healthassistant.config.ApiKeyResolver;
import com.example.healthassistant.exception.AiNotConfiguredException;
import com.example.healthassistant.service.PlatformAiQuotaService.UsageKind;
import com.example.healthassistant.util.ImageResizeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionContentPart;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 饮食日记拍照识食：优先用户自选的视觉服务商；未配置时按全局顺序 + 平台试用配额。
 */
@Service
public class ImageRecognitionService {

    private static final String ARK_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    private static final String FOOD_JSON_USER_TEXT = "请识别图片中的食物及其重量，只返回 JSON。";
    private static final String FOOD_JSON_SYSTEM = """
            你是食物营养分析师。只返回图中真实食物，每种一条，仅输出 JSON：
            {"foods":[{"name":"名称","weightGrams":克数,"calories":每100g千卡,"protein":蛋白质,"carbs":碳水,"fat":脂肪,"fiber":纤维}]}
            """;

    private final ApiKeyResolver apiKeyResolver;
    private final UserAiSettingsService userAiSettingsService;
    private final DashScopeVisionClient dashScopeVisionClient;
    private final OpenAiCompatibleChatClient openAiCompatibleChatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.image.recognition.order:doubao,lmstudio,dashscope}")
    private String providerOrder;

    @Value("${ai.image.recognition.max-side:768}")
    private int maxImageSide;

    @Value("${doubao.vision.model.name:doubao-seed-2-0-lite-260215}")
    private String defaultDoubaoVisionModel;

    @Value("${ai.lmstudio.vision-model:}")
    private String lmstudioVisionModel;

    public ImageRecognitionService(
            ApiKeyResolver apiKeyResolver,
            UserAiSettingsService userAiSettingsService,
            DashScopeVisionClient dashScopeVisionClient,
            OpenAiCompatibleChatClient openAiCompatibleChatClient) {
        this.apiKeyResolver = apiKeyResolver;
        this.userAiSettingsService = userAiSettingsService;
        this.dashScopeVisionClient = dashScopeVisionClient;
        this.openAiCompatibleChatClient = openAiCompatibleChatClient;
    }

    public Map<String, Object> recognizeFoodInImage(MultipartFile imageFile, String userId) throws IOException {
        userAiSettingsService.assertVisionConfigured(userId);
        ResolvedAiConfig config = userAiSettingsService.resolve(userId);

        String mime = resolveMimeType(imageFile);
        String format = mime.contains("png") ? "png" : mime.contains("webp") ? "webp" : "jpg";
        byte[] bytes = ImageResizeUtil.resizeForVision(imageFile.getBytes(), format, maxImageSide);
        String imageDataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);

        List<String> errors = new ArrayList<>();
        List<String> providers = resolveProviderOrder(config);

        for (String provider : providers) {
            String p = provider.trim().toLowerCase();
            if (p.isEmpty()) {
                continue;
            }
            try {
                Map<String, Object> result = tryProvider(p, userId, config, imageDataUri);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                String msg = p + ": " + rootMessage(e);
                errors.add(msg);
                System.err.println("识图尝试失败 " + msg);
            }
        }

        throw new AiNotConfiguredException(
                "拍照识图不可用。请在「AI 设置」配置拍照识食服务商与 Key，或查看「使用手册」。"
                        + " 失败详情：" + String.join("；", errors));
    }

    /** 用户指定 visionProvider 时只试该服务商；否则走全局顺序 */
    private List<String> resolveProviderOrder(ResolvedAiConfig config) {
        String vp = config.getVisionProvider();
        if (vp != null && !vp.isBlank() && !"unset".equals(vp)) {
            return List.of(vp.trim().toLowerCase());
        }
        return List.of(providerOrder.split(","));
    }

    private Map<String, Object> tryProvider(String provider, String userId, ResolvedAiConfig config,
            String imageDataUri) throws Exception {
        return switch (provider) {
            case "doubao" -> tryDoubao(userId, config, imageDataUri);
            case "lmstudio" -> tryLmStudio(userId, config, imageDataUri);
            case "dashscope" -> tryDashscope(userId, config, imageDataUri);
            default -> null;
        };
    }

    private Map<String, Object> tryDoubao(String userId, ResolvedAiConfig config, String imageDataUri)
            throws Exception {
        ResolvedApiKey keyRef = apiKeyResolver.resolveDoubaoForVision(userId);
        if (!keyRef.isPresent()) {
            return null;
        }
        String model = resolveDoubaoVisionModel(config);
        ArkService arkService = ArkService.builder()
                .apiKey(keyRef.key())
                .baseUrl(ARK_BASE_URL)
                .build();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(FOOD_JSON_SYSTEM).build());
        List<ChatCompletionContentPart> parts = new ArrayList<>();
        parts.add(ChatCompletionContentPart.builder()
                .type("image_url")
                .imageUrl(new ChatCompletionContentPart.ChatCompletionContentPartImageURL(imageDataUri))
                .build());
        parts.add(ChatCompletionContentPart.builder().type("text").text(FOOD_JSON_USER_TEXT).build());
        messages.add(ChatMessage.builder().role(ChatMessageRole.USER).multiContent(parts).build());

        Object raw = arkService.createChatCompletion(ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .build()).getChoices().get(0).getMessage().getContent();
        apiKeyResolver.recordPlatformUsageIfNeeded(userId, keyRef, UsageKind.IMAGE);
        return parseFoodJson(raw != null ? raw.toString() : "", "doubao-vision", model);
    }

    private Map<String, Object> tryLmStudio(String userId, ResolvedAiConfig config, String imageDataUri)
            throws Exception {
        String baseUrl = firstNonBlank(config.getVisionLmstudioBaseUrl(), config.getLmstudioBaseUrl());
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String model = resolveLmVisionModel(config);
        if (model == null || model.isBlank()) {
            return null;
        }
        String json = openAiCompatibleChatClient.completeWithVisionImage(
                baseUrl, "lm-studio", model, imageDataUri, FOOD_JSON_SYSTEM, FOOD_JSON_USER_TEXT, 1024);
        return parseFoodJson(json, "lmstudio-vision", model);
    }

    private Map<String, Object> tryDashscope(String userId, ResolvedAiConfig config, String imageDataUri)
            throws Exception {
        ResolvedApiKey keyRef = apiKeyResolver.resolveDashscopeForVision(userId);
        if (!keyRef.isPresent()) {
            return null;
        }
        String model = resolveDashscopeVisionModel(config);
        String json = dashScopeVisionClient.recognizeFoodInImage(keyRef.key(), model, imageDataUri);
        apiKeyResolver.recordPlatformUsageIfNeeded(userId, keyRef, UsageKind.IMAGE);
        return parseFoodJson(json, "dashscope-vision", model);
    }

    private Map<String, Object> parseFoodJson(String jsonResponse, String mode, String model) throws Exception {
        jsonResponse = stripMarkdownJson(jsonResponse).trim();
        if (!jsonResponse.startsWith("{")) {
            int start = jsonResponse.indexOf('{');
            int end = jsonResponse.lastIndexOf('}');
            if (start >= 0 && end > start) {
                jsonResponse = jsonResponse.substring(start, end + 1);
            } else {
                throw new IllegalStateException("模型返回内容无法解析为 JSON");
            }
        }
        Map<String, Object> resultMap = objectMapper.readValue(jsonResponse, Map.class);
        dedupeFoods(resultMap);
        resultMap.put("mode", mode);
        resultMap.put("model", model);
        return resultMap;
    }

    private String resolveDoubaoVisionModel(ResolvedAiConfig config) {
        if (config.getVisionModel() != null && !config.getVisionModel().isBlank()
                && ("doubao".equals(config.getVisionProvider()) || config.getVisionProvider() == null)) {
            return config.getVisionModel().trim();
        }
        if (config.getCloudModel() != null && !config.getCloudModel().isBlank()) {
            String t = config.getCloudModel().trim();
            if (t.toLowerCase().contains("seed") || t.toLowerCase().contains("vision") || t.startsWith("ep-")) {
                return t;
            }
        }
        return defaultDoubaoVisionModel;
    }

    private String resolveDashscopeVisionModel(ResolvedAiConfig config) {
        if (config.getVisionModel() != null && !config.getVisionModel().isBlank()
                && "dashscope".equals(config.getVisionProvider())) {
            return config.getVisionModel().trim();
        }
        if (config.getCloudModel() != null && config.getCloudModel().toLowerCase().contains("vl")) {
            return config.getCloudModel().trim();
        }
        return dashScopeVisionClient.getDefaultVisionModel();
    }

    private String resolveLmVisionModel(ResolvedAiConfig config) {
        if (config.getVisionModel() != null && !config.getVisionModel().isBlank()) {
            return config.getVisionModel().trim();
        }
        if (lmstudioVisionModel != null && !lmstudioVisionModel.isBlank()) {
            return lmstudioVisionModel.trim();
        }
        String cloud = config.getCloudModel();
        if (cloud != null && cloud.toLowerCase().contains("vl")) {
            return cloud.trim();
        }
        String lm = config.getLmstudioModel();
        if (lm != null && lm.toLowerCase().contains("vl")) {
            return lm.trim();
        }
        return null;
    }

    private static String resolveMimeType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && !ct.isBlank()) {
            return ct;
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".png")) {
                return "image/png";
            }
            if (lower.endsWith(".webp")) {
                return "image/webp";
            }
            if (lower.endsWith(".gif")) {
                return "image/gif";
            }
        }
        return "image/jpeg";
    }

    private static String stripMarkdownJson(String text) {
        if (text == null) {
            return "";
        }
        String s = text;
        if (s.contains("```json")) {
            s = s.substring(s.indexOf("```json") + 7);
            int end = s.indexOf("```");
            if (end > 0) {
                s = s.substring(0, end);
            }
        } else if (s.contains("```")) {
            s = s.substring(s.indexOf("```") + 3);
            int end = s.indexOf("```");
            if (end > 0) {
                s = s.substring(0, end);
            }
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private void dedupeFoods(Map<String, Object> resultMap) {
        if (!resultMap.containsKey("foods")) {
            return;
        }
        List<Map<String, Object>> foods = (List<Map<String, Object>>) resultMap.get("foods");
        List<Map<String, Object>> uniqueFoods = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> food : foods) {
            if (food.containsKey("name")) {
                String name = food.get("name").toString().trim();
                if (!name.isEmpty() && !seen.contains(name)) {
                    seen.add(name);
                    uniqueFoods.add(food);
                }
            }
        }
        resultMap.put("foods", uniqueFoods);
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
