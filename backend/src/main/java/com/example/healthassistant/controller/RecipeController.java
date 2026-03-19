package com.example.healthassistant.controller;

import com.example.healthassistant.model.Recipe;
import com.example.healthassistant.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    @Autowired
    private RecipeRepository recipeRepository;

    /**
     * 保存 AI 生成的食谱到数据库
     */
    @PostMapping("/save-ai-generated")
    public ResponseEntity<?> saveAiGeneratedRecipe(@RequestBody Map<String, Object> request) {
        try {
            Recipe recipe = new Recipe();
            
            // 设置基本信息
            recipe.setName((String) request.get("name"));
            recipe.setDescription((String) request.get("description"));
            recipe.setMealType((String) request.get("mealType"));
            
            // 设置营养成分
            if (request.get("calories") != null) {
                recipe.setCalories(Double.parseDouble(request.get("calories").toString()));
            }
            if (request.get("protein") != null) {
                recipe.setProtein(Double.parseDouble(request.get("protein").toString()));
            }
            if (request.get("carbs") != null) {
                recipe.setCarbs(Double.parseDouble(request.get("carbs").toString()));
            }
            if (request.get("fat") != null) {
                recipe.setFat(Double.parseDouble(request.get("fat").toString()));
            }
            
            // 设置图片（Base64 格式）
            if (request.get("image") != null) {
                recipe.setImageUrl((String) request.get("image"));
            }
            
            // 将食材列表转换为 JSON 字符串
            if (request.get("ingredients") != null) {
                @SuppressWarnings("unchecked")
                List<String> ingredients = (List<String>) request.get("ingredients");
                // 使用 JSON 格式存储，方便前端解析
                recipe.setIngredientsList(String.join("|", ingredients));
            }
            
            // 将制作步骤转换为 JSON 字符串
            if (request.get("instructions") != null) {
                @SuppressWarnings("unchecked")
                List<String> instructions = (List<String>) request.get("instructions");
                // 使用 JSON 格式存储，方便前端解析
                recipe.setInstructions(String.join("|", instructions));
            }
            
            // 设置其他默认值
            recipe.setCreatedAt(LocalDateTime.now());
            recipe.setUpdatedAt(LocalDateTime.now());
            recipe.setIsPublic(true);
            recipe.setSuitableForDiet(true);
            
            // 保存到数据库
            Recipe savedRecipe = recipeRepository.save(recipe);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedRecipe.getId());
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

    /**
     * 获取用户的所有食谱（从数据库读取）
     */
    @GetMapping("/my-ai-recipes")
    public ResponseEntity<List<Recipe>> getMyRecipes() {
        try {
            // 获取所有食谱，按创建时间倒序排列
            List<Recipe> recipes = recipeRepository.findAll();
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新食谱图片到数据库
     */
    @PutMapping("/update-recipe-image/{id}")
    public ResponseEntity<?> updateRecipeImage(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            Recipe recipe = recipeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("食谱不存在"));
            
            recipe.setImageUrl(request.get("image"));
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
     * 根据餐类型获取食谱
     */
    @GetMapping("/meal-type/{mealType}")
    public ResponseEntity<List<Recipe>> getRecipesByMealType(
            @PathVariable String mealType) {
        try {
            List<Recipe> recipes = recipeRepository.findByMealType(mealType);
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除食谱
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id) {
        try {
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
