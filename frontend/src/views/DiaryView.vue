<template>
  <div class="diary-layout">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>🍽️ 饮食日记</h1>
      <div class="stats-bar">
        <span class="stat-item">📊 今日记录: {{ diaryEntries.length }} 条</span>
        <span class="stat-item">🔥 总热量: {{ totalNutrition.calories }}kcal</span>
      </div>
    </div>
    
    <div class="main-container">
      <!-- 左侧：记录区域 -->
      <div class="left-panel">
        <div class="card record-card">
          <!-- 保存记录时的加载动画 - 居中显示 -->
          <div v-if="savingRecord" class="loading-overlay">
            <div class="loading-content">
              <div class="loading-spinner">
                <div class="spinner-ring"></div>
                <div class="spinner-icon">💾</div>
              </div>
              <p class="loading-text">⏳ 正在保存记录...</p>
            </div>
          </div>
          
          <h2>➕ 添加新记录</h2>
          
          <!-- 餐别选择 -->
          <div class="meal-selection">
            <label>🍽️ 餐别：</label>
            <div class="meal-buttons">
              <button 
                v-for="meal in mealOptions" 
                :key="meal.value"
                class="meal-btn"
                :class="{ active: newDiaryEntry.mealType === meal.value }"
                @click="newDiaryEntry.mealType = meal.value"
              >
                {{ meal.icon }} {{ meal.label }}
              </button>
            </div>
          </div>

          <!-- 食物输入区域 -->
          <div class="food-input-section">
            <div class="input-row">
              <div class="food-name-input">
                <label>🥗 食物名称：</label>
                <input
                  v-model="currentFoodItem.name"
                  placeholder="输入食物名称"
                  class="food-input"
                  @keyup.enter="addFoodItem"
                />
              </div>
              <div class="quantity-input">
                <label>⚖️ 重量/份量：</label>
                <div class="quantity-combo">
                  <input
                    v-model="currentFoodItem.quantity"
                    type="number"
                    placeholder=""
                    class="quantity-number"
                    min="0"
                    step="0.1"
                  />
                  <select v-model="currentFoodItem.unit" class="unit-select">
                    <option value="克">克</option>
                    <option value="斤">斤</option>
                    <option value="两">两</option>
                  </select>
                </div>
              </div>
              <button @click="addFoodItem" class="add-food-btn">+</button>
            </div>

            <!-- 已添加的食物列表 -->
            <div v-if="foodItems.length > 0" class="added-foods">
              <h3>✅ 已添加的食物：</h3>
              <div class="food-items-list">
                <div 
                  v-for="(item, index) in foodItems" 
                  :key="index" 
                  class="food-item-tag"
                >
                  <span class="food-name">{{ item.name }}</span>
                  <span class="food-quantity">⚖️ {{ item.quantity }}{{ item.unit }}</span>
                  <button @click="removeFoodItem(index)" class="remove-btn">×</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <button 
              class="btn analyze-btn" 
              @click="analyzeFoodItems" 
              :disabled="analyzing || foodItems.length === 0"
            >
              🤖 {{ analyzing ? '🔄 分析中...' : '智能分析' }}
            </button>
            <button 
              class="btn upload-btn" 
              @click="triggerImageUpload"
              :disabled="uploading"
            >
              📸 {{ uploading ? '⏳ 上传中...' : '拍照上传' }}
            </button>
            <input 
              type="file" 
              ref="imageUploader" 
              @change="handleImageUpload" 
              accept="image/*" 
              style="display: none;" 
            />
            <button 
              class="btn save-btn" 
              @click="recordDiet"
              :disabled="!hasAnalyzed || foodItems.length === 0"
            >
              💾 记录
            </button>
          </div>



          <!-- 分析结果 -->
          <div v-if="recognizedFoods.length > 0" class="analysis-results">
            <h3>🔍 识别到的食物：</h3>
            <div class="foods-display">
              <span
                v-for="(food, index) in recognizedFoods"
                :key="index"
                class="food-tag"
                :class="getFoodTypeClass(food)"
              >
                {{ food.name }} ({{ food.weightGrams }}g)
              </span>
            </div>
          </div>

          <!-- 营养预估 -->
          <div v-if="nutritionEstimate" class="nutrition-preview">
            <h3>📋 营养成分预估：</h3>
            <div class="nutrition-grid">
              <div class="nutrient-item calories">
                <div class="value">{{ nutritionEstimate.calories }}</div>
                <div class="unit">kcal</div>
                <div class="label">热量</div>
              </div>
              <div class="nutrient-item protein">
                <div class="value">{{ nutritionEstimate.protein }}</div>
                <div class="unit">g</div>
                <div class="label">蛋白质</div>
              </div>
              <div class="nutrient-item carbs">
                <div class="value">{{ nutritionEstimate.carbs }}</div>
                <div class="unit">g</div>
                <div class="label">碳水</div>
              </div>
              <div class="nutrient-item fat">
                <div class="value">{{ nutritionEstimate.fat }}</div>
                <div class="unit">g</div>
                <div class="label">脂肪</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：历史记录 -->
      <div class="right-panel">
        <div class="card history-card">
          <div class="history-header">
            <h2>📖 今日饮食记录</h2>
            <div class="summary-stats">
              <span class="summary-item">🔥 总热量: {{ totalNutrition.calories }}kcal</span>
              <span class="summary-item">🥩 蛋白质: {{ totalNutrition.protein }}g</span>
            </div>
          </div>

          <!-- 加载状态 - 居中显示 -->
          <div v-if="loading" class="loading-overlay">
            <div class="loading-content">
              <div class="loading-spinner">
                <div class="spinner-ring"></div>
                <div class="spinner-icon">🍽️</div>
              </div>
              <p class="loading-text">⏳ 正在加载记录...</p>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else-if="diaryEntries.length === 0" class="empty-state">
            <div class="empty-icon">🍽️</div>
            <h3>还没有记录</h3>
            <p>今天还没有记录饮食哦～</p>
            <p class="tip">💪 快去记录你的第一餐吧！</p>
          </div>

          <!-- 批量操作栏 -->
          <div v-else class="batch-actions-bar">
            <label class="select-all-label">
              <input 
                type="checkbox" 
                v-model="selectAll"
                @change="toggleSelectAll"
              >
              <span>☑️ 全选</span>
            </label>
            <span class="selected-count" v-if="selectedRecords.length > 0">
              已选择 {{ selectedRecords.length }} 条
            </span>
            <button 
              v-if="selectedRecords.length > 0"
              @click="batchDelete"
              class="btn-batch-delete"
              :disabled="batchDeleting"
            >
              🗑️ {{ batchDeleting ? '删除中...' : `批量删除 (${selectedRecords.length})` }}
            </button>
          </div>

          <!-- 记录列表 -->
          <div class="records-container">
            <div class="record-item" v-for="(entry, index) in diaryEntries" :key="entry.id || index" :class="{ 'selected': selectedRecords.includes(entry.id) }">
              <div class="record-header">
                <div class="meal-info">
                  <input 
                    type="checkbox" 
                    v-model="selectedRecords" 
                    :value="entry.id"
                    class="record-checkbox"
                    @change="updateSelectAllState"
                  >
                  <span class="meal-badge" :class="getMealTypeClass(entry.mealType)">
                    {{ getMealTypeName(entry.mealType) }}
                  </span>
                  <span class="time-stamp">🕐 {{ formatTime(entry.recordedAt) }}</span>
                </div>
                <div class="record-actions">
                  <div class="calories-badge">
                    🔥 {{ entry.calories || 0 }}<span class="unit">kcal</span>
                  </div>
                  <button 
                    @click="deleteRecord(entry.id, index)" 
                    class="delete-btn"
                    title="删除这条记录"
                  >
                    🗑️
                  </button>
                </div>
              </div>
              
              <div class="record-body">
                <p class="food-desc">{{ entry.foodDescription }}</p>
                
                <div class="nutrition-chips" v-if="entry.protein || entry.carbs || entry.fat">
                  <span v-if="entry.protein" class="chip protein">蛋白质: {{ entry.protein }}g</span>
                  <span v-if="entry.carbs" class="chip carbs">碳水: {{ entry.carbs }}g</span>
                  <span v-if="entry.fat" class="chip fat">脂肪: {{ entry.fat }}g</span>
                </div>
                
                <div v-if="entry.consumedIngredients && entry.consumedIngredients.length > 0" class="ingredients-section">
                  <strong>包含食材：</strong>
                  <div class="ingredient-tags">
                    <span class="ingredient-tag" v-for="ingredient in entry.consumedIngredients" :key="ingredient">
                      {{ ingredient }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
        
    <!-- 底部统计区域 -->
    <div class="bottom-summary-section">
      <div class="summary-card">
        <!-- 今日营养总计 -->
        <div class="summary-section" v-if="diaryEntries.length > 0">
          <h3>📈 今日营养总计</h3>
          <div class="summary-grid">
            <div class="summary-box calories">
              <div class="big-number">{{ totalNutrition.calories }}</div>
              <div class="metric">总热量</div>
              <div class="small-unit">kcal</div>
            </div>
            <div class="summary-box protein">
              <div class="big-number">{{ totalNutrition.protein }}</div>
              <div class="metric">蛋白质</div>
              <div class="small-unit">g</div>
            </div>
            <div class="summary-box carbs">
              <div class="big-number">{{ totalNutrition.carbs }}</div>
              <div class="metric">碳水化合物</div>
              <div class="small-unit">g</div>
            </div>
            <div class="summary-box fat">
              <div class="big-number">{{ totalNutrition.fat }}</div>
              <div class="metric">脂肪</div>
              <div class="small-unit">g</div>
            </div>
          </div>
              
          <!-- AI 营养分析按钮 -->
          <div class="ai-analysis-section">
            <button 
              @click="analyzeNutrition" 
              class="btn-ai-analyze"
              :disabled="analyzingNutrition"
            >
              {{ analyzingNutrition ? '🔄 分析中...' : '🤖 AI 智能分析建议' }}
            </button>
                
            <!-- AI 分析结果 -->
            <div v-if="nutritionAnalysis" class="analysis-result">
              <h4>💡 营养分析报告</h4>
              <div class="analysis-content" v-html="formatAnalysis(nutritionAnalysis)"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 手动输入营养成分模态框 -->
    <ManualNutritionInput
      :is-visible="showManualInput"
      :food-item="currentManualFood"
      @close="closeManualInput"
      @save="handleManualNutritionSave"
    />
    
    <!-- AI 分析失败提示 -->
    <div v-if="analyzing" class="ai-analyzing">
      <div class="analyzing-content">
        <div class="spinner"></div>
        <p>🤖 正在使用豆包 AI 进行智能分析...</p>
        <p class="analyzing-tip">💡 提示：AI 分析通常需要 10-30 秒，请耐心等待</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { healthApi } from '../api/healthApi'
import { useUserStore } from '../stores/userStore'
import ManualNutritionInput from '../components/ManualNutritionInput.vue'
import { ElNotification, ElMessageBox } from 'element-plus'

// 使用环境变量配置 API 基础 URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const userStore = useUserStore()

// 当前正在输入的食物项
const currentFoodItem = ref({
  name: '',
  quantity: '',
  unit: '克'
})

// 已添加的食物列表
const foodItems = ref([])

const newDiaryEntry = ref({
  mealType: 'BREAKFAST',
  foodDescription: '',
  consumedIngredients: [],
  calories: null,
  protein: null,
  carbs: null,
  fat: null,
  fiber: null
})

const diaryEntries = ref([])
const loading = ref(false)
const savingRecord = ref(false) // 保存记录时的加载状态
const analyzing = ref(false)
const recognizedFoods = ref([])
const nutritionEstimate = ref(null)
const showManualInput = ref(false)
const currentManualFood = ref(null)
const analyzingNutrition = ref(false)
const nutritionAnalysis = ref('')
const imageUploader = ref(null)
const uploading = ref(false)
const hasAnalyzed = ref(false) // 是否已经过 AI 分析

// 批量删除相关
const selectedRecords = ref([])
const selectAll = ref(false)
const batchDeleting = ref(false)

// 餐别选项
const mealOptions = [
  { value: 'BREAKFAST', label: '早餐', icon: '🍳' },
  { value: 'LUNCH', label: '午餐', icon: '🍚' },
  { value: 'DINNER', label: '晚餐', icon: '🍜' },
  { value: 'SNACK', label: '加餐', icon: '🍎' }
]

// 计算今日营养成分总计
const totalNutrition = computed(() => {
  const totals = {
    calories: 0,
    protein: 0,
    carbs: 0,
    fat: 0,
    fiber: 0
  }

  diaryEntries.value.forEach(entry => {
    // 添加安全检查，确保属性存在且为有效数字
    if (entry.calories && typeof entry.calories === 'number' && !isNaN(entry.calories)) {
      totals.calories += entry.calories
    }
    if (entry.protein && typeof entry.protein === 'number' && !isNaN(entry.protein)) {
      totals.protein += entry.protein
    }
    if (entry.carbs && typeof entry.carbs === 'number' && !isNaN(entry.carbs)) {
      totals.carbs += entry.carbs
    }
    if (entry.fat && typeof entry.fat === 'number' && !isNaN(entry.fat)) {
      totals.fat += entry.fat
    }
    if (entry.fiber && typeof entry.fiber === 'number' && !isNaN(entry.fiber)) {
      totals.fiber += entry.fiber
    }
  })

  return {
    calories: Math.round(totals.calories),
    protein: Math.round(totals.protein * 10) / 10,
    carbs: Math.round(totals.carbs * 10) / 10,
    fat: Math.round(totals.fat * 10) / 10,
    fiber: Math.round(totals.fiber * 10) / 10
  }
})

// 添加食物项
const addFoodItem = () => {
  if (!currentFoodItem.value.name.trim() || !currentFoodItem.value.quantity) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请输入食物名称和数量',
      type: 'warning',
      duration: 2000,
      offset: 80
    })
    return
  }

  foodItems.value.push({
    name: currentFoodItem.value.name.trim(),
    quantity: parseFloat(currentFoodItem.value.quantity),
    unit: currentFoodItem.value.unit
  })

  // 清空输入框
  currentFoodItem.value.name = ''
  currentFoodItem.value.quantity = ''
  currentFoodItem.value.unit = '克'
}

