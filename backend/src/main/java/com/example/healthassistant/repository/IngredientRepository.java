package com.example.healthassistant.repository;

import com.example.healthassistant.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Ingredient findByName(String name);
    
    // 根据类别查找食材
    List<Ingredient> findByCategory(String category);
    
    // 模糊搜索食材名称
    @Query("SELECT i FROM Ingredient i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<Ingredient> findByNameContainingIgnoreCase(String namePart);
    
    // 根据营养成分范围查找
    @Query("SELECT i FROM Ingredient i WHERE i.proteinPer100g >= ?1 AND i.proteinPer100g <= ?2")
    List<Ingredient> findByProteinRange(double minProtein, double maxProtein);
    
    @Query("SELECT i FROM Ingredient i WHERE i.carbsPer100g >= ?1 AND i.carbsPer100g <= ?2")
    List<Ingredient> findByCarbsRange(double minCarbs, double maxCarbs);
    
    @Query("SELECT i FROM Ingredient i WHERE i.fiberPer100g >= ?1")
    List<Ingredient> findByMinFiber(double minFiber);
}
