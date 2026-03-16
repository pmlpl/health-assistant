package com.example.healthassistant.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.ark.runtime.model.responses.content.InputContentItemText;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.service.ArkService;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.example.healthassistant.config.EnvConfig;

@Service
public class DoubaoFoodRecognitionService {

    @Value("${doubao.api.key:}")
    private String apiKeyFromConfig;

    @Value("${doubao.model.name:doubao-seed-1-8-251228}")
    private String modelName;

    private ArkService arkService;

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

    // API 调用超时时间（毫秒）- 增加到 60 秒
    private static final int API_TIMEOUT_MS = 60000;

    // API 调用最大重试次数 - 减少到 2 次
    private static final int MAX_RETRY_COUNT = 2;

    // 重试间隔时间（毫秒）- 增加到 2 秒
    private static final int RETRY_INTERVAL_MS = 2000;

    /**
     * 初始化豆包 AI 服务
     */
    private void initializeDoubaoService() {
        String effectiveApiKey = getApiKey();
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            throw new RuntimeException("未配置豆包 API 密钥，请在 backend/.env 文件中设置 DOUBAO_API_KEY");
        }

        if (arkService == null) {
            arkService = ArkService.builder()
                    .apiKey(effectiveApiKey)
                    .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                    .build();
        }
    }

    /**
     * 通用 API 调用方法，包含超时处理和重试机制
     */
    private ResponseObject callApiWithRetry(CreateResponsesRequest request) throws Exception {
        AtomicInteger retryCount = new AtomicInteger(0);

        while (true) {
            try {
                // 初始化服务
                initializeDoubaoService();

                // 创建一个线程池来执行 API 调用
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<ResponseObject> future = executor.submit(() -> {
                    return arkService.createResponse(request);
                });

                try {
                    // 设置超时时间
                    System.out.println("正在调用豆包 AI API...");
                    return future.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    System.err.println("⏱️ API 调用超时（超过" + (API_TIMEOUT_MS / 1000) + "秒），正在重试...");
                    future.cancel(true);
                    throw new Exception("API 调用超时", e);
                } finally {
                    executor.shutdown();
                }
            } catch (Exception e) {
                if (retryCount.incrementAndGet() <= MAX_RETRY_COUNT) {
                    System.err.println("❌ API 调用失败，第 " + retryCount.get() + " 次重试... (错误：" + e.getMessage() + ")");
                    Thread.sleep(RETRY_INTERVAL_MS * retryCount.get());
                } else {
                    System.err.println("💥 API 调用失败，已达到最大重试次数 (" + MAX_RETRY_COUNT + "次)，切换到本地识别模式");
                    throw e;
                }
            }
        }
    }

    /**
     * 使用豆包 AI 识别食物并获取营养信息
     * 如果没有配置 API Key，自动切换到本地识别模式
     */
    public List<FoodNutrition> recognizeFoodWithDoubao(String foodDescription) {
        List<FoodNutrition> results = new ArrayList<>();
        String effectiveApiKey = getApiKey();

        // 检查 API 密钥配置，如果没有配置则使用本地识别模式
        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            System.out.println("==============================================");
            System.out.println("豆包 API 密钥未配置，切换到本地演示模式");
            System.out.println("提示：如需使用真实 AI 服务，请在 backend/.env 文件中设置 DOUBAO_API_KEY");
            System.out.println("==============================================");
            return recognizeLocally(foodDescription);
        }

        try {
            // 构造请求
            CreateResponsesRequest request = CreateResponsesRequest.builder()
                    .model(modelName)
                    .input(ResponsesInput.builder().addListItem(
                            ItemEasyMessage.builder()
                                    .role(ResponsesConstants.MESSAGE_ROLE_USER)
                                    .content(
                                            MessageContent.builder()
                                                    .addListItem(InputContentItemText.builder()
                                                            .text(constructFoodRecognitionPrompt(foodDescription))
                                                            .build())
                                                    .build())
                                    .build())
                            .build())
                    .build();

            // 发送请求（带超时和重试机制）
            ResponseObject response = callApiWithRetry(request);

            if (response != null) {
                System.out.println("豆包API调用成功");

                // 从ResponseObject中提取实际的文本内容
                String actualResponseText = extractTextFromResponse(response);

                if (actualResponseText != null && !actualResponseText.isEmpty()) {
                    System.out.println("提取到的响应文本: " + actualResponseText);

                    // 解析提取到的文本内容
                    List<RecognizedFood> recognizedFoods = parseFoodFromText(actualResponseText);

                    // 将识别结果转换为FoodNutrition对象
                    for (RecognizedFood recognizedFood : recognizedFoods) {
                        FoodNutrition nutrition = new FoodNutrition();
                        nutrition.setName(recognizedFood.getName());
                        nutrition.setCalories(recognizedFood.getCalories());
                        nutrition.setProtein(recognizedFood.getProtein());
                        nutrition.setCarbs(recognizedFood.getCarbs());
                        nutrition.setFat(recognizedFood.getFat());
                        nutrition.setFiber(recognizedFood.getFiber());
                        nutrition.setWeight(recognizedFood.getWeightGrams());
                        results.add(nutrition);
                    }

                    System.out.println("成功解析API响应，识别到 " + results.size() + " 种食物");

                    // 如果没有识别到食物，使用本地识别作为备选
                    if (results.isEmpty()) {
                        System.out.println("API未识别到食物，切换到本地识别模式");
                        return recognizeLocally(foodDescription);
                    }
                } else {
                    System.out.println("无法从响应中提取有效文本内容");
                    // 如果无法提取有效内容，使用本地识别作为备选
                    return recognizeLocally(foodDescription);
                }
            } else {
                System.out.println("API响应为空");
                // 如果响应为空，使用本地识别作为备选
                return recognizeLocally(foodDescription);
            }

        } catch (Exception e) {
            System.err.println("豆包API调用失败: " + e.getMessage());
            e.printStackTrace();
            // API调用失败时，切换到本地识别模式
            System.out.println("切换到本地识别模式");
            return recognizeLocally(foodDescription);
        } finally {
            // 清理资源
            if (arkService != null) {
                try {
                    arkService.shutdownExecutor();
                } catch (Exception e) {
                    System.err.println("关闭ArkService时出错: " + e.getMessage());
                }
            }
        }

        return results;
    }

    /**
     * 构造食物识别提示词
     */
    private String constructFoodRecognitionPrompt(String foodDescription) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("任务：分析食物的营养成分并返回 JSON 数据\n\n");
        prompt.append("输入：\"").append(foodDescription).append("\"\n\n");
        prompt.append("输出格式要求：\n");
        prompt.append("{\n");
        prompt.append("  \"foods\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"食物名称\",\n");
        prompt.append("      \"weight\": 实际重量（克）,\n");
        prompt.append("      \"calories\": 每 100 克热量 (kcal),\n");
        prompt.append("      \"protein\": 每 100 克蛋白质 (g),\n");
        prompt.append("      \"carbs\": 每 100 克碳水 (g),\n");
        prompt.append("      \"fat\": 每 100 克脂肪 (g),\n");
        prompt.append("      \"fiber\": 每 100 克纤维 (g)\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("关键规则：\n");
        prompt.append("1. 仅输出 JSON，禁止任何额外文字、解释、标记或注释\n");
        prompt.append("2. name: 食物的准确名称\n");
        prompt.append("3. weight: 根据描述中的量词估算的实际重量（例如：1 个芒果约 150 克）\n");
        prompt.append("4. calories/protein/carbs/fat/fiber: 均为每 100 克食物的营养含量，数值精确到小数点后 1 位\n");
        prompt.append("5. 如果无法识别，返回空数组：{\"foods\": []}\n");
        prompt.append("6. 必须返回合法有效的 JSON 格式\n\n");
        prompt.append("示例输入：\"1 个芒果\"\n");
        prompt.append("示例输出：{\"foods\":[{\"name\":\"芒果\",\"weight\":150,\"calories\":60.0,\"protein\":0.6,\"carbs\":14.9,\"fat\":0.2,\"fiber\":1.3}]}\n\n");
        prompt.append("现在请分析输入并直接返回 JSON：");
        return prompt.toString();
    }

    /**
     * 本地识别作为备选方案
     */
    private List<FoodNutrition> recognizeLocally(String foodDescription) {
        List<FoodNutrition> results = new ArrayList<>();

        Map<String, FoodNutrition> foodDatabase = createFoodDatabase();

        // 首先尝试提取食物名称
        String extractedFoodName = extractFoodName(foodDescription);
        if (extractedFoodName != null && foodDatabase.containsKey(extractedFoodName)) {
            FoodNutrition nutrition = foodDatabase.get(extractedFoodName);
            double weight = extractWeight(foodDescription);
            nutrition.setWeight(weight);
            results.add(nutrition);
            return results;
        }

        // 如果没有匹配到，尝试精确匹配食物名称
        for (Map.Entry<String, FoodNutrition> entry : foodDatabase.entrySet()) {
            String foodKey = entry.getKey();
            // 精确匹配食物名称（避免部分匹配）
            if (foodDescription.contains(foodKey)) {
                // 检查是否是完整的食物名称匹配，而不是部分匹配
                boolean isExactMatch = false;
                // 检查前后是否是边界字符
                int index = foodDescription.indexOf(foodKey);
                if (index == 0 || !Character.isLetterOrDigit(foodDescription.charAt(index - 1))) {
                    int endIndex = index + foodKey.length();
                    if (endIndex == foodDescription.length()
                            || !Character.isLetterOrDigit(foodDescription.charAt(endIndex))) {
                        isExactMatch = true;
                    }
                }

                if (isExactMatch) {
                    FoodNutrition nutrition = entry.getValue();
                    double weight = estimateWeight(nutrition.getName(), foodDescription);
                    nutrition.setWeight(weight);
                    results.add(nutrition);
                    return results; // 只返回第一个匹配的结果
                }
            }
        }

        // 如果还是没有匹配到，尝试关键词提取
        if (results.isEmpty()) {
            results = extractByKeywords(foodDescription, foodDatabase);
        }

        return results;
    }

    /**
     * 从输入中提取食物名称
     */
    private String extractFoodName(String description) {
        // 食物数据库中的常见食物名称（按长度降序排列，优先匹配长名称）
        String[] foodNames = { "三文鱼", "胡萝卜", "西红柿", "西兰花", "猕猴桃",
                "鸡肉", "猪肉", "牛肉", "鱼肉", "豆腐", "青菜", "白菜",
                "黄瓜", "苹果", "香蕉", "橙子", "葡萄", "牛排", "土豆",
                "洋葱", "大蒜", "辣椒", "茄子", "豆角", "菠菜", "芹菜",
                "韭菜", "香菜", "蘑菇", "草莓", "西瓜", "梨", "桃子",
                "芒果", "菠萝", "酸奶", "奶酪", "黄油", "面包", "蛋糕",
                "饼干", "巧克力", "米饭", "面条", "馒头", "包子", "饺子",
                "鸡蛋", "牛奶", "杏仁", "核桃", "腰果", "花生", "开心果",
                "虾", "蟹", "扇贝", "鱿鱼", "燕麦", "糙米", "藜麦", "小米", "玉米",
                "蓝莓", "覆盆子", "牛油果", "石榴", "百香果", "芦笋", "抱子甘蓝",
                "甜椒", "紫甘蓝", "秋葵" };

        // 清理输入，移除常见的量词、修饰词和重量信息
        String cleanedDescription = description
                .replaceAll("[0-9]+\s*[克千克gkg]+", "") // 移除重量信息
                .replaceAll("[一二三四五六七八九十百千]+\s*[个只条碗杯盘碟份]", "") // 移除量词
                .replaceAll("[大小多少新鲜生熟好坏嫩老]", "") // 移除修饰词
                .replaceAll("\\s+", "") // 移除空格
                .trim();

        // 尝试精确匹配清理后的输入
        for (String foodName : foodNames) {
            if (cleanedDescription.equals(foodName) || cleanedDescription.contains(foodName)) {
                // 检查是否是完整匹配
                int index = cleanedDescription.indexOf(foodName);
                if (index == 0 || !Character.isLetterOrDigit(cleanedDescription.charAt(index - 1))) {
                    int endIndex = index + foodName.length();
                    if (endIndex == cleanedDescription.length()
                            || !Character.isLetterOrDigit(cleanedDescription.charAt(endIndex))) {
                        return foodName;
                    }
                }
            }
        }

        // 尝试匹配原始输入
        for (String foodName : foodNames) {
            if (description.contains(foodName)) {
                // 检查是否是完整匹配
                int index = description.indexOf(foodName);
                if (index == 0 || !Character.isLetterOrDigit(description.charAt(index - 1))) {
                    int endIndex = index + foodName.length();
                    if (endIndex == description.length() || !Character.isLetterOrDigit(description.charAt(endIndex))) {
                        return foodName;
                    }
                }
            }
        }

        // 作为最后手段，尝试部分匹配（但只匹配完整的食物名称）
        for (String foodName : foodNames) {
            if (description.contains(foodName)) {
                return foodName;
            }
        }

        return null;
    }

    /**
     * 从输入中提取重量信息
     */
    private double extractWeight(String description) {
        // 使用正则表达式提取数字
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(description);

        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                // 静默处理解析错误，返回默认值
            }
        }

        // 默认返回100克
        return 100.0;
    }

    /**
     * 关键词提取识别
     */
    private List<FoodNutrition> extractByKeywords(String description, Map<String, FoodNutrition> database) {
        List<FoodNutrition> results = new ArrayList<>();

        String[] descriptions = { "一个小", "一个", "两个", "三个" };

        for (String desc : descriptions) {
            if (description.contains(desc)) {
                String foodPart = description.replace(desc, "").trim();
                if (database.containsKey(foodPart)) {
                    FoodNutrition nutrition = database.get(foodPart);
                    double weight = estimateWeightByDescription(desc, nutrition.getName());
                    nutrition.setWeight(weight);
                    results.add(nutrition);
                }
                break;
            }
        }

        return results;
    }

    /**
     * 估算食物重量
     */
    private double estimateWeight(String foodName, String description) {
        String lowerDesc = description.toLowerCase();

        // 基于描述估算重量
        if (lowerDesc.contains("一个小") || lowerDesc.contains("小")) {
            return getBaseWeight(foodName) * 0.5;
        } else if (lowerDesc.contains("半个") || lowerDesc.contains("半")) {
            return getBaseWeight(foodName) * 0.5;
        } else if (lowerDesc.contains("一个") || lowerDesc.contains("一只") || lowerDesc.contains("一条")) {
            return getBaseWeight(foodName) * 0.8;
        }

        // 默认重量
        return getBaseWeight(foodName);
    }

    /**
     * 基于描述词估算重量
     */
    private double estimateWeightByDescription(String description, String foodName) {
        switch (description) {
            case "一个小":
                return getBaseWeight(foodName) * 0.5;
            case "一个":
            case "一只":
            case "一条":
                return getBaseWeight(foodName) * 0.8;
            default:
                return getBaseWeight(foodName);
        }
    }

    /**
     * 获取食物基础重量（克）
     */
    private double getBaseWeight(String foodName) {
        Map<String, Double> baseWeights = new HashMap<>();
        baseWeights.put("米饭", 150.0);
        baseWeights.put("面条", 100.0);
        baseWeights.put("馒头", 80.0);
        baseWeights.put("包子", 100.0);
        baseWeights.put("饺子", 20.0);
        baseWeights.put("鸡蛋", 60.0);
        baseWeights.put("牛奶", 250.0);
        baseWeights.put("面包", 50.0);
        baseWeights.put("苹果", 150.0);
        baseWeights.put("香蕉", 120.0);
        baseWeights.put("橙子", 130.0);
        baseWeights.put("鸡肉", 100.0);
        baseWeights.put("猪肉", 100.0);
        baseWeights.put("牛肉", 100.0);
        baseWeights.put("鱼肉", 100.0);
        baseWeights.put("豆腐", 100.0);
        baseWeights.put("青菜", 80.0);
        baseWeights.put("白菜", 80.0);
        baseWeights.put("西红柿", 100.0);

        // 新增食物的基础重量
        baseWeights.put("西兰花", 100.0);
        baseWeights.put("三文鱼", 100.0);
        baseWeights.put("牛排", 100.0);
        baseWeights.put("土豆", 100.0);
        baseWeights.put("洋葱", 100.0);
        baseWeights.put("大蒜", 50.0);
        baseWeights.put("辣椒", 50.0);
        baseWeights.put("茄子", 100.0);
        baseWeights.put("豆角", 100.0);
        baseWeights.put("菠菜", 100.0);
        baseWeights.put("芹菜", 100.0);
        baseWeights.put("韭菜", 100.0);
        baseWeights.put("香菜", 50.0);
        baseWeights.put("蘑菇", 100.0);
        baseWeights.put("草莓", 150.0);
        baseWeights.put("西瓜", 200.0);
        baseWeights.put("梨", 150.0);
        baseWeights.put("桃子", 150.0);
        baseWeights.put("芒果", 150.0);
        baseWeights.put("菠萝", 150.0);
        baseWeights.put("猕猴桃", 100.0);
        baseWeights.put("酸奶", 200.0);
        baseWeights.put("奶酪", 50.0);
        baseWeights.put("黄油", 30.0);
        baseWeights.put("蛋糕", 100.0);
        baseWeights.put("饼干", 50.0);
        baseWeights.put("巧克力", 50.0);

        // 坚果类
        baseWeights.put("杏仁", 30.0);
        baseWeights.put("核桃", 30.0);
        baseWeights.put("腰果", 30.0);
        baseWeights.put("花生", 30.0);
        baseWeights.put("开心果", 30.0);

        // 海鲜类
        baseWeights.put("三文鱼", 100.0);
        baseWeights.put("虾", 100.0);
        baseWeights.put("蟹", 100.0);
        baseWeights.put("扇贝", 100.0);
        baseWeights.put("鱿鱼", 100.0);

        // 谷物类
        baseWeights.put("燕麦", 50.0);
        baseWeights.put("糙米", 100.0);
        baseWeights.put("藜麦", 50.0);
        baseWeights.put("小米", 100.0);
        baseWeights.put("玉米", 150.0);

        // 水果类补充
        baseWeights.put("蓝莓", 100.0);
        baseWeights.put("覆盆子", 100.0);
        baseWeights.put("牛油果", 150.0);
        baseWeights.put("石榴", 150.0);
        baseWeights.put("百香果", 100.0);

        // 蔬菜类补充
        baseWeights.put("芦笋", 80.0);
        baseWeights.put("抱子甘蓝", 80.0);
        baseWeights.put("甜椒", 100.0);
        baseWeights.put("紫甘蓝", 80.0);
        baseWeights.put("秋葵", 80.0);

        return baseWeights.getOrDefault(foodName, 100.0);
    }

    /**
     * 创建本地食物数据库
     */
    private Map<String, FoodNutrition> createFoodDatabase() {
        Map<String, FoodNutrition> database = new HashMap<>();

        // 主食类
        database.put("米饭", createNutrition("米饭", 116, 2.6, 25.9, 0.3, 0.3));
        database.put("面条", createNutrition("面条", 130, 4.0, 26.7, 1.2, 1.0));
        database.put("馒头", createNutrition("馒头", 223, 7.0, 47.0, 1.5, 2.0));
        database.put("包子", createNutrition("包子", 220, 8.0, 35.0, 5.0, 1.5));
        database.put("饺子", createNutrition("饺子", 230, 9.0, 30.0, 8.0, 1.2));

        // 蛋白质类
        database.put("鸡蛋", createNutrition("鸡蛋", 144, 13.3, 1.1, 10.0, 0.1));
        database.put("牛奶", createNutrition("牛奶", 42, 3.2, 4.8, 1.5, 0.1));
        database.put("鸡肉", createNutrition("鸡肉", 165, 19.3, 0, 9.4, 0));
        database.put("猪肉", createNutrition("猪肉", 143, 20.3, 0, 6.2, 0));
        database.put("牛肉", createNutrition("牛肉", 125, 19.8, 0, 4.2, 0));
        database.put("鱼肉", createNutrition("鱼肉", 113, 17.8, 0, 4.9, 0));
        database.put("豆腐", createNutrition("豆腐", 76, 8.1, 3.8, 4.2, 0.5));

        // 蔬菜类
        database.put("青菜", createNutrition("青菜", 15, 1.5, 2.6, 0.3, 1.1));
        database.put("白菜", createNutrition("白菜", 17, 1.5, 3.1, 0.1, 0.8));
        database.put("西红柿", createNutrition("西红柿", 18, 0.9, 3.9, 0.2, 1.2));
        database.put("黄瓜", createNutrition("黄瓜", 15, 0.8, 2.9, 0.2, 0.5));
        database.put("胡萝卜", createNutrition("胡萝卜", 41, 0.9, 9.6, 0.2, 2.8));
        database.put("西兰花", createNutrition("西兰花", 34, 2.8, 7.0, 0.4, 2.6));
        database.put("土豆", createNutrition("土豆", 77, 2.0, 17.4, 0.1, 2.2));
        database.put("洋葱", createNutrition("洋葱", 40, 1.1, 9.3, 0.1, 1.7));
        database.put("大蒜", createNutrition("大蒜", 149, 6.4, 33.0, 0.5, 2.1));
        database.put("辣椒", createNutrition("辣椒", 21, 0.9, 4.7, 0.1, 1.7));
        database.put("茄子", createNutrition("茄子", 25, 1.0, 5.9, 0.1, 1.3));
        database.put("豆角", createNutrition("豆角", 34, 2.5, 7.4, 0.2, 2.1));
        database.put("菠菜", createNutrition("菠菜", 23, 2.9, 3.6, 0.4, 2.2));
        database.put("芹菜", createNutrition("芹菜", 16, 0.7, 3.1, 0.1, 1.6));
        database.put("韭菜", createNutrition("韭菜", 26, 2.4, 4.6, 0.4, 1.4));
        database.put("香菜", createNutrition("香菜", 37, 2.2, 6.9, 0.5, 2.8));
        database.put("蘑菇", createNutrition("蘑菇", 22, 3.1, 3.2, 0.3, 1.0));

        // 水果类
        database.put("苹果", createNutrition("苹果", 52, 0.3, 13.8, 0.2, 2.4));
        database.put("香蕉", createNutrition("香蕉", 93, 1.1, 22.8, 0.3, 2.6));
        database.put("橙子", createNutrition("橙子", 47, 0.9, 11.8, 0.1, 2.4));
        database.put("葡萄", createNutrition("葡萄", 69, 0.7, 17.2, 0.2, 0.9));
        database.put("草莓", createNutrition("草莓", 32, 0.7, 7.7, 0.3, 2.0));
        database.put("西瓜", createNutrition("西瓜", 30, 0.6, 7.5, 0.1, 0.4));
        database.put("梨", createNutrition("梨", 57, 0.4, 15.2, 0.1, 3.1));
        database.put("桃子", createNutrition("桃子", 39, 0.9, 9.5, 0.1, 1.5));
        database.put("芒果", createNutrition("芒果", 60, 0.8, 15.0, 0.4, 1.6));
        database.put("菠萝", createNutrition("菠萝", 50, 0.5, 13.1, 0.1, 1.4));
        database.put("猕猴桃", createNutrition("猕猴桃", 61, 1.1, 14.6, 0.4, 2.1));

        // 乳制品和零食类
        database.put("酸奶", createNutrition("酸奶", 59, 10.0, 3.4, 1.5, 0));
        database.put("奶酪", createNutrition("奶酪", 328, 25.0, 2.4, 23.0, 0));
        database.put("黄油", createNutrition("黄油", 717, 0.5, 0.1, 81.0, 0));
        database.put("面包", createNutrition("面包", 265, 9.0, 49.0, 3.2, 2.7));
        database.put("蛋糕", createNutrition("蛋糕", 349, 4.6, 65.0, 9.0, 0.9));
        database.put("饼干", createNutrition("饼干", 435, 7.5, 71.0, 13.0, 1.6));
        database.put("巧克力", createNutrition("巧克力", 546, 8.5, 47.0, 31.0, 2.0));

        // 坚果类
        database.put("杏仁", createNutrition("杏仁", 579, 21.2, 21.6, 49.9, 12.5));
        database.put("核桃", createNutrition("核桃", 654, 15.2, 14.0, 65.2, 6.7));
        database.put("腰果", createNutrition("腰果", 553, 18.2, 27.2, 43.8, 3.6));
        database.put("花生", createNutrition("花生", 567, 25.8, 16.1, 49.2, 8.5));
        database.put("开心果", createNutrition("开心果", 552, 20.2, 27.6, 45.1, 7.0));

        // 海鲜类
        database.put("三文鱼", createNutrition("三文鱼", 208, 25.4, 0, 10.4, 0));
        database.put("虾", createNutrition("虾", 99, 20.4, 1.0, 0.9, 0));
        database.put("蟹", createNutrition("蟹", 87, 19.0, 0, 1.2, 0));
        database.put("扇贝", createNutrition("扇贝", 74, 13.4, 2.6, 0.8, 0));
        database.put("鱿鱼", createNutrition("鱿鱼", 75, 15.5, 3.0, 0.8, 0));

        // 谷物类
        database.put("燕麦", createNutrition("燕麦", 389, 16.9, 66.3, 6.9, 10.6));
        database.put("糙米", createNutrition("糙米", 362, 7.5, 77.2, 2.8, 3.5));
        database.put("藜麦", createNutrition("藜麦", 368, 14.1, 64.2, 6.1, 7.0));
        database.put("小米", createNutrition("小米", 358, 9.0, 73.5, 3.3, 1.6));
        database.put("玉米", createNutrition("玉米", 86, 3.2, 19.0, 1.2, 2.7));

        // 水果类补充
        database.put("蓝莓", createNutrition("蓝莓", 57, 0.7, 14.5, 0.3, 2.4));
        database.put("覆盆子", createNutrition("覆盆子", 52, 1.4, 11.9, 0.7, 6.5));
        database.put("牛油果", createNutrition("牛油果", 160, 2.0, 8.5, 14.7, 6.7));
        database.put("石榴", createNutrition("石榴", 83, 1.7, 18.7, 1.2, 4.0));
        database.put("百香果", createNutrition("百香果", 97, 2.2, 23.3, 0.7, 10.4));

        // 蔬菜类补充
        database.put("芦笋", createNutrition("芦笋", 20, 2.2, 3.9, 0.2, 2.1));
        database.put("抱子甘蓝", createNutrition("抱子甘蓝", 43, 3.4, 8.9, 0.8, 2.6));
        database.put("甜椒", createNutrition("甜椒", 31, 1.0, 6.7, 0.3, 2.1));
        database.put("紫甘蓝", createNutrition("紫甘蓝", 32, 1.5, 7.4, 0.2, 2.0));
        database.put("秋葵", createNutrition("秋葵", 33, 2.0, 7.0, 0.1, 3.2));

        return database;
    }

    private FoodNutrition createNutrition(String name, double calories, double protein,
            double carbs, double fat, double fiber) {
        FoodNutrition nutrition = new FoodNutrition();
        nutrition.setName(name);
        nutrition.setCalories(calories);
        nutrition.setProtein(protein);
        nutrition.setCarbs(carbs);
        nutrition.setFat(fat);
        nutrition.setFiber(fiber);
        nutrition.setWeight(100.0); // 默认100克
        return nutrition;
    }

    /**
     * 食物营养信息类
     */
    public static class FoodNutrition {
        private String name;
        private double calories; // 千卡/100g
        private double protein; // 克/100g
        private double carbs; // 克/100g
        private double fat; // 克/100g
        private double fiber; // 克/100g
        private double weight; // 实际重量(克)

        // getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getCalories() {
            return calories;
        }

        public void setCalories(double calories) {
            this.calories = calories;
        }

        public double getProtein() {
            return protein;
        }

        public void setProtein(double protein) {
            this.protein = protein;
        }

        public double getCarbs() {
            return carbs;
        }

        public void setCarbs(double carbs) {
            this.carbs = carbs;
        }

        public double getFat() {
            return fat;
        }

        public void setFat(double fat) {
            this.fat = fat;
        }

        public double getFiber() {
            return fiber;
        }

        public void setFiber(double fiber) {
            this.fiber = fiber;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        /**
         * 计算实际营养成分（基于重量）
         */
        public double getActualCalories() {
            return calories * weight / 100.0;
        }

        public double getActualProtein() {
            return protein * weight / 100.0;
        }

        public double getActualCarbs() {
            return carbs * weight / 100.0;
        }

        public double getActualFat() {
            return fat * weight / 100.0;
        }

        public double getActualFiber() {
            return fiber * weight / 100.0;
        }
    }

    /**
     * 食物识别结果类
     */
    public static class FoodRecognitionResult {
        private List<RecognizedFood> foods;
        private int totalCount;
        private TotalNutrition totalNutrition;

        // getters and setters
        public List<RecognizedFood> getFoods() {
            return foods;
        }

        public void setFoods(List<RecognizedFood> foods) {
            this.foods = foods;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public TotalNutrition getTotalNutrition() {
            return totalNutrition;
        }

        public void setTotalNutrition(TotalNutrition totalNutrition) {
            this.totalNutrition = totalNutrition;
        }
    }

    /**
     * 识别到的食物类
     */
    public static class RecognizedFood {
        private String name;
        private double weightGrams;
        private double calories;
        private double protein;
        private double carbs;
        private double fat;
        private double fiber;

        // getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getWeightGrams() {
            return weightGrams;
        }

        public void setWeightGrams(double weightGrams) {
            this.weightGrams = weightGrams;
        }

        public double getCalories() {
            return calories;
        }

        public void setCalories(double calories) {
            this.calories = calories;
        }

        public double getProtein() {
            return protein;
        }

        public void setProtein(double protein) {
            this.protein = protein;
        }

        public double getCarbs() {
            return carbs;
        }

        public void setCarbs(double carbs) {
            this.carbs = carbs;
        }

        public double getFat() {
            return fat;
        }

        public void setFat(double fat) {
            this.fat = fat;
        }

        public double getFiber() {
            return fiber;
        }

        public void setFiber(double fiber) {
            this.fiber = fiber;
        }
    }

    /**
     * 总营养成分类
     */
    public static class TotalNutrition {
        private double calories;
        private double protein;
        private double carbs;
        private double fat;
        private double fiber;

        // getters and setters
        public double getCalories() {
            return calories;
        }

        public void setCalories(double calories) {
            this.calories = calories;
        }

        public double getProtein() {
            return protein;
        }

        public void setProtein(double protein) {
            this.protein = protein;
        }

        public double getCarbs() {
            return carbs;
        }

        public void setCarbs(double carbs) {
            this.carbs = carbs;
        }

        public double getFat() {
            return fat;
        }

        public void setFat(double fat) {
            this.fat = fat;
        }

        public double getFiber() {
            return fiber;
        }

        public void setFiber(double fiber) {
            this.fiber = fiber;
        }
    }

    // 从 ResponseObject 中提取实际的文本内容
    private String extractTextFromResponse(ResponseObject response) {
        try {
            // 查看响应对象的结构，提取真正的内容
            String responseString = response.toString();
            System.out.println("完整响应字符串：" + responseString);

            // 使用正则表达式从响应中提取 JSON 部分或关键信息
            // 匹配可能的 JSON 格式内容
            Pattern jsonPattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*}", Pattern.DOTALL);
            Matcher jsonMatcher = jsonPattern.matcher(responseString);

            if (jsonMatcher.find()) {
                String jsonString = jsonMatcher.group();
                System.out.println("提取到的完整 JSON: " + jsonString);
                
                // 清理 JSON 中的多余字符（如 Markdown 标记）
                jsonString = cleanJsonString(jsonString);
                System.out.println("清理后的 JSON: " + jsonString);
                
                return jsonString;
            }

            // 如果没有找到 JSON，尝试提取文本内容
            Pattern textPattern = Pattern.compile("text='([^']*)'");
            Matcher textMatcher = textPattern.matcher(responseString);

            if (textMatcher.find()) {
                String textContent = textMatcher.group(1);
                System.out.println("提取到的文本：" + textContent);
                
                // 从文本中提取 JSON 部分
                jsonMatcher = jsonPattern.matcher(textContent);
                if (jsonMatcher.find()) {
                    String jsonString = jsonMatcher.group();
                    System.out.println("从文本中提取的 JSON: " + jsonString);
                    return cleanJsonString(jsonString);
                }
                
                return textContent;
            }

            // 最后尝试直接解析整个响应字符串
            return responseString;

        } catch (Exception e) {
            System.err.println("提取响应文本时出错：" + e.getMessage());
            return null;
        }
    }
    
    /**
     * 清理 JSON 字符串，移除 Markdown 标记和其他多余字符
     */
    private String cleanJsonString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }
        
        System.out.println("原始 JSON 字符串：" + jsonString);
        
        // 移除可能存在的 Markdown 代码块标记
        jsonString = jsonString.replaceAll("```json\\s*", "");
        jsonString = jsonString.replaceAll("```\\s*", "");
        
        // 移除开头和结尾的空白字符
        jsonString = jsonString.trim();
        
        // 如果字符串包含多个 JSON 对象，尝试提取第一个完整的
        // 查找第一个 { 和最后一个 }
        int startIndex = jsonString.indexOf('{');
        int endIndex = jsonString.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            jsonString = jsonString.substring(startIndex, endIndex + 1);
        }
        
        // 再次清理可能的多余文字（在 JSON 前后的解释）
        // 使用正则表达式匹配完整的 JSON 结构
        Pattern jsonPattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*}", Pattern.DOTALL);
        Matcher matcher = jsonPattern.matcher(jsonString);
        
        if (matcher.find()) {
            String extractedJson = matcher.group();
            System.out.println("提取并清理后的 JSON: " + extractedJson);
            return extractedJson;
        }
        
        System.out.println("清理后的 JSON: " + jsonString);
        return jsonString;
    }

    // 改进的文本解析方法
    private List<RecognizedFood> parseFoodFromText(String text) {
        List<RecognizedFood> foods = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return foods;
        }

        System.out.println("开始解析文本: " + text);

        // 首先尝试解析JSON格式
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(text);

            // 尝试不同的JSON结构
            JsonNode foodsNode = rootNode.path("foods");
            if (foodsNode.isArray()) {
                for (JsonNode foodNode : foodsNode) {
                    RecognizedFood food = parseFoodFromJsonNode(foodNode);
                    if (food != null) {
                        foods.add(food);
                    }
                }
            }
            return foods;
        } catch (Exception e) {
            System.out.println("JSON解析失败，尝试文本解析: " + e.getMessage());
        }

        // 如果JSON解析失败，使用正则表达式解析文本
        return parseFoodFromPlainText(text);
    }

    // 从JsonNode解析单个食物
    private RecognizedFood parseFoodFromJsonNode(JsonNode foodNode) {
        try {
            RecognizedFood food = new RecognizedFood();
            food.setName(foodNode.path("name").asText());
            food.setWeightGrams(foodNode.path("weight").asDouble(100.0));
            food.setCalories(foodNode.path("calories").asDouble(0));
            food.setProtein(foodNode.path("protein").asDouble(0));
            food.setCarbs(foodNode.path("carbs").asDouble(0));
            food.setFat(foodNode.path("fat").asDouble(0));
            food.setFiber(foodNode.path("fiber").asDouble(0));

            // 验证必要字段
            if (food.getName() != null && !food.getName().isEmpty()) {
                return food;
            }
        } catch (Exception e) {
            System.err.println("解析单个食物JSON节点失败: " + e.getMessage());
        }
        return null;
    }

    // 从纯文本解析食物信息
    private List<RecognizedFood> parseFoodFromPlainText(String text) {
        List<RecognizedFood> foods = new ArrayList<>();

        // 多种正则表达式模式来匹配不同的文本格式

        // 模式1: "50克青瓜" 格式
        Pattern pattern1 = Pattern.compile("(\\d+(?:\\.\\d+)?)克(.*?)(?=\\d+克|$)");
        Matcher matcher1 = pattern1.matcher(text);

        while (matcher1.find()) {
            try {
                double weight = Double.parseDouble(matcher1.group(1));
                String foodName = matcher1.group(2).trim();

                if (!foodName.isEmpty()) {
                    RecognizedFood food = createRecognizedFood(foodName, weight);
                    if (food != null) {
                        foods.add(food);
                    }
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }

        // 如果第一种模式没有匹配到，尝试第二种模式
        if (foods.isEmpty()) {
            Pattern pattern2 = Pattern.compile("([^\\d]+)(\\d+(?:\\.\\d+)?)克");
            Matcher matcher2 = pattern2.matcher(text);

            while (matcher2.find()) {
                try {
                    String foodName = matcher2.group(1).trim();
                    double weight = Double.parseDouble(matcher2.group(2));

                    if (!foodName.isEmpty()) {
                        RecognizedFood food = createRecognizedFood(foodName, weight);
                        if (food != null) {
                            foods.add(food);
                        }
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        // 如果还是没有匹配到，尝试提取食物名称和默认重量
        if (foods.isEmpty()) {
            String[] commonFoods = { "青瓜", "黄瓜", "米饭", "面条", "鸡蛋", "鸡肉", "猪肉", "牛肉" };
            for (String foodName : commonFoods) {
                if (text.contains(foodName)) {
                    RecognizedFood food = createRecognizedFood(foodName, 100.0); // 默认100克
                    if (food != null) {
                        foods.add(food);
                    }
                    break;
                }
            }
        }

        return foods;
    }

    // 创建识别到的食物对象
    private RecognizedFood createRecognizedFood(String foodName, double weight) {
        RecognizedFood food = new RecognizedFood();
        food.setName(foodName);
        food.setWeightGrams(weight);
        setDefaultNutrition(food);
        return food;
    }

    // 设置默认营养成分
    private void setDefaultNutrition(RecognizedFood food) {
        String name = food.getName();
        double weight = food.getWeightGrams();

        // 简单的营养成分估算
        if (name.contains("米饭") || name.contains("米")) {
            food.setCalories(Math.round(weight * 1.3));
            food.setProtein(Math.round(weight * 0.027 * 10) / 10.0);
            food.setCarbs(Math.round(weight * 0.28 * 10) / 10.0);
            food.setFat(Math.round(weight * 0.003 * 10) / 10.0);
        } else if (name.contains("鸡") || name.contains("肉")) {
            food.setCalories(Math.round(weight * 1.65));
            food.setProtein(Math.round(weight * 0.19 * 10) / 10.0);
            food.setCarbs(0.0);
            food.setFat(Math.round(weight * 0.11 * 10) / 10.0);
        } else {
            // 默认值
            food.setCalories(Math.round(weight * 1.0));
            food.setProtein(Math.round(weight * 0.05 * 10) / 10.0);
            food.setCarbs(Math.round(weight * 0.1 * 10) / 10.0);
            food.setFat(Math.round(weight * 0.03 * 10) / 10.0);
        }
    }

    // 计算总营养成分
    private void calculateTotalNutrition(FoodRecognitionResult result) {
        TotalNutrition total = new TotalNutrition();
        double calories = 0, protein = 0, carbs = 0, fat = 0, fiber = 0;

        for (RecognizedFood food : result.getFoods()) {
            calories += food.getCalories();
            protein += food.getProtein();
            carbs += food.getCarbs();
            fat += food.getFat();
            fiber += food.getFiber();
        }

        total.setCalories(calories);
        total.setProtein(Math.round(protein * 10) / 10.0);
        total.setCarbs(Math.round(carbs * 10) / 10.0);
        total.setFat(Math.round(fat * 10) / 10.0);
        total.setFiber(Math.round(fiber * 10) / 10.0);

        result.setTotalNutrition(total);
    }
}