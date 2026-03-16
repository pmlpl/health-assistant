<template>
  <div class="dashboard-layout">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>🏠 主页</h1>
      <div class="stats-bar">
        <span class="stat-item">{{ greeting }}, {{ userStore.userData.userId }}！</span>
        <span v-if="userStore.userData.goal" class="stat-item">
          🎯 目标: {{ userStore.userData.goal }}
        </span>
        <span v-else class="stat-item">🌟 开始您的健康之旅吧！</span>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-container">
      <!-- 合并卡片：数据概览 + 快捷操作 -->
      <div class="card overview-actions-card">
        <div class="card-header-section">
          <h2>📊 今日概览</h2>
        </div>
        <div class="stats-grid">
          <div class="stat-card" title="今日已记录的饮食次数">
            <div class="stat-icon">🍽️</div>
            <div class="stat-content">
              <div class="stat-number">{{ todayRecordsCount }}</div>
              <div class="stat-label">饮食记录</div>
            </div>
          </div>
          <div class="stat-card" title="今日总热量摄入">
            <div class="stat-icon">🔥</div>
            <div class="stat-content">
              <div class="stat-number">{{ todayCalories }}</div>
              <div class="stat-label">摄入热量 (kcal)</div>
            </div>
          </div>
          <div class="stat-card" title="今日饮水量">
            <div class="stat-icon">💧</div>
            <div class="stat-content">
              <div class="stat-number">{{ dailyWaterIntake }}</div>
              <div class="stat-label">饮水杯数</div>
            </div>
          </div>
          <div class="stat-card" title="今日运动时长">
            <div class="stat-icon">🏃</div>
            <div class="stat-content">
              <div class="stat-number">{{ todayExerciseMinutes }}</div>
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
          <button class="btn action-button secondary" @click="$router.push('/ai-consult')">
            <span class="button-icon">🤖</span>
            <span class="button-text">AI 咨询</span>
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

      <!-- 合并卡片：喝水打卡 + 健康转盘 -->
      <div class="card water-wheel-card">
        <div class="card-header-section">
          <h2>💧 喝水打卡</h2>
        </div>
        <div class="water-intake-container">
          <div class="water-progress">
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
          
          <button class="btn water-btn" @click="addWaterIntake" :disabled="isWaterTargetAchieved">
            <span class="btn-icon">💧</span>
            <span class="btn-text">{{ isWaterTargetAchieved ? '已达标，明日再接再厉！' : '点击打卡喝水' }}</span>
          </button>
        </div>
        <div class="action-section-divider"></div>
        <div class="wheel-section">
          <h2 class="wheel-subtitle-title">🎡 每日健康转盘</h2>
          <p class="wheel-subtitle">不知道今天该做什么？转转看！</p>
          <div class="wheel-container">
            <div class="wheel-wrapper">
              <div class="wheel" :style="wheelStyle" :class="{ 'spinning': isSpinning }">
                <div class="wheel-bg"></div>
                <div class="wheel-icons">
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
              <div class="wheel-pointer">▼</div>
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
      </div>

      <!-- 喝水记录详情弹窗 -->
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

      <!-- 营养摄入详情 -->
      <div class="card nutrition-card">
        <h2>🥗 今日营养摄入</h2>
        <div class="nutrition-progress-container">
          <div class="nutrition-progress-item">
            <div class="progress-header">
              <span class="progress-icon protein">🥩</span>
              <span class="progress-label">蛋白质</span>
              <span class="progress-value">{{ todayProtein }}g</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill protein" :style="`width: ${getNutritionProgress(todayProtein, 50)}%`"></div>
            </div>
            <div class="progress-info">
              <span class="progress-percent">{{ getNutritionProgress(todayProtein, 50) }}%</span>
              <span class="progress-target">目标: 50g</span>
            </div>
          </div>
          
          <div class="nutrition-progress-item">
            <div class="progress-header">
              <span class="progress-icon carbs">🍚</span>
              <span class="progress-label">碳水化合物</span>
              <span class="progress-value">{{ todayCarbs }}g</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill carbs" :style="`width: ${getNutritionProgress(todayCarbs, 150)}%`"></div>
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
              <div class="progress-fill fat" :style="`width: ${getNutritionProgress(todayFat, 60)}%`"></div>
            </div>
            <div class="progress-info">
              <span class="progress-percent">{{ getNutritionProgress(todayFat, 60) }}%</span>
              <span class="progress-target">目标: 60g</span>
            </div>
          </div>
        </div>
      </div>

      

      <!-- 健康贴士 -->
      <div class="card tips-card full-width-card">
        <h2>💡 健康小贴士</h2>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useUserStore } from '../stores/userStore';
