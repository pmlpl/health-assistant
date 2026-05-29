package com.example.healthassistant.ai;

import com.example.healthassistant.ai.ResolvedApiKey;
import com.example.healthassistant.config.ApiKeyResolver;
import com.example.healthassistant.exception.AiNotConfiguredException;
import com.example.healthassistant.service.PlatformAiQuotaService.UsageKind;
import com.example.healthassistant.service.UserAiSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChatClientRouter {

    private static final ThreadLocal<String> LAST_BACKEND = new ThreadLocal<>();
    private static final ThreadLocal<String> LAST_MODEL = new ThreadLocal<>();

    private final UserAiSettingsService userAiSettingsService;
    private final LmStudioChatClient lmStudioChatClient;
    private final DashScopeChatClient dashScopeChatClient;
    private final OpenAiCompatibleChatClient openAiCompatibleChatClient;
    private final DoubaoChatClient doubaoChatClient;
    private final ApiKeyResolver apiKeyResolver;

    @Value("${ai.dashscope.default-model:qwen-plus}")
    private String defaultDashscopeModel;

    @Value("${ai.deepseek.default-model:deepseek-v4-flash}")
    private String defaultDeepseekModel;

    @Value("${ai.doubao.default-model:doubao-seed-1-8-251228}")
    private String defaultDoubaoModel;

    @Value("${ai.chat.max-tokens:1024}")
    private int maxTokens;

    @Value("${ai.auto.fallback-cloud:true}")
    private boolean autoFallbackCloud;

    public ChatClientRouter(
            UserAiSettingsService userAiSettingsService,
            LmStudioChatClient lmStudioChatClient,
            DashScopeChatClient dashScopeChatClient,
            OpenAiCompatibleChatClient openAiCompatibleChatClient,
            DoubaoChatClient doubaoChatClient,
            ApiKeyResolver apiKeyResolver) {
        this.userAiSettingsService = userAiSettingsService;
        this.lmStudioChatClient = lmStudioChatClient;
        this.dashScopeChatClient = dashScopeChatClient;
        this.openAiCompatibleChatClient = openAiCompatibleChatClient;
        this.doubaoChatClient = doubaoChatClient;
        this.apiKeyResolver = apiKeyResolver;
    }

    public String getLastUsedBackend() {
        return LAST_BACKEND.get();
    }

    public String getLastUsedModel() {
        return LAST_MODEL.get();
    }

    public String complete(String userId, String systemPrompt, String userPrompt) {
        return complete(userId, systemPrompt, userPrompt, maxTokens);
    }

    public String complete(String userId, String systemPrompt, String userPrompt, int tokenLimit) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new ChatMessage("system", systemPrompt));
        }
        messages.add(new ChatMessage("user", userPrompt));
        return completeWithMessages(userId, messages, tokenLimit);
    }

    public String completeWithMessages(String userId, List<ChatMessage> messages) {
        return completeWithMessages(userId, messages, maxTokens);
    }

    public String completeWithMessages(String userId, List<ChatMessage> messages, int tokenLimit) {
        int effective = tokenLimit > 0 ? tokenLimit : maxTokens;
        userAiSettingsService.assertTextAiConfigured(userId);
        ResolvedAiConfig config = userAiSettingsService.resolve(userId);
        String provider = config.getTextProvider();

        if ("auto".equals(provider)) {
            try {
                return callLmStudio(config, messages, effective);
            } catch (Exception lmError) {
                if (!autoFallbackCloud) {
                    throw new IllegalStateException(
                            "本地 LM Studio 不可用（开发模式未开启云端回退）。请确认 LM Studio 已启动、地址与模型正确。原因: "
                                    + lmError.getMessage(), lmError);
                }
                System.out.println("[AI] 自动模式：LM Studio 失败，回退云端 - " + lmError.getMessage());
                return callFirstAvailableCloud(userId, config, messages, lmError, effective);
            }
        }

        return switch (provider) {
            case "dashscope" -> callDashscope(userId, config, messages, effective);
            case "deepseek" -> callDeepseek(userId, config, messages, effective);
            case "doubao" -> callDoubao(userId, config, messages, effective);
            case "other" -> callOther(config, messages, effective);
            case "lmstudio" -> callLmStudio(config, messages, effective);
            default -> callLmStudio(config, messages, effective);
        };
    }

    public void streamWithMessages(String userId, List<ChatMessage> messages, AiStreamChunkConsumer consumer) {
        userAiSettingsService.assertTextAiConfigured(userId);
        ResolvedAiConfig config = userAiSettingsService.resolve(userId);
        String provider = config.getTextProvider();

        if ("auto".equals(provider)) {
            try {
                streamLmStudio(config, messages, consumer);
                return;
            } catch (Exception lmError) {
                if (!autoFallbackCloud) {
                    throw new IllegalStateException(
                            "本地 LM Studio 流式不可用: " + lmError.getMessage(), lmError);
                }
                emitAsPseudoStream(callFirstAvailableCloud(userId, config, messages, lmError, maxTokens), consumer);
                return;
            }
        }

        switch (provider) {
            case "lmstudio" -> streamLmStudio(config, messages, consumer);
            case "deepseek" -> streamDeepseek(config, messages, consumer);
            case "other" -> streamOther(config, messages, consumer);
            default -> emitAsPseudoStream(completeWithMessages(userId, messages), consumer);
        }
    }

    private static void emitAsPseudoStream(String fullText, AiStreamChunkConsumer consumer) {
        if (fullText == null || fullText.isEmpty()) {
            return;
        }
        int step = 40;
        for (int i = 0; i < fullText.length(); i += step) {
            consumer.onChunk(fullText.substring(i, Math.min(i + step, fullText.length())));
        }
    }

    private void trackCall(String backend, String model, String detail) {
        LAST_BACKEND.set(backend);
        LAST_MODEL.set(model);
        System.out.println("[AI] 实际调用: " + backend + " | model=" + model + " | " + detail);
    }

    private void streamLmStudio(ResolvedAiConfig config, List<ChatMessage> messages, AiStreamChunkConsumer consumer) {
        if (config.getLmstudioBaseUrl() == null || config.getLmstudioBaseUrl().isBlank()) {
            throw new AiNotConfiguredException("请配置 LM Studio 服务地址");
        }
        String model = resolveLmModel(config);
        trackCall("lmstudio", model, config.getLmstudioBaseUrl());
        // 与连接测试相同：始终走非流式 complete，再切片推送 SSE，避免 OpenAI stream 卡住导致 LM 无请求
        String full = lmStudioChatClient.complete(config.getLmstudioBaseUrl(), model, messages, maxTokens);
        emitAsPseudoStream(full, consumer);
    }

    private void streamDeepseek(ResolvedAiConfig config, List<ChatMessage> messages, AiStreamChunkConsumer consumer) {
        String key = requireKey(config.getDeepseekApiKey(), "请配置 DeepSeek API Key");
        String model = resolveCloudModel(config, defaultDeepseekModel);
        trackCall("deepseek", model, OpenAiCompatibleChatClient.DEEPSEEK_DEFAULT_BASE);
        openAiCompatibleChatClient.stream(
                OpenAiCompatibleChatClient.DEEPSEEK_DEFAULT_BASE,
                key,
                model,
                messages,
                maxTokens,
                consumer);
    }

    private void streamOther(ResolvedAiConfig config, List<ChatMessage> messages, AiStreamChunkConsumer consumer) {
        String base = config.getCustomApiBaseUrl();
        if (base == null || base.isBlank()) {
            throw new AiNotConfiguredException("请配置「其他」API 根地址");
        }
        String key = requireKey(config.getCustomApiKey(), "请配置「其他」API Key");
        String model = resolveCloudModel(config, "gpt-3.5-turbo");
        trackCall("other", model, base);
        openAiCompatibleChatClient.stream(base, key, model, messages, maxTokens, consumer);
    }

    private String callFirstAvailableCloud(String userId, ResolvedAiConfig config, List<ChatMessage> messages,
            Exception lmError, int tokens) {
        List<Exception> errors = new ArrayList<>();
        errors.add(lmError);
        try {
            return callDeepseek(userId, config, messages, tokens);
        } catch (Exception e) {
            errors.add(e);
        }
        try {
            return callDashscope(userId, config, messages, tokens);
        } catch (Exception e) {
            errors.add(e);
        }
        try {
            return callDoubao(userId, config, messages, tokens);
        } catch (Exception e) {
            errors.add(e);
        }
        if (hasKey(config.getCustomApiKey()) && config.getCustomApiBaseUrl() != null) {
            try {
                return callOther(config, messages, tokens);
            } catch (Exception e) {
                errors.add(e);
            }
        }
        throw new IllegalStateException("自动模式：本地与云端均不可用 - " + errors.get(errors.size() - 1).getMessage());
    }

    private String callDashscope(String userId, ResolvedAiConfig config, List<ChatMessage> messages, int tokens) {
        ResolvedApiKey keyRef = apiKeyResolver.resolveDashscopeForText(userId);
        String key = requireResolvedKey(keyRef, "请配置通义千问 DashScope API Key");
        String model = resolveCloudModel(config, defaultDashscopeModel);
        trackCall("dashscope", model, "dashscope");
        String result = dashScopeChatClient.complete(key, model, messages, tokens);
        apiKeyResolver.recordPlatformUsageIfNeeded(userId, keyRef, UsageKind.TEXT);
        return result;
    }

    private String callDeepseek(String userId, ResolvedAiConfig config, List<ChatMessage> messages, int tokens) {
        ResolvedApiKey keyRef = apiKeyResolver.resolveDeepseekForText(userId);
        String key = requireResolvedKey(keyRef, "请配置 DeepSeek API Key");
        String model = resolveCloudModel(config, defaultDeepseekModel);
        trackCall("deepseek", model, OpenAiCompatibleChatClient.DEEPSEEK_DEFAULT_BASE);
        String result = openAiCompatibleChatClient.complete(
                OpenAiCompatibleChatClient.DEEPSEEK_DEFAULT_BASE,
                key,
                model,
                messages,
                tokens);
        apiKeyResolver.recordPlatformUsageIfNeeded(userId, keyRef, UsageKind.TEXT);
        return result;
    }

    private String callDoubao(String userId, ResolvedAiConfig config, List<ChatMessage> messages, int tokens) {
        ResolvedApiKey keyRef = apiKeyResolver.resolveDoubaoForText(userId);
        String key = requireResolvedKey(keyRef, "请配置豆包 API Key");
        String model = resolveCloudModel(config, defaultDoubaoModel);
        trackCall("doubao", model, "doubao");
        String result = doubaoChatClient.complete(key, model, messages, tokens);
        apiKeyResolver.recordPlatformUsageIfNeeded(userId, keyRef, UsageKind.TEXT);
        return result;
    }

    private String callOther(ResolvedAiConfig config, List<ChatMessage> messages, int tokens) {
        String base = config.getCustomApiBaseUrl();
        if (base == null || base.isBlank()) {
            throw new AiNotConfiguredException("请配置「其他」API 根地址");
        }
        String key = requireKey(config.getCustomApiKey(), "请配置「其他」API Key");
        String model = resolveCloudModel(config, "gpt-3.5-turbo");
        trackCall("other", model, base);
        return openAiCompatibleChatClient.complete(base, key, model, messages, tokens);
    }

    private String callLmStudio(ResolvedAiConfig config, List<ChatMessage> messages, int tokens) {
        if (config.getLmstudioBaseUrl() == null || config.getLmstudioBaseUrl().isBlank()) {
            throw new AiNotConfiguredException("请配置 LM Studio 服务地址");
        }
        String model = resolveLmModel(config);
        trackCall("lmstudio", model, config.getLmstudioBaseUrl());
        return lmStudioChatClient.complete(config.getLmstudioBaseUrl(), model, messages, tokens);
    }

    private static String resolveLmModel(ResolvedAiConfig config) {
        if (config.getLmstudioModel() != null && !config.getLmstudioModel().isBlank()) {
            return config.getLmstudioModel().trim();
        }
        return "local-model";
    }

    private static String resolveCloudModel(ResolvedAiConfig config, String fallback) {
        if (config.getCloudModel() != null && !config.getCloudModel().isBlank()) {
            return config.getCloudModel().trim();
        }
        return fallback;
    }

    private static String requireResolvedKey(ResolvedApiKey keyRef, String message) {
        if (keyRef == null || !keyRef.isPresent()) {
            throw new AiNotConfiguredException(message);
        }
        return keyRef.key();
    }

    private static String requireKey(String key, String message) {
        if (!hasKey(key)) {
            throw new AiNotConfiguredException(message);
        }
        return key;
    }

    private static boolean hasKey(String key) {
        return key != null && !key.isBlank();
    }
}
