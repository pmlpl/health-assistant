package com.example.healthassistant.util;

/**
 * 图片 URL 校验：拒绝 Base64 内联数据入库，仅允许 HTTP(S) 或相对路径。
 */
public final class ImageUrlUtils {

    private ImageUrlUtils() {
    }

    /** 判断图片字段是否可写入数据库 */
    public static boolean isAcceptableImageValue(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (value.startsWith("data:")) {
            return false;
        }
        return value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("/");
    }

    /** 从请求中解析图片 URL，优先 imageUrl，兼容旧字段 image；Base64 返回 null */
    public static String resolveImageUrl(Object imageUrlField, Object legacyImageField) {
        String candidate = null;
        if (imageUrlField instanceof String url && !url.isBlank()) {
            candidate = url;
        } else if (legacyImageField instanceof String legacy && !legacy.isBlank()) {
            candidate = legacy;
        }
        if (candidate != null && !isAcceptableImageValue(candidate)) {
            return null;
        }
        return candidate;
    }
}
