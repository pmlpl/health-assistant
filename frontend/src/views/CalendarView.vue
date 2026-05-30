<template>

  <div class="calendar-page-layout">

    <!-- 主标题合并到 section-label，避免与卡片内标题重复 -->

    <PageHeader title="饮食与健身概览" section-label="健康日历" :label-pulse="true">

      <template #icon>

        <svg class="header-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">

          <rect x="3" y="4" width="18" height="18" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>

          <line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" stroke-width="2"/>

          <line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" stroke-width="2"/>

          <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/>

        </svg>

      </template>

      <template #stats>

        <span class="stat-item">点击日期查看饮食和健身详细统计</span>

      </template>

    </PageHeader>



    <!-- 主内容：左日历 + 右详情，大屏并排、小屏堆叠 -->

    <div class="calendar-main-content">

      <div class="calendar-split-layout">

        <!-- 左侧：月历 -->

        <section class="calendar-panel calendar-panel--calendar" aria-label="月历">

          <div class="calendar-card bh-card">

            <p class="calendar-hint">选择日期以在右侧查看当日热量与营养数据</p>

            <ProfessionalCalendar

              :user-id="userStore.userData.userId"

              type="combined"

              @date-selected="navigateToDailyDetail"

            />

          </div>

        </section>



        <!-- 右侧：选中日期的统计卡片（仅热量收支 + 营养构成） -->

        <section class="calendar-panel calendar-panel--detail" aria-label="日期详情">

          <div v-if="selectedDateData" class="daily-detail-section">

            <div class="detail-header">

              <h2>{{ formatDate(selectedDateData.date) }} 详细记录</h2>

            </div>



            <div class="charts-section">

              <!-- 热量收支对比 -->

              <div class="stat-card calories-comparison">

                <h3>热量收支对比</h3>

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



              <!-- 营养构成 -->

              <div class="stat-card nutrition-chart">

                <h3>营养构成</h3>

                <div class="chart-container">

                  <div
                    class="donut-chart"
                    :class="{ 'donut-chart--empty': isDonutEmpty() }"
                    :style="getDonutChartStyle()"
                  >

                    <div class="chart-center">

                      <div class="total-calories">{{ Math.round(selectedDateData.dietTotals.calories) }}</div>

                      <div class="calories-label">总热量</div>

                      <div v-if="isDonutEmpty()" class="donut-empty-hint">暂无宏量数据</div>

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

                      <span class="legend-label">碳水</span>

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

          </div>



          <!-- 数据加载前的占位 -->

          <div v-else class="detail-placeholder bh-card">

            <p>请在左侧日历中选择日期</p>

          </div>

        </section>

      </div>

    </div>

  </div>

</template>



<script setup>

import { ref, onMounted } from 'vue';

import PageHeader from '@/components/common/PageHeader.vue';

import ProfessionalCalendar from '../components/ProfessionalCalendar.vue';

import { useUserStore } from '../stores/userStore';

import { healthApi } from '../api/healthApi';



const userStore = useUserStore();

const selectedDateData = ref(null);



// 格式化日期为「M月D日」

const formatDate = (dateString) => {

  const date = new Date(dateString);

  return `${date.getMonth() + 1}月${date.getDate()}日`;

};



// 摄入热量进度（相对推荐值 2500 kcal）

const getIntakeProgress = () => {

  const maxCalories = 2500;

  return Math.min((selectedDateData.value.dietTotals.calories / maxCalories) * 100, 100);

};



// 消耗热量进度（相对推荐值 800 kcal）

const getBurnProgress = () => {

  const maxBurn = 800;

  return Math.min((selectedDateData.value.fitnessTotals.caloriesBurned / maxBurn) * 100, 100);

};



// 净热量 = 摄入 - 消耗

const getNetCalories = () => {

  const intake = selectedDateData.value.dietTotals.calories;

  const burn = selectedDateData.value.fitnessTotals.caloriesBurned;

  return Math.round(intake - burn);

};



// 根据净热量返回平衡状态样式类

const getBalanceClass = () => {

  const net = getNetCalories();

  if (net > 500) return 'surplus';

  if (net < -300) return 'deficit';

  return 'balanced';

};



// 平衡状态对应 emoji

const getBalanceIcon = () => {

  const net = getNetCalories();

  if (net > 500) return '📈';

  if (net < -300) return '📉';

  return '⚖️';

};



// 宏量营养素按热量换算后的总和（蛋白/碳水 4 kcal/g，脂肪 9 kcal/g）

const getMacroCalorieTotal = (totals) =>

  totals.protein * 4 + totals.carbs * 4 + totals.fat * 9;



// 无宏量数据时仅显示灰色空轨

const isDonutEmpty = () => {

  if (!selectedDateData.value) return true;

  return getMacroCalorieTotal(selectedDateData.value.dietTotals) === 0;

};



// 环形图：按蛋白/碳水/脂肪热量占比绘制 Bauhaus 三色 conic-gradient

