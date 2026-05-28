package com.example.healthassistant.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResolvedAiConfig {
    private String textProvider;
    private String lmstudioBaseUrl;
    private String lmstudioModel;
    private String dashscopeApiKey;
    private String doubaoApiKey;
    private String pexelsApiKey;
    private boolean configured;
    private String activeTextBackend;
}