import { healthApi } from '../api/healthApi';

const userStore = useUserStore();

// 数据状态
const todayRecordsCount = ref(0);
const todayCalories = ref(0);
const todayProtein = ref(0);
const todayCarbs = ref(0);
const todayFat = ref(0);
const todayExerciseMinutes = ref(0);
const healthTips = ref([]);

// 喝水打卡相关状态
const dailyWaterIntake = ref(0); // 当前每日喝水杯数
const targetWaterIntake = ref(8); // 目标喝水杯数
const waterLog = ref([]); // 喝水记录
const showWaterLogDetail = ref(false); // 是否显示喝水记录详情弹窗
const lastWaterTime = ref(''); // 最近一次喝水时间

// 转盘相关状态
const wheelItems = ref([
  { emoji: '🏃', title: '去运动', description: '今天去跑步或健身30分钟，让身体充满活力！', color: '#FF6B6B' },
  { emoji: '🥗', title: '吃健康餐', description: '做一份营养均衡的沙拉，多吃蔬菜少吃油腻！', color: '#4ECDC4' },
  { emoji: '💧', title: '多喝水', description: '每小时喝一杯水，保持身体水分充足！', color: '#45B7D1' },
  { emoji: '😴', title: '早睡觉', description: '今晚11点前入睡，保证8小时优质睡眠！', color: '#96CEB4' },
  { emoji: '🧘', title: '做冥想', description: '花10分钟做深呼吸冥想，放松身心！', color: '#FFEAA7' },
  { emoji: '🍎', title: '吃水果', description: '今天吃2-3种不同颜色的水果补充维生素！', color: '#DDA0DD' },
  { emoji: '🚶', title: '多走路', description: '今天走满10000步，少坐电梯多走楼梯！', color: '#98D8C8' },
  { emoji: '🥛', title: '喝牛奶', description: '喝一杯牛奶或酸奶，补充钙质和蛋白质！', color: '#F7DC6F' }
]);
const wheelRotation = ref(0);
const isSpinning = ref(false);
const selectedSuggestion = ref(null);
const showSuggestion = ref(false);

// 转盘样式计算
const wheelStyle = computed(() => ({
  transform: `rotate(${wheelRotation.value}deg)`
}));

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

// 转动转盘
const spinWheel = () => {
  if (isSpinning.value) return;

  isSpinning.value = true;
  showSuggestion.value = false;

  // 随机选择结果
  const randomIndex = Math.floor(Math.random() * wheelItems.value.length);
  const anglePerSegment = 360 / wheelItems.value.length;

  // 计算目标角度
  // 当前角度
  const currentRotation = wheelRotation.value % 360;
  // 目标扇形的中间角度（每个扇形45度，中间是22.5度偏移）
  const targetAngle = randomIndex * anglePerSegment + 22.5;
  // 至少转5圈
  const extraRotation = 360 * 5;

  // 计算需要旋转的角度
  // 转盘顺时针旋转，需要让目标扇形的中间对准指针（12点钟方向，即-90度或270度）
  const rotationNeeded = extraRotation + (360 - targetAngle) - currentRotation + 22.5;

  // 累加旋转角度
  wheelRotation.value += rotationNeeded;

  // 动画结束后显示结果
  setTimeout(() => {
    selectedSuggestion.value = wheelItems.value[randomIndex];
    showSuggestion.value = true;
    isSpinning.value = false;
  }, 3000);
};

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
    
    // 显示提示信息
    alert(`第${dailyWaterIntake.value}次喝水打卡成功！💧 ${dailyWaterIntake.value >= targetWaterIntake.value ? '🎉 恭喜达成今日饮水目标！' : `距离目标还有${targetWaterIntake.value - dailyWaterIntake.value}次`}`);
  } catch (err) {
    console.error('喝水打卡失败:', err);
    alert('喝水打卡失败，请重试。');
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


onMounted(() => {
  if (userStore.userData?.userId) {
    loadTodayData();
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
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
}

.dashboard-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0;
  background: #ffffff;
  min-height: 100vh;
  position: relative;
}

.dashboard-layout::before {
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
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
  padding: 0 32px 48px;
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
}

.card {
  background: #ffffff;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 0;
  border: none;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  transition: all 0.5s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
  overflow: hidden;
}

/* 合并卡片特殊样式 */
.overview-actions-card,
.water-wheel-card {
  padding: 32px 24px;
}

.card-header-section {
  margin-bottom: 20px;
}

.action-section-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 0, 0, 0.08), transparent);
  margin: 24px 0;
}

.wheel-section {
  margin-top: 8px;
}

