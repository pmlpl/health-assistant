-- 清空数据库表（按正确顺序删除，避免外键冲突）
USE health_assistant;

-- 临时禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 删除所有表的数据
TRUNCATE TABLE consumed_ingredients;
TRUNCATE TABLE dietary_restrictions;
TRUNCATE TABLE taste_preferences;
TRUNCATE TABLE recipe_tags;
TRUNCATE TABLE diet_record;
TRUNCATE TABLE fitness_record;
TRUNCATE TABLE user_profile;
TRUNCATE TABLE ingredient;
TRUNCATE TABLE exercise;
TRUNCATE TABLE recipe;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 验证删除结果
SELECT 'user_profile' as table_name, COUNT(*) as row_count FROM user_profile
UNION ALL
SELECT 'ingredient', COUNT(*) FROM ingredient
UNION ALL
SELECT 'exercise', COUNT(*) FROM exercise
UNION ALL
SELECT 'recipe', COUNT(*) FROM recipe;
