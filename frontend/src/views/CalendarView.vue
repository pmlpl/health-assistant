<template>
  <div class="calendar-page-layout">
    <div class="page-header">
      <h1>
        <svg class="header-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
          <line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" stroke-width="2"/>
          <line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" stroke-width="2"/>
          <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/>
        </svg>
        健康日历
      </h1>
      <div class="stats-bar">
        <span class="stat-item">查看每日饮食和健身记录</span>
      </div>
    </div>

    <div class="calendar-main-content">
      <!-- 合并的单一日历 -->
      <div class="unified-calendar-container">
        <div class="calendar-card unified-calendar">
          <div class="calendar-header">
            <h3>📅 健康日历</h3>
            <p class="calendar-subtitle">点击日期查看饮食和健身详细统计</p>
          </div>
          <ProfessionalCalendar 
            :user-id="userStore.userData.userId"
            type="combined"
            @date-selected="navigateToDailyDetail"
          />
        </div>
      </div>

      <!-- 选择日期时的显示 -->
      <div v-if="selectedDateData" class="daily-detail-section">
        <div class="detail-header">
          <h2>{{ formatDate(selectedDateData.date) }} 的详细记录</h2>
        </div>
        
        <!-- 统计图表区域 -->
        <div class="charts-section">
          <!-- 热量对比进度条 -->
          <div class="stat-card calories-comparison">
            <h3>📊 热量收支对比</h3>
            <div class="progress-container">
              <div class="progress-bar-wrapper">
                <div class="progress-label">摄入热量</div>
                <div class="progress-bar intake-bar">
                  <div class="progress-fill" :style="{ width: getIntakeProgress() + '%' }"></div>
                </div>
                <div class="progress-value">{{ Math.round(selectedDateData.dietTotals.calories) }} kcal</div>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-label">消耗热量</div>
                <div class="progress-bar burn-bar">
                  <div class="progress-fill" :style="{ width: getBurnProgress() + '%' }"></div>
                </div>
                <div class="progress-value">{{ Math.round(selectedDateData.fitnessTotals.caloriesBurned) }} kcal</div>
              </div>
            </div>
            <div class="net-balance" :class="getBalanceClass()">
              <span>净热量：</span>
              <strong>{{ getNetCalories() }} kcal</strong>
              <span class="balance-icon">{{ getBalanceIcon() }}</span>
            </div>
          </div>
          
          <!-- 营养构成环形图 -->
          <div class="stat-card nutrition-chart">
            <h3>🥗 营养摄入构成</h3>
            <div class="chart-container">
              <div class="donut-chart" :style="getDonutChartStyle()">
                <div class="chart-center">
                  <div class="total-calories">{{ Math.round(selectedDateData.dietTotals.calories) }}</div>
                  <div class="calories-label">总热量</div>
                </div>
              </div>
              <div class="chart-legend">
                <div class="legend-item protein">
                  <span class="legend-color"></span>
                  <span class="legend-label">蛋白质</span>
                  <span class="legend-value">{{ Math.round(selectedDateData.dietTotals.protein * 10) / 10 }}g</span>
                </div>
                <div class="legend-item carbs">
                  <span class="legend-color"></span>
                  <span class="legend-label">碳水化合物</span>
                  <span class="legend-value">{{ Math.round(selectedDateData.dietTotals.carbs * 10) / 10 }}g</span>
                </div>
                <div class="legend-item fat">
                  <span class="legend-color"></span>
                  <span class="legend-label">脂肪</span>
                  <span class="legend-value">{{ Math.round(selectedDateData.dietTotals.fat * 10) / 10 }}g</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="detail-content">
          <!-- 饮食记录 -->
          <div class="detail-card diet-section">
            <div class="card-header">
              <h3>🍽️ 饮食记录</h3>
              <div class="header-stats">
                <span class="stat-badge">🔥 {{ Math.round(selectedDateData.dietTotals.calories) }} kcal</span>
              </div>
            </div>
            <div class="records-list-wrapper">
              <template v-if="selectedDateData.dietRecords.length > 0">
                <div v-for="(record, index) in selectedDateData.dietRecords" :key="index" class="record-item">
                  <div class="record-header-row">
                    <div class="record-meta">
                      <span class="meal-tag" :class="getMealTypeClass(record.mealType)">
                        {{ getMealTypeName(record.mealType) }}
                      </span>
                      <span class="record-time">{{ formatTime(record.recordedAt) }}</span>
                    </div>
                    <div class="record-calories">{{ Math.round(record.calories) }} kcal</div>
                  </div>
                  <p class="food-desc">{{ record.foodDescription }}</p>
                  <div class="nutrition-tags">
                    <span class="tag tag-protein">🥩 {{ Math.round(record.protein * 10) / 10 }}g 蛋白质</span>
                    <span class="tag tag-carbs">🍞 {{ Math.round(record.carbs * 10) / 10 }}g 碳水</span>
                    <span class="tag tag-fat">🧈 {{ Math.round(record.fat * 10) / 10 }}g 脂肪</span>
                  </div>
                </div>
              </template>
              <div v-else class="empty-tip">🍽️ 这一天没有饮食记录</div>
            </div>
          </div>
          
          <!-- 健身记录 -->
          <div class="detail-card fitness-section">
            <div class="card-header">
              <h3>💪 健身记录</h3>
              <div class="header-stats">
                <span class="stat-badge stat-success">🔥 {{ Math.round(selectedDateData.fitnessTotals.caloriesBurned) }} kcal 消耗</span>
              </div>
            </div>
            <div class="records-list-wrapper">
              <template v-if="selectedDateData.fitnessRecords.length > 0">
                <div v-for="(record, index) in selectedDateData.fitnessRecords" :key="index" class="record-item">
                  <div class="record-header-row">
                    <div class="record-meta">
                      <span class="fitness-badge">{{ record.workoutName }}</span>
                      <span class="record-time">{{ formatTime(record.recordedAt) }}</span>
                    </div>
                    <div class="record-calories burn">{{ Math.round(record.caloriesBurned) }} kcal</div>
                  </div>
                  <div class="fitness-details">
                    <span v-if="record.durationMinutes" class="detail-tag tag-duration">⏱️ {{ record.durationMinutes }}分钟</span>
                    <span v-if="record.repetitions" class="detail-tag tag-reps">🔢 {{ record.repetitions }}个</span>
                    <span v-if="record.weightKg" class="detail-tag tag-weight">🏋️ {{ record.weightKg }}kg</span>
                  </div>
                </div>
              </template>
              <div v-else class="empty-tip">💪 这一天没有健身记录</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import ProfessionalCalendar from '../components/ProfessionalCalendar.vue';
