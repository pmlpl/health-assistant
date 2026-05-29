package com.example.healthassistant.service;

import com.example.healthassistant.ai.ChatClientRouter;
import com.example.healthassistant.config.EnvConfig;
import com.example.healthassistant.exception.AiNotConfiguredException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RecipeImageSearchService {

    private final ChatClientRouter chatClientRouter;
    private final RecipeImageStorageService storageService;
    private final UserAiSettingsService userAiSettingsService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pexels.api.key:}")
    private String pexelsFromConfig;

    /** 为 true 时用 LLM 生成英文搜图词；false 时仅用菜名/食材（更快） */
    @Value("${ai.recipe.image-use-llm-query:true}")
    private boolean useLlmImageQuery;

    public RecipeImageSearchService(
            ChatClientRouter chatClientRouter,
            RecipeImageStorageService storageService,
            UserAiSettingsService userAiSettingsService,
            RestTemplate restTemplate) {
        this.chatClientRouter = chatClientRouter;
        this.storageService = storageService;
        this.userAiSettingsService = userAiSettingsService;
        this.restTemplate = restTemplate;
    }

    public RecipeImageResult findAndStoreRecipeImage(String userId, String recipeName, List<String> ingredients) {
        String pexelsKey = resolvePexelsKey(userId);
        if (pexelsKey == null || pexelsKey.isBlank()) {
            return RecipeImageResult.fail("NO_PEXELS_KEY");
        }

        List<String> queries = buildSearchQueries(userId, recipeName, ingredients);
        String remoteUrl = null;
        for (String query : queries) {
            remoteUrl = searchPexels(pexelsKey, query);
            if (remoteUrl != null) {
                break;
            }
        }
        if (remoteUrl == null) {
            return RecipeImageResult.fail("PEXELS_NO_RESULT");
        }

        String stored = downloadAndStore(remoteUrl);
        if (stored == null) {
            return RecipeImageResult.fail("DOWNLOAD_FAILED");
        }
        return RecipeImageResult.ok(stored);
    }

    /** 用户 AI 设置 > application.properties / 环境变量 > .env */
    private String resolvePexelsKey(String userId) {
        String userKey = userAiSettingsService.resolve(userId).getPexelsApiKey();
        if (userKey != null && !userKey.isBlank()) {
            return userKey.trim();
        }
        String fromEnv = EnvConfig.getPexelsApiKey();
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        if (pexelsFromConfig != null && !pexelsFromConfig.isBlank()) {
            return pexelsFromConfig.trim();
        }
        return null;
    }

    private List<String> buildSearchQueries(String userId, String recipeName, List<String> ingredients) {
        Set<String> queries = new LinkedHashSet<>();
        if (useLlmImageQuery) {
            try {
                String llm = chatClientRouter.complete(userId,
                        "你只输出一行英文美食摄影搜索关键词，不要解释、不要 markdown",
                        "菜名：" + (recipeName != null ? recipeName : "meal")).trim().replace("\"", "");
                if (!llm.isBlank()) {
                    queries.add(llm);
                }
            } catch (AiNotConfiguredException ignored) {
                // 未配置文本 AI 时跳过 LLM 关键词
            } catch (Exception e) {
                System.err.println("LLM 搜图关键词失败: " + e.getMessage());
            }
        }
        if (recipeName != null && !recipeName.isBlank()) {
            queries.add(recipeName + " food");
            queries.add(recipeName + " dish");
        }
        if (ingredients != null) {
            for (String ing : ingredients) {
                if (ing != null && !ing.isBlank()) {
                    queries.add(ing.trim() + " food");
                    if (queries.size() >= 5) {
                        break;
                    }
                }
            }
        }
        queries.add("healthy food plate");
        return new ArrayList<>(queries);
    }

    private String searchPexels(String apiKey, String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        try {
            String url = "https://api.pexels.com/v1/search?query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&per_page=5&orientation=landscape";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey.trim());
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                System.err.println("Pexels HTTP " + resp.getStatusCode() + " query=" + query);
                return null;
            }
            JsonNode photos = objectMapper.readTree(resp.getBody()).path("photos");
            if (!photos.isArray() || photos.isEmpty()) {
                return null;
            }
            for (JsonNode photo : photos) {
                String large = photo.path("src").path("large").asText(null);
                if (large != null && !large.isBlank()) {
                    return large;
                }
                String medium = photo.path("src").path("medium").asText(null);
                if (medium != null && !medium.isBlank()) {
                    return medium;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Pexels 搜图失败 [" + query + "]: " + e.getMessage());
            return null;
        }
    }

    private String downloadAndStore(String imageUrl) {
        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    imageUrl, HttpMethod.GET, null, byte[].class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }
            String contentType = resp.getHeaders().getContentType() != null
                    ? resp.getHeaders().getContentType().toString() : "image/jpeg";
            return storageService.store(resp.getBody(), contentType);
        } catch (Exception e) {
            System.err.println("下载食谱图片失败: " + e.getMessage());
            return null;
        }
    }
}
