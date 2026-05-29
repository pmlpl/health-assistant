package com.example.healthassistant.controller;

import com.example.healthassistant.service.ImageRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/image")
public class ImageRecognitionController {

    @Autowired
    private ImageRecognitionService imageRecognitionService;

    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognizeImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "userId", required = false) String userId) {
        try {
            if (image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "图片文件不能为空"));
            }

            // 调用服务进行图片识别
            Map<String, Object> result = imageRecognitionService.recognizeFoodInImage(image, userId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "图片识别失败: " + e.getMessage()));
        }
    }
}
