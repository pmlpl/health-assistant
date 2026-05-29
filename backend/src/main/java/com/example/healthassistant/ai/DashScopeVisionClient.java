package com.example.healthassistant.ai;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 通义千问视觉模型（DashScope MultiModalConversation），用于拍照识食。
 */
@Component
public class DashScopeVisionClient {

    private static final String FOOD_JSON_PROMPT = """
            你是一个专业的食物营养分析师。识别图片中的食物并估算重量与营养成分。
            要求：只返回图中真实存在的食物；每种食物一条；仅输出 JSON，不要 markdown 说明。
            格式：{"foods":[{"name":"食物名称","weightGrams":重量克数,"calories":每100克千卡,\
            "protein":蛋白质克,"carbs":碳水克,"fat":脂肪克,"fiber":纤维克}]}
            """;

    /** 默认轻量视觉模型，比 qwen-vl-max 便宜 */
    @Value("${ai.dashscope.vision-model:qwen-vl-flash}")
    private String defaultVisionModel;

    public String getDefaultVisionModel() {
        return defaultVisionModel;
    }

    public String recognizeFoodInImage(String apiKey, String model, String imageDataUri) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("DashScope API Key 未配置");
        }
        String modelId = (model != null && !model.isBlank()) ? model.trim() : defaultVisionModel;

        try {
            MultiModalConversation conv = new MultiModalConversation();

            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Arrays.asList(
                            Map.of("image", imageDataUri),
                            Map.of("text", FOOD_JSON_PROMPT + "\n请识别这张图片里的食物及其重量，只返回 JSON。")))
                    .build();

            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(modelId)
                    .messages(List.of(userMessage))
                    .build();

            MultiModalConversationResult result = conv.call(param);
            if (result == null || result.getOutput() == null
                    || result.getOutput().getChoices() == null
                    || result.getOutput().getChoices().isEmpty()) {
                throw new IllegalStateException("通义视觉模型返回为空");
            }
            Object content = result.getOutput().getChoices().get(0).getMessage().getContent();
            if (content == null) {
                throw new IllegalStateException("通义视觉模型未返回文本内容");
            }
            // content 可能是 List 或 String
            if (content instanceof List<?> parts) {
                StringBuilder sb = new StringBuilder();
                for (Object part : parts) {
                    if (part instanceof Map<?, ?> map && map.containsKey("text")) {
                        sb.append(map.get("text"));
                    } else if (part != null) {
                        sb.append(part);
                    }
                }
                return sb.toString();
            }
            return content.toString();
        } catch (Exception e) {
            throw new IllegalStateException("通义千问视觉识别失败: " + rootMessage(e), e);
        }
    }

    public ConnectionTestResult testVisionDetailed(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            return new ConnectionTestResult(false, "DashScope API Key 未配置，请先填写并保存");
        }
        String modelId = (model != null && !model.isBlank()) ? model.trim() : defaultVisionModel;
        if (!modelId.toLowerCase().contains("vl")) {
            return new ConnectionTestResult(false,
                    "当前模型「" + modelId + "」不是视觉模型，拍照识食请使用 qwen-vl-flash / qwen-vl-plus");
        }
        try {
            // 最小 1x1 PNG 透明图 base64，仅验证 Key + 模型可用
            String tinyPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
            recognizeFoodInImage(apiKey, modelId, tinyPng);
            return new ConnectionTestResult(true, "通义视觉模型连接成功：" + modelId);
        } catch (Exception e) {
            return new ConnectionTestResult(false,
                    "通义视觉测试失败：" + rootMessage(e) + "。请确认已开通 qwen-vl-flash（推荐）或 qwen-vl-plus。");
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
