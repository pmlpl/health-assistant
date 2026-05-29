package com.example.healthassistant.controller;

import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health-tips")
@CrossOrigin(origins = "*")
public class HealthTipsController {

    @Autowired
    private UserService userService;

    @GetMapping("/personalized/{userId}")
    public ResponseEntity<Map<String, Object>> getPersonalizedTips(@PathVariable String userId) {
        UserProfile profile = userService.getUserProfile(userId);
        Map<String, Object> tips = new HashMap<>();

        if (profile != null) {
            tips.put("healthGoal", profile.getHealthGoal());
            tips.put("tip", generateTipByGoal(profile.getHealthGoal()));
            tips.put("nutritionalTargets", Map.of(
                    "calories", profile.getTargetCalories(),
                    "protein", profile.getTargetProtein(),
                    "carbs", profile.getTargetCarbs(),
                    "fat", profile.getTargetFat()
            ));
        } else {
            tips.put("tip", "请先完善您的健康档案以获取个性化建议");
        }

        return ResponseEntity.ok(tips);
    }

    private String generateTipByGoal(String healthGoal) {
        switch (healthGoal) {
            case "减脂":
                return "减脂期间建议采用少食多餐的方式，每餐控制在300-400卡路里，增加蛋白质摄入以保持肌肉量。";
            case "增肌":
                return "增肌期间需保证充足的蛋白质摄入，训练后30分钟内补充蛋白质效果最佳。";
            case "控糖":
                return "控糖饮食应选择低升糖指数食物，如燕麦、藜麦等全谷物，避免精制糖和高糖食品。";
            case "养胃":
                return "养胃饮食应定时定量，选择易消化的食物，避免过冷、过热、辛辣刺激的食物。";
            default:
                return "均衡饮食是健康的基础，注意荤素搭配，每天摄入12种以上食物。";
        }
    }
}
