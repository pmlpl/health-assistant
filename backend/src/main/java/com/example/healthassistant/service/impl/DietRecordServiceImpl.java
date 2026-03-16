package com.example.healthassistant.service.impl;

import com.example.healthassistant.dto.DietRecordDto;
import com.example.healthassistant.model.DietRecord;
import com.example.healthassistant.model.Ingredient;
import com.example.healthassistant.model.MealType;
import com.example.healthassistant.repository.DietRecordRepository;
import com.example.healthassistant.repository.IngredientRepository;
import com.example.healthassistant.service.DietRecordService;
import com.example.healthassistant.service.DoubaoFoodRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class DietRecordServiceImpl implements DietRecordService {
    
    @Autowired
    private DietRecordRepository dietRecordRepository;
    
    @Autowired
    private IngredientRepository ingredientRepository;
    
    @Autowired
    private DoubaoFoodRecognitionService doubaoFoodService;
    
    @Override
    public DietRecord saveDietRecord(DietRecordDto dietRecordDto) {
        DietRecord record = new DietRecord();
        record.setUserId(dietRecordDto.getUserId());
        record.setMealType(MealType.valueOf(dietRecordDto.getMealType().toUpperCase()));
        record.setFoodDescription(dietRecordDto.getFoodDescription());
        record.setConsumedIngredients(dietRecordDto.getConsumedIngredients());
        record.setDate(LocalDate.now());
        record.setRecordedAt(LocalDateTime.now());
        
        // 如果DTO中直接提供了营养成分，则使用提供的值
        if (dietRecordDto.getCalories() != null) {
            record.setCalories(dietRecordDto.getCalories());
            record.setProtein(dietRecordDto.getProtein());
            record.setCarbs(dietRecordDto.getCarbs());
            record.setFat(dietRecordDto.getFat());
            record.setFiber(dietRecordDto.getFiber());
        } else {
            // 使用豆包AI食物识别服务进行智能计算
            calculateNutritionIntelligently(record);
        }
        
        return dietRecordRepository.save(record);
    }
    
    /**
     * 智能计算营养成分 - 使用豆包AI识别服务
     */
    private void calculateNutritionIntelligently(DietRecord record) {
        // 使用食物描述进行智能识别
        if (record.getFoodDescription() != null && !record.getFoodDescription().trim().isEmpty()) {
            List<DoubaoFoodRecognitionService.FoodNutrition> results = 
                doubaoFoodService.recognizeFoodWithDoubao(record.getFoodDescription());
            
            // 计算总营养成分
            double totalCalories = 0;
            double totalProtein = 0;
            double totalCarbs = 0;
            double totalFat = 0;
            double totalFiber = 0;
            
            List<String> ingredientNames = new ArrayList<>();
            
            for (DoubaoFoodRecognitionService.FoodNutrition food : results) {
                totalCalories += food.getActualCalories();
                totalProtein += food.getActualProtein();
                totalCarbs += food.getActualCarbs();
                totalFat += food.getActualFat();
                totalFiber += food.getActualFiber();
                ingredientNames.add(food.getName());
            }
            
            // 设置计算得到的营养成分
            record.setCalories(totalCalories);
            record.setProtein(totalProtein);
            record.setCarbs(totalCarbs);
            record.setFat(totalFat);
            record.setFiber(totalFiber);
            
            // 更新消费的食材列表
            if (!ingredientNames.isEmpty()) {
                record.setConsumedIngredients(ingredientNames);
            }
            
            System.out.println("豆包AI识别完成 - 输入: " + record.getFoodDescription() + 
                             ", 识别到 " + results.size() + " 种食物" + 
                             ", 总热量: " + String.format("%.1f", totalCalories) + "kcal");
        } else if (record.getConsumedIngredients() != null) {
            // 如果没有食物描述但有食材列表，使用传统方式计算
            calculateNutritionTraditional(record);
        } else {
            // 都没有的话设置默认值
            record.setCalories(0.0);
            record.setProtein(0.0);
            record.setCarbs(0.0);
            record.setFat(0.0);
            record.setFiber(0.0);
        }
    }
    
    /**
     * 传统的营养成分计算方式
     */
    private void calculateNutritionTraditional(DietRecord record) {
        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;
        double totalFiber = 0;
        
        if (record.getConsumedIngredients() != null) {
            for (String ingredientName : record.getConsumedIngredients()) {
                Ingredient ingredient = ingredientRepository.findByName(ingredientName);
                if (ingredient != null) {
                    // 假设每种食材按100g计算
                    totalCalories += ingredient.getCaloriesPer100g();
                    totalProtein += ingredient.getProteinPer100g();
                    totalCarbs += ingredient.getCarbsPer100g();
                    totalFat += ingredient.getFatPer100g();
                    if (ingredient.getFiberPer100g() != null) {
                        totalFiber += ingredient.getFiberPer100g();
                    }
                }
            }
        }
        
        record.setCalories(totalCalories);
        record.setProtein(totalProtein);
        record.setCarbs(totalCarbs);
        record.setFat(totalFat);
        record.setFiber(totalFiber);
    }
    
    @Override
    public List<DietRecord> getDailyRecords(String userId, LocalDate date) {
        // 使用优化后的查询方法，避免 N+1 问题
        return dietRecordRepository.findByUserIdAndDateWithIngredients(userId, date);
    }
    
    @Override
    public List<DietRecord> getWeeklyRecords(String userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        // 使用优化后的查询方法，批量加载食材成分
        return dietRecordRepository.findByUserIdAndDateBetweenWithIngredients(userId, weekStart, weekEnd);
    }
    
    @Override
    public List<DietRecord> getRecordsInRange(String userId, LocalDate startDate, LocalDate endDate) {
        // 使用优化后的查询方法，批量加载食材成分
        return dietRecordRepository.findByUserIdAndDateBetweenWithIngredients(userId, startDate, endDate);
    }
    
    @Override
    public Map<String, Object> getMonthlySummary(String userId, int year, int month) {
        Map<String, Object> summary = new HashMap<>();
        
        // 获取月份的第一天和最后一天
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        // 获取该月的所有记录（使用优化查询）
        List<DietRecord> monthlyRecords = dietRecordRepository.findByUserIdAndDateBetweenWithIngredients(userId, startDate, endDate);
        
        // 过滤掉喝水打卡记录
        List<DietRecord> filteredRecords = monthlyRecords.stream()
            .filter(record -> {
                String desc = record.getFoodDescription();
                return desc == null || 
                       (!desc.contains("💧 喝水打卡") && 
                        !desc.toLowerCase().contains("water打卡"));
            })
            .collect(java.util.stream.Collectors.toList());
        
        // 按日期分组统计
        Map<LocalDate, DailySummary> dailySummaries = new HashMap<>();
        
        for (DietRecord record : filteredRecords) {
            LocalDate date = record.getDate();
            DailySummary dailySummary = dailySummaries.computeIfAbsent(date, k -> new DailySummary());
            
            if (record.getCalories() != null) dailySummary.totalCalories += record.getCalories();
            if (record.getProtein() != null) dailySummary.totalProtein += record.getProtein();
            if (record.getCarbs() != null) dailySummary.totalCarbs += record.getCarbs();
            if (record.getFat() != null) dailySummary.totalFat += record.getFat();
            dailySummary.recordCount++;
        }
        
        // 转换为返回格式
        List<Map<String, Object>> dailyData = new ArrayList<>();
        for (Map.Entry<LocalDate, DailySummary> entry : dailySummaries.entrySet()) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", entry.getKey().toString());
            dayData.put("calories", Math.round(entry.getValue().totalCalories * 100.0) / 100.0);
            dayData.put("protein", Math.round(entry.getValue().totalProtein * 100.0) / 100.0);
            dayData.put("carbs", Math.round(entry.getValue().totalCarbs * 100.0) / 100.0);
            dayData.put("fat", Math.round(entry.getValue().totalFat * 100.0) / 100.0);
            dayData.put("recordCount", entry.getValue().recordCount);
            dailyData.add(dayData);
        }
        
        summary.put("dailyData", dailyData);
        summary.put("month", month);
        summary.put("year", year);
        summary.put("totalDays", dailyData.size());
        
        return summary;
    }
    
    // 内部类用于每日汇总
    private static class DailySummary {
        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;
        int recordCount = 0;
    }
    
    @Override
    public void deleteDietRecord(Long id) {
        dietRecordRepository.deleteById(id);
    }
    
    @Override
    public int deleteBatchDietRecords(List<Long> ids) {
        int deletedCount = 0;
        for (Long id : ids) {
            try {
                dietRecordRepository.deleteById(id);
                deletedCount++;
            } catch (Exception e) {
                // 记录删除失败的日志，但继续处理其他记录
                System.err.println("删除记录ID " + id + " 失败: " + e.getMessage());
            }
        }
        return deletedCount;
    }
}