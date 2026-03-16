package com.example.healthassistant.service.impl;

import com.example.healthassistant.model.DietRecord;
import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.repository.DietRecordRepository;
import com.example.healthassistant.repository.UserProfileRepository;
import com.example.healthassistant.service.QwenAIService;
import com.example.healthassistant.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private DietRecordRepository dietRecordRepository;

    @Autowired
    private QwenAIService qwenAIService;

    @Override
    public Map<String, Object> getRecipeRecommendations(String userId, String mealType) {
        // 1. 获取用户档案
        UserProfile userProfile = userProfileRepository.findByUserIdWithDietaryRestrictions(userId);
        if (userProfile == null) {
            throw new RuntimeException("未找到用户档案: " + userId);
        }

        // 2. 获取今日已摄入的营养
        List<DietRecord> todayRecords = dietRecordRepository.findByUserIdAndRecordedAtBetween(
                userId,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay()
        );

        // 3. 计算今日已摄入的总营养
        Map<String, Double> consumedNutrition = calculateConsumedNutrition(todayRecords);

        // 4. 调用AI服务获取推荐
        return qwenAIService.generateRecipeRecommendations(userProfile, consumedNutrition, mealType);
    }

    private Map<String, Double> calculateConsumedNutrition(List<DietRecord> records) {
        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;

        for (DietRecord record : records) {
            if (record.getCalories() != null) totalCalories += record.getCalories();
            if (record.getProtein() != null) totalProtein += record.getProtein();
            if (record.getCarbs() != null) totalCarbs += record.getCarbs();
            if (record.getFat() != null) totalFat += record.getFat();
        }

        return Map.of(
                "calories", totalCalories,
                "protein", totalProtein,
                "carbs", totalCarbs,
                "fat", totalFat
        );
    }
}