// 删除食物项
const removeFoodItem = (index) => {
  foodItems.value.splice(index, 1)
}

// 智能分析食物输入 - 优先使用数据库匹配
const analyzeFoodItems = async () => {
  if (foodItems.value.length === 0) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请先添加食物',
      type: 'warning',
      duration: 2000,
      offset: 80
    })
    return
  }

  try {
    analyzing.value = true

    // 构造食物描述字符串
    const foodDescription = foodItems.value.map(item => 
      `${item.quantity}${item.unit}${item.name}`
    ).join('、')

    console.log('🔍 准备智能匹配，食物描述:', foodDescription)

    // 第一步：尝试智能匹配数据库中的食材
    let matchResult = null
    let needAIAnalysis = true
    
    try {
      const matchResponse = await axios.post('/api/smart-match/ingredients', {
        input: foodDescription
      }, {
        timeout: 10000
      })
      
      matchResult = matchResponse.data
      console.log('✅ 智能匹配成功:', matchResult)
      console.log('  - matched:', matchResult.matched)
      console.log('  - needAIAnalysis:', matchResult.needAIAnalysis)
      console.log('  - matchedFoods:', matchResult.matchedFoods)
      console.log('  - unknownFoods:', matchResult.unknownFoods)
      
      // 如果全部匹配成功，不需要调用 AI
      if (matchResult.matched && !matchResult.needAIAnalysis) {
        needAIAnalysis = false
        
        // 使用数据库数据构建结果（保留重复项）
        recognizedFoods.value = matchResult.matchedFoods.map((foodName, index) => ({
          name: foodName,
          weightGrams: parseInt(foodItems.value[index]?.quantity) || 100
        }))
        
        // 将匹配到的食物名称填充到 consumedIngredients（保留重复）
        newDiaryEntry.value.consumedIngredients = [...matchResult.matchedFoods]
        
        // 计算总营养（从数据库食材累加，每个食材独立计算）
        const ingredients = matchResult.matchedIngredients || []
        let totalCalories = 0
        let totalProtein = 0
        let totalCarbs = 0
        let totalFat = 0
        let totalFiber = 0
        
        // 遍历每个匹配的食材（包括重复的）
        ingredients.forEach((ingredient, idx) => {
          const quantity = parseInt(foodItems.value[idx]?.quantity) || 100
          const ratio = quantity / 100.0
          
          totalCalories += (ingredient.caloriesPer100g || 0) * ratio
          totalProtein += (ingredient.proteinPer100g || 0) * ratio
          totalCarbs += (ingredient.carbsPer100g || 0) * ratio
          totalFat += (ingredient.fatPer100g || 0) * ratio
          totalFiber += (ingredient.fiberPer100g || 0) * ratio
          
          console.log(`食材 ${idx + 1}: ${ingredient.name}, 重量：${quantity}g, 热量：${(ingredient.caloriesPer100g || 0) * ratio}kcal`)
        })
        
        nutritionEstimate.value = {
          calories: Math.round(totalCalories),
          protein: totalProtein.toFixed(1),
          carbs: totalCarbs.toFixed(1),
          fat: totalFat.toFixed(1),
          fiber: totalFiber.toFixed(1)
        }
        
        console.log('使用数据库数据，无需调用 AI')
        ElNotification({
          title: '✅ 快速匹配',
          message: `已从数据库匹配 ${matchResult.matchedFoods.length} 种食材`,
          type: 'success',
          duration: 2000,
          offset: 80
        })
      }
    } catch (matchError) {
      console.log('❌ 智能匹配失败，将使用 AI 分析:', matchError.message)
      console.log('错误详情:', matchError.response?.data || matchError)
    }
    
    // 第二步：如果有未匹配的食材，调用 AI 分析
    if (needAIAnalysis) {
      console.log('调用 AI 分析...')
      
      // 使用 axios 调用后端 AI 分析 API，支持更长超时（10 分钟）
      const response = await axios.post('/api/diet/smart-analyze', {
        foodDescription: foodDescription
      }, {
        timeout: 600000,  // AI 分析可能需要更长时间，设置为 10 分钟
        headers: {
          'Content-Type': 'application/json'
        }
      })

      const result = response.data
      
      // 显示识别到的食物
      recognizedFoods.value = result.foods || []
      
      // 将识别到的食物名称填充到 consumedIngredients
      newDiaryEntry.value.consumedIngredients = (result.foods || []).map(food => food.name)

      // 显示营养成分预估
      nutritionEstimate.value = result.totalNutrition
      
      ElNotification({
        title: '🤖 AI 分析完成',
        message: '已完成智能营养分析',
        type: 'success',
        duration: 2000,
        offset: 80
      })
    }

    // 自动填充营养成分
    newDiaryEntry.value.calories = nutritionEstimate.value?.calories || null
    newDiaryEntry.value.protein = nutritionEstimate.value?.protein || null
    newDiaryEntry.value.carbs = nutritionEstimate.value?.carbs || null
    newDiaryEntry.value.fat = nutritionEstimate.value?.fat || null
    newDiaryEntry.value.fiber = nutritionEstimate.value?.fiber || null

    // 标记已完成 AI 分析
    hasAnalyzed.value = true

    ElNotification({
      title: '✅ 分析成功',
      message: '食物营养分析完成！',
      type: 'success',
      duration: 2000,
      offset: 80
    })

  } catch (error) {
    console.error('分析失败:', error)
    ElNotification({
      title: '❌ 分析失败',
      message: error.response?.data?.message || error.message || '分析过程中出现错误',
      type: 'error',
      duration: 3000,
      offset: 80
    })
  } finally {
    analyzing.value = false
  }
}

