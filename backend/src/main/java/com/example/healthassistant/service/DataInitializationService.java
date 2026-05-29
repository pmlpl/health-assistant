package com.example.healthassistant.service;

import com.example.healthassistant.model.Ingredient;
import com.example.healthassistant.model.Recipe;
import com.example.healthassistant.model.UserProfile;
import com.example.healthassistant.repository.IngredientRepository;
import com.example.healthassistant.repository.RecipeRepository;
import com.example.healthassistant.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class DataInitializationService {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void initializeData() {
        initializeUsers();
        initializeIngredients();
        initializeRecipes();
    }

    private void initializeUsers() {
        System.out.println("检查用户数据初始化...");
        System.out.println("当前用户数量: " + userProfileRepository.count());

        if (userProfileRepository.count() == 0) {
            System.out.println("开始创建默认用户...");

            UserProfile defaultUser = new UserProfile();
            defaultUser.setUserId("testuser");
            defaultUser.setPassword(passwordEncoder.encode("123456")); // 默认密码 123456
            defaultUser.setHeight(175);
            defaultUser.setWeight(70.0);
            defaultUser.setAge(28);
            defaultUser.setGender("M");
            defaultUser.setActivityLevel("轻度运动");
            defaultUser.setHealthGoal("减脂");
            defaultUser.setDietaryRestrictions(Arrays.asList());
            defaultUser.setTastePreferences(Arrays.asList("清淡", "健康"));
            defaultUser.setCreatedAt(LocalDateTime.now());
            defaultUser.setUpdatedAt(LocalDateTime.now());

            double bmr = 10 * 70 + 6.25 * 175 - 5 * 28 + 5;
            double dailyCalories = bmr * 1.375 - 500;

            defaultUser.setTargetCalories(dailyCalories);
            defaultUser.setTargetProtein(70 * 1.2);
            defaultUser.setTargetFat(dailyCalories * 0.25 / 9);
            double remainingCalories = dailyCalories - (defaultUser.getTargetProtein() * 4)
                    - (defaultUser.getTargetFat() * 9);
            defaultUser.setTargetCarbs(remainingCalories / 4);

            UserProfile savedUser = userProfileRepository.save(defaultUser);
            System.out.println("默认用户创建成功: " + savedUser.getUserId());
            System.out.println("用户ID: " + savedUser.getId());

        } else {
            System.out.println("用户数据已存在，无需初始化");
            List<UserProfile> allUsers = userProfileRepository.findAll();
            System.out.println("现有用户列表:");
            for (UserProfile user : allUsers) {
                System.out.println("- ID: " + user.getId() + ", UserId: " + user.getUserId());
            }
        }
    }

    private void initializeIngredients() {
        if (ingredientRepository.count() == 0) {
            // 鸡胸肉
            Ingredient chickenBreast = new Ingredient();
            chickenBreast.setName("鸡胸肉");
            chickenBreast.setCategory("肉类");
            chickenBreast.setCaloriesPer100g(110.0);
            chickenBreast.setProteinPer100g(22.5);
            chickenBreast.setCarbsPer100g(0.0);
            chickenBreast.setFatPer100g(1.2);
            chickenBreast.setFiberPer100g(0.0);
            chickenBreast.setSeason("四季皆宜");
            chickenBreast.setHealthBenefits("高蛋白低脂，适合减脂增肌");

            // 西兰花
            Ingredient broccoli = new Ingredient();
            broccoli.setName("西兰花");
            broccoli.setCategory("蔬菜");
            broccoli.setCaloriesPer100g(35.0);
            broccoli.setProteinPer100g(2.8);
            broccoli.setCarbsPer100g(7.0);
            broccoli.setFatPer100g(0.4);
            broccoli.setFiberPer100g(2.6);
            broccoli.setSeason("春秋");
            broccoli.setHealthBenefits("富含维生素C和膳食纤维");

            // 燕麦
            Ingredient oats = new Ingredient();
            oats.setName("燕麦");
            oats.setCategory("谷物");
            oats.setCaloriesPer100g(389.0);
            oats.setProteinPer100g(16.9);
            oats.setCarbsPer100g(66.3);
            oats.setFatPer100g(6.9);
            oats.setFiberPer100g(10.6);
            oats.setSeason("四季皆宜");
            oats.setHealthBenefits("富含膳食纤维，有助于控糖降脂");

            // 三文鱼
            Ingredient salmon = new Ingredient();
            salmon.setName("三文鱼");
            salmon.setCategory("鱼类");
            salmon.setCaloriesPer100g(206.0);
            salmon.setProteinPer100g(20.4);
            salmon.setCarbsPer100g(0.0);
            salmon.setFatPer100g(13.4);
            salmon.setFiberPer100g(0.0);
            salmon.setSeason("四季皆宜");
            salmon.setHealthBenefits("富含Omega-3脂肪酸");

            // 藜麦
            Ingredient quinoa = new Ingredient();
            quinoa.setName("藜麦");
            quinoa.setCategory("谷物");
            quinoa.setCaloriesPer100g(120.0);
            quinoa.setProteinPer100g(4.4);
            quinoa.setCarbsPer100g(21.3);
            quinoa.setFatPer100g(1.9);
            quinoa.setFiberPer100g(2.8);
            quinoa.setSeason("四季皆宜");
            quinoa.setHealthBenefits("全蛋白谷物，适合控糖饮食");

            // 大米
            Ingredient rice = new Ingredient();
            rice.setName("大米");
            rice.setCategory("谷物");
            rice.setCaloriesPer100g(130.0);
            rice.setProteinPer100g(2.7);
            rice.setCarbsPer100g(28.0);
            rice.setFatPer100g(0.3);
            rice.setFiberPer100g(0.4);
            rice.setSeason("四季皆宜");
            rice.setHealthBenefits("主要碳水化合物来源");

            // 鸡蛋
            Ingredient egg = new Ingredient();
            egg.setName("鸡蛋");
            egg.setCategory("蛋类");
            egg.setCaloriesPer100g(155.0);
            egg.setProteinPer100g(13.0);
            egg.setCarbsPer100g(1.1);
            egg.setFatPer100g(11.0);
            egg.setFiberPer100g(0.0);
            egg.setSeason("四季皆宜");
            egg.setHealthBenefits("优质蛋白质来源");

            // 牛肉
            Ingredient beef = new Ingredient();
            beef.setName("牛肉");
            beef.setCategory("肉类");
            beef.setCaloriesPer100g(250.0);
            beef.setProteinPer100g(26.0);
            beef.setCarbsPer100g(0.0);
            beef.setFatPer100g(15.0);
            beef.setFiberPer100g(0.0);
            beef.setSeason("四季皆宜");
            beef.setHealthBenefits("富含铁质和蛋白质");

            // 豆腐
            Ingredient tofu = new Ingredient();
            tofu.setName("豆腐");
            tofu.setCategory("豆制品");
            tofu.setCaloriesPer100g(76.0);
            tofu.setProteinPer100g(8.1);
            tofu.setCarbsPer100g(1.9);
            tofu.setFatPer100g(4.8);
            tofu.setFiberPer100g(0.5);
            tofu.setSeason("四季皆宜");
            tofu.setHealthBenefits("植物蛋白，适合素食者");

            // 红薯
            Ingredient sweetPotato = new Ingredient();
            sweetPotato.setName("红薯");
            sweetPotato.setCategory("根茎类");
            sweetPotato.setCaloriesPer100g(86.0);
            sweetPotato.setProteinPer100g(1.6);
            sweetPotato.setCarbsPer100g(20.1);
            sweetPotato.setFatPer100g(0.1);
            sweetPotato.setFiberPer100g(3.0);
            sweetPotato.setSeason("秋冬");
            sweetPotato.setHealthBenefits("富含β-胡萝卜素和膳食纤维");

            List<Ingredient> ingredients = Arrays.asList(
                    chickenBreast, broccoli, oats, salmon, quinoa,
                    rice, egg, beef, tofu, sweetPotato);

            ingredientRepository.saveAll(ingredients);
            System.out.println("食材数据初始化完成，共创建 " + ingredients.size() + " 种食材");
        } else {
            System.out.println("食材数据已存在，无需初始化");
        }
    }

    private void initializeRecipes() {
        if (recipeRepository.count() == 0) {
            Recipe recipe1 = new Recipe();
            recipe1.setName("香煎鸡胸肉配西兰花");
            recipe1.setDescription("高蛋白低脂健身餐");
            recipe1.setIngredientsList("鸡胸肉200g,西兰花150g,橄榄油适量,盐适量,黑胡椒适量");
            recipe1.setInstructions("1.鸡胸肉切块用盐和黑胡椒腌制10分钟；2.西兰花焯水备用；3.平底锅刷少量橄榄油，煎鸡胸肉至两面金黄；4.加入西兰花翻炒即可");
            recipe1.setMealType("午餐");
            recipe1.setCuisineType("家常");
            recipe1.setCookingTime(20);
            recipe1.setCalories(280.0);
            recipe1.setProtein(35.0);
            recipe1.setCarbs(8.0);
            recipe1.setFat(10.0);
            recipe1.setDifficulty("简单");
            recipe1.setServings(1);
            recipe1.setTags(Arrays.asList("减脂", "高蛋白", "低脂"));
            recipe1.setSuitableForDiet(true);

            Recipe recipe2 = new Recipe();
            recipe2.setName("燕麦牛奶水果碗");
            recipe2.setDescription("营养丰富的健康早餐");
            recipe2.setIngredientsList("燕麦50g,牛奶200ml,香蕉半根,蓝莓30g,坚果适量");
            recipe2.setInstructions("1.燕麦用牛奶泡软；2.香蕉切片，与蓝莓一起放在燕麦上；3.撒上少量坚果即可");
            recipe2.setMealType("早餐");
            recipe2.setCuisineType("快手");
            recipe2.setCookingTime(5);
            recipe2.setCalories(320.0);
            recipe2.setProtein(12.0);
            recipe2.setCarbs(45.0);
            recipe2.setFat(10.0);
            recipe2.setDifficulty("简单");
            recipe2.setServings(1);
            recipe2.setTags(Arrays.asList("控糖", "高纤维", "营养均衡"));
            recipe2.setSuitableForDiet(true);

            Recipe recipe3 = new Recipe();
            recipe3.setName("香煎三文鱼");
            recipe3.setDescription("富含Omega-3的健康晚餐");
            recipe3.setIngredientsList("三文鱼150g,柠檬半个,橄榄油适量,盐适量,黑胡椒适量");
            recipe3.setInstructions("1.三文鱼洗净擦干，用盐和黑胡椒腌制；2.平底锅加热，放入三文鱼，中小火慢煎；3.煎至两面金黄，挤上柠檬汁即可");
            recipe3.setMealType("晚餐");
            recipe3.setCuisineType("家常");
            recipe3.setCookingTime(15);
            recipe3.setCalories(320.0);
            recipe3.setProtein(30.0);
            recipe3.setCarbs(0.0);
            recipe3.setFat(22.0);
            recipe3.setDifficulty("简单");
            recipe3.setServings(1);
            recipe3.setTags(Arrays.asList("增肌", "健康脂肪", "抗炎"));
            recipe3.setSuitableForDiet(true);

            List<Recipe> recipes = Arrays.asList(recipe1, recipe2, recipe3);

            recipeRepository.saveAll(recipes);
            System.out.println("食谱数据初始化完成，共创建 " + recipes.size() + " 个食谱");
        } else {
            System.out.println("食谱数据已存在，无需初始化");
        }
    }
}
