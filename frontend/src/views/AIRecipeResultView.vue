<template>
  <div class="ai-result-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="loading-animation">
          <div class="chef-hat">👨‍🍳</div>
          <div class="pulse-ring"></div>
        </div>
        <h2 class="loading-title">AI 正在为您定制专属食谱</h2>
        <p class="loading-subtitle">分析营养需求 · 匹配健康目标 · 生成美味方案</p>
        <div class="loading-steps">
          <div class="step" :class="{ active: currentStep >= 1 }">
            <span class="step-icon">📊</span>
            <span class="step-text">分析您的健康档案</span>
          </div>
          <div class="step" :class="{ active: currentStep >= 2 }">
            <span class="step-icon">🎯</span>
            <span class="step-text">计算营养需求</span>
          </div>
          <div class="step" :class="{ active: currentStep >= 3 }">
            <span class="step-icon">🍳</span>
            <span class="step-text">生成个性化食谱</span>
          </div>
          <div class="step" :class="{ active: currentStep >= 4 }">
            <span class="step-icon">🎨</span>
            <span class="step-text">绘制精美美食图片</span>
          </div>
        </div>
        <div class="loading-progress-bar">
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: progressWidth + '%' }"></div>
          </div>
          <span class="progress-text">{{ progressText }}</span>
        </div>
      </div>
    </div>

    <!-- 结果显示 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">❌</div>
      <h2>生成失败</h2>
      <p>{{ error }}</p>
      <button class="btn retry-btn" @click="retry">重新生成</button>
    </div>

    <div v-else-if="recommendations.length > 0" class="results-container">
      <div class="results-header">
        <div class="header-content">
          <p class="page-subtitle">基于您的健康档案和今日营养摄入情况，智能生成的个性化建议</p>
        </div>
        <div class="header-actions">
          <button class="btn print-btn" @click="printResults">
            🖨️ 打印结果
          </button>
          <button class="btn close-btn" @click="closePage">
            ✖️ 关闭页面
          </button>
        </div>
      </div>

      <div class="analysis-card" v-if="analysis">
        <h3>📊 营养分析</h3>
        <p class="analysis-text">{{ analysis }}</p>
      </div>

      <div class="recipes-grid">
        <div 
          class="card recipe-card" 
          v-for="(recipe, index) in recommendations" 
          :key="recipe.id"
          :style="{ animationDelay: index * 0.15 + 's' }"
        >
          <div class="recipe-image">
            <img 
              v-if="recipe.image && !recipe.imageLoading" 
              :src="recipe.image" 
              :alt="recipe.name" 
              class="recipe-img"
            />
            <div v-else class="image-loading-overlay">
              <div class="loading-spinner">
                <div class="spinner-ring"></div>
                <div class="spinner-ring"></div>
                <div class="spinner-ring"></div>
              </div>
              <p class="loading-hint">绘制中...</p>
            </div>
          </div>

          <div class="recipe-content">
            <div class="recipe-header">
              <span class="recipe-number">{{ index + 1 }}</span>
              <h3 class="recipe-title">{{ recipe.name }}</h3>
            </div>
            <p class="recipe-description">{{ recipe.description }}</p>
            
            <div class="recipe-tags">
              <span class="tag meal-type">{{ recipe.mealType }}</span>
              <span class="tag calories">{{ recipe.calories }} kcal</span>
              <span class="tag ai-tag">🤖 AI 推荐</span>
            </div>
            
            <div class="recipe-meta">
              <div class="meta-item">
                <span class="meta-label">蛋白质</span>
                <span class="meta-value">{{ recipe.protein }}g</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">碳水</span>
                <span class="meta-value">{{ recipe.carbs }}g</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">脂肪</span>
                <span class="meta-value">{{ recipe.fat }}g</span>
              </div>
            </div>

            <div class="recipe-section">
              <h4>🥗 所需食材</h4>
              <ul class="ingredients-list">
                <li v-for="(item, idx) in recipe.ingredients" :key="idx">{{ item }}</li>
              </ul>
            </div>

            <div class="recipe-section">
              <h4>👨‍🍳 制作步骤</h4>
              <ol class="instructions-list">
                <li v-for="(step, idx) in recipe.instructions" :key="idx">{{ step }}</li>
              </ol>
            </div>

            <div class="recipe-actions">
              <button class="btn add-btn" @click="addToMyRecipes(recipe)">
                ➕ 添加到我的食谱
              </button>
              <button v-if="recipe.databaseId && recipe.image" class="btn fix-btn" @click="updateRecipeImage(recipe)" title="如果数据库中缺少图片，点击此按钮修复">
                🖼️ 更新图片到数据库
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="footer-tip">
        💡 小贴士：点击"添加到我的食谱"可以保存到您个人收藏，方便日后快速查看
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import { ElNotification } from 'element-plus';

