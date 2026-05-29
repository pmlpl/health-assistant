<template>
  <div ref="dashboardLayoutRef" class="dashboard-layout">
    <!-- 页面头部：设计系统 PageHeader -->
    <PageHeader title="主页" section-label="今日概览" icon="🏠">
      <template #stats>
        <span class="stat-item">{{ greeting }}, {{ userStore.userData.userId }}！</span>
        <span v-if="userStore.userData.goal" class="stat-item">
          🎯 目标: {{ userStore.userData.goal }}
        </span>
        <span v-else class="stat-item">🌟 开始您的健康之旅吧！</span>
      </template>
    </PageHeader>

    <!-- 不对称 Bento 海报式布局 -->
    <div class="dashboard-bento">
      <!-- 今日概览 + 快捷操作：黄色大色块 -->
      <div class="card overview-actions-card bh-card bento-cell bento-cell--overview bauhaus-block bauhaus-block--yellow">
        <div class="card-header-section">
          <SectionLabel label="今日数据" />
          <h2>📊 今日概览</h2>
        </div>
        <div class="stats-grid">
          <div class="stat-card" title="今日已记录的饮食次数">
            <div class="stat-icon">🍽️</div>
            <div class="stat-content">
              <div class="stat-number" :data-count-immediate="todayRecordsCount">{{ todayRecordsCount }}</div>
              <div class="stat-label">饮食记录</div>
            </div>
          </div>
          <div class="stat-card" title="今日总热量摄入">
            <div class="stat-icon">🔥</div>
            <div class="stat-content">
              <div class="stat-number" :data-count-immediate="todayCalories">{{ todayCalories }}</div>
              <div class="stat-label">摄入热量 (kcal)</div>
            </div>
          </div>
          <div class="stat-card" title="今日饮水量">
            <div class="stat-icon">💧</div>
            <div class="stat-content">
              <div class="stat-number" :data-count-immediate="dailyWaterIntake">{{ dailyWaterIntake }}</div>
              <div class="stat-label">饮水杯数</div>
            </div>
          </div>
          <div class="stat-card" title="今日运动时长">
            <div class="stat-icon">🏃</div>
            <div class="stat-content">
              <div class="stat-number" :data-count-immediate="todayExerciseMinutes">{{ todayExerciseMinutes }}</div>
              <div class="stat-label">运动分钟</div>
            </div>
          </div>
        </div>
        <div class="action-section-divider"></div>
        <div class="action-grid">
          <button class="btn action-button primary" @click="$router.push('/diary')">
            <span class="button-icon">📝</span>
            <span class="button-text">记录饮食</span>
          </button>
          <button class="btn action-button secondary" @click="$router.push('/fitness')">
            <span class="button-icon">🏃</span>
            <span class="button-text">健身记录</span>
          </button>
          <button class="btn action-button accent" @click="$router.push('/recipes')">
            <span class="button-icon">🥗</span>
            <span class="button-text">健康食谱</span>
          </button>
          <button class="btn action-button info" @click="$router.push('/profile')">
            <span class="button-icon">👤</span>
            <span class="button-text">个人档案</span>
          </button>
        </div>
      </div>

      <!-- 喝水 + 转盘：蓝色竖向大色块 -->
      <div class="card water-wheel-card bh-card bh-card--blue bento-cell bento-cell--water bauhaus-block bauhaus-block--blue">
        <div class="card-header-section">
          <SectionLabel label="饮水打卡" />
          <h2>💧 喝水打卡</h2>
        </div>
        <div class="water-intake-container">
          <div class="water-progress">
            <div class="water-progress__row">
              <div class="water-ring-wrap" aria-hidden="true">
                <svg class="water-ring" viewBox="0 0 100 100">
                  <circle class="water-ring__track" cx="50" cy="50" r="42" />
                  <circle class="water-ring__fill" cx="50" cy="50" r="42" :style="waterRingStyle" />
                </svg>
                <span class="water-ring__pct">{{ waterProgress }}%</span>
              </div>
              <div class="water-progress__meta">
                <div class="progress-info">
                  <span class="progress-label">今日进度</span>
                  <span class="progress-percent">{{ waterProgress }}%</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill" :style="`width: ${waterProgress}%`"></div>
                </div>
                <div class="progress-status">
                  <span :class="{ 'status-success': isWaterTargetAchieved, 'status-pending': !isWaterTargetAchieved }">
                    {{ isWaterTargetAchieved ? '✅ 已达成今日饮水目标！' : `还需 ${targetWaterIntake - dailyWaterIntake} 次打卡` }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          
          <button ref="waterBtnRef" class="btn water-btn" @click="addWaterIntake" :disabled="isWaterTargetAchieved">
            <span class="btn-icon">💧</span>
            <span class="btn-text">{{ isWaterTargetAchieved ? '已达标，明日再接再厉！' : '点击打卡喝水' }}</span>
          </button>
        </div>
        <div class="action-section-divider"></div>
        <div class="wheel-section">
          <div class="bh-strip">
            <h2 class="wheel-subtitle-title">🎡 每日健康转盘</h2>
            <p class="wheel-subtitle">不知道今天该做什么？转转看！</p>
          </div>
          <div class="wheel-container">
            <div class="wheel-wrapper">
              <div
                ref="wheelRef"
                class="wheel"
                :class="{ spinning: isSpinning }"
                :style="{ transform: `rotate(${wheelRotation}deg)` }"
              >
                <div class="wheel-bg"></div>
                <div
                  ref="wheelIconsRef"
                  class="wheel-icons"
                  :style="{ transform: `rotate(${-wheelRotation}deg)` }"
                >
                  <div 
                    v-for="(item, index) in wheelItems" 
                    :key="index" 
                    class="wheel-icon"
                    :style="getIconStyle(index)"
                  >
                    {{ item.emoji }}
                  </div>
                </div>
              </div>
              <div ref="pointerRef" class="wheel-pointer">▼</div>
              <div class="wheel-center" @click="spinWheel" :class="{ 'disabled': isSpinning }">
                <span class="center-text">{{ isSpinning ? '...' : '开始' }}</span>
              </div>
            </div>
          </div>
          <div v-if="selectedSuggestion" class="suggestion-result" :class="{ 'show': showSuggestion }">
            <div class="result-icon">{{ selectedSuggestion.emoji }}</div>
            <div class="result-title">{{ selectedSuggestion.title }}</div>
            <div class="result-desc">{{ selectedSuggestion.description }}</div>
          </div>
        </div>

        <!-- 饮水格 + 打卡记录：填满列底空白 -->
        <div class="water-log-panel">
          <div class="water-log-panel__header">
            <h3 class="water-log-panel__title">今日饮水格</h3>
            <button type="button" class="water-log-panel__link" @click="showWaterLogDetail = true">
              详情 →
            </button>
          </div>
          <div class="water-cup-grid" :aria-label="`饮水进度 ${dailyWaterIntake}/${targetWaterIntake}`">
            <div
              v-for="n in targetWaterIntake"
              :key="n"
              class="water-cup-slot"
              :class="{ 'water-cup-slot--filled': n <= dailyWaterIntake }"
            >
              <span class="water-cup-slot__icon" aria-hidden="true">{{ n <= dailyWaterIntake ? '💧' : '' }}</span>
              <span class="water-cup-slot__num">{{ n }}</span>
            </div>
          </div>
          <ul v-if="waterLog.length > 0" class="water-time-list">
            <li v-for="(record, idx) in recentWaterLog" :key="idx">
              <span class="water-time-list__idx">第 {{ waterLog.length - idx }} 次</span>
              <span class="water-time-list__time">{{ record.time }}</span>
            </li>
          </ul>
          <p v-else class="water-log-panel__hint">点击「打卡喝水」开始记录，格子会逐格点亮</p>
        </div>
      </div>

      <!-- 营养摄入：与今日概览同宽（左 7 列）；类名避开 RecipesView 全局 .nutrition-card -->
      <div class="card dashboard-nutrition-card bh-card bento-cell bento-cell--nutrition">
        <div class="card-header-section">
          <SectionLabel label="营养摄入" />
          <h2>🥗 今日营养摄入</h2>
        </div>
        <div class="nutrition-progress-container">
          <div class="nutrition-progress-item">
            <div class="progress-header">
              <span class="progress-icon protein">🥩</span>
              <span class="progress-label">蛋白质</span>
              <span class="progress-value">{{ todayProtein }}g</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill protein nutrition-progress-fill" :data-progress="getNutritionProgress(todayProtein, 50)" :style="`width: ${getNutritionProgress(todayProtein, 50)}%`"></div>
            </div>
            <div class="progress-info">
              <span class="progress-percent">{{ getNutritionProgress(todayProtein, 50) }}%</span>
              <span class="progress-target">目标: 50g</span>
            </div>
          </div>

          <div class="nutrition-progress-item">
            <div class="progress-header">
              <span class="progress-icon carbs">🍚</span>
              <span class="progress-label">碳水</span>
              <span class="progress-value">{{ todayCarbs }}g</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill carbs nutrition-progress-fill" :data-progress="getNutritionProgress(todayCarbs, 150)" :style="`width: ${getNutritionProgress(todayCarbs, 150)}%`"></div>
            </div>
            <div class="progress-info">
              <span class="progress-percent">{{ getNutritionProgress(todayCarbs, 150) }}%</span>
              <span class="progress-target">目标: 150g</span>
            </div>
          </div>

          <div class="nutrition-progress-item">
            <div class="progress-header">
              <span class="progress-icon fat">🥑</span>
              <span class="progress-label">脂肪</span>
              <span class="progress-value">{{ todayFat }}g</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill fat nutrition-progress-fill" :data-progress="getNutritionProgress(todayFat, 60)" :style="`width: ${getNutritionProgress(todayFat, 60)}%`"></div>
            </div>
            <div class="progress-info">
              <span class="progress-percent">{{ getNutritionProgress(todayFat, 60) }}%</span>
              <span class="progress-target">目标: 60g</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 健康贴士：红色宽色块 -->
      <div class="card tips-card bh-card bento-cell bento-cell--tips bauhaus-block bauhaus-block--red">
        <h2 class="bh-heading-on-color">💡 健康小贴士</h2>
        <div class="tips-container">
          <div class="tip-item" v-for="(tip, index) in healthTips" :key="index">
            <div class="tip-header">
              <span class="tip-category">{{ tip.healthGoal || '健康生活' }}</span>
              <span class="tip-icon">✨</span>
            </div>
            <p class="tip-content">{{ tip.tip || getRandomTip() }}</p>
          </div>
        </div>
      </div>




    </div>

    <!-- 喝水记录详情弹窗：移出 Bento 网格，避免占据 grid 子项槽位 -->
    <div v-if="showWaterLogDetail" class="modal-overlay" @click="closeWaterLogDetail">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>📋 今日喝水记录详情</h3>
          <button @click="closeWaterLogDetail" class="close-button">×</button>
        </div>

        <div class="modal-body">
          <div v-if="waterLog.length > 0" class="water-log-detail">
            <div class="log-timeline">
              <div
                v-for="(record, index) in waterLog"
                :key="index"
                class="timeline-item"
              >
                <div class="timeline-marker">
                  <span class="marker-icon">💧</span>
                </div>
                <div class="timeline-content">
                  <div class="timeline-time">{{ record.time }}</div>
                  <div class="timeline-label">第 {{ index + 1 }} 次打卡</div>
                </div>
              </div>
            </div>

            <div class="daily-water-summary">
              <h4>今日总结</h4>
              <div class="summary-stats">
                <div class="stat-item">
                  <span class="stat-icon">💧</span>
                  <span class="stat-label">总次数</span>
                  <span class="stat-value">{{ waterLog.length }} / {{ targetWaterIntake }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon">⏰</span>
                  <span class="stat-label">首次打卡</span>
                  <span class="stat-value">{{ waterLog[0]?.time || '--:--' }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-icon">🎯</span>
                  <span class="stat-label">完成度</span>
                  <span class="stat-value">{{ waterProgress }}%</span>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="no-data">
            <div class="no-data-icon">🏜️</div>
            <p>今天还没有喝水记录</p>
            <button class="btn drink-now-btn" @click="addWaterIntake; closeWaterLogDetail()">
              立即补水
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Toast 提示 -->
  <transition name="toast">
    <div v-if="showToast" class="toast-container" :class="`toast-${toastType}`">
      <span class="toast-icon">{{ toastType === 'success' ? '✅' : '⚠️' }}</span>
      <span class="toast-message">{{ toastMessage }}</span>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useUserStore } from '../stores/userStore';
import { healthApi } from '../api/healthApi';
import SectionLabel from '../components/common/SectionLabel.vue';
import PageHeader from '@/components/common/PageHeader.vue';

const userStore = useUserStore();
const dashboardLayoutRef = ref(null);
const wheelRef = ref(null);
const wheelIconsRef = ref(null);
const pointerRef = ref(null);
const waterBtnRef = ref(null);
const WATER_RING_CIRC = 264;
let currentWheelRotation = 0;

// 数据状态
const todayRecordsCount = ref(0);
const todayCalories = ref(0);
const todayProtein = ref(0);
const todayCarbs = ref(0);
const todayFat = ref(0);
const todayExerciseMinutes = ref(0);
const healthTips = ref([]);

// Toast 提示状态
const showToast = ref(false);
const toastMessage = ref('');
const toastType = ref('success'); // 'success' | 'error'

// 喝水打卡相关状态
const dailyWaterIntake = ref(0); // 当前每日喝水杯数
const targetWaterIntake = ref(8); // 目标喝水杯数
const waterLog = ref([]); // 喝水记录
const showWaterLogDetail = ref(false); // 是否显示喝水记录详情弹窗
const lastWaterTime = ref(''); // 最近一次喝水时间

// 转盘相关状态
const wheelItems = ref([
  { emoji: '🏃', title: '去运动', description: '今天去跑步或健身30分钟，让身体充满活力！', color: '#d02020' },
  { emoji: '🥗', title: '吃健康餐', description: '做一份营养均衡的沙拉，多吃蔬菜少吃油腻！', color: '#1040c0' },
  { emoji: '💧', title: '多喝水', description: '每小时喝一杯水，保持身体水分充足！', color: '#f0c020' },
  { emoji: '😴', title: '早睡觉', description: '今晚11点前入睡，保证8小时优质睡眠！', color: '#d02020' },
  { emoji: '🧘', title: '做冥想', description: '花10分钟做深呼吸冥想，放松身心！', color: '#1040c0' },
  { emoji: '🍎', title: '吃水果', description: '今天吃2-3种不同颜色的水果补充维生素！', color: '#f0c020' },
  { emoji: '🚶', title: '多走路', description: '今天走满10000步，少坐电梯多走楼梯！', color: '#d02020' },
  { emoji: '🥛', title: '喝牛奶', description: '喝一杯牛奶或酸奶，补充钙质和蛋白质！', color: '#1040c0' },
]);
const wheelRotation = ref(0);
const isSpinning = ref(false);
const selectedSuggestion = ref(null);
const showSuggestion = ref(false);

// 获取图标位置样式
const getIconStyle = (index) => {
  // 每个扇形45度，图标放在扇形中间（22.5度偏移）
  // 背景从-22.5度开始，所以图标角度需要相应调整
  const angle = (index * 45) - 90; // -90度从顶部开始
  const radius = 85; // 距离中心的半径
  const radian = (angle * Math.PI) / 180;
  const x = Math.cos(radian) * radius;
  const y = Math.sin(radian) * radius;

  return {
    transform: `translate(${x}px, ${y}px)`
  };
};

// 转动转盘（CSS transition，无弹性 GSAP）
const spinWheel = () => {
  if (isSpinning.value) return;

  isSpinning.value = true;
  showSuggestion.value = false;

  const randomIndex = Math.floor(Math.random() * wheelItems.value.length);
  const anglePerSegment = 360 / wheelItems.value.length;
  const targetAngle = randomIndex * anglePerSegment + 22.5;
  const extraRotation = 360 * (5 + Math.floor(Math.random() * 3));
  const currentRotation = currentWheelRotation % 360;
  const rotationNeeded = extraRotation + (360 - targetAngle) - currentRotation + 22.5;
  const finalRotation = currentWheelRotation + rotationNeeded;

  currentWheelRotation = finalRotation;
  wheelRotation.value = finalRotation;

  const spinMs = 2800;
  setTimeout(() => {
    selectedSuggestion.value = wheelItems.value[randomIndex];
    showSuggestion.value = true;
    isSpinning.value = false;
  }, spinMs);
};

/** 喝水环形进度样式（CSS，无 GSAP） */
const waterRingStyle = computed(() => ({
  strokeDasharray: `${WATER_RING_CIRC}`,
  strokeDashoffset: `${WATER_RING_CIRC * (1 - waterProgress.value / 100)}`,
}));

// 根据时间显示不同的问候语
const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 12) {
    return '🌞 早上好';
  } else if (hour < 18) {
    return '🌤️ 下午好';
  } else {
    return '🌙 晚上好';
  }
});

