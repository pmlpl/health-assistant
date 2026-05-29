// ============================================================
// 这个文件负责：返回给前端的用户档案 DTO（不含密码等敏感字段）
// ============================================================
package com.example.healthassistant.dto;

import com.example.healthassistant.model.UserProfile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserProfileResponseDto {

    private Long id;
    private String userId;
    private Integer height;
    private Double weight;
    private Integer age;
    private String gender;
    private List<String> dietaryRestrictions = new ArrayList<>();
    private List<String> tastePreferences = new ArrayList<>();
    private String activityLevel;
    private String healthGoal;
    private Double targetCalories;
    private Double targetProtein;
    private Double targetCarbs;
    private Double targetFat;
    private String email;
    private String phone;
    private String avatarUrl;
    private LocalDateTime lastLoginTime;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为安全响应对象，排除 password 字段
     */
    public static UserProfileResponseDto from(UserProfile profile) {
        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.id = profile.getId();
        dto.userId = profile.getUserId();
        dto.height = profile.getHeight();
        dto.weight = profile.getWeight();
        dto.age = profile.getAge();
        dto.gender = profile.getGender();
        if (profile.getDietaryRestrictions() != null) {
            dto.dietaryRestrictions = new ArrayList<>(profile.getDietaryRestrictions());
        }
        if (profile.getTastePreferences() != null) {
            dto.tastePreferences = new ArrayList<>(profile.getTastePreferences());
        }
        dto.activityLevel = profile.getActivityLevel();
        dto.healthGoal = profile.getHealthGoal();
        dto.targetCalories = profile.getTargetCalories();
        dto.targetProtein = profile.getTargetProtein();
        dto.targetCarbs = profile.getTargetCarbs();
        dto.targetFat = profile.getTargetFat();
        dto.email = profile.getEmail();
        dto.phone = profile.getPhone();
        dto.avatarUrl = profile.getAvatarUrl();
        dto.lastLoginTime = profile.getLastLoginTime();
        dto.isActive = profile.getIsActive();
        dto.createdAt = profile.getCreatedAt();
        dto.updatedAt = profile.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Integer getHeight() { return height; }
    public Double getWeight() { return weight; }
    public Integer getAge() { return age; }
    public String getGender() { return gender; }
    public List<String> getDietaryRestrictions() { return dietaryRestrictions; }
    public List<String> getTastePreferences() { return tastePreferences; }
    public String getActivityLevel() { return activityLevel; }
    public String getHealthGoal() { return healthGoal; }
    public Double getTargetCalories() { return targetCalories; }
    public Double getTargetProtein() { return targetProtein; }
    public Double getTargetCarbs() { return targetCarbs; }
    public Double getTargetFat() { return targetFat; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
