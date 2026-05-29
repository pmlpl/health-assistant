-- ====================================
-- 检查食材数据库表
-- ====================================

-- 1. 查看表中所有食材
SELECT id, name, category, calories_per_100g, protein_per_100g 
FROM ingredient 
ORDER BY id;

-- 2. 统计食材总数
SELECT COUNT(*) as total_ingredients FROM ingredient;

-- 3. 查找"鸡蛋"
SELECT * FROM ingredient WHERE name = '鸡蛋';

-- 4. 查找所有蛋类
SELECT * FROM ingredient WHERE category = '蛋类';

-- 5. 查看是否有重复数据
SELECT name, COUNT(*) as count 
FROM ingredient 
GROUP BY name 
HAVING COUNT(*) > 1;

-- ====================================
-- 插入所有常见食材 (如果数据库中为空或缺失)
-- ====================================

-- 插入肉类
INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('鸡胸肉', '肉类', 110.0, 22.5, 0.0, 1.2, 0.0, '四季皆宜', true, '高蛋白低脂，适合减脂增肌', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('牛肉', '肉类', 250.0, 26.0, 0.0, 15.0, 0.0, '四季皆宜', true, '富含铁质和蛋白质', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('猪肉', '肉类', 250.0, 26.0, 0.0, 15.0, 0.0, '四季皆宜', true, '富含优质蛋白质和血红素铁', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('鸭肉', '肉类', 240.0, 16.0, 0.0, 19.0, 0.0, '四季皆宜', true, '滋阴养胃，利水消肿', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('鹅肉', '肉类', 251.0, 17.0, 0.0, 20.0, 0.0, '四季皆宜', true, '益气补虚，和胃止渴', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 插入蛋类
INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('鸡蛋', '蛋类', 155.0, 13.0, 1.1, 11.0, 0.0, '四季皆宜', true, '优质蛋白质来源', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 插入蔬菜类
INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('青菜', '蔬菜', 15.0, 1.5, 2.5, 0.2, 1.0, '四季皆宜', true, '富含维生素 C 和钙', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('西兰花', '蔬菜', 35.0, 2.8, 7.0, 0.4, 2.6, '春秋', true, '富含维生素 C 和膳食纤维', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 插入水果类
INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('苹果', '水果', 52.0, 0.3, 14.0, 0.2, 2.4, '秋季', true, '生津开胃，润肺止咳', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('香蕉', '水果', 89.0, 1.1, 23.0, 0.3, 2.6, '四季皆宜', true, '补充能量，润肠通便', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 插入谷物类
INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('米饭', '谷物', 130.0, 2.7, 28.0, 0.3, 0.4, '四季皆宜', true, '主要碳水化合物来源', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('燕麦', '谷物', 389.0, 16.9, 66.3, 6.9, 10.6, '四季皆宜', true, '富含膳食纤维，有助于控糖降脂', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('大米', '谷物', 130.0, 2.7, 28.0, 0.3, 0.4, '四季皆宜', true, '主要碳水化合物来源', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 插入豆制品
INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('豆腐', '豆制品', 76.0, 8.1, 1.9, 4.8, 0.5, '四季皆宜', true, '植物蛋白，适合素食者', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 插入根茎类
INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('红薯', '根茎类', 86.0, 1.6, 20.1, 0.1, 3.0, '秋冬', true, '富含β-胡萝卜素和膳食纤维', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 插入鱼类
INSERT INTO ingredient (name, category, calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, season, is_healthy, health_benefits, created_at, updated_at) VALUES
('三文鱼', '鱼类', 206.0, 20.4, 0.0, 13.4, 0.0, '四季皆宜', true, '富含 Omega-3 脂肪酸', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- ====================================
-- 验证插入结果
-- ====================================

-- 查看所有食材
SELECT id, name, category, calories_per_100g 
FROM ingredient 
ORDER BY category, id;

-- 统计总数
SELECT COUNT(*) as total FROM ingredient;