// 计算营养摄入进度百分比
const getNutritionProgress = (current, target) => {
  if (!current || !target) return 0;
  const progress = (current / target) * 100;
  return Math.min(Math.round(progress), 100);
};

// 获取本地日期字符串 (YYYY-MM-DD)
const getLocalDateString = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// 加载今日数据
const loadTodayData = async () => {
  const userId = userStore.userData?.userId;
  if (!userId) {
    return;
  }
  
  try {
    const today = getLocalDateString();
    const records = await healthApi.getDailyDiet(userId, today);
    const filteredRecords = (records || []).filter(record => 
      !(record.foodDescription && 
        (record.foodDescription.includes('💧 喝水打卡') || 
         record.foodDescription.toLowerCase().includes('water打卡')))
    );
    
    todayRecordsCount.value = filteredRecords?.length || 0;
    
    if (filteredRecords && filteredRecords.length > 0) {
      const totalCalories = filteredRecords.reduce((sum, record) => sum + (record.calories || 0), 0);
      const totalProtein = filteredRecords.reduce((sum, record) => sum + (record.protein || 0), 0);
      const totalCarbs = filteredRecords.reduce((sum, record) => sum + (record.carbs || 0), 0);
      const totalFat = filteredRecords.reduce((sum, record) => sum + (record.fat || 0), 0);
      
      todayCalories.value = Math.round(totalCalories);
      todayProtein.value = Math.round(totalProtein * 10) / 10;
      todayCarbs.value = Math.round(totalCarbs * 10) / 10;
      todayFat.value = Math.round(totalFat * 10) / 10;

    } else {
      todayCalories.value = 0;
      todayProtein.value = 0;
      todayCarbs.value = 0;
      todayFat.value = 0;
    }
    
    // 加载健身记录
    const fitnessRecords = await healthApi.getDailyFitnessRecords(userId, today);
    if (fitnessRecords && fitnessRecords.length > 0) {
      const totalMinutes = fitnessRecords.reduce((sum, record) => sum + (record.durationMinutes || 0), 0);
      todayExerciseMinutes.value = totalMinutes;
    } else {
      todayExerciseMinutes.value = 0;
    }
    
    // 加载喝水记录
    const waterRecords = await healthApi.getDailyWaterIntake(userId, today);
    // 处理喝水记录，提取时间
    const processedWaterRecords = (waterRecords || []).map(record => {
      let waterTime = '';
      if (record.foodDescription) {
        const match = record.foodDescription.match(/喝水打卡\s*-\s*(\d{1,2}:\d{2})/);
        if (match) {
          waterTime = match[1];
        }
      }
      return {
        ...record,
        time: waterTime || record.recordedAt?.substring(11, 16) || ''
      };
    });
    
    dailyWaterIntake.value = processedWaterRecords.length;
    waterLog.value = processedWaterRecords;
    
    // 设置最近一次喝水时间
    if (processedWaterRecords.length > 0) {
      const lastRecord = processedWaterRecords[processedWaterRecords.length - 1];
      lastWaterTime.value = lastRecord.time;
    } else {
      lastWaterTime.value = '';
    }
  } catch (err) {
    console.error('加载今日数据失败:', err);
    todayCalories.value = 0;
    todayProtein.value = 0;
    todayCarbs.value = 0;
    todayFat.value = 0;
    todayExerciseMinutes.value = 0;
    dailyWaterIntake.value = 0;
  }
};

