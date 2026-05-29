package com.example.healthassistant.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * 环境变量初始化配置
 * 确保在 Spring Boot 启动时加载 .env 文件中的环境变量
 */
@Configuration
public class EnvironmentInitializer {
    
    @PostConstruct
    public void init() {
        // 触发 EnvConfig 的静态块执行，加载 .env 文件
        EnvConfig.isLoaded();
    }
}
