package com.example.healthassistant.service;



import com.example.healthassistant.ai.LmStudioChatClient;

import com.example.healthassistant.ai.ResolvedAiConfig;

import com.example.healthassistant.config.ApiKeyResolver;

import com.example.healthassistant.security.AuthSupport;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;



import java.util.HashMap;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;



@Service

public class ApiAvailabilityService {



    @Autowired

    private ApiKeyResolver apiKeyResolver;



    @Autowired

    private UserAiSettingsService userAiSettingsService;



    @Autowired

    private LmStudioChatClient lmStudioChatClient;



    private final Map<String, ApiStatus> apiStatusCache = new ConcurrentHashMap<>();

    private static final long CACHE_EXPIRY_MS = 60000;



    public Map<String, ApiStatus> checkAllApis() {

        String userId = safeCurrentUserId();

        Map<String, ApiStatus> statusMap = new HashMap<>();

        statusMap.put("lmstudio", checkLmStudio(userId));

        statusMap.put("dashscope", checkDashscope(userId));

        statusMap.put("doubao", checkDoubao(userId));

        statusMap.put("pexels", checkPexels(userId));

        return statusMap;

    }



    public ApiStatus checkLmStudio(String userId) {

        ResolvedAiConfig config = userAiSettingsService.resolve(userId);

        boolean reachable = config.getLmstudioBaseUrl() != null

                && lmStudioChatClient.testConnection(config.getLmstudioBaseUrl());

        return new ApiStatus(reachable,

                reachable ? "LM Studio 可达" : "请启动 LM Studio 并配置地址",

                config.getLmstudioBaseUrl() != null);

    }



    public ApiStatus checkDashscope(String userId) {

        boolean hasKey = apiKeyResolver.isDashscopeConfigured(userId);

        return new ApiStatus(hasKey, hasKey ? "DashScope 已配置" : "未配置 DashScope Key", hasKey);

    }



    public ApiStatus checkDoubao(String userId) {

        boolean hasKey = apiKeyResolver.isDoubaoConfigured(userId);

        return new ApiStatus(hasKey, hasKey ? "豆包已配置" : "未配置豆包 Key", hasKey);

    }



    public ApiStatus checkPexels(String userId) {

        String key = userAiSettingsService.resolve(userId).getPexelsApiKey();

        boolean hasKey = key != null && !key.isBlank();

        return new ApiStatus(hasKey, hasKey ? "Pexels 已配置" : "未配置 Pexels Key", hasKey);

    }



    /** 兼容旧接口 */

    public ApiStatus checkQwenApi() {

        return checkDashscope(safeCurrentUserId());

    }



    public ApiStatus checkDoubaoApi() {

        return checkDoubao(safeCurrentUserId());

    }



    private String safeCurrentUserId() {

        try {

            return AuthSupport.currentUserId();

        } catch (Exception e) {

            return "anonymous";

        }

    }



    public static class ApiStatus {

        private final boolean available;

        private final String message;

        private final boolean hasApiKey;

        private final long timestamp;



        public ApiStatus(boolean available, String message, boolean hasApiKey) {

            this.available = available;

            this.message = message;

            this.hasApiKey = hasApiKey;

            this.timestamp = System.currentTimeMillis();

        }



        public boolean isAvailable() { return available; }

        public String getMessage() { return message; }

        public boolean hasApiKey() { return hasApiKey; }

        public boolean isExpired() {

            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;

        }

        public long getTimestamp() { return timestamp; }

    }

}