const recordDiet = async () => {
  if (foodItems.value.length === 0) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请添加至少一种食物',
      type: 'warning',
      duration: 2000,
      offset: 80
    })
    return
  }

  try {
    savingRecord.value = true // 使用本地加载状态
    userStore.clearError()

    // 构造食物描述
    const foodDescription = foodItems.value.map(item => 
      `${item.quantity}${item.unit}${item.name}`
    ).join('、')

    const dietData = {
      userId: userStore.userData.userId,
      mealType: newDiaryEntry.value.mealType,
      foodDescription: foodDescription,
      consumedIngredients: newDiaryEntry.value.consumedIngredients.filter(item => item.trim()),
      calories: newDiaryEntry.value.calories || null,
      protein: newDiaryEntry.value.protein || null,
      carbs: newDiaryEntry.value.carbs || null,
      fat: newDiaryEntry.value.fat || null,
      fiber: newDiaryEntry.value.fiber || null
    }

    const result = await healthApi.recordDiet(dietData)
    ElNotification({
      title: '✅ 保存成功',
      message: '饮食记录成功！',
      type: 'success',
      duration: 2000,
      offset: 80
    })

    // 清空表单
    resetForm()

    // 重新加载今日记录
    await loadTodayRecords()
  } catch (err) {
    userStore.setError(err.message || '记录饮食失败')
    console.error('记录饮食失败:', err)
  } finally {
    savingRecord.value = false // 关闭本地加载状态
  }
}

