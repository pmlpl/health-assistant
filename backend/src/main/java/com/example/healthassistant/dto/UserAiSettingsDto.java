package com.example.healthassistant.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UserAiSettingsDto {
    private String deploymentMode;
    private String textProvider;
    private String lmstudioBaseUrl;
    private String lmstudioModel;
    private String cloudModel;
    private String customApiBaseUrl;
    private boolean configured;
    private String activeTextBackend;
    private String dashscopeKeyMasked;
    private String deepseekKeyMasked;
    private String customKeyMasked;
    private String doubaoKeyMasked;
    private String pexelsKeyMasked;
    private boolean hasDashscopeKey;
    private boolean hasDeepseekKey;
    private boolean hasCustomKey;
    private boolean hasDoubaoKey;
    private boolean hasPexelsKey;
    /** 拍照识食 */
    private String visionProvider;
    private String visionModel;
    private String visionLmstudioBaseUrl;
    private boolean visionConfigured;
    private String visionApiKeyMasked;
    private boolean hasVisionApiKey;
    /** 平台试用剩余额度 */
    private Map<String, Object> platformQuota;
}
