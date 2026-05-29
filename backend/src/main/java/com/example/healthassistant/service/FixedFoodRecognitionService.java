package com.example.healthassistant.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FixedFoodRecognitionService {

    /**
     * 修复后的食物识别服务 - 专门解决"米饭100克"这类输入的问题
     */
    public List<RecognizedFoodItem> recognizeFoodFixed(String userInput) {
        List<RecognizedFoodItem> results = new ArrayList<>();
        
        // 增强的食物数据库
        Map<String, FoodInfo> foodDatabase = createFixedFoodDatabase();
        
        // 使用正则表达式精确提取食物和重量信息
        List<FoodQuantity> extractedItems = extractFoodQuantities(userInput);
        
        for (FoodQuantity foodQty : extractedItems) {
            // 智能匹配食物
            FoodInfo matchedFood = findBestMatch(foodQty.foodName, foodDatabase);
            
            if (matchedFood != null) {
                double actualWeight = calculatePreciseWeight(foodQty, matchedFood.defaultWeight);
                results.add(new RecognizedFoodItem(
                    matchedFood.name,
                    actualWeight,
                    matchedFood.calories,
                    matchedFood.protein,
                    matchedFood.carbs,
                    matchedFood.fat,
                    matchedFood.fiber
                ));
            }
        }
        
        return results;
    }

    /**
     * 使用正则表达式精确提取食物名称和数量
     */
    private List<FoodQuantity> extractFoodQuantities(String input) {
        List<FoodQuantity> results = new ArrayList<>();
        
        if (input == null || input.trim().isEmpty()) {
            return results;
        }
        
        System.out.println("=== 食物识别输入分析 ===");
        System.out.println("原始输入: " + input);
        
        // 处理多种输入格式
        String cleanInput = input.trim();
        
        // 模式1: "米饭100克" 或 "米饭 100 克"
        Pattern pattern1 = Pattern.compile("(\\p{IsHan}+)[\\s]*(\\d+(?:\\.\\d+)?)\\s*(克|g|kg)");
        Matcher matcher1 = pattern1.matcher(cleanInput);
        
        // 模式2: "100克米饭" 或 "100 克 米饭"
        Pattern pattern2 = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(克|g|kg)[\\s]*(\\p{IsHan}+)");
        Matcher matcher2 = pattern2.matcher(cleanInput);
        
        // 模式3: "一碗米饭" 或 "一份米饭" (量词模式)
        Pattern pattern3 = Pattern.compile("(一[\\u4e00-\\u9fa5]*|\\d+[\\u4e00-\\u9fa5]*)[\\s]*(\\p{IsHan}+)");
        Matcher matcher3 = pattern3.matcher(cleanInput);
        
        // 模式4: 纯食物名称，如"米饭"
        Pattern pattern4 = Pattern.compile("(\\p{IsHan}+)");
        Matcher matcher4 = pattern4.matcher(cleanInput);
        
        boolean found = false;
        
        // 优先匹配模式1
        while (matcher1.find()) {
            String foodName = matcher1.group(1).trim();
            double quantity = Double.parseDouble(matcher1.group(2));
            String unit = matcher1.group(3);
            
            System.out.println("模式1匹配: " + foodName + " - " + quantity + unit);
            
            // 转换单位
            if ("kg".equalsIgnoreCase(unit)) {
                quantity *= 1000;
            }
            
            results.add(new FoodQuantity(foodName, quantity));
            found = true;
        }
        
        // 如果模式1没匹配到，尝试模式2
        if (!found) {
            while (matcher2.find()) {
                double quantity = Double.parseDouble(matcher2.group(1));
                String unit = matcher2.group(2);
                String foodName = matcher2.group(3).trim();
                
                System.out.println("模式2匹配: " + foodName + " - " + quantity + unit);
                
                // 转换单位
                if ("kg".equalsIgnoreCase(unit)) {
                    quantity *= 1000;
                }
                
                results.add(new FoodQuantity(foodName, quantity));
                found = true;
            }
        }
        
        // 如果模式1、2都没匹配到，尝试模式3（量词模式）
        if (!found) {
            while (matcher3.find()) {
                String quantityDesc = matcher3.group(1).trim();
                String foodName = matcher3.group(2).trim();
                
                System.out.println("模式3匹配: " + foodName + " - " + quantityDesc);
                
                double estimatedWeight = estimateWeightFromQuantity(quantityDesc, foodName);
                results.add(new FoodQuantity(foodName, estimatedWeight));
                found = true;
            }
        }
        
        // 如果都没匹配到，尝试简单匹配纯食物名称
        if (!found) {
            // 移除数字和单位，只保留中文字符
            String chineseOnly = cleanInput.replaceAll("[^\\p{IsHan}]", "").trim();
            if (!chineseOnly.isEmpty()) {
                System.out.println("模式4匹配: " + chineseOnly + " - 默认100克");
                results.add(new FoodQuantity(chineseOnly, 100.0)); // 默认100克
                found = true;
            }
        }
        
        System.out.println("提取到的食物项数量: " + results.size());
        for (FoodQuantity item : results) {
            System.out.println("  - " + item.foodName + ": " + item.quantity + "克");
        }
        System.out.println("=====================");
        
        return results;
    }

    /**
     * 创建修复后的食物数据库
     */
    private Map<String, FoodInfo> createFixedFoodDatabase() {
        Map<String, FoodInfo> db = new HashMap<>();
        
        // 谷物类 - 确保包含"米饭"
        db.put("米饭", new FoodInfo("大米", 100, 130, 2.7, 28.0, 0.3, 0.4));
        db.put("大米", new FoodInfo("大米", 100, 130, 2.7, 28.0, 0.3, 0.4));
        db.put("粥", new FoodInfo("白粥", 150, 46, 1.2, 10.0, 0.1, 0.3));
        db.put("面条", new FoodInfo("面条", 100, 110, 3.7, 23.0, 0.6, 1.2));
        db.put("米粉", new FoodInfo("米粉", 100, 132, 2.2, 29.0, 0.2, 0.8));
        db.put("馒头", new FoodInfo("馒头", 100, 223, 7.0, 46.0, 1.0, 2.0));
        db.put("包子", new FoodInfo("包子", 100, 200, 8.0, 35.0, 5.0, 1.5));
        
        // 肉类
        db.put("鸡肉", new FoodInfo("鸡胸肉", 100, 110, 22.5, 0.0, 1.2, 0.0));
        db.put("牛肉", new FoodInfo("瘦牛肉", 100, 250, 26.0, 0.0, 15.0, 0.0));
        db.put("猪肉", new FoodInfo("瘦猪肉", 100, 143, 20.3, 0.0, 6.2, 0.0));
        db.put("鱼肉", new FoodInfo("鲈鱼", 100, 89, 18.8, 0.0, 1.2, 0.0));
        db.put("鸡蛋", new FoodInfo("鸡蛋", 60, 155, 13.0, 1.1, 11.0, 0.0));
        
        // 蔬菜类
        db.put("西兰花", new FoodInfo("西兰花", 100, 35, 2.8, 7.0, 0.4, 2.6));
        db.put("白菜", new FoodInfo("白菜", 100, 17, 1.5, 3.1, 0.1, 0.8));
        db.put("胡萝卜", new FoodInfo("胡萝卜", 100, 41, 0.9, 10.0, 0.2, 2.8));
        db.put("菠菜", new FoodInfo("菠菜", 100, 23, 2.9, 3.6, 0.4, 2.2));
        db.put("青菜", new FoodInfo("青菜", 100, 15, 1.5, 2.6, 0.3, 1.4));
        
        // 豆制品
        db.put("豆腐", new FoodInfo("嫩豆腐", 100, 76, 8.1, 1.9, 4.8, 0.5));
        db.put("豆浆", new FoodInfo("豆浆", 250, 30, 3.0, 1.2, 1.6, 0.4));
        
        // 汤粉面类
        db.put("肠粉", new FoodInfo("肠粉", 150, 110, 3.2, 22.0, 1.1, 0.6));
        db.put("小笼包", new FoodInfo("小笼包", 100, 230, 9.5, 32.0, 7.0, 1.2));
        db.put("馄饨", new FoodInfo("馄饨", 150, 180, 7.5, 25.0, 6.0, 0.8));
        
        return db;
    }

    /**
     * 改进的食物匹配算法
     */
    private FoodInfo findBestMatch(String input, Map<String, FoodInfo> database) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        String cleanInput = input.trim();
        
        // 1. 精确匹配
        if (database.containsKey(cleanInput)) {
            return database.get(cleanInput);
        }
        
        // 2. 部分匹配（输入包含数据库键名）
        for (Map.Entry<String, FoodInfo> entry : database.entrySet()) {
            if (cleanInput.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 3. 反向匹配（数据库键名包含输入）
        for (Map.Entry<String, FoodInfo> entry : database.entrySet()) {
            if (entry.getKey().contains(cleanInput)) {
                return entry.getValue();
            }
        }
        
        // 4. 关键字匹配
        String[] keywords = {"米", "面", "肉", "菜", "蛋", "豆", "粥", "粉"};
        for (String keyword : keywords) {
            if (cleanInput.contains(keyword)) {
                for (Map.Entry<String, FoodInfo> entry : database.entrySet()) {
                    if (entry.getKey().contains(keyword)) {
                        return entry.getValue();
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * 精确计算重量
     */
    private double calculatePreciseWeight(FoodQuantity foodQty, double defaultWeight) {
        // 如果提取到了具体的重量，使用提取的重量
        if (foodQty.quantity > 0) {
            return foodQty.quantity;
        }
        // 否则使用默认重量
        return defaultWeight;
    }

    // 内部类定义
    public static class RecognizedFoodItem {
        private String name;
        private double weightGrams;
        private double caloriesPer100g;
        private double proteinPer100g;
        private double carbsPer100g;
        private double fatPer100g;
        private Double fiberPer100g;

        public RecognizedFoodItem(String name, double weightGrams, double caloriesPer100g,
                                double proteinPer100g, double carbsPer100g, double fatPer100g, Double fiberPer100g) {
            this.name = name;
            this.weightGrams = weightGrams;
            this.caloriesPer100g = caloriesPer100g;
            this.proteinPer100g = proteinPer100g;
            this.carbsPer100g = carbsPer100g;
            this.fatPer100g = fatPer100g;
            this.fiberPer100g = fiberPer100g;
        }

        // Getters
        public String getName() { return name; }
        public double getWeightGrams() { return weightGrams; }
        public double getCaloriesPer100g() { return caloriesPer100g; }
        public double getProteinPer100g() { return proteinPer100g; }
        public double getCarbsPer100g() { return carbsPer100g; }
        public double getFatPer100g() { return fatPer100g; }
        public Double getFiberPer100g() { return fiberPer100g; }
    }

    public static class NutritionEstimate {
        private double calories;
        private double protein;
        private double carbs;
        private double fat;
        private double fiber;

        public NutritionEstimate(double calories, double protein, double carbs, double fat, double fiber) {
            this.calories = calories;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
            this.fiber = fiber;
        }

        public double getCalories() { return Math.round(calories * 10) / 10.0; }
        public double getProtein() { return Math.round(protein * 10) / 10.0; }
        public double getCarbs() { return Math.round(carbs * 10) / 10.0; }
        public double getFat() { return Math.round(fat * 10) / 10.0; }
        public double getFiber() { return Math.round(fiber * 10) / 10.0; }
    }

    // 辅助类
    private static class FoodInfo {
        String name;
        double defaultWeight;
        double calories;
        double protein;
        double carbs;
        double fat;
        double fiber;

        FoodInfo(String name, double defaultWeight, double calories, double protein, 
                double carbs, double fat, double fiber) {
            this.name = name;
            this.defaultWeight = defaultWeight;
            this.calories = calories;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
            this.fiber = fiber;
        }
    }

    private static class FoodQuantity {
        String foodName;
        double quantity;

        FoodQuantity(String foodName, double quantity) {
            this.foodName = foodName;
            this.quantity = quantity;
        }
    }

    /**
     * 根据量词估算重量
     */
    private double estimateWeightFromQuantity(String quantityStr, String foodName) {
        // 处理"一碗"、"一份"等量词
        if (quantityStr.startsWith("一")) {
            switch (quantityStr) {
                case "一碗":
                case "一份":
                    return estimateBaseWeight(foodName);
                case "一杯":
                    return 250.0; // 杯装大约250ml
                case "一瓶":
                    return 500.0; // 瓶装大约500ml
                case "一个":
                case "一只":
                    return estimateBaseWeight(foodName) * 0.8;
                default:
                    return estimateBaseWeight(foodName);
            }
        } else {
            // 处理数字+量词，如"2碗"、"3个"
            try {
                int count = Integer.parseInt(quantityStr.replaceAll("[^0-9]", ""));
                return estimateBaseWeight(foodName) * count;
            } catch (NumberFormatException e) {
                return estimateBaseWeight(foodName);
            }
        }
    }

    /**
     * 获取食物的基础重量估算
     */
    private double estimateBaseWeight(String foodName) {
        Map<String, Double> baseWeights = new HashMap<>();
        baseWeights.put("米饭", 150.0);
        baseWeights.put("粥", 200.0);
        baseWeights.put("面条", 100.0);
        baseWeights.put("馒头", 80.0);
        baseWeights.put("包子", 100.0);
        baseWeights.put("鸡蛋", 60.0);
        baseWeights.put("牛奶", 250.0);
        baseWeights.put("苹果", 150.0);
        baseWeights.put("香蕉", 120.0);
        baseWeights.put("鸡肉", 100.0);
        baseWeights.put("猪肉", 100.0);
        baseWeights.put("牛肉", 100.0);
        baseWeights.put("鱼肉", 100.0);
        baseWeights.put("豆腐", 100.0);
        baseWeights.put("西兰花", 80.0);
        baseWeights.put("白菜", 80.0);
        
        return baseWeights.getOrDefault(foodName, 100.0);
    }
}