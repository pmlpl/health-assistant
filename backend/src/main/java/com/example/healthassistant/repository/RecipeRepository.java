package com.example.healthassistant.repository;

import com.example.healthassistant.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    
    // 基础查询方法
    List<Recipe> findByTagsContaining(String tag);
    List<Recipe> findByMealType(String mealType);
    
    // === 性能优化：使用 JOIN FETCH 避免 N+1 问题 ===
    
    /**
     * 优化查询：一次性加载食谱和标签信息
     * 使用 LEFT JOIN FETCH 避免 N+1 查询问题
     */
    @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.tags")
    List<Recipe> findAllWithTags();
    
    /**
     * 优化查询：按餐别加载食谱并包含标签
     */
    @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.tags WHERE r.mealType = :mealType")
    List<Recipe> findByMealTypeWithTags(@Param("mealType") String mealType);
}
