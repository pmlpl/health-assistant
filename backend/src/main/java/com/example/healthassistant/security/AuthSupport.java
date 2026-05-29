// ============================================================
// 这个文件负责：获取当前登录用户、校验资源归属（防 IDOR）
// Controller 在处理带 userId 的请求时应调用 requireSelf
// ============================================================
package com.example.healthassistant.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class AuthSupport {

    private AuthSupport() {
    }

    /**
     * 获取当前 JWT 对应的 userId
     */
    public static String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        return authentication.getName();
    }

    /**
     * 校验路径或请求体中的 userId 必须与当前登录用户一致
     */
    public static void requireSelf(String requestedUserId) {
        if (requestedUserId == null || requestedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户ID不能为空");
        }
        if (!currentUserId().equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该用户数据");
        }
    }
}