/* 营养卡片特殊设置 */
.nutrition-card {
  padding: 32px 24px;
  min-height: 320px;
}

.card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.04),
    0 24px 64px rgba(0, 0, 0, 0.08);
}

.card h2 {
  color: #1d1d1f;
  margin-bottom: 20px;
  padding-bottom: 0;
  border-bottom: none;
  font-weight: 600;
  letter-spacing: -0.3px;
  font-size: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-card {
  background: #f5f5f7;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  display: none;
}

.stat-card:hover {
  transform: translateY(-3px);
  background: #ebebf0;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  font-size: 1.1rem;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border-radius: 12px;
  color: white;
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.25);
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
  color: #1d1d1f;
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
  background: linear-gradient(135deg, #f5f5f7 0%, #ebebf0 100%);
  border-radius: 20px;
  padding: 24px;
  border: none;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-label {
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.2px;
  font-size: 17px;
}

.progress-percent {
  font-weight: 700;
  font-size: 28px;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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
  background: linear-gradient(90deg, #007aff 0%, #5856d6 50%, #af52de 100%);
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
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
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
  background: #007aff;
  color: white;
  box-shadow: 0 4px 20px rgba(0, 122, 255, 0.3);
}

.water-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.6s ease;
}

.water-btn:hover:not(:disabled)::before {
  left: 100%;
}

.water-btn:hover:not(:disabled) {
  background: #0066d6;
  transform: scale(1.02);
  box-shadow: 0 8px 32px rgba(0, 122, 255, 0.4);
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
  background: #f5f5f7;
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
  color: #1d1d1f;
  font-size: 15px;
  letter-spacing: -0.1px;
  margin-left: 8px;
}

.last-time {
  color: #007aff;
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
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
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
  box-shadow: 
    0 24px 80px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(0, 0, 0, 0.02);
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
  color: #1d1d1f;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.close-button {
  background: #f5f5f7;
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
  color: #1d1d1f;
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
  background: #f5f5f7;
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
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border-radius: 50%;
  color: white;
  font-size: 0.9rem;
  box-shadow: 0 4px 16px rgba(0, 122, 255, 0.3);
}

.timeline-content {
  flex: 1;
}

.timeline-time {
  font-size: 20px;
  color: #1d1d1f;
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
  color: #1d1d1f;
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
  background: #f5f5f7;
  border-radius: 16px;
  border: none;
}

.stat-icon {
  font-size: 1.25rem;
  color: #007aff;
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
  color: #1d1d1f;
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
  background: #007aff;
  color: white;
  padding: 14px 28px;
  border-radius: 980px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
  box-shadow: 0 4px 20px rgba(0, 122, 255, 0.3);
  letter-spacing: -0.2px;
}

.drink-now-btn:hover {
  background: #0066d6;
  transform: scale(1.02);
  box-shadow: 0 8px 32px rgba(0, 122, 255, 0.4);
}

/* 营养摄入进度条样式 */
.nutrition-progress-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.nutrition-progress-item {
  background: #f8f9ff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(0, 122, 255, 0.1);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.nutrition-progress-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.1);
  border-color: rgba(0, 122, 255, 0.2);
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.progress-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 1.25rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.progress-icon.protein {
  background: linear-gradient(135deg, #ff3b30 0%, #ff6b6b 100%);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
  color: white;
}

.progress-icon.carbs {
  background: linear-gradient(135deg, #ff9500 0%, #ffcc00 100%);
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3);
  color: white;
}

.progress-icon.fat {
  background: linear-gradient(135deg, #34c759 0%, #30d158 100%);
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
  color: white;
}

.progress-label {
  flex: 1;
  font-weight: 600;
  color: #1d1d1f;
  font-size: 16px;
  letter-spacing: -0.2px;
}

.progress-value {
  font-weight: 700;
  font-size: 18px;
  color: #1d1d1f;
  background: linear-gradient(135deg, #1d1d1f 0%, #424245 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  flex-shrink: 0;
  min-width: 60px;
  text-align: right;
}

.progress-bar {
  height: 12px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 12px;
  position: relative;
}

.progress-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 1s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-fill.protein {
  background: linear-gradient(90deg, #ff3b30, #ff6b6b);
  box-shadow: 0 0 12px rgba(255, 59, 48, 0.4);
}

.progress-fill.carbs {
  background: linear-gradient(90deg, #ff9500, #ffcc00);
  box-shadow: 0 0 12px rgba(255, 149, 0, 0.4);
}

.progress-fill.fat {
  background: linear-gradient(90deg, #34c759, #30d158);
  box-shadow: 0 0 12px rgba(52, 199, 89, 0.4);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.progress-percent {
  font-weight: 600;
  color: #1d1d1f;
}

.progress-target {
  color: #86868b;
  font-weight: 500;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-button {
  padding: 24px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  letter-spacing: -0.2px;
  position: relative;
  overflow: hidden;
}

.action-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.action-button:hover::before {
  opacity: 1;
}

.action-button:hover {
  transform: translateY(-4px) scale(1.02);
}

.action-button:active {
  transform: translateY(-2px) scale(0.98);
}

.primary {
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(0, 122, 255, 0.25);
}

.primary:hover {
  box-shadow: 0 12px 40px rgba(0, 122, 255, 0.35);
}

.secondary {
  background: linear-gradient(135deg, #34c759 0%, #30d158 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(52, 199, 89, 0.25);
}

.secondary:hover {
  box-shadow: 0 12px 40px rgba(52, 199, 89, 0.35);
}

.accent {
  background: linear-gradient(135deg, #ff9500 0%, #ff6b00 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(255, 149, 0, 0.25);
}

.accent:hover {
  box-shadow: 0 12px 40px rgba(255, 149, 0, 0.35);
}

.info {
  background: linear-gradient(135deg, #af52de 0%, #5856d6 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(175, 82, 222, 0.25);
}

.info:hover {
  box-shadow: 0 12px 40px rgba(175, 82, 222, 0.35);
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
  background: #f5f5f7;
  border-radius: 20px;
  padding: 24px;
  border-left: none;
  border-image: none;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  box-shadow: none;
  position: relative;
  overflow: hidden;
}

.tip-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #007aff, #af52de);
  border-radius: 2px;
}

.tip-item:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  transform: translateX(4px);
  background: #ebebf0;
}

.tip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tip-category {
  font-weight: 600;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-size: 13px;
  letter-spacing: -0.1px;
  text-transform: uppercase;
}

.tip-icon {
  font-size: 1rem;
  animation: iconFloat 2s ease-in-out infinite;
}

.tip-content {
  color: #424245;
  line-height: 1.6;
  margin: 0;
  font-size: 15px;
  letter-spacing: -0.1px;
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
  color: #1d1d1f;
  margin-bottom: 8px;
  padding-bottom: 0;
  border-bottom: none;
  font-weight: 600;
  letter-spacing: -0.3px;
  font-size: 18px;
  text-align: center;
}

.wheel-subtitle {
  color: #86868b;
  font-size: 13px;
  margin-bottom: 16px;
  text-align: center;
}

.wheel-container {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
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
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.15),
    inset 0 0 0 8px rgba(255, 255, 255, 0.1);
  transition: transform 3s cubic-bezier(0.17, 0.67, 0.12, 0.99);
}

.wheel.spinning {
  transition: transform 3s cubic-bezier(0.17, 0.67, 0.12, 0.99);
}

.wheel-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: conic-gradient(
    from -22.5deg,
    #FF6B6B 0deg 45deg,
    #4ECDC4 45deg 90deg,
    #45B7D1 90deg 135deg,
    #96CEB4 135deg 180deg,
    #FFEAA7 180deg 225deg,
    #DDA0DD 225deg 270deg,
    #98D8C8 270deg 315deg,
    #F7DC6F 315deg 360deg
  );
}

.wheel-icons {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
}

.wheel-icon {
  position: absolute;
  font-size: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.4));
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
  color: #ff3b30;
  z-index: 10;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
  animation: bounce 1s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(-5px); }
}

.wheel-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 
    0 4px 20px rgba(0, 122, 255, 0.4),
    inset 0 0 0 4px rgba(255, 255, 255, 0.2);
  z-index: 10;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.wheel-center:hover:not(.disabled) {
  transform: translate(-50%, -50%) scale(1.1);
  box-shadow: 
    0 6px 30px rgba(0, 122, 255, 0.5),
    inset 0 0 0 4px rgba(255, 255, 255, 0.3);
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
  background: linear-gradient(135deg, #f5f5f7 0%, #ffffff 100%);
  border-radius: 20px;
  padding: 24px;
  margin-top: 20px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.5s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.suggestion-result.show {
  opacity: 1;
  transform: translateY(0);
}

.result-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: iconFloat 2s ease-in-out infinite;
}

.result-title {
  font-size: 20px;
  font-weight: 700;
  color: #1d1d1f;
  margin-bottom: 8px;
  letter-spacing: -0.3px;
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
  background: #f5f5f7;
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
  color: #1d1d1f;
  font-size: 17px;
  letter-spacing: -0.2px;
}

.progress-percent {
  font-weight: 700;
  font-size: 24px;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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
  background: linear-gradient(90deg, #007aff 0%, #5856d6 50%, #af52de 100%);
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
</style>
