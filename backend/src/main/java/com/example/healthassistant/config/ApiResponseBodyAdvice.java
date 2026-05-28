// ============================================================
// 这个文件负责：自动将 Controller 返回值包装为统一 ApiResponse 格式
// 已返回 ApiResponse 的接口不会被重复包装
// ============================================================
package com.example.healthassistant.config;

import com.example.healthassistant.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@RestControllerAdvice(basePackages = "com.example.healthassistant.controller")
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !isAlreadyApiResponse(returnType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse) {
            return body;
        }
        if (response instanceof ServletServerHttpResponse servletResponse) {
            int status = servletResponse.getServletResponse().getStatus();
            if (status >= 400) {
                return body;
            }
        }
        if (body == null) {
            return null;
        }
        return ApiResponse.success(body);
    }

    private boolean isAlreadyApiResponse(MethodParameter returnType) {
        Class<?> type = returnType.getParameterType();
        if (ApiResponse.class.isAssignableFrom(type)) {
            return true;
        }
        Type generic = returnType.getGenericParameterType();
        if (generic instanceof ParameterizedType parameterizedType) {
            Type raw = parameterizedType.getRawType();
            if (raw instanceof Class<?> rawClass && org.springframework.http.ResponseEntity.class.isAssignableFrom(rawClass)) {
                Type[] args = parameterizedType.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> argClass) {
                    return ApiResponse.class.isAssignableFrom(argClass);
                }
            }
        }
        return false;
    }
}
