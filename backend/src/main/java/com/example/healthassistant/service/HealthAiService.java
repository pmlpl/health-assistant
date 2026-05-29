package com.example.healthassistant.service;

import com.example.healthassistant.ai.AiPromptTemplates;
import com.example.healthassistant.ai.ChatClientRouter;
import com.example.healthassistant.ai.ChatMessage;
import com.example.healthassistant.exception.AiNotConfiguredException;
import com.example.healthassistant.model.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 健康助手 AI 业务层：拼 prompt、会话历史、食谱配图等。
 * 具体模型调用由 {@link ChatClientRouter} 按用户设置路由到 LM Studio / 通义 / DeepSeek 等。
 */
@Service
public class HealthAiService {

    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    @Autowired
    private UserProfileLoadService userProfileLoadService;

    @Autowired
    private ChatClientRouter chatClientRouter;

    @Autowired
    private RecipeImageSearchService recipeImageSearchService;

    /** 用户会话历史（userId -> 消息列表） */
    private final Map<String, List<ChatMessage>> sessionHistories = new ConcurrentHashMap<>();

    @Value("${ai.chat.max-history-messages:24}")
    private int maxHistoryMessages;

    @Value("${ai.recipe.max-tokens:4096}")
    private int recipeMaxTokens;

    public String getNutritionAdvice(String userId, String userMessage) {
        System.out.println("正在调用 AI 服务，用户 ID: " + userId + ", 用户消息：" + userMessage);

        try {
            UserProfile userProfile = userProfileLoadService.loadWithCollections(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);

            List<ChatMessage> history = sessionHistories.computeIfAbsent(userId, k -> new ArrayList<>());
            if (history.isEmpty()) {
                history.add(new ChatMessage(ROLE_SYSTEM, AiPromptTemplates.nutritionConsultSystem(userProfileInfo)));
            }
            history.add(new ChatMessage(ROLE_USER, userMessage));
            trimHistory(history);

            String response = chatClientRouter.completeWithMessages(userId, history);
            history.add(new ChatMessage(ROLE_ASSISTANT, response));
            System.out.println("AI返回内容: " + response);
            return response;

        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("营养咨询异常: " + e.getMessage());
            throw new RuntimeException("营养咨询失败: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> generateRecipeRecommendations(UserProfile userProfile,
            Map<String, Double> consumedNutrition, String mealType) {
        String userId = userProfile.getUserId();

        try {
            String prompt = buildRecipePrompt(userProfile, consumedNutrition, mealType);
            String system = AiPromptTemplates.recipeJsonSystem();
            String jsonResponse = chatClientRouter.complete(userId, system, prompt, recipeMaxTokens);
            Map<String, Object> responseMap = parseRecipeJsonResponse(jsonResponse);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) responseMap.get("recommendations");
            attachRecipeImages(userId, recommendations);
            return responseMap;

        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("AI食谱推荐失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("食谱推荐失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseRecipeJsonResponse(String jsonResponse) throws com.fasterxml.jackson.core.JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String cleaned = jsonResponse.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```json?", "").replaceAll("```$", "").trim();
        }
        try {
            return objectMapper.readValue(cleaned, Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return objectMapper.readValue(cleaned.substring(start, end + 1), Map.class);
            }
            throw new RuntimeException("AI 返回的食谱 JSON 无法解析，请重试。原始片段: "
                    + cleaned.substring(0, Math.min(200, cleaned.length())), e);
        }
    }

    private void attachRecipeImages(String userId, List<Map<String, Object>> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }
        AtomicInteger successCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> imageTasks = new ArrayList<>();
        for (Map<String, Object> recipe : recommendations) {
            imageTasks.add(CompletableFuture.runAsync(() -> {
                try {
                    String recipeName = resolveRecipeName(recipe);
                    List<String> ingredients = normalizeIngredientList(recipe.get("ingredients"));
                    RecipeImageResult img = recipeImageSearchService.findAndStoreRecipeImage(
                            userId, recipeName, ingredients);
                    synchronized (recipe) {
                        if (img.isSuccess()) {
                            recipe.put("imageUrl", img.imageUrl());
                            recipe.put("image", img.imageUrl());
                            successCount.incrementAndGet();
                        } else if (img.imageStatus() != null) {
                            recipe.put("imageStatus", img.imageStatus());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("单道菜配图异常: " + e.getMessage());
                    e.printStackTrace();
                    synchronized (recipe) {
                        recipe.put("imageStatus", "SEARCH_FAILED");
                    }
                }
            }));
        }
        for (CompletableFuture<Void> task : imageTasks) {
            try {
                task.join();
            } catch (Exception e) {
                System.err.println("配图任务 join 异常: " + e.getMessage());
            }
        }
        System.out.println("食谱配图完成：" + successCount.get() + "/" + recommendations.size());
    }

    private static String resolveRecipeName(Map<String, Object> recipe) {
        Object name = recipe.get("recipeName");
        if (name == null) {
            name = recipe.get("name");
        }
        if (name == null) {
            name = recipe.get("title");
        }
        return name != null ? String.valueOf(name) : "healthy meal";
    }

    private static List<String> normalizeIngredientList(Object raw) {
        List<String> list = new ArrayList<>();
        if (raw == null) {
            return list;
        }
        if (raw instanceof List<?> items) {
            for (Object item : items) {
                if (item == null) {
                    continue;
                }
                if (item instanceof String s) {
                    if (!s.isBlank()) {
                        list.add(s.trim());
                    }
                } else if (item instanceof Map<?, ?> map) {
                    Object n = map.get("name");
                    if (n == null) {
                        n = map.get("ingredient");
                    }
                    if (n != null && !String.valueOf(n).isBlank()) {
                        list.add(String.valueOf(n).trim());
                    }
                } else {
                    list.add(String.valueOf(item).trim());
                }
            }
        } else if (raw instanceof String s) {
            if (!s.isBlank()) {
                list.add(s.trim());
            }
        }
        return list;
    }

    private String buildRecipePrompt(UserProfile userProfile, Map<String, Double> consumedNutrition, String mealType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为我推荐一顿").append(translateMealType(mealType)).append("。\n\n");
        prompt.append("【我的健康档案】\n");
        prompt.append(buildUserProfileInfo(userProfile));
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

    /**
     * 流式营养咨询：通过 SSE 逐段推送；会话历史与同步接口共用。
     */
    public void streamNutritionAdvice(String userId, String userMessage, SseEmitter emitter) {
        try {
            UserProfile userProfile = userProfileLoadService.loadWithCollections(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);

            List<ChatMessage> history = sessionHistories.computeIfAbsent(userId, k -> new ArrayList<>());
            if (history.isEmpty()) {
                history.add(new ChatMessage(ROLE_SYSTEM, AiPromptTemplates.nutritionConsultSystem(userProfileInfo)));
            }
            history.add(new ChatMessage(ROLE_USER, userMessage));
            trimHistory(history);

            StringBuilder full = new StringBuilder();
            chatClientRouter.streamWithMessages(userId, history, chunk -> {
                full.append(chunk);
                try {
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (Exception sendEx) {
                    throw new RuntimeException(sendEx);
                }
            });

            String response = full.toString();
            history.add(new ChatMessage(ROLE_ASSISTANT, response));
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            try {
                emitter.send(SseEmitter.event().name("error").data(msg));
                emitter.complete();
            } catch (Exception ignored) {
                try {
                    emitter.complete();
                } catch (Exception ignoredAgain) {
                }
            }
        }
    }

    public void clearSessionHistory(String userId) {
        sessionHistories.remove(userId);
        System.out.println("已清除用户 " + userId + " 的会话历史");
    }

    public int getSessionHistoryLength(String userId) {
        List<ChatMessage> history = sessionHistories.get(userId);
        return history != null ? history.size() : 0;
    }

    public String analyzeDailyNutrition(String userId, Map<String, Object> nutritionData) {
        System.out.println("正在调用 AI分析，用户 ID: " + userId);
        System.out.println("接收到的数据：" + nutritionData);
        System.out.println("analysisType: " + nutritionData.get("analysisType"));

        try {
            UserProfile userProfile = userProfileLoadService.loadWithCollections(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);
            boolean isFitnessAnalysis = "fitness".equals(nutritionData.get("analysisType"));

            String analysisPrompt;
            String systemMessage;
            if (isFitnessAnalysis) {
                analysisPrompt = buildFitnessAnalysisPrompt(userProfileInfo, nutritionData);
                systemMessage = AiPromptTemplates.fitnessCoachSystem();
            } else {
                analysisPrompt = buildNutritionAnalysisPrompt(userProfileInfo, nutritionData);
                systemMessage = AiPromptTemplates.dailyNutritionAnalysisSystem();
            }

            String response = chatClientRouter.complete(userId, systemMessage, analysisPrompt);
            System.out.println("AI分析完成，类型：" + (isFitnessAnalysis ? "健身" : "营养"));
            return response;

        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("AI分析失败：" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("营养分析失败: " + e.getMessage(), e);
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

    public String analyzeFitnessWorkout(String userId, Map<String, Object> workoutData) {
        System.out.println("正在调用 AI 分析健身收获，用户 ID: " + userId);
        System.out.println("健身数据：" + workoutData);

        try {
            UserProfile userProfile = userProfileLoadService.loadWithCollections(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);
            String analysisPrompt = buildFitnessWorkoutPrompt(userProfileInfo, workoutData);
            String response = chatClientRouter.complete(userId,
                    AiPromptTemplates.fitnessCoachSystem(),
                    analysisPrompt);
            System.out.println("AI 健身收获分析完成");
            return response;

        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("AI 健身收获分析失败：" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("健身分析失败: " + e.getMessage(), e);
        }
    }

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

    public String getMentalHealthAdvice(String userId, String userMessage) {
        System.out.println("正在调用AI服务进行心理健康咨询，用户ID: " + userId + ", 用户消息: " + userMessage);

        try {
            UserProfile userProfile = userProfileLoadService.loadWithCollections(userId);
            String userProfileInfo = buildUserProfileInfo(userProfile);

            List<ChatMessage> history = sessionHistories.computeIfAbsent(userId, k -> new ArrayList<>());
            if (history.isEmpty()) {
                history.add(new ChatMessage(ROLE_SYSTEM, AiPromptTemplates.mentalHealthSystem(userProfileInfo)));
            }
            history.add(new ChatMessage(ROLE_USER, userMessage));
            trimHistory(history);

            String response = chatClientRouter.completeWithMessages(userId, history);
            history.add(new ChatMessage(ROLE_ASSISTANT, response));

            System.out.println("AI返回内容: " + response);
            return response;

        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("心理健康咨询异常: " + e.getMessage());
            throw new RuntimeException("心理健康咨询失败: " + e.getMessage(), e);
        }
    }

    private void trimHistory(List<ChatMessage> history) {
        if (history.size() <= maxHistoryMessages) {
            return;
        }
        List<ChatMessage> trimmedHistory = new ArrayList<>();
        if (!history.isEmpty() && ROLE_SYSTEM.equals(history.get(0).role())) {
            trimmedHistory.add(history.get(0));
        }
        int startIndex = Math.max(1, history.size() - (maxHistoryMessages - 1));
        trimmedHistory.addAll(history.subList(startIndex, history.size()));
        history.clear();
        history.addAll(trimmedHistory);
    }
}
