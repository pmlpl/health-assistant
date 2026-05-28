package com.example.healthassistant.ai;

import com.example.healthassistant.exception.AiNotConfiguredException;
import com.example.healthassistant.service.UserAiSettingsService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChatClientRouter {

    private final UserAiSettingsService userAiSettingsService;
    private final LmStudioChatClient lmStudioChatClient;
    private final DashScopeChatClient dashScopeChatClient;

    public ChatClientRouter(
            UserAiSettingsService userAiSettingsService,
            LmStudioChatClient lmStudioChatClient,
            DashScopeChatClient dashScopeChatClient) {
        this.userAiSettingsService = userAiSettingsService;
        this.lmStudioChatClient = lmStudioChatClient;
        this.dashScopeChatClient = dashScopeChatClient;
    }

    public String complete(String userId, String systemPrompt, String userPrompt) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new ChatMessage("system", systemPrompt));
        }
        messages.add(new ChatMessage("user", userPrompt));
        return completeWithMessages(userId, messages);
    }

    public String completeWithMessages(String userId, List<ChatMessage> messages) {
        userAiSettingsService.assertTextAiConfigured(userId);
        ResolvedAiConfig config = userAiSettingsService.resolve(userId);
        String provider = config.getTextProvider();

        if ("auto".equals(provider)) {
            try {
                return callLmStudio(config, messages);
            } catch (Exception e) {
                if (config.getDashscopeApiKey() != null && !config.getDashscopeApiKey().isBlank()) {
                    return dashScopeChatClient.complete(config.getDashscopeApiKey(), messages);
                }
                throw e;
            }
        }
        if ("dashscope".equals(provider)) {
            if (config.getDashscopeApiKey() == null || config.getDashscopeApiKey().isBlank()) {
                throw new AiNotConfiguredException("请配置 DashScope API Key");
            }
            return dashScopeChatClient.complete(config.getDashscopeApiKey(), messages);
        }
        return callLmStudio(config, messages);
    }

    private String callLmStudio(ResolvedAiConfig config, List<ChatMessage> messages) {
        if (config.getLmstudioBaseUrl() == null || config.getLmstudioBaseUrl().isBlank()) {
            throw new AiNotConfiguredException("请配置 LM Studio 服务地址");
        }
        String model = config.getLmstudioModel() != null ? config.getLmstudioModel() : "qwen3.5-9b";
        return lmStudioChatClient.complete(config.getLmstudioBaseUrl(), model, messages);
    }
}
