<template>
  <div class="recipes-layout">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>
        <svg class="header-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V20C2 20.5304 2.21071 21.0391 2.58579 21.4142C2.96086 21.7893 3.46957 22 4 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M18.5 2.50023C18.8978 2.1024 19.4374 1.87891 20 1.87891C20.5626 1.87891 21.1022 2.1024 21.4999 2.50023C21.8978 2.89789 22.1213 3.43762 22.1213 4.00023C22.1213 4.56284 21.8978 5.10257 21.4999 5.50023L12 15.0002L8 16.0002L9 12.0002L18.5 2.50023Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        健康食谱
      </h1>
      <div class="stats-bar">
        <span class="stat-item">📊 全部食谱：{{ filteredRecipes.length }} 个</span>
        <span class="stat-item" v-if="selectedType !== 'all'">🔍 分类：{{ mealTypes.find(t => t.value === selectedType)?.label }}</span>
      </div>
    </div>

    
    <div class="main-container">
      <!-- AI 智能推荐按钮 -->
      <div class="ai-generate-section">
        <button class="ai-generate-btn" @click="showMealTypeSelector">
          <div class="btn-content">
            <span class="ai-icon">✨</span>
            <div class="btn-text-group">
              <span class="btn-text">AI 智能生成健康食谱</span>
              <span class="btn-desc">根据您的健康档案，定制个性化食谱方案</span>
            </div>
            <span class="ai-arrow">→</span>
          </div>
        </button>
      </div>

      <!-- 餐类型选择弹窗 -->
      <div v-if="showSelector" class="modal-overlay" @click="closeSelector">
        <div class="meal-type-selector" @click.stop>
          <div class="selector-header">
            <h3>🍽️ 选择餐类型</h3>
            <p>请选择您想要生成的食谱类型</p>
          </div>
          
          <div class="meal-type-options">
            <div
              v-for="type in selectableMealTypes"
              :key="type.value"
              :class="['meal-option', { selected: selectedMealType === type.value }]"
              @click="selectedMealType = type.value"
            >
              <div class="option-emoji">{{ type.emoji }}</div>
              <div class="option-info">
                <div class="option-name">{{ type.label }}</div>
                <div class="option-desc">{{ type.description }}</div>
              </div>
              <div class="option-radio">
                <div :class="['radio-dot', { active: selectedMealType === type.value }]"></div>
              </div>
            </div>
          </div>
          
          <div class="selector-actions">
            <button class="cancel-btn" @click="closeSelector">取消</button>
            <button 
              class="confirm-btn" 
              @click="confirmAndGenerate"
              :disabled="!selectedMealType"
            >
              开始生成 ✨
            </button>
          </div>
        </div>
      </div>



      <!-- 食谱列表 -->
      <div class="recipe-list">
        <div v-if="filteredRecipes.length === 0" class="empty-state">
          <div class="empty-icon">📝</div>
          <p>暂无食谱</p>
          <p class="empty-hint" v-if="searchQuery || selectedType !== 'all'">尝试更换搜索词或切换分类</p>
        </div>

        <div
          v-else
          class="recipe-grid"
        >
          <div
            v-for="recipe in paginatedRecipes"
            :key="recipe.id"
            class="recipe-card"
          >
            <div class="recipe-image">
              <img
                v-if="recipe.image"
                :src="recipe.image"
                :alt="recipe.name"
                @error="handleImageError"
              />
              <div v-else class="no-image">😋</div>
            </div>

            <div class="recipe-info">
              <h4>{{ recipe.name }}</h4>
              <p class="description">{{ recipe.description }}</p>

              <div class="tags">
                <span class="tag meal-type">{{ recipe.mealType }}</span>
                <span class="tag calories">{{ recipe.calories }} kcal</span>
              </div>

              <div class="nutrition-preview" v-if="recipe.protein || recipe.carbs || recipe.fat">
                <div class="nutrition-item">
                  <span class="nutrient-label">蛋白质</span>
                  <span class="nutrient-value">{{ recipe.protein }}g</span>
                </div>
                <div class="nutrition-item">
                  <span class="nutrient-label">碳水</span>
                  <span class="nutrient-value">{{ recipe.carbs }}g</span>
                </div>
                <div class="nutrition-item">
                  <span class="nutrient-label">脂肪</span>
                  <span class="nutrient-value">{{ recipe.fat }}g</span>
                </div>
              </div>

              <div class="actions">
                <button class="btn-detail" @click.stop="viewDetail(recipe)" title="查看详情">
                  📋 详细
                </button>
                <button class="btn-print" @click.stop="printRecipe(recipe)" title="打印食谱">
                  🖨️
                </button>
                <button class="btn-delete" @click.stop="removeFromAllRecipes(recipe.id)" title="删除">
                  🗑️
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 分页控件 -->
        <div v-if="totalPages > 1" class="pagination">
          <button 
            class="page-btn" 
            @click="currentPage = 1" 
            :disabled="currentPage === 1"
            title="首页"
          >
            ⏮️
          </button>
          <button 
            class="page-btn" 
            @click="currentPage--" 
            :disabled="currentPage === 1"
            title="上一页"
          >
            ◀️
          </button>
          
          <div class="page-numbers">
            <button
              v-for="page in visiblePages"
              :key="page"
              :class="['page-number', { active: currentPage === page }]"
              @click="currentPage = page"
            >
              {{ page }}
            </button>
          </div>
          
          <button 
            class="page-btn" 
            @click="currentPage++" 
            :disabled="currentPage === totalPages"
            title="下一页"
          >
            ▶️
          </button>
          <button 
            class="page-btn" 
            @click="currentPage = totalPages" 
            :disabled="currentPage === totalPages"
            title="末页"
          >
            ⏭️
          </button>
        </div>
        
        <div class="pagination-info" v-if="totalPages > 1">
          第 {{ currentPage }} 页，共 {{ totalPages }} 页，每页 {{ pageSize }} 个
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';

