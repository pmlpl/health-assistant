package com.example.healthassistant.config;



import com.example.healthassistant.service.UserAiSettingsService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;



import java.util.Optional;



@Component

public class ApiKeyResolver {



    @Value("${dashscope.api.key:}")

    private String dashscopeFromConfig;



    @Value("${doubao.api.key:}")

    private String doubaoFromConfig;



    @Value("${ai.deployment-mode:dev}")

    private String deploymentMode;



    @Autowired(required = false)

    private UserAiSettingsService userAiSettingsService;



  /** 获取通义千问 DashScope API Key（可选 userId） */

    public String getDashscopeApiKey() {

        return getDashscopeApiKey(null);

    }



    public String getDashscopeApiKey(String userId) {

        if (userId != null && userAiSettingsService != null) {

            Optional<String> userKey = userAiSettingsService.getDecryptedDashscopeKey(userId);

            if (userKey.isPresent() && isValidKey(userKey.get())) {

                return userKey.get();

            }

        }

        if (isDevMode()) {

            return resolve(EnvConfig.getDashscopeApiKey(), dashscopeFromConfig);

        }

        return null;

    }



    public String getDoubaoApiKey() {

        return getDoubaoApiKey(null);

    }



    public String getDoubaoApiKey(String userId) {

        if (userId != null && userAiSettingsService != null) {

            Optional<String> userKey = userAiSettingsService.getDecryptedDoubaoKey(userId);

            if (userKey.isPresent() && isValidKey(userKey.get())) {

                return userKey.get();

            }

        }

        if (isDevMode()) {

            return resolve(EnvConfig.getDoubaoApiKey(), doubaoFromConfig);

        }

        return null;

    }



    public boolean isDashscopeConfigured() {

        return isDashscopeConfigured(null);

    }



    public boolean isDashscopeConfigured(String userId) {

        return getDashscopeApiKey(userId) != null;

    }



    public boolean isDoubaoConfigured() {

        return isDoubaoConfigured(null);

    }



    public boolean isDoubaoConfigured(String userId) {

        return getDoubaoApiKey(userId) != null;

    }



    public boolean isDevMode() {

        return "dev".equalsIgnoreCase(deploymentMode);

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


