package com.example.healthassistant.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DashScopeChatClient {

    @Value("${ai.dashscope.default-model:qwen-plus}")
    private String defaultModel;

    @Value("${ai.dashscope.timeout-ms:300000}")
    private int timeoutMs;

    public String complete(String apiKey, String model, List<ChatMessage> messages, int maxTokens) {
        String modelId = (model != null && !model.isBlank()) ? model.trim() : defaultModel;
        try {
            List<Message> dashMessages = new ArrayList<>();
            for (ChatMessage m : messages) {
                dashMessages.add(Message.builder().role(m.role()).content(m.content()).build());
            }
            GenerationParam.GenerationParamBuilder<?, ?> builder = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(modelId)
                    .messages(dashMessages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE);
            if (maxTokens > 0) {
                builder.maxTokens(maxTokens);
            }
            GenerationParam param = builder.build();
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            throw new IllegalStateException("DashScope 调用失败: " + e.getMessage(), e);
        }
    }

    public boolean testConnection(String apiKey, String model) {
        return testConnectionDetailed(apiKey, model).success();
    }

    public ConnectionTestResult testConnectionDetailed(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            return new ConnectionTestResult(false, "DashScope API Key 未配置，请先填写并保存");
        }
        String modelId = (model != null && !model.isBlank()) ? model.trim() : defaultModel;
        try {
            complete(apiKey, modelId, List.of(
                    new ChatMessage(Role.SYSTEM.getValue(), "test"),
                    new ChatMessage(Role.USER.getValue(), "ping")), 256);
            return new ConnectionTestResult(true, "通义千问连接成功，模型：" + modelId);
        } catch (Exception e) {
            return new ConnectionTestResult(false,
                    "通义千问连接失败：" + rootMessage(e) + "。请检查 DashScope Key 与模型名是否正确。");
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
}
