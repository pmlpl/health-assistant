<template>
  <div class="professional-calendar">
    <div class="calendar-header">
      <button @click="previousMonth" class="nav-btn">←</button>
      <h2 class="month-title">{{ currentYear }}年{{ currentMonth }}月 {{ type === 'fitness' ? '健身' : type === 'diet' ? '饮食' : '健康' }}日历</h2>
      <button @click="nextMonth" class="nav-btn">→</button>
    </div>

    <div class="calendar-wrapper">
      <div class="weekdays-header">
        <div class="weekday" v-for="day in weekdays" :key="day">{{ day }}</div>
      </div>
      
      <div class="calendar-grid">
        <div 
          v-for="(day, index) in calendarDays" 
          :key="index"
          class="calendar-cell"
          :class="{
            'prev-month': !day.isCurrentMonth && day.date < `${currentYear}-${String(currentMonth).padStart(2, '0')}-01`,
            'next-month': !day.isCurrentMonth && day.date >= `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-01`,
            'current-month': day.isCurrentMonth,
            'today': day.isToday,
            'has-data': day.hasData,
            'selected': day.isSelected
          }"
          @click="selectDate(day)"
        >
          <div class="day-number">{{ day.day }}</div>
          <div v-if="type === 'fitness' ? day.caloriesBurned > 0 : day.calories > 0" class="calories-badge">
            {{ Math.round(type === 'fitness' ? day.caloriesBurned : day.calories) }}kcal
          </div>
        </div>
      </div>
    </div>

    <!-- 日期详情弹窗（仅在 type 不为 combined 时显示） -->
    <div v-if="selectedDateData && props.type !== 'combined'" class="modal-overlay" @click="closeDetail">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ formatDate(selectedDate) }} 的{{ type === 'fitness' ? '健身' : '饮食' }}记录</h3>
          <button @click="closeDetail" class="close-button">×</button>
        </div>
        
        <div class="modal-body">
          <div v-if="selectedDateData.records.length > 0" class="records-container">
            <div 
              v-for="(record, index) in selectedDateData.records" 
              :key="index"
              class="record-card"
            >
              <div class="record-header">
                <span v-if="type !== 'fitness'" class="meal-tag" :class="getMealTypeClass(record.mealType)">
                  {{ getMealTypeName(record.mealType) }}
                </span>
                <span class="record-time">{{ formatTime(record.recordedAt || record.recordTime) }}</span>
              </div>
              <div class="record-content">
                <p v-if="type !== 'fitness'" class="food-description">{{ record.foodDescription }}</p>
                <p v-else class="workout-name"><strong>{{ record.workoutName }}</strong></p>
                <div class="nutrition-tags">
                  <span v-if="type === 'fitness'" class="nutrition-tag calories">
                    🔥 {{ Math.round(record.caloriesBurned || 0) }}kcal 消耗
                  </span>
                  <span v-else>
                    <span v-if="record.calories" class="nutrition-tag calories">
                      🔥 {{ Math.round(record.calories) }}kcal
                    </span>
                    <span v-if="record.protein" class="nutrition-tag protein">
                      🥩 {{ Math.round(record.protein * 10) / 10 }}g蛋白质
                    </span>
                    <span v-if="record.carbs" class="nutrition-tag carbs">
                      🍞 {{ Math.round(record.carbs * 10) / 10 }}g碳水
                    </span>
                    <span v-if="record.fat" class="nutrition-tag fat">
                      🧈 {{ Math.round(record.fat * 10) / 10 }}g脂肪
                    </span>
                  </span>
                </div>
              </div>
            </div>
            
            <div class="daily-summary">
              <h4>当日总计</h4>
              <div class="summary-grid">
                <div v-if="type === 'fitness'" class="summary-item">
                  <span class="label">总消耗</span>
                  <span class="value">{{ Math.round(selectedDateData.totals.caloriesBurned || 0) }} kcal</span>
                </div>
                <template v-else>
                  <div class="summary-item">
                    <span class="label">总热量</span>
                    <span class="value">{{ Math.round(selectedDateData.totals.calories) }} kcal</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">蛋白质</span>
                    <span class="value">{{ Math.round(selectedDateData.totals.protein * 10) / 10 }} g</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">碳水化合物</span>
                    <span class="value">{{ Math.round(selectedDateData.totals.carbs * 10) / 10 }} g</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">脂肪</span>
                    <span class="value">{{ Math.round(selectedDateData.totals.fat * 10) / 10 }} g</span>
                  </div>
                </template>
              </div>
            </div>
          </div>
          
          <div v-else class="no-data">
            <div class="no-data-icon">{{ type === 'fitness' ? '💪' : '🍽️' }}</div>
            <p>这一天没有{{ type === 'fitness' ? '健身' : '饮食' }}记录</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { healthApi } from '../api/healthApi'