const loadTodayRecords = async () => {
  try {
    loading.value = true
    const now = new Date()
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    const userId = userStore.userData?.userId

    if (!userId) {
      console.error('用户 ID 为空，无法加载记录')
      return
    }
    
    const records = await healthApi.getDailyDiet(userId, today)

    // 过滤掉喝水打卡记录
    const filteredRecords = (records || []).filter(record => 
      !(record.foodDescription && 
        (record.foodDescription.includes('💧 喝水打卡') || 
         record.foodDescription.toLowerCase().includes('water 打卡')))
    )
    
    diaryEntries.value = filteredRecords
    
    // 清空选中状态，避免缓存问题
    selectedRecords.value = []
    selectAll.value = false
  } catch (err) {
    console.error('加载今日记录失败:', err)
    userStore.setError(err.message || '加载记录失败')
  } finally {
    loading.value = false
  }
}

// 手动输入相关方法
const openManualInput = () => {
  if (foodItems.value.length === 0) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请先添加食物',
      type: 'warning',
      duration: 2000,
      offset: 80
    })
    return
  }
  
  // 如果有多项食物，默认编辑第一项
  currentManualFood.value = foodItems.value[0]
  showManualInput.value = true
}

const closeManualInput = () => {
  showManualInput.value = false
  currentManualFood.value = null
}

const handleManualNutritionSave = (nutritionData) => {
  // 更新营养成分数据
  newDiaryEntry.value.calories = nutritionData.calories
  newDiaryEntry.value.protein = nutritionData.protein
  newDiaryEntry.value.carbs = nutritionData.carbs
  newDiaryEntry.value.fat = nutritionData.fat
  newDiaryEntry.value.fiber = nutritionData.fiber
  
  closeManualInput()
  ElNotification({
    title: '✅ 保存成功',
    message: '手动营养成分输入完成！',
    type: 'success',
    duration: 2000,
    offset: 80
  })
}

const resetForm = () => {
  newDiaryEntry.value = {
    mealType: 'BREAKFAST',
    foodDescription: '',
    consumedIngredients: [],
    calories: null,
    protein: null,
    carbs: null,
    fat: null,
    fiber: null
  }
  foodItems.value = []
  currentFoodItem.value = {
    name: '',
    quantity: '',
    unit: '克'
  }
  recognizedFoods.value = []
  nutritionEstimate.value = null
  showManualInput.value = false
  currentManualFood.value = null
  hasAnalyzed.value = false
}

