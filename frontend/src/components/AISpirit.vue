<template>
  <div 
    class="ai-spirit-container"
    :class="{ 'is-dragging': isDragging, 'show-menu': showMenu }"
    :style="{ left: position.x + 'px', top: position.y + 'px' }"
    @mousedown="handleMouseDown"
    @click="handleClick"
  >
    <div class="spirit-body" :class="{ 'is-dragging': isDragging }">
      <div class="spirit-head">
        <!-- 护士帽 -->
        <div class="nurse-hat">
          <div class="hat-cross">+</div>
        </div>
        <!-- 眼睛 -->
        <div class="spirit-eyes" :style="eyesStyle">
          <div class="eye left"></div>
          <div class="eye right"></div>
        </div>
        <!-- 嘴巴 -->
        <div class="spirit-mouth"></div>
        <!-- 发光效果 -->
        <div class="spirit-glow"></div>
      </div>
      <div class="spirit-tail"></div>
    </div>
    <div class="spirit-tooltip">AI 健康助手</div>
    
    <!-- 环绕菜单 -->
    <div class="orbit-menu">
      <div 
        v-for="(item, index) in menuItems" 
        :key="item.id"
        class="orbit-item"
        :style="getOrbitItemStyle(index)"
        @click="selectMenuItem(item)"
        @mouseenter="hoverMenuItem(index)"
        @mouseleave="leaveMenuItem()"
      >
        <span class="orbit-icon">{{ item.icon }}</span>
        <span class="orbit-label">{{ item.label }}</span>
      </div>
    </div>
  </div>

  <!-- AI 咨询聊天框 -->
  <div v-if="showChat" class="chat-overlay" @click="closeChat">
    <div class="chat-dialog" @click.stop>
      <div class="chat-header">
        <div class="chat-header-left">
          <span class="chat-icon">{{ currentChatIcon }}</span>
          <h3 class="chat-title">{{ currentChatTitle }}</h3>
        </div>
        <button class="chat-close-btn" @click="closeChat">×</button>
      </div>
            
      <div class="chat-body">
        <!-- 放松练习和咨询问卷的菜单选项 -->
        <div v-if="showFeatureMenu" class="feature-menu-container">
          <div class="feature-menu-grid">
            <div 
              v-for="feature in filteredFeatureItems" 
              :key="feature.id"
              class="feature-menu-item"
              @click="selectFeature(feature)"
            >
              <span class="feature-icon">{{ feature.icon }}</span>
              <span class="feature-label">{{ feature.label }}</span>
              <span class="feature-desc">{{ feature.description }}</span>
            </div>
          </div>
        </div>
              
        <!-- 放松练习时长选择 -->
        <div v-if="currentMode === 'relaxation' && selectedRelaxationType && !isPracticing" class="relaxation-duration-container">
          <div class="duration-header">
            <span class="duration-icon">{{ selectedRelaxationType.icon }}</span>
            <h3>{{ selectedRelaxationType.label }}</h3>
          </div>
          <p class="duration-desc">{{ selectedRelaxationType.description }}</p>
          
          <div class="duration-slider-container">
            <label>选择练习时长（分钟）：</label>
            <div class="slider-wrapper">
              <input 
                type="range" 
                v-model="selectedDuration" 
                :min="0"
                :max="selectedRelaxationType.maxDuration" 
                step="1"
                class="duration-slider"
              />
              <div class="slider-labels">
                <span>{{ selectedRelaxationType.minDuration }}分钟</span>
                <span class="current-duration">建议：{{ selectedDuration }}分钟</span>
                <span>{{ selectedRelaxationType.maxDuration }}分钟</span>
              </div>
            </div>
            <p class="suggestion-text">💡 建议时长：{{ selectedRelaxationType.minDuration }}-{{ selectedRelaxationType.maxDuration }} 分钟</p>
          </div>
          
          <div class="action-buttons">
            <button @click="backToMenu" class="btn-back">返回</button>
            <button @click="startRelaxation" class="btn-start">开始练习</button>
          </div>
        </div>
        
        <!-- 心理咨询问卷表单 -->
        <div v-if="currentMode === 'questionnaire' && selectedQuestionnaire && !showAnalysisResult" class="questionnaire-container">
          <div class="questionnaire-header">
            <span class="questionnaire-icon">{{ selectedQuestionnaire.icon }}</span>
            <h3>{{ selectedQuestionnaire.label }}</h3>
          </div>
          <p class="questionnaire-desc">{{ selectedQuestionnaire.description }}</p>
          
          <form @submit.prevent="submitQuestionnaire" class="questionnaire-form">
            <div v-for="(question, qIndex) in questionnaireQuestions" :key="qIndex" class="question-item">
              <p class="question-text">{{ question.text }}</p>
              <div class="question-options">
                <label v-for="(option, oIndex) in question.options" :key="oIndex" class="option-label">
                  <input 
                    type="radio" 
                    :name="`q${qIndex}`" 
                    v-model="questionnaireAnswers[qIndex]" 
                    :value="oIndex"
                    class="option-radio"
                  />
                  <span class="option-text">{{ option }}</span>
                </label>
              </div>
            </div>
            
            <div class="action-buttons">
              <button type="button" @click="backToMenu" class="btn-back">返回</button>
              <button type="submit" :disabled="!isQuestionnaireComplete" class="btn-submit">
                {{ isSubmitting ? '分析中...' : '提交分析' }}
              </button>
            </div>
          </form>
        </div>
        
        <!-- 放松练习倒计时 -->
        <div v-else-if="currentMode === 'relaxation' && isPracticing" class="practice-countdown-container">
          <div class="countdown-display">
            <div class="countdown-icon">🧘</div>
            <div class="countdown-timer">{{ formatCountdown }}</div>
            <div class="countdown-tip">{{ currentPracticeTip }}</div>
          </div>
          <button @click="stopPractice" class="btn-stop">停止练习</button>
        </div>
        
        <!-- 问卷分析结果 -->
        <div v-else-if="currentMode === 'questionnaire' && showAnalysisResult" class="analysis-result-container">
          <div class="result-header">
            <span class="result-icon">📊</span>
            <h3>心理健康分析报告</h3>
          </div>
          
          <div class="result-content" v-html="analysisResult"></div>
          
          <div class="action-buttons">
            <button @click="retakeQuestionnaire" class="btn-back">重新测试</button>
            <button @click="backToMenu" class="btn-ok">确定</button>
          </div>
        </div>
              
        <!-- 消息列表（仅 AI 咨询显示） -->
        <div class="messages-container" v-show="currentMode === 'consultation'">
          <div
            v-for="(message, index) in chatMessages"
            :key="index"
            class="message-item"
            :class="message.type"
          >
            <div class="message-avatar">
              {{ message.type === 'user' ? '👤' : '🤖' }}
            </div>
            <div class="message-content-wrapper">
              <div class="message-bubble" v-html="formatMessage(message.content)"></div>
              <div class="message-time">
                {{ formatTime(message.timestamp) }}
              </div>
            </div>
          </div>
          
          <div v-if="isLoading" class="message-item assistant">
            <div class="message-avatar">🤖</div>
            <div class="message-content-wrapper">
              <div class="message-bubble loading-bubble">
                <span class="loading-dot"></span>
                <span class="loading-dot"></span>
                <span class="loading-dot"></span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 聊天输入框（仅在 AI 咨询模式显示） -->
      <div v-if="currentMode === 'consultation' && !showFeatureMenu" class="chat-footer">
        <input
          v-model="currentMessage"
          @keyup.enter="sendMessage"
          type="text"
          class="chat-input"
          placeholder="输入您的健康问题..."
          :disabled="isLoading"
        />
        <button 
          @click="sendMessage" 
          class="send-btn"
          :disabled="isLoading || !currentMessage.trim()"
        >
          <span v-if="!isLoading">发送</span>
          <span v-else class="sending-spinner"></span>
        </button>
      </div>
    </div>
  </div>