import { useUserStore } from '../stores/userStore';

const router = useRouter();
const userStore = useUserStore();
const currentDate = new Date();
const currentYear = ref(currentDate.getFullYear());
const currentMonth = ref(currentDate.getMonth() + 1);
const selectedDateData = ref(null);

// 格式化日期
const formatDate = (dateString) => {
  const date = new Date(dateString);
  return `${date.getMonth() + 1}月${date.getDate()}日`;
};

// 格式化时间
const formatTime = (dateTime) => {
  if (!dateTime) return '';
  const date = new Date(dateTime);
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
};

// 获取餐次类型名称
const getMealTypeName = (mealType) => {
  const mealTypes = {
    'BREAKFAST': '早餐',
    'LUNCH': '午餐',
    'DINNER': '晚餐',
    'SNACK': '加餐'
  };
  return mealTypes[mealType] || mealType;
};

// 获取餐次类型样式
const getMealTypeClass = (mealType) => {
  const classes = {
    'BREAKFAST': 'breakfast',
    'LUNCH': 'lunch',
    'DINNER': 'dinner',
    'SNACK': 'snack'
  };
  return classes[mealType] || '';
};

// 计算进度百分比
const getIntakeProgress = () => {
  const maxCalories = 2500; // 每日推荐摄入
  return Math.min((selectedDateData.value.dietTotals.calories / maxCalories) * 100, 100);
};