import { useUserStore } from '../stores/userStore'

const props = defineProps({
  userId: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'diet', // 'diet'、'fitness' 或 'combined'
    validator: (value) => ['diet', 'fitness', 'combined'].includes(value)
  }
})

const emit = defineEmits(['date-selected'])

const userStore = useUserStore()
const currentDate = new Date()
const currentYear = ref(currentDate.getFullYear())
const currentMonth = ref(currentDate.getMonth() + 1)  // getMonth()返回 0-11，所以 +1
const selectedDate = ref(null)
const selectedDateData = ref(null)
const monthlyData = ref([])

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const calendarDays = computed(() => {
  const days = []
  const year = currentYear.value
  const month = currentMonth.value  // 这里的 month 是 1-12（例如 3 表示 3 月）
  
  // 获取当月第一天是星期几
  const firstDay = new Date(year, month - 1, 1)  // month-1 转换为 0-11
  const firstDayOfWeek = firstDay.getDay()
  
  // 获取当月总天数
  const daysInMonth = new Date(year, month, 0).getDate()  // month 表示下个月，0 号是上个月最后一天
  
  // 获取上个月总天数
  const daysInPrevMonth = new Date(year, month - 1, 0).getDate()  // month-1 月的 0 号是上上个月最后一天
  
  let gridIndex = 0
  
  // 填充上个月的日期（只有当 firstDayOfWeek > 0 时才需要）
  if (firstDayOfWeek > 0) {
    for (let i = 0; i < firstDayOfWeek; i++) {
      const day = daysInPrevMonth - firstDayOfWeek + i + 1
      const dateObj = new Date(year, month - 2, day)
      const dateString = dateObj.toISOString().split('T')[0]
      
      days.push({
        date: dateString,
        day: day,
        isCurrentMonth: false,
        isToday: dateString === new Date().toISOString().split('T')[0],
        isSelected: selectedDate.value === dateString,
        hasData: hasDataForDate(dateString),
        calories: getCaloriesForDate(dateString),
        caloriesBurned: props.type === 'fitness' ? getCaloriesForDate(dateString) : 0
      })
    }
  } else {
  }
  
  // 填充当月的日期
  for (let day = 1; day <= daysInMonth; day++) {
    // 使用 UTC 时间避免时区问题
    const dateObj = new Date(Date.UTC(year, month - 1, day))
    const dateString = dateObj.toISOString().split('T')[0]
    const isToday = dateString === new Date().toISOString().split('T')[0]
    
    days.push({
      date: dateString,
      day: day,
      isCurrentMonth: true,
      isToday: isToday,
      isSelected: selectedDate.value === dateString,
      hasData: hasDataForDate(dateString),
      calories: getCaloriesForDate(dateString),
      caloriesBurned: props.type === 'fitness' ? getCaloriesForDate(dateString) : 0
    })
  }
  
  // 填充下个月的日期
  const remainingDays = 42 - days.length
  for (let day = 1; day <= remainingDays; day++) {
    const dateObj = new Date(year, month, day)
    const dateString = dateObj.toISOString().split('T')[0]
    
    days.push({
      date: dateString,
      day: day,
      isCurrentMonth: false,
      isToday: dateString === new Date().toISOString().split('T')[0],
      isSelected: selectedDate.value === dateString,
      hasData: hasDataForDate(dateString),
      calories: getCaloriesForDate(dateString),
      caloriesBurned: props.type === 'fitness' ? getCaloriesForDate(dateString) : 0
    })
  }
  
  return days
})

const hasDataForDate = (dateString) => {
  return monthlyData.value.some(d => d.date === dateString)
}

const getCaloriesForDate = (dateString) => {
  const data = monthlyData.value.find(d => d.date === dateString)
  if (!data) return 0
  
  // 根据 type 返回不同的数据
  return props.type === 'fitness' ? (data.caloriesBurned || 0) : (data.calories || 0)
}

