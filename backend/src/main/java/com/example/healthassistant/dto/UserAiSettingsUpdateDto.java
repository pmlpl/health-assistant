package com.example.healthassistant.dto;

import lombok.Data;

@Data
public class UserAiSettingsUpdateDto {
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
}
