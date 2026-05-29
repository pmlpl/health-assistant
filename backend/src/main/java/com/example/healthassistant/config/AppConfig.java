package com.example.healthassistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Value("${ai.lmstudio.connect-timeout-seconds:90}")
    private int lmConnectTimeoutSeconds;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int connectMs = Math.max(lmConnectTimeoutSeconds, 8) * 1000;
        factory.setConnectTimeout(connectMs);
        factory.setReadTimeout(600_000);
        return new RestTemplate(factory);
    }
}