</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watchEffect, computed } from 'vue';
import { useAIConsult } from '../composables/useAIConsult';
import { submitQuestionnaireToAI } from '../api/questionnaireApi';
import { useUserStore } from '../stores/userStore';

// 精灵位置
const position = reactive({ x: window.innerWidth - 150, y: window.innerHeight - 150 });
const isDragging = ref(false);
const wasDraggedRecently = ref(false);
const showChat = ref(false);
const showMenu = ref(false);
const showFeatureMenu = ref(true); // 显示功能菜单
const currentMode = ref(''); // 当前模式：relaxation, questionnaire, consultation
const dragOffset = reactive({ x: 0, y: 0 });
const mousePosition = reactive({ x: 0, y: 0 });
const hoveredItemIndex = ref(-1);

// 聊天标题和图标
const currentChatTitle = ref('');
const currentChatIcon = ref('');

// 放松练习相关状态
const selectedRelaxationType = ref(null);
const selectedDuration = ref(5);
const isPracticing = ref(false);
const countdownSeconds = ref(0);
let countdownTimer = null;

// 问卷调查相关状态
const selectedQuestionnaire = ref(null);
const questionnaireAnswers = ref({});
const isSubmitting = ref(false);
const showAnalysisResult = ref(false);
const analysisResult = ref('');

// 用户信息
const userStore = useUserStore();

// 放松练习配置
const relaxationConfig = {
  breathing: { minDuration: 3, maxDuration: 15 },
  meditation: { minDuration: 5, maxDuration: 30 },
  muscle_relax: { minDuration: 10, maxDuration: 20 }
};

// 问卷题目（示例：压力测试）
const questionnaireQuestions = [
  {
    text: '1. 您最近是否经常感到紧张或焦虑？',
    options: ['从不', '偶尔', '有时', '经常', '总是']
  },
  {
    text: '2. 您是否难以控制自己的担忧情绪？',
    options: ['从不', '偶尔', '有时', '经常', '总是']
  },
  {
    text: '3. 您是否对很多事情过分担忧？',
    options: ['从不', '偶尔', '有时', '经常', '总是']
  },
  {
    text: '4. 您是否难以放松下来？',
    options: ['从不', '偶尔', '有时', '经常', '总是']
  },
  {
    text: '5. 您是否因为不安而无法静坐？',
    options: ['从不', '偶尔', '有时', '经常', '总是']
  },
  {
    text: '6. 您是否容易烦恼或急躁？',
    options: ['从不', '偶尔', '有时', '经常', '总是']
  }
];

// 功能菜单选项
const featureMenuItems = [
  {
    id: 'breathing',
    label: '深呼吸练习',
    icon: '🌬️',
    description: '4-7-8 呼吸法，帮助放松身心',
    type: 'relaxation'
  },
  {
    id: 'meditation',
    label: '冥想引导',
    icon: '🧘',
    description: '5 分钟正念冥想，减轻压力',
    type: 'relaxation'
  },
  {
    id: 'muscle_relax',
    label: '肌肉放松',
    icon: '💆',
    description: '渐进式肌肉放松训练',
    type: 'relaxation'
  },
  {
    id: 'stress_test',
    label: '压力测试',
    icon: '📊',
    description: '评估您当前的压力水平',
    type: 'questionnaire'
  },
  {
    id: 'anxiety_test',
    label: '焦虑自测',
    icon: '😰',
    description: 'GAD-7 焦虑症筛查量表',
    type: 'questionnaire'
  },
  {
    id: 'depression_test',
    label: '抑郁自测',
    icon: '😔',
    description: 'PHQ-9 抑郁症筛查量表',
    type: 'questionnaire'
  },
  {
    id: 'sleep_quality',
    label: '睡眠质量评估',
    icon: '😴',
    description: '匹兹堡睡眠质量指数',
    type: 'questionnaire'
  },
  {
    id: 'consultation',
    label: 'AI 心理咨询',
    icon: '🤖',
    description: '与 AI 心理咨询师对话',
    type: 'consultation'
  }
];

// 菜单选项
const menuItems = [
  { id: 'relaxation', label: '放松练习', icon: '🧘', action: 'relaxation' },
  { id: 'questionnaire', label: '咨询问卷', icon: '📋', action: 'questionnaire' },
  { id: 'consultation', label: 'AI 咨询', icon: '🤖', action: 'consultation' }
];

// 计算眼睛跟随鼠标的偏移量
const eyesStyle = computed(() => {
  if (!showMenu.value) return {};
  
  const elfCenterX = position.x + 35;
  const elfCenterY = position.y + 35;
  const dx = mousePosition.x - elfCenterX;
  const dy = mousePosition.y - elfCenterY;
  
  // 限制最大偏移距离
  const maxOffset = 8;
  const distance = Math.sqrt(dx * dx + dy * dy);
  const scale = Math.min(1, maxOffset / distance);
  
  const offsetX = dx * scale * 0.15;
  const offsetY = dy * scale * 0.15;
  
  return {
    transform: `translate(${offsetX}px, ${offsetY}px)`
  };
});

