<template>
  <div class="diary-layout">
    <!-- 页面头部：设计系统 PageHeader -->
    <PageHeader
      title="饮食日记"
      section-label="饮食记录"
      :label-pulse="true"
      icon="🍽️"
    >
      <template #stats>
        <span class="stat-item">📊 今日记录: {{ diaryEntries.length }} 条</span>
        <span class="stat-item">🔥 总热量: {{ totalNutrition.calories }}kcal</span>
      </template>
    </PageHeader>
    
    <div class="main-container">
      <!-- 左侧：记录区域 -->
      <div class="left-panel">
        <div class="card record-card bh-card">
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
          
          <div class="card-header-section">
            <SectionLabel label="新建记录" />
            <h2>➕ 添加新记录</h2>
          </div>
          
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
              🤖 {{ analyzing ? '智能分析中...' : '智能分析' }}
            </button>
            <button 
              class="btn upload-btn" 
              @click="triggerImageUpload"
              :disabled="uploading"
            >
              📸 {{ uploading ? '智能分析中...' : '拍照上传' }}
            </button>
            <input 
              type="file" 
              ref="imageUploader" 
              @change="handleImageUpload" 
              accept="image/*" 
              style="display: none;" 
            />
            <button
              class="btn-brand"
              @click="recordDiet"
              :disabled="recordDisabled"
              :aria-disabled="recordDisabled"
              title="请先完成智能分析后再记录"
            >
              💾 记录
            </button>
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
        <div class="card history-card bh-card">
          <div class="history-header">
            <div class="card-header-section">
              <SectionLabel label="今日记录" />
              <h2>📖 今日饮食记录</h2>
            </div>
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
              <div class="calorie-ring-wrap" aria-hidden="true">
                <svg class="calorie-ring" viewBox="0 0 100 100">
                  <circle class="calorie-ring__track" cx="50" cy="50" r="42" />
                  <circle
                    class="calorie-ring__fill"
                    :class="calorieRingStatusClass"
                    cx="50"
                    cy="50"
                    r="42"
                    :style="calorieRingStyle"
                  />
                </svg>
                <div class="calorie-ring__center">
                  <div class="big-number">{{ totalNutrition.calories }}</div>
                  <div class="metric">总热量</div>
                  <div class="small-unit">kcal</div>
                </div>
              </div>
              <p class="calorie-target-label">
                目标 {{ Math.round(calorieTarget) }} kcal · {{ calorieProgressPct }}%
              </p>
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
              {{ analyzingNutrition ? '智能分析中...' : '🤖 AI 智能分析建议' }}
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
    
    <!-- 底部引导：跳转食谱或健身 -->
    <PageCta
      title="搭配运动，效果更佳"
      description="记录饮食后，可浏览健康食谱或前往健身页记录今日训练。"
      to="/recipes"
      button-text="浏览健康食谱"
    />

    <!-- AI 分析失败提示 -->
    <div v-if="analyzing" class="ai-analyzing">
      <div class="analyzing-content">
        <div class="spinner"></div>
        <p>智能分析中...</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import PageHeader from '@/components/common/PageHeader.vue'
import PageCta from '@/components/common/PageCta.vue'
import SectionLabel from '@/components/common/SectionLabel.vue'
import { healthApi } from '../api/healthApi'
import { useUserStore } from '../stores/userStore'
import { useAiPageJobsStore } from '../stores/aiPageJobsStore'
import { aiPageJobRunner } from '../services/aiPageJobRunner'
import { AI_PAGE_JOB_IDS } from '../constants/aiPageJobIds'
import ManualNutritionInput from '../components/ManualNutritionInput.vue'
import { ElNotification, ElMessageBox } from 'element-plus'
import { savePageDraft, loadPageDraft, clearPageDraft } from '../utils/pageDraftStorage'

// 饮食日记未保存表单草稿键（切路由后恢复）
const DIARY_DRAFT_KEY = 'diary_form_draft'

// 总热量圆环：周长与默认目标（档案未加载时的回退值）
const CALORIE_RING_CIRC = 264
const DEFAULT_TARGET_CALORIES = 2000

