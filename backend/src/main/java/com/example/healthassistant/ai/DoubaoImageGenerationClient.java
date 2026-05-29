package com.example.healthassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.healthassistant.util.RemoteImageFetchUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 豆包 Seedream 食谱配图（仅火山方舟，不走 DeepSeek）。
 */
@Component
public class DoubaoImageGenerationClient {

    public static final String ARK_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DoubaoImageGenerationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Seedream 文生图官方模型 ID（ep- 接入点若非图片模型会 400，需回退到此列表） */
    public static final List<String> SEEDREAM_MODEL_FALLBACKS = List.of(
            "doubao-seedream-5-0-lite-260128",
            "doubao-seedream-5-0-260128",
            "doubao-seedream-4-5-251128"
    );

    /** 根据提示词生成图片字节（优先 b64_json，避免 TOS 预签名 URL 下载鉴权问题） */
    public GeneratedImage generateImage(String apiKey, String model, String prompt, String size) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("豆包 API Key 未配置");
        }
        String primary = normalizeSeedreamModel(model);
        List<String> candidates = buildModelCandidates(primary);
        IllegalStateException lastError = null;

        for (String modelId : candidates) {
            try {
                return invokeGeneration(apiKey, modelId, prompt, size);
            } catch (IllegalStateException e) {
                lastError = e;
                if (!shouldRetryWithFallbackModel(e.getMessage())) {
                    throw e;
                }
                System.err.println("[RecipeImage] model=" + modelId + " 不支持生图，尝试下一模型…");
            }
        }
        throw lastError != null ? lastError
                : new IllegalStateException("豆包 Seedream 生图失败：无可用模型");
    }

    /** 组装尝试顺序：配置项 → 官方 Seedream 模型 ID */
    private static List<String> buildModelCandidates(String primary) {
        List<String> out = new ArrayList<>();
        if (primary != null && !primary.isBlank()) {
            out.add(primary);
        }
        for (String fb : SEEDREAM_MODEL_FALLBACKS) {
            if (!out.contains(fb)) {
                out.add(fb);
            }
        }
        return out;
    }

    private static boolean shouldRetryWithFallbackModel(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("image generation is only supported")
                || message.contains("InvalidParameter")
                || message.contains("not valid");
    }

    private GeneratedImage invokeGeneration(String apiKey, String modelId, String prompt, String size) {
        String imageSize = resolveSize(modelId, size);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelId);
        body.put("prompt", prompt);
        body.put("size", imageSize);
        // b64_json 直接返回图片，避免 TOS 临时 URL 服务端下载鉴权失败
        body.put("response_format", "b64_json");
        body.put("watermark", false);
        body.put("sequential_image_generation", "disabled");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    ARK_BASE_URL + "/images/generations",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
            responseBody = response.getBody();
        } catch (HttpClientErrorException e) {
            String errBody = e.getResponseBodyAsString();
            String errMsg = errBody != null ? errBody : e.getMessage();
            throw new IllegalStateException("豆包 Seedream 生图失败（model=" + modelId + "）: " + errMsg);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) {
                JsonNode first = data.get(0);
                String b64 = first.path("b64_json").asText(null);
                if (b64 != null && !b64.isBlank()) {
                    byte[] bytes = RemoteImageFetchUtil.decodeBase64Image(b64);
                    if (bytes != null && bytes.length > 0) {
                        System.out.println("[RecipeImage] 生图成功 model=" + modelId + " (b64_json)");
                        return new GeneratedImage(bytes, "image/jpeg");
                    }
                }
                // 回退：部分响应仍带 url
                String url = first.path("url").asText(null);
                if (url != null && !url.isBlank()) {
                    byte[] bytes = RemoteImageFetchUtil.downloadFromSignedUrl(url);
                    if (bytes != null && bytes.length > 0) {
                        System.out.println("[RecipeImage] 生图成功 model=" + modelId + " (url fallback)");
                        return new GeneratedImage(bytes, "image/jpeg");
                    }
                    throw new IllegalStateException("豆包 Seedream 生图 URL 下载失败（model=" + modelId + "）");
                }
            }
            String err = root.path("error").path("message").asText("未知错误");
            throw new IllegalStateException("豆包 Seedream 生图失败（model=" + modelId + "）: " + err);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析豆包生图响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将控制台展示名转为方舟 API 模型 ID。
     * 例：Doubao-Seedream-5.0-lite → doubao-seedream-5-0-lite
     * 若控制台为 ep- 接入点，原样使用。
     */
    public static String normalizeSeedreamModel(String model) {
        if (model == null || model.isBlank()) {
            return "doubao-seedream-5-0-lite";
        }
        String m = model.trim();
        if (m.startsWith("ep-")) {
            return m;
        }
        String lower = m.toLowerCase().replace('.', '-').replace('_', '-');
        if (lower.contains("seedream") && lower.contains("5") && lower.contains("lite")) {
            return "doubao-seedream-5-0-lite-260128";
        }
        if (lower.contains("seedream") && lower.contains("4")) {
            return "doubao-seedream-4-5-251128";
        }
        return m;
    }

    /** Seedream 5 推荐 2K；4 系可用像素尺寸 */
    private static String resolveSize(String modelId, String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        if (modelId.contains("5-0") || modelId.contains("5.0")) {
            return "2K";
        }
        return "1024x1024";
    }
}
