package com.example.healthassistant.repository;

import com.example.healthassistant.model.UserAiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAiSettingsRepository extends JpaRepository<UserAiSettings, Long> {
    Optional<UserAiSettings> findByUserId(String userId);
}