// 使用环境变量配置 API 基础 URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const userStore = useUserStore()
const aiPageJobs = useAiPageJobsStore()

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
const analyzing = computed(() => aiPageJobs.isRunning(AI_PAGE_JOB_IDS.DIARY_SMART_ANALYZE))
const recognizedFoods = ref([])
const nutritionEstimate = ref(null)
const showManualInput = ref(false)
const currentManualFood = ref(null)
const analyzingNutrition = computed(() =>
  aiPageJobs.isRunning(AI_PAGE_JOB_IDS.DIARY_NUTRITION_ANALYZE)
)
const nutritionAnalysis = ref('')
const imageUploader = ref(null)
const uploading = computed(() => aiPageJobs.isRunning(AI_PAGE_JOB_IDS.DIARY_IMAGE_RECOGNIZE))
const hasAnalyzed = ref(false) // 是否已经过 AI 分析

// 未完成智能分析前禁用「记录」按钮
const recordDisabled = computed(
  () => !hasAnalyzed.value || foodItems.value.length === 0
)

// 批量删除相关
const selectedRecords = ref([])
const selectAll = ref(false)
const batchDeleting = ref(false)

// 用户档案中的每日目标热量（kcal）
const targetCalories = ref(null)

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

// 有效目标热量：优先档案值，无效时回退默认值
const calorieTarget = computed(() => {
  const target = Number(targetCalories.value)
  return target > 0 ? target : DEFAULT_TARGET_CALORIES
})

// 摄入占目标的百分比（可超过 100%）
const calorieProgressPct = computed(() => {
  const intake = totalNutrition.value.calories || 0
  if (!calorieTarget.value) return 0
  return Math.round((intake / calorieTarget.value) * 100)
})

// 圆环填充比例（SVG 最多画满一圈）
const calorieRingFillPct = computed(() =>
  Math.min(Math.max(calorieProgressPct.value, 0), 100)
)

// 圆环 stroke-dashoffset：随摄入/目标比例更新弧长
const calorieRingStyle = computed(() => ({
  strokeDasharray: `${CALORIE_RING_CIRC}`,
  strokeDashoffset: `${CALORIE_RING_CIRC * (1 - calorieRingFillPct.value / 100)}`
}))

