package com.example.healthassistant.dto;

import lombok.Data;

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
}