// 配置 axios 携带凭证
axios.defaults.withCredentials = true;
// 从 localStorage 或 opener 窗口获取 token
const token = localStorage.getItem('token') || 
              (window.opener && window.opener.localStorage.getItem('token')) || 
              '';
axios.defaults.headers.common['Authorization'] = token;

const route = useRoute();
const loading = ref(true);
const error = ref(null);
const recommendations = ref([]);
const analysis = ref('');
const currentStep = ref(0);
const progressWidth = ref(0);
const progressText = ref('准备中...');
const generatedCount = ref(0); // 已生成的图片数量
const totalCount = ref(4); // 总共需要生成的图片数

// 从路由参数获取数据
const mealTypeName = computed(() => {
  const type = route.query.mealType || 'LUNCH';
  const map = {
    'BREAKFAST': '早餐',
    'LUNCH': '午餐',
    'DINNER': '晚餐',
    'SNACK': '加餐'
  };
  return map[type] || '餐点';
});

// 更新进度
const updateProgress = (step, text, width) => {
  currentStep.value = step;
  progressText.value = text;
  progressWidth.value = width;
};

// 模拟进度更新（仅用于初始阶段）
const simulateInitialProgress = () => {
  updateProgress(1, '初始化中...', 5);
  
  setTimeout(() => {
    updateProgress(1, '分析健康档案...', 15);
  }, 800);
  
  setTimeout(() => {
    updateProgress(2, '计算营养需求...', 30);
  }, 1600);
  
  setTimeout(() => {
    updateProgress(3, '生成食谱方案...', 50);
  }, 2400);
};

// 加载数据
const loadData = async () => {
  try {
    // 启动初始进度动画
    simulateInitialProgress();
    
    // 检查是否有缓存的数据
    const cachedData = sessionStorage.getItem('aiRecommendationsResult');
    if (cachedData) {
      const { recommendations: recs, analysis: ana } = JSON.parse(cachedData);
      recommendations.value = recs;
      analysis.value = ana;
      loading.value = false;
      updateProgress(4, '加载完成！', 100);
      return;
    }

    // 如果没有缓存，调用 API
    const mealType = route.query.mealType || 'LUNCH';
    const userId = route.query.userId;
    
    if (!userId) {
      throw new Error('缺少用户 ID 参数');
    }
    
    // 更新进度：开始生成图片
    simulateInitialProgress(); // 确保初始动画已启动
    updateProgress(3, '正在绘制精美图片...', 60);
    
    const response = await axios.post('/api/ai/recommend-recipes', {
      userId: userId,
      mealType: mealType,
    });

    if (response.data && response.data.recommendations) {
      // 处理推荐结果
      const processedRecommendations = response.data.recommendations.map((r, index) => ({
        ...r,
        id: `ai-${r.recipeName || r.name}-${Date.now()}`,
        name: r.recipeName || r.name,
        image: r.image || null,
        imageLoading: !r.image,
        mealType: mealTypeName.value,
        isAiRecommended: true
      }));

      recommendations.value = processedRecommendations;
      analysis.value = response.data.analysis || '';
      
      // 根据实际生成的图片数量更新进度
      totalCount.value = processedRecommendations.length;
      
      // 检查有多少图片已经生成完成
      const completedCount = processedRecommendations.filter(r => r.image && !r.imageLoading).length;
      generatedCount.value = completedCount;
      
      // 根据完成情况更新进度
      if (completedCount === 0) {
        updateProgress(4, '开始绘制第一张图片...', 65);
      } else if (completedCount < totalCount.value) {
        const progressPercent = 65 + Math.floor((completedCount / totalCount.value) * 35);
        updateProgress(4, `已绘制 ${completedCount}/${totalCount.value} 张图片...`, progressPercent);
      } else {
        updateProgress(4, '所有图片绘制完成！', 100);
      }
      
      // 保存到 sessionStorage
      sessionStorage.setItem('aiRecommendationsResult', JSON.stringify({
        recommendations: processedRecommendations,
        analysis: response.data.analysis || ''
      }));
    }

    loading.value = false;
  } catch (err) {
    console.error('加载 AI 推荐失败:', err);
    error.value = err.message || '加载失败，请重试';
    loading.value = false;
    updateProgress(4, '加载失败', 0);
  }
};

