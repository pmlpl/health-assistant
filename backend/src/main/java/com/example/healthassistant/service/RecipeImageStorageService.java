package com.example.healthassistant.service;

import com.example.healthassistant.util.RemoteImageFetchUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 食谱图片落盘：生成预览在 recipes/，用户点击「添加」后永久保存到 recipes/saved/。
 * 同名食谱复用 by-name/ 与索引，避免重复 AI 生图。
 */
@Service
public class RecipeImageStorageService {

    private static final long MAX_BYTES = 8 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final Path recipeDir;
    private final Path savedDir;
    private final Path byNameDir;
    private final Path uploadRoot;
    private final Path indexFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> indexCache = new ConcurrentHashMap<>();

    public RecipeImageStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.recipeDir = uploadRoot.resolve("recipes");
        this.savedDir = recipeDir.resolve("saved");
        this.byNameDir = recipeDir.resolve("by-name");
        this.indexFile = recipeDir.resolve("recipe-image-index.json");
    }

    /** 按食谱名查找已落盘的本地配图（by-name、索引、saved、预览目录） */
    public Optional<String> findExistingImageUrl(String recipeName) {
        String key = normalizeRecipeKey(recipeName);
        if (key.isBlank()) {
            return Optional.empty();
        }
        loadIndexIfNeeded();

        String indexed = indexCache.get(key);
        if (indexed != null && fileExistsOnDisk(indexed)) {
            return Optional.of(indexed);
        }

        for (String ext : ALLOWED_EXT) {
            Path candidate = byNameDir.resolve(recipeFileName(key, ext));
            if (Files.exists(candidate)) {
                String url = "/uploads/recipes/by-name/" + candidate.getFileName();
                indexCache.put(key, url);
                persistIndexQuietly();
                return Optional.of(url);
            }
        }
        return Optional.empty();
    }

    /** AI 生成后按食谱名落盘（同名则直接返回已有路径） */
    public String storeForRecipe(String recipeName, byte[] imageBytes, String contentType) throws IOException {
        Optional<String> existing = findExistingImageUrl(recipeName);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        if (imageBytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("图片超过 8MB 限制");
        }

        String key = normalizeRecipeKey(recipeName);
        String ext = extensionFromContentType(contentType);
        Files.createDirectories(byNameDir);
        String filename = recipeFileName(key, ext);
        Path target = byNameDir.resolve(filename);
        if (!Files.exists(target)) {
            Files.write(target, imageBytes);
        }
        String url = "/uploads/recipes/by-name/" + filename;
        indexCache.put(key, url);
        persistIndexQuietly();
        return url;
    }

    /** 将已有 URL 登记到索引，便于下次按名复用 */
    public void registerImageUrl(String recipeName, String urlPath) {
        String key = normalizeRecipeKey(recipeName);
        if (key.isBlank() || urlPath == null || urlPath.isBlank()) {
            return;
        }
        loadIndexIfNeeded();
        indexCache.put(key, normalizeUploadPath(urlPath));
        persistIndexQuietly();
    }

    private static String normalizeUploadPath(String url) {
        String u = url.trim();
        int idx = u.indexOf("/uploads/recipes/");
        if (idx >= 0) {
            return u.substring(idx);
        }
        return u.startsWith("/uploads/") ? u : u;
    }

    /** 手动创建食谱时上传封面，直接写入 saved/ */
    public String storeManualUpload(byte[] imageBytes, String contentType) throws IOException {
        return storeToDir(savedDir, imageBytes, contentType, "/uploads/recipes/saved/");
    }

    public String store(byte[] imageBytes, String contentType) throws IOException {
        return storeToDir(recipeDir, imageBytes, contentType, "/uploads/recipes/");
    }

    /**
     * 用户点击「添加到我的食谱」时：将图片永久保存到 uploads/recipes/saved/。
     */
    public String persistSavedRecipeImage(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String trimmed = imageUrl.trim();
        if (trimmed.contains("/recipes/saved/")) {
            return trimmed;
        }
        if (trimmed.startsWith("/uploads/")) {
            Path source = uploadRoot.resolve(trimmed.substring("/uploads/".length()));
            if (Files.exists(source)) {
                return copyToSaved(source);
            }
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            byte[] bytes = downloadBytes(trimmed);
            if (bytes == null) {
                return null;
            }
            return storeToDir(savedDir, bytes, "image/jpeg", "/uploads/recipes/saved/");
        }
        return trimmed;
    }

    /** 食谱名规范化：去空白与标点，便于索引 */
    public static String normalizeRecipeKey(String recipeName) {
        if (recipeName == null) {
            return "";
        }
        return recipeName.trim().toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}\\p{IsPunctuation}]", "");
    }

    private static String recipeFileName(String recipeKey, String ext) {
        String hash = sha256Hex(recipeKey).substring(0, 16);
        return hash + ext;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private void loadIndexIfNeeded() {
        if (!indexCache.isEmpty()) {
            return;
        }
        if (!Files.exists(indexFile)) {
            return;
        }
        try {
            byte[] raw = Files.readAllBytes(indexFile);
            Map<String, String> loaded = objectMapper.readValue(raw, new TypeReference<>() {});
            indexCache.putAll(loaded);
        } catch (Exception e) {
            System.err.println("读取食谱配图索引失败: " + e.getMessage());
        }
    }

    private void persistIndexQuietly() {
        try {
            Files.createDirectories(recipeDir);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(indexFile.toFile(), indexCache);
        } catch (Exception e) {
            System.err.println("写入食谱配图索引失败: " + e.getMessage());
        }
    }

    private boolean fileExistsOnDisk(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            return false;
        }
        if (!urlPath.startsWith("/uploads/")) {
            return urlPath.startsWith("http://") || urlPath.startsWith("https://");
        }
        Path p = uploadRoot.resolve(urlPath.substring("/uploads/".length()));
        return Files.exists(p);
    }

    private String copyToSaved(Path source) throws IOException {
        Files.createDirectories(savedDir);
        String ext = extensionFromFilename(source.getFileName().toString());
        String filename = java.util.UUID.randomUUID() + ext;
        Path target = savedDir.resolve(filename);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/recipes/saved/" + filename;
    }

    private String storeToDir(Path dir, byte[] imageBytes, String contentType, String urlPrefix)
            throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        if (imageBytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("图片超过 8MB 限制");
        }
        String ext = extensionFromContentType(contentType);
        Files.createDirectories(dir);
        String filename = java.util.UUID.randomUUID() + ext;
        Path target = dir.resolve(filename);
        Files.write(target, imageBytes);
        return urlPrefix + filename;
    }

    private byte[] downloadBytes(String url) {
        byte[] bytes = RemoteImageFetchUtil.downloadFromSignedUrl(url);
        if (bytes == null || bytes.length == 0) {
            System.err.println("下载食谱图片失败: " + url);
        }
        return bytes;
    }

    private String extensionFromContentType(String contentType) {
        if (contentType != null && contentType.contains("png")) {
            return ".png";
        }
        if (contentType != null && contentType.contains("webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    private String extensionFromFilename(String name) {
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            String ext = name.substring(dot).toLowerCase();
            if (ALLOWED_EXT.contains(ext)) {
                return ext;
            }
        }
        return ".jpg";
    }
}