const loadMonthlyData = async () => {
  try {
    if (props.type === 'fitness') {
      const summary = await healthApi.getMonthlyFitnessSummary(
        props.userId, 
        currentYear.value, 
        currentMonth.value
      )
      monthlyData.value = summary.dailyData || []
    } else if (props.type === 'diet') {
      const summary = await healthApi.getMonthlyDietSummary(
        props.userId, 
        currentYear.value, 
        currentMonth.value
      )
      monthlyData.value = summary.dailyData || []
    } else if (props.type === 'combined') {
      const [dietSummary, fitnessSummary] = await Promise.all([
        healthApi.getMonthlyDietSummary(props.userId, currentYear.value, currentMonth.value),
        healthApi.getMonthlyFitnessSummary(props.userId, currentYear.value, currentMonth.value)
      ]);
      
      const mergedData = {};
      
      (dietSummary.dailyData || []).forEach(day => {
        mergedData[day.date] = {
          date: day.date,
          calories: day.calories || 0,
          protein: day.protein || 0,
          carbs: day.carbs || 0,
          fat: day.fat || 0,
          caloriesBurned: 0
        };
      });
      
      (fitnessSummary.dailyData || []).forEach(day => {
        if (mergedData[day.date]) {
          mergedData[day.date].caloriesBurned = day.caloriesBurned || 0;
        } else {
          mergedData[day.date] = {
            date: day.date,
            calories: 0,
            protein: 0,
            carbs: 0,
            fat: 0,
            caloriesBurned: day.caloriesBurned || 0
          };
        }
      });
      
      monthlyData.value = Object.values(mergedData);
    }
  } catch (error) {
    console.error('加载月度数据失败:', error)
    monthlyData.value = []
  }
}

const previousMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
  selectedDate.value = null
  selectedDateData.value = null
}

const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
  selectedDate.value = null
  selectedDateData.value = null
}

const selectDate = async (day) => {
  if (!day.isCurrentMonth) return
  
  selectedDate.value = day.date
  emit('date-selected', day.date)
  
  try {
    // 根据 type 调用不同的 API
    let records = [];
    let dietRecords = [];
    let fitnessRecords = [];
    
    if (props.type === 'fitness') {
      const response = await fetch(`/api/diet/fitness/daily/${props.userId}/${day.date}`);
      if (response.ok) {
        records = await response.json();
      }
    } else if (props.type === 'diet') {
      records = await healthApi.getDailyDiet(props.userId, day.date);
      records = (records || []).filter(record => 
        !(record.foodDescription && 
          (record.foodDescription.includes('💧 喝水打卡') || 
           record.foodDescription.toLowerCase().includes('water 打卡')))
      );
    } else if (props.type === 'combined') {
      const [dietResponse, fitnessResponse] = await Promise.all([
        fetch(`/api/diet/records/${props.userId}/${day.date}`),
        fetch(`/api/diet/fitness/daily/${props.userId}/${day.date}`)
      ]);
      
      if (dietResponse.ok) {
        dietRecords = await dietResponse.json();
        dietRecords = dietRecords.filter(record => 
          !(record.foodDescription && 
            (record.foodDescription.includes('💧 喝水打卡') || 
             record.foodDescription.toLowerCase().includes('water 打卡')))
        );
      }
      
      if (fitnessResponse.ok) {
        fitnessRecords = await fitnessResponse.json();
      }
      
      records = [...dietRecords, ...fitnessRecords];
    }
    
    const totals = {
      calories: 0,
      protein: 0,
      carbs: 0,
      fat: 0,
      caloriesBurned: 0
    }
    
    if (props.type === 'fitness') {
      // 健身记录数据处理
      records.forEach(record => {
        if (record.caloriesBurned) totals.caloriesBurned += record.caloriesBurned
      })
    } else if (props.type === 'diet') {
      // 饮食记录数据处理
      records.forEach(record => {
        if (record.calories) totals.calories += record.calories
        if (record.protein) totals.protein += record.protein
        if (record.carbs) totals.carbs += record.carbs
        if (record.fat) totals.fat += record.fat
      })
    } else if (props.type === 'combined') {
      // 综合数据处理
      dietRecords.forEach(record => {
        if (record.calories) totals.calories += record.calories
        if (record.protein) totals.protein += record.protein
        if (record.carbs) totals.carbs += record.carbs
        if (record.fat) totals.fat += record.fat
      });
      
      fitnessRecords.forEach(record => {
        if (record.caloriesBurned) totals.caloriesBurned += record.caloriesBurned
      });
    }
    
    selectedDateData.value = {
      date: day.date,
      records: records,
      dietRecords: dietRecords,
      fitnessRecords: fitnessRecords,
      totals: totals
    }
  } catch (error) {
    console.error('加载日期详情失败:', error)
    selectedDateData.value = {
      date: day.date,
      records: [],
      dietRecords: [],
      fitnessRecords: [],
      totals: {
        calories: 0,
        protein: 0,
        carbs: 0,
        fat: 0,
        caloriesBurned: 0
      }
    }
  }
}

