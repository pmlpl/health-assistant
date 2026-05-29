package com.example.healthassistant.ai;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 豆包 / 火山方舟文本对话（与食物识别共用 API Key）。
 */
@Component
public class DoubaoChatClient {

    private static final String ARK_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    public String complete(String apiKey, String model, List<com.example.healthassistant.ai.ChatMessage> messages, int maxTokens) {
        ArkService arkService = ArkService.builder()
                .apiKey(apiKey)
                .baseUrl(ARK_BASE_URL)
                .build();
        try {
            List<ChatMessage> arkMessages = new ArrayList<>();
            for (com.example.healthassistant.ai.ChatMessage m : messages) {
                ChatMessageRole role = mapRole(m.role());
                arkMessages.add(ChatMessage.builder().role(role).content(m.content()).build());
            }
            ChatCompletionRequest.Builder reqBuilder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(arkMessages);
            if (maxTokens > 0) {
                reqBuilder.maxTokens(maxTokens);
            }
            ChatCompletionRequest request = reqBuilder.build();
            Object content = arkService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
            return content != null ? content.toString() : "";
        } catch (Exception e) {
            throw new IllegalStateException("豆包调用失败: " + e.getMessage(), e);
        } finally {
            try {
                arkService.shutdownExecutor();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean testConnection(String apiKey, String model) {
        return testConnectionDetailed(apiKey, model).success();
    }

    public ConnectionTestResult testConnectionDetailed(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            return new ConnectionTestResult(false, "豆包 API Key 未配置，请先填写并保存");
        }
        String modelId = (model != null && !model.isBlank()) ? model.trim() : "doubao-seed-1-8-251228";
        try {
            complete(apiKey, modelId, List.of(new com.example.healthassistant.ai.ChatMessage("user", "ping")), 256);
            return new ConnectionTestResult(true, "豆包连接成功，模型：" + modelId);
        } catch (Exception e) {
            return new ConnectionTestResult(false,
                    "豆包连接失败：" + rootMessage(e) + "。请检查 API Key 与模型/接入点 ID。");
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

    private static ChatMessageRole mapRole(String role) {
        if ("system".equalsIgnoreCase(role)) {
            return ChatMessageRole.SYSTEM;
        }
        if ("assistant".equalsIgnoreCase(role)) {
            return ChatMessageRole.ASSISTANT;
        }
        return ChatMessageRole.USER;
    }
}
