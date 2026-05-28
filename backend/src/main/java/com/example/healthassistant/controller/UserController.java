package com.example.healthassistant.controller;

import com.example.healthassistant.dto.ApiResponse;
import com.example.healthassistant.dto.LoginRequestDto;
import com.example.healthassistant.dto.RecipeRecommendationRequestDto;
import com.example.healthassistant.dto.UserProfileDto;
import com.example.healthassistant.dto.UserProfileResponseDto;
import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.security.AuthSupport;
import com.example.healthassistant.security.JwtTokenProvider;
import com.example.healthassistant.service.QwenAIService;
import com.example.healthassistant.service.RecommendationService;
import com.example.healthassistant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private QwenAIService qwenAIService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequestDto loginRequest) {
        UserProfile user = userService.login(loginRequest);
        if (user == null) {
            return ApiResponse.fail(401, "用户名或密码错误");
        }
        String token = jwtTokenProvider.generateToken(user.getUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", UserProfileResponseDto.from(user));
        return ApiResponse.ok("登录成功", data);
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> createUserProfile(@RequestBody UserProfileDto profileDto) {
        AuthSupport.requireSelf(profileDto.getUserId());
        UserProfile profile = userService.createOrUpdateUserProfile(profileDto);
        qwenAIService.clearSessionHistory(profile.getUserId());
        return ApiResponse.ok(UserProfileResponseDto.from(profile));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody LoginRequestDto registerRequest) {
        UserProfile newUser = userService.register(registerRequest.getUsername(), registerRequest.getPassword());
        if (newUser == null) {
            return ApiResponse.fail(409, "用户名已存在");
        }
        String token = jwtTokenProvider.generateToken(newUser.getUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", UserProfileResponseDto.from(newUser));
        return ApiResponse.ok("注册成功", data);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String, String> request) {
        return ApiResponse.fail(400, "暂不支持在线重置密码，请登录后在个人中心使用「修改密码」功能");
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getUserProfile(@PathVariable String userId) {
        AuthSupport.requireSelf(userId);
        UserProfile profile = userService.getUserProfile(userId);
        if (profile == null) {
            return ApiResponse.fail(404, "用户不存在");
        }
        return ApiResponse.ok(UserProfileResponseDto.from(profile));
    }

    @PostMapping("/recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendations(
            @RequestBody RecipeRecommendationRequestDto request) {
        AuthSupport.requireSelf(request.getUserId());
        Map<String, Object> recommendations = recommendationService.getRecipeRecommendations(
                request.getUserId(), request.getMealType());
        return ApiResponse.ok(recommendations);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody Map<String, String> request) {
        AuthSupport.requireSelf(request.get("userId"));
        return ApiResponse.ok("注销成功", null);
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        AuthSupport.requireSelf(userId);
        if (userService.deleteUser(userId)) {
            return ApiResponse.ok("用户删除成功", null);
        }
        return ApiResponse.fail(404, "用户不存在或删除失败");
    }

    @PutMapping("/update-username/{userId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateUsername(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        AuthSupport.requireSelf(userId);
        String newUsername = request.get("newUsername");
        if (userService.updateUsername(userId, newUsername)) {
            return ApiResponse.ok("用户名修改成功", Map.of("newUsername", newUsername));
        }
        return ApiResponse.fail(409, "用户名已存在或修改失败");
    }

    @PutMapping("/update-password/{userId}")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        AuthSupport.requireSelf(userId);
        boolean updated = userService.updatePassword(
                userId, request.get("oldPassword"), request.get("newPassword"));
        if (updated) {
            return ApiResponse.ok("密码修改成功", null);
        }
        return ApiResponse.fail(401, "原密码错误或修改失败");
    }
}
