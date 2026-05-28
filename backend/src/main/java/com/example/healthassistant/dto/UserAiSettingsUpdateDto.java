package com.example.healthassistant.dto;

import lombok.Data;

@Data
public class UserAiSettingsUpdateDto {
    private String textProvider;
    private String lmstudioBaseUrl;
    private String lmstudioModel;
    private String dashscopeApiKey;
    private String doubaoApiKey;
    private String pexelsApiKey;
}
