package com.example.healthassistant.controller;

import com.example.healthassistant.security.AuthSupport;
import com.example.healthassistant.service.QwenAIService;
import com.example.healthassistant.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private QwenAIService qwenAIService;

    @Autowired
    private RecommendationService recommendationService;

    @PostMapping("/nutrition-advice")
    public ResponseEntity<Map<String, Object>> getNutritionAdvice(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String query = (String) request.get("query");

            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "用户ID不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            AuthSupport.requireSelf(userId);

            if (query == null || query.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "查询内容不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String advice = qwenAIService.getNutritionAdvice(userId, query);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("query", query);
            response.put("advice", advice);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "处理请求时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/recommend-recipes")
    public ResponseEntity<Map<String, Object>> recommendRecipes(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String mealType = request.get("mealType");
        AuthSupport.requireSelf(userId);
        try {
            Map<String, Object> recommendations = recommendationService.getRecipeRecommendations(userId, mealType);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "食谱推荐失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/session/{userId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String userId) {
        AuthSupport.requireSelf(userId);
        try {
            qwenAIService.clearSessionHistory(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "用户 " + userId + " 的会话历史已清除");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "清除会话时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/session/history/{userId}")
    public ResponseEntity<Map<String, Object>> getSessionHistory(@PathVariable String userId) {
        AuthSupport.requireSelf(userId);
        try {
            int historyLength = qwenAIService.getSessionHistoryLength(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("historyLength", historyLength);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取会话历史时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/analyze-fitness-workout/{userId}")
    public ResponseEntity<Map<String, Object>> analyzeFitnessWorkout(
            @PathVariable String userId,
            @RequestBody Map<String, Object> workoutData) {
        AuthSupport.requireSelf(userId);
        try {
            String analysis = qwenAIService.analyzeFitnessWorkout(userId, workoutData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysis", analysis);
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "健身分析失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/mental-health")
    public ResponseEntity<Map<String, Object>> getMentalHealthAdvice(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String message = (String) request.get("message");

            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "用户ID不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            AuthSupport.requireSelf(userId);

            if (message == null || message.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "消息内容不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String response = qwenAIService.getMentalHealthAdvice(userId, message);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", true);
            responseMap.put("response", response);
            responseMap.put("userId", userId);

            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "心理健康咨询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
