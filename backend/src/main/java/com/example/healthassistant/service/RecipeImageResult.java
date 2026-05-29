package com.example.healthassistant.service;

/**
 * 食谱配图结果：成功时带本地 URL，失败时带 imageStatus 供前端展示原因。
 */
public record RecipeImageResult(String imageUrl, String imageStatus) {

    public static RecipeImageResult ok(String imageUrl) {
        return new RecipeImageResult(imageUrl, null);
    }

    public static RecipeImageResult fail(String imageStatus) {
        return new RecipeImageResult(null, imageStatus);
    }

    public boolean isSuccess() {
        return imageUrl != null && !imageUrl.isBlank();
    }
}
