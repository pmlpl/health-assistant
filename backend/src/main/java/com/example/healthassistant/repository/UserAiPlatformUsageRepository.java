package com.example.healthassistant.repository;

import com.example.healthassistant.model.UserAiPlatformUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAiPlatformUsageRepository extends JpaRepository<UserAiPlatformUsage, Long> {

    Optional<UserAiPlatformUsage> findByUserId(String userId);
}
