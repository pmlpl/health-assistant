package com.example.healthassistant.service;

import com.example.healthassistant.exception.AiQuotaExceededException;
import com.example.healthassistant.model.UserAiPlatformUsage;
import com.example.healthassistant.repository.UserAiPlatformUsageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** 平台代付 Key 试用配额：对话可不限次，识图/食谱生图各有限额 */
@Service
public class PlatformAiQuotaService {

    public enum UsageKind {
        TEXT, IMAGE, RECIPE_IMAGE
    }

    private final UserAiPlatformUsageRepository repository;

    @Value("${ai.platform.trial-enabled:true}")
    private boolean trialEnabled;

    /** ≤0 表示不限次（平台 DeepSeek 对话） */
    @Value("${ai.platform.trial.text-quota:0}")
    private int textQuota;

    @Value("${ai.platform.trial.image-quota:10}")
    private int imageQuota;

    @Value("${ai.platform.trial.recipe-image-quota:10}")
    private int recipeImageQuota;

    public PlatformAiQuotaService(UserAiPlatformUsageRepository repository) {
        this.repository = repository;
    }

    public boolean isTrialEnabled() {
        return trialEnabled;
    }

    public boolean isTextUnlimited() {
        return textQuota <= 0;
    }

    public void assertTrialAvailable(String userId, UsageKind kind) {
        if (!trialEnabled) {
            throw new AiQuotaExceededException(
                    "平台未开放试用代付，请在「AI 设置」中配置您自己的 API Key。详见「使用手册」。");
        }
        if (kind == UsageKind.TEXT && isTextUnlimited()) {
            return;
        }
        int used = getUsed(userId, kind);
        int limit = limitFor(kind);
        if (used >= limit) {
            throw new AiQuotaExceededException(
                    "平台试用额度已用完（"
                            + kindLabel(kind) + " " + used + "/" + limit
                            + "）。请在右上角「AI 设置」填写您自己的 Key，或查看「使用手册」。");
        }
    }

    @Transactional
    public void recordPlatformUsage(String userId, UsageKind kind) {
        if (kind == UsageKind.TEXT && isTextUnlimited()) {
            return;
        }
        UserAiPlatformUsage row = repository.findByUserId(userId).orElseGet(() -> {
            UserAiPlatformUsage u = new UserAiPlatformUsage();
            u.setUserId(userId);
            u.setTextCalls(0);
            u.setImageCalls(0);
            u.setRecipeImageCalls(0);
            u.setUpdatedAt(LocalDateTime.now());
            return u;
        });
        switch (kind) {
            case TEXT -> row.setTextCalls(row.getTextCalls() + 1);
            case IMAGE -> row.setImageCalls(row.getImageCalls() + 1);
            case RECIPE_IMAGE -> row.setRecipeImageCalls(row.getRecipeImageCalls() + 1);
        }
        row.setUpdatedAt(LocalDateTime.now());
        repository.save(row);
    }

    public Map<String, Object> quotaStatus(String userId) {
        int textUsed = getUsed(userId, UsageKind.TEXT);
        int imageUsed = getUsed(userId, UsageKind.IMAGE);
        int recipeUsed = getUsed(userId, UsageKind.RECIPE_IMAGE);
        Map<String, Object> m = new HashMap<>();
        m.put("trialEnabled", trialEnabled);
        m.put("textUnlimited", isTextUnlimited());
        m.put("textUsed", textUsed);
        m.put("textLimit", isTextUnlimited() ? null : textQuota);
        m.put("textRemaining", isTextUnlimited() ? null : Math.max(0, textQuota - textUsed));
        m.put("imageUsed", imageUsed);
        m.put("imageLimit", imageQuota);
        m.put("imageRemaining", Math.max(0, imageQuota - imageUsed));
        m.put("recipeImageUsed", recipeUsed);
        m.put("recipeImageLimit", recipeImageQuota);
        m.put("recipeImageRemaining", Math.max(0, recipeImageQuota - recipeUsed));
        return m;
    }

    private int limitFor(UsageKind kind) {
        return switch (kind) {
            case TEXT -> textQuota;
            case IMAGE -> imageQuota;
            case RECIPE_IMAGE -> recipeImageQuota;
        };
    }

    private int getUsed(String userId, UsageKind kind) {
        if (userId == null || userId.isBlank()) {
            return 0;
        }
        return repository.findByUserId(userId)
                .map(u -> switch (kind) {
                    case TEXT -> u.getTextCalls();
                    case IMAGE -> u.getImageCalls();
                    case RECIPE_IMAGE -> u.getRecipeImageCalls();
                })
                .orElse(0);
    }

    private static String kindLabel(UsageKind kind) {
        return switch (kind) {
            case TEXT -> "AI 对话";
            case IMAGE -> "拍照识食";
            case RECIPE_IMAGE -> "食谱配图";
        };
    }
}
