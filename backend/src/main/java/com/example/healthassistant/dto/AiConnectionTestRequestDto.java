package com.example.healthassistant.dto;

import lombok.Data;

/**
 * AI 连通性测试请求：支持保存前用表单预览。
 */
@Data
public class AiConnectionTestRequestDto {
    private String type;
    private String lmstudioBaseUrl;
    private String lmstudioModel;
    private String cloudModel;
    private String customApiBaseUrl;
    private String dashscopeApiKey;
    private String deepseekApiKey;
    private String customApiKey;
    private String doubaoApiKey;
    private String pexelsApiKey;
}