// 喝水打卡
const addWaterIntake = async () => {
  try {
    const today = getLocalDateString();
    const currentTime = new Date().toTimeString().split(' ')[0].slice(0, 5);
    
    await healthApi.addWaterIntake(userStore.userData.userId, {
      date: today,
      time: currentTime
    });
    
    // 重新加载喝水记录
    const updatedRecords = await healthApi.getDailyWaterIntake(userStore.userData.userId, today);
    
    // 处理喝水记录，提取时间
    const processedRecords = (updatedRecords || []).map(record => {
      let waterTime = '';
      if (record.foodDescription) {
        const match = record.foodDescription.match(/喝水打卡\s*-\s*(\d{1,2}:\d{2})/);
        if (match) {
          waterTime = match[1];
        }
      }
      return {
        ...record,
        time: waterTime || record.recordedAt?.substring(11, 16) || currentTime
      };
    });
    
    waterLog.value = processedRecords;
    dailyWaterIntake.value = processedRecords.length;
    
    // 更新最近一次喝水时间
    if (waterLog.value.length > 0) {
      const lastRecord = waterLog.value[waterLog.value.length - 1];
      lastWaterTime.value = lastRecord.time;
    }
    
    // 显示成功提示
    const isSuccess = dailyWaterIntake.value >= targetWaterIntake.value;
    toastMessage.value = `💧 第${dailyWaterIntake.value}次喝水打卡成功！${isSuccess ? '🎉 恭喜达成今日饮水目标！' : `距离目标还有${targetWaterIntake.value - dailyWaterIntake.value}次`}`;
    toastType.value = 'success';
    showToast.value = true;
    
    // 3 秒后自动隐藏提示
    setTimeout(() => {
      showToast.value = false;
    }, 3000);
  } catch (err) {
    console.error('喝水打卡失败:', err);
    toastMessage.value = '❌ 喝水打卡失败，请重试。';
    toastType.value = 'error';
    showToast.value = true;
    
    // 3 秒后自动隐藏提示
    setTimeout(() => {
      showToast.value = false;
    }, 3000);
  }
};