// 手臂样式
const armStyle = computed(() => ({
  transform: `rotate(${armAngle.value}deg)`
}));

// 过滤后的功能菜单项（根据当前模式）
const filteredFeatureItems = computed(() => {
  if (currentMode.value === 'relaxation') {
    return featureMenuItems.filter(item => item.type === 'relaxation');
  } else if (currentMode.value === 'questionnaire') {
    return featureMenuItems.filter(item => item.type === 'questionnaire');
  }
  return featureMenuItems;
});

// 问卷是否完成
const isQuestionnaireComplete = computed(() => {
  const questionsCount = questionnaireQuestions.length;
  // 使用 Object.values 并过滤掉 undefined 的值
  const answeredCount = Object.values(questionnaireAnswers.value).filter(v => v !== undefined && v !== null).length;
  console.log('问卷状态:', { questionsCount, answeredCount, answers: questionnaireAnswers.value });
  return answeredCount === questionsCount;
});

// 格式化倒计时
const formatCountdown = computed(() => {
  const minutes = Math.floor(countdownSeconds.value / 60);
  const seconds = countdownSeconds.value % 60;
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
});

// 当前练习提示
const currentPracticeTip = computed(() => {
  if (!selectedRelaxationType.value || !isPracticing.value) return '';
  
  const tips = {
    breathing: ['吸气 4 秒', '屏息 7 秒', '呼气 8 秒'],
    meditation: ['放松身心', '专注呼吸', '保持正念'],
    muscle_relax: ['收紧肌肉', '保持 5 秒', '慢慢放松']
  };
  
  const typeTips = tips[selectedRelaxationType.value.id] || [];
  // 根据剩余时间切换提示，每 3 秒切换一次
  const index = Math.floor((countdownSeconds.value / 3) % typeTips.length);
  return typeTips[index] || '放松练习';
});

// 监听时长选择，确保不超过最大值
watchEffect(() => {
  if (selectedRelaxationType.value && selectedDuration.value) {
    const config = relaxationConfig[selectedRelaxationType.value.id];
    if (config && selectedDuration.value > config.maxDuration) {
      // 只在超过最大值时修正
      selectedDuration.value = config.maxDuration;
    }
  }
});

// 使用 AI 咨询 composable
const { currentMessage, chatMessages, isLoading, sendMessage, clearChatHistory, loadChatHistory } = useAIConsult();

// 组件挂载时加载聊天记录
onMounted(() => {
    // 等待用户数据加载完成
    setTimeout(() => {
        if (userStore.userData && userStore.userData.userId) {
            loadChatHistory();
        } else {
            console.warn('⚠️ 用户数据未准备好，延迟加载聊天记录');
            // 如果用户数据还没准备好，等待 watchEffect 来处理
        }
    }, 100);
});

// 开始拖动
const startDrag = (e) => {
  isDragging.value = true;
  dragOffset.x = e.clientX - position.x;
  dragOffset.y = e.clientY - position.y;
  
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
};

// 拖动中
const onDrag = (e) => {
  if (!isDragging.value) return;
  
  const newX = e.clientX - dragOffset.x;
  const newY = e.clientY - dragOffset.y;
  
  // 限制在视口范围内
  const maxX = window.innerWidth - 80;
  const maxY = window.innerHeight - 80;
  
  position.x = Math.max(0, Math.min(newX, maxX));
  position.y = Math.max(0, Math.min(newY, maxY));
};

// 停止拖动
const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
};

// 处理点击事件
const handleClick = (e) => {
  // 检查是否是拖拽操作
  if (!isDragging.value && !wasDraggedRecently.value) {
    showMenu.value = !showMenu.value;
    if (showMenu.value) {
      // 添加全局鼠标移动监听
      document.addEventListener('mousemove', trackMousePosition);
    } else {
      document.removeEventListener('mousemove', trackMousePosition);
    }
  }
};

// 跟踪鼠标位置
const trackMousePosition = (e) => {
  mousePosition.x = e.clientX;
  mousePosition.y = e.clientY;
};

// 计算环绕菜单项的位置（改为水平排列，根据精灵位置决定左右）
const getOrbitItemStyle = (index) => {
  const baseDistance = 70; // 基础距离
  const horizontalSpacing = 68; // 水平间距
  
  // 根据精灵在屏幕的位置决定功能球在左边还是右边
  const elfCenterX = position.x + 35;
  const screenWidth = window.innerWidth;
  const isOnLeft = elfCenterX < screenWidth / 2;
  
  // 精灵在左边 → 功能球在右边（正方向）
  // 精灵在右边 → 功能球在左边（负方向）
  const direction = isOnLeft ? 1 : -1;
  
  // 精灵在右边时增加额外间距，避免功能球太靠近精灵
  const extraSpacing = isOnLeft ? 0 : 40;
  
  // 所有功能球都在同一侧水平排列
  const x = direction * (baseDistance + extraSpacing + (index * horizontalSpacing));
  const y = 0; // 保持同一水平线
  
  const isHovered = index === hoveredItemIndex.value;
  const hoverScale = isHovered ? 1.1 : 1;
  
  // 计算延迟时间，让功能球依次滑出
  const delay = index * 0.08; // 每个功能球延迟 80ms
  
  return {
    transitionDelay: `${delay}s`,
    transform: showMenu.value ? `translate(${x}px, ${y}px) scale(${hoverScale})` : 'scale(0.2) translate(0, 0)',
    opacity: showMenu.value ? 1 : 0
  };
};

// 悬停菜单项
const hoverMenuItem = (index) => {
  hoveredItemIndex.value = index;
};

// 离开菜单项
const leaveMenuItem = () => {
  hoveredItemIndex.value = -1;
};