// 图片加载完成
const onImageLoad = (recipe) => {
  recipe.imageLoading = false;
  generatedCount.value++;
  
  // 更新进度
  if (generatedCount.value < totalCount.value) {
    const progressPercent = 65 + Math.floor((generatedCount.value / totalCount.value) * 35);
    updateProgress(4, `已绘制 ${generatedCount.value}/${totalCount.value} 张图片...`, progressPercent);
  } else {
    updateProgress(4, '所有图片绘制完成！', 100);
  }
  
  console.log('图片加载完成:', recipe.name, `(${generatedCount.value}/${totalCount.value})`);
};

// 添加到我的食谱
const addToMyRecipes = async (recipe) => {
  // 创建一个纯净的、可序列化的食谱对象
  const serializableRecipe = {
    id: recipe.id,
    name: recipe.name,
    description: recipe.description,
    image: recipe.image,
    mealType: recipe.mealType,
    calories: recipe.calories,
    protein: recipe.protein,
    carbs: recipe.carbs,
    fat: recipe.fat,
    ingredients: recipe.ingredients || [],
    instructions: recipe.instructions || [],
    isAiRecommended: true,
    addedAt: new Date().toISOString()
  };
  
  try {
    // 1. 先保存到后端数据库（永久存储）
    console.log('🔍 准备保存食谱:', serializableRecipe.name);
    console.log('🖼️ 图片数据:', serializableRecipe.image ? '有图片，长度:' + serializableRecipe.image.length : '❌ 无图片');
    
    const saveResponse = await fetch('/api/recipes/save-ai-generated', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token
      },
      body: JSON.stringify({
        name: serializableRecipe.name,
        description: serializableRecipe.description,
        image: serializableRecipe.image, // Base64 图片
        mealType: serializableRecipe.mealType,
        calories: serializableRecipe.calories,
        protein: serializableRecipe.protein,
        carbs: serializableRecipe.carbs,
        fat: serializableRecipe.fat,
        ingredients: serializableRecipe.ingredients,
        instructions: serializableRecipe.instructions
      })
    });
    
    if (saveResponse.ok) {
      const savedData = await saveResponse.json();
      console.log('✅ 食谱已保存到数据库:', savedData);
      serializableRecipe.databaseId = savedData.id; // 保存数据库 ID
    } else {
      console.error('⚠️ 后端保存失败，仅使用本地存储');
    }
    
    // 2. 同时保存到 localStorage（作为缓存）
    const storedRecipes = localStorage.getItem('allRecipes');
    const allRecipes = storedRecipes ? JSON.parse(storedRecipes) : [];
    
    // 检查是否已存在
    const exists = allRecipes.some(r => r.id === serializableRecipe.id || r.name === serializableRecipe.name);
    if (!exists) {
      allRecipes.push(serializableRecipe);
      localStorage.setItem('allRecipes', JSON.stringify(allRecipes));
      
      // 使用 Element Plus 的成功提示
      ElNotification({
        title: '✅ 添加成功',
        message: `已将「${serializableRecipe.name}」添加到您的食谱（已永久保存）`,
        type: 'success',
        duration: 3000,
        position: 'top-right',
        offset: 80
      });
    } else {
      // 使用 Element Plus 的警告提示
      ElNotification({
        title: 'ℹ️ 提示',
        message: `「${serializableRecipe.name}」已经在您的食谱中了`,
        type: 'info',
        duration: 3000,
        position: 'top-right',
        offset: 80
      });
    }
  } catch (e) {
    console.error('保存到我的食谱失败:', e);
    ElNotification({
      title: '❌ 错误',
      message: '添加失败，请重试',
      type: 'error',
      duration: 3000,
      position: 'top-right',
      offset: 80
    });
  }
};

