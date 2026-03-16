package com.example.healthassistant.controller;

import com.example.healthassistant.dto.DietRecordDto;
import com.example.healthassistant.model.DietRecord;
import com.example.healthassistant.model.FitnessRecord;
import com.example.healthassistant.repository.FitnessRecordRepository;
import com.example.healthassistant.service.DietRecordService;
import com.example.healthassistant.service.DoubaoFoodRecognitionService;
import com.example.healthassistant.service.QwenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diet")
public class DietController {

    @Autowired
    private DietRecordService dietRecordService;

    @Autowired
    private DoubaoFoodRecognitionService doubaoFoodService;

    @Autowired
    private QwenAIService qwenAIService;

    @Autowired
    private FitnessRecordRepository fitnessRecordRepository;

    @PostMapping("/record")
    public ResponseEntity<DietRecord> recordDiet(@RequestBody DietRecordDto dietRecordDto) {
        DietRecord record = dietRecordService.saveDietRecord(dietRecordDto);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/daily/{userId}/{date}")
    public ResponseEntity<List<DietRecord>> getDailyDiet(@PathVariable String userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DietRecord> records = dietRecordService.getDailyRecords(userId, date);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/weekly/{userId}/{weekStart}")
    public ResponseEntity<List<DietRecord>> getWeeklyDiet(@PathVariable String userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        List<DietRecord> records = dietRecordService.getWeeklyRecords(userId, weekStart);
        return ResponseEntity.ok(records);
    }

    // 新增获取指定日期范围的饮食记录API
    @GetMapping("/range/{userId}")
    public ResponseEntity<List<DietRecord>> getDietRecordsInRange(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DietRecord> records = dietRecordService.getRecordsInRange(userId, startDate, endDate);
        return ResponseEntity.ok(records);
    }

    // 新增获取月度饮食统计数据 API
    @GetMapping("/monthly-summary/{userId}/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlyDietSummary(
            @PathVariable String userId,
            @PathVariable int year,
            @PathVariable int month) {
        Map<String, Object> summary = dietRecordService.getMonthlySummary(userId, year, month);
        return ResponseEntity.ok(summary);
    }
    
    // 新增获取月度健身统计数据 API
    @GetMapping("/fitness/monthly-summary/{userId}/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getMonthlyFitnessSummary(
            @PathVariable String userId,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            List<FitnessRecord> records = fitnessRecordRepository.findByUserIdAndYearMonth(userId, year, month);
                
            // 计算每日数据
            Map<String, Double> dailyData = new HashMap<>();
            double totalCaloriesBurned = 0;
                
            for (FitnessRecord record : records) {
                String date = record.getDate().toString();
                double calories = record.getCalories() != null ? record.getCalories() : 0;
                    
                dailyData.put(date, dailyData.getOrDefault(date, 0.0) + calories);
                totalCaloriesBurned += calories;
            }
                
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("year", year);
            response.put("month", month);
            response.put("totalCaloriesBurned", Math.round(totalCaloriesBurned * 100.0) / 100.0);
                
            // 构建每日数据数组
            List<Map<String, Object>> dailyDataList = new ArrayList<>();
            for (Map.Entry<String, Double> entry : dailyData.entrySet()) {
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", entry.getKey());
                dayData.put("caloriesBurned", Math.round(entry.getValue() * 100.0) / 100.0);
                dailyDataList.add(dayData);
            }
            response.put("dailyData", dailyDataList);
                
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取月度健身统计失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 新增删除饮食记录API
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDietRecord(@PathVariable Long id) {
        try {
            dietRecordService.deleteDietRecord(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "饮食记录删除成功");
            response.put("deletedId", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "删除饮食记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 新增批量删除饮食记录API
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteBatchDietRecords(@RequestBody List<Long> ids) {
        try {
            int deletedCount = dietRecordService.deleteBatchDietRecords(ids);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "批量删除饮食记录成功");
            response.put("deletedCount", deletedCount);
            response.put("totalRequested", ids.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "批量删除饮食记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 智能分析API - 将食物名称和重量组装成自然语言句子
    @PostMapping("/smart-analyze")
    public ResponseEntity<Map<String, Object>> smartAnalyzeFood(@RequestBody Map<String, Object> request) {
        String foodDescription = (String) request.get("foodDescription");

        if (foodDescription == null || foodDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // 构造自然语言查询语句
            String naturalLanguageQuery = constructNaturalLanguageQuery(foodDescription);
        
            // 调用 AI 服务进行分析
            List<DoubaoFoodRecognitionService.FoodNutrition> results = doubaoFoodService
                    .recognizeFoodWithDoubao(naturalLanguageQuery);

            Map<String, Object> response = new HashMap<>();
            response.put("input", foodDescription);
            response.put("naturalLanguageQuery", naturalLanguageQuery);
            response.put("foods", results);
            response.put("count", results.size());

            // 计算总营养成分
            double totalCalories = 0;
            double totalProtein = 0;
            double totalCarbs = 0;
            double totalFat = 0;
            double totalFiber = 0;

            for (DoubaoFoodRecognitionService.FoodNutrition food : results) {
                totalCalories += food.getActualCalories();
                totalProtein += food.getActualProtein();
                totalCarbs += food.getActualCarbs();
                totalFat += food.getActualFat();
                totalFiber += food.getActualFiber();
            }

            Map<String, Double> totals = new HashMap<>();
            totals.put("calories", Math.round(totalCalories * 100.0) / 100.0);
            totals.put("protein", Math.round(totalProtein * 100.0) / 100.0);
            totals.put("carbs", Math.round(totalCarbs * 100.0) / 100.0);
            totals.put("fat", Math.round(totalFat * 100.0) / 100.0);
            totals.put("fiber", Math.round(totalFiber * 100.0) / 100.0);

            response.put("totalNutrition", totals);
            response.put("mode", "智能自然语言分析");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    // 传统的食物识别 API
    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognizeFood(@RequestBody Map<String, String> request) {
        String foodDescription = request.get("foodDescription");

        if (foodDescription == null || foodDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<DoubaoFoodRecognitionService.FoodNutrition> results = doubaoFoodService
                    .recognizeFoodWithDoubao(foodDescription);

            Map<String, Object> response = new HashMap<>();
            response.put("input", foodDescription);
            response.put("foods", results);
            response.put("count", results.size());

            // 计算总营养成分
            double totalCalories = 0;
            double totalProtein = 0;
            double totalCarbs = 0;
            double totalFat = 0;
            double totalFiber = 0;

            for (DoubaoFoodRecognitionService.FoodNutrition food : results) {
                totalCalories += food.getActualCalories();
                totalProtein += food.getActualProtein();
                totalCarbs += food.getActualCarbs();
                totalFat += food.getActualFat();
                totalFiber += food.getActualFiber();
            }

            Map<String, Double> totals = new HashMap<>();
            totals.put("calories", Math.round(totalCalories * 100.0) / 100.0);
            totals.put("protein", Math.round(totalProtein * 100.0) / 100.0);
            totals.put("carbs", Math.round(totalCarbs * 100.0) / 100.0);
            totals.put("fat", Math.round(totalFat * 100.0) / 100.0);
            totals.put("fiber", Math.round(totalFiber * 100.0) / 100.0);

            response.put("totalNutrition", totals);
            response.put("mode", "豆包AI模式");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 构造自然语言查询语句
    private String constructNaturalLanguageQuery(String foodDescription) {
        // 将用户输入的食物描述转换为自然语言查询
        // 例如："1碗米饭、2个鸡蛋" -> "一个（分量）米饭（食物名称）和两个（分量）鸡蛋（食物名称）的总热量、蛋白质、脂肪、碳水化合物是多少"

        StringBuilder query = new StringBuilder();
        query.append("请分析以下食物的营养成分：\n\n");

        // 解析食物描述
        String[] items = foodDescription.split("、");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (!item.isEmpty()) {
                // 提取数量和单位
                String quantity = extractQuantity(item);
                String foodName = extractFoodName(item);
                String unit = extractUnit(item);

                query.append(quantity).append(unit).append(foodName);
                if (i < items.length - 1) {
                    query.append("、");
                }
            }
        }

        query.append("\n\n请告诉我这些食物的总热量、蛋白质、脂肪、碳水化合物分别是多少？");
        query.append("请以清晰的数字形式回答，精确到小数点后一位。");

        return query.toString();
    }

    // 提取数量
    private String extractQuantity(String item) {
        // 匹配数字开头的部分
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d+(?:\\.\\d+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(item);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "1"; // 默认数量为1
    }

    // 提取食物名称
    private String extractFoodName(String item) {
        // 移除数字和单位，剩下的就是食物名称
        return item.replaceAll("^\\d+(?:\\.\\d+)?", "").replaceAll("[克斤两个]", "").trim();
    }

    // 提取单位
    private String extractUnit(String item) {
        // 匹配单位
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[克斤两个]");
        java.util.regex.Matcher matcher = pattern.matcher(item);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "克"; // 默认单位
    }

    // AI营养分析 API - 分析今日营养摄入并给出建议
    @PostMapping("/analyze-nutrition/{userId}")
    public ResponseEntity<Map<String, Object>> analyzeNutrition(
            @PathVariable String userId,
            @RequestBody Map<String, Object> nutritionData) {
        try {
            String analysis = qwenAIService.analyzeDailyNutrition(userId, nutritionData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysis", analysis);
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "营养分析失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ========== 以下为健身记录相关 API ==========

    // 批量保存健身记录
    @PostMapping("/fitness/batch-save")
    public ResponseEntity<Map<String, Object>> batchSaveFitnessRecords(
            @RequestBody List<Map<String, Object>> fitnessRecords) {
        try {
            if (fitnessRecords == null || fitnessRecords.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "健身记录不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 获取第一个记录的 userId
            String userId = (String) fitnessRecords.get(0).get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "用户 ID 不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<FitnessRecord> savedRecords = new ArrayList<>();

            // 保存每条记录到数据库
            for (Map<String, Object> record : fitnessRecords) {
                FitnessRecord fitnessRecord = new FitnessRecord();
                fitnessRecord.setUserId(userId);

                // 设置日期
                Object dateObj = record.get("date");
                if (dateObj != null) {
                    fitnessRecord.setDate(LocalDate.parse(dateObj.toString()));
                } else {
                    fitnessRecord.setDate(LocalDate.now());
                }

                // 设置类型
                fitnessRecord.setType((String) record.get("type"));

                // 设置名称
                fitnessRecord.setName((String) record.get("name"));

                // 设置时长
                Object durationObj = record.get("durationMinutes");
                if (durationObj != null) {
                    fitnessRecord.setDurationMinutes(((Number) durationObj).intValue());
                }

                // 设置次数（新增）
                Object repsObj = record.get("repetitions");
                if (repsObj != null) {
                    fitnessRecord.setRepetitions(((Number) repsObj).intValue());
                }

                // 设置重量（新增）
                Object weightObj = record.get("weightKg");
                if (weightObj != null) {
                    fitnessRecord.setWeightKg(((Number) weightObj).doubleValue());
                }

                // 设置卡路里
                Object caloriesObj = record.get("calories");
                if (caloriesObj != null) {
                    fitnessRecord.setCalories(((Number) caloriesObj).doubleValue());
                }

                savedRecords.add(fitnessRecordRepository.save(fitnessRecord));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "健身记录保存成功");
            response.put("savedCount", savedRecords.size());
            response.put("records", savedRecords);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "保存健身记录失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 获取指定日期的健身记录
    @GetMapping("/fitness/daily/{userId}/{date}")
    public ResponseEntity<List<Map<String, Object>>> getDailyFitnessRecords(
            @PathVariable String userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<FitnessRecord> records = fitnessRecordRepository.findByUserIdAndDate(userId, date);

            // 转换为前端期望的格式
            List<Map<String, Object>> result = records.stream().map(record -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", record.getId());
                map.put("userId", record.getUserId());
                map.put("date", record.getDate().toString());
                map.put("workoutType", record.getType());
                map.put("workoutName", record.getName());
                map.put("durationMinutes", record.getDurationMinutes());
                map.put("repetitions", record.getRepetitions());
                map.put("weightKg", record.getWeightKg());
                map.put("caloriesBurned", record.getCalories());
                map.put("recordedAt", record.getRecordedAt() != null ? record.getRecordedAt().toString() : null);
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 删除单条健身记录
    @DeleteMapping("/fitness/{id}")
    public ResponseEntity<Map<String, Object>> deleteFitnessRecord(@PathVariable Long id) {
        try {
            if (fitnessRecordRepository.existsById(id)) {
                fitnessRecordRepository.deleteById(id);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "健身记录删除成功");
                response.put("deletedId", id);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "未找到指定的健身记录");
                return ResponseEntity.status(404).body(errorResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "删除健身记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 批量删除健身记录
    @DeleteMapping("/fitness/batch")
    public ResponseEntity<Map<String, Object>> deleteBatchFitnessRecords(@RequestBody List<Long> ids) {
        try {
            System.out.println("批量删除健身记录，数量: " + ids.size());

            fitnessRecordRepository.deleteAllById(ids);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "批量删除健身记录成功");
            response.put("deletedCount", ids.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "批量删除健身记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}