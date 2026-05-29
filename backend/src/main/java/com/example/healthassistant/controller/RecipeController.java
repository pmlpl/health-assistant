package com.example.healthassistant.controller;

import com.example.healthassistant.model.Recipe;
import com.example.healthassistant.repository.RecipeRepository;
import com.example.healthassistant.security.AuthSupport;
import com.example.healthassistant.service.RecipeImageStorageService;
import com.example.healthassistant.util.ImageUrlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeImageStorageService recipeImageStorageService;

    /**
     * 保存 AI 生成的食谱到数据库
     */
    @PostMapping("/save-ai-generated")
    public ResponseEntity<?> saveAiGeneratedRecipe(@RequestBody Map<String, Object> request) {
        return saveUserRecipe(request, "ai");
    }

    /** 用户手动创建食谱 */
    @PostMapping("/create")
    public ResponseEntity<?> createManualRecipe(@RequestBody Map<String, Object> request) {
        Object name = request.get("name");
        if (name == null || String.valueOf(name).isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "食谱名称不能为空");
            return ResponseEntity.badRequest().body(err);
        }
        return saveUserRecipe(request, "manual");
    }

    /** 手动创建时上传封面图 */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadRecipeImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("error", "请选择图片文件");
                return ResponseEntity.badRequest().body(err);
            }
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            if (!contentType.startsWith("image/")) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("error", "仅支持图片文件");
                return ResponseEntity.badRequest().body(err);
            }
            String url = recipeImageStorageService.storeManualUpload(
                    file.getBytes(), contentType);
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("imageUrl", url);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "上传失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /** 通用保存逻辑：AI / 手动创建共用 */
    private ResponseEntity<?> saveUserRecipe(Map<String, Object> request, String sourceTag) {
        try {
            Recipe recipe = buildRecipeFromRequest(request);
            applySourceTag(recipe, sourceTag);
            Recipe savedRecipe = recipeRepository.save(recipe);
            if (savedRecipe.getImageUrl() != null && savedRecipe.getName() != null) {
                recipeImageStorageService.registerImageUrl(
                        savedRecipe.getName(), savedRecipe.getImageUrl());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedRecipe.getId());
            response.put("imageUrl", savedRecipe.getImageUrl());
            response.put("message", "食谱保存成功");
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "保存食谱失败：" + e.getMessage());
            errorResponse.put("success", false);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Recipe buildRecipeFromRequest(Map<String, Object> request) throws IOException {
        Recipe recipe = new Recipe();
        recipe.setName(String.valueOf(request.get("name")).trim());
        recipe.setDescription(request.get("description") != null
                ? String.valueOf(request.get("description")) : null);
        recipe.setMealType(request.get("mealType") != null
                ? String.valueOf(request.get("mealType")) : null);
        recipe.setCreatedBy(AuthSupport.currentUserId());

        if (request.get("calories") != null && !String.valueOf(request.get("calories")).isBlank()) {
            recipe.setCalories(Double.parseDouble(request.get("calories").toString()));
        }
        if (request.get("protein") != null && !String.valueOf(request.get("protein")).isBlank()) {
            recipe.setProtein(Double.parseDouble(request.get("protein").toString()));
        }
        if (request.get("carbs") != null && !String.valueOf(request.get("carbs")).isBlank()) {
            recipe.setCarbs(Double.parseDouble(request.get("carbs").toString()));
        }
        if (request.get("fat") != null && !String.valueOf(request.get("fat")).isBlank()) {
            recipe.setFat(Double.parseDouble(request.get("fat").toString()));
        }

        String imageUrl = ImageUrlUtils.resolveImageUrl(request.get("imageUrl"), request.get("image"));
        if (imageUrl != null) {
            try {
                String persisted = recipeImageStorageService.persistSavedRecipeImage(imageUrl);
                recipe.setImageUrl(persisted != null ? persisted : imageUrl);
            } catch (IOException e) {
                System.err.println("食谱图片落盘失败，使用原 URL: " + e.getMessage());
                recipe.setImageUrl(imageUrl);
            }
        }

        if (request.get("ingredients") != null) {
            @SuppressWarnings("unchecked")
            List<String> ingredients = (List<String>) request.get("ingredients");
            recipe.setIngredientsList(String.join("|", ingredients));
        }

        if (request.get("instructions") != null) {
            @SuppressWarnings("unchecked")
            List<String> instructions = (List<String>) request.get("instructions");
            recipe.setInstructions(String.join("|", instructions));
        }

        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());
        recipe.setIsPublic(true);
        recipe.setSuitableForDiet(true);
        return recipe;
    }

    private static void applySourceTag(Recipe recipe, String sourceTag) {
        List<String> tags = new ArrayList<>();
        tags.add(sourceTag);
        recipe.setTags(tags);
    }

    /**
     * 获取当前用户的食谱（分页，默认每页 20 条）
     */
    @GetMapping("/my-ai-recipes")
    public ResponseEntity<Map<String, Object>> getMyRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String userId = AuthSupport.currentUserId();
            Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
            Page<Recipe> recipePage = recipeRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable);

            Map<String, Object> body = new HashMap<>();
            body.put("content", recipePage.getContent());
            body.put("totalElements", recipePage.getTotalElements());
            body.put("totalPages", recipePage.getTotalPages());
            body.put("page", recipePage.getNumber());
            body.put("size", recipePage.getSize());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新食谱图片（仅接受 URL，拒绝 Base64）
     */
    @PutMapping("/update-recipe-image/{id}")
    public ResponseEntity<?> updateRecipeImage(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            Recipe recipe = recipeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("食谱不存在"));

            if (recipe.getCreatedBy() != null) {
                AuthSupport.requireSelf(recipe.getCreatedBy());
            }

            String imageUrl = ImageUrlUtils.resolveImageUrl(
                    request.get("imageUrl"), request.get("image"));
            if (imageUrl == null && (request.get("image") != null || request.get("imageUrl") != null)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "请提供 HTTP(S) 图片 URL，不支持 Base64 内联存储");
                errorResponse.put("success", false);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            if (imageUrl != null) {
                recipe.setImageUrl(imageUrl);
            }
            recipe.setUpdatedAt(LocalDateTime.now());
            
            recipeRepository.save(recipe);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "图片更新成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "更新图片失败：" + e.getMessage());
            errorResponse.put("success", false);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 根据餐类型获取食谱（JOIN FETCH 标签）
     */
    @GetMapping("/meal-type/{mealType}")
    public ResponseEntity<List<Recipe>> getRecipesByMealType(
            @PathVariable String mealType) {
        try {
            List<Recipe> recipes = recipeRepository.findByMealTypeWithTags(mealType);
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id) {
        try {
            Recipe recipe = recipeRepository.findById(id).orElse(null);
            if (recipe != null && recipe.getCreatedBy() != null) {
                AuthSupport.requireSelf(recipe.getCreatedBy());
            }
            recipeRepository.deleteById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "食谱删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "删除食谱失败：" + e.getMessage());
            errorResponse.put("success", false);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
