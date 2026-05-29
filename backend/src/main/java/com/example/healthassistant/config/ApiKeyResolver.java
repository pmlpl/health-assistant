package com.example.healthassistant.config;

import com.example.healthassistant.ai.ResolvedApiKey;
import com.example.healthassistant.service.PlatformAiQuotaService;
import com.example.healthassistant.service.PlatformAiQuotaService.UsageKind;
import com.example.healthassistant.service.UserAiSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 统一解析用户自备 Key 与平台试用 Key（试用时检查配额，成功后由调用方 record）。
 */
@Component
public class ApiKeyResolver {

    @Value("${dashscope.api.key:}")
    private String dashscopeFromConfig;

    @Value("${doubao.api.key:}")
    private String doubaoFromConfig;

    @Value("${deepseek.api.key:}")
    private String deepseekFromConfig;

    @Value("${ai.deployment-mode:dev}")
    private String deploymentMode;

    @Autowired(required = false)
    private UserAiSettingsService userAiSettingsService;

    @Autowired(required = false)
    private PlatformAiQuotaService platformAiQuotaService;

    public String getDashscopeApiKey() {
        return getDashscopeApiKey(null);
    }

    public String getDashscopeApiKey(String userId) {
        return resolveDashscope(userId, false, UsageKind.TEXT).key();
    }

    public String getDoubaoApiKey() {
        return getDoubaoApiKey(null);
    }

    public String getDoubaoApiKey(String userId) {
        return resolveDoubao(userId, false, UsageKind.TEXT).key();
    }

    public ResolvedApiKey resolveDoubaoForVision(String userId) {
        return resolveDoubao(userId, true, UsageKind.IMAGE);
    }

    public ResolvedApiKey resolveDashscopeForVision(String userId) {
        return resolveDashscope(userId, true, UsageKind.IMAGE);
    }

    public ResolvedApiKey resolveDoubaoForText(String userId) {
        return resolveDoubao(userId, false, UsageKind.TEXT);
    }

    public ResolvedApiKey resolveDashscopeForText(String userId) {
        return resolveDashscope(userId, false, UsageKind.TEXT);
    }

    public ResolvedApiKey resolveDeepseekForText(String userId) {
        String userKey = pickUserDeepseekKey(userId);
        if (isValidKey(userKey)) {
            return new ResolvedApiKey(userKey, false);
        }
        return resolvePlatformDeepseek(userId, UsageKind.TEXT);
    }

    /** AI 食谱配图：豆包 Seedream 生图 */
    public ResolvedApiKey resolveDoubaoForRecipeImage(String userId) {
        return resolveDoubao(userId, true, UsageKind.RECIPE_IMAGE);
    }

    /** 平台试用成功调用后计数 */
    public void recordPlatformUsageIfNeeded(String userId, ResolvedApiKey key, UsageKind kind) {
        if (key != null && key.platformTrial() && userId != null && platformAiQuotaService != null) {
            platformAiQuotaService.recordPlatformUsage(userId, kind);
        }
    }

    public boolean isDashscopeConfigured() {
        return isDashscopeConfigured(null);
    }

    public boolean isDashscopeConfigured(String userId) {
        return resolveDashscope(userId, false, UsageKind.TEXT).isPresent();
    }

    public boolean isDoubaoConfigured() {
        return isDoubaoConfigured(null);
    }

    public boolean isDoubaoConfigured(String userId) {
        return resolveDoubao(userId, false, UsageKind.TEXT).isPresent();
    }

    public boolean isDevMode() {
        return "dev".equalsIgnoreCase(deploymentMode);
    }

    private ResolvedApiKey resolveDashscope(String userId, boolean forVision, UsageKind quotaKind) {
        String userKey = pickUserDashscopeKey(userId, forVision);
        if (isValidKey(userKey)) {
            return new ResolvedApiKey(userKey, false);
        }
        return resolvePlatformDashscope(userId, quotaKind);
    }

    private ResolvedApiKey resolveDoubao(String userId, boolean forVision, UsageKind quotaKind) {
        String userKey = pickUserDoubaoKey(userId, forVision);
        if (isValidKey(userKey)) {
            return new ResolvedApiKey(userKey, false);
        }
        return resolvePlatformDoubao(userId, quotaKind);
    }

    private ResolvedApiKey resolvePlatformDoubao(String userId, UsageKind quotaKind) {
        String platform = resolve(EnvConfig.getDoubaoApiKey(), doubaoFromConfig);
        return wrapPlatformKey(userId, platform, quotaKind);
    }

    private ResolvedApiKey resolvePlatformDashscope(String userId, UsageKind quotaKind) {
        String platform = resolve(EnvConfig.getDashscopeApiKey(), dashscopeFromConfig);
        return wrapPlatformKey(userId, platform, quotaKind);
    }

    private ResolvedApiKey resolvePlatformDeepseek(String userId, UsageKind quotaKind) {
        String platform = resolve(EnvConfig.getDeepseekApiKey(), deepseekFromConfig);
        return wrapPlatformKey(userId, platform, quotaKind);
    }

    private ResolvedApiKey wrapPlatformKey(String userId, String platform, UsageKind quotaKind) {
        if (!isValidKey(platform)) {
            return new ResolvedApiKey(null, false);
        }
        if (platformAiQuotaService != null && platformAiQuotaService.isTrialEnabled()) {
            if (userId != null) {
                platformAiQuotaService.assertTrialAvailable(userId, quotaKind);
            }
            return new ResolvedApiKey(platform, true);
        }
        if (isDevMode()) {
            return new ResolvedApiKey(platform, true);
        }
        return new ResolvedApiKey(null, false);
    }

    private String pickUserDashscopeKey(String userId, boolean forVision) {
        if (userId == null || userAiSettingsService == null) {
            return null;
        }
        if (forVision) {
            Optional<String> vision = userAiSettingsService.getDecryptedVisionApiKey(userId);
            if (vision.isPresent() && isValidKey(vision.get())) {
                return vision.get();
            }
        }
        return userAiSettingsService.getDecryptedDashscopeKey(userId).orElse(null);
    }

    private String pickUserDoubaoKey(String userId, boolean forVision) {
        if (userId == null || userAiSettingsService == null) {
            return null;
        }
        if (forVision) {
            Optional<String> vision = userAiSettingsService.getDecryptedVisionApiKey(userId);
            if (vision.isPresent() && isValidKey(vision.get())) {
                return vision.get();
            }
        }
        return userAiSettingsService.getDecryptedDoubaoKey(userId).orElse(null);
    }

    private String pickUserDeepseekKey(String userId) {
        if (userId == null || userAiSettingsService == null) {
            return null;
        }
        return userAiSettingsService.getDecryptedDeepseekKey(userId).orElse(null);
    }

    private String resolve(String fromEnv, String fromConfig) {
        if (isValidKey(fromEnv)) {
            return fromEnv;
        }
        if (isValidKey(fromConfig)) {
            return fromConfig;
        }
        return null;
    }

    private boolean isValidKey(String key) {
        return key != null && !key.isBlank() && !key.contains("your_");
    }
}
