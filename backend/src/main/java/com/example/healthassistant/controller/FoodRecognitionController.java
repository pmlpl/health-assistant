package com.example.healthassistant.controller;

import com.example.healthassistant.config.ApiKeyResolver;
import com.example.healthassistant.dto.ApiResponse;
import com.example.healthassistant.service.DoubaoFoodRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food")
public class FoodRecognitionController {

    @Autowired
    private DoubaoFoodRecognitionService doubaoFoodService;

    @Autowired
    private ApiKeyResolver apiKeyResolver;

    /** 食物识别（统一入口，原 recognize-with-doubao 已合并至此） */
    @PostMapping("/recognize")
    public ApiResponse<Map<String, Object>> recognizeFood(@RequestBody Map<String, String> request) {
        return doRecognize(request.get("foodDescription"));
    }

    /** @deprecated 请使用 POST /api/food/recognize */
    @Deprecated
    @PostMapping("/recognize-with-doubao")
    public ApiResponse<Map<String, Object>> recognizeFoodWithDoubao(@RequestBody Map<String, String> request) {
        return doRecognize(request.get("foodDescription"));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getApiStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean configured = apiKeyResolver.isDoubaoConfigured();
        status.put("doubaoConfigured", configured);
        status.put("mode", configured ? "智能识别模式" : "本地识别模式");
        status.put("message", configured
                ? "已配置 AI 识食，享受智能食物识别"
                : "未配置 AI API 密钥，使用本地识别");
        if (!configured) {
            status.put("setupInstructions", "请在 backend/.env 中设置 DOUBAO_API_KEY");
        }
        return ApiResponse.success(status);
    }

    private ApiResponse<Map<String, Object>> doRecognize(String foodDescription) {
        if (foodDescription == null || foodDescription.trim().isEmpty()) {
            return ApiResponse.badRequest("食物描述不能为空");
        }
        try {
            List<DoubaoFoodRecognitionService.FoodNutrition> results =
                    doubaoFoodService.recognizeFoodWithDoubao(foodDescription);

            Map<String, Object> response = new HashMap<>();
            response.put("input", foodDescription);
            response.put("foods", results);
            response.put("count", results.size());
            response.put("totalNutrition", sumNutrition(results));
            response.put("mode", apiKeyResolver.isDoubaoConfigured() ? "智能识别模式" : "本地识别模式");
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("食物识别失败，请稍后重试");
        }
    }

    private Map<String, Double> sumNutrition(List<DoubaoFoodRecognitionService.FoodNutrition> results) {
        double totalCalories = 0, totalProtein = 0, totalCarbs = 0, totalFat = 0, totalFiber = 0;
        for (DoubaoFoodRecognitionService.FoodNutrition food : results) {
            totalCalories += food.getActualCalories();
            totalProtein += food.getActualProtein();
            totalCarbs += food.getActualCarbs();
            totalFat += food.getActualFat();
            totalFiber += food.getActualFiber();
        }
        Map<String, Double> totals = new HashMap<>();
        totals.put("calories", round2(totalCalories));
        totals.put("protein", round2(totalProtein));
        totals.put("carbs", round2(totalCarbs));
        totals.put("fat", round2(totalFat));
        totals.put("fiber", round2(totalFiber));
        return totals;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
