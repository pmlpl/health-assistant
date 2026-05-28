// ============================================================
// 这个文件负责：全局异常处理，统一返回 ApiResponse 格式
// ============================================================
package com.example.healthassistant.config;

import com.example.healthassistant.dto.ApiResponse;
import com.example.healthassistant.exception.AiNotConfiguredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        return ApiResponse.fail(code, ex.getReason() != null ? ex.getReason() : "请求被拒绝");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(400, ex.getMessage());
    }

    @ExceptionHandler(AiNotConfiguredException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiNotConfigured(AiNotConfiguredException ex) {
        return ApiResponse.fail(428, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("未处理的服务器异常", ex);
        return ApiResponse.fail(500, "服务器内部错误，请稍后重试");
    }
}
