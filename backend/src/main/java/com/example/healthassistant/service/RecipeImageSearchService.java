package com.example.healthassistant.service;



import com.example.healthassistant.ai.DoubaoImageGenerationClient;

import com.example.healthassistant.ai.GeneratedImage;

import com.example.healthassistant.ai.ResolvedApiKey;

import com.example.healthassistant.config.ApiKeyResolver;

import com.example.healthassistant.exception.AiQuotaExceededException;

import com.example.healthassistant.model.Recipe;

import com.example.healthassistant.repository.RecipeRepository;

import com.example.healthassistant.service.PlatformAiQuotaService.UsageKind;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;



import java.util.List;

import java.util.Optional;



/**

 * AI 食谱配图：优先复用 uploads/ 已有同名配图，否则豆包 Seedream 文生图。

 */

@Service

public class RecipeImageSearchService {



    private final ApiKeyResolver apiKeyResolver;

    private final DoubaoImageGenerationClient doubaoImageGenerationClient;

    private final RecipeImageStorageService storageService;

    private final RecipeRepository recipeRepository;



    @Value("${doubao.image.model.name:doubao-seedream-5-0-lite}")

    private String defaultImageModel;



    @Value("${doubao.image.size:2K}")

    private String defaultImageSize;



    public RecipeImageSearchService(

            ApiKeyResolver apiKeyResolver,

            DoubaoImageGenerationClient doubaoImageGenerationClient,

            RecipeImageStorageService storageService,

            RecipeRepository recipeRepository) {

        this.apiKeyResolver = apiKeyResolver;

        this.doubaoImageGenerationClient = doubaoImageGenerationClient;

        this.storageService = storageService;

        this.recipeRepository = recipeRepository;

    }



    public RecipeImageResult findAndStoreRecipeImage(String userId, String recipeName, List<String> ingredients) {

        try {

            Optional<String> cached = resolveExistingImage(recipeName);

            if (cached.isPresent()) {

                System.out.println("[RecipeImage] 复用已有配图 recipe=" + recipeName + " url=" + cached.get());

                return RecipeImageResult.ok(cached.get());

            }



            ResolvedApiKey keyRef = apiKeyResolver.resolveDoubaoForRecipeImage(userId);

            if (!keyRef.isPresent()) {

                return RecipeImageResult.fail("NO_DOUBAO_KEY");

            }



            String prompt = buildImagePrompt(recipeName, ingredients);

            String model = DoubaoImageGenerationClient.normalizeSeedreamModel(defaultImageModel);

            System.out.println("[RecipeImage] 豆包 Seedream 生图 model=" + model + " recipe=" + recipeName);

            GeneratedImage generated = doubaoImageGenerationClient.generateImage(

                    keyRef.key(), model, prompt, defaultImageSize);

            apiKeyResolver.recordPlatformUsageIfNeeded(userId, keyRef, UsageKind.RECIPE_IMAGE);



            String stored = storageService.storeForRecipe(recipeName, generated.bytes(), generated.contentType());

            if (stored != null) {

                return RecipeImageResult.ok(stored);

            }

            return RecipeImageResult.fail("GENERATION_FAILED");

        } catch (AiQuotaExceededException e) {

            return RecipeImageResult.fail("QUOTA_EXCEEDED");

        } catch (Exception e) {

            System.err.println("豆包食谱生图失败: " + e.getMessage());

            return RecipeImageResult.fail("GENERATION_FAILED");

        }

    }



    /** 数据库 + uploads 索引 + by-name 目录 */

    private Optional<String> resolveExistingImage(String recipeName) {

        if (recipeName == null || recipeName.isBlank()) {

            return Optional.empty();

        }

        Optional<Recipe> fromDb = recipeRepository.findFirstByNameIgnoreCaseAndImageUrlIsNotNull(recipeName.trim());

        if (fromDb.isPresent()) {
            String url = fromDb.get().getImageUrl();
            if (isReusableLocalUrl(url)) {
                String normalized = normalizeUploadUrl(url);
                storageService.registerImageUrl(recipeName, normalized);
                return Optional.of(normalized);
            }
        }

        return storageService.findExistingImageUrl(recipeName);

    }



    private static boolean isReusableLocalUrl(String url) {

        if (url == null || url.isBlank()) {

            return false;

        }

        String u = url.trim();

        return u.startsWith("/uploads/recipes/");

    }



    private static String normalizeUploadUrl(String url) {

        String u = url.trim();

        if (u.startsWith("/uploads/")) {

            return u;

        }

        int idx = u.indexOf("/uploads/recipes/");

        if (idx >= 0) {

            return u.substring(idx);

        }

        return u;

    }



    private String buildImagePrompt(String recipeName, List<String> ingredients) {

        StringBuilder sb = new StringBuilder();

        sb.append("Professional food photography, appetizing, well-lit, on a clean plate, ");

        if (recipeName != null && !recipeName.isBlank()) {

            sb.append("dish: ").append(recipeName.trim()).append(", ");

        }

        if (ingredients != null && !ingredients.isEmpty()) {

            sb.append("main ingredients: ").append(String.join(", ", ingredients.subList(0, Math.min(5, ingredients.size()))));

            sb.append(", ");

        }

        sb.append("high quality, realistic, no text, no watermark");

        return sb.toString();

    }

}