// 更新食谱图片到数据库（修复之前未保存的问题）
const updateRecipeImage = async (recipe) => {
  if (!recipe.databaseId || !recipe.image) {
    ElNotification({
      title: '⚠️ 提示',
      message: '该食谱没有可用的图片数据',
      type: 'info',
      duration: 2000,
      position: 'top-right',
      offset: 80
    });
    return;
  }
  
  try {
    console.log('🔧 正在更新食谱图片到数据库:', recipe.name);
    
    const response = await fetch(`/api/recipes/update-recipe-image/${recipe.databaseId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token
      },
      body: JSON.stringify({
        image: recipe.image
      })
    });
    
    const result = await response.json();
    
    if (response.ok && result.success) {
      console.log('✅ 图片已更新到数据库');
      ElNotification({
        title: '✅ 修复成功',
        message: `已将「${recipe.name}」的图片保存到数据库`,
        type: 'success',
        duration: 3000,
        position: 'top-right',
        offset: 80
      });
    } else {
      console.error('❌ 更新失败:', result);
      ElNotification({
        title: '❌ 错误',
        message: '更新失败：' + (result.error || '未知错误'),
        type: 'error',
        duration: 3000,
        position: 'top-right',
        offset: 80
      });
    }
  } catch (err) {
    console.error('更新图片异常:', err);
    ElNotification({
      title: '❌ 错误',
      message: '更新失败，请重试',
      type: 'error',
      duration: 3000,
      position: 'top-right',
      offset: 80
    });
  }
};

// 重试
const retry = () => {
  loading.value = true;
  error.value = null;
  loadData();
};

// 打印结果
const printResults = () => {
  // 创建一个新窗口用于打印
  const printWindow = window.open('', '_blank');
  
  // 生成打印内容
  const printContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <title>AI 推荐食谱 - ${mealTypeName.value}</title>
      <style>
        body {
          font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
          padding: 20px;
          color: #333;
        }
        h1 {
          text-align: center;
          color: #1d1d1f;
          font-size: 24px;
          margin-bottom: 10px;
        }
        .subtitle {
          text-align: center;
          color: #666;
          font-size: 14px;
          margin-bottom: 30px;
        }
        .recipe-card {
          border: 1px solid #ddd;
          border-radius: 8px;
          padding: 20px;
          margin-bottom: 20px;
          page-break-inside: avoid;
        }
        .recipe-number {
          display: inline-block;
          width: 32px;
          height: 32px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
          border-radius: 50%;
          text-align: center;
          line-height: 32px;
          font-weight: bold;
          margin-right: 10px;
        }
        .recipe-title {
          display: inline-block;
          font-size: 20px;
          font-weight: bold;
          color: #1d1d1f;
          vertical-align: middle;
        }
        .recipe-description {
          color: #666;
          line-height: 1.6;
          margin: 15px 0;
        }
        .nutrition-info {
          background: #f5f5f7;
          padding: 15px;
          border-radius: 8px;
          margin: 15px 0;
        }
        .nutrition-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 10px;
        }
        .nutrition-item {
          text-align: center;
        }
        .nutrition-label {
          font-size: 12px;
          color: #666;
        }
        .nutrition-value {
          font-size: 18px;
          font-weight: bold;
          color: #1d1d1f;
        }
        .section-title {
          font-size: 16px;
          font-weight: bold;
          color: #1d1d1f;
          margin: 15px 0 10px 0;
        }
        .ingredients-list,
        .instructions-list {
          padding-left: 20px;
          line-height: 1.8;
        }
        .tag {
          display: inline-block;
          padding: 6px 12px;
          border-radius: 980px;
          font-size: 12px;
          font-weight: 600;
          margin-right: 8px;
        }
        .tag.meal-type {
          background: rgba(0, 122, 255, 0.1);
          color: #007aff;
        }
        .tag.calories {
          background: rgba(255, 149, 0, 0.1);
          color: #ff9500;
        }
        @media print {
          body { padding: 0; }
          .no-print { display: none; }
        }
      </style>
    </head>
    <body>
      <h1>🍽️ AI 推荐${mealTypeName.value}</h1>
      <p class="subtitle">基于您的健康档案和今日营养摄入情况，智能生成的个性化建议</p>
      
      ${recommendations.value.map((recipe, index) => `
        <div class="recipe-card">
          <div>
            <span class="recipe-number">${index + 1}</span>
            <span class="recipe-title">${recipe.name}</span>
          </div>
          
          <p class="recipe-description">${recipe.description || ''}</p>
          
          <div>
            <span class="tag meal-type">${recipe.mealType}</span>
            <span class="tag calories">${recipe.calories} kcal</span>
          </div>
          
          <div class="nutrition-info">
            <div class="nutrition-grid">
              <div class="nutrition-item">
                <div class="nutrition-label">蛋白质</div>
                <div class="nutrition-value">${recipe.protein}g</div>
              </div>
              <div class="nutrition-item">
                <div class="nutrition-label">碳水</div>
                <div class="nutrition-value">${recipe.carbs}g</div>
              </div>
              <div class="nutrition-item">
                <div class="nutrition-label">脂肪</div>
                <div class="nutrition-value">${recipe.fat}g</div>
              </div>
            </div>
          </div>
          
          ${recipe.ingredients && recipe.ingredients.length > 0 ? `
            <div class="section-title">🥗 所需食材</div>
            <ul class="ingredients-list">
              ${recipe.ingredients.map(item => `<li>${item}</li>`).join('')}
            </ul>
          ` : ''}
          
          ${recipe.instructions && recipe.instructions.length > 0 ? `
            <div class="section-title">👨‍🍳 制作步骤</div>
            <ol class="instructions-list">
              ${recipe.instructions.map(step => `<li>${step}</li>`).join('')}
            </ol>
          ` : ''}
        </div>
      `).join('')}
      
      <div class="no-print" style="text-align: center; margin-top: 30px; color: #666;">
        <button onclick="window.print()" style="padding: 12px 24px; background: #007aff; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 15px;">🖨️ 打印</button>
        <button onclick="window.close()" style="padding: 12px 24px; background: #f5f5f7; color: #333; border: 1px solid #ddd; border-radius: 8px; cursor: pointer; font-size: 15px; margin-left: 10px;">✖️ 关闭</button>
      </div>
    </body>
    </html>
  `;
  
  printWindow.document.write(printContent);
  printWindow.document.close();
  
  // 等待图片加载完成后打印
  setTimeout(() => {
    printWindow.focus();
    printWindow.print();
  }, 500);
};

// 关闭页面
const closePage = () => {
  window.close();
};

onMounted(() => {
  loadData();
  
  // 监听图片加载完成
  window.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'IMAGE_LOADED') {
      const recipe = recommendations.value.find(r => r.id === event.data.recipeId);
      if (recipe) {
        recipe.image = event.data.imageUrl;
        recipe.imageLoading = false;
      }
    }
  });
});
</script>

