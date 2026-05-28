package com.example.healthassistant.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_ai_settings", indexes = {
        @Index(name = "uk_user_ai_settings_user_id", columnList = "user_id", unique = true)
})
@Data
public class UserAiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    /** lmstudio / dashscope / auto / unset */
    @Column(name = "text_provider", nullable = false, length = 20)
    private String textProvider = "unset";

    @Column(name = "lmstudio_base_url", length = 200)
    private String lmstudioBaseUrl;

    @Column(name = "lmstudio_model", length = 100)
    private String lmstudioModel;

    @Column(name = "dashscope_api_key_enc", columnDefinition = "TEXT")
    private String dashscopeApiKeyEnc;

    @Column(name = "doubao_api_key_enc", columnDefinition = "TEXT")
    private String doubaoApiKeyEnc;

    @Column(name = "pexels_api_key_enc", columnDefinition = "TEXT")
    private String pexelsApiKeyEnc;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
