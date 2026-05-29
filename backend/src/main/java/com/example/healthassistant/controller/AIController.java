package com.example.healthassistant.controller;

import com.example.healthassistant.ai.ChatClientRouter;
import com.example.healthassistant.exception.AiNotConfiguredException;
import com.example.healthassistant.security.AuthSupport;
import com.example.healthassistant.service.HealthAiService;
import com.example.healthassistant.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private HealthAiService healthAiService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private ChatClientRouter chatClientRouter;

    @Autowired
    @Qualifier("aiConsultStreamExecutor")
    private AsyncTaskExecutor aiConsultStreamExecutor;

    @Value("${ai.consult.stream-enabled:true}")
    private boolean streamEnabled;

    @Value("${ai.consult.sse-timeout-ms:600000}")
    private long consultSseTimeoutMs;

    @PostMapping("/nutrition-advice")
    public ResponseEntity<Map<String, Object>> getNutritionAdvice(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String query = (String) request.get("query");

            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "用户ID不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            AuthSupport.requireSelf(userId);

            if (query == null || query.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "查询内容不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String advice = healthAiService.getNutritionAdvice(userId, query);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("query", query);
            response.put("advice", advice);
            response.put("aiBackend", chatClientRouter.getLastUsedBackend());
            response.put("aiModel", chatClientRouter.getLastUsedModel());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "处理请求时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping(value = "/nutrition-advice/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNutritionAdvice(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        String query = (String) request.get("query");
        if (userId == null || userId.isBlank()) {
            SseEmitter err = new SseEmitter(0L);
            err.completeWithError(new IllegalArgumentException("用户ID不能为空"));
            return err;
        }
        AuthSupport.requireSelf(userId);
        if (query == null || query.isBlank()) {
            SseEmitter err = new SseEmitter(0L);
            err.completeWithError(new IllegalArgumentException("查询内容不能为空"));
            return err;
        }
        SseEmitter emitter = new SseEmitter(consultSseTimeoutMs);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        String trimmedQuery = query.trim();

        // 超时/异常时主动结束 SSE，避免前端 fetch 永久挂起
        emitter.onTimeout(() -> completeSseWithError(emitter, "AI 咨询流式响应超时，请重试或关闭流式模式"));
        emitter.onError(ex -> completeSseWithError(emitter,
                ex.getMessage() != null ? ex.getMessage() : "流式连接异常"));

        // 在返回 SseEmitter 前立即发送 ready，让代理/浏览器尽快收到首字节
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (Exception readyEx) {
            emitter.completeWithError(readyEx);
            return emitter;
        }

        aiConsultStreamExecutor.execute(() -> {
            SecurityContextHolder.setContext(securityContext);
            try {
                System.out.println("[AI] SSE 异步任务开始 userId=" + userId);
                healthAiService.streamNutritionAdvice(userId, trimmedQuery, emitter);
            } catch (Exception asyncEx) {
                System.err.println("[AI] SSE 异步任务异常: " + asyncEx.getMessage());
                completeSseWithError(emitter,
                        asyncEx.getMessage() != null ? asyncEx.getMessage() : "AI 咨询失败");
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        return emitter;
    }

    @GetMapping("/consult-stream-enabled")
    public ResponseEntity<Map<String, Object>> consultStreamEnabled() {
        Map<String, Object> body = new HashMap<>();
        body.put("streamEnabled", streamEnabled);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/recommend-recipes")
    public ResponseEntity<Map<String, Object>> recommendRecipes(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String mealType = request.get("mealType");
        AuthSupport.requireSelf(userId);
        try {
            Map<String, Object> recommendations = recommendationService.getRecipeRecommendations(userId, mealType);
            return ResponseEntity.ok(recommendations);
        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "食谱推荐失败: " + e.getMessage());
            errorResponse.put("success", false);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/session/{userId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String userId) {
        AuthSupport.requireSelf(userId);
        try {
            healthAiService.clearSessionHistory(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "用户 " + userId + " 的会话历史已清除");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "清除会话时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/session/history/{userId}")
    public ResponseEntity<Map<String, Object>> getSessionHistory(@PathVariable String userId) {
        AuthSupport.requireSelf(userId);
        try {
            int historyLength = healthAiService.getSessionHistoryLength(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("historyLength", historyLength);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取会话历史时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/analyze-fitness-workout/{userId}")
    public ResponseEntity<Map<String, Object>> analyzeFitnessWorkout(
            @PathVariable String userId,
            @RequestBody Map<String, Object> workoutData) {
        AuthSupport.requireSelf(userId);
        try {
            String analysis = healthAiService.analyzeFitnessWorkout(userId, workoutData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysis", analysis);
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "健身分析失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /** SSE 超时或错误时向前端推送 error 事件并关闭连接 */
    private static void completeSseWithError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
            emitter.complete();
        } catch (Exception ignored) {
            try {
                emitter.complete();
            } catch (Exception ignoredAgain) {
            }
        }
    }

    @PostMapping("/mental-health")
    public ResponseEntity<Map<String, Object>> getMentalHealthAdvice(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String message = (String) request.get("message");

            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "用户ID不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            AuthSupport.requireSelf(userId);

            if (message == null || message.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "消息内容不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String response = healthAiService.getMentalHealthAdvice(userId, message);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", true);
            responseMap.put("response", response);
            responseMap.put("userId", userId);

            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "心理健康咨询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