<style scoped>
.ai-result-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
}

.loading-content {
  text-align: center;
  color: white;
}

.loading-animation {
  position: relative;
  margin-bottom: 32px;
}

.chef-hat {
  font-size: 5rem;
  animation: float 3s ease-in-out infinite;
}

.pulse-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  border: 3px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  animation: pulse 2s ease-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

@keyframes pulse {
  0% {
    width: 100px;
    height: 100px;
    opacity: 1;
  }
  100% {
    width: 200px;
    height: 200px;
    opacity: 0;
  }
}

.loading-title {
  font-size: 2rem;
  margin-bottom: 12px;
  font-weight: 700;
}

.loading-subtitle {
  font-size: 1rem;
  opacity: 0.9;
  margin-bottom: 32px;
}

.loading-steps {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-width: 600px;
  margin: 0 auto 32px;
}

.step {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.step.active {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.05);
}

.step-icon {
  font-size: 1.5rem;
}

.step-text {
  font-size: 0.9rem;
  font-weight: 500;
}

.loading-progress-bar {
  max-width: 600px;
  margin: 0 auto;
}

.progress-track {
  width: 100%;
  height: 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #fff, #f0f0f0);
  transition: width 0.5s ease;
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.5);
}

.progress-text {
  font-size: 0.9rem;
  opacity: 0.9;
}

