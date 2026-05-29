package com.example.healthassistant.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResolvedAiConfig {
    private String textProvider;
    private String lmstudioBaseUrl;
    private String lmstudioModel;
    private String cloudModel;
    private String customApiBaseUrl;
    private String dashscopeApiKey;
    private String deepseekApiKey;
    private String customApiKey;
    private String doubaoApiKey;
    private String pexelsApiKey;
    private String visionProvider;
    private String visionModel;
    private String visionApiKey;
    private String visionLmstudioBaseUrl;
    private boolean configured;
    private boolean visionConfigured;
    private String activeTextBackend;
}
