package com.example.healthassistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class ApiAvailabilityService {

    @Value("${dashscope.api.key:}")
    private String dashscopeApiKey;

    @Value("${doubao.api.key:}")
    private String doubaoApiKey;

    // API 检测超时时间（毫秒）
    private static final int API_CHECK_TIMEOUT_MS = 5000;

    // 缓存 API 状态，避免频繁检测
    private final Map<String, ApiStatus> apiStatusCache = new ConcurrentHashMap<>();

    // 缓存过期时间（毫秒）
    private static final long CACHE_EXPIRY_MS = 60000; // 1分钟

    /**
     * 检测所有 API 的可用性
     */
    public Map<String, ApiStatus> checkAllApis() {
        Map<String, ApiStatus> statusMap = new HashMap<>();

        // 检测通义千问 API
        statusMap.put("qwen", checkQwenApi());

        // 检测豆包 API
        statusMap.put("doubao", checkDoubaoApi());

        return statusMap;
    }

    /**
     * 检测通义千问 API 的可用性
     */
    public ApiStatus checkQwenApi() {
        // 检查缓存
        ApiStatus cachedStatus = apiStatusCache.get("qwen");
        if (cachedStatus != null && !cachedStatus.isExpired()) {
            return cachedStatus;
        }

        // 检查是否配置了 API Key
        boolean hasApiKey = dashscopeApiKey != null && !dashscopeApiKey.isEmpty();
        
        // 也检查环境变量
        if (!hasApiKey) {
            String envApiKey = System.getenv("DASHSCOPE_API_KEY");
            hasApiKey = envApiKey != null && !envApiKey.isEmpty();
        }
        
        ApiStatus status;
        if (hasApiKey) {
            status = new ApiStatus(true, "API 可用", true);
        } else {
            status = new ApiStatus(false, "未配置 API Key", false);
        }

        // 更新缓存
        apiStatusCache.put("qwen", status);
        return status;
    }

    /**
     * 检测豆包 API 的可用性
     */
    public ApiStatus checkDoubaoApi() {
        // 检查缓存
        ApiStatus cachedStatus = apiStatusCache.get("doubao");
        if (cachedStatus != null && !cachedStatus.isExpired()) {
            return cachedStatus;
        }

        // 检查是否配置了 API Key
        boolean hasApiKey = doubaoApiKey != null && !doubaoApiKey.isEmpty();
        
        // 也检查环境变量
        if (!hasApiKey) {
            String envApiKey = System.getenv("DOUBAO_API_KEY");
            hasApiKey = envApiKey != null && !envApiKey.isEmpty();
        }
        
        ApiStatus status;
        if (hasApiKey) {
            status = new ApiStatus(true, "API 可用", true);
        } else {
            status = new ApiStatus(false, "未配置 API Key", false);
        }

        // 更新缓存
        apiStatusCache.put("doubao", status);
        return status;
    }

    /**
     * 检查 API 端点是否可用
     */
    private boolean isApiAvailable(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(API_CHECK_TIMEOUT_MS);
            connection.setReadTimeout(API_CHECK_TIMEOUT_MS);
            connection.setRequestMethod("HEAD");

            // 执行请求并获取响应码
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * API 状态类
     */
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

        public boolean isAvailable() {
            return available;
        }

        public String getMessage() {
            return message;
        }

        public boolean hasApiKey() {
            return hasApiKey;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}