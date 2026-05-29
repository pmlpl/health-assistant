package com.example.healthassistant.service;

import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.repository.UserProfileRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在事务内加载 UserProfile 的懒加载集合，避免 LazyInitializationException。
 */
@Service
public class UserProfileLoadService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileLoadService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * 加载档案并初始化饮食禁忌、口味偏好（Hibernate 不能一次 JOIN FETCH 两个 List）。
     */
    @Transactional(readOnly = true)
    public UserProfile loadWithCollections(String userId) {
        UserProfile profile = userProfileRepository.findByUserIdWithDietaryRestrictions(userId);
        if (profile != null) {
            Hibernate.initialize(profile.getTastePreferences());
        }
        return profile;
    }
}
