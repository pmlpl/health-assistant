package com.example.healthassistant.service.impl;

import com.example.healthassistant.dto.LoginRequestDto;
import com.example.healthassistant.dto.UserProfileDto;
import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.repository.UserProfileRepository;
import com.example.healthassistant.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserProfileRepository userProfileRepository;
    
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserProfile createOrUpdateUserProfile(UserProfileDto profileDto) {
        UserProfile existingProfile = userProfileRepository.findByUserId(profileDto.getUserId());
        UserProfile profile;

        if (existingProfile != null) {
            profile = existingProfile;
            BeanUtils.copyProperties(profileDto, profile, "id", "createdAt"); // 更新时不改变ID和创建时间
        } else {
            profile = new UserProfile();
            BeanUtils.copyProperties(profileDto, profile);
            profile.setCreatedAt(LocalDateTime.now());
            
            // 确保集合字段被正确初始化
            if (profile.getDietaryRestrictions() == null) {
                profile.setDietaryRestrictions(new ArrayList<>());
            }
            if (profile.getTastePreferences() == null) {
                profile.setTastePreferences(new ArrayList<>());
            }
        }

        profile.setUpdatedAt(LocalDateTime.now());

        // 根据用户信息计算营养目标
        calculateNutritionalGoals(profile);

        return userProfileRepository.save(profile);
    }

    private void calculateNutritionalGoals(UserProfile profile) {
        // 检查必需字段是否存在，如果为null则使用默认值
        Integer height = profile.getHeight() != null ? profile.getHeight() : 170; // 默认身高170cm
        Double weight = profile.getWeight() != null ? profile.getWeight() : 65.0; // 默认体重65kg
        Integer age = profile.getAge() != null ? profile.getAge() : 25; // 默认年龄25岁
        String gender = profile.getGender() != null ? profile.getGender() : "M"; // 默认男性
        String activityLevel = profile.getActivityLevel() != null ? profile.getActivityLevel() : "轻度运动"; // 默认活动水平
        String healthGoal = profile.getHealthGoal() != null ? profile.getHealthGoal() : "一般健康"; // 默认健康目标

        // 简单的营养目标计算逻辑，实际应用中可能需要更复杂的算法
        double bmr = calculateBMR(height, weight, age, gender);

        // 根据活动水平调整
        double activityMultiplier = getActivityMultiplier(activityLevel);
        double dailyCalories = bmr * activityMultiplier;

        // 根据健康目标调整
        if ("减脂".equals(healthGoal)) {
            dailyCalories -= 500; // 减肥通常减少500卡路里
        } else if ("增肌".equals(healthGoal)) {
            dailyCalories += 300; // 增肌通常增加300卡路里
        }

        profile.setTargetCalories(dailyCalories);

        // 设置宏量营养素目标（简化版）
        // 蛋白质：按体重计算，每kg 1.2-2.2g
        double proteinMultiplier = "增肌".equals(healthGoal) ? 2.0 : 1.2;
        profile.setTargetProtein(weight * proteinMultiplier);

        // 脂肪：占总热量20-35%
        profile.setTargetFat(dailyCalories * 0.25 / 9); // 9卡路里/克脂肪

        // 碳水化合物：剩余热量
        double remainingCalories = dailyCalories - (profile.getTargetProtein() * 4) - (profile.getTargetFat() * 9);
        profile.setTargetCarbs(remainingCalories / 4); // 4卡路里/克碳水
    }

    private double calculateBMR(Integer height, Double weight, Integer age, String gender) {
        // 添加空值检查
        if (height == null || weight == null || age == null || gender == null) {
            // 返回默认BMR值（基于默认参数）
            return 10 * 65.0 + 6.25 * 170 - 5 * 25 + 5; // 男性默认值
        }
        
        // 使用Mifflin-St Jeor方程计算基础代谢率
        if ("M".equals(gender)) {
            return 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            return 10 * weight + 6.25 * height - 5 * age - 161;
        }
    }

    private double getActivityMultiplier(String activityLevel) {
        switch (activityLevel) {
            case "久坐": return 1.2;
            case "轻度运动": return 1.375;
            case "中度运动": return 1.55;
            case "重度运动": return 1.725;
            default: return 1.2;
        }
    }

    @Override
    public UserProfile getUserProfile(String userId) {
        // 使用优化后的查询方法，加载用户档案和饮食禁忌
        // tastePreferences 通过 @BatchSize 懒加载优化
        return userProfileRepository.findByUserIdWithDietaryRestrictions(userId);
    }

    @Override
    public UserProfile login(LoginRequestDto loginRequest) {
        System.out.println("尝试登录用户: " + loginRequest.getUsername());
        
        // 查找用户
        UserProfile user = userProfileRepository.findByUserId(loginRequest.getUsername());
        
        if (user != null) {
            System.out.println("找到用户: " + user.getUserId());
            // 验证密码
            if (validatePassword(loginRequest.getPassword(), user.getPassword())) {
                return user;
            } else {
                System.out.println("密码验证失败");
                return null;
            }
        } else {
            System.out.println("未找到用户: " + loginRequest.getUsername());
            return null;
        }
    }
    
    @Override
    public UserProfile register(String username, String password) {
        System.out.println("开始注册用户: " + username);
        
        // 检查用户名是否已存在
        UserProfile existingUser = userProfileRepository.findByUserId(username);
        if (existingUser != null) {
            System.out.println("用户名已存在: " + username);
            return null; // 用户名已存在
        }
        
        // 创建新用户
        UserProfile newUser = new UserProfile();
        newUser.setUserId(username);
        newUser.setPassword(passwordEncoder.encode(password)); // 加密存储密码
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        
        // 设置默认值
        newUser.setDietaryRestrictions(new ArrayList<>());
        newUser.setTastePreferences(new ArrayList<>());
        
        UserProfile savedUser = userProfileRepository.save(newUser);
        System.out.println("用户注册成功: " + savedUser.getUserId());
        
        return savedUser;
    }
    
    @Override
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    @Override
    public boolean deleteUser(String userId) {
        System.out.println("开始删除用户: " + userId);
        
        try {
            // 查找用户
            UserProfile user = userProfileRepository.findByUserId(userId);
            if (user == null) {
                System.out.println("未找到用户: " + userId);
                return false;
            }
            
            // 删除用户
            userProfileRepository.delete(user);
            System.out.println("用户删除成功: " + userId);
            return true;
            
        } catch (Exception e) {
            System.err.println("删除用户失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean updateUsername(String userId, String newUsername) {
        System.out.println("开始修改用户名: " + userId + " -> " + newUsername);
        
        try {
            // 检查新用户名是否已存在
            UserProfile existingUser = userProfileRepository.findByUserId(newUsername);
            if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                System.out.println("新用户名已存在: " + newUsername);
                return false;
            }
            
            // 查找当前用户
            UserProfile currentUser = userProfileRepository.findByUserId(userId);
            if (currentUser == null) {
                System.out.println("未找到用户: " + userId);
                return false;
            }
            
            // 更新用户名
            currentUser.setUserId(newUsername);
            currentUser.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(currentUser);
            
            System.out.println("用户名修改成功: " + userId + " -> " + newUsername);
            return true;
            
        } catch (Exception e) {
            System.err.println("修改用户名失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean updatePassword(String userId, String oldPassword, String newPassword) {
        System.out.println("开始修改密码: " + userId);
        
        try {
            // 查找用户
            UserProfile user = userProfileRepository.findByUserId(userId);
            if (user == null) {
                System.out.println("未找到用户: " + userId);
                return false;
            }
            
            // 验证原密码
            if (!validatePassword(oldPassword, user.getPassword())) {
                System.out.println("原密码验证失败: " + userId);
                return false;
            }
            
            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(user);
            
            System.out.println("密码修改成功：" + userId);
            return true;
            
        } catch (Exception e) {
            System.err.println("修改密码失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean resetPassword(String username, String email, String newPassword) {
        System.out.println("开始重置密码：用户名=" + username + ", 邮箱=" + email);
        
        try {
            // 查找用户
            UserProfile user = userProfileRepository.findByUserId(username);
            if (user == null) {
                System.out.println("未找到用户：" + username);
                return false;
            }
            
            // TODO: 验证邮箱是否匹配（目前 UserProfile 中没有 email 字段）
            // 暂时跳过邮箱验证
            
            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(user);
            
            System.out.println("密码重置成功：" + username);
            return true;
            
        } catch (Exception e) {
            System.err.println("重置密码失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}