// AI 营养分析
const analyzeNutrition = async () => {
  if (diaryEntries.value.length === 0) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请先添加饮食记录',
      type: 'warning',
      duration: 2000,
      offset: 80
    })
    return
  }

  try {
    analyzingNutrition.value = true
    nutritionAnalysis.value = ''

    const userId = userStore.userData?.userId
    if (!userId) {
      ElNotification({
        title: '⚠️ 提示',
        message: '请先登录',
        type: 'warning',
        duration: 2000,
        offset: 80
      })
      return
    }

    const nutritionData = {
      calories: totalNutrition.value.calories,
      protein: totalNutrition.value.protein,
      carbs: totalNutrition.value.carbs,
      fat: totalNutrition.value.fat,
      fiber: totalNutrition.value.fiber,
      mealCount: diaryEntries.value.length
    }

    console.log('发送营养分析请求:', { userId, nutritionData })

    const apiUrl = API_BASE_URL.startsWith('/api') 
      ? window.location.origin + API_BASE_URL 
      : API_BASE_URL;

    const response = await axios.post(
      `${apiUrl}/diet/analyze-nutrition/${userId}`,
      nutritionData,
      {
        timeout: 600000,  // 营养分析可能需要更长时间，设置为 10 分钟
        headers: { 'Content-Type': 'application/json' }
      }
    )

    console.log('营养分析响应:', response.data)

    if (response.data.success) {
      nutritionAnalysis.value = response.data.analysis
    } else {
      ElNotification({
        title: '❌ 分析失败',
        message: response.data.error,
        type: 'error',
        duration: 3000,
        offset: 80
      })
    }
  } catch (err) {
    console.error('营养分析失败:', err)
    if (err.code === 'ECONNABORTED') {
      ElNotification({
        title: '⏰ 超时提示',
        message: '分析超时，请稍后重试',
        type: 'warning',
        duration: 3000,
        offset: 80
      })
    } else {
      ElNotification({
        title: '❌ 分析失败',
        message: '营养分析失败：' + err.message,
        type: 'error',
        duration: 3000,
        offset: 80
      })
    }
  } finally {
    analyzingNutrition.value = false
  }
}

// 格式化分析结果（支持Markdown转HTML）
const formatAnalysis = (text) => {
  if (!text) return ''
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
    .replace(/📊/g, '<span class="emoji">📊</span>')
    .replace(/⚠️/g, '<span class="emoji">⚠️</span>')
    .replace(/✅/g, '<span class="emoji">✅</span>')
    .replace(/💡/g, '<span class="emoji">💡</span>')
}

// 触发图片上传
const triggerImageUpload = () => {
  imageUploader.value.click()
}

// 处理图片上传
const handleImageUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  uploading.value = true
  const formData = new FormData()
  formData.append('image', file)
  if (userStore.userData?.userId) {
    formData.append('userId', userStore.userData.userId);
  }

  try {
    // 使用环境变量配置的 API 地址
    const apiUrl = API_BASE_URL.startsWith('/api') 
      ? window.location.origin + API_BASE_URL 
      : API_BASE_URL;
    
    const response = await axios.post(`${apiUrl}/image/recognize`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 600000, // 图片识别可能需要更长时间，设置为 10 分钟
    });

    const result = response.data;
    if (result.foods && result.foods.length > 0) {
      // 清空现有食物项
      foodItems.value = [];
      // 将识别结果填充到食物列表
      result.foods.forEach(food => {
        foodItems.value.push({
          name: food.name,
          quantity: food.weightGrams,
          unit: '克',
        });
      });
      ElNotification({
        title: '📸 识别成功',
        message: `共识别到 ${result.foods.length} 种食物`,
        type: 'success',
        duration: 3000,
        offset: 80
      });
    } else {
      ElNotification({
        title: 'ℹ️ 提示',
        message: '图片识别成功，但未能识别出任何食物',
        type: 'info',
        duration: 3000,
        offset: 80
      });
    }

  } catch (err) {
    console.error('图片识别失败:', err);
    
    // 检查是否是 API 配额限制错误
    const isQuotaError = err.message && (
      err.message.includes('配额限制') || 
      err.message.includes('429') ||
      err.message.includes('TooManyRequests') ||
      err.message.includes('inference limit')
    );
    
    if (isQuotaError) {
      ElNotification({
        title: '⚠️ API 配额已达限制',
        message: '豆包 AI 图片识别服务暂时不可用。\n\n' + 
                 '原因：您的 AI 账户已达到推理次数限制\n' +
                 '解决方案：请登录豆包 AI 控制台调整模型配额\n' +
                 '或暂时使用手动输入功能记录食物。\n\n' +
                 '错误详情：' + err.message,
        type: 'warning',
        duration: 8000,
        offset: 80,
        dangerouslyUseHTMLString: false
      });
    } else {
      ElNotification({
        title: '❌ 识别失败',
        message: '图片识别失败：' + err.message,
        type: 'error',
        duration: 5000,
        offset: 80
      });
    }
  } finally {
    uploading.value = false;
    // 清空文件输入框，以便可以再次上传同一张图片
    event.target.value = '';
  }
};

// 工具函数
const getMealTypeName = (mealType) => {
  const mealTypes = {
    'BREAKFAST': '早餐',
    'LUNCH': '午餐',
    'DINNER': '晚餐',
    'SNACK': '加餐'
  }
  return mealTypes[mealType] || mealType
}

const getMealTypeClass = (mealType) => {
  const classes = {
    'BREAKFAST': 'breakfast',
    'LUNCH': 'lunch',
    'DINNER': 'dinner',
    'SNACK': 'snack'
  }
  return classes[mealType] || ''
}

const getFoodTypeClass = (food) => {
  // 根据食物名称判断类型
  const proteinKeywords = ['鸡', '牛', '猪', '鱼', '蛋', '豆腐', '虾', '肉']
  const carbKeywords = ['米', '面', 'bread', '馒头', '燕麦', '红薯', '玉米', '土豆']
  const fiberKeywords = ['菜', '西兰花', '菠菜', '胡萝卜', '芹菜', '黄瓜', '西红柿']

  const name = food.name
  if (proteinKeywords.some(keyword => name.includes(keyword))) return 'protein'
  if (carbKeywords.some(keyword => name.includes(keyword))) return 'carbs'
  if (fiberKeywords.some(keyword => name.includes(keyword))) return 'fiber'
  return 'other'
}



const formatTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 全选/取消全选
const toggleSelectAll = () => {
  if (selectAll.value) {
    // 全选：添加所有记录的ID
    selectedRecords.value = diaryEntries.value
      .filter(entry => entry.id)
      .map(entry => entry.id)
  } else {
    // 取消全选
    selectedRecords.value = []
  }
}

// 更新全选状态
const updateSelectAllState = () => {
  const allIds = diaryEntries.value
    .filter(entry => entry.id)
    .map(entry => entry.id)
  selectAll.value = allIds.length > 0 && 
    allIds.every(id => selectedRecords.value.includes(id))
}

// 批量删除
const batchDelete = async () => {
  if (selectedRecords.value.length === 0) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请先选择要删除的记录',
      type: 'warning',
      duration: 2000,
      offset: 80
    })
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRecords.value.length} 条记录吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    batchDeleting.value = true
    
    const apiUrl = API_BASE_URL.startsWith('/api') 
      ? window.location.origin + API_BASE_URL 
      : API_BASE_URL;
    
    const response = await fetch(`${apiUrl}/diet/batch`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(selectedRecords.value)
    })
    
    if (response.ok) {
      const result = await response.json()
      console.log('批量删除成功:', result)
      
      // 清空选中项
      selectedRecords.value = []
      selectAll.value = false
      
      // 重新加载最新数据，而不是直接过滤本地数组
      await loadTodayRecords()
      
      ElNotification({
        title: '✅ 删除成功',
        message: `成功删除 ${result.deletedCount} 条记录`,
        type: 'success',
        duration: 3000,
        offset: 80
      })
    } else {
      const errorResult = await response.json()
      throw new Error(errorResult.error || '批量删除失败')
    }
  } catch (err) {
    console.error('批量删除失败:', err)
    ElNotification({
      title: '❌ 删除失败',
      message: '批量删除失败：' + err.message,
      type: 'error',
      duration: 3000,
      offset: 80
    })
  } finally {
    batchDeleting.value = false
  }
}



