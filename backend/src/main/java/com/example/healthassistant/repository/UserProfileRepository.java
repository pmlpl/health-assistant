package com.example.healthassistant.repository;

import com.example.healthassistant.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    // 基础查询方法
    UserProfile findByUserId(String userId);
    
    // === 性能优化：使用 JOIN FETCH 避免 N+1 问题 ===
    
    /**
     * 优化查询：一次性加载用户档案和饮食禁忌
     * 注意：Hibernate 不支持同时 fetch 多个集合，所以只 fetch 主要的 dietaryRestrictions
     * tastePreferences 通过 @BatchSize 懒加载优化
     */
    @Query("SELECT up FROM UserProfile up " +
           "LEFT JOIN FETCH up.dietaryRestrictions " +
           "WHERE up.userId = :userId")
    UserProfile findByUserIdWithDietaryRestrictions(@Param("userId") String userId);
}
