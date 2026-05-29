<template>
  <div class="health-report-layout">
    <PageHeader
      title="健康报告"
      section-label="数据分析"
      :label-pulse="true"
      icon="📊"
    >
      <template #stats>
        <span v-if="profileData.healthGoal" class="stat-item">
          🎯 当前目标：{{ profileData.healthGoal }}
        </span>
        <span v-if="profileData.targetCalories" class="stat-item">
          🔥 日目标 {{ Math.round(profileData.targetCalories) }} kcal
        </span>
        <span v-else class="stat-item">请先在个人中心完善档案以生成推荐营养目标</span>
      </template>
    </PageHeader>

    <div class="content-grid">
      <!-- 推荐营养目标（来自档案保存后后端计算） -->
      <div class="card goals-card bh-card">
        <div class="card-header-section">
          <SectionLabel label="营养目标" />
          <h2>🎯 推荐营养目标</h2>
        </div>
        <div v-if="profileLoading" class="empty-state">
          <p>⏳ 正在加载档案…</p>
        </div>
        <div v-else-if="!profileData.targetCalories" class="empty-state">
          <p>尚未生成营养目标</p>
          <p class="hint">前往「个人中心」填写身高、体重、年龄等信息并保存档案后，系统将自动计算每日推荐摄入。</p>
          <button class="btn-brand link-btn" type="button" @click="router.push('/profile')">去完善档案 →</button>
        </div>
        <div v-else class="nutrition-goals">
          <div class="goal-item">
            <span class="goal-label">🔥 每日热量</span>
            <span
              class="goal-value"
              :data-count-immediate="Math.round(profileData.targetCalories || 0)"
              data-count-suffix=" kcal"
            >0</span>
          </div>
          <div class="goal-item">
            <span class="goal-label">🥩 蛋白质</span>
            <span
              class="goal-value"
              :data-count-immediate="Math.round(profileData.targetProtein || 0)"
              data-count-suffix=" g"
            >0</span>
          </div>
          <div class="goal-item">
            <span class="goal-label">🍚 碳水化合物</span>
            <span
              class="goal-value"
              :data-count-immediate="Math.round(profileData.targetCarbs || 0)"
              data-count-suffix=" g"
            >0</span>
          </div>
          <div class="goal-item">
            <span class="goal-label">🥑 脂肪</span>
            <span
              class="goal-value"
              :data-count-immediate="Math.round(profileData.targetFat || 0)"
              data-count-suffix=" g"
            >0</span>
          </div>
        </div>
      </div>

      <!-- 近 7 日饮食趋势 -->
      <div class="card report-card bh-card">
        <div class="card-header-section">
          <SectionLabel label="趋势报告" />
          <h2>📈 近 7 日健康报告</h2>
        </div>
        <div v-if="dataLoading" class="empty-state">
          <p>⏳ 正在加载饮食数据…</p>
        </div>
        <div v-else-if="healthData.length === 0" class="empty-state">
          <p>📝 过去 7 天暂无饮食记录</p>
          <p class="hint">在「饮食日记」中记录餐食后，这里会展示热量趋势与营养素构成。</p>
          <button class="btn-brand link-btn" type="button" @click="router.push('/diary')">去记录饮食 →</button>
        </div>
        <div v-else class="charts-grid">
          <HealthCharts title="🔥 热量摄入趋势" :option="caloriesChartOption" />
          <HealthCharts title="🥗 宏量营养素来源" :option="macrosChartOption" />
        </div>
        <div v-if="healthData.length && reportSummary" class="report-summary card">
          <h3>📝 本周小结</h3>
          <p class="report-summary__text">{{ typedSummary }}<span v-if="isTyping" class="report-summary__cursor">|</span></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue';
import PageHeader from '@/components/common/PageHeader.vue';
import SectionLabel from '@/components/common/SectionLabel.vue';
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