// 处理日历日期选择
// 删除饮食记录
const deleteRecord = async (recordId, index) => {
  if (!recordId) {
    ElNotification({
      title: '⚠️ 提示',
      message: '记录 ID 无效，无法删除',
      type: 'warning',
      duration: 2000,
      offset: 80
    })
    return
  }
  
  // 确认删除
  try {
    await ElMessageBox.confirm(
      '确定要删除这条饮食记录吗？此操作不可撤销。',
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    userStore.setLoading(true)
    userStore.clearError()
      
    const apiUrl = API_BASE_URL.startsWith('/api') 
      ? window.location.origin + API_BASE_URL 
      : API_BASE_URL;
      
    // 调用后端删除 API
    const response = await fetch(`${apiUrl}/diet/${recordId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      }
    })
    
    if (response.ok) {
      const result = await response.json()
      console.log('删除记录成功:', result)
      
      // 重新加载最新数据，而不是直接操作数组
      await loadTodayRecords()
      
      ElNotification({
        title: '✅ 删除成功',
        message: '记录删除成功！',
        type: 'success',
        duration: 3000,
        offset: 80
      })
    } else {
      const errorResult = await response.json()
      throw new Error(errorResult.error || '删除失败')
    }
  } catch (err) {
    console.error('删除记录失败:', err)
    userStore.setError(err.message || '删除记录失败')
    ElNotification({
      title: '❌ 删除失败',
      message: '删除失败：' + err.message,
      type: 'error',
      duration: 3000,
      offset: 80
    })
  } finally {
    userStore.setLoading(false)
  }
}

// 组件挂载时加载数据
onMounted(async () => {
  
  // 初始化 store
  userStore.initializeStore()
  
  // 如果已认证，加载记录
  if (userStore.isAuthenticated) {
    await loadTodayRecords()
  } else {
    // 尝试从 localStorage 恢复用户状态
    const storedUserData = localStorage.getItem('userProfile')
    if (storedUserData) {
      try {
        const userData = JSON.parse(storedUserData)
        console.log('从 localStorage 恢复用户数据:', userData)
        if (userData.userId) {
          userStore.setUserData(userData)
          await loadTodayRecords()
        }
      } catch (e) {
        console.error('恢复用户数据失败:', e)
      }
    }
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
}

.card.history-card::-webkit-scrollbar {
  width: 6px;
}

.card.history-card::-webkit-scrollbar-track {
  background: transparent;
}

.card.history-card::-webkit-scrollbar-thumb {
  background: #d1d1d6;
  border-radius: 3px;
}

.card.history-card::-webkit-scrollbar-thumb:hover {
  background: #a1a1a6;
}

/* 加载动画 - 居中显示 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  border-radius: 20px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.loading-spinner {
  position: relative;
  width: 80px;
  height: 80px;
  /* 确保容器本身不旋转 */
  animation: none !important;
}

.spinner-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 4px solid rgba(0, 122, 255, 0.1);
  border-top-color: #007aff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  /* 确保只有圆环在旋转 */
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.spinner-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  z-index: 1; /* 确保图标在圆环上方 */
  /* 绝对静止，没有任何动画 */
  animation: none !important;
  transform-origin: center center;
}

.loading-text {
  color: #1d1d1f;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: -0.2px;
  text-align: center;
  animation: slideUp 0.4s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 记录卡片需要相对定位来容纳加载遮罩 */
.card.record-card {
  position: relative;
}

.diary-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
  background: #ffffff;
  min-height: 100vh;
  position: relative;
}

.diary-layout::before {
  content: '';
  position: fixed;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: 
    radial-gradient(ellipse at 30% 20%, rgba(0, 122, 255, 0.03) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 80%, rgba(88, 86, 214, 0.03) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
  animation: gradientShift 20s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-5%, -5%); }
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
  letter-spacing: -0.5px;
  font-size: 36px;
  line-height: 1.1;
  background: linear-gradient(135deg, #1d1d1f 0%, #424245 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin: 0 32px;
  position: relative;
  z-index: 1;
  align-items: stretch;
}

.card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 0;
  border: none;
  transition: all 0.5s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.04),
    0 24px 64px rgba(0, 0, 0, 0.08);
}

.card.history-card {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.card h2 {
  color: #1d1d1f;
  margin-bottom: 24px;
  padding-bottom: 0;
  border-bottom: none;
  font-weight: 600;
  letter-spacing: -0.3px;
  font-size: 24px;
}

.food-input-section {
  margin-bottom: 24px;
}

.input-row {
  display: flex;
  gap: 16px;
  align-items: end;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.food-name-input, .quantity-input {
  flex: 1;
  min-width: 150px;
}

.food-name-input label, .quantity-input label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #1d1d1f;
  font-size: 15px;
  letter-spacing: -0.1px;
}

.food-input, .quantity-number {
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

.food-input:focus, .quantity-number:focus {
  outline: none;
  background: #ebebf0;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}

.food-input::placeholder, .quantity-number::placeholder {
  color: #86868b;
}

.quantity-combo {
  display: flex;
  gap: 8px;
}

.unit-select {
  padding: 14px 16px;
  border: none;
  border-radius: 12px;
  font-size: 17px;
  background: #f5f5f7;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  color: #1d1d1f;
  cursor: pointer;
  min-width: 80px;
}

.unit-select:focus {
  outline: none;
  background: #ebebf0;
}

.unit-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #86868b;
  letter-spacing: -0.1px;
  line-height: 1.5;
}

.add-food-btn {
  width: 48px;
  height: 48px;
  background: #007aff;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-food-btn:hover {
  background: #0066d6;
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.3);
}

.add-food-btn:active {
  transform: scale(0.95);
}

.added-foods {
  margin-top: 24px;
  padding: 20px;
  background: #f5f5f7;
  border-radius: 16px;
}

.added-foods h3 {
  margin: 0 0 16px 0;
  color: #1d1d1f;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.food-items-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.food-item-tag {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  border: none;
  border-radius: 980px;
  padding: 8px 16px;
  font-size: 15px;
  color: #1d1d1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.food-name {
  font-weight: 600;
  letter-spacing: -0.1px;
}

.food-quantity {
  color: #86868b;
  font-size: 13px;
}

.remove-btn {
  width: 22px;
  height: 22px;
  background: #ff3b30;
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-btn:hover {
  background: #e5342d;
  transform: scale(1.1);
}

.meal-selection {
  margin-bottom: 24px;
}

.meal-selection label {
  display: block;
  margin-bottom: 12px;
  font-weight: 600;
  color: #1d1d1f;
  font-size: 15px;
  letter-spacing: -0.1px;
}

.meal-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.meal-btn {
  padding: 12px 20px;
  border: none;
  background: #f5f5f7;
  border-radius: 980px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-size: 15px;
  font-weight: 500;
  color: #1d1d1f;
  letter-spacing: -0.1px;
}

.meal-btn:hover {
  background: #ebebf0;
}

.meal-btn.active {
  background: #007aff;
  color: white;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.3);
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.btn {
  padding: 14px 24px;
  border: none;
  border-radius: 980px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: -0.1px;
}

.analyze-btn {
  background: #007aff;
  color: white;
}

.analyze-btn:hover:not(:disabled) {
  background: #0066d6;
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.3);
}

.upload-btn {
  background: #34c759;
  color: white;
}

.upload-btn:hover:not(:disabled) {
  background: #2db84e;
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(52, 199, 89, 0.3);
}

.save-btn {
  background: #1d1d1f;
  color: white;
}

.save-btn:hover:not(:disabled) {
  background: #333336;
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.btn:disabled {
  background: #d1d1d6;
  color: #86868b;
  cursor: not-allowed;
  transform: none;
}

.analysis-results {
  margin-bottom: 24px;
  padding: 24px;
  background: #f5f5f7;
  border-radius: 16px;
}

.analysis-results h3 {
  color: #1d1d1f;
  margin-bottom: 16px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.foods-display {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.food-tag {
  padding: 8px 16px;
  border-radius: 980px;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.1px;
}

.food-tag.protein { background: rgba(0, 122, 255, 0.1); color: #007aff; }
.food-tag.carbs { background: rgba(255, 149, 0, 0.1); color: #ff9500; }
.food-tag.fiber { background: rgba(52, 199, 89, 0.1); color: #34c759; }
.food-tag.other { background: #f5f5f7; color: #86868b; }

.nutrition-preview {
  padding: 24px;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border-radius: 20px;
  color: white;
  margin-bottom: 24px;
}

.nutrition-preview h3 {
  margin-bottom: 20px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.nutrient-item {
  background: rgba(255, 255, 255, 0.15);
  padding: 16px;
  border-radius: 12px;
  text-align: center;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.nutrient-item .value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
}

.nutrient-item .unit {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 4px;
}

.nutrient-item .label {
  font-size: 13px;
  opacity: 0.9;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 0;
  border-bottom: none;
  flex-shrink: 0;
}

.history-header h2 {
  margin-bottom: 0;
}

.summary-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-item {
  background: #f5f5f7;
  color: #1d1d1f;
  padding: 8px 16px;
  border-radius: 980px;
  font-weight: 500;
  font-size: 13px;
  letter-spacing: -0.1px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 80px 40px;
  background: #f5f5f7;
  border-radius: 20px;
  margin: 32px auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 3px solid #f5f5f7;
  border-top: 3px solid #007aff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 24px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.5;
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-state h3 {
  color: #1d1d1f;
  margin-bottom: 12px;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.empty-state p {
  color: #86868b;
  margin: 0;
  font-size: 17px;
  letter-spacing: -0.2px;
}

.tip {
  font-style: normal;
  color: #007aff !important;
  margin-top: 8px !important;
}

.records-container {
  flex: 1;
  overflow-y: auto;
  max-height: 500px;
}

.records-list-wrapper {
  flex: 1;
  overflow-y: auto;
  max-height: 500px;
  padding-right: 8px;
}

.records-list-wrapper::-webkit-scrollbar {
  width: 6px;
}

.records-list-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.records-list-wrapper::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

.records-list-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

.batch-actions-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #f5f5f7;
  border-radius: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.select-all-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 500;
  color: #1d1d1f;
  font-size: 15px;
  letter-spacing: -0.1px;
}

.select-all-label input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #007aff;
}

.selected-count {
  color: #007aff;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: -0.1px;
}

.btn-batch-delete {
  margin-left: auto;
  padding: 10px 20px;
  background: #ff3b30;
  color: white;
  border: none;
  border-radius: 980px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  letter-spacing: -0.1px;
}

.btn-batch-delete:hover:not(:disabled) {
  background: #e5342d;
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgba(255, 59, 48, 0.3);
}

.btn-batch-delete:disabled {
  background: #d1d1d6;
  cursor: not-allowed;
}

.record-item {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 12px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.record-item:hover {
  border-color: rgba(0, 122, 255, 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.record-item.selected {
  background: rgba(0, 122, 255, 0.05);
  border-color: #007aff;
}

.record-checkbox {
  width: 20px;
  height: 20px;
  cursor: pointer;
  margin-right: 12px;
  accent-color: #007aff;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.meal-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meal-badge {
  padding: 6px 12px;
  border-radius: 980px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: -0.1px;
}

.meal-badge.breakfast { background: rgba(255, 149, 0, 0.1); color: #ff9500; }
.meal-badge.lunch { background: rgba(0, 122, 255, 0.1); color: #007aff; }
.meal-badge.dinner { background: rgba(175, 82, 222, 0.1); color: #af52de; }
.meal-badge.snack { background: rgba(52, 199, 89, 0.1); color: #34c759; }

.time-stamp {
  color: #86868b;
  font-size: 13px;
  letter-spacing: -0.1px;
}

.record-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.calories-badge {
  font-size: 20px;
  font-weight: 700;
  color: #ff9500;
  letter-spacing: -0.5px;
}

.unit {
  font-size: 13px;
  color: #86868b;
  margin-left: 2px;
}

.delete-btn {
  background: #f5f5f7;
  color: #86868b;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  background: #ff3b30;
  color: white;
}

.record-body .food-desc {
  color: #1d1d1f;
  line-height: 1.6;
  margin-bottom: 12px;
  font-size: 15px;
  letter-spacing: -0.1px;
}

.nutrition-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.chip {
  padding: 6px 12px;
  border-radius: 980px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: -0.1px;
}

.chip.protein { background: rgba(0, 122, 255, 0.1); color: #007aff; }
.chip.carbs { background: rgba(255, 149, 0, 0.1); color: #ff9500; }
.chip.fat { background: rgba(255, 59, 48, 0.1); color: #ff3b30; }

.ingredients-section {
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  padding-top: 12px;
}

.ingredients-section strong {
  display: block;
  margin-bottom: 10px;
  color: #1d1d1f;
  font-size: 14px;
  font-weight: 600;
}

.ingredient-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ingredient-tag {
  background: #f5f5f7;
  padding: 6px 12px;
  border-radius: 980px;
  font-size: 12px;
  color: #424245;
}

.summary-section {
  margin-top: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border-radius: 20px;
  color: white;
  flex-shrink: 0;
}

.bottom-summary-section {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.summary-card {
  width: 100%;
  max-width: 1200px;
}

.summary-section h3 {
  text-align: center;
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.ai-analysis-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-ai-analyze {
  width: 100%;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  border-radius: 980px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  letter-spacing: -0.1px;
}

.btn-ai-analyze:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.01);
}

.btn-ai-analyze:disabled {
  background: rgba(255, 255, 255, 0.1);
  cursor: not-allowed;
}

.analysis-result {
  margin-top: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  color: #1d1d1f;
  text-align: left;
}

.analysis-result h4 {
  margin: 0 0 16px 0;
  color: #007aff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.analysis-content {
  line-height: 1.6;
  font-size: 15px;
  letter-spacing: -0.1px;
}

.analysis-content strong {
  color: #007aff;
}

.analysis-content .emoji {
  font-size: 1rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.summary-box {
  background: rgba(255, 255, 255, 0.15);
  padding: 16px;
  border-radius: 12px;
  text-align: center;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.big-number {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
}

.metric {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 2px;
}

.small-unit {
  font-size: 12px;
  opacity: 0.8;
}

.ai-analyzing {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.analyzing-content {
  background: #ffffff;
  padding: 32px 48px;
  border-radius: 20px;
  text-align: center;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.2);
}

.analyzing-content p {
  margin: 0;
  font-size: 17px;
  color: #1d1d1f;
  margin-top: 16px;
  letter-spacing: -0.2px;
}

.analyzing-tip {
  font-size: 14px;
  color: #86868b;
  margin-top: 12px !important;
  font-weight: normal;
}

@media (max-width: 1024px) {
  .main-container {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 60px 20px 40px;
  }
  
  .page-header h1 {
    font-size: 32px;
  }
  
  .stats-bar {
    flex-direction: column;
    gap: 8px;
  }
  
  .calendar-section {
    margin: 0 20px 20px;
    padding: 20px;
    border-radius: 20px;
  }
  
  .main-container {
    grid-template-columns: 1fr;
    gap: 20px;
    margin: 0 20px;
  }
  
  .card {
    padding: 20px;
    border-radius: 20px;
  }
  
  .input-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .nutrition-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
  
  .history-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .summary-stats {
    justify-content: center;
  }
  
  .batch-actions-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .btn-batch-delete {
    margin-left: 0;
  }
  
  .record-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .record-actions {
    align-self: flex-end;
  }
}

@media (max-width: 480px) {
  .page-header h1 {
    font-size: 28px;
  }
  
  .card {
    padding: 16px;
  }
  
  .card h2 {
    font-size: 20px;
  }
  
  .meal-buttons {
    justify-content: center;
  }
  
  .meal-btn {
    padding: 10px 16px;
    font-size: 14px;
  }
}
</style>