// 计算饮水进度
const waterProgress = computed(() => {
  return Math.min(Math.round((dailyWaterIntake.value / targetWaterIntake.value) * 100), 100);
});

// 是否已达标
const isWaterTargetAchieved = computed(() => {
  return dailyWaterIntake.value >= targetWaterIntake.value;
});

/** 最近 4 条饮水记录（倒序，用于面板展示） */
const recentWaterLog = computed(() => {
  return [...waterLog.value].reverse().slice(0, 4);
});

// 随机健康小贴士库
const healthTipLibrary = [
  '每天吃五种不同颜色的水果和蔬菜，有助于摄取全面的营养。',
  '保持规律的作息时间，每天保证 7-8 小时的睡眠。',
  '每周至少进行 150 分钟的中等强度有氧运动。',
  '吃饭时细嚼慢咽，有助于消化吸收和控制食量。',
  '每天喝足够的水，保持身体水分平衡。',
  '减少加工食品的摄入，多吃天然食物。',
  '保持正确的坐姿，避免长时间低头使用手机。',
  '每天晒太阳 15-20 分钟，促进维生素 D 的合成。',
  '学会管理压力，可以通过冥想、深呼吸等方式放松身心。',
  '定期体检，及时了解自己的身体状况。'
];

// 获取随机健康小贴士
const getRandomTip = () => {
  const randomIndex = Math.floor(Math.random() * healthTipLibrary.length);
  return healthTipLibrary[randomIndex];
};

