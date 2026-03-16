package com.example.healthassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profile", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id", unique = true),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_health_goal", columnList = "health_goal"),
    @Index(name = "idx_activity_level", columnList = "activity_level")
})
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(name = "password", nullable = false, length = 255)
    private String password; // 密码字段（BCrypt加密存储）

    @Column(name = "height")
    private Integer height; // 身高 cm (50-250)

    @Column(name = "weight")
    private Double weight; // 体重 kg (支持小数，30-300)

    @Column(name = "age")
    private Integer age; // 年龄 (10-120)

    @Column(name = "gender", length = 10)
    private String gender; // 性别：M(男)/F(女)/O(其他)/N(不愿透露)

    @BatchSize(size = 10)  // 性能优化：批量加载饮食禁忌
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "dietary_restrictions", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "restriction", length = 100)
    private List<String> dietaryRestrictions = new ArrayList<>(); // 饮食禁忌

    @BatchSize(size = 10)  // 性能优化：批量加载口味偏好
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "taste_preferences", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "preference", length = 50)
    private List<String> tastePreferences = new ArrayList<>(); // 口味偏好

    @Column(name = "activity_level", length = 20)
    private String activityLevel; // 活动量级别：sedentary/light/moderate/heavy/very_heavy

    @Column(name = "health_goal", length = 20)
    private String healthGoal; // 健康目标：weight_loss/muscle_gain/blood_sugar_control/general_health

    @Column(name = "target_calories")
    private Double targetCalories; // 目标卡路里 (0-10000)

    @Column(name = "target_protein")
    private Double targetProtein; // 目标蛋白质(g) (0-500)

    @Column(name = "target_carbs")
    private Double targetCarbs; // 目标碳水化合物(g) (0-1000)

    @Column(name = "target_fat")
    private Double targetFat; // 目标脂肪(g) (0-300)

    @Column(name = "email", unique = true, length = 100)
    private String email; // 邮箱地址
    
    @Column(name = "phone", length = 20)
    private String phone; // 手机号码
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl; // 头像 URL
    
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime; // 最后登录时间
    
    @Column(name = "is_active")
    private Boolean isActive = true; // 账户是否激活
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Getters and Setters for password
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    // 手动添加其他getter/setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public List<String> getDietaryRestrictions() { return dietaryRestrictions; }
    public void setDietaryRestrictions(List<String> dietaryRestrictions) { this.dietaryRestrictions = dietaryRestrictions; }
    
    public List<String> getTastePreferences() { return tastePreferences; }
    public void setTastePreferences(List<String> tastePreferences) { this.tastePreferences = tastePreferences; }
    
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    
    public String getHealthGoal() { return healthGoal; }
    public void setHealthGoal(String healthGoal) { this.healthGoal = healthGoal; }
    
    public Double getTargetCalories() { return targetCalories; }
    public void setTargetCalories(Double targetCalories) { this.targetCalories = targetCalories; }
    
    public Double getTargetProtein() { return targetProtein; }
    public void setTargetProtein(Double targetProtein) { this.targetProtein = targetProtein; }
    
    public Double getTargetCarbs() { return targetCarbs; }
    public void setTargetCarbs(Double targetCarbs) { this.targetCarbs = targetCarbs; }
    
    public Double getTargetFat() { return targetFat; }
    public void setTargetFat(Double targetFat) { this.targetFat = targetFat; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
