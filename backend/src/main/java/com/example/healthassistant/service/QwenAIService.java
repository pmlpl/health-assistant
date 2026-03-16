// HealthAssistant/src/main/java/com/example/healthassistant/service/QwenAIService.java
package com.example.healthassistant.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.example.healthassistant.config.EnvConfig;

@Service
public class QwenAIService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Value("${dashscope.api.key:}")
    private String configApiKey;

    /**
     * 获取 API Key，优先从 .env 文件加载
     */
    private String getApiKey() {
        // 优先从 EnvConfig 获取（支持 .env 文件）
        String key = EnvConfig.getDashscopeApiKey();
        if (key != null && !key.isEmpty() && !key.contains("your_")) {
            return key;
        }
        // 其次从 Spring 配置获取
        if (configApiKey != null && !configApiKey.isEmpty()) {
            return configApiKey;
        }
        return null;
    }

    private static final String MODEL_NAME = "qwen-plus"; // 或者使用 "qwen-plus" 或 "qwen-max"/qwen-turbo

    // 存储用户的会话历史 (userId -> List<Message>)
    private final Map<String, List<Message>> sessionHistories = new ConcurrentHashMap<>();

    // API 调用超时时间（毫秒）
    private static final int API_TIMEOUT_MS = 300000;

    // API 调用最大重试次数
    private static final int MAX_RETRY_COUNT = 10;

    // 重试间隔时间（毫秒）
    private static final int RETRY_INTERVAL_MS = 1000;

    // 会话历史最大长度
    private static final int MAX_HISTORY_LENGTH = 1000;

    public String getNutritionAdvice(String userId, String userMessage) {
        System.out.println("正在调用 AI 服务，用户 ID: " + userId + ", 用户消息：" + userMessage);
    
        // 检查 API 密钥是否配置，优先从 .env 文件加载
        String apiKey = getApiKey();
    
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("==============================================");
            System.out.println("通义千问 API 密钥未配置，切换到本地演示模式");
            System.out.println("提示：如需使用真实 AI 服务，请在 backend/.env 文件中设置 DASHSCOPE_API_KEY");
            System.out.println("==============================================");
            return generateMockResponse(userMessage);
        }
    
        try {
            // 获取用户档案信息
            UserProfile userProfile = userProfileRepository.findByUserIdWithDietaryRestrictions(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);

            // 获取或创建该用户的会话历史
            List<Message> history = sessionHistories.computeIfAbsent(userId, k -> new ArrayList<>());

            // 添加系统角色设定（只在第一次对话时添加）
            if (history.isEmpty()) {
                String systemPrompt = buildSystemPrompt(userProfileInfo);
                Message systemMsg = Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(systemPrompt)
                        .build();
                history.add(systemMsg);
            }

            // 添加用户当前消息
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
                    .build();
            history.add(userMsg);

            // 如果历史太长，保留最近的几条消息
            if (history.size() > MAX_HISTORY_LENGTH) {
                // 保留系统消息和最近的用户和助手消息
                List<Message> trimmedHistory = new ArrayList<>();
                // 保留系统消息
                if (!history.isEmpty() && Role.SYSTEM.getValue().equals(history.get(0).getRole())) {
                    trimmedHistory.add(history.get(0));
                }
                // 保留最近的用户和助手消息
                int startIndex = Math.max(1, history.size() - (MAX_HISTORY_LENGTH - 1));
                trimmedHistory.addAll(history.subList(startIndex, history.size()));
                history.clear();
                history.addAll(trimmedHistory);
            }

            // 构建参数
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey) // 使用配置文件或环境变量中的API密钥
                    .model(MODEL_NAME)
                    .messages(history)
                    .resultFormat("message")
                    .temperature(0.7f) // 设置随机性，使用float类型
                    .topP(0.8) // 设置采样参数，使用float类型
                    .build();

            // 调用API（带超时和重试机制）
            GenerationResult result = callApiWithRetry(param);

            // 提取回复内容
            String response = result.getOutput().getChoices().get(0).getMessage().getContent();

            // 添加助手回复到历史记录
            Message assistantMsg = Message.builder()
                    .role(Role.ASSISTANT.getValue())
                    .content(response)
                    .build();
            history.add(assistantMsg);

            System.out.println("AI返回内容: " + response);
            return response;

        } catch (NoApiKeyException e) {
            System.err.println("API密钥缺失: " + e.getMessage());
            return generateMockResponse(userMessage);
        } catch (ApiException | InputRequiredException e) {
            System.err.println("API调用异常: " + e.getMessage());
            e.printStackTrace();
            return "服务暂时不可用：" + e.getMessage();
        } catch (Exception e) {
            System.err.println("未知异常: " + e.getMessage());
            e.printStackTrace();
            return "服务暂时不可用：" + e.getMessage();
        }
    }

    public Map<String, Object> generateRecipeRecommendations(UserProfile userProfile,
            Map<String, Double> consumedNutrition, String mealType) {
        String effectiveApiKey = getApiKey();
    
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            System.out.println("通义千问 API 密钥未配置，使用模拟食谱推荐");
            return generateMockRecommendations(userProfile, mealType);
        }

        try {
            String prompt = buildRecipePrompt(userProfile, consumedNutrition, mealType);

            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(
                            "你是一名顶级营养师和厨师。你的任务是根据用户的详细健康数据和今日已摄入的营养，为他们推荐下一餐的食谱。请以JSON格式返回，格式为：{\"analysis\": \"分析用户今日营养摄入情况，指出缺少的营养素或需要控制的营养素，以及推荐理由（100字以内）\", \"recommendations\": [{\"recipeName\": \"食谱名称\", \"description\": \"描述\", \"calories\": 卡路里, \"protein\": 蛋白质, \"carbs\": 碳水, \"fat\": 脂肪, \"ingredients\": [\"食材1\", \"食材2\"], \"instructions\": [\"步骤1\", \"步骤2\"]}]}。只返回JSON，不要任何多余的解释。")
                    .build();

            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .model(MODEL_NAME)
                    .messages(List.of(systemMsg, userMsg))
                    .apiKey(effectiveApiKey)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            // 调用API（带超时和重试机制）
            GenerationResult result = callApiWithRetry(param);

            String jsonResponse = result.getOutput().getChoices().get(0).getMessage().getContent();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
            
            // 为所有食谱生成图片（异步串行处理，避免触发频率限制）
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) responseMap.get("recommendations");
            if (recommendations != null && !recommendations.isEmpty()) {
                System.out.println("📊 共有 " + recommendations.size() + " 个食谱，开始串行生成图片...");
                
                // 串行生成图片，每个间隔 1 秒，避免触发 429 限流
                int successCount = 0;
                for (int i = 0; i < recommendations.size(); i++) {
                    final int index = i;
                    final Map<String, Object> recipe = recommendations.get(i);
                    
                    try {
                        String recipeName = (String) recipe.get("recipeName");
                        String description = (String) recipe.get("description");
                        @SuppressWarnings("unchecked")
                        List<String> ingredients = (List<String>) recipe.get("ingredients");
                        
                        // 生成图片提示词
                        String imagePrompt = buildImagePrompt(recipeName, description, ingredients);
                        
                        // 调用通义万相生成图片
                        String imageUrl = generateRecipeImage(imagePrompt, effectiveApiKey);
                        
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            recipe.put("image", imageUrl);
                            System.out.println("✅ 第 " + (index + 1) + " 张图片生成成功：" + recipeName);
                            successCount++;
                        } else {
                            System.out.println("⚠️ 第 " + (index + 1) + " 张图片生成失败：" + recipeName);
                        }
                        
                        // 延迟 1.5 秒，避免触发 429 限流
                        if (i < recommendations.size() - 1) {
                            Thread.sleep(1500);
                        }
                    } catch (InterruptedException e) {
                        System.err.println("⚠️ 第 " + (index + 1) + " 张图片生成被中断：" + e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("❌ 第 " + (index + 1) + " 张图片生成异常：" + e.getMessage());
                        e.printStackTrace();
                        // 继续生成下一张，不中断整个流程
                    }
                }
                
                System.out.println("✨ 所有食谱图片生成完成！成功：" + successCount + "/" + recommendations.size());
            }
            
            return responseMap;

        } catch (Exception e) {
            System.err.println("AI食谱推荐失败: " + e.getMessage());
            e.printStackTrace();
            return generateMockRecommendations(userProfile, mealType);
        }
    }

    private String buildRecipePrompt(UserProfile userProfile, Map<String, Double> consumedNutrition, String mealType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为我推荐一顿").append(translateMealType(mealType)).append("。\n\n");
        prompt.append("【我的健康档案】\n");
        prompt.append(buildUserProfileInfo(userProfile)); // 复用已有的方法
        prompt.append("\n【我今天已经吃了】\n");
        prompt.append("- 总热量: ").append(String.format("%.1f", consumedNutrition.get("calories"))).append(" kcal\n");
        prompt.append("- 蛋白质: ").append(String.format("%.1f", consumedNutrition.get("protein"))).append(" g\n");
        prompt.append("- 碳水: ").append(String.format("%.1f", consumedNutrition.get("carbs"))).append(" g\n");
        prompt.append("- 脂肪: ").append(String.format("%.1f", consumedNutrition.get("fat"))).append(" g\n");
        prompt.append("\n请根据我的目标和我今天已有的摄入，为我推荐3个不同的").append(translateMealType(mealType))
                .append("食谱。请确保推荐的食谱能帮助我更好地达成健康目标，同时要考虑我的口味偏好和饮食禁忌。请以JSON格式返回结果。\n");
        return prompt.toString();
    }

    private String translateMealType(String mealType) {
        return switch (mealType) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            case "DINNER" -> "晚餐";
            case "SNACK" -> "加餐";
            default -> "餐食";
        };
    }

    private Map<String, Object> generateMockRecommendations(UserProfile userProfile, String mealType) {
        Map<String, Object> mockData = new HashMap<>();

        mockData.put("analysis", "根据您今日的营养摄入情况，建议适当增加蛋白质摄入，控制碳水化合物。以下推荐食谱富含优质蛋白，有助于达成您的健康目标。");

        List<Map<String, Object>> recommendations = List.of(
                Map.of("recipeName", "模拟-活力鸡胸肉沙拉",
                        "description", "一份清爽、高蛋白的沙拉，适合减脂和健身人群。",
                        "calories", 350,
                        "protein", 40,
                        "carbs", 15,
                        "fat", 15,
                        "ingredients", List.of("鸡胸肉 150g", "生菜", "圣女果", "黄瓜", "橄榄油", "黑胡椒"),
                        "instructions", List.of("鸡胸肉煮熟或煎熟，切块。", "所有蔬菜洗净切好，与鸡胸肉混合。", "淋上橄榄油，撒上黑胡椒即可。")),
                Map.of("recipeName", "模拟-能量糙米饭套餐",
                        "description", "均衡营养的套餐，提供稳定的能量释放，适合所有人群。",
                        "calories", 500,
                        "protein", 30,
                        "carbs", 60,
                        "fat", 18,
                        "ingredients", List.of("糙米饭 1碗", "清蒸鱼 100g", "炒时蔬", "豆腐汤"),
                        "instructions", List.of("将鱼清蒸。", "蔬菜快炒。", "组合成一份营养均衡的套餐。")),
                Map.of("recipeName", "模拟-暖心蔬菜豆腐汤",
                        "description", "低卡、暖胃的素食选择，富含植物蛋白和纤维。",
                        "calories", 250,
                        "protein", 20,
                        "carbs", 20,
                        "fat", 10,
                        "ingredients", List.of("嫩豆腐 1块", "香菇", "青菜", "胡萝卜", "盐"),
                        "instructions", List.of("所有食材切好。", "锅中加水烧开，放入所有食材煮10分钟。", "加盐调味即可。")));
        mockData.put("recommendations", recommendations);
        mockData.put("message", "这是一个模拟推荐，因为未配置API密钥。");
        return mockData;
    }

    // 构建用户档案信息字符串
    private String buildUserProfileInfo(UserProfile userProfile) {
        if (userProfile == null) {
            return "用户档案信息未找到";
        }

        StringBuilder profileInfo = new StringBuilder();
        profileInfo.append("用户档案信息：\n");

        if (userProfile.getHeight() != null) {
            profileInfo.append("- 身高：").append(userProfile.getHeight()).append("cm\n");
        }
        if (userProfile.getWeight() != null) {
            profileInfo.append("- 体重：").append(userProfile.getWeight()).append("kg\n");
        }
        if (userProfile.getAge() != null) {
            profileInfo.append("- 年龄：").append(userProfile.getAge()).append("岁\n");
        }
        if (userProfile.getGender() != null) {
            profileInfo.append("- 性别：").append(userProfile.getGender()).append("\n");
        }
        if (userProfile.getActivityLevel() != null) {
            profileInfo.append("- 活动量：").append(userProfile.getActivityLevel()).append("\n");
        }
        if (userProfile.getHealthGoal() != null) {
            profileInfo.append("- 健康目标：").append(userProfile.getHealthGoal()).append("\n");
        }
        if (userProfile.getTargetCalories() != null) {
            profileInfo.append("- 目标卡路里：").append(userProfile.getTargetCalories()).append("kcal/天\n");
        }
        if (userProfile.getTargetProtein() != null) {
            profileInfo.append("- 目标蛋白质：").append(userProfile.getTargetProtein()).append("g/天\n");
        }
        if (userProfile.getTargetCarbs() != null) {
            profileInfo.append("- 目标碳水化合物：").append(userProfile.getTargetCarbs()).append("g/天\n");
        }
        if (userProfile.getTargetFat() != null) {
            profileInfo.append("- 目标脂肪：").append(userProfile.getTargetFat()).append("g/天\n");
        }
        if (userProfile.getDietaryRestrictions() != null && !userProfile.getDietaryRestrictions().isEmpty()) {
            profileInfo.append("- 饮食禁忌：").append(String.join(", ", userProfile.getDietaryRestrictions())).append("\n");
        }
        if (userProfile.getTastePreferences() != null && !userProfile.getTastePreferences().isEmpty()) {
            profileInfo.append("- 口味偏好：").append(String.join(", ", userProfile.getTastePreferences())).append("\n");
        }

        return profileInfo.toString();
    }

    // 构建系统提示词
    private String buildSystemPrompt(String userProfileInfo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名专业的营养师，以下是你的用户档案信息：\n\n");
        prompt.append(userProfileInfo);
        prompt.append("\n\n请根据用户的档案信息和个人需求，提供科学、实用、个性化的饮食建议。");
        prompt.append("回答要专业、详细且易于理解，充分考虑用户的年龄、性别、身高体重、活动量、健康目标等因素。");
        prompt.append("如果用户询问关于个人档案相关的建议，请结合其具体的身体数据和目标给出针对性的指导。");

        return prompt.toString();
    }

    // 通用 API 调用方法，包含超时处理和重试机制
    private GenerationResult callApiWithRetry(GenerationParam param) throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);

        while (true) {
            try {
                // 创建一个线程池来执行 API 调用
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<GenerationResult> future = executor.submit(() -> {
                    Generation gen = new Generation();
                    return gen.call(param);
                });

                try {
                    // 设置超时时间
                    return future.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    System.err.println("API 调用超时，正在重试...");
                    future.cancel(true);
                    throw new Exception("API 调用超时", e);
                } finally {
                    executor.shutdown();
                }
            } catch (Exception e) {
                if (retryCount.incrementAndGet() <= MAX_RETRY_COUNT) {
                    System.err.println("API 调用失败，第 " + retryCount.get() + " 次重试...");
                    Thread.sleep(RETRY_INTERVAL_MS * retryCount.get());
                } else {
                    System.err.println("API 调用失败，已达到最大重试次数");
                    throw e;
                }
            }
        }
    }

    // 清除指定用户的会话历史
    public void clearSessionHistory(String userId) {
        sessionHistories.remove(userId);
        System.out.println("已清除用户 " + userId + " 的会话历史");
    }

    // 获取会话历史长度（用于调试）
    public int getSessionHistoryLength(String userId) {
        List<Message> history = sessionHistories.get(userId);
        return history != null ? history.size() : 0;
    }

    public String analyzeDailyNutrition(String userId, Map<String, Object> nutritionData) {
        System.out.println("正在调用 AI分析，用户 ID: " + userId);
        System.out.println("接收到的数据：" + nutritionData);
        System.out.println("analysisType: " + nutritionData.get("analysisType"));

        String apiKey = configApiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("通义千问 API 密钥未配置，切换到本地演示模式");
            return generateMockNutritionAnalysis(userId, nutritionData);
        }

        try {
            UserProfile userProfile = userProfileRepository.findByUserIdWithDietaryRestrictions(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);

            // 判断是否为健身分析
            boolean isFitnessAnalysis = "fitness".equals(nutritionData.get("analysisType"));

            System.out.println("是否健身分析：" + isFitnessAnalysis);

            String analysisPrompt;
            String systemMessage;

            if (isFitnessAnalysis) {
                // 健身分析模式
                System.out.println("使用健身分析提示词");
                analysisPrompt = buildFitnessAnalysisPrompt(userProfileInfo, nutritionData);
                systemMessage = "你是一名专业的健身教练，专注于运动训练指导。请根据用户的身体数据和今日健身训练情况，给出专业的健身分析和建议。**注意这是健身训练分析，不是饮食营养分析**。重点评估训练强度、动作质量、训练计划等健身相关内容。";
            } else {
                // 营养分析模式（默认）
                System.out.println("使用营养分析提示词");
                analysisPrompt = buildNutritionAnalysisPrompt(userProfileInfo, nutritionData);
                systemMessage = "你是一名专业的营养师，请根据用户的身体数据和今日营养摄入情况，给出专业的分析和建议。";
            }

            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemMessage)
                    .build();

            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(analysisPrompt)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .model(MODEL_NAME)
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .apiKey(apiKey)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            // 调用API（带超时和重试机制）
            GenerationResult result = callApiWithRetry(param);
            String response = result.getOutput().getChoices().get(0).getMessage().getContent();
            System.out.println("AI分析完成，类型：" + (isFitnessAnalysis ? "健身" : "营养"));
            return response;

        } catch (Exception e) {
            System.err.println("AI分析失败：" + e.getMessage());
            e.printStackTrace();
            return generateMockNutritionAnalysis(userId, nutritionData);
        }
    }

    private String buildNutritionAnalysisPrompt(String userProfileInfo, Map<String, Object> nutritionData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下用户的今日营养摄入情况，并给出专业建议：\n\n");
        prompt.append("【用户档案】\n");
        prompt.append(userProfileInfo);
        prompt.append("\n【今日营养摄入】\n");

        if (nutritionData.containsKey("calories")) {
            prompt.append("- 热量：").append(nutritionData.get("calories")).append(" kcal\n");
        }
        if (nutritionData.containsKey("protein")) {
            prompt.append("- 蛋白质：").append(nutritionData.get("protein")).append(" g\n");
        }
        if (nutritionData.containsKey("carbs")) {
            prompt.append("- 碳水化合物：").append(nutritionData.get("carbs")).append(" g\n");
        }
        if (nutritionData.containsKey("fat")) {
            prompt.append("- 脂肪：").append(nutritionData.get("fat")).append(" g\n");
        }
        if (nutritionData.containsKey("fiber")) {
            prompt.append("- 膳食纤维：").append(nutritionData.get("fiber")).append(" g\n");
        }
        if (nutritionData.containsKey("mealCount")) {
            prompt.append("- 用餐次数：").append(nutritionData.get("mealCount")).append(" 次\n");
        }

        prompt.append("\n请从以下几个方面进行分析：\n");
        prompt.append("1. **摄入评估**：根据用户的健康目标和活动量，判断今日热量和各营养素摄入是否合理\n");
        prompt.append("2. **营养均衡**：分析蛋白质、碳水、脂肪的比例是否符合用户的健康目标\n");
        prompt.append("3. **改进建议**：针对用户的具体目标（如减脂、增肌、控糖等）给出个性化的饮食调整建议\n");
        prompt.append("4. **推荐食物**：根据用户的饮食禁忌和口味偏好，建议接下来可以补充哪些食物\n");
        prompt.append("5. **目标达成**：分析今日摄入对用户健康目标的贡献程度\n");
        prompt.append("\n请用友好、专业的语气回答，控制在 200 字以内。");

        return prompt.toString();
    }

    // 新增：健身分析提示词构建方法
    private String buildFitnessAnalysisPrompt(String userProfileInfo, Map<String, Object> fitnessData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【重要提示】这是一份健身训练分析报告，请作为专业健身教练进行分析。\n\n");
        prompt.append("请分析以下用户的今日健身训练情况，并给出专业的健身教练建议：\n\n");
        prompt.append("【用户档案】\n");
        prompt.append(userProfileInfo);
        prompt.append("\n【今日健身训练数据】\n");

        if (fitnessData.containsKey("calories")) {
            prompt.append("- 训练总消耗热量：").append(fitnessData.get("calories")).append(" kcal\n");
        }
        if (fitnessData.containsKey("trainingDuration")) {
            prompt.append("- 总训练时长：").append(fitnessData.get("trainingDuration")).append(" 分钟\n");
        }
        if (fitnessData.containsKey("mealCount")) {
            prompt.append("- 训练项目数量：").append(fitnessData.get("mealCount")).append(" 个\n");
        }
        if (fitnessData.containsKey("workoutDetails")) {
            prompt.append("- 详细训练内容：\n");
            String details = (String) fitnessData.get("workoutDetails");
            // 将逗号分隔的训练详情转换为列表格式
            String[] workouts = details.split(", ");
            for (String workout : workouts) {
                prompt.append("  • ").append(workout).append("\n");
            }
        }

        prompt.append("\n【分析要求】请从以下几个方面进行专业的健身训练分析（注意：这是健身分析，不是饮食分析）：\n");
        prompt.append("1. **训练强度评估**：根据用户的年龄、性别、体重、健康目标和总训练时长，判断本次训练强度是否合适，是否达到有效训练刺激\n");
        prompt.append("2. **训练结构分析**：分析有氧运动、力量训练、柔韧性训练等不同类型的组合是否科学合理\n");
        prompt.append("3. **动作质量建议**：针对用户进行的训练项目，给出标准动作要点和常见错误提醒\n");
        prompt.append("4. **改进建议**：针对用户的健康目标（如减脂、增肌、增强体能等）给出个性化的训练调整建议，包括训练频率、强度、时长等\n");
        prompt.append("5. **恢复建议**：根据训练强度给出训练后的恢复建议（如拉伸、休息、泡沫轴放松等）\n");
        prompt.append("6. **长期计划**：为用户制定一个循序渐进的周期性健身计划建议\n");
        prompt.append(
                "\n【重要】请用友好、专业且鼓励的语气回答，控制在 200-300 字以内。**重点放在健身训练本身**，不要偏离到纯饮食营养分析。如果提到营养，仅限于训练后的补充时机（如训练后补充蛋白质）。");

        return prompt.toString();
    }

    public String generateMockNutritionAnalysis(String userId, Map<String, Object> nutritionData) {
        // 判断是否为健身分析
        boolean isFitnessAnalysis = "fitness".equals(nutritionData.get("analysisType"));

        if (isFitnessAnalysis) {
            return generateMockFitnessAnalysis(userId, nutritionData);
        }

        // 原有的营养分析逻辑
        double calories = nutritionData.containsKey("calories") ? ((Number) nutritionData.get("calories")).doubleValue()
                : 0;
        double protein = nutritionData.containsKey("protein") ? ((Number) nutritionData.get("protein")).doubleValue()
                : 0;
        double carbs = nutritionData.containsKey("carbs") ? ((Number) nutritionData.get("carbs")).doubleValue() : 0;
        double fat = nutritionData.containsKey("fat") ? ((Number) nutritionData.get("fat")).doubleValue() : 0;

        // 获取用户档案信息
        UserProfile userProfile = userProfileRepository.findByUserId(userId);
        String healthGoal = userProfile != null && userProfile.getHealthGoal() != null ? userProfile.getHealthGoal()
                : "general_health";
        String activityLevel = userProfile != null && userProfile.getActivityLevel() != null
                ? userProfile.getActivityLevel()
                : "moderate";

        StringBuilder analysis = new StringBuilder();
        analysis.append("📊 **今日营养分析报告**\n\n");

        // 根据健康目标和活动量调整热量评估标准
        int minCalories = 1200;
        int maxCalories = 2500;

        if ("muscle_gain".equals(healthGoal)) {
            minCalories = 1800;
            maxCalories = 3000;
        } else if ("weight_loss".equals(healthGoal)) {
            minCalories = 1000;
            maxCalories = 2000;
        } else if ("very_heavy".equals(activityLevel)) {
            minCalories = 2000;
            maxCalories = 3500;
        }

        if (calories < minCalories) {
            analysis.append("⚠️ **热量摄入不足**\n");
            analysis.append("今日热量仅").append(Math.round(calories)).append("kcal");
            if ("muscle_gain".equals(healthGoal)) {
                analysis.append("，低于增肌所需。\n建议：增加优质蛋白和复合碳水的摄入，如鸡胸肉、糙米、燕麦等。\n");
            } else if ("weight_loss".equals(healthGoal)) {
                analysis.append("，但符合减脂需求。\n建议：确保营养均衡，增加蛋白质摄入以保持肌肉量。\n");
            } else {
                analysis.append("，低于基础代谢需求。\n建议：适当增加主食和优质蛋白的摄入，如全麦面包、鸡蛋、牛奶等。\n");
            }
        } else if (calories > maxCalories) {
            analysis.append("⚠️ **热量摄入偏高**\n");
            analysis.append("今日热量已达").append(Math.round(calories)).append("kcal");
            if ("weight_loss".equals(healthGoal)) {
                analysis.append("，超过减脂所需。\n建议：减少高热量食物，增加蔬菜水果，控制晚餐份量。\n");
            } else if ("muscle_gain".equals(healthGoal)) {
                analysis.append("，符合增肌需求。\n建议：确保热量来自优质食物，增加蛋白质比例。\n");
            } else {
                analysis.append("。\n建议：减少高热量食物，增加蔬菜水果，控制晚餐份量。\n");
            }
        } else {
            analysis.append("✅ **热量摄入合理**\n");
            analysis.append("今日热量").append(Math.round(calories)).append("kcal");
            if ("weight_loss".equals(healthGoal)) {
                analysis.append("，符合减脂需求。\n");
            } else if ("muscle_gain".equals(healthGoal)) {
                analysis.append("，符合增肌需求。\n");
            } else {
                analysis.append("，处于健康范围内。\n");
            }
        }

        double proteinRatio = calories > 0 ? (protein * 4 / calories) * 100 : 0;
        if (proteinRatio < 10) {
            analysis.append("\n⚠️ 蛋白质摄入偏低");
            if ("muscle_gain".equals(healthGoal)) {
                analysis.append("，建议增加瘦肉、鱼类、蛋白粉等高蛋白食物。");
            } else {
                analysis.append("，建议增加瘦肉、鱼类、豆制品。");
            }
        } else if (proteinRatio > 30) {
            analysis.append("\n✅ 蛋白质摄入充足");
            if ("muscle_gain".equals(healthGoal)) {
                analysis.append("，非常适合增肌，继续保持！");
            } else {
                analysis.append("，继续保持！");
            }
        }

        // 根据健康目标给出特定建议
        if ("blood_sugar_control".equals(healthGoal)) {
            analysis.append("\n\n💡 **控糖建议**：选择低升糖指数食物，如燕麦、藜麦、绿叶蔬菜，避免精制糖和高糖食品。");
        } else if ("muscle_gain".equals(healthGoal)) {
            analysis.append("\n\n💡 **增肌建议**：训练后30分钟内补充蛋白质和碳水化合物，保证每日蛋白质摄入量达到体重(kg)×1.6-2.2g。");
        } else if ("weight_loss".equals(healthGoal)) {
            analysis.append("\n\n💡 **减脂建议**：采用少食多餐的方式，增加膳食纤维摄入，选择低热量高营养的食物。");
        } else {
            analysis.append("\n\n💡 **建议**：多喝水，保持规律作息，适量运动。");
        }

        return analysis.toString();
    }

    // 新增：健身分析模拟响应方法（本地演示模式）
    private String generateMockFitnessAnalysis(String userId, Map<String, Object> fitnessData) {
        System.out.println("=== 调用健身分析 Mock 模式 ===");
        System.out.println("健身数据：" + fitnessData);

        double caloriesBurned = fitnessData.containsKey("calories")
                ? ((Number) fitnessData.get("calories")).doubleValue()
                : 0;
        int trainingDuration = fitnessData.containsKey("trainingDuration")
                ? ((Number) fitnessData.get("trainingDuration")).intValue()
                : 0;
        int workoutCount = fitnessData.containsKey("mealCount") ? ((Number) fitnessData.get("mealCount")).intValue()
                : 0;

        // 获取用户档案信息
        UserProfile userProfile = userProfileRepository.findByUserId(userId);
        String healthGoal = userProfile != null && userProfile.getHealthGoal() != null ? userProfile.getHealthGoal()
                : "general_health";
        String activityLevel = userProfile != null && userProfile.getActivityLevel() != null
                ? userProfile.getActivityLevel()
                : "moderate";

        StringBuilder analysis = new StringBuilder();
        analysis.append("🏋️ **今日健身训练分析报告**\n\n");

        // 训练强度评估
        analysis.append("💪 **训练强度评估**\n");
        if (trainingDuration < 30) {
            analysis.append("今日训练时长较短（").append(trainingDuration).append("分钟），强度偏低。");
            if ("weight_loss".equals(healthGoal)) {
                analysis.append("建议延长有氧运动时间至 45 分钟以上，心率达到最大心率的 60-70%，以达到更好的燃脂效果。\n");
            } else if ("muscle_gain".equals(healthGoal)) {
                analysis.append("增肌期建议增加力量训练组数和重量，确保肌肉得到充分刺激，每组做到力竭。\n");
            } else {
                analysis.append("建议适当增加训练时长，提升心肺功能和肌肉耐力。\n");
            }
        } else if (trainingDuration > 90) {
            analysis.append("今日训练时长较长（").append(trainingDuration).append("分钟），强度偏高。");
            if ("weight_loss".equals(healthGoal)) {
                analysis.append("但符合减脂需求，注意补充水分和电解质，避免脱水。\n");
            } else if ("muscle_gain".equals(healthGoal)) {
                analysis.append("注意避免过度训练，确保肌肉恢复时间，防止肌肉分解。\n");
            } else {
                analysis.append("注意合理安排休息，避免过度疲劳和运动损伤。\n");
            }
        } else {
            analysis.append("✅ 今日训练时长适中（").append(trainingDuration).append("分钟），强度合理，达到了有效训练刺激。\n");
        }

        // 热量消耗分析
        analysis.append("\n🔥 **热量消耗分析**\n");
        analysis.append("今日训练共消耗 ").append(Math.round(caloriesBurned)).append(" kcal");
        if (caloriesBurned < 200) {
            analysis.append("，消耗较少。建议增加训练强度、延长训练时间或采用 HIIT 间歇训练提高燃脂效率。\n");
        } else if (caloriesBurned > 600) {
            analysis.append("，消耗优秀！继续保持这样的训练强度，非常适合减脂和心肺功能提升。\n");
        } else {
            analysis.append("，处于正常范围，继续保持！\n");
        }

        // 训练结构分析
        analysis.append("\n📊 **训练结构分析**\n");
        analysis.append("今日完成 ").append(workoutCount).append(" 个训练项目");
        if (workoutCount < 3) {
            analysis.append("，项目较少。建议增加不同类型的训练，如热身 + 有氧 + 力量 + 拉伸的组合，全面锻炼身体。\n");
        } else if (workoutCount > 6) {
            analysis.append("，训练内容丰富，注意避免过度疲劳，确保每个动作的质量而非数量。\n");
        } else {
            analysis.append("，训练安排合理，涵盖了多个肌群和运动类型。\n");
        }

        // 根据健康目标给出建议
        analysis.append("\n💡 **个性化训练建议**\n");
        if ("weight_loss".equals(healthGoal)) {
            analysis.append("• 减脂期建议保持每周 3-5 次有氧运动（跑步、游泳、骑行），每次 45-60 分钟\n");
            analysis.append("• 配合 2-3 次力量训练提高基础代谢，优先大肌群训练（深蹲、硬拉、卧推）\n");
            analysis.append("• 训练后补充蛋白质（20-30g）和适量碳水，帮助肌肉恢复\n");
            analysis.append("• 保持充足睡眠，促进脂肪代谢和身体恢复");
        } else if ("muscle_gain".equals(healthGoal)) {
            analysis.append("• 增肌期重点进行复合动作训练（深蹲、硬拉、卧推、引体向上、划船）\n");
            analysis.append("• 每组 8-12 次力竭，3-4 组为宜，组间休息 60-90 秒\n");
            analysis.append("• 训练后 30 分钟内补充蛋白质（30-40g）和快速吸收的碳水（香蕉、白面包）\n");
            analysis.append("• 确保同一肌群训练间隔 48-72 小时，给肌肉充分恢复时间");
        } else if ("endurance".equals(healthGoal)) {
            analysis.append("• 增强体能建议循序渐进增加训练强度和时长\n");
            analysis.append("• 结合 HIIT 间歇训练提升心肺功能和无氧耐力\n");
            analysis.append("• 注意训练后的拉伸和恢复，预防运动损伤\n");
            analysis.append("• 保持规律作息，提升整体体能水平");
        } else {
            analysis.append("• 保持规律运动，每周至少 150 分钟中等强度有氧运动\n");
            analysis.append("• 结合力量训练维持肌肉量和骨密度\n");
            analysis.append("• 注意热身和拉伸，预防运动损伤\n");
            analysis.append("• 循序渐进增加训练难度，保持持续进步");
        }

        // 恢复建议
        analysis.append("\n\n🧘 **恢复建议**\n");
        analysis.append("• 训练后进行 10-15 分钟静态拉伸，重点拉伸训练过的肌群\n");
        analysis.append("• 补充足够水分（每日 2-3 升）和电解质\n");
        analysis.append("• 保证 7-8 小时优质睡眠，促进生长激素分泌\n");
        if (trainingDuration > 60 || caloriesBurned > 500) {
            analysis.append("• 高强度/长时间训练后建议休息 24-48 小时再训练同一肌群\n");
            analysis.append("• 可进行泡沫轴放松、按摩等方式加速恢复");
        }

        return analysis.toString();
    }

    // 新增：健身收获分析 - 专注于分析用户的健身训练收获和热量消耗
    public String analyzeFitnessWorkout(String userId, Map<String, Object> workoutData) {
        System.out.println("正在调用 AI 分析健身收获，用户 ID: " + userId);
        System.out.println("健身数据：" + workoutData);

        String apiKey = configApiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("通义千问 API 密钥未配置，切换到本地演示模式");
            return generateMockFitnessWorkoutAnalysis(userId, workoutData);
        }

        try {
            UserProfile userProfile = userProfileRepository.findByUserIdWithDietaryRestrictions(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);

            // 构建健身收获分析提示词
            String analysisPrompt = buildFitnessWorkoutPrompt(userProfileInfo, workoutData);

            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一名专业的健身教练，请根据用户的身体数据和今日健身训练情况，重点分析健身收获（训练效果）和热量消耗，给出专业的评估和建议。**注意这是健身训练分析，不是饮食营养分析**。")
                    .build();

            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(analysisPrompt)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .model(MODEL_NAME)
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .apiKey(apiKey)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            // 调用API（带超时和重试机制）
            GenerationResult result = callApiWithRetry(param);
            String response = result.getOutput().getChoices().get(0).getMessage().getContent();
            System.out.println("AI 健身收获分析完成");
            return response;

        } catch (Exception e) {
            System.err.println("AI 健身收获分析失败：" + e.getMessage());
            e.printStackTrace();
            return generateMockFitnessWorkoutAnalysis(userId, workoutData);
        }
    }

    // 构建健身收获分析的提示词
    private String buildFitnessWorkoutPrompt(String userProfileInfo, Map<String, Object> workoutData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【重要】这是一份健身训练收获分析报告，请作为专业健身教练进行分析。\n\n");
        prompt.append("请分析以下用户的今日健身训练情况，**重点分析健身收获和热量消耗**：\n\n");
        prompt.append("【用户档案】\n");
        prompt.append(userProfileInfo);
        prompt.append("\n【今日健身训练数据】\n");

        if (workoutData.containsKey("totalCalories")) {
            prompt.append("- 总消耗热量：").append(workoutData.get("totalCalories")).append(" kcal\n");
        }
        if (workoutData.containsKey("totalDuration")) {
            prompt.append("- 总训练时长：").append(workoutData.get("totalDuration")).append(" 分钟\n");
        }
        if (workoutData.containsKey("workoutCount")) {
            prompt.append("- 训练项目数量：").append(workoutData.get("workoutCount")).append(" 个\n");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workouts = (List<Map<String, Object>>) workoutData.get("workouts");
        if (workouts != null && !workouts.isEmpty()) {
            prompt.append("- 详细训练内容：\n");
            for (Map<String, Object> workout : workouts) {
                String name = (String) workout.get("name");
                String type = (String) workout.get("type");
                Integer duration = (Integer) workout.get("duration");
                Integer calories = (Integer) workout.get("calories");
                prompt.append("  • ").append(name)
                        .append(" (").append(type).append(")")
                        .append(": ").append(duration).append("分钟")
                        .append(", 消耗 ").append(calories).append(" kcal\n");
            }
        }

        prompt.append("\n【分析要求】请从以下几个方面进行专业的健身训练分析（**重点聚焦健身收获和消耗**）：\n");
        prompt.append("1. **训练收获评估**：评估本次训练的整体效果，包括心肺功能提升、肌肉刺激、柔韧性改善等\n");
        prompt.append("2. **热量消耗分析**：根据总消耗热量和各项目的消耗，分析燃脂效率和能量代谢情况\n");
        prompt.append("3. **训练结构评价**：分析有氧运动、力量训练等不同类型训练的配比是否科学合理\n");
        prompt.append("4. **进步空间建议**：针对用户的健康目标，指出可以优化的训练项目和方式\n");
        prompt.append("5. **恢复与持续**：给出训练后的恢复建议，以及如何保持持续的健身收获\n");
        prompt.append("\n【重要】请用友好、专业且鼓励的语气回答，控制在 200-300 字以内。**重点放在健身训练收获和热量消耗上**，不要偏离到纯饮食营养分析。");

        return prompt.toString();
    }

    // 生成健身收获的模拟分析（本地演示模式）
    private String generateMockFitnessWorkoutAnalysis(String userId, Map<String, Object> workoutData) {
        Integer totalCalories = workoutData.containsKey("totalCalories") ? (Integer) workoutData.get("totalCalories")
                : 0;
        Integer totalDuration = workoutData.containsKey("totalDuration") ? (Integer) workoutData.get("totalDuration")
                : 0;
        Integer workoutCount = workoutData.containsKey("workoutCount") ? (Integer) workoutData.get("workoutCount") : 0;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> workouts = (List<Map<String, Object>>) workoutData.get("workouts");

        // 获取用户档案信息
        UserProfile userProfile = userProfileRepository.findByUserId(userId);
        String healthGoal = userProfile != null && userProfile.getHealthGoal() != null ? userProfile.getHealthGoal()
                : "general_health";
        Integer age = userProfile != null && userProfile.getAge() != null ? userProfile.getAge() : 30;
        Double weight = userProfile != null && userProfile.getWeight() != null ? userProfile.getWeight() : 70.0;

        StringBuilder analysis = new StringBuilder();
        analysis.append("🏋️ **今日健身训练收获分析**\n\n");

        // 训练收获评估
        analysis.append("💪 **训练收获评估**\n");
        analysis.append("您今日完成了 ").append(workoutCount).append(" 个训练项目，总时长 ").append(totalDuration).append(" 分钟。\n");
        if (totalDuration >= 60) {
            analysis.append("✅ 训练时长充足，达到了理想的训练效果，对心肺功能、肌肉耐力和代谢都有显著提升。\n");
        } else if (totalDuration >= 30) {
            analysis.append("✅ 达到了有效训练时长，对心肺功能和肌肉耐力都有良好刺激。\n");
        } else {
            analysis.append("⚠️ 训练时长较短，建议延长至 30 分钟以上以获得更好效果。\n");
        }

        // 热量消耗分析
        analysis.append("\n🔥 **热量消耗分析**\n");
        analysis.append("总消耗热量：").append(totalCalories).append(" kcal\n");

        // 计算热量消耗效率
        double caloriesPerMinute = totalDuration > 0 ? (double) totalCalories / totalDuration : 0;
        analysis.append("热量消耗效率：").append(String.format("%.1f", caloriesPerMinute)).append(" kcal/分钟\n");

        if (totalCalories >= 500) {
            analysis.append("✅ 燃脂效果优秀！达到了深度燃脂的热量消耗水平。\n");
        } else if (totalCalories >= 300) {
            analysis.append("✅ 燃脂效果良好，能够有效消耗体内多余脂肪。\n");
        } else if (totalCalories >= 150) {
            analysis.append("⚠️ 燃脂效果一般，建议增加训练强度或时长以提升燃脂效率。\n");
        } else {
            analysis.append("⚠️ 热量消耗较低，建议增加训练强度或时长，以达到更好的燃脂效果。\n");
        }

        // 训练结构评价
        analysis.append("\n📊 **训练结构评价**\n");
        if (workouts != null && !workouts.isEmpty()) {
            boolean hasCardio = workouts.stream().anyMatch(w -> "cardio".equals(w.get("type")));
            boolean hasStrength = workouts.stream().anyMatch(w -> "strength".equals(w.get("type")));
            boolean hasFlexibility = workouts.stream().anyMatch(w -> "flexibility".equals(w.get("type")));

            if (hasCardio && hasStrength && hasFlexibility) {
                analysis.append("✅ 训练结构非常合理，有氧 + 力量 + 柔韧性训练的组合，全面发展身体素质。\n");
            } else if (hasCardio && hasStrength) {
                analysis.append("✅ 训练结构合理，有氧 + 力量组合，全面发展身体素质。建议适当增加柔韧性训练以提高关节活动度。\n");
            } else if (hasCardio) {
                analysis.append("⚠️ 仅进行了有氧运动，建议增加力量训练以提高基础代谢和肌肉量。\n");
            } else if (hasStrength) {
                analysis.append("⚠️ 仅进行了力量训练，建议增加有氧运动提升心肺功能和燃脂效果。\n");
            } else if (hasFlexibility) {
                analysis.append("⚠️ 仅进行了柔韧性训练，建议增加有氧和力量训练以全面提升身体素质。\n");
            } else {
                analysis.append("⚠️ 训练类型单一，建议多样化训练类型，全面提升身体素质。\n");
            }

            // 详细分析每种训练类型
            if (hasCardio) {
                analysis.append("• 有氧训练：有助于提高心肺功能、燃烧脂肪、改善心血管健康。\n");
            }
            if (hasStrength) {
                analysis.append("• 力量训练：有助于增加肌肉量、提高基础代谢、增强骨骼健康。\n");
            }
            if (hasFlexibility) {
                analysis.append("• 柔韧性训练：有助于提高关节活动度、减少运动损伤风险、改善姿势。\n");
            }
        } else {
            analysis.append("⚠️ 未检测到详细的训练数据，建议记录具体的训练内容以获得更准确的分析。\n");
        }

        // 进步空间建议
        analysis.append("\n💡 **个性化进步建议**\n");
        if ("weight_loss".equals(healthGoal)) {
            analysis.append("• 减脂期可增加 HIIT 间歇训练（如30秒冲刺+60秒步行，重复10-15组），提升燃脂效率\n");
            analysis.append("• 保持心率在最大心率的 60-70% 区间（约 ").append((220 - age) * 0.6).append("-").append((220 - age) * 0.7)
                    .append(" 次/分钟），燃脂效果最佳\n");
            analysis.append("• 增加训练频率，每周 4-5 次，每次 45-60 分钟\n");
            analysis.append("• 结合饮食控制，创造热量缺口，加速减脂效果\n");
        } else if ("muscle_gain".equals(healthGoal)) {
            analysis.append("• 增肌期应注重复合动作（深蹲、硬拉、卧推、引体向上），这些动作能刺激多个肌群\n");
            analysis.append("• 每组做到力竭，确保肌肉得到充分刺激，建议每组 8-12 次\n");
            analysis.append("• 增加训练强度，逐渐提高重量，遵循渐进超负荷原则\n");
            analysis.append("• 保证充足的蛋白质摄入（每日 " + weight * 1.6 + "-" + weight * 2.2 + " 克），支持肌肉修复和生长\n");
        } else {
            analysis.append("• 保持规律运动，每周至少 150 分钟中等强度有氧运动\n");
            analysis.append("• 结合力量训练，每周 2-3 次，维持肌肉量和骨密度\n");
            analysis.append("• 加入柔韧性训练，提高关节活动度和身体协调性\n");
            analysis.append("• 多样化训练方式，避免单调，提高运动依从性\n");
        }

        // 恢复与持续
        analysis.append("\n\n🧘 **恢复与营养建议**\n");
        analysis.append("• 训练后进行 10-15 分钟静态拉伸，重点拉伸训练过的肌群，促进肌肉恢复\n");
        analysis.append("• 补充足够水分（训练后 30 分钟内补充 500-1000ml），帮助代谢废物排出\n");
        analysis.append("• 训练后 30 分钟内补充蛋白质（20-40g）和碳水化合物，促进肌肉修复\n");
        analysis.append("• 保证 7-9 小时优质睡眠，促进生长激素分泌，加速身体恢复\n");
        analysis.append("• 循序渐进增加训练强度，每周可增加 5-10% 的重量或时长，保持持续的健身收获\n");
        analysis.append("• 定期休息，避免过度训练，给身体足够的恢复时间\n");

        // 长期健身建议
        analysis.append("\n🎯 **长期健身建议**\n");
        analysis.append("• 设定明确的健身目标，定期评估和调整\n");
        analysis.append("• 保持训练日志，记录每次训练的内容、重量、组数和感受\n");
        analysis.append("• 寻找健身伙伴或加入健身社群，提高运动依从性\n");
        analysis.append("• 保持积极的心态，健身是一个长期过程，坚持才是关键\n");

        return analysis.toString();
    }

    private String generateMockResponse(String userMessage) {
        System.out.println("使用模拟响应");
        // 当API调用失败时，返回模拟的营养建议，模拟真实AI响应的风格和内容
        if (userMessage.toLowerCase().contains("减脂") || userMessage.contains("减肥") || userMessage.contains("瘦身")) {
            return "您好！关于减脂，我有以下建议：\n\n" +
                    "**饮食原则**：\n" +
                    "• 采用少食多餐的方式，每餐控制在300-400卡路里\n" +
                    "• 增加蛋白质摄入（每日1.2-1.6g/kg体重）以保持肌肉量\n" +
                    "• 控制碳水化合物摄入，选择低GI值的全谷物\n" +
                    "• 增加膳食纤维摄入，促进肠道健康\n\n" +
                    "**推荐食物**：\n" +
                    "• 高蛋白低脂：鸡胸肉、鱼类、蛋类、豆腐、希腊酸奶\n" +
                    "• 蔬菜：菠菜、西兰花、芦笋、甜椒、黄瓜\n" +
                    "• 水果：蓝莓、草莓、苹果、梨（适量）\n\n" +
                    "**饮食禁忌**：\n" +
                    "• 避免高糖食物：蛋糕、糖果、甜饮料\n" +
                    "• 减少油炸食品和加工食品\n" +
                    "• 控制酒精摄入\n\n" +
                    "**生活建议**：\n" +
                    "• 结合适量有氧运动，每周至少150分钟\n" +
                    "• 保证充足睡眠（7-8小时/天）\n" +
                    "• 保持水分摄入，每天至少2000ml\n" +
                    "• 记录饮食和运动情况，及时调整计划";
        } else if (userMessage.toLowerCase().contains("增肌") || userMessage.contains("健身")
                || userMessage.contains("肌肉")) {
            return "您好！关于增肌，我有以下专业建议：\n\n" +
                    "**营养需求**：\n" +
                    "• 蛋白质：每日摄入量为体重(kg)×1.6-2.2g，是肌肉修复和生长的基础\n" +
                    "• 碳水化合物：提供训练能量，占总热量的50-60%\n" +
                    "• 脂肪：健康脂肪对激素分泌至关重要，占总热量的20-30%\n\n" +
                    "**进餐时机**：\n" +
                    "• 训练后30分钟内：补充蛋白质(20-40g)和快速吸收的碳水\n" +
                    "• 睡前：缓释蛋白质（如酪蛋白）促进夜间肌肉修复\n\n" +
                    "**推荐食物**：\n" +
                    "• 优质蛋白：鸡胸肉、鱼肉、牛肉、鸡蛋、希腊酸奶、蛋白粉\n" +
                    "• 碳水来源：糙米、燕麦、红薯、藜麦、全麦面包\n" +
                    "• 健康脂肪：坚果、牛油果、橄榄油、鱼油\n\n" +
                    "**训练建议**：\n" +
                    "• 复合动作为主：深蹲、硬拉、卧推、引体向上\n" +
                    "• 每组8-12次力竭，3-4组\n" +
                    "• 同一肌群训练间隔48-72小时\n\n" +
                    "**恢复**：\n" +
                    "• 保证7-9小时睡眠\n" +
                    "• 适当补充肌酸、BCAA等补剂\n" +
                    "• 定期调整训练计划，避免适应";
        } else if (userMessage.toLowerCase().contains("控糖") || userMessage.contains("糖尿病")
                || userMessage.contains("血糖")) {
            return "您好！关于控糖，我有以下专业建议：\n\n" +
                    "**饮食原则**：\n" +
                    "• 选择低升糖指数(GI)食物，避免血糖剧烈波动\n" +
                    "• 定时定量进餐，避免暴饮暴食\n" +
                    "• 控制总热量摄入，维持健康体重\n" +
                    "• 增加膳食纤维摄入，延缓碳水吸收\n\n" +
                    "**推荐食物**：\n" +
                    "• 全谷物：燕麦、藜麦、糙米、全麦面包\n" +
                    "• 蔬菜：菠菜、西兰花、青椒、黄瓜、西红柿\n" +
                    "• 蛋白质：鱼类、鸡胸肉、豆腐、鸡蛋\n" +
                    "• 低糖水果：蓝莓、草莓、苹果、梨（适量）\n\n" +
                    "**饮食禁忌**：\n" +
                    "• 避免精制糖：糖果、蛋糕、甜饮料\n" +
                    "• 减少白米、白面等精制碳水\n" +
                    "• 控制油炸食品和加工食品\n" +
                    "• 限制酒精摄入，尤其是甜酒\n\n" +
                    "**生活建议**：\n" +
                    "• 定期监测血糖，了解食物对血糖的影响\n" +
                    "• 结合适量运动，提高胰岛素敏感性\n" +
                    "• 保持充足睡眠，减少压力\n" +
                    "• 咨询医生或营养师，制定个性化控糖方案";
        } else if (userMessage.toLowerCase().contains("养胃") || userMessage.contains("胃病")
                || userMessage.contains("消化")) {
            return "您好！关于养胃，我有以下专业建议：\n\n" +
                    "**饮食原则**：\n" +
                    "• 定时定量进餐，避免过饥过饱\n" +
                    "• 细嚼慢咽，减轻肠胃负担\n" +
                    "• 避免刺激性食物，保护胃黏膜\n" +
                    "• 选择易消化、营养丰富的食物\n\n" +
                    "**推荐食物**：\n" +
                    "• 温和主食：小米粥、软米饭、馒头、面条\n" +
                    "• 蛋白质：蒸蛋、豆腐、鱼肉、鸡肉（去皮）\n" +
                    "• 蔬菜：南瓜、胡萝卜、土豆（煮熟）、西兰花\n" +
                    "• 水果：苹果（蒸熟）、香蕉、木瓜（适量）\n\n" +
                    "**饮食禁忌**：\n" +
                    "• 避免辛辣刺激：辣椒、芥末、咖喱\n" +
                    "• 减少油腻食物：油炸食品、肥肉\n" +
                    "• 避免过冷过热：冰镇饮料、滚烫食物\n" +
                    "• 限制咖啡、浓茶、酒精\n\n" +
                    "**生活建议**：\n" +
                    "• 保持规律作息，避免熬夜\n" +
                    "• 减少压力，保持心情舒畅\n" +
                    "• 避免饭后立即剧烈运动\n" +
                    "• 如有严重症状，及时就医";
        } else if (userMessage.toLowerCase().contains("营养") || userMessage.contains("饮食")
                || userMessage.contains("健康")) {
            return "您好！关于健康饮食，我有以下专业建议：\n\n" +
                    "**均衡饮食原则**：\n" +
                    "• 每天摄入12种以上食物，每周25种以上\n" +
                    "• 谷薯类：占总热量的50-60%，选择全谷物\n" +
                    "• 蔬菜水果：每天500g以上，种类多样化\n" +
                    "• 蛋白质：占总热量的15-20%，选择优质蛋白\n" +
                    "• 脂肪：占总热量的20-30%，选择健康脂肪\n\n" +
                    "**每日饮食建议**：\n" +
                    "• 早餐：全麦面包/燕麦 + 鸡蛋 + 牛奶/豆浆 + 水果\n" +
                    "• 午餐：米饭/面条 + 瘦肉 + 蔬菜\n" +
                    "• 晚餐：杂粮粥 + 鱼类/豆腐 + 蔬菜\n" +
                    "• 加餐：坚果、酸奶、水果（适量）\n\n" +
                    "**饮水建议**：\n" +
                    "• 每天饮水1500-2000ml\n" +
                    "• 优先选择白开水，避免甜饮料\n" +
                    "• 运动前后适当补水\n\n" +
                    "**生活建议**：\n" +
                    "• 结合适量运动，每周至少150分钟\n" +
                    "• 保证充足睡眠，每天7-8小时\n" +
                    "• 减少加工食品和外卖\n" +
                    "• 定期体检，了解身体状况";
        } else {
            return "您好！作为您的健康助手，我可以为您提供以下方面的专业建议：\n\n" +
                    "• **营养咨询**：减脂、增肌、控糖、养胃等饮食建议\n" +
                    "• **健康管理**：日常饮食搭配、营养均衡指导\n" +
                    "• **食谱推荐**：根据您的健康目标和口味偏好推荐食谱\n" +
                    "• **运动建议**：结合饮食的运动方案\n\n" +
                    "请告诉我您的具体健康需求，我会为您提供个性化的专业建议。";
        }
    }

    // 新增：心理健康咨询方法
    public String getMentalHealthAdvice(String userId, String userMessage) {
        System.out.println("正在调用AI服务进行心理健康咨询，用户ID: " + userId + ", 用户消息: " + userMessage);

        // 检查API密钥是否配置，优先读取配置文件，其次读取环境变量
        String apiKey = configApiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("==============================================");
            System.out.println("通义千问API密钥未配置，切换到本地演示模式");
            System.out.println("提示：如需使用真实AI服务，请在application.properties中设置qwen.api.key");
            System.out.println("==============================================");
            return generateMockMentalHealthResponse(userMessage);
        }

        try {
            Generation gen = new Generation();

            // 获取用户档案信息
            UserProfile userProfile = userProfileRepository.findByUserIdWithDietaryRestrictions(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);

            // 获取或创建该用户的会话历史
            List<Message> history = sessionHistories.computeIfAbsent(userId, k -> new ArrayList<>());

            // 添加系统角色设定（只在第一次对话时添加）
            if (history.isEmpty()) {
                String systemPrompt = buildMentalHealthSystemPrompt(userProfileInfo);
                Message systemMsg = Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(systemPrompt)
                        .build();
                history.add(systemMsg);
            }

            // 添加用户当前消息
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
                    .build();
            history.add(userMsg);

            // 如果历史太长，保留最近的几条消息
            if (history.size() > MAX_HISTORY_LENGTH) {
                // 保留系统消息和最近的用户和助手消息
                List<Message> trimmedHistory = new ArrayList<>();
                // 保留系统消息
                if (!history.isEmpty() && Role.SYSTEM.getValue().equals(history.get(0).getRole())) {
                    trimmedHistory.add(history.get(0));
                }
                // 保留最近的用户和助手消息
                int startIndex = Math.max(1, history.size() - (MAX_HISTORY_LENGTH - 1));
                trimmedHistory.addAll(history.subList(startIndex, history.size()));
                history.clear();
                history.addAll(trimmedHistory);
            }

            // 构建参数
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey) // 使用配置文件或环境变量中的API密钥
                    .model(MODEL_NAME)
                    .messages(history)
                    .resultFormat("message")
                    .temperature(0.7f) // 设置随机性，使用float类型
                    .topP(0.8) // 设置采样参数，使用float类型
                    .build();

            // 调用API
            GenerationResult result = gen.call(param);

            // 提取回复内容
            String response = result.getOutput().getChoices().get(0).getMessage().getContent();

            // 添加助手回复到历史记录
            Message assistantMsg = Message.builder()
                    .role(Role.ASSISTANT.getValue())
                    .content(response)
                    .build();
            history.add(assistantMsg);

            System.out.println("AI返回内容: " + response);
            return response;

        } catch (NoApiKeyException e) {
            System.err.println("API密钥缺失: " + e.getMessage());
            return generateMockMentalHealthResponse(userMessage);
        } catch (ApiException | InputRequiredException e) {
            System.err.println("API调用异常: " + e.getMessage());
            e.printStackTrace();
            return "服务暂时不可用：" + e.getMessage();
        } catch (Exception e) {
            System.err.println("未知异常: " + e.getMessage());
            e.printStackTrace();
            return "服务暂时不可用：" + e.getMessage();
        }
    }

    // 构建心理健康系统提示词
    private String buildMentalHealthSystemPrompt(String userProfileInfo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名专业的心理咨询师，以下是你的用户档案信息：\n\n");

        prompt.append(userProfileInfo);
        prompt.append("\n\n请根据用户的档案信息和个人需求，提供专业、温暖、支持性的心理健康建议。");
        prompt.append("回答要专业、详细且易于理解，充分考虑用户的年龄、性别、健康目标等因素。");
        prompt.append("如果用户询问关于情绪、压力、焦虑、抑郁等心理健康问题，请提供科学的应对策略和建议。");
        prompt.append("请保持同理心，避免评判性语言，鼓励用户表达自己的感受。");

        return prompt.toString();
    }

    // 生成心理健康模拟响应
    private String generateMockMentalHealthResponse(String userMessage) {
        System.out.println("使用心理健康模拟响应");
        // 当API调用失败时，返回模拟的心理健康建议
        if (userMessage.toLowerCase().contains("压力") || userMessage.contains("紧张") || userMessage.contains("焦虑")) {
            return "面对压力和焦虑，建议你尝试深呼吸练习：找一个安静的地方，缓慢吸气4秒，屏住呼吸4秒，然后缓慢呼气6秒，重复10-15次。同时，保持规律的作息和运动，适当的身体活动可以释放内啡肽，改善心情。如果症状持续，建议寻求专业心理咨询师的帮助。";
        } else if (userMessage.toLowerCase().contains("抑郁") || userMessage.contains("情绪低落")
                || userMessage.contains("不开心")) {
            return "情绪低落是很常见的，建议你尝试一些小的积极行动，比如每天进行30分钟的户外活动，与朋友或家人交流，或者做一些你曾经喜欢的事情。保持规律的作息和健康的饮食也很重要。如果情绪持续低落超过两周，建议寻求专业心理健康服务。";
        } else if (userMessage.toLowerCase().contains("失眠") || userMessage.contains("睡眠")
                || userMessage.contains("熬夜")) {
            return "改善睡眠质量的建议：保持规律的睡眠时间表，睡前避免使用电子设备，创造一个安静、黑暗、凉爽的睡眠环境。睡前可以尝试放松练习，如渐进式肌肉放松或冥想。避免睡前摄入咖啡因和大量食物，适量的白天运动也有助于改善睡眠质量。";
        } else if (userMessage.toLowerCase().contains("人际关系") || userMessage.contains("朋友")
                || userMessage.contains("家人")) {
            return "良好的人际关系对心理健康至关重要。建议你定期与朋友和家人保持联系，表达自己的感受和需求，同时也要倾听他人的想法。建立健康的边界，学会说'不'，避免过度承诺。如果关系中存在冲突，尝试通过开放、诚实的沟通来解决问题。";
        } else {
            return "心理健康是整体健康的重要组成部分。建议你保持规律的作息，进行适量的运动，保持社交联系，培养兴趣爱好，以及学会管理压力。如果你有具体的心理健康问题，欢迎随时向我咨询，我会尽力为你提供支持和建议。";
        }
    }

    /**
     * 构建食谱图片生成提示词
     */
    private String buildImagePrompt(String recipeName, String description, List<String> ingredients) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("一张写实风格的美食摄影作品，高清，专业拍摄。");
        prompt.append("菜肴名称：").append(recipeName).append("。");
        prompt.append(description).append("。");
        prompt.append("主要食材：").append(String.join(",", ingredients)).append("。");
        prompt.append("画面要求：精美的摆盘，柔和的自然光，浅景深，食物特写，诱人的色泽，高分辨率，细节清晰。");
        return prompt.toString();
    }

    /**
     * 使用通义万相生成食谱图片（同步调用 + 详细调试）
     */
    private String generateRecipeImage(String prompt, String apiKey) {
        try {
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            
            // 使用更快的配置 - 使用合法尺寸
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(apiKey)
                    .model("wanx-v1")
                    .prompt(prompt)
                    .n(1)  // 生成 1 张图片
                    .size("1024*1024")  // 使用支持的尺寸（虽然大一点但能成功）
                    .build();

            System.out.println("🎨 正在生成图片：" + prompt.substring(0, Math.min(30, prompt.length())) + "...");
            System.out.println("📋 使用的模型：wanx-v1");
            System.out.println("📋 图片尺寸：512*512");
            
            // 直接同步调用
            ImageSynthesisResult result = imageSynthesis.call(param);
            
            if (result == null) {
                System.err.println("⚠️ 图片生成结果为 null");
                return null;
            }
            
            System.out.println("✅ API 调用成功，开始解析结果...");
            
            // 尝试获取输出对象
            var output = result.getOutput();
            if (output == null) {
                System.err.println("⚠️ API 输出对象为 null");
                return null;
            }
            
            // 尝试获取 results 字段
            var results = output.getResults();
            if (results == null) {
                System.err.println("⚠️ API 结果列表为 null");
                System.err.println("🔍 完整输出对象：" + output.toString());
                return null;
            }
            
            if (results.isEmpty()) {
                System.err.println("⚠️ API 结果列表为空数组");
                System.err.println("🔍 完整输出对象：" + output.toString());
                return null;
            }
            
            // 从 Map 中获取 URL
            Object firstResult = results.get(0);
            if (!(firstResult instanceof Map)) {
                System.err.println("⚠️ 结果不是 Map 类型：" + firstResult.getClass().getName());
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) firstResult;
            
            if (resultMap == null) {
                System.err.println("⚠️ 第一个结果对象为 null");
                return null;
            }
            
            System.out.println("🔍 结果数据类型：" + resultMap.getClass().getName());
            System.out.println("🔍 结果包含的键：" + resultMap.keySet());
            
            // 尝试多种方式获取 URL
            String imageUrl = null;
            
            // 方式 1: 直接获取 url 字段（小写）
            if (resultMap.containsKey("url")) {
                imageUrl = String.valueOf(resultMap.get("url"));
                System.out.println("🔍 从 'url' 字段获取到：" + imageUrl);
            }
            // 方式 2: 尝试 output 字段（某些 API 返回格式）
            else if (resultMap.containsKey("output")) {
                Object outputObj = resultMap.get("output");
                if (outputObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> outputMap = (Map<String, Object>) outputObj;
                    if (outputMap.containsKey("url")) {
                        imageUrl = String.valueOf(outputMap.get("url"));
                        System.out.println("🔍 从 'output.url' 获取到：" + imageUrl);
                    }
                }
            }
            // 方式 3: 尝试 results 嵌套
            else if (resultMap.containsKey("results")) {
                Object nestedResults = resultMap.get("results");
                if (nestedResults instanceof java.util.List && !((java.util.List<?>) nestedResults).isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedMap = (Map<String, Object>) ((java.util.List<?>) nestedResults).get(0);
                    if (nestedMap.containsKey("url")) {
                        imageUrl = String.valueOf(nestedMap.get("url"));
                        System.out.println("🔍 从 'results[0].url' 获取到：" + imageUrl);
                    }
                }
            }
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                System.out.println("✅ 图片生成成功：" + imageUrl);
                return imageUrl;
            } else {
                System.err.println("⚠️ 所有方式都未获取到 URL");
                System.err.println("🔍 完整结果对象：" + resultMap.toString());
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ 图片生成异常：" + e.getMessage());
            System.err.println("🔍 异常类型：" + e.getClass().getName());
            e.printStackTrace();
            return null;
        }
    }
}
