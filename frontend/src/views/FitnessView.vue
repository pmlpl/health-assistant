<template>
  <div class="fitness-layout">
    <div class="page-header">
      <h1>
        <svg class="header-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M6.5 6.5L17.5 17.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M21 21L15 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M3 3L9 9" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <rect x="9" y="9" width="6" height="6" rx="1" stroke="currentColor" stroke-width="2"/>
          <path d="M3 9V15C3 15.5304 3.21071 16.0391 3.58579 16.4142C3.96086 16.7893 4.46957 17 5 17H9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M15 9H19C19.5304 9 20.0391 9.21071 20.4142 9.58579C20.7893 9.96086 21 10.4696 21 11V15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        健身记录
      </h1>
      <div class="stats-bar">
        <span class="stat-item">🏋️ 今日训练：{{ fitnessEntries.length }} 项</span>
        <span class="stat-item">🔥 总消耗：{{ totalCaloriesBurned }}kcal</span>
      </div>
    </div>
  
    <div class="main-container">
      <div class="left-panel">
        <div class="card record-card">
          <h2>➕ 添加健身项目</h2>
          
          <div class="fitness-selection">
            <label>类型</label>
            <div class="fitness-buttons">
              <button 
                v-for="fitness in fitnessOptions" 
                :key="fitness.value"
                class="fitness-btn"
                :class="{ active: newFitnessEntry.type === fitness.value }"
                @click="newFitnessEntry.type = fitness.value"
              >
                {{ fitness.icon }} {{ fitness.label }}
              </button>
            </div>
          </div>

          <div class="custom-input-section">
            <div class="input-row">
              <div class="fitness-name-input">
                <label>项目名称</label>
                <input
                  v-model="newFitnessEntry.name"
                  placeholder="输入健身项目名称"
                  class="fitness-input"
                  @keyup.enter="addCustomFitnessItem"
                />
              </div>
              <div class="metric-type-selector">
                <label>记录类型</label>
                <select v-model="newFitnessEntry.metricType" class="metric-select">
                  <option value="duration">⏱️ 时长</option>
                  <option value="reps">🔢 次数</option>
                  <option value="weight">🏋️ 重量</option>
                  <option value="duration_reps">⏱️+🔢 时长 + 次数</option>
                  <option value="weight_reps">🏋️+🔢 重量 + 次数</option>
                </select>
              </div>
            </div>
                      
            <div class="input-row metrics-inputs">
              <div v-if="['duration', 'duration_reps'].includes(newFitnessEntry.metricType)" class="metric-input">
                <label>时长 (分钟)</label>
                <input
                  v-model="newFitnessEntry.duration"
                  type="number"
                  placeholder="30"
                  class="metric-number"
                  min="1"
                  step="1"
                />
              </div>
                        
              <div v-if="['reps', 'duration_reps', 'weight_reps'].includes(newFitnessEntry.metricType)" class="metric-input">
                <label>次数 (个)</label>
                <input
                  v-model="newFitnessEntry.repetitions"
                  type="number"
                  placeholder="10"
                  class="metric-number"
                  min="1"
                  step="1"
                />
              </div>
                        
              <div v-if="['weight', 'weight_reps'].includes(newFitnessEntry.metricType)" class="metric-input">
                <label>重量 (kg)</label>
                <input
                  v-model="newFitnessEntry.weight"
                  type="number"
                  placeholder="20"
                  class="metric-number"
                  min="0.5"
                  step="0.5"
                />
              </div>
                        
              <button @click="addCustomFitnessItem" class="add-fitness-btn">+</button>
            </div>
          </div>

          <div v-if="fitnessItems.length > 0" class="added-fitnesses">
            <h3>✅ 已添加的项目</h3>
            <div class="fitness-items-list">
              <div 
                v-for="(item, index) in fitnessItems" 
                :key="index" 
                class="fitness-item-tag"
              >
                <span class="fitness-name">{{ item.name }}</span>
                <div class="fitness-metrics">
                  <span v-if="item.duration" class="fitness-metric">⏱️ {{ item.duration }}分钟</span>
                  <span v-if="item.repetitions" class="fitness-metric">🔢 {{ item.repetitions }}个</span>
                  <span v-if="item.weight" class="fitness-metric">🏋️ {{ item.weight }}kg</span>
                </div>
                <button @click="removeFitnessItem(index)" class="remove-btn">×</button>
              </div>
            </div>
          </div>

          <div class="action-buttons">
            <button 
              class="btn save-btn" 
              @click="saveFitnessRecord" 
              :disabled="saving || fitnessItems.length === 0"
            >
              💾 保存记录
            </button>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <div class="card history-card">
          <div class="history-header">
            <h2>📋 今日健身记录</h2>
            <div class="summary-stats">
              <span class="summary-item">🔥 总消耗：{{ totalCaloriesBurned }}kcal</span>
              <span class="summary-item">⏱️ 总时长：{{ totalDuration }}分钟</span>
              <span v-if="totalRepetitions > 0" class="summary-item">🔢 总次数：{{ totalRepetitions }}个</span>
              <span v-if="totalWeight > 0" class="summary-item">🏋️ 总重量：{{ totalWeight }}kg</span>
            </div>
          </div>

          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>正在加载记录...</p>
          </div>

          <div v-else-if="fitnessEntries.length === 0" class="empty-state">
            <div class="empty-icon">🏋️</div>
            <h3>还没有记录</h3>
            <p>今天还没有健身记录</p>
            <p class="tip">💪 快去添加你的第一个健身项目吧</p>
          </div>

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

          <div class="records-container">
            <div class="record-item" v-for="(entry, index) in fitnessEntries" :key="entry.id || index" :class="{ 'selected': selectedRecords.includes(entry.id) }">
              <div class="record-header">
                <div class="fitness-info">
                  <input 
                    type="checkbox" 
                    v-model="selectedRecords" 
                    :value="entry.id"
                    class="record-checkbox"
                    @change="updateSelectAllState"
                  >
                  <span class="fitness-badge" :class="getFitnessTypeClass(entry.workoutType)">
                    {{ getFitnessTypeName(entry.workoutType) }}
                  </span>
                  <span class="time-stamp">{{ entry.recordTime || formatTime(entry.recordedAt) }}</span>
                </div>
                <div class="record-actions">
                  <div class="calories-badge">
                    🔥 {{ entry.caloriesBurned || 0 }}<span class="unit">kcal</span>
                  </div>
                  <button 
                    @click="deleteRecord(entry.id, index)" 
                    class="delete-btn"
                    title="删除这条记录"
                  >
                    🗑️ 删除
                  </button>
                </div>
              </div>
              
              <div class="record-body">
                <p class="fitness-desc">{{ entry.workoutName }}</p>
                              
                <div class="fitness-details">
                  <span v-if="entry.durationMinutes" class="detail-item">⏱️ 时长：{{ entry.durationMinutes }}分钟</span>
                  <span v-if="entry.repetitions" class="detail-item">🔢 次数：{{ entry.repetitions }}个</span>
                  <span v-if="entry.weightKg" class="detail-item">🏋️ 重量：{{ entry.weightKg }}kg</span>
                  <span class="detail-item">📊 强度：{{ getIntensityLevel(entry.intensityLevel) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="summary-section" v-if="fitnessEntries.length > 0">
            <h3>📈 今日训练总结</h3>
            <div class="summary-grid">
              <div class="summary-box calories">
                <div class="summary-icon">🔥</div>
                <div class="big-number">{{ totalCaloriesBurned }}</div>
                <div class="metric">总消耗</div>
                <div class="small-unit">kcal</div>
              </div>
              <div class="summary-box duration">
                <div class="summary-icon">⏱️</div>
                <div class="big-number">{{ totalDuration }}</div>
                <div class="metric">总时长</div>
                <div class="small-unit">分钟</div>
              </div>
              <div class="summary-box reps" v-if="totalRepetitions > 0">
                <div class="summary-icon">🔢</div>
                <div class="big-number">{{ totalRepetitions }}</div>
                <div class="metric">总次数</div>
                <div class="small-unit">个</div>
              </div>
              <div class="summary-box weight" v-if="totalWeight > 0">
                <div class="summary-icon">🏋️</div>
                <div class="big-number">{{ totalWeight }}</div>
                <div class="metric">总重量</div>
                <div class="small-unit">kg</div>
              </div>
              <div class="summary-box count">
                <div class="summary-icon">🏋️</div>
                <div class="big-number">{{ fitnessEntries.length }}</div>
                <div class="metric">训练项目</div>
                <div class="small-unit">项</div>
              </div>
            </div>
            
            <div class="ai-analysis-section">
              <button 
                @click="analyzeFitnessItems" 
                class="btn-ai-analyze"
                :disabled="analyzing"
              >
                {{ analyzing ? '🔄 分析中...' : '🤖 AI智能分析收获' }}
              </button>
              
              <div v-if="aiAnalysis" class="analysis-result">
                <h4>💡 训练分析报告</h4>
                <div class="analysis-content" v-html="formatAnalysis(aiAnalysis)"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useUserStore } from '../stores/userStore';
// 导入healthApi
import { healthApi } from '../api/healthApi';

const userStore = useUserStore();

// 健身类型选项
const fitnessOptions = [
  { value: 'cardio', label: '有氧运动', icon: '🏃' },
  { value: 'strength', label: '力量训练', icon: '💪' },
  { value: 'flexibility', label: '柔韧性训练', icon: '🧘' },
  { value: 'balance', label: '平衡训练', icon: '🤸' }
];

// 获取本地日期字符串
const getLocalDateString = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// 当前日期
const currentDate = ref(getLocalDateString());
// 健身条目列表
const fitnessEntries = ref([]);
// 新的健身记录
const newFitnessEntry = ref({
  type: 'cardio',
  name: '',
  metricType: 'duration', // duration, reps, weight, duration_reps, weight_reps
  duration: 30,
  repetitions: 10,
  weight: 20
});
// 已添加的健身项目
const fitnessItems = ref([]);
// 分析状态
const analyzing = ref(false);
// 保存状态
const saving = ref(false);
// 加载状态
const loading = ref(false);
// AI分析结果
const aiAnalysis = ref('');

// 批量删除相关
const selectedRecords = ref([]);
const selectAll = ref(false);
const batchDeleting = ref(false);

// 计算总消耗热量
const totalCaloriesBurned = computed(() => {
  const savedCalories = fitnessEntries.value.reduce((total, item) => {
    return total + (item.caloriesBurned || 0);
  }, 0);
  return savedCalories;
});

// 计算总时长
const totalDuration = computed(() => {
  return fitnessEntries.value.reduce((total, item) => {
    return total + (item.durationMinutes || 0);
  }, 0);
});

// 计算总次数
const totalRepetitions = computed(() => {
  return fitnessEntries.value.reduce((total, item) => {
    return total + (item.repetitions || 0);
  }, 0);
});

// 计算总重量
const totalWeight = computed(() => {
  return fitnessEntries.value.reduce((total, item) => {
    return total + (item.weightKg || 0);
  }, 0);
});

// 计算单个项目的热量消耗
const calculateCalories = (item) => {
  // 简化的热量计算公式，根据项目类型和时长/次数估算
  const baseMetabolicRate = 1.05; // kcal/min
  let multiplier = 1;
  
  switch(item.type) {
    case 'cardio':
      multiplier = 1.5;
      break;
    case 'strength':
      multiplier = 1.2;
      break;
    case 'flexibility':
      multiplier = 0.8;
      break;
    case 'balance':
      multiplier = 0.6;
      break;
    default:
      multiplier = 1;
  }
  
  // 如果有时长，按时长计算；如果只有次数/重量，按次数估算
  if (item.duration) {
    return Math.round(item.duration * baseMetabolicRate * multiplier);
  } else if (item.repetitions) {
    // 假设每次动作约 3 秒，转换为分钟计算
    const estimatedMinutes = item.repetitions * 3 / 60;
    return Math.round(estimatedMinutes * baseMetabolicRate * multiplier);
  } else if (item.weight) {
    // 如果有重量，考虑重量因素
    const baseCalories = baseMetabolicRate * multiplier * 5; // 基础 5 分钟
    return Math.round(baseCalories * (1 + (item.weight || 0) / 100));
  }
  
  return 0;
};

// 添加自定义健身项目
const addCustomFitnessItem = () => {
  if (!newFitnessEntry.value.name.trim()) {
    alert('请输入健身项目名称');
    return;
  }
  
  const metricType = newFitnessEntry.value.metricType;
  
  // 根据类型验证输入
  if (metricType === 'duration' && (!newFitnessEntry.value.duration || newFitnessEntry.value.duration <= 0)) {
    alert('请输入有效的时长');
    return;
  }
  if (metricType === 'reps' && (!newFitnessEntry.value.repetitions || newFitnessEntry.value.repetitions <= 0)) {
    alert('请输入有效的次数');
    return;
  }
  if (metricType === 'weight' && (!newFitnessEntry.value.weight || newFitnessEntry.value.weight <= 0)) {
    alert('请输入有效的重量');
    return;
  }
  if (metricType === 'duration_reps' && ((!newFitnessEntry.value.duration || newFitnessEntry.value.duration <= 0) || 
      (!newFitnessEntry.value.repetitions || newFitnessEntry.value.repetitions <= 0))) {
    alert('请输入有效的时长和次数');
    return;
  }
  if (metricType === 'weight_reps' && ((!newFitnessEntry.value.weight || newFitnessEntry.value.weight <= 0) || 
      (!newFitnessEntry.value.repetitions || newFitnessEntry.value.repetitions <= 0))) {
    alert('请输入有效的重量和次数');
    return;
  }
  
  // 创建新的健身项目
  const newItem = {
    id: Date.now(),
    type: newFitnessEntry.value.type,
    name: newFitnessEntry.value.name,
    metricType: metricType,
    duration: metricType.includes('duration') ? parseInt(newFitnessEntry.value.duration) : null,
    repetitions: metricType.includes('reps') ? parseInt(newFitnessEntry.value.repetitions) : null,
    weight: metricType.includes('weight') ? parseFloat(newFitnessEntry.value.weight) : null
  };
  
  fitnessItems.value.push(newItem);
  
  // 重置表单
  newFitnessEntry.value.name = '';
  newFitnessEntry.value.duration = 30;
  newFitnessEntry.value.repetitions = 10;
  newFitnessEntry.value.weight = 20;
};

// 移除健身项目
const removeFitnessItem = (index) => {
  fitnessItems.value.splice(index, 1);
};

// AI 分析健身项目 - 专注于健身收获和消耗
const analyzeFitnessItems = async () => {
  if (fitnessEntries.value.length === 0) {
    alert('请先添加健身记录');
    return;
  }
  
  analyzing.value = true;
  aiAnalysis.value = '';
  
  try {
    const userId = userStore.userData?.userId;
    if (!userId) {
      alert('请先登录');
      return;
    }

    const fitnessAnalysisData = {
      totalCalories: totalCaloriesBurned.value,
      totalDuration: totalDuration.value,
      totalRepetitions: totalRepetitions.value,
      totalWeight: totalWeight.value,
      workoutCount: fitnessEntries.value.length,
      workouts: fitnessEntries.value.map(item => ({
        name: item.workoutName,
        type: item.workoutType,
        duration: item.durationMinutes,
        repetitions: item.repetitions,
        weight: item.weightKg,
        calories: item.caloriesBurned
      })),
      analysisType: 'fitness_workout'
    };
    
    console.log('发送健身收获分析请求:', { userId, fitnessAnalysisData });
    
    const response = await axios.post(
      `/api/ai/analyze-fitness-workout/${userId}`,
      fitnessAnalysisData,
      {
        timeout: 60000,
        headers: { 'Content-Type': 'application/json' }
      }
    );
    
    console.log('健身收获分析响应:', response.data);
    
    if (response.data.success) {
      aiAnalysis.value = response.data.analysis;
    } else {
      aiAnalysis.value = '分析失败：' + response.data.error;
    }
  } catch (error) {
    console.error('AI 分析失败:', error);
    if (error.code === 'ECONNABORTED') {
      aiAnalysis.value = '分析超时，请稍后重试';
    } else {
      aiAnalysis.value = 'AI 分析失败：' + error.message;
    }
  } finally {
    analyzing.value = false;
  }
};

// 保存健身记录
const saveFitnessRecord = async () => {
  if (fitnessItems.value.length === 0) return;
  
  saving.value = true;
  
  try {
    const userId = userStore.userData?.userId;
    if (!userId) {
      alert('请先登录');
      return;
    }

    // 构建健身记录数据
    const recordsToSave = fitnessItems.value.map(item => ({
      userId: userId,
      date: currentDate.value,
      name: item.name,
      type: item.type,
      durationMinutes: item.duration,
      repetitions: item.repetitions,
      weightKg: item.weight,
      calories: Math.round(calculateCalories(item))
    }));

    // 调用后端 API 保存记录
    const result = await healthApi.batchSaveFitnessRecords(recordsToSave);
    
    if (result.success) {
      alert('健身记录保存成功！');
      
      // 重置表单和分析结果
      aiAnalysis.value = '';
      newFitnessEntry.value.name = '';
      newFitnessEntry.value.duration = 30;
      
      // 清空健身项目列表，准备重新加载
      fitnessItems.value = [];
      
      // 重新加载数据
      loadTodayData();
    } else {
      throw new Error(result.error || '保存失败');
    }
  } catch (error) {
    console.error('保存记录失败:', error);
    alert('保存记录失败，请重试。');
  } finally {
    saving.value = false;
  }
};

// 加载今日数据
const loadTodayData = async () => {
  try {
    loading.value = true;
    const userId = userStore.userData?.userId;
    if (!userId) return;
    
    const dateStr = currentDate.value;
      
    const records = await healthApi.getDailyFitnessRecords(userId, dateStr);
    fitnessEntries.value = records || [];
  } catch (error) {
    console.error('加载数据失败:', error);
    fitnessEntries.value = [];
  } finally {
    loading.value = false;
  }
};

// 获取健身类型名称
const getFitnessTypeName = (type) => {
  const types = {
    'cardio': '有氧运动',
    'strength': '力量训练',
    'flexibility': '柔韧性训练',
    'balance': '平衡训练'
  };
  return types[type] || type;
};

// 获取健身类型样式类
const getFitnessTypeClass = (type) => {
  const classes = {
    'cardio': 'cardio',
    'strength': 'strength',
    'flexibility': 'flexibility',
    'balance': 'balance'
  };
  return classes[type] || '';
};

// 获取强度等级名称
const getIntensityLevel = (level) => {
  const levels = {
    1: '低强度',
    2: '中低强度',
    3: '中等强度',
    4: '中高强度',
    5: '高强度'
  };
  return levels[level] || '中等强度';
};

// 格式化时间
const formatTime = (dateTime) => {
  if (!dateTime) return '';
  const date = new Date(dateTime);
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
};

// 格式化分析结果
const formatAnalysis = (text) => {
  if (!text) return '';
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>');
};

// 全选/取消全选
const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedRecords.value = fitnessEntries.value
      .filter(entry => entry.id)
      .map(entry => entry.id);
  } else {
    selectedRecords.value = [];
  }
};

