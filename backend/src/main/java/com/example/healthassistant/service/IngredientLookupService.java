package com.example.healthassistant.service;

import com.example.healthassistant.model.Ingredient;
import com.example.healthassistant.repository.IngredientRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 食材内存索引：表数据量小，启动时全量加载，避免 smart-analyze 等场景重复查库。
 */
@Service
public class IngredientLookupService {

    private final IngredientRepository ingredientRepository;
    private volatile Map<String, Ingredient> byName = Map.of();

    public IngredientLookupService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    /** 启动时构建 name -> Ingredient 索引 */
    @PostConstruct
    public void loadCache() {
        Map<String, Ingredient> map = new HashMap<>();
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            if (ingredient.getName() != null) {
                map.put(ingredient.getName(), ingredient);
            }
        }
        byName = Collections.unmodifiableMap(map);
    }

    /** 按名称查找单个食材 */
    public Ingredient findByName(String name) {
        if (name == null) {
            return null;
        }
        return byName.get(name);
    }

    /** 批量按名称查找，返回 name -> Ingredient 映射 */
    public Map<String, Ingredient> findByNames(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return Map.of();
        }
        return names.stream()
                .filter(n -> n != null && byName.containsKey(n))
                .distinct()
                .collect(Collectors.toMap(n -> n, byName::get, (a, b) -> a));
    }

    /** 食材变更后刷新缓存（管理端增删改时可调用） */
    public void refresh() {
        loadCache();
    }
}
