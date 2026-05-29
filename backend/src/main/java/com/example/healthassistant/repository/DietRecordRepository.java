package com.example.healthassistant.repository;

import com.example.healthassistant.model.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {
    List<DietRecord> findByUserIdAndRecordedAtBetween(String userId, LocalDateTime start, LocalDateTime end);
    List<DietRecord> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);
    
    // 基础查询方法
    List<DietRecord> findByUserIdAndDate(String userId, LocalDate date);
    
    // === 性能优化：使用 JOIN FETCH 避免 N+1 问题 ===
    
    /**
     * 优化查询：一次性加载饮食记录和食材成分
     * 使用 LEFT JOIN FETCH 避免 N+1 查询问题
     */
    @Query("SELECT dr FROM DietRecord dr LEFT JOIN FETCH dr.consumedIngredients WHERE dr.userId = :userId AND dr.date = :date")
    List<DietRecord> findByUserIdAndDateWithIngredients(@Param("userId") String userId, @Param("date") LocalDate date);
    
    /**
     * 优化查询：批量加载日期范围内的饮食记录和食材成分
     */
    @Query("SELECT dr FROM DietRecord dr LEFT JOIN FETCH dr.consumedIngredients WHERE dr.userId = :userId AND dr.date BETWEEN :startDate AND :endDate")
    List<DietRecord> findByUserIdAndDateBetweenWithIngredients(
        @Param("userId") String userId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
}
