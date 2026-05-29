package com.example.healthassistant.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 记录各用户使用「平台代付 Key」的调用次数 */
@Entity
@Table(name = "user_ai_platform_usage", indexes = {
        @Index(name = "uk_user_ai_platform_usage_user_id", columnList = "user_id", unique = true)
})
@Data
public class UserAiPlatformUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(name = "text_calls", nullable = false)
    private int textCalls;

    /** 饮食日记拍照识食 */
    @Column(name = "image_calls", nullable = false)
    private int imageCalls;

    /** AI 食谱配图（豆包 Seedream 生图） */
    @Column(name = "recipe_image_calls", nullable = false)
    private int recipeImageCalls;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