// 选择菜单项
const selectMenuItem = (item) => {
  showMenu.value = false;
  document.removeEventListener('mousemove', trackMousePosition);
  
  switch (item.action) {
    case 'relaxation':
      // 打开放松练习菜单
      currentMode.value = 'relaxation';
      currentChatTitle.value = '放松练习';
      currentChatIcon.value = '🧘';
      showChat.value = true;
      showFeatureMenu.value = true;
      break;
    case 'questionnaire':
      // 打开咨询问卷菜单
      currentMode.value = 'questionnaire';
      currentChatTitle.value = '心理咨询问卷';
      currentChatIcon.value = '📋';
      showChat.value = true;
      showFeatureMenu.value = true;
      break;
    case 'consultation':
      // 直接打开 AI 咨询
      currentMode.value = 'consultation';
      currentChatTitle.value = '健康顾问';
      currentChatIcon.value = '🤖';
      showChat.value = true;
      showFeatureMenu.value = false;
      nextTick(() => {
        scrollToBottom();
      });
      break;
  }
};

// 选择功能菜单项
const selectFeature = (feature) => {
  if (feature.type === 'relaxation') {
    // 放松练习：设置配置并显示时长选择
    const config = relaxationConfig[feature.id];
    // 创建包含时长配置的对象
    selectedRelaxationType.value = {
      ...feature,
      minDuration: config.minDuration,
      maxDuration: config.maxDuration
    };
    // 建议时长设置为范围中间值（向上取整）
    selectedDuration.value = Math.ceil((config.minDuration + config.maxDuration) / 2);
    currentChatTitle.value = feature.label;
    currentChatIcon.value = feature.icon;
    currentMode.value = 'relaxation';
    showFeatureMenu.value = false;
  } else if (feature.type === 'questionnaire') {
    // 问卷调查：重置状态并显示问卷
    selectedQuestionnaire.value = feature;
    questionnaireAnswers.value = {};
    showAnalysisResult.value = false;
    analysisResult.value = '';
    currentChatTitle.value = feature.label;
    currentChatIcon.value = feature.icon;
    currentMode.value = 'questionnaire';
    showFeatureMenu.value = false;
  } else if (feature.type === 'consultation') {
    // AI 咨询
    currentChatTitle.value = 'AI 心理咨询';
    currentChatIcon.value = '🤖';
    currentMode.value = 'consultation';
    showFeatureMenu.value = false;
    nextTick(() => {
      scrollToBottom();
    });
  }
};

// 返回菜单
const backToMenu = () => {
  showFeatureMenu.value = true;
  selectedRelaxationType.value = null;
  selectedQuestionnaire.value = null;
  isPracticing.value = false;
  showAnalysisResult.value = false;
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
};

// 开始放松练习
const startRelaxation = () => {
  isPracticing.value = true;
  countdownSeconds.value = selectedDuration.value * 60;
  
  // 启动倒计时
  countdownTimer = setInterval(() => {
    if (countdownSeconds.value > 0) {
      countdownSeconds.value--;
    } else {
      stopPractice();
    }
  }, 1000);
};

// 停止练习
const stopPractice = () => {
  isPracticing.value = false;
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
  countdownSeconds.value = 0;
};

// 提交问卷
const submitQuestionnaire = async () => {
  console.log('开始提交问卷...');
  console.log('问卷是否完成:', isQuestionnaireComplete.value);
  
  if (!isQuestionnaireComplete.value) {
    console.log('问卷未完成，拒绝提交');
    return;
  }
  
  console.log('问卷已完成，开始 AI 分析...');
  isSubmitting.value = true;
  
  try {
    // 计算得分
    let totalScore = 0;
    Object.values(questionnaireAnswers.value).forEach(answer => {
      totalScore += parseInt(answer);
    });
    
    console.log('总分:', totalScore);
    
    // 准备答案数据（包含题目和答案）
    const answersData = questionnaireQuestions.map((q, index) => ({
      question: q.text,
      answer: q.options[questionnaireAnswers.value[index]],
      score: parseInt(questionnaireAnswers.value[index])
    }));
    
    // 调用 AI 生成分析报告
    console.log('正在调用 AI 服务生成报告...');
    const report = await submitQuestionnaireToAI(
      userStore.userData.userId,
      selectedQuestionnaire.value?.label || '心理健康评估',
      answersData,
      totalScore
    );
    
    console.log('AI 分析报告已生成');
    analysisResult.value = report;
    showAnalysisResult.value = true;
  } catch (error) {
    console.error('问卷分析失败:', error);
    // 如果 AI 分析失败，使用本地生成作为备选
    let totalScore = 0;
    Object.values(questionnaireAnswers.value).forEach(answer => {
      totalScore += parseInt(answer);
    });
    const report = generateAnalysisReport(totalScore);
    analysisResult.value = report;
    showAnalysisResult.value = true;
  } finally {
    isSubmitting.value = false;
  }
};

// 生成分析报告
const generateAnalysisReport = (score) => {
  const maxScore = questionnaireQuestions.length * 4; // 5 个选项，最高 4 分
  const percentage = (score / maxScore) * 100;
  
  let level = '';
  let suggestions = [];
  
  if (percentage <= 25) {
    level = '<strong style="color: #4CAF50;">心理健康状况良好</strong>';
    suggestions = [
      '继续保持积极乐观的心态',
      '定期进行放松练习',
      '维持良好的作息和运动习惯'
    ];
  } else if (percentage <= 50) {
    level = '<strong style="color: #FFC107;">轻度压力</strong>';
    suggestions = [
      '建议每天进行深呼吸练习',
      '适当增加休闲活动时间',
      '与亲友多交流沟通'
    ];
  } else if (percentage <= 75) {
    level = '<strong style="color: #FF9800;">中度焦虑</strong>';
    suggestions = [
      '建议寻求专业心理咨询',
      '每天进行冥想放松',
      '减少咖啡因摄入',
      '保证充足睡眠'
    ];
  } else {
    level = '<strong style="color: #F44336;">重度焦虑，建议尽快就医</strong>';
    suggestions = [
      '立即预约心理咨询师或精神科医生',
      '避免独处，多与家人朋友在一起',
      '暂停高强度工作',
      '遵医嘱进行治疗'
    ];
  }
  
  return `<div class="analysis-section"><h4>评估结果</h4><p>您的总分为 ${score} 分（满分${maxScore}分），属于${level}。</p></div><div class="analysis-section"><h4>建议方案</h4><ul>${suggestions.map(s => `<li>${s}</li>`).join('')}</ul></div><div class="analysis-section"><h4>温馨提示</h4><p>本测试结果仅供参考，不能作为医学诊断依据。如有严重不适，请及时就医。</p></div>`;
};

// 重新测试
const retakeQuestionnaire = () => {
  questionnaireAnswers.value = {};
  showAnalysisResult.value = false;
  analysisResult.value = '';
};

