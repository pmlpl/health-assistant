package com.example.healthassistant.dto;

import lombok.Data;

@Data
public class UserAiSettingsDto {
    private String deploymentMode;
    private String textProvider;
    private String lmstudioBaseUrl;
    private String lmstudioModel;
    private boolean configured;
    private String activeTextBackend;
    private String dashscopeKeyMasked;
    private String doubaoKeyMasked;
    private String pexelsKeyMasked;
    private boolean hasDashscopeKey;
    private boolean hasDoubaoKey;
    private boolean hasPexelsKey;
}
