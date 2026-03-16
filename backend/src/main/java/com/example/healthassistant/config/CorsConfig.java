package com.example.healthassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 不再使用这个配置，由 SecurityConfig 统一管理 CORS
        // 避免与 Spring Security 的 CORS 配置冲突
    }
}
