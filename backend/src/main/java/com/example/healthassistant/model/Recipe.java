package com.example.healthassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "recipe", indexes = {
    @Index(name = "idx_name", columnList = "name"),
    @Index(name = "idx_meal_type", columnList = "meal_type"),
    @Index(name = "idx_cuisine_type", columnList = "cuisine_type"),
    @Index(name = "idx_difficulty", columnList = "difficulty"),
    @Index(name = "idx_calories", columnList = "calories"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name; // 食谱名称

    @Column(length = 500)
    private String description; // 描述

    @Column(name = "ingredients_list", length = 2000)
    private String ingredientsList; // 食材列表（JSON格式存储详细信息）

    @Column(length = 2000)
    private String instructions; // 制作步骤

    @Column(name = "meal_type", length = 20)
    private String mealType; // 餐别：breakfast/lunch/dinner/snack

    @Column(name = "cuisine_type", length = 50)
    private String cuisineType; // 菜系类型：quick/home_style/restaurant_recommendation

    @Column(name = "cooking_time")
    private Integer cookingTime; // 烹饪时间（分钟）

    @Column(name = "calories")
    private Double calories; // 卡路里

    @Column(name = "protein")
    private Double protein; // 蛋白质含量(g)

    @Column(name = "carbs")
    private Double carbs; // 碳水化合物含量(g)

    @Column(name = "fat")
    private Double fat; // 脂肪含量(g)

    @Column(length = 20)
    private String difficulty; // 难度等级：easy/medium/hard

    private Integer servings; // 份量

    @BatchSize(size = 20)  // 性能优化：批量加载标签，减少 SQL 查询次数
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_tags", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "tag", length = 50)
    private List<String> tags; // 标签：weight_loss/muscle_gain/blood_sugar_control

    @Column(name = "suitable_for_diet")
    private Boolean suitableForDiet = false; // 是否适合特定饮食
    
    @Column(name = "image_url", length = 500)
    private String imageUrl; // 食谱图片URL
    
    @Column(name = "video_url", length = 500)
    private String videoUrl; // 教学视频URL
    
    @Column(name = "prep_time")
    private Integer prepTime; // 准备时间（分钟）
    
    @Column(name = "cook_time")
    private Integer cookTime; // 烹饪时间（分钟）
    
    @Column(name = "total_time")
    private Integer totalTime; // 总时间（分钟）
    
    @Column(name = "serving_size", length = 100)
    private String servingSize; // 份量规格
    
    @Column(name = "equipment_needed", length = 500)
    private String equipmentNeeded; // 所需厨具
    
    @Column(name = "tips_and_tricks", length = 1000)
    private String tipsAndTricks; // 小贴士和技巧
    
    @Column(name = "nutritional_facts", length = 1000)
    private String nutritionalFacts; // 营养成分详情（JSON格式）
    
    @Column(name = "allergen_warnings", length = 300)
    private String allergenWarnings; // 过敏原警告
    
    @Column(name = "dietary_restrictions_compatible", length = 200)
    private String dietaryRestrictionsCompatible; // 兼容的饮食限制
    
    @Column(name = "seasonal_availability", length = 100)
    private String seasonalAvailability; // 季节适宜性
    
    @Column(name = "cost_estimate")
    private Double costEstimate; // 成本预估（元）
    
    @Column(name = "rating_average")
    private Double ratingAverage; // 平均评分
    
    @Column(name = "rating_count")
    private Integer ratingCount; // 评分人数
    
    @Column(name = "popularity_score")
    private Integer popularityScore; // 流行度评分
    
    @Column(name = "health_score")
    private Integer healthScore; // 健康评分(1-10)
    
    @Column(name = "created_by", length = 50)
    private String createdBy; // 创建者
    
    @Column(name = "is_public")
    private Boolean isPublic = true; // 是否公开

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