const router = useRouter();

// 状态管理
const allRecipes = ref([]);
const selectedType = ref('all');
const searchQuery = ref('');
const currentPage = ref(1);
const pageSize = ref(12); // 每页显示 12 个食谱

// 餐类型选择器相关状态
const showSelector = ref(false);
const selectedMealType = ref('');

// 可供选择的膳食类型（带 emoji 和描述）
const selectableMealTypes = [
  { value: 'breakfast', label: '早餐', emoji: '🌅', description: '开启活力满满的一天' },
  { value: 'lunch', label: '午餐', emoji: '☀️', description: '营养均衡的午后能量' },
  { value: 'dinner', label: '晚餐', emoji: '🌙', description: '清淡健康的晚间美食' },
  { value: 'snack', label: '加餐', emoji: '🍎', description: '健康美味的零食小点' }
];

// 膳食类型
const mealTypes = [
  { value: 'all', label: '全部' },
  { value: 'breakfast', label: '早餐' },
  { value: 'lunch', label: '午餐' },
  { value: 'dinner', label: '晚餐' },
  { value: 'snack', label: '加餐' }
];

// 从所有食谱中删除
const removeFromAllRecipes = (recipeId) => {
  const index = allRecipes.value.findIndex(r => r.id === recipeId);
  if (index !== -1) {
    allRecipes.value.splice(index, 1);
    saveAllRecipesToStorage();
    ElMessage.success('已删除');
  }
};

// 从 localStorage 加载全部食谱
const loadAllRecipesFromStorage = () => {
  try {
    const stored = localStorage.getItem('allRecipes');
    if (stored) {
      allRecipes.value = JSON.parse(stored);
    }
  } catch (e) {
    console.error('加载全部食谱失败:', e);
  }
};

// 保存全部食谱到 localStorage
const saveAllRecipesToStorage = () => {
  localStorage.setItem('allRecipes', JSON.stringify(allRecipes.value));
};

// 图片加载失败处理
const handleImageError = (e) => {
  e.target.style.display = 'none';
};

// 显示餐类型选择器
const showMealTypeSelector = () => {
  selectedMealType.value = '';
  showSelector.value = true;
};

// 关闭选择器
const closeSelector = () => {
  showSelector.value = false;
};