const closeDetail = () => {
  selectedDateData.value = null
  selectedDate.value = null
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const formatTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

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

watch([currentYear, currentMonth], () => {
  loadMonthlyData()
})

onMounted(() => {
  loadMonthlyData()
})
</script>

<style scoped>
.professional-calendar {
  max-width: 500px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.nav-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  color: white;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.month-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.calendar-wrapper {
  padding: 20px;
}

.weekdays-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 15px;
  font-weight: 600;
  color: #666;
}

.weekday {
  padding: 10px 0;
  font-size: 14px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.calendar-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  background: #f8f9fa;
  border: 2px solid transparent;
}

.calendar-cell.prev-month,
.calendar-cell.next-month {
  color: #ccc;
  background: #f0f0f0;
}

.calendar-cell.current-month {
  background: white;
  border-color: #eee;
}

.calendar-cell.today {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  font-weight: bold;
  border-color: #667eea;
}

.calendar-cell.today .day-number {
  color: white;
}

.calendar-cell.has-data {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-color: #667eea;
  border-width: 2px;
  font-weight: 600;
}

.calendar-cell.has-data .day-number {
  color: #1565c0;
}

.calendar-cell.selected {
  background: linear-gradient(135deg, #ff6b6b, #ee5a52);
  color: white;
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
}

.calendar-cell:hover:not(.selected) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.day-number {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}

.calories-badge {
  font-size: 10px;
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 600;
}

.calendar-cell.today .calories-badge,
.calendar-cell.selected .calories-badge {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.calendar-cell.has-data .calories-badge {
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
  font-weight: 700;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  z-index: 999999;
  padding: 120px 20px 20px;
  overflow-y: auto;
}

.modal-content {
  background: white;
  border-radius: 16px;
  max-width: 500px;
  width: 100%;
  max-height: calc(100vh - 140px);
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  margin-bottom: 20px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 16px 16px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-button {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s ease;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.2);
}

.modal-body {
  padding: 20px;
}

.records-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.record-card {
  background: #fafafa;
  border-radius: 12px;
  padding: 15px;
  border: 1px solid #eee;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.meal-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.meal-tag.breakfast { background: #fff3cd; color: #856404; }
.meal-tag.lunch { background: #d1ecf1; color: #0c5460; }
.meal-tag.dinner { background: #f8d7da; color: #721c24; }
.meal-tag.snack { background: #d4edda; color: #155724; }

.record-time {
  color: #999;
  font-size: 12px;
}

.food-description {
  margin: 0 0 12px 0;
  color: #333;
  line-height: 1.4;
}

.nutrition-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.nutrition-tag {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.nutrition-tag.calories { background: #ffebee; color: #c62828; }
.nutrition-tag.protein { background: #e3f2fd; color: #1976d2; }
.nutrition-tag.carbs { background: #fff3e0; color: #ef6c00; }
.nutrition-tag.fat { background: #fce4ec; color: #c2185b; }

.daily-summary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-radius: 12px;
  padding: 20px;
  margin-top: 20px;
}

.daily-summary h4 {
  margin: 0 0 15px 0;
  text-align: center;
  font-size: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
}

.label {
  font-size: 13px;
  opacity: 0.9;
}

.value {
  font-weight: 700;
  font-size: 14px;
}

.no-data {
  text-align: center;
  padding: 40px 20px;
}

.no-data-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.no-data p {
  color: #999;
  margin: 0;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .professional-calendar {
    margin: 10px;
    border-radius: 12px;
  }

  .calendar-header {
    padding: 15px;
  }

  .month-title {
    font-size: 18px;
  }

  .calendar-wrapper {
    padding: 15px;
  }

  .day-number {
    font-size: 14px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .modal-overlay {
    padding: 100px 15px 15px;
    align-items: flex-start;
  }

  .modal-content {
    max-height: calc(100vh - 115px);
    margin-bottom: 15px;
  }
}
</style>