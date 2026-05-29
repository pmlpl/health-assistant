package com.example.healthassistant.controller;

import com.example.healthassistant.dto.LoginRequestDto;
import com.example.healthassistant.dto.RecipeRecommendationRequestDto;
import com.example.healthassistant.dto.UserProfileDto;
import com.example.healthassistant.model.Recipe;
import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.service.QwenAIService;
import com.example.healthassistant.service.RecommendationService;
import com.example.healthassistant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private QwenAIService qwenAIService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestDto loginRequest) {
        UserProfile user = userService.login(loginRequest);
        Map<String, Object> response = new HashMap<>();

        // 临时测试：即使找不到用户也返回成功
        if (user != null || "testuser".equals(loginRequest.getUsername())) {
            // 如果找不到真实用户，创建一个临时用户对象
            if (user == null) {
                user = new UserProfile();
                user.setUserId(loginRequest.getUsername());
                user.setGender("M");
                user.setHealthGoal("减脂");
                user.setWeight(70.0);
            }

            response.put("success", true);
            response.put("message", "登录成功");
            response.put("user", user);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<UserProfile> createUserProfile(@RequestBody UserProfileDto profileDto) {
        UserProfile profile = userService.createOrUpdateUserProfile(profileDto);
        // 清除用户的 AI 会话历史，确保下次获取最新的个人档案信息
        qwenAIService.clearSessionHistory(profile.getUserId());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody LoginRequestDto registerRequest) {
        try {
            UserProfile newUser = userService.register(registerRequest.getUsername(), registerRequest.getPassword());
            Map<String, Object> response = new HashMap<>();

            if (newUser != null) {
                response.put("success", true);
                response.put("message", "注册成功");
                response.put("user", newUser);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户名已存在");
                return ResponseEntity.status(409).body(response); // 409 Conflict
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "注册失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 查找用户
            UserProfile user = userService.getUserProfile(username);
            
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            // 直接重置密码为默认密码 12345678
            boolean resetSuccess = userService.resetPassword(username, null, "12345678");
            
            if (resetSuccess) {
                response.put("success", true);
                response.put("message", "密码已重置为默认密码 12345678，请重新登录");
            } else {
                response.put("success", false);
                response.put("message", "密码重置失败");
                return ResponseEntity.internalServerError().body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "重置密码失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable String userId) {
        UserProfile profile = userService.getUserProfile(userId);
        if (profile != null) {
            return ResponseEntity.ok(profile);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendations(@RequestBody RecipeRecommendationRequestDto request) {
        Map<String, Object> recommendations = recommendationService.getRecipeRecommendations(
                request.getUserId(),
                request.getMealType());
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
    
        Map<String, Object> response = new HashMap<>();

        try {
            // 清除用户相关的会话数据（如果有）
            // 这里可以添加清除用户缓存、会话等逻辑

            response.put("success", true);
            response.put("message", "注销成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "注销失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean deleted = userService.deleteUser(userId);

            if (deleted) {
                response.put("success", true);
                response.put("message", "用户删除成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户不存在或删除失败");
                return ResponseEntity.status(404).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除用户失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/update-username/{userId}")
    public ResponseEntity<Map<String, Object>> updateUsername(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        String newUsername = request.get("newUsername");
        
        Map<String, Object> response = new HashMap<>();

        try {
            boolean updated = userService.updateUsername(userId, newUsername);

            if (updated) {
                response.put("success", true);
                response.put("message", "用户名修改成功");
                response.put("newUsername", newUsername);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户名已存在或修改失败");
                return ResponseEntity.status(409).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "修改用户名失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/update-password/{userId}")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        System.out.println("收到修改密码请求: " + userId);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean updated = userService.updatePassword(userId, oldPassword, newPassword);

            if (updated) {
                response.put("success", true);
                response.put("message", "密码修改成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "原密码错误或修改失败");
                return ResponseEntity.status(401).body(response);
            }

        } catch (Exception e) {
            System.err.println("修改密码失败: " + e.getMessage());
            response.put("success", false);
            response.put("message", "修改密码失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
