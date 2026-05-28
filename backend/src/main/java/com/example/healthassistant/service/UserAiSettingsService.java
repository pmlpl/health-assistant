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
        String dashKey = encryptionService.decrypt(settings.getDashscopeApiKeyEnc());
        String doubaoKey = encryptionService.decrypt(settings.getDoubaoApiKeyEnc());
        String pexelsKey = encryptionService.decrypt(settings.getPexelsApiKeyEnc());

        boolean configured = isConfigured(provider, lmUrl, dashKey);
        String activeBackend = resolveActiveBackend(provider, lmUrl, dashKey);

        return ResolvedAiConfig.builder()
                .textProvider(provider)
                .lmstudioBaseUrl(lmUrl)
                .lmstudioModel(lmModel)
                .dashscopeApiKey(dashKey)
                .doubaoApiKey(doubaoKey)
                .pexelsApiKey(pexelsKey)
                .configured(configured)
                .activeTextBackend(activeBackend)
                .build();
    }

    private boolean isConfigured(String provider, String lmUrl, String dashKey) {
        if ("dashscope".equals(provider)) {
            return dashKey != null && !dashKey.isBlank();
        }
        if ("lmstudio".equals(provider)) {
            return lmUrl != null && !lmUrl.isBlank();
        }
        if ("auto".equals(provider)) {
            return (lmUrl != null && !lmUrl.isBlank()) || (dashKey != null && !dashKey.isBlank());
        }
        if (isDevMode()) {
            return lmUrl != null && !lmUrl.isBlank();
        }
        return false;
    }

    private String resolveActiveBackend(String provider, String lmUrl, String dashKey) {
        return switch (provider) {
            case "dashscope" -> "dashscope";
            case "lmstudio" -> "lmstudio";
            case "auto" -> (lmUrl != null && !lmUrl.isBlank()) ? "lmstudio" : "dashscope";
            default -> isDevMode() ? "lmstudio" : "unset";
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
        dto.setConfigured(resolved.isConfigured());
        dto.setActiveTextBackend(resolved.getActiveTextBackend());
        dto.setDashscopeKeyMasked(maskIfPresent(settings.getDashscopeApiKeyEnc()));
        dto.setDoubaoKeyMasked(maskIfPresent(settings.getDoubaoApiKeyEnc()));
        dto.setPexelsKeyMasked(maskIfPresent(settings.getPexelsApiKeyEnc()));
        dto.setHasDashscopeKey(settings.getDashscopeApiKeyEnc() != null);
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
        if (update.getDashscopeApiKey() != null && !update.getDashscopeApiKey().isBlank()) {
            settings.setDashscopeApiKeyEnc(encryptionService.encrypt(update.getDashscopeApiKey()));
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
        if (requireUserConfig && !resolve(userId).isConfigured()) {
            throw new com.example.healthassistant.exception.AiNotConfiguredException();
        }
    }

    private String maskIfPresent(String enc) {
        if (enc == null) {
            return null;
        }
        return AiSettingsEncryptionService.maskKey(encryptionService.decrypt(enc));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