const getDonutChartStyle = () => {

  const totals = selectedDateData.value.dietTotals;

  const totalKcal = getMacroCalorieTotal(totals);



  if (totalKcal === 0) {

    return { background: 'var(--color-muted)' };

  }



  const proteinKcal = totals.protein * 4;

  const carbsKcal = totals.carbs * 4;

  const proteinEnd = (proteinKcal / totalKcal) * 100;

  const carbsEnd = proteinEnd + (carbsKcal / totalKcal) * 100;



  return {

    background: `conic-gradient(

      var(--color-protein) 0% ${proteinEnd}%,

      var(--color-carbs) ${proteinEnd}% ${carbsEnd}%,

      var(--color-fat) ${carbsEnd}% 100%

    )`,

  };

};



// 加载指定日期的汇总数据（饮食 + 健身）

const loadDailyDetail = async (date, scrollOnMobile = true) => {

  try {

    const userId = userStore.userData.userId;

    const [dietRecords, fitnessRecords] = await Promise.all([

      healthApi.getDailyDiet(userId, date),

      healthApi.getDailyFitnessRecords(userId, date),

    ]);



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



    // 小屏堆叠布局时，选中日期后滚动到详情区

    if (scrollOnMobile && window.matchMedia('(max-width: 992px)').matches) {

      setTimeout(() => {

        const detailSection = document.querySelector('.calendar-panel--detail');

        if (detailSection) {

          detailSection.scrollIntoView({ behavior: 'smooth', block: 'start' });

        }

      }, 100);

    }

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



// 日历组件选中日期回调

const navigateToDailyDetail = (date) => {

  loadDailyDetail(date);

};



// 页面初始化：默认展示当天数据，不触发滚动

const initializeTodayData = () => {

  const today = new Date();

  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

  loadDailyDetail(todayStr, false);

};



onMounted(() => {

  initializeTodayData();

});

</script>



<style scoped>

/* 页面容器：全宽，减少两侧留白 */

.calendar-page-layout {

  width: 100%;

  max-width: none;

  margin: 0;

  padding: 0;

  background: var(--color-background);

  min-height: 100vh;

}



.header-icon {

  width: 1.15em;

  height: 1.15em;

  color: var(--color-primary);

  flex-shrink: 0;

}



.stat-item {

  display: flex;

  align-items: center;

  gap: var(--space-2);

  padding: var(--space-2) var(--space-4);

  background: var(--color-muted);

  border-radius: 0;

  font-weight: 500;

}



/* 顶栏与主内容区间距 */

.calendar-main-content {

  padding: var(--space-8) var(--space-6) var(--space-12);

  width: 100%;

  max-width: 1600px;

  margin: 0 auto;

}



/* 左右分栏：日历 | 详情 */

.calendar-split-layout {

  display: grid;

  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);

  gap: var(--space-8);

  align-items: start;

}



.calendar-panel {

  min-width: 0;

}



/* 左侧日历卡片 */

.calendar-card {

  background: var(--color-card);

  border-radius: 0;

  padding: var(--space-6);

  box-shadow: var(--shadow-xl);

  border: var(--border-width-thick) solid var(--color-border);

  color: var(--color-foreground);

}



.calendar-hint {

  margin: 0 0 var(--space-4);

  font-size: 0.9375rem;

  color: var(--color-muted-foreground);

  letter-spacing: -0.01em;

}



/* 右侧详情区 */

.daily-detail-section {

  background: var(--color-card);

  border-radius: 0;

  padding: var(--space-6);

  border: var(--border-width-thick) solid var(--color-border);

  box-shadow: var(--shadow-md);

  animation: slideUp 0.4s ease-out;

}



@keyframes slideUp {

  from {

    opacity: 0;

    transform: translateY(var(--space-4));

  }

  to {

    opacity: 1;

    transform: translateY(0);

  }

}



.detail-header {

  margin-bottom: var(--space-6);

  padding-bottom: var(--space-4);

  border-bottom: var(--border-width) solid var(--color-border);

}



.detail-header h2 {

  font-size: 1.375rem;

  font-weight: 700;

  color: var(--color-foreground);

  margin: 0;

  text-transform: uppercase;

  letter-spacing: 0.02em;

}



/* 两块统计卡片上下排列 */

.charts-section {

  display: flex;

  flex-direction: column;

  gap: var(--space-6);

}



.stat-card {

  background: var(--color-card);

  border-radius: 0;

  padding: var(--space-6);

  border: var(--border-width-thick) solid var(--color-border);

  box-shadow: var(--shadow-md);

  color: var(--color-foreground);

}



.stat-card h3 {

  font-size: 1rem;

  font-weight: var(--font-weight-black);

  color: var(--color-foreground);

  margin: 0 0 var(--space-5) 0;

  text-transform: uppercase;

  letter-spacing: 0.04em;

}



.progress-container {

  display: flex;

  flex-direction: column;

  gap: var(--space-5);

}



.progress-bar-wrapper {

  display: flex;

  flex-direction: column;

  gap: var(--space-2);

}



.progress-label {

  font-size: 0.875rem;

  font-weight: 600;

  color: var(--color-muted-foreground);

}



.progress-bar {

  height: 1.5rem;

  background: var(--color-muted);

  border-radius: 0;

  overflow: hidden;

  border: var(--border-width) solid var(--color-border);

}



.progress-fill {

  height: 100%;

  border-radius: 0;

  transition: width 0.8s cubic-bezier(0.25, 0.1, 0.25, 1);

}



.intake-bar .progress-fill {

  background: var(--color-primary-red);

}



.burn-bar .progress-fill {

  background: var(--color-primary-blue);

}



.progress-value {

  font-size: 1rem;

  font-weight: 700;

  color: var(--color-foreground);

}



.net-balance {

  margin-top: var(--space-5);

  padding: var(--space-4);

  background: var(--color-muted);

  border-radius: 0;

  display: flex;

  align-items: center;

  gap: var(--space-3);

  font-size: 1rem;

  border: var(--border-width) solid var(--color-border);

}



.net-balance strong {

  font-size: 1.25rem;

  font-weight: 700;

}



.net-balance.surplus {

  background: var(--color-primary-yellow);

  color: var(--color-foreground);

}



.net-balance.deficit {

  background: var(--color-primary-blue);

  color: #fff;

}



.net-balance.balanced {

  background: var(--color-muted);

  color: var(--color-foreground);

}



.balance-icon {

  font-size: 1.5rem;

  margin-left: auto;

}



.chart-container {

  display: flex;

  align-items: center;

  gap: var(--space-6);

}



.donut-chart {

  width: 140px;

  height: 140px;

  border-radius: 50%;

  position: relative;

  flex-shrink: 0;

  border: var(--border-width) solid var(--color-border);

}



.chart-center {

  position: absolute;

  top: 50%;

  left: 50%;

  transform: translate(-50%, -50%);

  width: 96px;

  height: 96px;

  background: var(--color-card);

  border-radius: 50%;

  display: flex;

  flex-direction: column;

  align-items: center;

  justify-content: center;

  border: var(--border-width) solid var(--color-border);

}



.total-calories {

  font-size: 1.5rem;

  font-weight: 700;

  color: var(--color-foreground);

  line-height: 1;

}



.calories-label {

  font-size: 0.8125rem;

  color: var(--color-muted-foreground);

  margin-top: var(--space-1);

}



.chart-legend {

  flex: 1;

  display: flex;

  flex-direction: column;

  gap: var(--space-4);

  min-width: 0;

}



.legend-item {

  display: flex;

  align-items: center;

  gap: var(--space-3);

}



.legend-color {

  width: 1.25rem;

  height: 1.25rem;

  border-radius: 0;

  flex-shrink: 0;

  border: var(--border-width) solid var(--color-border);

}



.legend-item.protein .legend-color {

  background: var(--color-protein);

}



.legend-item.carbs .legend-color {

  background: var(--color-carbs);

}



.legend-item.fat .legend-color {

  background: var(--color-fat);

}



.donut-chart--empty {

  background: var(--color-muted);

}



.donut-empty-hint {

  margin-top: var(--space-1);

  font-size: 0.6875rem;

  color: var(--color-muted-foreground);

  text-align: center;

  line-height: 1.2;

}



.legend-label {

  font-size: 0.875rem;

  color: var(--color-muted-foreground);

  flex: 1;

}



.legend-value {

  font-size: 0.9375rem;

  font-weight: 700;

  color: var(--color-foreground);

  min-width: 4rem;

  text-align: right;

}



/* 未选日期占位 */

.detail-placeholder {

  padding: var(--space-12) var(--space-6);

  text-align: center;

  color: var(--color-muted-foreground);

  border: var(--border-width-thick) solid var(--color-border);

  background: var(--color-card);

}



.detail-placeholder p {

  margin: 0;

  font-size: 1rem;

}



/* 小屏：日历在上、详情在下 */

@media (max-width: 992px) {

  .calendar-main-content {

    padding: var(--space-6) var(--space-4) var(--space-10);

  }



  .calendar-split-layout {

    grid-template-columns: 1fr;

    gap: var(--space-6);

  }



  .calendar-card,

  .daily-detail-section {

    padding: var(--space-5);

  }



  .chart-container {

    flex-direction: column;

    align-items: stretch;

  }



  .donut-chart {

    margin: 0 auto;

  }

}



@media (max-width: 480px) {

  .detail-header h2 {

    font-size: 1.125rem;

  }



  .donut-chart {

    width: 120px;

    height: 120px;

  }



  .chart-center {

    width: 80px;

    height: 80px;

  }



  .total-calories {

    font-size: 1.25rem;

  }

}

</style>