const typedSummary = ref('');
const isTyping = ref(false);

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
  const bauhausColors = ['#D02020', '#1040C0', '#F0C020', '#121212', '#525252'];

  caloriesChartOption.value = {
    color: bauhausColors,
    title: { text: '近7日热量摄入趋势', left: 'center', textStyle: { color: '#121212' } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dailyData.dates, boundaryGap: false },
    yAxis: { type: 'value', name: '千卡 (kcal)' },
    series: [{
      name: '热量',
      type: 'line',
      data: dailyData.calories,
      smooth: true,
      lineStyle: { color: '#D02020' },
      itemStyle: { color: '#D02020' },
      areaStyle: { color: 'rgba(208, 32, 32, 0.12)' },
      markPoint: {
        data: [
          { type: 'max', name: '最大值' },
          { type: 'min', name: '最小值' },
        ],
      },
    }],
  };

  macrosChartOption.value = {
    color: ['#D02020', '#1040C0', '#F0C020'],
    title: { text: '近7日宏量营养素来源', left: 'center', textStyle: { color: '#121212' } },
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
          shadowBlur: 0,
          shadowOffsetX: 0,
          shadowColor: 'transparent',
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

/** 根据近 7 日数据生成文字小结 */
const reportSummary = computed(() => {
  if (!healthData.value.length) return '';
  const daily = processHealthData(healthData.value);
  const days = daily.calories.filter((c) => c > 0).length;
  const totalCal = daily.calories.reduce((s, c) => s + c, 0);
  const avg = days ? Math.round(totalCal / days) : 0;
  const goal = profileData.value.targetCalories
    ? `推荐日摄入约 ${Math.round(profileData.value.targetCalories)} kcal`
    : '完善档案后可对比推荐目标';
  return `过去 7 天你有 ${days} 天记录了饮食，日均热量约 ${avg} kcal。${goal}。保持规律记录，趋势会更清晰。`;
});

/** 打字机效果展示报告小结 */
const typewriteSummary = async () => {
  typedSummary.value = reportSummary.value;
};

/** 营养目标数字直接展示（无 GSAP 滚动） */
const animateGoalCounts = () => {
  document.querySelectorAll('.nutrition-goals [data-count-immediate]').forEach((el) => {
    const end = Number(el.dataset.countImmediate) || 0;
    const suffix = el.dataset.countSuffix ?? '';
    el.textContent = `${Math.round(end)}${suffix}`;
  });
};

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login');
    return;
  }
  userId.value = userStore.userData.userId || userStore.userData.name || '';
  await Promise.all([loadProfile(), fetchHealthData()]);
  await nextTick();
  if (profileData.value.targetCalories) animateGoalCounts();
  if (reportSummary.value) {
    await typewriteSummary();
  }
});
</script>

<style scoped>
.health-report-layout {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: var(--space-20);
}

.page-header {
  text-align: center;
  padding: var(--space-12) var(--space-6) var(--space-8);
  background: var(--color-background);
}

.page-header h1 {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text);
  margin-bottom: var(--space-3);
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
  border-radius: 0;
  font-size: 14px;
  color: var(--color-muted-foreground);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

/* 与 PageHeader 同宽：占满路由区，左右内边距与顶栏一致 */
.content-grid {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0 var(--space-6) var(--space-12);
}

.card {
  padding: var(--space-6);
}

.card h2::before {
  content: '';
  width: 4px;
  height: 24px;
  background: var(--color-primary-red);
  border-radius: 0;
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
  background: var(--color-muted);
  border-radius: 16px;
}

.goal-label {
  font-weight: 500;
  color: var(--color-foreground);
}

.goal-value {
  font-weight: 600;
  color: var(--color-foreground);
  font-size: 17px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.report-summary {
  margin-top: var(--space-6);
  padding: var(--space-6);
}

.report-summary h3 {
  margin: 0 0 var(--space-4);
  font-size: var(--font-size-lg);
  color: var(--color-text);
}

.report-summary__text {
  margin: 0;
  line-height: 1.75;
  color: var(--color-text-secondary);
}

.report-summary__cursor {
  color: var(--color-primary);
  animation: report-cursor-blink 0.9s step-end infinite;
}

@keyframes report-cursor-blink {
  50% {
    opacity: 0;
  }
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: var(--color-muted-foreground);
  background: var(--color-muted);
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
  border: var(--border-width) solid var(--color-border);
  border-radius: 0;
  background: var(--color-primary-red);
  color: #fff;
  font-size: 15px;
  font-weight: var(--font-weight-bold);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}

.link-btn:hover {
  filter: brightness(1.06);
}

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 28px;
  }
}
</style>