// 更新全选状态
const updateSelectAllState = () => {
  const allIds = fitnessEntries.value
    .filter(entry => entry.id)
    .map(entry => entry.id);
  selectAll.value = allIds.length > 0 && 
    allIds.every(id => selectedRecords.value.includes(id));
};

// 批量删除
const batchDelete = async () => {
  if (selectedRecords.value.length === 0) {
    alert('请先选择要删除的记录');
    return;
  }
  
  if (!confirm(`确定要删除选中的 ${selectedRecords.value.length} 条记录吗？此操作不可撤销。`)) {
    return;
  }
  
  try {
    batchDeleting.value = true;
    
    const response = await fetch('/api/diet/fitness/batch', {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(selectedRecords.value)
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log('批量删除成功:', result);
      
      fitnessEntries.value = fitnessEntries.value.filter(
        entry => !selectedRecords.value.includes(entry.id)
      );
      
      selectedRecords.value = [];
      selectAll.value = false;
      
      alert(`成功删除 ${result.deletedCount} 条记录！`);
    } else {
      const errorResult = await response.json();
      throw new Error(errorResult.error || '批量删除失败');
    }
  } catch (err) {
    console.error('批量删除失败:', err);
    alert(`批量删除失败: ${err.message}`);
  } finally {
    batchDeleting.value = false;
  }
};

// 删除单条记录
const deleteRecord = async (recordId, index) => {
  if (!recordId) {
    alert('记录ID无效，无法删除');
    return;
  }
  
  if (!confirm('确定要删除这条健身记录吗？此操作不可撤销。')) {
    return;
  }
  
  try {
    userStore.setLoading(true);
    userStore.clearError();
    
    const response = await fetch(`/api/diet/fitness/${recordId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log('删除记录成功:', result);
      
      fitnessEntries.value.splice(index, 1);
      
      alert('记录删除成功！');
    } else {
      const errorResult = await response.json();
      throw new Error(errorResult.error || '删除失败');
    }
  } catch (err) {
    console.error('删除记录失败:', err);
    userStore.setError(err.message || '删除记录失败');
    alert(`删除失败: ${err.message}`);
  } finally {
    userStore.setLoading(false);
  }
};

// 组件挂载时加载数据
onMounted(() => {
  loadTodayData();
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

.fitness-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0;
  background: #ffffff;
  min-height: 100vh;
  position: relative;
}

.fitness-layout::before {
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

.main-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 0 32px 48px;
  position: relative;
  z-index: 1;
}

@media (max-width: 1024px) {
  .main-container {
    grid-template-columns: 1fr;
  }
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
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.04),
    0 24px 64px rgba(0, 0, 0, 0.08);
}

.card h2 {
  color: #1d1d1f;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.fitness-selection {
  margin-bottom: 24px;
}

.fitness-selection label {
  display: block;
  margin-bottom: 12px;
  font-weight: 500;
  color: #1d1d1f;
  font-size: 15px;
}

.fitness-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.fitness-btn {
  padding: 12px 24px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: #ffffff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-size: 15px;
  font-weight: 500;
  color: #1d1d1f;
}

.fitness-btn:hover {
  border-color: #007aff;
  background: rgba(0, 122, 255, 0.05);
}

.fitness-btn.active {
  border-color: #007aff;
  background: #007aff;
  color: white;
}

.custom-input-section {
  margin-bottom: 24px;
}

.input-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .input-row {
    flex-direction: column;
    align-items: stretch;
  }
}

.fitness-name-input, .duration-input {
  flex: 1;
  min-width: 150px;
}

.fitness-input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  font-size: 17px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  background: #f5f5f7;
  color: #1d1d1f;
  font-family: inherit;
}

.fitness-input:focus {
  outline: none;
  border-color: #007aff;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}

.metric-type-selector {
  flex: 1;
  min-width: 150px;
}

.metric-select {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  font-size: 17px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  background: #f5f5f7;
  color: #1d1d1f;
  font-family: inherit;
  cursor: pointer;
}

.metric-select:focus {
  outline: none;
  border-color: #007aff;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}

.metrics-inputs {
  margin-top: 16px;
}

.metric-input {
  flex: 1;
  min-width: 120px;
}

.metric-input label, .fitness-name-input label, .duration-input label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #1d1d1f;
  font-size: 15px;
}

.metric-number {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  font-size: 17px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  background: #f5f5f7;
  color: #1d1d1f;
  font-family: inherit;
}

.metric-number:focus {
  outline: none;
  border-color: #007aff;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}

.add-fitness-btn {
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

.add-fitness-btn:hover {
  background: #0077ed;
  transform: scale(1.05);
}

.added-fitnesses h3 {
  margin-bottom: 16px;
  color: #1d1d1f;
  font-size: 17px;
  font-weight: 600;
}

.fitness-items-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.fitness-item-tag {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: #f5f5f7;
  border: none;
  border-radius: 980px;
  padding: 10px 16px;
  font-size: 15px;
  color: #1d1d1f;
}

.fitness-name {
  font-weight: 500;
}

.fitness-metrics {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-left: 8px;
}

.fitness-metric {
  color: #86868b;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.05);
  padding: 4px 8px;
  border-radius: 6px;
  white-space: nowrap;
}

.fitness-duration {
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
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-btn:hover {
  background: #ff2d23;
  transform: scale(1.1);
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.btn {
  padding: 14px 28px;
  border: none;
  border-radius: 980px;
  font-size: 17px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.save-btn {
  background: #34c759;
  color: white;
}

.save-btn:hover:not(:disabled) {
  background: #30b350;
  transform: scale(1.02);
}

.save-btn:disabled {
  background: #d1d1d6;
  cursor: not-allowed;
}

.history-card {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.history-card::-webkit-scrollbar {
  width: 8px;
}

.history-card::-webkit-scrollbar-track {
  background: transparent;
}

.history-card::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

.history-card::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.history-header h2 {
  margin-bottom: 0;
  border-bottom: none;
  padding-bottom: 0;
}

.summary-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  max-width: 100%;
  overflow-x: auto;
}

.summary-stats::-webkit-scrollbar {
  height: 4px;
}

.summary-stats::-webkit-scrollbar-track {
  background: transparent;
}

.summary-stats::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

.summary-item {
  font-size: 13px;
  color: #86868b;
  background: #f5f5f7;
  padding: 8px 16px;
  border-radius: 980px;
  font-weight: 500;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  color: #86868b;
  min-height: 300px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 0, 0, 0.05);
  border-top: 3px solid #007aff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 24px;
  color: #86868b;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-state h3 {
  color: #1d1d1f;
  margin-bottom: 8px;
  font-size: 21px;
  font-weight: 600;
}

.empty-state .tip {
  color: #007aff;
  font-weight: 500;
  font-size: 15px;
}

.batch-actions-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f5f7;
  border-radius: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.select-all-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  color: #1d1d1f;
}

.record-checkbox {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #007aff;
}

.selected-count {
  color: #007aff;
  font-size: 14px;
  font-weight: 500;
}

.btn-batch-delete {
  background: #ff3b30;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 980px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.btn-batch-delete:hover:not(:disabled) {
  background: #ff2d23;
}

.btn-batch-delete:disabled {
  background: #d1d1d6;
  cursor: not-allowed;
}

.records-container {
  flex: 1;
}

.record-item {
  padding: 20px;
  border: none;
  border-radius: 16px;
  margin-bottom: 12px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  background: #f5f5f7;
}

.record-item:hover {
  background: #e8e8ed;
}

.record-item.selected {
  background: rgba(0, 122, 255, 0.08);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-header .fitness-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fitness-badge {
  padding: 6px 14px;
  border-radius: 980px;
  font-size: 13px;
  font-weight: 500;
}

.fitness-badge.cardio {
  background: rgba(255, 149, 0, 0.12);
  color: #ff9500;
}

.fitness-badge.strength {
  background: rgba(0, 122, 255, 0.12);
  color: #007aff;
}

.fitness-badge.flexibility {
  background: rgba(175, 82, 222, 0.12);
  color: #af52de;
}

.fitness-badge.balance {
  background: rgba(52, 199, 89, 0.12);
  color: #34c759;
}

.time-stamp {
  color: #86868b;
  font-size: 13px;
}

.record-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.calories-badge {
  font-size: 17px;
  font-weight: 600;
  color: #ff9500;
}

.calories-badge .unit {
  font-size: 13px;
  font-weight: normal;
}

.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 6px 12px;
  opacity: 0.5;
  transition: all 0.2s ease;
  color: #86868b;
}

.delete-btn:hover {
  opacity: 1;
  color: #ff3b30;
}

.record-body {
  padding-left: 32px;
}

.fitness-desc {
  font-weight: 500;
  color: #1d1d1f;
  margin-bottom: 8px;
  font-size: 15px;
}

.fitness-details {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.detail-item {
  font-size: 14px;
  color: #86868b;
}

.summary-section {
  margin-top: 28px;
  padding-top: 28px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.summary-section h3 {
  color: #1d1d1f;
  margin-bottom: 20px;
  font-size: 19px;
  font-weight: 600;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 600px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

.summary-box {
  text-align: center;
  padding: 24px;
  border-radius: 20px;
  background: #f5f5f7;
}

.summary-box.calories {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.08) 0%, rgba(255, 149, 0, 0.02) 100%);
}

.summary-box.duration {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.08) 0%, rgba(0, 122, 255, 0.02) 100%);
}

.summary-box.reps {
  background: linear-gradient(135deg, rgba(175, 82, 222, 0.08) 0%, rgba(175, 82, 222, 0.02) 100%);
}

.summary-box.weight {
  background: linear-gradient(135deg, rgba(255, 149, 0, 0.08) 0%, rgba(255, 149, 0, 0.02) 100%);
}

.summary-box.count {
  background: linear-gradient(135deg, rgba(52, 199, 89, 0.08) 0%, rgba(52, 199, 89, 0.02) 100%);
}

.summary-icon {
  font-size: 2rem;
  margin-bottom: 12px;
  animation: iconFloat 2.5s ease-in-out infinite;
}

.big-number {
  font-size: 36px;
  font-weight: 700;
  color: #1d1d1f;
  letter-spacing: -1px;
}

.metric {
  font-size: 15px;
  color: #1d1d1f;
  margin: 4px 0;
  font-weight: 500;
}

.small-unit {
  font-size: 13px;
  color: #86868b;
}

.ai-analysis-section {
  margin-top: 20px;
}

.btn-ai-analyze {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 17px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.btn-ai-analyze:hover:not(:disabled) {
  transform: scale(1.01);
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.3);
}

.btn-ai-analyze:disabled {
  background: #d1d1d6;
  cursor: not-allowed;
}

.analysis-result {
  margin-top: 20px;
  padding: 24px;
  background: #f5f5f7;
  border-radius: 16px;
  border-left: 4px solid #007aff;
}

.analysis-result h4 {
  color: #1d1d1f;
  margin-bottom: 12px;
  font-size: 17px;
  font-weight: 600;
}

.analysis-content {
  line-height: 1.7;
  color: #1d1d1f;
  font-size: 15px;
}

@media (max-width: 768px) {
  .page-header {
    padding: 48px 20px 40px;
  }
  
  .page-header h1 {
    font-size: 32px;
  }
  
  .stats-bar {
    gap: 12px;
  }
  
  .main-container {
    padding: 0 20px 48px;
    gap: 24px;
  }
  
  .card {
    padding: 24px;
    border-radius: 20px;
  }
  
  .card h2 {
    font-size: 20px;
  }
}
</style>