// 处理鼠标按下
const handleMouseDown = (e) => {
  isDragging.value = false;
  wasDraggedRecently.value = false;
  dragOffset.x = e.clientX - position.x;
  dragOffset.y = e.clientY - position.y;
  
  // 直接添加全局事件监听，确保捕获所有鼠标移动
  document.addEventListener('mousemove', handleGlobalMouseMove);
  document.addEventListener('mouseup', handleGlobalMouseUp);
};

// 全局鼠标移动处理
const handleGlobalMouseMove = (e) => {
  const threshold = 3; // 像素阈值
  const deltaX = Math.abs(e.clientX - (position.x + dragOffset.x));
  const deltaY = Math.abs(e.clientY - (position.y + dragOffset.y));
  
  if (deltaX > threshold || deltaY > threshold) {
    isDragging.value = true;
    
    // 直接更新位置，不使用 requestAnimationFrame
    const newX = e.clientX - dragOffset.x;
    const newY = e.clientY - dragOffset.y;
    
    // 限制在视口范围内
    const maxX = window.innerWidth - 80;
    const maxY = window.innerHeight - 80;
    
    position.x = Math.max(0, Math.min(newX, maxX));
    position.y = Math.max(0, Math.min(newY, maxY));
  }
};

// 全局鼠标松开处理
const handleGlobalMouseUp = () => {
  // 移除全局监听
  document.removeEventListener('mousemove', handleGlobalMouseMove);
  document.removeEventListener('mouseup', handleGlobalMouseUp);
  
  if (isDragging.value) {
    wasDraggedRecently.value = true;
    setTimeout(() => {
      wasDraggedRecently.value = false;
    }, 200);
  }
  isDragging.value = false;
};

// 关闭聊天框
const closeChat = () => {
  showChat.value = false;
  showFeatureMenu.value = true;
  currentMode.value = '';
};

// 滚动到消息底部
const scrollToBottom = () => {
  const container = document.querySelector('.messages-container');
  if (container) {
    setTimeout(() => {
      container.scrollTop = container.scrollHeight;
    }, 100);
  }
};

// 格式化时间
const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
};

