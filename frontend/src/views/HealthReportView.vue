<template>
  <div class="health-report-layout">
    <div class="page-header">
      <h1>📊 健康报告</h1>
      <div class="stats-bar">
        <span v-if="profileData.healthGoal" class="stat-item">
          🎯 当前目标：{{ profileData.healthGoal }}
        </span>
        <span v-if="profileData.targetCalories" class="stat-item">
          🔥 日目标 {{ Math.round(profileData.targetCalories) }} kcal
        </span>
        <span v-else class="stat-item">请先在个人中心完善档案以生成推荐营养目标</span>
      </div>
    </div>

    <div class="content-grid">
      <!-- 推荐营养目标（来自档案保存后后端计算） -->
      <div class="card goals-card">
        <h2>🎯 推荐营养目标</h2>
        <div v-if="profileLoading" class="empty-state">
          <p>⏳ 正在加载档案…</p>
        </div>
        <div v-else-if="!profileData.targetCalories" class="empty-state">
          <p>尚未生成营养目标</p>
          <p class="hint">前往「个人中心」填写身高、体重、年龄等信息并保存档案后，系统将自动计算每日推荐摄入。</p>
          <button class="link-btn" type="button" @click="router.push('/profile')">去完善档案 →</button>
        </div>
        <div v-else class="nutrition-goals">
          <div class="goal-item">
            <span class="goal-label">🔥 每日热量</span>
            <span class="goal-value">{{ Math.round(profileData.targetCalories) }} kcal</span>
          </div>
          <div class="goal-item">
            <span class="goal-label">🥩 蛋白质</span>
            <span class="goal-value">{{ Math.round(profileData.targetProtein) }} g</span>
          </div>
          <div class="goal-item">
            <span class="goal-label">🍚 碳水化合物</span>
            <span class="goal-value">{{ Math.round(profileData.targetCarbs) }} g</span>
          </div>
          <div class="goal-item">
            <span class="goal-label">🥑 脂肪</span>
            <span class="goal-value">{{ Math.round(profileData.targetFat) }} g</span>
          </div>
        </div>
      </div>

      <!-- 近 7 日饮食趋势 -->
      <div class="card report-card">
        <h2>📈 近 7 日健康报告</h2>
        <div v-if="dataLoading" class="empty-state">
          <p>⏳ 正在加载饮食数据…</p>
        </div>
        <div v-else-if="healthData.length === 0" class="empty-state">
          <p>📝 过去 7 天暂无饮食记录</p>
          <p class="hint">在「饮食日记」中记录餐食后，这里会展示热量趋势与营养素构成。</p>
          <button class="link-btn" type="button" @click="router.push('/diary')">去记录饮食 →</button>
        </div>
        <div v-else class="charts-grid">
          <HealthCharts title="🔥 热量摄入趋势" :option="caloriesChartOption" />
          <HealthCharts title="🥗 宏量营养素来源" :option="macrosChartOption" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { healthApi, apiClient } from '../api/healthApi';
import { useUserStore } from '../stores/userStore';
import HealthCharts from '../components/dashboard/HealthCharts.vue';

const router = useRouter();
const userStore = useUserStore();

const userId = ref('');
const profileLoading = ref(true);
const dataLoading = ref(true);
const healthData = ref([]);
const caloriesChartOption = ref({});
const macrosChartOption = ref({});

const profileData = ref({
  healthGoal: '',
  targetCalories: null,
  targetProtein: null,
  targetCarbs: null,
  targetFat: null,
});

/** 加载用户档案中的推荐营养目标 */
const loadProfile = async () => {
  profileLoading.value = true;
  try {
    const profile = await healthApi.getUserProfile(userId.value);
    if (profile) {
      profileData.value = {
        healthGoal: profile.healthGoal || '',
        targetCalories: profile.targetCalories ?? null,
        targetProtein: profile.targetProtein ?? null,
        targetCarbs: profile.targetCarbs ?? null,
        targetFat: profile.targetFat ?? null,
      };
    }
  } catch (e) {
    console.error('加载档案失败:', e);
  } finally {
    profileLoading.value = false;
  }
};

