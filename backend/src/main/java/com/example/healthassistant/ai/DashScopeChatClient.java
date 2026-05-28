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

    private static final String MODEL = "qwen-plus";

    @Value("${ai.dashscope.timeout-ms:300000}")
    private int timeoutMs;

    public String complete(String apiKey, List<ChatMessage> messages) {
        try {
            List<Message> dashMessages = new ArrayList<>();
            for (ChatMessage m : messages) {
                dashMessages.add(Message.builder().role(m.role()).content(m.content()).build());
            }
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(MODEL)
                    .messages(dashMessages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            throw new IllegalStateException("DashScope 调用失败: " + e.getMessage(), e);
        }
    }

    public boolean testConnection(String apiKey) {
        try {
            complete(apiKey, List.of(
                    new ChatMessage(Role.SYSTEM.getValue(), "test"),
                    new ChatMessage(Role.USER.getValue(), "ping")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
