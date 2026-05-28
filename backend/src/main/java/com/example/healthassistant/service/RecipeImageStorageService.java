package com.example.healthassistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class RecipeImageStorageService {

    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final Path recipeDir;

    public RecipeImageStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.recipeDir = Paths.get(uploadDir, "recipes").toAbsolutePath().normalize();
    }

    public String store(byte[] imageBytes, String contentType) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        if (imageBytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("图片超过 5MB 限制");
        }
        String ext = extensionFromContentType(contentType);
        Files.createDirectories(recipeDir);
        String filename = UUID.randomUUID() + ext;
        Path target = recipeDir.resolve(filename);
        Files.write(target, imageBytes);
        return "/uploads/recipes/" + filename;
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".jpg";
        }
        if (contentType.contains("png")) {
            return ".png";
        }
        if (contentType.contains("webp")) {
            return ".webp";
        }
        return ".jpg";
    }
}