/** 拉取近 7 日饮食记录并刷新图表 */
const fetchHealthData = async () => {
  dataLoading.value = true;
  const endDate = new Date();
  const startDate = new Date();
  startDate.setDate(endDate.getDate() - 6);
  const formatDate = (date) => date.toISOString().split('T')[0];

  try {
    const response = await apiClient.get(`/diet/range/${userId.value}`, {
      params: {
        startDate: formatDate(startDate),
        endDate: formatDate(endDate),
      },
    });
    healthData.value = response.data || [];
    updateCharts();
  } catch (error) {
    console.error('获取健康数据失败:', error);
    healthData.value = [];
  } finally {
    dataLoading.value = false;
  }
};

const updateCharts = () => {
  const dailyData = processHealthData(healthData.value);

  caloriesChartOption.value = {
    title: { text: '近7日热量摄入趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dailyData.dates, boundaryGap: false },
    yAxis: { type: 'value', name: '千卡 (kcal)' },
    series: [{
      name: '热量',
      type: 'line',
      data: dailyData.calories,
      smooth: true,
      areaStyle: {},
      markPoint: {
        data: [
          { type: 'max', name: '最大值' },
          { type: 'min', name: '最小值' },
        ],
      },
    }],
  };

  macrosChartOption.value = {
    title: { text: '近7日宏量营养素来源', left: 'center' },
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c}g ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      name: '营养素',
      type: 'pie',
      radius: '50%',
      data: [
        { value: dailyData.totalProtein, name: '蛋白质' },
        { value: dailyData.totalCarbs, name: '碳水化合物' },
        { value: dailyData.totalFat, name: '脂肪' },
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)',
        },
      },
    }],
  };
};

const processHealthData = (records) => {
  const dailyMap = new Map();
  const today = new Date();

  for (let i = 6; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    const dateString = d.toISOString().split('T')[0];
    dailyMap.set(dateString, { calories: 0, protein: 0, carbs: 0, fat: 0 });
  }

  records.forEach((record) => {
    const dateString = record.recordedAt.split('T')[0];
    if (dailyMap.has(dateString)) {
      const day = dailyMap.get(dateString);
      day.calories += record.calories || 0;
      day.protein += record.protein || 0;
      day.carbs += record.carbs || 0;
      day.fat += record.fat || 0;
    }
  });

  const dates = Array.from(dailyMap.keys());
  const calories = dates.map((date) => Math.round(dailyMap.get(date).calories));
  const totalProtein = Math.round(
    Array.from(dailyMap.values()).reduce((sum, day) => sum + day.protein, 0)
  );
  const totalCarbs = Math.round(
    Array.from(dailyMap.values()).reduce((sum, day) => sum + day.carbs, 0)
  );
  const totalFat = Math.round(
    Array.from(dailyMap.values()).reduce((sum, day) => sum + day.fat, 0)
  );

  return { dates, calories, totalProtein, totalCarbs, totalFat };
};

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login');
    return;
  }
  userId.value = userStore.userData.userId || userStore.userData.name || '';
  await Promise.all([loadProfile(), fetchHealthData()]);
});
</script>

<style scoped>
.health-report-layout {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 80px;
}

.page-header {
  text-align: center;
  padding: 48px 24px 32px;
  background: linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
}

.page-header h1 {
  font-size: 36px;
  font-weight: 700;
  color: #1d1d1f;
  margin-bottom: 12px;
}

.stats-bar {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.stat-item {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 980px;
  font-size: 14px;
  color: #86868b;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.content-grid {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px 48px;
}

.card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02), 0 8px 32px rgba(0, 0, 0, 0.04);
}

.card h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card h2::before {
  content: '';
  width: 4px;
  height: 24px;
  background: linear-gradient(135deg, #007aff, #5856d6);
  border-radius: 2px;
}

.nutrition-goals {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.goal-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f5f5f7;
  border-radius: 16px;
}

.goal-label {
  font-weight: 500;
  color: #1d1d1f;
}

.goal-value {
  font-weight: 600;
  color: #007aff;
  font-size: 17px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: #86868b;
  background: #f5f5f7;
  border-radius: 16px;
}

.empty-state .hint {
  margin-top: 8px;
  font-size: 14px;
  max-width: 420px;
  margin-left: auto;
  margin-right: auto;
}

.link-btn {
  margin-top: 16px;
  padding: 10px 20px;
  border: none;
  border-radius: 980px;
  background: #007aff;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}

.link-btn:hover {
  background: #0066d6;
}

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 28px;
  }
}
</style>
