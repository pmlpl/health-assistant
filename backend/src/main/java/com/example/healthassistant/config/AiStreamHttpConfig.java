package com.example.healthassistant.config;

import com.example.healthassistant.ai.OpenAiStreamClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 配置 LM / OpenAI 兼容流式 HTTP 客户端超时。
 */
@Configuration
public class AiStreamHttpConfig {

    @Value("${ai.lmstudio.connect-timeout-seconds:90}")
    private int connectTimeoutSeconds;

    @PostConstruct
    void configureStreamClient() {
        OpenAiStreamClient.setConnectTimeoutSeconds(connectTimeoutSeconds);
    }
}
