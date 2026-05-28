package com.example.healthassistant.service;

import com.example.healthassistant.ai.ChatClientRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RecipeImageSearchService {

    private final ChatClientRouter chatClientRouter;
    private final RecipeImageStorageService storageService;
    private final UserAiSettingsService userAiSettingsService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    /** 搜图并落盘，返回本地 URL；失败返回 null */
    public String findAndStoreRecipeImage(String userId, String recipeName, List<String> ingredients) {
        String pexelsKey = userAiSettingsService.resolve(userId).getPexelsApiKey();
        if (pexelsKey == null || pexelsKey.isBlank()) {
            return null;
        }
        String query = buildSearchQuery(userId, recipeName);
        String remoteUrl = searchPexels(pexelsKey, query);
        if (remoteUrl == null) {
            return null;
        }
        return downloadAndStore(remoteUrl);
    }

    private String buildSearchQuery(String userId, String recipeName) {
        try {
            return chatClientRouter.complete(userId,
                    "你只输出一行英文美食摄影搜索关键词，不要解释、不要 markdown",
                    "菜名：" + recipeName).trim().replace("\"", "");
        } catch (Exception e) {
            return recipeName + " food";
        }
    }

    private String searchPexels(String apiKey, String query) {
        try {
            String url = "https://api.pexels.com/v1/search?query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&per_page=3&orientation=landscape";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode photos = objectMapper.readTree(resp.getBody()).path("photos");
            if (!photos.isArray() || photos.isEmpty()) {
                return null;
            }
            return photos.get(0).path("src").path("large").asText(null);
        } catch (Exception e) {
            System.err.println("Pexels 搜图失败: " + e.getMessage());
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
