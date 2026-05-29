package com.example.healthassistant.service;

import com.example.healthassistant.ai.ResolvedAiConfig;
import com.example.healthassistant.dto.UserAiSettingsDto;
import com.example.healthassistant.dto.UserAiSettingsUpdateDto;
import com.example.healthassistant.model.UserAiSettings;
import com.example.healthassistant.repository.UserAiSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserAiSettingsService {

    private final UserAiSettingsRepository repository;
    private final AiSettingsEncryptionService encryptionService;
    private final String deploymentMode;
    private final String defaultProvider;
    private final String defaultLmBaseUrl;
    private final String defaultLmModel;
    private final boolean requireUserConfig;

    public UserAiSettingsService(
            UserAiSettingsRepository repository,
            AiSettingsEncryptionService encryptionService,
            @Value("${ai.deployment-mode:dev}") String deploymentMode,
            @Value("${ai.provider.default:lmstudio}") String defaultProvider,
            @Value("${ai.lmstudio.base-url:http://127.0.0.1:1234/v1}") String defaultLmBaseUrl,
            @Value("${ai.lmstudio.model:qwen3.5-9b}") String defaultLmModel,
            @Value("${ai.require-user-config:false}") boolean requireUserConfig) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.deploymentMode = deploymentMode;
        this.defaultProvider = defaultProvider;
        this.defaultLmBaseUrl = defaultLmBaseUrl;
        this.defaultLmModel = defaultLmModel;
        this.requireUserConfig = requireUserConfig;
    }

    public boolean isDevMode() {
        return "dev".equalsIgnoreCase(deploymentMode);
    }

    @Transactional
    public UserAiSettings initForNewUser(String userId) {
        if (repository.findByUserId(userId).isPresent()) {
            return repository.findByUserId(userId).get();
        }
        UserAiSettings settings = new UserAiSettings();
        settings.setUserId(userId);
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        if (isDevMode()) {
            settings.setTextProvider("lmstudio");
            settings.setLmstudioBaseUrl(defaultLmBaseUrl);
            settings.setLmstudioModel(defaultLmModel);
        } else {
            settings.setTextProvider("unset");
        }
        return repository.save(settings);
    }

    public UserAiSettings getOrCreate(String userId) {
        return repository.findByUserId(userId).orElseGet(() -> initForNewUser(userId));
    }

    public ResolvedAiConfig resolve(String userId) {
        UserAiSettings settings = getOrCreate(userId);
        String provider = settings.getTextProvider();
        if (provider == null || "unset".equals(provider)) {
            provider = isDevMode() ? defaultProvider : "unset";
        }

        String lmUrl = firstNonBlank(settings.getLmstudioBaseUrl(), isDevMode() ? defaultLmBaseUrl : null);
        String lmModel = firstNonBlank(settings.getLmstudioModel(), isDevMode() ? defaultLmModel : null);
        String dashKey = encryptionService.decryptSafe(settings.getDashscopeApiKeyEnc());
        String deepseekKey = encryptionService.decryptSafe(settings.getDeepseekApiKeyEnc());
        String customKey = encryptionService.decryptSafe(settings.getCustomApiKeyEnc());
        String doubaoKey = encryptionService.decryptSafe(settings.getDoubaoApiKeyEnc());
        String pexelsKey = encryptionService.decryptSafe(settings.getPexelsApiKeyEnc());

        boolean configured = isConfigured(provider, lmUrl, dashKey, deepseekKey, customKey, doubaoKey, settings);

        return ResolvedAiConfig.builder()
                .textProvider(provider)
                .lmstudioBaseUrl(lmUrl)
                .lmstudioModel(lmModel)
                .cloudModel(settings.getCloudModel())
                .customApiBaseUrl(settings.getCustomApiBaseUrl())
                .dashscopeApiKey(dashKey)
                .deepseekApiKey(deepseekKey)
                .customApiKey(customKey)
                .doubaoApiKey(doubaoKey)
                .pexelsApiKey(pexelsKey)
                .configured(configured)
                .activeTextBackend(provider)
                .build();
    }

    private boolean isConfigured(String provider, String lmUrl, String dashKey, String deepseekKey,
            String customKey, String doubaoKey, UserAiSettings settings) {
        return switch (provider) {
            case "dashscope" -> hasText(dashKey);
            case "deepseek" -> hasText(deepseekKey);
            case "doubao" -> hasText(doubaoKey);
            case "other" -> hasText(customKey) && hasText(settings.getCustomApiBaseUrl());
            case "lmstudio" -> hasText(lmUrl);
            case "auto" -> hasText(lmUrl) || hasText(dashKey) || hasText(deepseekKey)
                    || hasText(doubaoKey) || (hasText(customKey) && hasText(settings.getCustomApiBaseUrl()));
            default -> isDevMode() && hasText(lmUrl);
        };
    }

    public UserAiSettingsDto toDto(String userId) {
        UserAiSettings settings = getOrCreate(userId);
        ResolvedAiConfig resolved = resolve(userId);
        UserAiSettingsDto dto = new UserAiSettingsDto();
        dto.setDeploymentMode(deploymentMode);
        dto.setTextProvider(settings.getTextProvider());
        dto.setLmstudioBaseUrl(settings.getLmstudioBaseUrl());
        dto.setLmstudioModel(settings.getLmstudioModel());
        dto.setCloudModel(settings.getCloudModel());
        dto.setCustomApiBaseUrl(settings.getCustomApiBaseUrl());
        dto.setConfigured(resolved.isConfigured());
        dto.setActiveTextBackend(resolved.getActiveTextBackend());
        dto.setDashscopeKeyMasked(maskIfPresent(settings.getDashscopeApiKeyEnc()));
        dto.setDeepseekKeyMasked(maskIfPresent(settings.getDeepseekApiKeyEnc()));
        dto.setCustomKeyMasked(maskIfPresent(settings.getCustomApiKeyEnc()));
        dto.setDoubaoKeyMasked(maskIfPresent(settings.getDoubaoApiKeyEnc()));
        dto.setPexelsKeyMasked(maskIfPresent(settings.getPexelsApiKeyEnc()));
        dto.setHasDashscopeKey(settings.getDashscopeApiKeyEnc() != null);
        dto.setHasDeepseekKey(settings.getDeepseekApiKeyEnc() != null);
        dto.setHasCustomKey(settings.getCustomApiKeyEnc() != null);
        dto.setHasDoubaoKey(settings.getDoubaoApiKeyEnc() != null);
        dto.setHasPexelsKey(settings.getPexelsApiKeyEnc() != null);
        return dto;
    }

    @Transactional
    public UserAiSettingsDto update(String userId, UserAiSettingsUpdateDto update) {
        UserAiSettings settings = getOrCreate(userId);
        if (update.getTextProvider() != null && !update.getTextProvider().isBlank()) {
            settings.setTextProvider(update.getTextProvider());
        }
        if (update.getLmstudioBaseUrl() != null) {
            settings.setLmstudioBaseUrl(update.getLmstudioBaseUrl().isBlank() ? null : update.getLmstudioBaseUrl());
        }
        if (update.getLmstudioModel() != null) {
            settings.setLmstudioModel(update.getLmstudioModel().isBlank() ? null : update.getLmstudioModel());
        }
        if (update.getCloudModel() != null) {
            settings.setCloudModel(update.getCloudModel().isBlank() ? null : update.getCloudModel());
        }
        if (update.getCustomApiBaseUrl() != null) {
            settings.setCustomApiBaseUrl(update.getCustomApiBaseUrl().isBlank() ? null : update.getCustomApiBaseUrl());
        }
        if (update.getDashscopeApiKey() != null && !update.getDashscopeApiKey().isBlank()) {
            settings.setDashscopeApiKeyEnc(encryptionService.encrypt(update.getDashscopeApiKey()));
        }
        if (update.getDeepseekApiKey() != null && !update.getDeepseekApiKey().isBlank()) {
            settings.setDeepseekApiKeyEnc(encryptionService.encrypt(update.getDeepseekApiKey()));
        }
        if (update.getCustomApiKey() != null && !update.getCustomApiKey().isBlank()) {
            settings.setCustomApiKeyEnc(encryptionService.encrypt(update.getCustomApiKey()));
        }
        if (update.getDoubaoApiKey() != null && !update.getDoubaoApiKey().isBlank()) {
            settings.setDoubaoApiKeyEnc(encryptionService.encrypt(update.getDoubaoApiKey()));
        }
        if (update.getPexelsApiKey() != null && !update.getPexelsApiKey().isBlank()) {
            settings.setPexelsApiKeyEnc(encryptionService.encrypt(update.getPexelsApiKey()));
        }
        settings.setUpdatedAt(LocalDateTime.now());
        repository.save(settings);
        return toDto(userId);
    }

    public Optional<String> getDecryptedDashscopeKey(String userId) {
        return Optional.ofNullable(resolve(userId).getDashscopeApiKey());
    }

    public Optional<String> getDecryptedDoubaoKey(String userId) {
        return Optional.ofNullable(resolve(userId).getDoubaoApiKey());
    }

    public Optional<String> getDecryptedPexelsKey(String userId) {
        return Optional.ofNullable(resolve(userId).getPexelsApiKey());
    }

    public void assertTextAiConfigured(String userId) {
        ResolvedAiConfig config = resolve(userId);
        String provider = config.getTextProvider();
        switch (provider) {
            case "lmstudio" -> {
                if (!hasText(config.getLmstudioBaseUrl())) {
                    throw new com.example.healthassistant.exception.AiNotConfiguredException(
                            "请先在 AI 设置中选择「本地 LM Studio」并填写服务地址（如 http://127.0.0.1:1234/v1）");
                }
            }
            case "dashscope" -> {
                if (!hasText(config.getDashscopeApiKey())) {
                    throw new com.example.healthassistant.exception.AiNotConfiguredException(
                            "当前为通义千问，请配置 DashScope API Key，或改选「本地 LM Studio」");
                }
            }
            case "deepseek" -> {
                if (!hasText(config.getDeepseekApiKey())) {
                    throw new com.example.healthassistant.exception.AiNotConfiguredException(
                            "当前为 DeepSeek，请配置 API Key，或改选「本地 LM Studio」");
                }
            }
            case "doubao" -> {
                if (!hasText(config.getDoubaoApiKey())) {
                    throw new com.example.healthassistant.exception.AiNotConfiguredException(
                            "当前为豆包，请配置 API Key，或改选「本地 LM Studio」");
                }
            }
            case "other" -> {
                if (!hasText(config.getCustomApiBaseUrl()) || !hasText(config.getCustomApiKey())) {
                    throw new com.example.healthassistant.exception.AiNotConfiguredException(
                            "请配置「其他」API 地址与 Key，或改选「本地 LM Studio」");
                }
            }
            case "auto" -> {
                if (!hasText(config.getLmstudioBaseUrl())
                        && !hasText(config.getDashscopeApiKey())
                        && !hasText(config.getDeepseekApiKey())
                        && !hasText(config.getDoubaoApiKey())) {
                    throw new com.example.healthassistant.exception.AiNotConfiguredException(
                            "自动模式需要 LM Studio 地址或至少一个云端 API Key");
                }
            }
            default -> {
                if (requireUserConfig && !config.isConfigured()) {
                    throw new com.example.healthassistant.exception.AiNotConfiguredException();
                }
            }
        }
        if (requireUserConfig && !config.isConfigured()) {
            throw new com.example.healthassistant.exception.AiNotConfiguredException();
        }
    }

    private String maskIfPresent(String enc) {
        if (enc == null) {
            return null;
        }
        return AiSettingsEncryptionService.maskKey(encryptionService.decryptSafe(enc));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
