package com.example.healthassistant.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 远程图片拉取：TOS 预签名 URL 不能带 Authorization 头，需裸 GET。
 */
public final class RemoteImageFetchUtil {

    private RemoteImageFetchUtil() {
    }

    /** 从 https 预签名 URL 下载图片字节（不附加 Authorization） */
    public static byte[] downloadFromSignedUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url.trim()).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(120_000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "image/*,*/*");
            conn.setRequestProperty("User-Agent", "HealthAssistant/1.0");

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                return null;
            }
            try (InputStream in = conn.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** 解析 Seedream b64_json（支持带 data:image/...;base64, 前缀） */
    public static byte[] decodeBase64Image(String b64) {
        if (b64 == null || b64.isBlank()) {
            return null;
        }
        String payload = b64.trim();
        int comma = payload.indexOf(',');
        if (payload.startsWith("data:") && comma > 0) {
            payload = payload.substring(comma + 1);
        }
        return Base64.getDecoder().decode(payload.getBytes(StandardCharsets.UTF_8));
    }
}