// 确认并生成
const confirmAndGenerate = () => {
  if (!selectedMealType.value) {
    ElMessage.warning('请选择餐类型');
    return;
  }
  
  closeSelector();
  goToAIGenerate(selectedMealType.value);
};

// 查看食谱详情
const viewDetail = (recipe) => {
  // 使用 Element Plus 的 MessageBox 显示详情 - Apple 风格简约设计
  const detailContent = `
    <div class="apple-recipe-detail">
      <!-- 食谱头部 -->
      <div class="recipe-header">
        <h3 class="recipe-title">${recipe.name}</h3>
        <p class="recipe-description">${recipe.description || '无描述'}</p>
        
        <!-- 标签 -->
        <div class="recipe-tags">
          <span class="tag tag-meal">${recipe.mealType}</span>
          <span class="tag tag-calories">${recipe.calories} kcal</span>
        </div>
      </div>
      
      <!-- 营养成分 -->
      ${recipe.protein || recipe.carbs || recipe.fat ? `
        <div class="nutrition-section">
          <h4 class="section-title">营养成分</h4>
          <div class="nutrition-grid">
            ${recipe.protein ? `
              <div class="nutrition-card">
                <div class="nutrient-value">${recipe.protein}g</div>
                <div class="nutrient-label">蛋白质</div>
              </div>
            ` : ''}
            ${recipe.carbs ? `
              <div class="nutrition-card">
                <div class="nutrient-value">${recipe.carbs}g</div>
                <div class="nutrient-label">碳水化合物</div>
              </div>
            ` : ''}
            ${recipe.fat ? `
              <div class="nutrition-card">
                <div class="nutrient-value">${recipe.fat}g</div>
                <div class="nutrient-label">脂肪</div>
              </div>
            ` : ''}
          </div>
        </div>
      ` : ''}
      
      <!-- 所需食材 -->
      ${recipe.ingredients && recipe.ingredients.length > 0 ? `
        <div class="ingredients-section">
          <h4 class="section-title">
            <span class="section-icon">🥗</span>
            所需食材
          </h4>
          <ul class="ingredient-list">
            ${recipe.ingredients.map(item => `
              <li class="ingredient-item">${item}</li>
            `).join('')}
          </ul>
        </div>
      ` : ''}
      
      <!-- 制作步骤 -->
      ${recipe.instructions && recipe.instructions.length > 0 ? `
        <div class="instructions-section">
          <h4 class="section-title">
            <span class="section-icon">👨‍🍳</span>
            制作步骤
          </h4>
          <ol class="instruction-list">
            ${recipe.instructions.map((step, index) => `
              <li class="instruction-item">
                <span class="step-number">${index + 1}</span>
                <span class="step-text">${step}</span>
              </li>
            `).join('')}
          </ol>
        </div>
      ` : ''}
    </div>
  `;
  
  ElMessageBox.confirm(detailContent, '食谱详情', {
    confirmButtonText: '关闭',
    cancelButtonText: '',
    showCancelButton: false,
    dangerouslyUseHTMLString: true,
    customClass: 'apple-recipe-dialog'
  }).catch(() => {
    // 关闭对话框
  });
};

// 跳转到 AI 生成页面
const goToAIGenerate = (mealType) => {
  // 从 userProfile 中获取用户 ID
  let userId = null;
  
  try {
    const userProfileStr = localStorage.getItem('userProfile');
    if (userProfileStr) {
      const userProfile = JSON.parse(userProfileStr);
      userId = userProfile.id || userProfile.userId || userProfile.uid;
    }
  } catch (e) {
    console.error('解析 userProfile 失败:', e);
  }
  
  if (!userId) {
    ElMessage.warning('请先登录或设置健康档案');
    router.push('/profile');
    return;
  }
  
  // 在新标签页直接打开 AI 食谱结果页面
  const routeUrl = router.resolve({
    path: '/ai-recipe-result',
    query: {
      mealType: mealType.toUpperCase(), // 使用用户选择的餐类型
      userId: userId
    }
  });
  
  window.open(routeUrl.href, '_blank');
};