// 格式化消息内容（支持 Markdown）
const formatMessage = (content) => {
  if (!content) return '';
  
  let formatted = content;
  
  // 转义 HTML 防止 XSS
  formatted = formatted.replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  
  // 处理标题 ### 到 ######
  formatted = formatted.replace(/^###### (.+)$/gm, '<h6>$1</h6>');
  formatted = formatted.replace(/^##### (.+)$/gm, '<h5>$1</h5>');
  formatted = formatted.replace(/^#### (.+)$/gm, '<h4>$1</h4>');
  formatted = formatted.replace(/^### (.+)$/gm, '<h3>$1</h3>');
  formatted = formatted.replace(/^## (.+)$/gm, '<h2>$1</h2>');
  formatted = formatted.replace(/^# (.+)$/gm, '<h1>$1</h1>');
  
  // 处理粗体 **text** - 只保留文字，去掉 ** 符号
  formatted = formatted.replace(/\*\*(.+?)\*\*/g, '$1');
  
  // 处理斜体 *text*
  formatted = formatted.replace(/\*(.+?)\*/g, '<em>$1</em>');
  
  // 智能表格解析函数
  const parseTableData = (rows) => {
    if (!rows || rows.length < 2) return null;
    
    try {
      // 检测主要分隔符类型
      let delimiter = '|';
      let pipeCount = rows.filter(r => (r.match(/\|/g) || []).length >= 2).length;
      let tabCount = rows.filter(r => r.includes('\t')).length;
      let spaceCount = rows.filter(r => / {2,}/.test(r)).length;
      
      // 决定使用哪种分隔符
      if (tabCount > pipeCount) {
        delimiter = '\t';
      } else if (spaceCount > pipeCount && spaceCount > tabCount) {
        delimiter = / {2,}/;
      } else {
        delimiter = '|';
      }
      
      // 统一处理所有行 - 移除首尾的 |
      const cleanRows = rows.map(row => {
        let cleaned = row.trim();
        if (typeof delimiter === 'string' && delimiter === '|') {
          // 移除开头和结尾的 |
          if (cleaned.startsWith('|')) cleaned = cleaned.substring(1);
          if (cleaned.endsWith('|')) cleaned = cleaned.substring(0, cleaned.length - 1);
        }
        return cleaned;
      });
      
      // 解析表头
      const headers = typeof delimiter === 'string' 
        ? cleanRows[0].split(delimiter).filter(c => c.trim()).map(h => h.trim())
        : cleanRows[0].split(delimiter).filter(c => c.trim()).map(h => h.trim());
      
      if (headers.length < 2) return null;
      
      // 解析数据行（跳过包含 --- 的分隔线和引用块）
      const dataRows = cleanRows.slice(1).filter(row => {
        const trimmed = row.trim();
        // 跳过空行
        if (!trimmed) return false;
        // 跳过 --- 分隔线
        if (/^-+$/.test(trimmed.replace(/\|/g, ''))) return false;
        // 跳过引用块标记
        if (trimmed.startsWith('>')) return false;
        return true;
      });
      
      // 如果没有有效的数据行，不解析为表格
      if (dataRows.length === 0) return null;
      
      // 判断是否为多列表格（超过 3 列）
      const isMultiColumn = headers.length > 3;
      
      if (isMultiColumn) {
        // 多列表格：使用分组列表展示
        let html = '<div class="multi-column-list">';
        
        dataRows.forEach((row, index) => {
          const cells = typeof delimiter === 'string'
            ? row.split(delimiter).filter(c => c.trim())
            : row.split(delimiter).filter(c => c.trim());
          
          html += '<div class="multi-column-group">';
          
          // 显示组标题（如周一、周二）
          if (cells.length > 0 && /[周一二三四五六日天]/.test(cells[0])) {
            html += `<div class="multi-column-title">${cells[0]}</div>`;
            // 从第二列开始显示内容
            for (let i = 1; i < cells.length && i < headers.length; i++) {
              if (cells[i] && cells[i].trim()) {
                html += `<div class="multi-column-item"><span class="multi-column-label">${headers[i]}</span><div class="multi-column-content">${cells[i].trim()}</div></div>`;
              }
            }
          } else {
            // 没有日期标识，按顺序显示所有列
            for (let i = 0; i < cells.length && i < headers.length; i++) {
              if (cells[i] && cells[i].trim()) {
                html += `<div class="multi-column-item"><span class="multi-column-label">${headers[i]}</span><div class="multi-column-content">${cells[i].trim()}</div></div>`;
              }
            }
          }
          
          html += '</div>';
        });
        
        html += '</div>';
        return html;
      } else {
        // 少列数表格：使用卡片式展示
        let html = '<div class="table-cards">';
        
        dataRows.forEach((row, index) => {
          const cells = typeof delimiter === 'string'
            ? row.split(delimiter).filter(c => c.trim())
            : row.split(delimiter).filter(c => c.trim());
          
          html += '<div class="table-card">';
          
          // 如果有多个列，显示为键值对
          if (cells.length === headers.length) {
            cells.forEach((cell, i) => {
              html += `<div class="table-card-item"><span class="table-card-label">${headers[i]}</span><span class="table-card-value">${cell.trim()}</span></div>`;
            });
          } else {
            // 否则只显示内容
            html += `<div class="table-card-content">${cells.join(' ')}</div>`;
          }
          
          html += '</div>';
        });
        
        html += '</div>';
        return html;
      }
    } catch (e) {
      console.error('表格解析失败:', e);
      return null;
    }
  };
  
  // 先处理表格（在换行之前）
  const lines = formatted.split('\n');
  let inTable = false;
  let tableRows = [];
  let resultLines = [];
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmedLine = line.trim();
    
    // 检查是否是表格行（以 | 开头和结尾，或者包含多个 tab/空格分隔的列）
    const isTableRow = (trimmedLine.startsWith('|') && trimmedLine.endsWith('|')) || 
                       (trimmedLine.split(/\t+/).length > 2) ||
                       (trimmedLine.split(/ {2,}/).length > 2);
    
    // 检查是否是表格分隔线（如 |---|---| 或 ---	---）
    const isTableDivider = /^\|[\s:-]+\|$/.test(trimmedLine) || 
                           /^[\s:-]+$/.test(trimmedLine.replace(/\|/g, ''));
    
    if (isTableRow && !isTableDivider && trimmedLine.length > 5) {
      if (!inTable) {
        inTable = true;
        tableRows = [trimmedLine];
      } else {
        // 如果当前行是表格行，添加到表格数据中
        tableRows.push(trimmedLine);
      }
    } else {
      if (inTable) {
        // 结束表格，构建 HTML
        if (tableRows.length >= 2) {
          // 尝试解析表格数据
          const parsedTable = parseTableData(tableRows);
          if (parsedTable) {
            resultLines.push(parsedTable);
          } else {
            resultLines.push(tableRows.join('<br/>'));
          }
        } else {
          resultLines.push(tableRows.join('<br/>'));
        }
        inTable = false;
        tableRows = [];
      }
      if (trimmedLine) {
        resultLines.push(line);
      }
    }
  }
  
  // 处理剩余的表格行
  if (inTable && tableRows.length > 0) {
    if (tableRows.length >= 2) {
      const parsedTable = parseTableData(tableRows);
      if (parsedTable) {
        resultLines.push(parsedTable);
      } else {
        resultLines.push(tableRows.join('<br/>'));
      }
    } else {
      resultLines.push(tableRows.join('<br/>'));
    }
  }
  
  formatted = resultLines.join('\n');
  
  // 处理换行<br>
  formatted = formatted.replace(/<br\s*\/?>/gi, '<br/>');
  formatted = formatted.replace(/\n/g, '<br/>');
  
  // 处理列表 - 或 *
  formatted = formatted.replace(/^\s*[-*]\s+(.+)$/gm, '<li>$1</li>');
  formatted = formatted.replace(/(<li>.+<\/li>\n?)+/g, '<ul>$&</ul>');
  
  // 处理数字列表
  formatted = formatted.replace(/^\s*\d+\.\s+(.+)$/gm, '<li>$1</li>');
  
  // 处理引用块
  formatted = formatted.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>');
  
  // 处理代码块 ```code```
  formatted = formatted.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');
  
  // 处理行内代码 `code`
  formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>');
  
  return formatted;
};

// 监听消息变化，自动滚动
watchEffect(() => {
  if (showChat.value && chatMessages.length > 0) {
    nextTick(() => {
      scrollToBottom();
    });
  }
});

// 清理 DOM 事件
onUnmounted(() => {
  document.removeEventListener('mousemove', handleGlobalMouseMove);
  document.removeEventListener('mouseup', handleGlobalMouseUp);
  document.removeEventListener('mousemove', trackMousePosition);
});
</script>

<style scoped>
.ai-spirit-container {
  position: fixed;
  cursor: grab;
  z-index: 9999;
  user-select: none;
}

.ai-spirit-container.is-dragging {
  cursor: grabbing;
  transition: none;
}

.ai-spirit-container:not(.is-dragging):hover {
  transform: scale(1.1);
}

.spirit-body {
  position: relative;
  width: 70px;
  height: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: transform 0.15s ease;
}

.spirit-head {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  position: relative;
  box-shadow: 
    0 8px 20px rgba(102, 126, 234, 0.4),
    inset 0 -4px 10px rgba(0, 0, 0, 0.2),
    inset 0 4px 10px rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

/* 护士帽 */
.nurse-hat {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 20px;
  background: white;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 3;
}

.hat-cross {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 16px;
  height: 16px;
  color: #e74c3c;
  font-size: 20px;
  font-weight: bold;
  line-height: 16px;
  text-align: center;
}

/* 精灵手臂 */
.spirit-arm {
  display: none;  /* 隐藏手臂 */
}

/* 环绕菜单 */
.orbit-menu {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  pointer-events: none;
}

.orbit-item {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 60px;
  height: 60px;
  padding: 8px;
  background: white;
  border-radius: 50%;
  box-shadow: 
    0 6px 24px rgba(102, 126, 234, 0.15),
    0 2px 6px rgba(102, 126, 234, 0.1);
  cursor: pointer;
  pointer-events: auto;
  transition: 
    transform 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94),
    opacity 0.35s ease,
    box-shadow 0.3s ease,
    background 0.3s ease;
  transform-origin: center center;
  /* 垂直居中：向上偏移自身高度的一半 */
  margin-top: -30px;
}

.orbit-item:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 
    0 12px 40px rgba(102, 126, 234, 0.4),
    0 4px 12px rgba(102, 126, 234, 0.2);
}

.orbit-item:hover .orbit-icon {
  transform: scale(1.25) rotate(8deg);
}

.orbit-item:hover .orbit-label {
  color: white;
}

.orbit-icon {
  font-size: 24px;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.orbit-label {
  font-size: 11px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  transition: color 0.3s ease;
}

/* 功能菜单样式 */
.feature-menu-container {
  padding: 20px;
  background: #f8f9fa;
}

.feature-menu-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);  /* 固定 3 列 */
  gap: 20px;
  max-height: 500px;
  overflow-y: auto;
  justify-items: center;  /* 居中显示 */
}

.feature-menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 140px;
  height: 140px;
  padding: 20px;
  background: white;
  border-radius: 50%;  /* 圆形 */
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
}

.feature-menu-item:hover {
  transform: scale(1.08);  /* 圆形放大效果 */
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.feature-icon {
  font-size: 42px;  /* 调大图标 */
  margin-bottom: 8px;
  transition: transform 0.3s ease;
}

.feature-menu-item:hover .feature-icon {
  transform: scale(1.15);
}

.feature-label {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 4px;
  text-align: center;
  line-height: 1.2;
}

.feature-desc {
  font-size: 11px;  /* 调小描述文字 */
  color: #86868b;
  text-align: center;
  line-height: 1.3;
  max-width: 120px;  /* 限制宽度 */
}

/* 放松练习时长选择样式 */
.relaxation-duration-container {
  padding: 30px 20px;
  text-align: center;
}

.duration-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.duration-icon {
  font-size: 42px;
}

.duration-header h3 {
  margin: 0;
  font-size: 20px;
  color: #1d1d1f;
}

.duration-desc {
  color: #86868b;
  margin-bottom: 30px;
}

.slider-wrapper {
  margin: 30px 0;
}

.duration-slider {
  width: 100%;
  height: 8px;
  border-radius: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  outline: none;
  -webkit-appearance: none;
}

.duration-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  transition: transform 0.2s;
}

.duration-slider::-webkit-slider-thumb:hover {
  transform: scale(1.2);
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 14px;
  color: #86868b;
}

.slider-labels .current-duration {
  font-weight: 600;
  color: #667eea;
  font-size: 16px;
}

.suggestion-text {
  font-size: 14px;
  color: #667eea;
  margin-top: 12px;
}

/* 问卷调查样式 */
.questionnaire-container {
  padding: 20px;
}

.questionnaire-header {
  text-align: center;
  margin-bottom: 20px;
}

.questionnaire-icon {
  font-size: 42px;
  display: block;
  margin-bottom: 8px;
}

.questionnaire-header h3 {
  margin: 0;
  font-size: 20px;
  color: #1d1d1f;
}

.questionnaire-desc {
  text-align: center;
  color: #86868b;
  margin-bottom: 24px;
}

.questionnaire-form {
  max-height: 500px;
  overflow-y: auto;
}

.question-item {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
}

.question-text {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 12px;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.option-label:hover {
  background: rgba(102, 126, 234, 0.1);
}

.option-radio {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.option-text {
  font-size: 14px;
  color: #333;
}

/* 倒计时样式 */
.practice-countdown-container {
  padding: 40px 20px;
  text-align: center;
}

.countdown-display {
  margin-bottom: 30px;
}

.countdown-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: breathe 4s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.countdown-timer {
  font-size: 56px;
  font-weight: 700;
  color: #667eea;
  font-family: 'Courier New', monospace;
  margin-bottom: 20px;
}

.countdown-tip {
  font-size: 18px;
  color: #86868b;
  animation: fadeInOut 3s ease-in-out infinite;
}

@keyframes fadeInOut {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.btn-stop {
  background: #ff3b30;
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-stop:hover {
  background: #ff2d23;
  transform: scale(1.05);
}

/* 分析结果样式 */
.analysis-result-container {
  padding: 24px;
}

.result-header {
  text-align: center;
  margin-bottom: 24px;
}

.result-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.result-header h3 {
  margin: 0;
  font-size: 22px;
  color: #1d1d1f;
}

.result-content {
  background: #f8f9fa;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 24px;
}

.analysis-section {
  margin-bottom: 20px;
}

.analysis-section h4 {
  color: #667eea;
  margin-bottom: 12px;
  font-size: 16px;
}

.analysis-section p {
  color: #333;
  line-height: 1.6;
}

.analysis-section ul {
  margin: 8px 0;
  padding-left: 20px;
}

.analysis-section li {
  color: #333;
  line-height: 1.8;
  margin-bottom: 6px;
}

/* 按钮通用样式 */
.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}

.btn-back,
.btn-ok,
.btn-start,
.btn-submit {
  padding: 12px 28px;
  border-radius: 20px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.btn-back {
  background: #f5f5f7;
  color: #1d1d1f;
}

.btn-back:hover {
  background: #e8e8ed;
  transform: scale(1.05);
}

.btn-start,
.btn-submit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-start:hover,
.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spirit-tail {
  width: 40px;
  height: 35px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  position: relative;
  margin-top: -5px;
  box-shadow: 
    0 6px 15px rgba(102, 126, 234, 0.3),
    inset 0 -3px 8px rgba(0, 0, 0, 0.15);
  z-index: 1;
  animation: tailFloat 3s ease-in-out infinite;
}

@keyframes tailFloat {
  0%, 100% {
    transform: scaleY(1);
  }
  50% {
    transform: scaleY(1.1);
  }
}

.spirit-body.is-dragging .spirit-tail {
  animation: none;
  transform: scaleY(0.9);
}



.spirit-head {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  position: relative;
  box-shadow: 
    0 8px 20px rgba(102, 126, 234, 0.4),
    inset 0 -4px 10px rgba(0, 0, 0, 0.2),
    inset 0 4px 10px rgba(255, 255, 255, 0.3);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.spirit-eyes {
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
  transition: transform 0.15s ease;
}

.eye {
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
  position: relative;
  animation: blink 4s ease-in-out infinite;
}

.eye::after {
  content: '';
  position: absolute;
  width: 4px;
  height: 4px;
  background: #333;
  border-radius: 50%;
  top: 2px;
  left: 2px;
}

@keyframes blink {
  0%, 96%, 100% {
    height: 8px;
  }
  98% {
    height: 1px;
  }
}

.spirit-mouth {
  width: 10px;
  height: 5px;
  background: white;
  border-radius: 0 0 10px 10px;
  opacity: 0.9;
  margin-top: 8px;  /* 往下移动（从 6px → 8px）*/
}

.spirit-glow {
  position: absolute;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  animation: glow 2s ease-in-out infinite;
  z-index: 3;
}

@keyframes glow {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

.spirit-tooltip {
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.ai-spirit-container:hover .spirit-tooltip {
  opacity: 1;
}

/* 聊天框样式 */
.chat-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.chat-dialog {
  width: 95%;
  max-width: 600px;
  height: 700px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-icon {
  font-size: 28px;
}

.chat-title {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 600;
}

.chat-close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 28px;
  cursor: pointer;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.3s ease;
}

.chat-close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  background: #f8f9fa;
}

.messages-container {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  font-size: 24px;
  flex-shrink: 0;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}

.message-item.user .message-content-wrapper {
  align-items: flex-end;
}

.message-bubble {
  background: white;
  padding: 16px 20px;
  border-radius: 16px;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  line-height: 1.8;
  word-wrap: break-word;
  font-size: 15px;
  color: #2c3e50;
}

.message-bubble h1,
.message-bubble h2,
.message-bubble h3,
.message-bubble h4,
.message-bubble h5,
.message-bubble h6 {
  margin: 20px 0 12px;
  font-weight: 700;
  line-height: 1.4;
  color: #1a1a1a;
}

.message-bubble h1 { 
  font-size: 1.6em;
  border-bottom: 2px solid #667eea;
  padding-bottom: 8px;
  margin-top: 24px;
}

.message-bubble h2 { 
  font-size: 1.4em;
  border-bottom: 1px solid #e1e4e8;
  padding-bottom: 6px;
}

.message-bubble h3 { 
  font-size: 1.25em;
  color: #667eea;
}

.message-bubble h4 { font-size: 1.1em; }
.message-bubble h5 { font-size: 1em; }
.message-bubble h6 { font-size: 0.9em; }

.message-bubble h1:first-child,
.message-bubble h2:first-child,
.message-bubble h3:first-child {
  margin-top: 0;
}

.message-bubble strong {
  font-weight: 700;
  color: #1a1a1a;
}

.message-bubble em {
  font-style: italic;
  color: #555;
}

.message-bubble ul,
.message-bubble ol {
  margin: 12px 0;
  padding-left: 24px;
}

.message-bubble li {
  margin: 6px 0;
  line-height: 1.7;
  position: relative;
}

.message-bubble blockquote {
  margin: 16px 0;
  padding: 14px 18px;
  border-left: 4px solid #667eea;
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.05) 0%, transparent 100%);
  border-radius: 6px;
  color: #444;
  font-size: 14px;
}

.message-bubble pre {
  margin: 16px 0;
  padding: 16px;
  background: #f6f8fa;
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #e1e4e8;
}

.message-bubble code {
  font-family: 'Consolas', 'Monaco', monospace;
  background: #f0f4ff;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 0.9em;
  color: #476582;
}

.message-bubble pre code {
  background: none;
  padding: 0;
  color: inherit;
}

.message-bubble table {
  width: 100%;
  margin: 16px 0;
  border-collapse: collapse;
  font-size: 14px;
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.message-bubble thead {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-bubble th,
.message-bubble td {
  padding: 14px 16px;
  border: 1px solid #e1e4e8;
  text-align: left;
}

.message-bubble th {
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.3px;
}

.message-bubble tbody tr {
  transition: all 0.2s ease;
}

.message-bubble tbody tr:nth-child(even) {
  background: #fafbfc;
}

.message-bubble tbody tr:hover {
  background: #f0f4ff;
  transform: translateX(2px);
}

.message-bubble td:first-child {
  font-weight: 600;
  color: #1a1a1a;
}

/* 卡片式表格样式 */
.message-bubble .table-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 16px 0;
}

.message-bubble .table-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-left: 4px solid #667eea;
  transition: all 0.3s ease;
}

.message-bubble .table-card:hover {
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
  transform: translateY(-2px);
}

.message-bubble .table-card-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
  line-height: 1.6;
}

.message-bubble .table-card-item:last-child {
  margin-bottom: 0;
}

.message-bubble .table-card-label {
  font-weight: 700;
  color: #667eea;
  min-width: 80px;
  font-size: 14px;
  flex-shrink: 0;
}

.message-bubble .table-card-value {
  color: #2c3e50;
  font-size: 15px;
  line-height: 1.7;
  flex: 1;
}

.message-bubble .table-card-content {
  color: #2c3e50;
  font-size: 15px;
  line-height: 1.7;
}

/* 多列分组列表样式 */
.message-bubble .multi-column-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin: 20px 0;
}

.message-bubble .multi-column-group {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.08);
  border-left: 5px solid #667eea;
  transition: all 0.3s ease;
}

.message-bubble .multi-column-group:hover {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.15);
  transform: translateX(4px);
}

.message-bubble .multi-column-title {
  font-size: 18px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e1e4e8;
  text-align: center;
}

.message-bubble .multi-column-item {
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  transition: background 0.2s ease;
}

.message-bubble .multi-column-item:last-child {
  margin-bottom: 0;
}

.message-bubble .multi-column-item:hover {
  background: rgba(240, 244, 255, 0.8);
}

.message-bubble .multi-column-label {
  display: block;
  font-weight: 700;
  color: #667eea;
  font-size: 14px;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.message-bubble .multi-column-content {
  color: #2c3e50;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.message-bubble hr {
  border: none;
  height: 2px;
  background: linear-gradient(90deg, #667eea 0%, rgba(102, 126, 234, 0) 100%);
  margin: 24px 0;
}

.message-bubble br {
  line-height: 1.8;
}

.message-bubble p {
  margin: 12px 0;
  line-height: 1.8;
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  padding: 0 4px;
}

.loading-bubble {
  display: flex;
  gap: 4px;
  padding: 16px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-footer {
  display: flex;
  gap: 12px;
  padding: 20px;
  background: white;
  border-top: 1px solid #eee;
}

.chat-input {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 24px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s ease;
}

.chat-input:focus {
  border-color: #667eea;
}

.chat-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.send-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.sending-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-spirit-container {
    transform: scale(0.8);
  }
  
  .spirit-body {
    width: 50px;
    height: 50px;
  }
  
  .chat-dialog {
    width: 95%;
    height: 70vh;
  }
  
  .message-content-wrapper {
    max-width: 80%;
  }
}
</style>