const getBurnProgress = () => {
  const maxBurn = 800; // 每日推荐消耗
  return Math.min((selectedDateData.value.fitnessTotals.caloriesBurned / maxBurn) * 100, 100);
};

// 计算净热量
const getNetCalories = () => {
  const intake = selectedDateData.value.dietTotals.calories;
  const burn = selectedDateData.value.fitnessTotals.caloriesBurned;
  return Math.round(intake - burn);
};

// 获取热量平衡样式类
const getBalanceClass = () => {
  const net = getNetCalories();
  if (net > 500) return 'surplus';
  if (net < -300) return 'deficit';
  return 'balanced';
};

// 获取热量平衡图标
const getBalanceIcon = () => {
  const net = getNetCalories();
  if (net > 500) return '📈';
  if (net < -300) return '📉';
  return '⚖️';
};

// 获取环形图样式
const getDonutChartStyle = () => {
  const totals = selectedDateData.value.dietTotals;
  const total = totals.protein * 4 + totals.carbs * 4 + totals.fat * 9;
  
  if (total === 0) return {};
  
  const proteinPct = (totals.protein * 4 / total) * 100;
  const carbsPct = (totals.carbs * 4 / total) * 100;
  const fatPct = (totals.fat * 9 / total) * 100;
  
  return {
    background: `conic-gradient(
      #4CAF50 0% ${proteinPct}%,
      #FF9800 ${proteinPct}% ${proteinPct + carbsPct}%,
      #F44336 ${proteinPct + carbsPct}% 100%
    )`
  };
};

