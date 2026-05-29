package com.example.healthassistant.controller;

import com.example.healthassistant.dto.AiConnectionTestRequestDto;
import com.example.healthassistant.dto.ApiResponse;
import com.example.healthassistant.dto.UserAiSettingsDto;
import com.example.healthassistant.dto.UserAiSettingsUpdateDto;
import com.example.healthassistant.security.AuthSupport;
import com.example.healthassistant.service.AiConnectionTestService;
import com.example.healthassistant.service.UserAiSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me/ai-settings")
public class UserAiSettingsController {

    @Autowired
    private UserAiSettingsService userAiSettingsService;

    @Autowired
    private AiConnectionTestService aiConnectionTestService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserAiSettingsDto>> getSettings() {
        String userId = AuthSupport.currentUserId();
        return ApiResponse.ok(userAiSettingsService.toDto(userId));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserAiSettingsDto>> updateSettings(@RequestBody UserAiSettingsUpdateDto body) {
        String userId = AuthSupport.currentUserId();
        return ApiResponse.ok(userAiSettingsService.update(userId, body));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConnection(@RequestBody AiConnectionTestRequestDto body) {
        String userId = AuthSupport.currentUserId();
        return ApiResponse.ok(aiConnectionTestService.test(userId, body));
    }
}
