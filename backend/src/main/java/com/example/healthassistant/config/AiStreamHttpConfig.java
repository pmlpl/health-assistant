package com.example.healthassistant.config;

import com.example.healthassistant.ai.OpenAiStreamClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * 配置 LM / OpenAI 兼容流式 HTTP 客户端超时，以及 AI 咨询 SSE 专用线程池。
 */
@Configuration
public class AiStreamHttpConfig {

    @Value("${ai.lmstudio.connect-timeout-seconds:90}")
    private int connectTimeoutSeconds;

    /** 流式读取两次 chunk 之间的最大空闲秒数，防止 LM 无响应时永久阻塞 */
    @Value("${ai.consult.stream-idle-timeout-seconds:120}")
    private int streamIdleTimeoutSeconds;

    @PostConstruct
    void configureStreamClient() {
        OpenAiStreamClient.setConnectTimeoutSeconds(connectTimeoutSeconds);
        OpenAiStreamClient.setIdleReadTimeoutSeconds(streamIdleTimeoutSeconds);
    }

    /**
     * AI 咨询 SSE 异步任务专用执行器（虚拟线程），避免占用 ForkJoinPool 导致任务迟迟不执行。
     */
    @Bean(name = "aiConsultStreamExecutor")
    public AsyncTaskExecutor aiConsultStreamExecutor() {
        return new VirtualThreadTaskExecutor("ai-consult-sse-");
    }
}