// 加载指定日期的详细数据
const loadDailyDetail = async (date) => {
  try {
    const [dietResponse, fitnessResponse] = await Promise.all([
      fetch(`/api/diet/daily/${userStore.userData.userId}/${date}`),
      fetch(`/api/diet/fitness/daily/${userStore.userData.userId}/${date}`)
    ]);
    
    const dietRecords = dietResponse.ok ? await dietResponse.json() : [];
    const fitnessRecords = fitnessResponse.ok ? await fitnessResponse.json() : [];
    
    const dietTotals = { calories: 0, protein: 0, carbs: 0, fat: 0 };
    const fitnessTotals = { caloriesBurned: 0 };
    
    dietRecords.forEach(record => {
      if (record.calories) dietTotals.calories += record.calories;
      if (record.protein) dietTotals.protein += record.protein;
      if (record.carbs) dietTotals.carbs += record.carbs;
      if (record.fat) dietTotals.fat += record.fat;
    });
    
    fitnessRecords.forEach(record => {
      if (record.caloriesBurned) fitnessTotals.caloriesBurned += record.caloriesBurned;
    });
    
    selectedDateData.value = {
      date,
      dietRecords,
      fitnessRecords,
      dietTotals,
      fitnessTotals
    };
    
    setTimeout(() => {
      const detailSection = document.querySelector('.daily-detail-section');
      if (detailSection) {
        detailSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 100);
  } catch (error) {
    console.error('加载数据失败:', error);
    selectedDateData.value = {
      date,
      dietRecords: [],
      fitnessRecords: [],
      dietTotals: { calories: 0, protein: 0, carbs: 0, fat: 0 },
      fitnessTotals: { caloriesBurned: 0 }
    };
  }
};

// 导航到详细统计页面（从日历组件调用）
const navigateToDailyDetail = (date) => {
  loadDailyDetail(date);
};

// 初始化时加载当天数据
const initializeTodayData = () => {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth() + 1;
  const day = today.getDate();
  const todayStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  
  // 直接设置日期，但不滚动（因为页面刚加载）
  loadDailyDetailWithoutScroll(todayStr);
};

// 加载详细数据（不滚动，用于初始化）
const loadDailyDetailWithoutScroll = async (date) => {
  try {
    const [dietResponse, fitnessResponse] = await Promise.all([
      fetch(`/api/diet/daily/${userStore.userData.userId}/${date}`),
      fetch(`/api/diet/fitness/daily/${userStore.userData.userId}/${date}`)
    ]);
    
    const dietRecords = dietResponse.ok ? await dietResponse.json() : [];
    const fitnessRecords = fitnessResponse.ok ? await fitnessResponse.json() : [];
    
    const dietTotals = { calories: 0, protein: 0, carbs: 0, fat: 0 };
    const fitnessTotals = { caloriesBurned: 0 };
    
    dietRecords.forEach(record => {
      if (record.calories) dietTotals.calories += record.calories;
      if (record.protein) dietTotals.protein += record.protein;
      if (record.carbs) dietTotals.carbs += record.carbs;
      if (record.fat) dietTotals.fat += record.fat;
    });
    
    fitnessRecords.forEach(record => {
      if (record.caloriesBurned) fitnessTotals.caloriesBurned += record.caloriesBurned;
    });
    
    selectedDateData.value = {
      date,
      dietRecords,
      fitnessRecords,
      dietTotals,
      fitnessTotals
    };
  } catch (error) {
    console.error('加载数据失败:', error);
    selectedDateData.value = {
      date,
      dietRecords: [],
      fitnessRecords: [],
      dietTotals: { calories: 0, protein: 0, carbs: 0, fat: 0 },
      fitnessTotals: { caloriesBurned: 0 }
    };
  }
};

onMounted(() => {
  // 页面加载时自动显示当天的详细记录
  initializeTodayData();
});
</script>

<style scoped>
.calendar-page-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0;
  background: #ffffff;
  min-height: 100vh;
}

.page-header {
  text-align: center;
  padding: 48px 24px 36px;
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
  gap: 16px;
  align-items: center;
  font-size: 14px;
  color: #86868b;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f5f5f7;
  border-radius: 980px;
  font-weight: 500;
}

.calendar-main-content {
  padding: 0 48px 48px;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* 统一日历容器 */
.unified-calendar-container {
  margin-bottom: 48px;
  width: 100%;
}

.unified-calendar {
  background: #ffffff;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  border: none;
  width: 100%;
}

.calendar-header {
  text-align: center;
  margin-bottom: 32px;
  max-width: 100%;
}

.calendar-header h3 {
  font-size: 24px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.calendar-subtitle {
  font-size: 15px;
  color: #86868b;
  margin: 0;
  letter-spacing: -0.2px;
}

/* 详细统计区域样式 */
.daily-detail-section {
  margin-top: 32px;
  background: white;
  border-radius: 24px;
  padding: 40px;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  animation: slideUp 0.4s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f5f5f7;
}

.detail-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0;
}

/* 统计图表区域 */
.charts-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.stat-card {
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  border-radius: 20px;
  padding: 28px;
  border: 1px solid #eee;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
}

.stat-card h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0 0 24px 0;
}

/* 热量对比进度条 */
.progress-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.progress-bar-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-label {
  font-size: 14px;
  font-weight: 600;
  color: #666;
}

.progress-bar {
  height: 24px;
  background: #e8e8ed;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 0.8s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
  overflow: hidden;
}

.intake-bar .progress-fill {
  background: linear-gradient(90deg, #ff6b6b 0%, #ee5a52 100%);
}

.burn-bar .progress-fill {
  background: linear-gradient(90deg, #4CAF50 0%, #45a049 100%);
}

.progress-value {
  font-size: 16px;
  font-weight: 700;
  color: #1d1d1f;
}

.net-balance {
  margin-top: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
}

.net-balance strong {
  font-size: 20px;
  font-weight: 700;
}

.net-balance.surplus {
  background: linear-gradient(135deg, #fff3cd 0%, #ffe69c 100%);
  color: #856404;
}

.net-balance.deficit {
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
  color: #155724;
}

.net-balance.balanced {
  background: linear-gradient(135deg, #d1ecf1 0%, #bee5eb 100%);
  color: #0c5460;
}

.balance-icon {
  font-size: 24px;
  margin-left: auto;
}

/* 营养环形图 */
.chart-container {
  display: flex;
  align-items: center;
  gap: 32px;
}

.donut-chart {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  position: relative;
  flex-shrink: 0;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120px;
  height: 120px;
  background: white;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.total-calories {
  font-size: 28px;
  font-weight: 700;
  color: #1d1d1f;
  line-height: 1;
}

.calories-label {
  font-size: 13px;
  color: #86868b;
  margin-top: 4px;
}

.chart-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.legend-color {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  flex-shrink: 0;
}

.legend-item.protein .legend-color {
  background: #4CAF50;
}

.legend-item.carbs .legend-color {
  background: #FF9800;
}

.legend-item.fat .legend-color {
  background: #F44336;
}

.legend-label {
  font-size: 14px;
  color: #666;
  flex: 1;
}

.legend-value {
  font-size: 15px;
  font-weight: 700;
  color: #1d1d1f;
  min-width: 80px;
  text-align: right;
}

.detail-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.detail-card {
  background: #fafafa;
  border-radius: 20px;
  padding: 28px;
  border: 1px solid #eee;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 22px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0;
}

.header-stats {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stat-badge {
  padding: 8px 16px;
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  color: #ef6c00;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 700;
}

.stat-badge.stat-success {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #2e7d32;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.records-list-wrapper {
  max-height: 400px;
  overflow-y: auto;
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

.record-item {
  background: white;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e5e5e5;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  margin-bottom: 16px;
}

.record-item:last-child {
  margin-bottom: 0;
}

.record-item:hover {
  border-color: #007aff;
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.15);
  transform: translateY(-2px);
}

.record-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.meal-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.meal-tag.breakfast { background: #fff3cd; color: #856404; }
.meal-tag.lunch { background: #d1ecf1; color: #0c5460; }
.meal-tag.dinner { background: #f8d7da; color: #721c24; }
.meal-tag.snack { background: #d4edda; color: #155724; }

.fitness-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.record-time {
  color: #86868b;
  font-size: 13px;
  font-weight: 500;
}

.record-calories {
  font-size: 16px;
  font-weight: 700;
  color: #ff6b6b;
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  padding: 6px 14px;
  border-radius: 12px;
}

.record-calories.burn {
  color: #4CAF50;
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
}

.food-desc {
  margin: 0 0 16px 0;
  color: #333;
  line-height: 1.6;
  font-size: 15px;
  padding-left: 12px;
  border-left: 3px solid #007aff;
}

.fitness-details {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e5e5e5;
}

.detail-tag {
  padding: 6px 12px;
  background: #f5f5f7;
  border-radius: 10px;
  font-size: 13px;
  color: #1d1d1f;
  font-weight: 600;
}

.detail-tag.tag-duration {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
}

.detail-tag.tag-reps {
  background: linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%);
  color: #7b1fa2;
}

.detail-tag.tag-weight {
  background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
  color: #c62828;
}

.calories-info {
  font-size: 14px;
  color: #ff3b30;
  font-weight: 600;
}

.nutrition-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tag {
  padding: 6px 12px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
}

.tag.tag-protein {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #2e7d32;
}

.tag.tag-carbs {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  color: #ef6c00;
}

.tag.tag-fat {
  background: linear-gradient(135deg, #fce4ec 0%, #f8bbd9 100%);
  color: #c2185b;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #86868b;
  font-size: 15px;
  background: #fafafa;
  border-radius: 12px;
}

@media (max-width: 768px) {
  .daily-detail-section {
    padding: 24px;
  }
  
  .detail-header h2 {
    font-size: 22px;
  }
  
  .charts-section {
    grid-template-columns: 1fr;
  }
  
  .detail-content {
    grid-template-columns: 1fr;
  }
  
  .chart-container {
    flex-direction: column;
  }
  
  .donut-chart {
    width: 150px;
    height: 150px;
  }
  
  .chart-center {
    width: 100px;
    height: 100px;
  }
  
  .total-calories {
    font-size: 24px;
  }
  
  .record-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .calendar-main-content {
    padding: 0 16px 32px;
  }
  
  .unified-calendar {
    padding: 20px;
  }
  
  .calendar-header h3 {
    font-size: 20px;
  }
  
  .calendar-subtitle {
    font-size: 14px;
  }
}
</style>
