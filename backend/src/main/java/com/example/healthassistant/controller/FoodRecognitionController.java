package com.example.healthassistant.controller;

import com.example.healthassistant.dto.ApiResponse;
import com.example.healthassistant.service.DoubaoFoodRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food")
@CrossOrigin(origins = "*")
public class FoodRecognitionController {

    @Autowired
    private DoubaoFoodRecognitionService doubaoFoodService;

    /**
     * 食物识别 API - 使用豆包 AI
     */
    @PostMapping("/recognize")
    public ApiResponse<Map<String, Object>> recognizeFood(@RequestBody Map<String, String> request) {
        String foodDescription = request.get("foodDescription");
        
        if (foodDescription == null || foodDescription.trim().isEmpty()) {
            return ApiResponse.error("食物描述不能为空");
        }
        
        try {
            List<DoubaoFoodRecognitionService.FoodNutrition> results = doubaoFoodService.recognizeFoodWithDoubao(foodDescription);
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", foodDescription);
            response.put("foods", results);
            response.put("count", results.size());
            
            // 计算总营养成分
            double totalCalories = 0;
            double totalProtein = 0;
            double totalCarbs = 0;
            double totalFat = 0;
            double totalFiber = 0;
            
            for (DoubaoFoodRecognitionService.FoodNutrition food : results) {
                totalCalories += food.getActualCalories();
                totalProtein += food.getActualProtein();
                totalCarbs += food.getActualCarbs();
                totalFat += food.getActualFat();
                totalFiber += food.getActualFiber();
            }
            
            Map<String, Double> totals = new HashMap<>();
            totals.put("calories", Math.round(totalCalories * 100.0) / 100.0);
            totals.put("protein", Math.round(totalProtein * 100.0) / 100.0);
            totals.put("carbs", Math.round(totalCarbs * 100.0) / 100.0);
            totals.put("fat", Math.round(totalFat * 100.0) / 100.0);
            totals.put("fiber", Math.round(totalFiber * 100.0) / 100.0);
            
            response.put("totalNutrition", totals);
            response.put("mode", isDoubaoConfigured() ? "豆包AI模式" : "本地识别模式");
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("食物识别失败: " + e.getMessage());
        }
    }
    
    /**
     * 专门的豆包AI识别接口
     */
    @PostMapping("/recognize-with-doubao")
    public ApiResponse<Map<String, Object>> recognizeFoodWithDoubao(@RequestBody Map<String, String> request) {
        String foodDescription = request.get("foodDescription");
        
        if (foodDescription == null || foodDescription.trim().isEmpty()) {
            return ApiResponse.error("食物描述不能为空");
        }
        
        try {
            List<DoubaoFoodRecognitionService.FoodNutrition> results = doubaoFoodService.recognizeFoodWithDoubao(foodDescription);
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", foodDescription);
            response.put("foods", results);
            response.put("count", results.size());
            
            // 计算总营养成分
            double totalCalories = 0;
            double totalProtein = 0;
            double totalCarbs = 0;
            double totalFat = 0;
            double totalFiber = 0;
            
            for (DoubaoFoodRecognitionService.FoodNutrition food : results) {
                totalCalories += food.getActualCalories();
                totalProtein += food.getActualProtein();
                totalCarbs += food.getActualCarbs();
                totalFat += food.getActualFat();
                totalFiber += food.getActualFiber();
            }
            
            Map<String, Double> totals = new HashMap<>();
            totals.put("calories", Math.round(totalCalories * 100.0) / 100.0);
            totals.put("protein", Math.round(totalProtein * 100.0) / 100.0);
            totals.put("carbs", Math.round(totalCarbs * 100.0) / 100.0);
            totals.put("fat", Math.round(totalFat * 100.0) / 100.0);
            totals.put("fiber", Math.round(totalFiber * 100.0) / 100.0);
            
            response.put("totalNutrition", totals);
            response.put("mode", "豆包AI");
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("豆包AI识别失败: " + e.getMessage());
        }
    }

    /**
     * 检查API配置状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getApiStatus() {
        Map<String, Object> status = new HashMap<>();
        
        boolean doubaoConfigured = isDoubaoConfigured();
        status.put("doubaoConfigured", doubaoConfigured);
        status.put("mode", doubaoConfigured ? "豆包AI模式" : "本地识别模式");
        
        if (doubaoConfigured) {
            status.put("message", "✅ 已配置豆包AI，享受智能食物识别");
        } else {
            status.put("message", "⚠️ 未配置豆包API密钥，使用本地识别");
            status.put("setupInstructions", getDoubaoSetupInstructions());
        }
        
        return ApiResponse.success(status);
    }

    /**
     * 检查豆包API是否已配置
     */
    private boolean isDoubaoConfigured() {
        try {
            String apiKey = System.getenv("DOUBAO_API_KEY");
            return apiKey != null && !apiKey.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取豆包API配置说明
     */
    private String getDoubaoSetupInstructions() {
        return "请设置环境变量 DOUBAO_API_KEY 为您的豆包API密钥来启用智能识别功能";
    }

}