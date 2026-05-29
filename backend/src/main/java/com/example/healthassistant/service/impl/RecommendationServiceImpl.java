package com.example.healthassistant.service.impl;

import com.example.healthassistant.model.DietRecord;
import com.example.healthassistant.model.Recipe;
import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.repository.DietRecordRepository;
import com.example.healthassistant.repository.RecipeRepository;
import com.example.healthassistant.repository.UserProfileRepository;
import com.example.healthassistant.service.HealthAiService;
import com.example.healthassistant.service.RecommendationService;
import com.example.healthassistant.service.UserProfileLoadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private DietRecordRepository dietRecordRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private HealthAiService healthAiService;

    @Autowired
    private UserProfileLoadService userProfileLoadService;

    @Override
    public Map<String, Object> getRecipeRecommendations(String userId, String mealType) {
        // 1. 获取用户档案（含口味偏好，避免懒加载异常）
        UserProfile userProfile = userProfileLoadService.loadWithCollections(userId);
        if (userProfile == null) {
            throw new RuntimeException("未找到用户档案: " + userId);
        }

        // 2. 获取今日已摄入的营养
        List<DietRecord> todayRecords = dietRecordRepository.findByUserIdAndDate(
                userId,
                LocalDate.now()
        );

        // 3. 计算今日已摄入的总营养
        Map<String, Double> consumedNutrition = calculateConsumedNutrition(todayRecords);

        // 今日已吃描述（避免推荐重复菜品）
        List<String> todayFoods = todayRecords.stream()
                .map(DietRecord::getFoodDescription)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(12)
                .collect(Collectors.toList());

        // 用户近期已有食谱名（避免 AI 反复生成同名菜）
        List<String> recentRecipeNames = recipeRepository
                .findByCreatedByOrderByCreatedAtDesc(userId, PageRequest.of(0, 20))
                .getContent().stream()
                .map(Recipe::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return healthAiService.generateRecipeRecommendations(
                userProfile, consumedNutrition, mealType, todayFoods, recentRecipeNames);
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
