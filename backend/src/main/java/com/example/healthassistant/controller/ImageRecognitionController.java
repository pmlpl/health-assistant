package com.example.healthassistant.controller;

import com.example.healthassistant.security.AuthSupport;
import com.example.healthassistant.service.ImageRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/image")
public class ImageRecognitionController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Autowired
    private ImageRecognitionService imageRecognitionService;

    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognizeImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "userId", required = false) String userId) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "图片文件不能为空"));
        }
        if (image.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("error", "图片大小不能超过 10MB"));
        }
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("error", "仅支持 JPG、PNG、WebP、GIF 格式"));
        }
        if (userId != null && !userId.isBlank()) {
            AuthSupport.requireSelf(userId);
        }
        try {
            Map<String, Object> result = imageRecognitionService.recognizeFoodInImage(image, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "图片识别失败，请稍后重试"));
        }
    }
}
