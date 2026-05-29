package com.example.healthassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "diet_record", indexes = {
    @Index(name = "idx_user_date", columnList = "user_id,date"),
    @Index(name = "idx_meal_type", columnList = "meal_type"),
    @Index(name = "idx_recorded_at", columnList = "recorded_at"),
    @Index(name = "idx_calories", columnList = "calories")
})
public class DietRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String userId;
    
    @Column(nullable = false)
    private LocalDate date; // 记录日期
    
    @Column(name = "record_time")
    private LocalTime recordTime; // 记录的具体时间
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MealType mealType; // 餐别
    
    @Column(name = "food_description", length = 500)
    private String foodDescription; // 食物描述
    
    @Column(name = "image_url", length = 500)
    private String imageUrl; // 食物图片URL
    
    @BatchSize(size = 10)  // 性能优化：批量加载食材成分，减少 SQL 查询次数
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "consumed_ingredients", joinColumns = @JoinColumn(name = "diet_record_id"))
    @Column(name = "ingredient_name", length = 100)
    private List<String> consumedIngredients; // 消费的食材名称列表
    
    @Column(name = "calories")
    private Double calories; // 摄入卡路里
    
    @Column(name = "actual_calories")
    private Double actualCalories; // 实际摄入卡路里（考虑重量）
    
    @Column(name = "protein")
    private Double protein; // 摄入蛋白质(g)
    
    @Column(name = "actual_protein")
    private Double actualProtein; // 实际摄入蛋白质(g)
    
    @Column(name = "carbs")
    private Double carbs; // 摄入碳水化合物(g)
    
    @Column(name = "actual_carbs")
    private Double actualCarbs; // 实际摄入碳水化合物(g)
    
    @Column(name = "fat")
    private Double fat; // 摄入脂肪(g)
    
    @Column(name = "actual_fat")
    private Double actualFat; // 实际摄入脂肪(g)
    
    @Column(name = "fiber")
    private Double fiber; // 摄入膳食纤维(g)
    
    @Column(name = "actual_fiber")
    private Double actualFiber; // 实际摄入膳食纤维(g)
    
    @Column(name = "water_content")
    private Double waterContent; // 水分含量(g)
    
    @Column(name = "sodium")
    private Double sodium; // 钠含量(mg)
    
    @Column(name = "calcium")
    private Double calcium; // 钙含量(mg)
    
    @Column(name = "iron")
    private Double iron; // 铁含量(mg)
    
    @Column(name = "vitamin_c")
    private Double vitaminC; // 维生素C含量(mg)
    
    @Column(name = "satisfaction_level")
    private Integer satisfactionLevel; // 满意度评分 (1-5星)
    
    @Column(name = "location", length = 100)
    private String location; // 用餐地点
    
    @Column(name = "notes", length = 500)
    private String notes; // 备注信息
    
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt; // 记录时间
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 更新时间
    
    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 手动添加getter/setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public LocalTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalTime recordTime) { this.recordTime = recordTime; }
    
    public MealType getMealType() { return mealType; }
    public void setMealType(MealType mealType) { this.mealType = mealType; }
    
    public String getFoodDescription() { return foodDescription; }
    public void setFoodDescription(String foodDescription) { this.foodDescription = foodDescription; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public List<String> getConsumedIngredients() { return consumedIngredients; }
    public void setConsumedIngredients(List<String> consumedIngredients) { this.consumedIngredients = consumedIngredients; }
    
    public Double getCalories() { return calories; }
    public void setCalories(Double calories) { this.calories = calories; }
    
    public Double getActualCalories() { return actualCalories; }
    public void setActualCalories(Double actualCalories) { this.actualCalories = actualCalories; }
    
    public Double getProtein() { return protein; }
    public void setProtein(Double protein) { this.protein = protein; }
    
    public Double getActualProtein() { return actualProtein; }
    public void setActualProtein(Double actualProtein) { this.actualProtein = actualProtein; }
    
    public Double getCarbs() { return carbs; }
    public void setCarbs(Double carbs) { this.carbs = carbs; }
    
    public Double getActualCarbs() { return actualCarbs; }
    public void setActualCarbs(Double actualCarbs) { this.actualCarbs = actualCarbs; }
    
    public Double getFat() { return fat; }
    public void setFat(Double fat) { this.fat = fat; }
    
    public Double getActualFat() { return actualFat; }
    public void setActualFat(Double actualFat) { this.actualFat = actualFat; }
    
    public Double getFiber() { return fiber; }
    public void setFiber(Double fiber) { this.fiber = fiber; }
    
    public Double getActualFiber() { return actualFiber; }
    public void setActualFiber(Double actualFiber) { this.actualFiber = actualFiber; }
    
    public Double getWaterContent() { return waterContent; }
    public void setWaterContent(Double waterContent) { this.waterContent = waterContent; }
    
    public Double getSodium() { return sodium; }
    public void setSodium(Double sodium) { this.sodium = sodium; }
    
    public Double getCalcium() { return calcium; }
    public void setCalcium(Double calcium) { this.calcium = calcium; }
    
    public Double getIron() { return iron; }
    public void setIron(Double iron) { this.iron = iron; }
    
    public Double getVitaminC() { return vitaminC; }
    public void setVitaminC(Double vitaminC) { this.vitaminC = vitaminC; }
    
    public Integer getSatisfactionLevel() { return satisfactionLevel; }
    public void setSatisfactionLevel(Integer satisfactionLevel) { this.satisfactionLevel = satisfactionLevel; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