// 初始化健康贴士
const initHealthTips = () => {
  healthTips.value = [
    {
      healthGoal: '营养均衡',
      tip: getRandomTip()
    },
    {
      healthGoal: '科学饮水',
      tip: '每天保证充足的水分摄入，有助于身体各项机能正常运转。'
    }
  ];
};

// 关闭喝水记录详情弹窗
const closeWaterLogDetail = () => {
  showWaterLogDetail.value = false;
};


onMounted(async () => {
  if (userStore.userData?.userId) {
    await loadTodayData();
  }
  initHealthTips();
});

watch(
  () => userStore.userData?.userId,
  (newUserId, oldUserId) => {
    if (newUserId && newUserId !== oldUserId) {
      loadTodayData();
    }
  },
  { immediate: true }
);
</script>

<style scoped>

.dashboard-layout {
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  padding: 0;
  background: var(--color-background);
  min-height: 100vh;
  position: relative;
}

.page-header {
  text-align: center;
  padding: 32px 24px 24px;
  position: relative;
  z-index: 1;
  background: var(--color-foreground);
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
  box-shadow: none;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: none;
  transform: translateY(-2px);
}

.main-container,
.dashboard-bento {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: var(--space-4);
  padding: var(--space-6) var(--space-4) var(--space-12);
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: var(--content-area-max-width);
  margin: 0 auto;
}

/* Bento 不对称分区：左 7 列概览+营养同宽，右 5 列饮水区跨两行 */
.bento-cell {
  width: 100%;
  min-width: 0;
  justify-self: stretch;
}

.dashboard-bento > .bento-cell--overview,
.dashboard-bento > .bento-cell--nutrition {
  grid-column: 1 / 8;
}

.bento-cell--overview {
  grid-row: 1;
}

.bento-cell--water {
  grid-column: 8 / -1;
  grid-row: 1 / span 2;
}

.bento-cell--nutrition {
  grid-row: 2;
}

.bento-cell--tips {
  grid-column: 1 / -1;
}

@media (max-width: 1023px) {
  .dashboard-bento {
    grid-template-columns: 1fr;
  }

  .bento-cell--overview,
  .bento-cell--water,
  .bento-cell--nutrition,
  .bento-cell--tips {
    grid-column: 1 / -1;
    grid-row: auto;
  }
}

.card {
  background: var(--color-card);
  border-radius: 0;
  padding: var(--space-6);
  margin-bottom: 0;
  border: var(--border-width-thick) solid var(--color-border);
  box-shadow: var(--shadow-xl);
  transition: transform var(--transition-fast);
  position: relative;
  overflow: visible;
}

.card:hover {
  transform: translateY(-4px);
}

.bauhaus-block--blue .section-label,
.bauhaus-block--yellow .section-label,
.bauhaus-block--red .section-label {
  background: #fff;
  color: var(--color-foreground);
}

.bauhaus-block--blue > .card-header-section h2,
.bauhaus-block--red > .bh-heading-on-color {
  color: #fff;
  text-shadow: 1px 1px 0 rgba(0, 0, 0, 0.25);
}

/* 蓝色饮水区：进度条改白卡片，文字深色 */
.bauhaus-block--blue .water-progress {
  background: #fff;
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.bauhaus-block--blue .water-progress .progress-label,
.bauhaus-block--blue .water-progress .progress-percent,
.bauhaus-block--blue .water-progress .progress-status,
.bauhaus-block--blue .water-ring__pct {
  color: var(--color-foreground);
}

.bauhaus-block--blue .water-btn {
  background: var(--color-primary-yellow);
  color: var(--color-foreground);
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-md);
}

.bauhaus-block--blue .water-btn:hover:not(:disabled) {
  background: #fff;
  transform: translateY(-2px);
}

.bauhaus-block--blue .action-section-divider {
  background: rgba(255, 255, 255, 0.45);
}

/* 合并卡片特殊样式 */
.overview-actions-card,
.water-wheel-card {
  padding: 32px 24px;
}

.water-wheel-card {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.card-header-section {
  margin-bottom: 20px;
}

.action-section-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 24px 0;
}

.wheel-section {
  margin-top: 8px;
}

/* 营养卡片：占满 Bento 列宽，边框完整包裹内部三列网格 */
.dashboard-nutrition-card {
  padding: 32px 24px;
  min-height: 320px;
  width: 100%;
  min-width: 0;
  max-width: none;
  box-sizing: border-box;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
}

.card h2 {
  color: var(--color-foreground);
  margin-bottom: 20px;
  padding-bottom: 0;
  border-bottom: none;
  font-weight: var(--font-weight-black);
  text-transform: uppercase;
  letter-spacing: -0.02em;
  font-size: var(--font-size-xl);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 0;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  transition: transform var(--transition-fast);
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  display: none;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  font-size: 1.1rem;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-foreground);
  border-radius: 12px;
  color: white;
  box-shadow: none;
  position: relative;
  z-index: 1;
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.stat-content {
  text-align: left;
  position: relative;
  z-index: 1;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 4px;
  letter-spacing: -2px;
  line-height: 1;
}

.stat-label {
  color: #86868b;
  font-size: 12px;
  line-height: 1.4;
  letter-spacing: -0.1px;
  font-weight: 500;
  text-transform: none;
}

.water-intake-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.water-progress {
  background: var(--color-bg-subtle);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  border: 1px solid var(--color-border);
}

.water-progress__row {
  display: flex;
  align-items: center;
  gap: var(--space-5);
}

