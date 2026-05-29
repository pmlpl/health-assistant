package com.example.healthassistant.service;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.example.healthassistant.config.EnvConfig;

@Service
public class ImageRecognitionService {

    @Value("${doubao.api.key:}")
    private String apiKeyFromConfig;

    @Value("${doubao.model.name:doubao-vision-pro-32k}")
    private String modelName;

    /**
     * 获取 API Key，优先从 .env 文件加载
     */
    private String getApiKey() {
        // 优先从 EnvConfig 获取（支持 .env 文件）
        String key = EnvConfig.getDoubaoApiKey();
        if (key != null && !key.isEmpty() && !key.contains("your_")) {
            return key;
        }
        // 其次从 Spring 配置获取
        if (apiKeyFromConfig != null && !apiKeyFromConfig.isEmpty()) {
            return apiKeyFromConfig;
        }
        return null;
    }

    public Map<String, Object> recognizeFoodInImage(MultipartFile imageFile, String userId) throws IOException {
        String effectiveApiKey = getApiKey();

        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            System.out.println("豆包 API 密钥未配置，使用模拟数据");
            return generateMockRecognitionResult(imageFile);
        }

        try {
            ArkService arkService = ArkService.builder()
                    .apiKey(effectiveApiKey)
                    .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                    .build();

            // 将图片文件转换为Base64编码
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            String imageUrl = "data:image/jpeg;base64," + base64Image;

            List<ChatMessage> messages = new ArrayList<>();

            // 系统提示词
            ChatMessage systemMessage = ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(
                            "你是一个专业的食物营养分析师。你的任务是识别图片中的食物，并估算它们的重量和营养成分。请严格遵循以下要求：\n1. 只返回图片中实际存在的食物，不要返回不存在的食物\n2. 每种食物只返回一次，不要重复\n3. 对于单个食物，只返回一个条目\n4. 请以JSON格式返回结果，格式为： {\"foods\": [{\"name\": \"食物名称\", \"weightGrams\": 重量（克）, \"calories\": 热量（千卡）, \"protein\": 蛋白质（克）, \"carbs\": 碳水化合物（克）, \"fat\": 脂肪（克）, \"fiber\": 纤维（克）}]}。\n5. 所有营养成分数值均为每100克的含量\n6. 请确保数值精确到小数点后一位\n7. 只返回JSON，不要任何多余的解释。")
                    .build();
            messages.add(systemMessage);

            // 用户图片和文字
            List<ChatCompletionContentPart> contentParts = new ArrayList<>();
            contentParts.add(ChatCompletionContentPart.builder()
                    .type("image_url")
                    .imageUrl(new ChatCompletionContentPart.ChatCompletionContentPartImageURL(imageUrl))
                    .build());
            contentParts.add(ChatCompletionContentPart.builder()
                    .type("text")
                    .text("请识别这张图片里的食物及其重量，并以JSON格式返回。")
                    .build());

            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .multiContent(contentParts)
                    .build();
            messages.add(userMessage);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(modelName)
                    .messages(messages)
                    .build();

            String jsonResponse = arkService.createChatCompletion(request).getChoices().get(0).getMessage().getContent()
                    .toString();

            // 处理可能的Markdown代码块
            if (jsonResponse.contains("```json")) {
                jsonResponse = jsonResponse.substring(jsonResponse.indexOf("```json") + 7);
                jsonResponse = jsonResponse.substring(0, jsonResponse.lastIndexOf("```"));
            } else if (jsonResponse.contains("```")) {
                jsonResponse = jsonResponse.substring(jsonResponse.indexOf("```") + 3);
                jsonResponse = jsonResponse.substring(0, jsonResponse.lastIndexOf("```"));
            }

            // 清理JSON字符串，移除可能的多余字符
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("{") && jsonResponse.endsWith("}")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> resultMap = objectMapper.readValue(jsonResponse, Map.class);

                    // 对食物列表进行去重处理
                    if (resultMap.containsKey("foods")) {
                        List<Map<String, Object>> foods = (List<Map<String, Object>>) resultMap.get("foods");
                        List<Map<String, Object>> uniqueFoods = new ArrayList<>();
                        Set<String> seenFoods = new HashSet<>();

                        for (Map<String, Object> food : foods) {
                            if (food.containsKey("name")) {
                                String foodName = food.get("name").toString().trim();
                                if (!seenFoods.contains(foodName)) {
                                    seenFoods.add(foodName);
                                    uniqueFoods.add(food);
                                }
                            }
                        }

                        // 更新去重后的食物列表
                        resultMap.put("foods", uniqueFoods);
                    }

                    return resultMap;
                } catch (Exception e) {
                    System.err.println("JSON解析失败: " + e.getMessage());
                    // 解析失败时返回模拟数据
                    return generateMockRecognitionResult(imageFile);
                }
            } else {
                System.err.println("无效的JSON格式: " + jsonResponse);
                // 无效格式时返回模拟数据
                return generateMockRecognitionResult(imageFile);
            }

        } catch (Exception e) {
            System.err.println("图片识别失败: " + e.getMessage());
            e.printStackTrace();
            return generateMockRecognitionResult(imageFile);
        }
    }

    private Map<String, Object> generateMockRecognitionResult(MultipartFile imageFile) {
        Map<String, Object> mockResult = new HashMap<>();
        List<Map<String, Object>> foods = List.of(
                Map.of("name", "模拟-米饭", "weightGrams", 200, "calories", 116.0, "protein", 2.6, "carbs", 25.9, "fat",
                        0.3, "fiber", 0.3),
                Map.of("name", "模拟-西兰花炒鸡胸肉", "weightGrams", 250, "calories", 150.0, "protein", 20.0, "carbs", 10.0,
                        "fat", 5.0, "fiber", 3.0),
                Map.of("name", "模拟-番茄鸡蛋汤", "weightGrams", 150, "calories", 30.0, "protein", 2.0, "carbs", 4.0, "fat",
                        1.0, "fiber", 1.0));
        mockResult.put("foods", foods);
        return mockResult;
    }
}