/* 结果展示样式 */
.results-container {
  max-width: 1400px;
  margin: 0 auto;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.header-content h1 {
  font-size: 2rem;
  color: #1d1d1f;
  margin-bottom: 8px;
}

.header-content p {
  color: #86868b;
  font-size: 1rem;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.print-btn {
  background: #007aff;
  color: white;
}

.print-btn:hover {
  background: #0066d6;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.3);
}

.close-btn {
  background: #f5f5f7;
  color: #1d1d1f;
}

.close-btn:hover {
  background: #e8e8ed;
}

.analysis-card {
  background: rgba(255, 255, 255, 0.95);
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 32px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

.analysis-card h3 {
  color: #1d1d1f;
  margin-bottom: 12px;
  font-size: 1.3rem;
}

.analysis-text {
  color: #4a5568;
  line-height: 1.6;
}

.recipes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.recipe-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.6s ease forwards;
  opacity: 0;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.recipe-image {
  height: 280px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.recipe-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-loading-overlay {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.spinner-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 3px solid transparent;
  border-top-color: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  animation: spin 1.5s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

.spinner-ring:nth-child(1) {
  animation-delay: -0.45s;
  border-top-color: rgba(255, 255, 255, 0.8);
}

.spinner-ring:nth-child(2) {
  animation-delay: -0.3s;
  border-top-color: rgba(255, 255, 255, 0.5);
}

.spinner-ring:nth-child(3) {
  animation-delay: -0.15s;
  border-top-color: rgba(255, 255, 255, 0.3);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-hint {
  color: white;
  font-size: 14px;
  font-weight: 500;
}

.recipe-content {
  padding: 24px;
  display: flex;
  flex-direction: column;
  height: calc(100% - 280px); /* 减去图片高度 */
}

.recipe-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.recipe-number {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.1rem;
}

.recipe-title {
  color: #1d1d1f;
  font-size: 1.3rem;
  font-weight: 700;
  margin: 0;
}

.recipe-description {
  color: #86868b;
  line-height: 1.5;
  margin-bottom: 16px;
}

.recipe-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tag {
  padding: 6px 12px;
  border-radius: 980px;
  font-size: 12px;
  font-weight: 600;
}

.tag.meal-type {
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.tag.calories {
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

.tag.ai-tag {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.recipe-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f5f7;
  border-radius: 12px;
}

.meta-item {
  text-align: center;
}

.meta-label {
  display: block;
  font-size: 12px;
  color: #86868b;
  margin-bottom: 4px;
}

.meta-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #1d1d1f;
}

.recipe-section {
  margin-bottom: 20px;
}

.recipe-section h4 {
  color: #1d1d1f;
  font-size: 1.1rem;
  margin-bottom: 12px;
}

.ingredients-list,
.instructions-list {
  padding-left: 20px;
  color: #4a5568;
  line-height: 1.8;
}

.ingredients-list li,
.instructions-list li {
  margin-bottom: 8px;
}

.recipe-actions {
  margin-top: auto; /* 推到最底部 */
  padding-top: 16px;
}

.add-btn {
  width: 100%;
  background: linear-gradient(135deg, #34c759 0%, #30b34f 100%);
  color: white;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(52, 199, 89, 0.4);
}

.fix-btn {
  width: 100%;
  background: linear-gradient(135deg, #007aff 0%, #005ecb 100%);
  color: white;
  margin-top: 8px;
}

.fix-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.4);
}

.footer-tip {
  text-align: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  color: #4a5568;
  font-size: 0.95rem;
}

/* 错误状态 */
.error-container {
  text-align: center;
  color: white;
  padding: 60px 20px;
}

.error-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.retry-btn {
  background: white;
  color: #667eea;
  margin-top: 20px;
}

/* 打印样式 */
@media print {
  .ai-result-page {
    background: white;
    padding: 0;
  }
  
  .header-actions {
    display: none;
  }
  
  .recipe-card {
    break-inside: avoid;
    box-shadow: none;
    border: 1px solid #ddd;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .recipes-grid {
    grid-template-columns: 1fr;
  }
  
  .loading-steps {
    grid-template-columns: 1fr;
  }
  
  .results-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .header-actions {
    width: 100%;
    justify-content: center;
  }
}
</style>