.water-progress__meta {
  flex: 1;
  min-width: 0;
}

.water-ring-wrap {
  position: relative;
  width: 88px;
  height: 88px;
  flex-shrink: 0;
}

.water-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.water-ring__track {
  fill: none;
  stroke: var(--color-border-strong);
  stroke-width: 6;
}

.water-ring__fill {
  fill: none;
  stroke: var(--color-primary);
  stroke-width: 6;
  stroke-linecap: round;
}

.water-ring__pct {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.water-btn {
  position: relative;
  overflow: hidden;
}

.water-ripple {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 48px;
  height: 48px;
  margin: -24px 0 0 -24px;
  border-radius: 50%;
  background: var(--color-primary-muted);
  pointer-events: none;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-label {
  font-weight: 600;
  color: var(--color-foreground);
  letter-spacing: -0.2px;
  font-size: 17px;
}

.progress-percent {
  font-weight: var(--font-weight-black);
  font-size: 28px;
  color: var(--color-foreground);
  letter-spacing: -1px;
}

.progress-bar {
  height: 12px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 16px;
}

.progress-fill {
  height: 100%;
  background: var(--color-foreground);
  border-radius: 6px;
  transition: width 0.8s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--color-border-light);
  animation: shimmer 2.5s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-status {
  text-align: center;
}

.status-success {
  color: #34c759;
  font-weight: 600;
  letter-spacing: -0.2px;
  font-size: 15px;
}

.status-pending {
  color: #ff9500;
  font-weight: 600;
  letter-spacing: -0.2px;
  font-size: 15px;
}

.btn {
  padding: 16px 32px;
  border: none;
  border-radius: 980px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  letter-spacing: -0.2px;
  position: relative;
  overflow: hidden;
}

.water-btn {
  background: var(--color-foreground);
  color: white;
  box-shadow: none;
}

.water-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: var(--color-border-light);
  transition: left 0.6s ease;
}

.water-btn:hover:not(:disabled)::before {
  left: 100%;
}

.water-btn:hover:not(:disabled) {
  background: #0066d6;
  transform: scale(1.02);
  box-shadow: none;
}

.water-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.water-btn:disabled {
  background: #d1d1d6;
  color: #86868b;
  cursor: not-allowed;
  box-shadow: none;
}

.btn-icon {
  font-size: 1.1rem;
}

.btn-text {
  font-size: 17px;
}

.water-log-summary {
  background: var(--color-muted);
  border-radius: 16px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
}

.water-log-summary:hover {
  background: #ebebf0;
  transform: scale(1.01);
}

.summary-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-icon {
  font-size: 1.1rem;
}

.summary-text {
  flex: 1;
  color: var(--color-foreground);
  font-size: 15px;
  letter-spacing: -0.1px;
  margin-left: 8px;
}

.last-time {
  color: var(--color-foreground);
  font-weight: 500;
  margin-left: 4px;
}

.summary-arrow {
  font-size: 1.25rem;
  color: #c7c7cc;
  transition: transform 0.3s ease;
}

.water-log-summary:hover .summary-arrow {
  transform: translateX(4px);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 100px;
  z-index: 99999;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: #ffffff;
  border-radius: 24px;
  padding: 32px;
  width: 90%;
  max-width: 480px;
  box-shadow: none;
  position: relative;
  border: none;
  animation: slideUp 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
}

@keyframes slideUp {
  from { 
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to { 
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-content::before {
  display: none;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.modal-header h3 {
  font-size: 22px;
  margin: 0;
  color: var(--color-foreground);
  font-weight: 600;
  letter-spacing: -0.3px;
}

.close-button {
  background: var(--color-muted);
  border: none;
  font-size: 18px;
  color: #86868b;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  border-radius: 50%;
}

.close-button:hover {
  color: var(--color-foreground);
  background: #ebebf0;
}

.modal-body {
  max-height: 400px;
  overflow-y: auto;
}

.modal-body::-webkit-scrollbar {
  width: 8px;
}

.modal-body::-webkit-scrollbar-track {
  background: transparent;
}

.modal-body::-webkit-scrollbar-thumb {
  background: #d1d1d6;
  border-radius: 4px;
}

.water-log-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.log-timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--color-muted);
  border-radius: 16px;
  border: none;
  transition: all 0.3s ease;
}

.timeline-item:hover {
  background: #ebebf0;
}

.timeline-marker {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-foreground);
  border-radius: 50%;
  color: white;
  font-size: 0.9rem;
  box-shadow: none;
}

.timeline-content {
  flex: 1;
}

.timeline-time {
  font-size: 20px;
  color: var(--color-foreground);
  font-weight: 600;
  letter-spacing: -0.5px;
}

.timeline-label {
  font-size: 13px;
  color: #86868b;
  letter-spacing: -0.1px;
  margin-top: 2px;
}

.daily-water-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.daily-water-summary h4 {
  font-size: 17px;
  margin: 0;
  color: var(--color-foreground);
  font-weight: 600;
  letter-spacing: -0.2px;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: var(--color-muted);
  border-radius: 16px;
  border: none;
}

.stat-icon {
  font-size: 1.25rem;
  color: var(--color-foreground);
  width: auto;
  height: auto;
  background: none;
  border-radius: 0;
}

.stat-label {
  font-size: 12px;
  color: #86868b;
  letter-spacing: -0.1px;
  text-transform: none;
}

.stat-value {
  font-size: 17px;
  color: var(--color-foreground);
  font-weight: 600;
  letter-spacing: -0.3px;
}

.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 40px;
}

.no-data-icon {
  font-size: 64px;
  opacity: 0.3;
}

.no-data p {
  font-size: 17px;
  color: #86868b;
  text-align: center;
  margin: 0;
  letter-spacing: -0.2px;
}

.drink-now-btn {
  background: var(--color-foreground);
  color: white;
  padding: 14px 28px;
  border-radius: 980px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
  box-shadow: none;
  letter-spacing: -0.2px;
}

.drink-now-btn:hover {
  background: #0066d6;
  transform: scale(1.02);
  box-shadow: none;
}

/* 营养摄入：三列等宽网格，子项可收缩以防撑破父级边框 */
.nutrition-progress-container {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
  width: 100%;
  min-width: 0;
}

@media (max-width: 1023px) {
  .nutrition-progress-container {
    grid-template-columns: minmax(0, 1fr);
  }
}

/* 每项固定纵向分区：标题区 / 进度条 / 底部信息，保证等高对齐 */
.nutrition-progress-item {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 132px;
  background: #fff;
  border-radius: 0;
  padding: var(--space-5);
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  box-sizing: border-box;
}

.nutrition-progress-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* 营养区专用进度样式，与饮水区 .progress-* 解耦 */
.nutrition-progress-item .progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
  min-height: 48px;
  flex-shrink: 0;
}