// 打印单个食谱
const printRecipe = (recipe) => {
  const printWindow = window.open('', '_blank');

  const printContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <title>${recipe.name}</title>
      <style>
        body {
          font-family: Arial, sans-serif;
          padding: 20px;
          color: #333;
        }
        .recipe-title {
          font-size: 24px;
          margin-bottom: 10px;
        }
        .recipe-description {
          color: #666;
          margin-bottom: 20px;
        }
        .recipe-meta {
          display: flex;
          gap: 15px;
          margin-bottom: 20px;
        }
        .meta-item {
          background: #f5f5f7;
          padding: 8px 12px;
          border-radius: 6px;
        }
        .section-title {
          font-size: 18px;
          margin: 20px 0 10px;
          border-bottom: 2px solid #007aff;
          padding-bottom: 5px;
        }
        .ingredients-list, .instructions-list {
          padding-left: 20px;
        }
        li {
          margin-bottom: 8px;
        }
        @media print {
          .no-print { display: none; }
        }
      </style>
    </head>
    <body>
      <h1 class="recipe-title">${recipe.name}</h1>
      <p class="recipe-description">${recipe.description || ''}</p>

      <div class="recipe-meta">
        <div class="meta-item">🍽️ ${recipe.mealType}</div>
        <div class="meta-item">🔥 ${recipe.calories} kcal</div>
        <div class="meta-item">💪 蛋白质${recipe.protein}g</div>
        <div class="meta-item">🍚 碳水${recipe.carbs}g</div>
        <div class="meta-item">🥑 脂肪${recipe.fat}g</div>
      </div>

      ${recipe.ingredients && recipe.ingredients.length > 0 ? `
        <h2 class="section-title">所需食材</h2>
        <ul class="ingredients-list">
          ${recipe.ingredients.map(item => `<li>${item}</li>`).join('')}
        </ul>
      ` : ''}

      ${recipe.instructions && recipe.instructions.length > 0 ? `
        <h2 class="section-title">制作步骤</h2>
        <ol class="instructions-list">
          ${recipe.instructions.map((step, i) => `<li>${step}</li>`).join('')}
        </ol>
      ` : ''}

      <div class="no-print" style="text-align: center; margin-top: 30px;">
        <button onclick="window.print()" style="padding: 12px 24px; background: #007aff; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 15px;">打印</button>
        <button onclick="window.close()" style="padding: 12px 24px; background: #f5f5f7; color: #333; border: 1px solid #ddd; border-radius: 8px; cursor: pointer; font-size: 15px; margin-left: 10px;">关闭</button>
      </div>
    </body>
    </html>
  `;

  printWindow.document.write(printContent);
  printWindow.document.close();

  setTimeout(() => {
    printWindow.focus();
    printWindow.print();
  }, 500);
};

// 计算属性：过滤后的食谱
const filteredRecipes = computed(() => {
  let recipes = allRecipes.value;
  
  // 按类型筛选
  if (selectedType.value !== 'all') {
    recipes = recipes.filter(r => r.mealType && r.mealType.toLowerCase() === selectedType.value.toLowerCase());
  }
  
  // 按搜索词筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    recipes = recipes.filter(r => 
      (r.name && r.name.toLowerCase().includes(query)) ||
      (r.description && r.description.toLowerCase().includes(query)) ||
      (r.ingredients && r.ingredients.some(ing => ing.toLowerCase().includes(query)))
    );
  }
  
  return recipes;
});

// 计算属性：分页后的食谱
const paginatedRecipes = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredRecipes.value.slice(start, end);
});

// 计算属性：总页数
const totalPages = computed(() => {
  return Math.ceil(filteredRecipes.value.length / pageSize.value);
});

// 计算属性：可见的页码
const visiblePages = computed(() => {
  const pages = [];
  const current = currentPage.value;
  const total = totalPages.value;
  
  for (let i = Math.max(1, current - 2); i <= Math.min(total, current + 2); i++) {
    pages.push(i);
  }
  
  return pages;
});

// 监听筛选和搜索变化，重置页码
watch([selectedType, searchQuery], () => {
  currentPage.value = 1;
});

onMounted(() => {
  loadAllRecipesFromStorage();
});
</script>

<style scoped>
.recipes-layout {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 80px;
}

.page-header {
  text-align: center;
  padding: 48px 24px 36px;
  position: relative;
  z-index: 1;
  background: linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
}

.page-header h1 {
  color: #1d1d1f;
  margin-bottom: 12px;
  font-weight: 700;
  font-size: 36px;
  line-height: 1.1;
  background: linear-gradient(135deg, #1d1d1f 0%, #424245 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.header-icon {
  width: 40px;
  height: 40px;
  color: #007aff;
  flex-shrink: 0;
}

.stats-bar {
  display: flex;
  justify-content: center;
  gap: 10px;
  align-items: center;
  font-size: 15px;
  color: #86868b;
  flex-wrap: wrap;
  margin-top: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  letter-spacing: -0.2px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 980px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.main-container {
  display: grid;
  gap: 20px;
  margin: 0 32px;
  position: relative;
  z-index: 1;
}

/* AI 生成按钮区域 */
.ai-generate-section {
  margin-bottom: 20px;
}

.ai-generate-btn {
  width: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  color: white;
  padding: 0;
  overflow: hidden;
}

.ai-generate-btn:hover {
  transform: translateY(-3px);
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.04),
    0 24px 64px rgba(0, 0, 0, 0.08);
}

.btn-content {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  gap: 16px;
}

.ai-icon {
  font-size: 32px;
  flex-shrink: 0;
  animation: sparkle 2s ease-in-out infinite;
}

@keyframes sparkle {
  0%, 100% {
    transform: scale(1) rotate(0deg);
    opacity: 1;
  }
  50% {
    transform: scale(1.1) rotate(10deg);
    opacity: 0.8;
  }
}

.btn-text-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.btn-text {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.2px;
  line-height: 1.3;
}

.btn-desc {
  font-size: 13px;
  opacity: 0.9;
  letter-spacing: -0.1px;
  line-height: 1.4;
}

.ai-arrow {
  font-size: 24px;
  flex-shrink: 0;
  transition: transform 0.3s ease;
  opacity: 0.8;
}

.ai-generate-btn:hover .ai-arrow {
  transform: translateX(4px);
  opacity: 1;
}

/* 餐类型选择器 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.meal-type-selector {
  background: #ffffff;
  border-radius: 24px;
  padding: 32px;
  width: 90%;
  max-width: 520px;
  box-shadow: 
    0 24px 80px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(0, 0, 0, 0.05);
  animation: slideUp 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.selector-header {
  text-align: center;
  margin-bottom: 32px;
}

.selector-header h3 {
  font-size: 26px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.selector-header p {
  font-size: 15px;
  color: #86868b;
  margin: 0;
  letter-spacing: -0.2px;
}

.meal-type-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 32px;
}

.meal-option {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f5f5f7;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: 2px solid transparent;
}

.meal-option:hover {
  background: #ebebf0;
  transform: translateX(4px);
}

.meal-option.selected {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  border-color: #667eea;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
}

.option-emoji {
  font-size: 40px;
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.option-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.option-name {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.3px;
}

.option-desc {
  font-size: 14px;
  color: #86868b;
  letter-spacing: -0.1px;
  line-height: 1.4;
}

.option-radio {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.radio-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #d1d1d6;
  background: #ffffff;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.radio-dot.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.selector-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.cancel-btn {
  padding: 14px 28px;
  background: #f5f5f7;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #86868b;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.cancel-btn:hover {
  background: #ebebf0;
  color: #1d1d1f;
}

.confirm-btn {
  padding: 14px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.confirm-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 768px) {
  .meal-type-selector {
    padding: 24px;
    width: 95%;
  }
  
  .selector-header h3 {
    font-size: 22px;
  }
  
  .option-emoji {
    width: 50px;
    height: 50px;
    font-size: 32px;
  }
  
  .option-name {
    font-size: 16px;
  }
  
  .option-desc {
    font-size: 13px;
  }
}

/* 筛选栏 */
.filter-bar {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 20px;
  border: none;
}

.filter-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  overflow-x: auto;
}

.tab-btn {
  padding: 12px 20px;
  border: none;
  background: #f5f5f7;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  white-space: nowrap;
  font-size: 15px;
  color: #1d1d1f;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.3);
}

.search-box {
  position: relative;
}

.search-box input {
  width: 100%;
  padding: 14px 16px;
  border: none;
  border-radius: 12px;
  font-size: 17px;
  background: #f5f5f7;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  color: #1d1d1f;
  letter-spacing: -0.2px;
}

.search-box input:focus {
  outline: none;
  background: #ebebf0;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}

.search-box input::placeholder {
  color: #86868b;
}

.search-icon {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: #86868b;
}

/* 分页控件 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 40px;
  padding: 20px 0;
}

.page-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: #f5f5f7;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #1d1d1f;
}

.page-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 8px;
}

.page-number {
  min-width: 40px;
  height: 40px;
  border: none;
  background: #f5f5f7;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  padding: 0 16px;
}

.page-number:hover {
  background: #ebebf0;
  transform: translateY(-2px);
}

.page-number.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  transform: scale(1.05);
}

.pagination-info {
  text-align: center;
  font-size: 14px;
  color: #86868b;
  margin-top: 16px;
  letter-spacing: -0.1px;
}

/* 食谱列表 */
.recipe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.recipe-card {
  background: #ffffff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  transition: all 0.5s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
}

.recipe-card:hover {
  transform: translateY(-3px);
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.04),
    0 24px 64px rgba(0, 0, 0, 0.08);
}

.recipe-image {
  width: 100%;
  height: 220px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recipe-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  font-size: 80px;
}

.recipe-info {
  padding: 24px;
}

.recipe-info h4 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #1d1d1f;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.description {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #86868b;
  line-height: 1.6;
  letter-spacing: -0.1px;
}

.tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.tag {
  padding: 6px 12px;
  border-radius: 980px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.tag.meal-type {
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.tag.calories {
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

.nutrition-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
  padding: 12px;
  background: #f5f5f7;
  border-radius: 12px;
}

.nutrition-item {
  text-align: center;
}

.nutrient-label {
  display: block;
  font-size: 12px;
  color: #86868b;
  margin-bottom: 4px;
}

.nutrient-value {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #1d1d1f;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.actions button {
  padding: 10px 16px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  white-space: nowrap;
}

.btn-detail {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  flex: 1;
}

.btn-detail:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.btn-print {
  background: #f5f5f7;
  color: #1d1d1f;
}

.btn-print:hover {
  background: #e8e8ed;
}

.btn-delete {
  background: #ffebee;
  color: #f44336;
}

.btn-delete:hover {
  background: #ffcdd2;
}

.actions button:hover {
  transform: scale(1.05);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.empty-state p {
  margin: 10px 0;
  color: #86868b;
}

.empty-hint {
  font-size: 14px;
  color: #86868b;
}
</style>

<!-- 全局样式 - Apple 风格食谱详情对话框 -->
<style>
/* Apple 风格食谱详情整体样式 */
.apple-recipe-dialog.el-message-box {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  max-width: 720px;
  width: 90%;
  border: none;
}

/* 隐藏默认头部 */
.apple-recipe-dialog .el-message-box__header {
  display: none;
}

/* 内容区域 */
.apple-recipe-dialog .el-message-box__content {
  padding: 0;
  max-height: 75vh;
  overflow-y: auto;
}

/* 自定义滚动条 - Webkit */
.apple-recipe-detail::-webkit-scrollbar {
  width: 6px;
}

.apple-recipe-detail::-webkit-scrollbar-track {
  background: transparent;
}

.apple-recipe-detail::-webkit-scrollbar-thumb {
  background: #d1d1d6;
  border-radius: 3px;
}

.apple-recipe-detail::-webkit-scrollbar-thumb:hover {
  background: #aeaeb2;
}

/* Firefox 滚动条 */
.apple-recipe-detail {
  scrollbar-width: thin;
  scrollbar-color: #d1d1d6 transparent;
}

/* 食谱详情内容容器 */
.apple-recipe-detail {
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
  color: #1d1d1f;
  line-height: 1.6;
}

/* 食谱头部区域 */
.recipe-header {
  padding: 40px 40px 32px;
  background: linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
  border-bottom: 1px solid #f5f5f7;
}

.recipe-title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0 0 12px 0;
  color: #1d1d1f;
  line-height: 1.2;
}

.recipe-description {
  font-size: 17px;
  color: #86868b;
  margin: 0 0 20px 0;
  line-height: 1.6;
  letter-spacing: -0.01em;
}

.recipe-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tag {
  padding: 8px 16px;
  border-radius: 980px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.01em;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
}

.tag:hover {
  transform: translateY(-2px);
}

.tag-meal {
  background: rgba(0, 122, 255, 0.08);
  color: #007aff;
}

.tag-calories {
  background: rgba(255, 149, 0, 0.08);
  color: #ff9500;
}

/* 营养成分区域 */
.nutrition-section {
  padding: 32px 40px;
  background: #ffffff;
  border-bottom: 1px solid #f5f5f7;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0 0 24px 0;
  color: #1d1d1f;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  font-size: 22px;
}

.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.nutrition-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #f1f3f5 100%);
  padding: 24px 20px;
  border-radius: 16px;
  text-align: center;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.nutrition-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
}

.nutrient-value {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #007aff 0%, #5ac8fa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: block;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.nutrient-label {
  font-size: 14px;
  color: #86868b;
  font-weight: 500;
  letter-spacing: -0.01em;
}

/* 所需食材区域 */
.ingredients-section {
  padding: 32px 40px;
  background: #ffffff;
  border-bottom: 1px solid #f5f5f7;
}

.ingredient-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ingredient-item {
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 16px;
  color: #424245;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  border-left: 3px solid transparent;
}

.ingredient-item:hover {
  background: #f5f5f7;
  border-left-color: #007aff;
  transform: translateX(4px);
}

/* 制作步骤区域 */
.instructions-section {
  padding: 32px 40px;
  background: #ffffff;
}

.instruction-list {
  list-style: none;
  padding: 0;
  margin: 0;
  counter-reset: step-counter;
}

.instruction-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: #fafafa;
  border-radius: 12px;
  margin-bottom: 16px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  border-left: 3px solid transparent;
}

.instruction-item:hover {
  background: #f5f5f7;
  border-left-color: #5ac8fa;
  transform: translateX(4px);
}

.step-number {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #007aff 0%, #5ac8fa 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2);
}

.step-text {
  flex: 1;
  font-size: 16px;
  color: #424245;
  line-height: 1.7;
  padding-top: 2px;
}

/* 底部按钮区域 */
.apple-recipe-dialog .el-message-box__footer {
  padding: 20px 24px;
  background: #fafafa;
  border-top: 1px solid #e8e8ed;
}

.apple-recipe-dialog .el-button--primary {
  background: linear-gradient(135deg, #007aff 0%, #5ac8fa 100%) !important;
  border: none !important;
  padding: 12px 40px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.25) !important;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
}

.apple-recipe-dialog .el-button--primary:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.35) !important;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .apple-recipe-dialog.el-message-box {
    width: 95%;
    max-width: 95%;
  }
  
  .recipe-header,
  .nutrition-section,
  .ingredients-section,
  .instructions-section {
    padding-left: 24px;
    padding-right: 24px;
  }
  
  .recipe-title {
    font-size: 24px;
  }
  
  .nutrition-grid {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }
  
  .nutrient-value {
    font-size: 28px;
  }
  
  .page-header h1 {
    font-size: 32px;
  }
  
  .main-container {
    margin: 0 20px;
  }
  
  .filter-bar {
    padding: 20px;
  }
  
  .filter-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  
  .recipe-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }
  
  .pagination {
    flex-wrap: wrap;
    gap: 6px;
  }
  
  .page-btn,
  .page-number {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .recipe-grid {
    grid-template-columns: 1fr;
  }
  
  .page-numbers {
    display: none;
  }
}
</style>