// 圆环进度色：未达标黄、达标蓝、超标红
const calorieRingStatusClass = computed(() => {
  const ratio = (totalNutrition.value.calories || 0) / calorieTarget.value
  if (ratio >= 1) return ratio > 1.15 ? 'is-over' : 'is-met'
  return 'is-under'
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

// 将拍照识图结果写入食物列表（供上传回调与切路由恢复共用）
const applyImageRecognizeResult = (result) => {
  if (!result?.foods?.length) return
  foodItems.value = result.foods.map((food) => ({
    name: food.name,
    quantity: food.weightGrams,
    unit: '克',
  }))
  saveDiaryDraft()
}

/** 持久化当前表单草稿到 sessionStorage */
const saveDiaryDraft = () => {
  if (foodItems.value.length === 0 && !hasAnalyzed.value && !nutritionAnalysis.value) {
    clearPageDraft(DIARY_DRAFT_KEY)
    return
  }
  savePageDraft(DIARY_DRAFT_KEY, {
    foodItems: foodItems.value,
    newDiaryEntry: newDiaryEntry.value,
    recognizedFoods: recognizedFoods.value,
    nutritionEstimate: nutritionEstimate.value,
    hasAnalyzed: hasAnalyzed.value,
    currentFoodItem: currentFoodItem.value,
    nutritionAnalysis: nutritionAnalysis.value,
  })
}

/** 从 sessionStorage 恢复未保存的表单 */
const restoreDiaryDraft = () => {
  const draft = loadPageDraft(DIARY_DRAFT_KEY)
  if (!draft) return
  if (Array.isArray(draft.foodItems) && draft.foodItems.length > 0) {
    foodItems.value = draft.foodItems
  }
  if (draft.newDiaryEntry) {
    newDiaryEntry.value = { ...newDiaryEntry.value, ...draft.newDiaryEntry }
  }
  if (Array.isArray(draft.recognizedFoods)) {
    recognizedFoods.value = draft.recognizedFoods
  }
  if (draft.nutritionEstimate) {
    nutritionEstimate.value = draft.nutritionEstimate
  }
  if (typeof draft.hasAnalyzed === 'boolean') {
    hasAnalyzed.value = draft.hasAnalyzed
  }
  if (draft.currentFoodItem) {
    currentFoodItem.value = { ...currentFoodItem.value, ...draft.currentFoodItem }
  }
  if (draft.nutritionAnalysis) {
    nutritionAnalysis.value = draft.nutritionAnalysis
  }
}

// 表单变更时自动保存草稿
watch(
  [foodItems, newDiaryEntry, recognizedFoods, nutritionEstimate, hasAnalyzed, nutritionAnalysis],
  () => saveDiaryDraft(),
  { deep: true }
)

// 将智能分析结果应用到表单
const applySmartAnalyzeResult = (result) => {
  if (!result) return
  recognizedFoods.value = result.foods || []
  newDiaryEntry.value.consumedIngredients = (result.foods || []).map((food) => food.name)
  nutritionEstimate.value = result.totalNutrition
  newDiaryEntry.value.calories = result.totalNutrition?.calories ?? null
  newDiaryEntry.value.protein = result.totalNutrition?.protein ?? null
  newDiaryEntry.value.carbs = result.totalNutrition?.carbs ?? null
  newDiaryEntry.value.fat = result.totalNutrition?.fat ?? null
  newDiaryEntry.value.fiber = result.totalNutrition?.fiber ?? null
  hasAnalyzed.value = true
}

// 智能分析食物 - 全局 Runner，切路由不中断
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

  const foodDescription = foodItems.value
    .map((item) => `${item.quantity}${item.unit}${item.name}`)
    .join('、')

  try {
    const result = await aiPageJobRunner.runDiarySmartAnalyze(foodDescription)
    if (!result) return
    applySmartAnalyzeResult(result)
    if (result.mode === '数据库匹配') {
      ElNotification({
        title: '✅ 数据库匹配成功',
        message: `已从数据库匹配 ${result.databaseMatchCount} 种食材`,
        type: 'success',
        duration: 2000,
        offset: 80
      })
    } else {
      ElNotification({
        title: '🤖 AI 分析完成',
        message: `数据库匹配 ${result.databaseMatchCount} 种 + AI 分析 ${result.aiAnalysisCount} 种`,
        type: 'success',
        duration: 2000,
        offset: 80
      })
    }
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
  }
}

const recordDiet = async () => {
  if (recordDisabled.value) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请先点击「智能分析」完成营养评估后再记录',
      type: 'warning',
      duration: 2500,
      offset: 80
    })
    return
  }

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
  clearPageDraft(DIARY_DRAFT_KEY)
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

    const data = await aiPageJobRunner.runDiaryNutritionAnalyze(userId, nutritionData)
    if (!data) return
    if (data.success) {
      nutritionAnalysis.value = data.analysis
    } else {
      ElNotification({
        title: '❌ 分析失败',
        message: data.error,
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

  const formData = new FormData()
  formData.append('image', file)
  if (userStore.userData?.userId) {
    formData.append('userId', userStore.userData.userId);
  }

  try {
    const result = await aiPageJobRunner.runDiaryImageRecognize(formData)
    if (!result) return
    if (result.foods && result.foods.length > 0) {
      const mockItems = result.foods.filter((f) => String(f.name || '').startsWith('模拟-'));
      if (mockItems.length > 0) {
        ElNotification({
          title: '⚠️ 未使用真实识别',
          message: '返回了演示数据，请在「AI 设置」配置视觉 API Key 与模型后重试。',
          type: 'warning',
          duration: 6000,
          offset: 80,
        });
        return;
      }
      applyImageRecognizeResult(result);
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

    const status = err.response?.status;
    const errMsg = err.response?.data?.error || err.response?.data?.message || err.message;

    if (status === 429 || errMsg?.includes('试用额度') || errMsg?.includes('平台试用')) {
      ElNotification({
        title: '平台试用已用尽',
        message: errMsg || '请在右上角「AI 设置」填写您自己的拍照 API Key，或查看「使用手册」。',
        type: 'warning',
        duration: 10000,
        offset: 80,
      });
      return;
    }

    if (status === 428 || errMsg?.includes('AI 设置') || errMsg?.includes('拍照识食')) {
      ElNotification({
        title: '需要配置拍照识食',
        message: errMsg || '请在右上角「AI 设置」选择视觉服务商并填写 Key，详见「使用手册」。',
        type: 'warning',
        duration: 8000,
        offset: 80,
      });
      return;
    }

    const isQuotaError = errMsg && (
      errMsg.includes('配额限制') ||
      errMsg.includes('429') ||
      errMsg.includes('TooManyRequests') ||
      errMsg.includes('inference limit')
    );
    
    if (isQuotaError) {
      ElNotification({
        title: '⚠️ API 配额已达限制',
        message: '智能分析服务暂时不可用。\n\n' +
                 '原因：您的 AI 账户已达到推理次数限制\n' +
                 '解决方案：请在 AI 服务商控制台调整模型配额，\n' +
                 '或暂时使用手动输入功能记录食物。\n\n' +
                 '错误详情：' + errMsg,
        type: 'warning',
        duration: 8000,
        offset: 80,
        dangerouslyUseHTMLString: false
      });
    } else {
      ElNotification({
        title: '❌ 识别失败',
        message: '图片识别失败：' + errMsg,
        type: 'error',
        duration: 5000,
        offset: 80
      });
    }
  } finally {
    event.target.value = '';
  }
};

// 从全局任务 store 恢复切路由前的分析结果
const restoreAiPageJobs = () => {
  const img = aiPageJobs.jobs[AI_PAGE_JOB_IDS.DIARY_IMAGE_RECOGNIZE]
  if (img.status === 'success' && img.result?.foods?.length && foodItems.value.length === 0) {
    applyImageRecognizeResult(img.result)
  }
  const smart = aiPageJobs.jobs[AI_PAGE_JOB_IDS.DIARY_SMART_ANALYZE]
  if (smart.status === 'success' && smart.result) {
    applySmartAnalyzeResult(smart.result)
  }
  const nut = aiPageJobs.jobs[AI_PAGE_JOB_IDS.DIARY_NUTRITION_ANALYZE]
  if (nut.status === 'success' && nut.result?.success) {
    nutritionAnalysis.value = nut.result.analysis
  }
}

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

// 从用户档案加载每日目标热量
const loadTargetCalories = async () => {
  const userId = userStore.userData?.userId
  if (!userId) return
  try {
    const profile = await healthApi.getUserProfile(userId)
    const target = Number(profile?.targetCalories)
    targetCalories.value = target > 0 ? target : null
  } catch (err) {
    console.warn('加载目标热量失败，将使用默认值', err)
  }
}

// 组件挂载时加载数据
onMounted(async () => {
  
  // 初始化 store
  userStore.initializeStore()
  
  // 如果已认证，加载记录
  if (userStore.isAuthenticated) {
    await Promise.all([loadTodayRecords(), loadTargetCalories()])
    restoreDiaryDraft()
    restoreAiPageJobs()
  } else {
    // 尝试从 localStorage 恢复用户状态
    const storedUserData = localStorage.getItem('userProfile')
    if (storedUserData) {
      try {
        const userData = JSON.parse(storedUserData)
        console.log('从 localStorage 恢复用户数据:', userData)
        if (userData.userId) {
          userStore.setUserData(userData)
          await Promise.all([loadTodayRecords(), loadTargetCalories()])
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
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  border-radius: 0;
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
  border-top-color: var(--color-foreground);
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
  color: var(--color-foreground);
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




.page-header {
  text-align: center;
  padding: 48px 24px 36px;
  position: relative;
  z-index: 1;
  background: var(--color-background);
}

.page-header h1 {
  margin-bottom: 12px;
  font-weight: 700;
  letter-spacing: -0.5px;
  font-size: 36px;
  line-height: 1.1;
}

.stats-bar {
  display: flex;
  justify-content: center;
  gap: 10px;
  align-items: center;
  font-size: 15px;
  color: var(--color-muted-foreground);
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
  border-radius: 0;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: none;
  -webkit-transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: none;
  transform: translateY(-2px);
}


.main-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  /* 顶栏间距由 design-system .page-shell-header margin-bottom 统一控制 */
  margin: 0 32px 0;
  position: relative;
  z-index: 1;
  align-items: stretch;
}

.card {
  background: #ffffff;
  border-radius: 0;
  box-shadow: none;
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
  box-shadow: none;
}

.card.history-card {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.card h2 {
  color: var(--color-foreground);
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
  color: var(--color-foreground);
  font-size: 15px;
  letter-spacing: -0.1px;
}

.food-input, .quantity-number {
  width: 100%;
  padding: 14px 16px;
  border: none;
  border-radius: 0;
  font-size: 17px;
  background: var(--color-muted);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  color: var(--color-foreground);
  letter-spacing: -0.2px;
}

.food-input:focus, .quantity-number:focus {
  outline: none;
  background: #ebebf0;
  box-shadow: none;
}

.food-input::placeholder, .quantity-number::placeholder {
  color: var(--color-muted-foreground);
}

.quantity-combo {
  display: flex;
  gap: 8px;
}

.unit-select {
  padding: 14px 16px;
  border: none;
  border-radius: 0;
  font-size: 17px;
  background: var(--color-muted);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  color: var(--color-foreground);
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
  color: var(--color-muted-foreground);
  letter-spacing: -0.1px;
  line-height: 1.5;
}

.add-food-btn {
  width: 48px;
  height: 48px;
  background: var(--color-foreground);
  color: white;
  border: none;
  border-radius: 0;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-food-btn:hover {
  background: var(--color-foreground);
  transform: scale(1.05);
  box-shadow: none;
}

.add-food-btn:active {
  transform: scale(0.95);
}

.added-foods {
  margin-top: 24px;
  padding: 20px;
  background: var(--color-muted);
  border-radius: 0;
}

.added-foods h3 {
  margin: 0 0 16px 0;
  color: var(--color-foreground);
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
  border-radius: 0;
  padding: 8px 16px;
  font-size: 15px;
  color: var(--color-foreground);
  box-shadow: none;
}

.food-name {
  font-weight: 600;
  letter-spacing: -0.1px;
}

.food-quantity {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.remove-btn {
  width: 22px;
  height: 22px;
  background: #000000;
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
  color: var(--color-foreground);
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
  background: var(--color-muted);
  border-radius: 0;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-size: 15px;
  font-weight: 500;
  color: var(--color-foreground);
  letter-spacing: -0.1px;
}

.meal-btn:hover {
  background: #ebebf0;
}

.meal-btn.active {
  background: var(--color-foreground);
  color: white;
  box-shadow: none;
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
  border-radius: 0;
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
  background: var(--color-foreground);
  color: white;
}

.analyze-btn:hover:not(:disabled) {
  background: var(--color-foreground);
  transform: scale(1.02);
  box-shadow: none;
}

.upload-btn {
  background: #000000;
  color: white;
}

.upload-btn:hover:not(:disabled) {
  background: #2db84e;
  transform: scale(1.02);
  box-shadow: none;
}

.save-btn {
  background: var(--color-foreground);
  color: white;
}

.save-btn:hover:not(:disabled) {
  background: #333336;
  transform: scale(1.02);
  box-shadow: none;
}

.btn:disabled {
  background: #d1d1d6;
  color: var(--color-muted-foreground);
  cursor: not-allowed;
  transform: none;
}

.analysis-results {
  margin-bottom: 24px;
  padding: 24px;
  background: var(--color-muted);
  border-radius: 0;
}

.analysis-results h3 {
  color: var(--color-foreground);
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
  border-radius: 0;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.1px;
}

.food-tag.protein { background: rgba(0, 122, 255, 0.1); color: var(--color-foreground); }
.food-tag.carbs { background: rgba(255, 149, 0, 0.1); color: #525252; }
.food-tag.fiber { background: rgba(52, 199, 89, 0.1); color: #000000; }
.food-tag.other { background: var(--color-muted); color: var(--color-muted-foreground); }

/* 智能成分评估：浅底黑字，符合 Bauhaus 对比度规范 */
.nutrition-preview {
  padding: 24px;
  background: var(--color-card);
  border: var(--border-width) solid var(--color-border);
  border-radius: 0;
  color: var(--color-foreground);
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
}

.nutrition-preview h3 {
  margin-bottom: 20px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: var(--color-foreground);
}

.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.nutrient-item {
  background: var(--color-muted);
  padding: 16px;
  border-radius: 0;
  text-align: center;
  border: var(--border-width) solid var(--color-border);
  color: var(--color-foreground);
}

.nutrient-item .value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
  color: var(--color-foreground);
}

.nutrient-item.calories .value { color: var(--color-calories); }
.nutrient-item.protein .value { color: var(--color-protein); }
.nutrient-item.carbs .value { color: var(--color-carbs); }
.nutrient-item.fat .value { color: var(--color-fat); }

.nutrient-item .unit {
  font-size: 13px;
  margin-bottom: 4px;
  color: var(--color-muted-foreground);
}

.nutrient-item .label {
  font-size: 13px;
  color: var(--color-muted-foreground);
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
  background: var(--color-muted);
  color: var(--color-foreground);
  padding: 8px 16px;
  border-radius: 0;
  font-weight: 500;
  font-size: 13px;
  letter-spacing: -0.1px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 80px 40px;
  background: var(--color-muted);
  border-radius: 0;
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
  border: 3px solid var(--color-muted);
  border-top: 3px solid var(--color-foreground);
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
  color: var(--color-foreground);
  margin-bottom: 12px;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.empty-state p {
  color: var(--color-muted-foreground);
  margin: 0;
  font-size: 17px;
  letter-spacing: -0.2px;
}

.tip {
  font-style: normal;
  color: var(--color-foreground) !important;
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
  background: var(--color-muted);
  border-radius: 0;
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
  color: var(--color-foreground);
  font-size: 15px;
  letter-spacing: -0.1px;
}

.select-all-label input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: var(--color-foreground);
}

.selected-count {
  color: var(--color-foreground);
  font-weight: 600;
  font-size: 14px;
  letter-spacing: -0.1px;
}

.btn-batch-delete {
  margin-left: auto;
  padding: 10px 20px;
  background: #000000;
  color: white;
  border: none;
  border-radius: 0;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  letter-spacing: -0.1px;
}

.btn-batch-delete:hover:not(:disabled) {
  background: #e5342d;
  transform: scale(1.02);
  box-shadow: none;
}

.btn-batch-delete:disabled {
  background: #d1d1d6;
  cursor: not-allowed;
}

.record-item {
  background: #ffffff;
  border-radius: 0;
  padding: 20px;
  margin-bottom: 12px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.record-item:hover {
  border-color: rgba(0, 122, 255, 0.2);
  box-shadow: none;
}

.record-item.selected {
  background: rgba(0, 122, 255, 0.05);
  border-color: var(--color-foreground);
}

.record-checkbox {
  width: 20px;
  height: 20px;
  cursor: pointer;
  margin-right: 12px;
  accent-color: var(--color-foreground);
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
  border-radius: 0;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: -0.1px;
}

.meal-badge.breakfast { background: rgba(255, 149, 0, 0.1); color: #525252; }
.meal-badge.lunch { background: rgba(0, 122, 255, 0.1); color: var(--color-foreground); }
.meal-badge.dinner { background: rgba(175, 82, 222, 0.1); color: #525252; }
.meal-badge.snack { background: rgba(52, 199, 89, 0.1); color: #000000; }

.time-stamp {
  color: var(--color-muted-foreground);
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
  color: #525252;
  letter-spacing: -0.5px;
}

.unit {
  font-size: 13px;
  color: var(--color-muted-foreground);
  margin-left: 2px;
}

.delete-btn {
  background: var(--color-muted);
  color: var(--color-muted-foreground);
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
  background: #000000;
  color: white;
}

.record-body .food-desc {
  color: var(--color-foreground);
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
  border-radius: 0;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: -0.1px;
}

.chip.protein { background: rgba(0, 122, 255, 0.1); color: var(--color-foreground); }
.chip.carbs { background: rgba(255, 149, 0, 0.1); color: #525252; }
.chip.fat { background: rgba(255, 59, 48, 0.1); color: #000000; }

.ingredients-section {
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  padding-top: 12px;
}

.ingredients-section strong {
  display: block;
  margin-bottom: 10px;
  color: var(--color-foreground);
  font-size: 14px;
  font-weight: 600;
}

.ingredient-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ingredient-tag {
  background: var(--color-muted);
  padding: 6px 12px;
  border-radius: 0;
  font-size: 12px;
  color: #424245;
}

/* 今日营养总计：浅底黑字，与「营养成分预估」卡片风格一致 */
.summary-section {
  margin-top: 24px;
  padding: 24px;
  background: var(--color-card);
  border: var(--border-width) solid var(--color-border);
  border-radius: 0;
  color: var(--color-foreground);
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.bottom-summary-section {
  margin: 20px 32px 0;
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
  color: var(--color-foreground);
}

.ai-analysis-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: var(--border-width) solid var(--color-border-light);
}

.btn-ai-analyze {
  width: 100%;
  padding: 14px 24px;
  background: var(--color-foreground);
  color: var(--color-inverted-fg);
  border: var(--border-width) solid var(--color-border);
  border-radius: 0;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  letter-spacing: -0.1px;
  box-shadow: var(--shadow-sm);
}

.btn-ai-analyze:hover:not(:disabled) {
  background: var(--color-primary-blue);
  transform: translateY(-2px);
}

.btn-ai-analyze:disabled {
  background: var(--color-muted);
  color: var(--color-muted-foreground);
  cursor: not-allowed;
  box-shadow: none;
}

.analysis-result {
  margin-top: 16px;
  padding: 20px;
  background: var(--color-muted);
  border: var(--border-width) solid var(--color-border-light);
  border-radius: 0;
  color: var(--color-foreground);
  text-align: left;
}

.analysis-result h4 {
  margin: 0 0 16px 0;
  color: var(--color-foreground);
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
  color: var(--color-foreground);
}

.analysis-content .emoji {
  font-size: 1rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

/* 营养四项网格：浅灰底 + 三原色数值 + 静音标签 */
.summary-box {
  background: var(--color-muted);
  padding: 16px;
  border-radius: 0;
  text-align: center;
  border: var(--border-width) solid var(--color-border);
  color: var(--color-foreground);
}

.summary-box.calories .big-number { color: var(--color-calories); }
.summary-box.protein .big-number { color: var(--color-protein); }
.summary-box.carbs .big-number { color: var(--color-carbs); }
.summary-box.fat .big-number { color: var(--color-fat); }

/* 总热量进度环：背景轨 + 按比例着色的进度轨 */
.calorie-ring-wrap {
  position: relative;
  width: 112px;
  height: 112px;
  margin: 0 auto 10px;
}

.calorie-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.calorie-ring__track {
  fill: none;
  stroke: var(--color-border-light);
  stroke-width: 6;
}

.calorie-ring__fill {
  fill: none;
  stroke-width: 6;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.45s ease, stroke 0.3s ease;
}

.calorie-ring__fill.is-under { stroke: var(--color-warning); }
.calorie-ring__fill.is-met { stroke: var(--color-success); }
.calorie-ring__fill.is-over { stroke: var(--color-danger); }

.calorie-ring__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.calorie-ring__center .big-number {
  font-size: 22px;
  margin-bottom: 2px;
}

.calorie-ring__center .metric,
.calorie-ring__center .small-unit {
  font-size: 11px;
  line-height: 1.2;
}

.calorie-target-label {
  margin: 0;
  font-size: 12px;
  color: var(--color-muted-foreground);
  letter-spacing: -0.1px;
}

.big-number {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
  color: var(--color-foreground);
}

.metric {
  font-size: 13px;
  color: var(--color-muted-foreground);
  margin-bottom: 2px;
}

.small-unit {
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.ai-analyzing {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.analyzing-content {
  background: #ffffff;
  padding: 32px 48px;
  border-radius: 0;
  text-align: center;
  box-shadow: none;
}

.analyzing-content p {
  margin: 0;
  font-size: 17px;
  color: var(--color-foreground);
  margin-top: 16px;
  letter-spacing: -0.2px;
}

.analyzing-tip {
  font-size: 14px;
  color: var(--color-muted-foreground);
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
    border-radius: 0;
  }
  
  .main-container {
    grid-template-columns: 1fr;
    gap: 20px;
    margin: 0 20px;
  }

  .bottom-summary-section {
    margin: 20px 20px 0;
  }
  
  .card {
    padding: 20px;
    border-radius: 0;
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