.nutrition-progress-item .progress-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  border: var(--border-width) solid var(--color-border);
  font-size: 1.25rem;
  flex-shrink: 0;
}

.nutrition-progress-item .progress-icon.protein {
  background: var(--color-primary-red);
  color: #fff;
}

.nutrition-progress-item .progress-icon.carbs {
  background: var(--color-primary-blue);
  color: #fff;
}

.nutrition-progress-item .progress-icon.fat {
  background: var(--color-primary-yellow);
  color: var(--color-foreground);
}

.nutrition-progress-item .progress-label {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  color: var(--color-foreground);
  font-size: var(--font-size-base);
  letter-spacing: -0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nutrition-progress-item .progress-value {
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-lg);
  color: var(--color-foreground);
  flex-shrink: 0;
  min-width: 3.5rem;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* 固定高度进度条槽，三项视觉等高 */
.nutrition-progress-item .progress-bar {
  height: 12px;
  flex-shrink: 0;
  background: var(--color-muted);
  border: var(--border-width) solid var(--color-border);
  border-radius: 0;
  overflow: hidden;
  margin-bottom: var(--space-3);
  position: relative;
}

.nutrition-progress-item .progress-fill {
  height: 100%;
  border-radius: 0;
  transition: width 1s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
}

.nutrition-progress-item .progress-fill::after {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--color-border-light);
  animation: shimmer 2s infinite;
}

.nutrition-progress-item .progress-fill.protein,
.nutrition-progress-item .progress-fill.carbs,
.nutrition-progress-item .progress-fill.fat {
  background: var(--color-foreground);
  box-shadow: none;
}

.nutrition-progress-item .progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  min-height: 24px;
  flex-shrink: 0;
  font-size: var(--font-size-sm);
}

.nutrition-progress-item .progress-percent {
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-base);
  color: var(--color-foreground);
  letter-spacing: -0.02em;
}

.nutrition-progress-item .progress-target {
  color: var(--color-muted-foreground);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-button {
  padding: var(--space-6);
  border: var(--border-width) solid var(--color-border);
  border-radius: 0;
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-sm);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  box-shadow: var(--shadow-md);
}

.action-button::before {
  display: none;
}

.action-button:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.action-button:active {
  transform: translate(2px, 2px);
  box-shadow: none;
}

.primary {
  background: var(--color-primary-red);
  color: #fff;
}

.secondary {
  background: var(--color-primary-blue);
  color: #fff;
}

.accent {
  background: var(--color-primary-yellow);
  color: var(--color-foreground);
}

.info {
  background: #fff;
  color: var(--color-foreground);
}

.button-icon {
  font-size: 1.75rem;
  animation: iconFloat 2.5s ease-in-out infinite;
}

.button-text {
  font-size: 15px;
}

.tips-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

.full-width-card {
  grid-column: 1 / -1;
}

.tip-item {
  background: #fff;
  border-radius: 0;
  padding: var(--space-6);
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast);
  position: relative;
}

.tip-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.tip-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--color-primary-yellow);
}

.tip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tip-icon {
  font-size: 1rem;
}

.tips-card > .bh-heading-on-color,
.tips-card > h2 {
  margin-bottom: var(--space-5);
  font-weight: var(--font-weight-black);
  text-transform: uppercase;
}

.tip-category {
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.tip-content {
  color: var(--color-muted-foreground);
  line-height: 1.65;
  margin: 0;
  font-size: var(--font-size-base);
}

.progress-container {
  display: grid;
  gap: 20px;
}

/* 转盘样式 */
.wheel-card {
  text-align: center;
}

.wheel-subtitle-title {
  margin-bottom: 8px;
  padding-bottom: 0;
  border-bottom: none;
  font-weight: var(--font-weight-black);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-size: var(--font-size-lg);
  text-align: center;
}

.wheel-subtitle {
  font-size: var(--font-size-sm);
  margin-bottom: 16px;
  text-align: center;
}

.wheel-container {
  display: flex;
  justify-content: center;
  margin-bottom: var(--space-4);
}

/* 饮水格面板：白底黑框高对比，在蓝底上始终可读 */
.water-log-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: #fff;
  border: var(--border-width-thick) solid var(--color-border);
  box-shadow: var(--shadow-md);
  min-height: 140px;
  color: var(--color-foreground);
}

.water-log-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding-bottom: var(--space-2);
  border-bottom: var(--border-width) solid var(--color-border);
}

.water-log-panel__title {
  margin: 0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-black);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-foreground);
}

.water-log-panel__link {
  border: var(--border-width) solid var(--color-border);
  background: var(--color-primary-yellow);
  color: var(--color-foreground);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: var(--space-2) var(--space-3);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.water-log-panel__link:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.water-log-panel__link:active {
  transform: translate(2px, 2px);
  box-shadow: none;
}

.water-cup-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-2);
  flex: 1;
}

.water-cup-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-height: 56px;
  background: var(--color-muted);
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast), background var(--transition-fast);
}

.water-cup-slot--filled {
  background: var(--color-primary-yellow);
  border-color: var(--color-border);
  color: var(--color-foreground);
  box-shadow: var(--shadow-md);
}

