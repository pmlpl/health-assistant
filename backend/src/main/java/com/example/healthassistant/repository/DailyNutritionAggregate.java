package com.example.healthassistant.repository;

import java.time.LocalDate;

/** 月度饮食统计 SQL 聚合投影 */
public interface DailyNutritionAggregate {
    LocalDate getDate();
    Double getTotalCalories();
    Double getTotalProtein();
    Double getTotalCarbs();
    Double getTotalFat();
    Long getRecordCount();
}