.water-cup-slot__icon {
  font-size: 1.15rem;
  line-height: 1;
  min-height: 1.15rem;
}

.water-cup-slot__num {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-black);
  color: var(--color-foreground);
}

.water-time-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.water-time-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  background: var(--color-muted);
  border: var(--border-width) solid var(--color-border);
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
  font-weight: var(--font-weight-medium);
}

.water-time-list__idx {
  font-weight: var(--font-weight-black);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: var(--font-size-xs);
  color: var(--color-foreground);
}

.water-time-list__time {
  font-weight: var(--font-weight-bold);
  font-variant-numeric: tabular-nums;
}

.water-log-panel__hint {
  margin: 0;
  font-size: var(--font-size-sm);
  line-height: 1.55;
  text-align: center;
  padding: var(--space-2) 0;
  color: var(--color-muted-foreground);
  font-weight: var(--font-weight-medium);
}

.wheel-wrapper {
  position: relative;
  width: 280px;
  height: 280px;
}

.wheel {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  position: relative;
  border: 2px solid var(--color-border);
  box-shadow: none;
  transition: transform 2.8s ease-out;
}

.wheel.spinning {
  transition: transform 2.8s ease-out;
}

.wheel-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--color-muted);
}

.wheel-icons {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  transition: transform 2.8s ease-out;
}

.wheel-icon {
  position: absolute;
  font-size: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: none;
  transform-origin: center center;
  margin-left: -22px;
  margin-top: -22px;
  width: 44px;
  height: 44px;
}

.wheel-pointer {
  position: absolute;
  top: -15px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 28px;
  color: #fff;
  z-index: 10;
}

.wheel-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80px;
  height: 80px;
  background: var(--color-foreground);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: none;
  border: 2px solid var(--color-foreground);
  z-index: 10;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.wheel-center:hover:not(.disabled) {
  transform: translate(-50%, -50%) scale(1.1);
  box-shadow: none;
}

.wheel-center:active:not(.disabled) {
  transform: translate(-50%, -50%) scale(0.95);
}

.wheel-center.disabled {
  cursor: not-allowed;
  opacity: 0.8;
}

.center-text {
  color: white;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: -0.3px;
}

.suggestion-result {
  position: relative;
  background: #fff;
  border-radius: 0;
  padding: var(--space-6);
  margin-top: var(--space-5);
  opacity: 0;
  transform: translateY(12px);
  transition: opacity var(--transition-normal), transform var(--transition-normal);
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-md);
  color: var(--color-foreground);
}

.suggestion-result.show {
  opacity: 1;
  transform: translateY(0);
  box-shadow: none;
}

.suggestion-result.show::before {
  display: none;
}

.result-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: iconFloat 2s ease-in-out infinite;
}

.result-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-black);
  color: var(--color-foreground);
  margin-bottom: 8px;
  text-transform: uppercase;
}

.result-desc {
  color: var(--color-muted-foreground);
  line-height: 1.6;
  font-size: var(--font-size-base);
}

.result-desc {
  font-size: 15px;
  color: #666;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .wheel-wrapper {
    width: 260px;
    height: 260px;
  }
  
  .wheel-icon {
    font-size: 38px;
    width: 38px;
    height: 38px;
    margin-left: -19px;
    margin-top: -19px;
  }
  
  .wheel-center {
    width: 70px;
    height: 70px;
  }
  
  .center-text {
    font-size: 14px;
  }
}

.progress-item {
  background: var(--color-muted);
  border-radius: 20px;
  padding: 24px;
  border: none;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-label {
  font-weight: 600;
  color: var(--color-foreground);
  font-size: 17px;
  letter-spacing: -0.2px;
}

.progress-percent {
  font-weight: var(--font-weight-black);
  font-size: 24px;
  color: var(--color-foreground);
  letter-spacing: -1px;
}

.progress-bar {
  height: 12px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--color-foreground);
  border-radius: 6px;
  transition: width 0.8s cubic-bezier(0.25, 0.1, 0.25, 1);
}

@media (max-width: 1024px) {
  .main-container {
    grid-template-columns: 1fr;
    padding: 0 24px 60px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .nutrition-grid {
    grid-template-columns: repeat(3, 1fr);
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
  
  .main-container {
    padding: 0 20px 40px;
  }
  
  .card {
    padding: 24px;
    border-radius: 20px;
  }
  
  .card h2 {
    font-size: 20px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .stat-card {
    padding: 20px;
  }
  
  .stat-number {
    font-size: 32px;
  }
  
  .nutrition-grid {
    grid-template-columns: 1fr;
  }
  
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .action-button {
    padding: 20px;
  }
}

@media (max-width: 480px) {
  .page-header h1 {
    font-size: 28px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .action-grid {
    grid-template-columns: 1fr;
  }
  
  .summary-stats {
    grid-template-columns: 1fr;
  }
}

/* Toast 提示样式 */
.toast-container {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: none;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 10000;
  min-width: 300px;
  animation: toastSlideIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-success {
  background: var(--color-foreground);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.toast-error {
  background: var(--color-muted-foreground);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.toast-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.toast-message {
  font-size: 15px;
  font-weight: 500;
  letter-spacing: -0.2px;
  line-height: 1.4;
}

/* Toast 动画 */
.toast-enter-active {
  animation: toastSlideIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-leave-active {
  animation: toastSlideOut 0.3s cubic-bezier(0.55, 0.055, 0.675, 0.19);
}

@keyframes toastSlideIn {
  from {
    transform: translateX(-50%) translateY(-100px);
    opacity: 0;
  }
  to {
    transform: translateX(-50%) translateY(0);
    opacity: 1;
  }
}

@keyframes toastSlideOut {
  from {
    transform: translateX(-50%) translateY(0);
    opacity: 1;
  }
  to {
    transform: translateX(-50%) translateY(-100px);
    opacity: 0;
  }
}
</style>
