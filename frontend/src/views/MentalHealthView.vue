<template>
  <div class="mental-health-wrapper">
    <header class="chat-header">
      <div class="header-inner">
        <div class="header-left">
          <div class="ai-avatar">🧠</div>
          <div class="header-text">
            <h1>心理陪伴助手</h1>
            <p class="status">💚 在线为您提供心理支持</p>
          </div>
        </div>
        <div class="header-right">
          <div class="dropdown-wrapper">
            <button 
              ref="menuButtonRef"
              class="menu-btn" 
              @click.stop="toggleMenu"
              :disabled="loading"
            >
              📋 功能菜单
            </button>
            
            <div 
              v-show="showMenu" 
              class="dropdown-menu"
              @click.stop
            >
              <div 
                v-for="option in menuOptions" 
                :key="option.id"
                class="menu-item"
                @click.stop="handleMenuOption(option)"
              >
                <span class="menu-icon">{{ option.icon }}</span>
                <span class="menu-label">{{ option.label }}</span>
              </div>
            </div>
          </div>
          
          <button class="stats-btn" @click="showChatHistoryModal">
            💬 {{ messages.length }} 条记录
          </button>
        </div>
      </div>
    </header>

    <div v-if="showHistoryModal" class="history-modal-overlay" @click="closeHistoryModal">
      <div class="history-modal" @click.stop>
        <div class="history-modal-header">
          <h3>📜 聊天记录 ({{ userMessages.length }} 条询问)</h3>
          <button class="close-modal-btn" @click="closeHistoryModal">×</button>
        </div>
        <div class="history-toolbar">
          <input 
            v-model="searchKeyword" 
            placeholder="🔍 搜索聊天记录..." 
            class="search-input"
          />
          <div class="toolbar-actions">
            <button 
              v-if="!isBatchDeleteMode" 
              class="batch-btn" 
              @click="startBatchDelete"
              :disabled="userMessages.length === 0"
            >
              🗑️ 批量删除
            </button>
            <template v-else>
              <button class="select-all-btn" @click="toggleSelectAllHistory">
                {{ isAllHistorySelected ? '取消全选' : '☑️ 全选' }}
              </button>
              <button 
                class="delete-selected-btn" 
                @click="deleteSelectedHistory"
                :disabled="selectedHistoryItems.size === 0"
              >
                🗑️ 删除 ({{ selectedHistoryItems.size }})
              </button>
              <button class="cancel-btn" @click="cancelBatchDelete">取消</button>
            </template>
          </div>
        </div>
        <div class="history-list">
          <div 
            v-for="(msg, index) in filteredUserMessages" 
            :key="index"
            class="history-item"
            :class="{ 'selected': selectedHistoryItems.has(msg.originalIndex) }"
            @click="handleHistoryItemClick(msg, $event)"
          >
            <div v-if="isBatchDeleteMode" class="history-checkbox">
              <input 
                type="checkbox" 
                :checked="selectedHistoryItems.has(msg.originalIndex)"
                @click.stop
                @change="toggleHistorySelection(msg.originalIndex)"
              />
            </div>
            <div class="history-index">#{{ msg.originalIndex + 1 }}</div>
            <div class="history-preview">{{ getPreviewText(msg.content) }}</div>
            <div class="history-time">{{ formatTime(msg.timestamp) }}</div>
            <button 
              v-if="!isBatchDeleteMode" 
              class="history-delete-btn" 
              @click.stop="deleteHistoryItem(msg.originalIndex)"
              title="删除"
            >
              🗑️ 删除
            </button>
          </div>
          <div v-if="filteredUserMessages.length === 0" class="no-results">
            🔍 暂无匹配的聊天记录
          </div>
        </div>
      </div>
    </div>

    <main class="chat-main">
      <div class="messages-container" ref="messagesRef">
        <div class="welcome-section">
          <div class="welcome-card">
            <h3>👋 您好，我是您的心理陪伴助手</h3>
            <p>我可以为您提供专业的心理健康支持和建议</p>
            <div class="quick-actions">
              <div class="action-item" @click="quickAsk('如何缓解压力和焦虑？')">😌 缓解压力</div>
              <div class="action-item" @click="quickAsk('如何改善睡眠质量？')">😴 改善睡眠</div>
              <div class="action-item" @click="quickAsk('如何处理人际关系问题？')">🤝 人际关系</div>
              <div class="action-item" @click="startAssessment">📊 心理评估</div>
              <div class="action-item" @click="showRelaxationExercises">🧘 放松练习</div>
            </div>
          </div>
        </div>

        <div 
          v-for="(msg, index) in messages" 
          :key="index" 
          :id="'message-' + index"
          class="message-row"
          :class="{ 'from-user': msg.type === 'user', 'from-ai': msg.type === 'bot', 'highlighted': highlightedIndex === index }"
        >
          <div class="message-content">
            <div class="message-bubble" :class="{ 'user-bubble': msg.type === 'user', 'ai-bubble': msg.type === 'bot' }">
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div class="message-footer">
                <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
                <button class="delete-btn" @click="deleteMessage(index)" title="删除此条消息">🗑️ 删除</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-section">
          <div class="message-row from-ai">
            <div class="message-content">
              <div class="message-bubble ai-bubble typing-indicator">
                <div class="typing-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <div class="message-text">🤔 心理陪伴助手正在思考中...</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <footer class="input-footer">
        <div class="input-wrapper">
          <textarea
            v-model="userInput"
            placeholder="💭 请输入您的心理问题或感受..."
            class="message-input"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact="addLineBreak"
            :disabled="loading"
          ></textarea>
          <button 
            class="send-button" 
            @click="sendMessage"
            :disabled="!userInput.trim() || loading"
          >
            {{ loading ? '⏳ 发送中' : '📤 发送' }}
          </button>
        </div>
        <div class="input-tip">
          ⌨️ 按 Enter 发送，Shift + Enter 换行 | 💚 您可以随时与我分享您的感受和困惑
        </div>
      </footer>
    </main>

    <div v-if="showAssessment" class="modal-overlay" @click="closeAssessment">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>📊 心理健康评估</h3>
          <button @click="closeAssessment" class="close-button">×</button>
        </div>
        <div class="modal-body">
          <div class="assessment-questions">
            <div 
              v-for="(question, index) in assessmentQuestions" 
              :key="index" 
              class="question-item"
            >
              <p class="question-text">{{ question.question }}</p>
              <div class="rating-options">
                <button 
                  v-for="(option, optIndex) in question.options" 
                  :key="optIndex"
                  :class="['rating-btn', { active: assessmentAnswers[index] === optIndex }]"
                  @click="assessmentAnswers[index] = optIndex"
                >
                  {{ option }}
                </button>
              </div>
            </div>
          </div>
          <div class="assessment-actions">
            <button class="btn submit-assessment-btn" @click="submitAssessment">
              ✅ 提交评估
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showRelaxation" class="modal-overlay" @click="closeRelaxation">
      <div class="modal-content relaxation-modal" @click.stop>
        <div class="modal-header">
          <h3>🧘 放松练习</h3>
          <button @click="closeRelaxation" class="close-button">×</button>
        </div>
        <div class="modal-body">
          <div class="relaxation-exercises">
            <div 
              v-for="(exercise, index) in relaxationExercises" 
              :key="index" 
              class="exercise-item"
              @click="startExercise(exercise)"
            >
              <div class="exercise-info">
                <h3>{{ exercise.icon }} {{ exercise.name }}</h3>
                <p>{{ exercise.description }}</p>
                <span class="duration">⏱️ {{ exercise.duration }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showExerciseDetail && currentExercise" class="modal-overlay" @click="closeExerciseDetail">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ currentExercise.icon }} {{ currentExercise.name }}</h3>
          <button @click="closeExerciseDetail" class="close-button">×</button>
        </div>
        <div class="modal-body">
          <div class="exercise-content">
            <p class="exercise-description">{{ currentExercise.description }}</p>
            <div class="exercise-instructions">
              <h4>📝 练习步骤</h4>
              <ol>
                <li v-for="(step, index) in currentExercise.steps" :key="index">
                  {{ step }}
                </li>
              </ol>
            </div>
            <div class="exercise-duration">
              <span class="duration-label">⏱️ 建议时长：</span>
              <span class="duration-value">{{ currentExercise.duration }}</span>
            </div>
            
            <!-- 自定义时长选择 -->
            <div class="custom-duration">
              <h4>🎯 设置练习时长</h4>
              <div class="duration-slider">
                <input 
                  type="range" 
                  :min="currentExercise.minMinutes" 
                  :max="currentExercise.maxMinutes" 
                  v-model.number="selectedDuration"
                  class="slider"
                />
                <div class="duration-display">
                  <span class="selected-time">{{ selectedDuration }} 分钟</span>
                  <span class="range-hint">{{ currentExercise.minMinutes }} - {{ currentExercise.maxMinutes }} 分钟</span>
                </div>
              </div>
              <button class="use-recommended-btn" @click="useRecommendedDuration">
                ✅ 使用推荐时长 ({{ Math.floor((currentExercise.minMinutes + currentExercise.maxMinutes) / 2) }}分钟)
              </button>
            </div>
          </div>
          <div class="exercise-actions">
            <button class="btn start-exercise-btn" @click="beginExercise">
              ▶️ 开始练习
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 放松练习倒计时弹窗 -->
    <div v-if="showExerciseTimer && currentExercise" class="timer-overlay" @click="closeExerciseTimer">
      <div class="timer-content" @click.stop>
        <div class="timer-header">
          <h3>{{ currentExercise.icon }} {{ currentExercise.name }}</h3>
          <button @click="closeExerciseTimer" class="close-timer-btn" title="关闭">×</button>
        </div>
        <div class="timer-body">
          <div class="progress-circle">
            <svg viewBox="0 0 100 100" class="circle-svg">
              <circle
                class="circle-bg"
                cx="50"
                cy="50"
                r="45"
              />
              <circle
                class="circle-progress"
                cx="50"
                cy="50"
                r="45"
                :style="circleStyle"
              />
            </svg>
            <div class="timer-display">
              <span class="time-text">{{ formatTimeDisplay(remainingTime) }}</span>
            </div>
          </div>
          <div class="timer-controls">
            <button 
              v-if="!isPaused" 
              class="control-btn pause-btn" 
              @click="pauseTimer"
            >
              ⏸️ 暂停
            </button>
            <button 
              v-else 
              class="control-btn resume-btn" 
              @click="resumeTimer"
            >
              ▶️ 继续
            </button>
            <button class="control-btn close-btn" @click="closeExerciseTimer">
              ❌ 关闭
            </button>
          </div>
          <div class="timer-instructions">
            <p>{{ exerciseInstruction }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useUserStore } from '../stores/userStore'
import { healthApi } from '../api/healthApi'

const userStore = useUserStore()
const inputText = ref('')
const userInput = ref('')
const messages = ref([])
const loading = ref(false)
const messagesRef = ref(null)
const showMenu = ref(false)
const menuButtonRef = ref(null)

const showAssessment = ref(false)
const showRelaxation = ref(false)
const showExerciseDetail = ref(false)
const showExerciseTimer = ref(false)
const currentExercise = ref(null)
const selectedDuration = ref(5) // 默认 5 分钟

// 倒计时相关
const timerInterval = ref(null)
const remainingTime = ref(0)
const totalTime = ref(0)
const isPaused = ref(false)
const exerciseInstruction = ref('')

// 聊天记录搜索相关
const showHistoryModal = ref(false)
const searchKeyword = ref('')
const highlightedIndex = ref(-1)

const assessmentQuestions = ref([
  {
    question: '在过去的两周里，你是否经常感到情绪低落、沮丧或绝望？',
    options: ['完全没有', '有几天', '一半以上的天数', '几乎每天']
  },
  {
    question: '在过去的两周里，你是否经常对做事情缺乏兴趣或乐趣？',
    options: ['完全没有', '有几天', '一半以上的天数', '几乎每天']
  },
  {
    question: '你是否经常感到紧张、焦虑或不安？',
    options: ['完全没有', '有几天', '一半以上的天数', '几乎每天']
  },
  {
    question: '你是否能够享受生活中的小乐趣？',
    options: ['完全能够', '大部分时间能够', '偶尔能够', '几乎不能']
  },
  {
    question: '你是否感到有足够的能量来完成日常任务？',
    options: ['完全有', '大部分时间有', '偶尔有', '几乎没有']
  }
])
const assessmentAnswers = ref(new Array(5).fill(null))

const menuOptions = ref([
  { id: 'save', label: '保存聊天记录', icon: '💾' },
  { id: 'load', label: '加载聊天记录', icon: '📂' },
  { id: 'export', label: '导出为文本', icon: '📤' },
  { id: 'clear', label: '清除历史记录', icon: '🗑️' },
  { id: 'assessment', label: '心理健康评估', icon: '📋' },
  { id: 'relaxation', label: '放松练习', icon: '🧘' }
])

const relaxationExercises = [
  {
    name: '深呼吸练习',
    icon: '🌬️',
    description: '通过深呼吸来缓解紧张和焦虑',
    duration: '5-10 分钟',
    minMinutes: 5,
    maxMinutes: 10,
    steps: [
      '找一个安静舒适的地方坐下或躺下',
      '闭上眼睛，放松全身肌肉',
      '缓慢地吸气，数到4',
      '屏住呼吸，数到4',
      '缓慢地呼气，数到6',
      '重复这个过程10-15次'
    ]
  },
  {
    name: '渐进式肌肉放松',
    icon: '💪',
    description: '通过有意识地紧张和放松肌肉来减轻压力',
    duration: '15-20 分钟',
    minMinutes: 15,
    maxMinutes: 20,
    steps: [
      '从脚趾开始，用力收紧肌肉5秒',
      '放松肌肉，感受放松的感觉10秒',
      '逐渐向上移动，依次收紧和放松小腿、大腿、臀部、腹部、胸部、手臂、肩膀和面部肌肉',
      '最后，感受全身的放松状态'
    ]
  },
  {
    name: '正念冥想',
    icon: '🧘',
    description: '专注于当下，减少杂念和焦虑',
    duration: '10-15 分钟',
    minMinutes: 10,
    maxMinutes: 15,
    steps: [
      '找一个安静的地方坐下，保持舒适的姿势',
      '闭上眼睛，将注意力集中在呼吸上',
      '当杂念出现时，轻轻地将注意力拉回到呼吸上',
      '保持这种状态10-15分钟',
      '缓慢地睁开眼睛，感受周围的环境'
    ]
  }
]

const formatMessage = (content) => {
  return content.replace(/\n/g, '<br>')
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const sendMessage = async () => {
  if (!userInput.value.trim() || loading.value) return

  const userMessage = {
    type: 'user',
    content: userInput.value.trim(),
    timestamp: new Date()
  }
  messages.value.push(userMessage)
  
  const messageToSend = userInput.value.trim()
  userInput.value = ''
  
  scrollToBottom()

  try {
    loading.value = true
    
    // 验证用户 ID
    const userId = userStore.userData?.userId
    if (!userId) {
      throw new Error('用户未登录')
    }
    
    console.log('🔍 发送 AI 咨询请求:', { userId, messageLength: messageToSend.length })
    
    const response = await healthApi.getMentalHealthAdvice({
      userId: userId,
      message: messageToSend
    })

    console.log('✅ AI 咨询响应:', response.data)

    if (response.data.success) {
      const botMessage = {
        type: 'bot',
        content: response.data.response,
        timestamp: new Date()
      }
      messages.value.push(botMessage)
    } else {
      throw new Error(response.data.error || 'AI 服务响应失败')
    }
  } catch (error) {
    console.error('❌ 发送消息失败:', error)
    console.error('错误详情:', error.response?.data || error.message)
    const errorMessage = {
      type: 'bot',
      content: `抱歉，发生错误：${error.message || '网络连接出现问题，请稍后再试。'}`,
      timestamp: new Date()
    }
    messages.value.push(errorMessage)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const addLineBreak = () => {
  userInput.value += '\n'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const quickAsk = (question) => {
  userInput.value = question
  sendMessage()
}

const toggleMenu = () => {
  showMenu.value = !showMenu.value
}

const closeMenu = () => {
  showMenu.value = false
}

const handleMenuOption = async (option) => {
  closeMenu()
  
  switch (option.id) {
    case 'save':
      saveChatHistory()
      alert('聊天记录已保存')
      break
    case 'load':
      loadChatHistory()
      alert('聊天记录已加载')
      break
    case 'export':
      exportChatHistory()
      break
    case 'clear':
      await clearChatHistory()
      break
    case 'assessment':
      startAssessment()
      break
    case 'relaxation':
      showRelaxationExercises()
      break
  }
}

const showStats = () => {
  alert(`当前共有 ${messages.value.length} 条聊天记录`)
}

// 获取用户消息列表
const userMessages = computed(() => {
  return messages.value
    .map((msg, index) => ({ ...msg, originalIndex: index }))
    .filter(msg => msg.type === 'user')
})

// 搜索过滤后的用户消息
const filteredUserMessages = computed(() => {
  if (!searchKeyword.value.trim()) {
    return userMessages.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return userMessages.value.filter(msg => 
    msg.content.toLowerCase().includes(keyword)
  )
})

// 获取预览文本（只显示前20个字）
const getPreviewText = (content) => {
  if (!content) return ''
  return content.length > 20 ? content.substring(0, 20) + '...' : content
}

// 显示聊天记录弹窗
const showChatHistoryModal = () => {
  showHistoryModal.value = true
  searchKeyword.value = ''
  highlightedIndex.value = -1
}

// 关闭聊天记录弹窗
const closeHistoryModal = () => {
  showHistoryModal.value = false
}

// 定位到指定消息
const locateMessage = (index) => {
  closeHistoryModal()
  highlightedIndex.value = index
  
  nextTick(() => {
    const messageEl = document.getElementById(`message-${index}`)
    if (messageEl) {
      messageEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
      setTimeout(() => {
        highlightedIndex.value = -1
      }, 3000)
    }
  })
}

const saveChatHistory = () => {
  try {
    const historyData = {
      messages: messages.value,
      timestamp: new Date().toISOString(),
      userId: userStore.userData?.userId || 'anonymous'
    }
    localStorage.setItem('mentalHealthChatHistory', JSON.stringify(historyData))
  } catch (error) {
    console.error('保存聊天记录失败:', error)
  }
}

const loadChatHistory = () => {
  try {
    const savedData = localStorage.getItem('mentalHealthChatHistory')
    if (savedData) {
      const historyData = JSON.parse(savedData)
      if (historyData.messages && Array.isArray(historyData.messages)) {
        messages.value = historyData.messages.map(msg => ({
          ...msg,
          timestamp: new Date(msg.timestamp)
        }))
        nextTick(() => {
          scrollToBottom()
        })
      }
    }
  } catch (error) {
    console.error('加载聊天记录失败:', error)
  }
}

const exportChatHistory = () => {
  if (messages.value.length === 0) {
    alert('暂无聊天记录可导出')
    return
  }

  try {
    let exportText = '=== 心理陪伴助手聊天记录 ===\n\n'
    exportText += `导出时间: ${new Date().toLocaleString()}\n`
    exportText += `记录总数: ${messages.value.length} 条\n\n`
    exportText += '='.repeat(50) + '\n\n'

    messages.value.forEach((msg, index) => {
      const sender = msg.type === 'user' ? '👤 用户' : '🧠 心理陪伴助手'
      const time = new Date(msg.timestamp).toLocaleString()
      exportText += `[${index + 1}] ${sender} (${time})\n`
      exportText += msg.content + '\n\n'
    })

    exportText += '='.repeat(50) + '\n'
    exportText += '导出完成于: ' + new Date().toLocaleString()

    const blob = new Blob([exportText], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `mental_health_chat_${new Date().getTime()}.txt`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    alert('聊天记录已导出')
  } catch (error) {
    console.error('导出聊天记录失败:', error)
  }
}

const clearChatHistory = async () => {
  if (messages.value.length === 0) {
    alert('暂无聊天记录需要清除')
    return
  }

  const confirmed = confirm('确定要清除所有聊天记录吗？此操作不可撤销。')
  
  if (confirmed) {
    messages.value = []
    localStorage.removeItem('mentalHealthChatHistory')
    alert('聊天记录已清除')
  }
}

// 删除单条消息
const deleteMessage = async (index) => {
  const confirmed = confirm('确定要删除这条消息吗？')
  if (confirmed) {
    messages.value.splice(index, 1)
    saveChatHistory()
    alert('消息已删除')
  }
}

// 历史记录批量删除相关
const isBatchDeleteMode = ref(false)
const selectedHistoryItems = ref(new Set())

// 开始批量删除模式
const startBatchDelete = () => {
  isBatchDeleteMode.value = true
  selectedHistoryItems.value.clear()
}

// 取消批量删除模式
const cancelBatchDelete = () => {
  isBatchDeleteMode.value = false
  selectedHistoryItems.value.clear()
}

// 切换历史记录选择
const toggleHistorySelection = (index) => {
  if (selectedHistoryItems.value.has(index)) {
    selectedHistoryItems.value.delete(index)
  } else {
    selectedHistoryItems.value.add(index)
  }
  selectedHistoryItems.value = new Set(selectedHistoryItems.value)
}

// 全选历史记录
const isAllHistorySelected = computed(() => {
  return filteredUserMessages.value.length > 0 && 
         filteredUserMessages.value.every(msg => selectedHistoryItems.value.has(msg.originalIndex))
})

const toggleSelectAllHistory = () => {
  if (isAllHistorySelected.value) {
    filteredUserMessages.value.forEach(msg => {
      selectedHistoryItems.value.delete(msg.originalIndex)
    })
  } else {
    filteredUserMessages.value.forEach(msg => {
      selectedHistoryItems.value.add(msg.originalIndex)
    })
  }
  selectedHistoryItems.value = new Set(selectedHistoryItems.value)
}

// 处理历史记录项点击
const handleHistoryItemClick = (msg, event) => {
  if (isBatchDeleteMode.value) {
    toggleHistorySelection(msg.originalIndex)
  } else {
    locateMessage(msg.originalIndex)
  }
}

// 删除单条历史记录
const deleteHistoryItem = async (index) => {
  const confirmed = confirm('确定要删除这条记录吗？')
  if (confirmed) {
    messages.value.splice(index, 1)
    if (messages.value[index] && messages.value[index].type === 'bot') {
      messages.value.splice(index, 1)
    }
    saveChatHistory()
    alert('记录已删除')
  }
}

// 批量删除选中的历史记录
const deleteSelectedHistory = async () => {
  if (selectedHistoryItems.value.size === 0) {
    alert('请先选择要删除的记录')
    return
  }
  
  const confirmed = confirm(`确定要删除选中的 ${selectedHistoryItems.value.size} 条记录吗？`)
  if (confirmed) {
    const indicesToDelete = Array.from(selectedHistoryItems.value).sort((a, b) => b - a)
    indicesToDelete.forEach(index => {
      messages.value.splice(index, 1)
      if (messages.value[index] && messages.value[index].type === 'bot') {
        messages.value.splice(index, 1)
      }
    })
    selectedHistoryItems.value.clear()
    isBatchDeleteMode.value = false
    saveChatHistory()
    alert('已删除选中的记录')
  }
}

const startAssessment = () => {
  assessmentAnswers.value = new Array(5).fill(null)
  showAssessment.value = true
}

const submitAssessment = () => {
  if (assessmentAnswers.value.includes(null)) {
    alert('请回答所有问题')
    return
  }

  const score = assessmentAnswers.value.reduce((sum, answer) => sum + answer, 0)
  let result = ''

  if (score <= 5) {
    result = '你的心理健康状态良好，请继续保持！'
  } else if (score <= 10) {
    result = '你可能有轻微的情绪困扰，建议尝试一些放松练习。'
  } else if (score <= 15) {
    result = '你可能有中度的情绪困扰，建议寻求专业心理咨询。'
  } else {
    result = '你可能有较严重的情绪问题，强烈建议寻求专业心理健康服务。'
  }

  alert(result)
  closeAssessment()
}

const closeAssessment = () => {
  showAssessment.value = false
}

const showRelaxationExercises = () => {
  showRelaxation.value = true
}

const closeRelaxation = () => {
  showRelaxation.value = false
}

const startExercise = (exercise) => {
  currentExercise.value = exercise
  selectedDuration.value = Math.floor((exercise.minMinutes + exercise.maxMinutes) / 2) // 默认使用中间值
  showExerciseDetail.value = true
  showRelaxation.value = false
}

// 解析时长获取分钟数
const parseDuration = (durationStr) => {
  const match = durationStr.match(/(\d+)-(\d+)/)
  if (match) {
    // 返回中间值作为默认时长
    return Math.floor((parseInt(match[1]) + parseInt(match[2])) / 2)
  }
  return 5 // 默认 5 分钟
}

// 开始练习倒计时
const beginExercise = () => {
  if (!currentExercise.value) {
    console.error('当前练习信息为空')
    return
  }
  
  // 使用用户选择的时长
  currentExercise.value.durationMinutes = selectedDuration.value
  startExerciseTimer()
  
  // 延迟关闭详情弹窗，确保倒计时已启动
  setTimeout(() => {
    showExerciseDetail.value = false
  }, 50)
}

// 使用推荐时长
const useRecommendedDuration = () => {
  if (!currentExercise.value) return
  selectedDuration.value = Math.floor((currentExercise.value.minMinutes + currentExercise.value.maxMinutes) / 2)
}

const startExerciseTimer = () => {
  const minutes = currentExercise.value?.durationMinutes || 5
  totalTime.value = minutes * 60
  remainingTime.value = totalTime.value
  isPaused.value = false
  
  // 设置练习指导语
  const instructions = {
    '深呼吸练习': '请跟随节奏，缓慢地吸气和呼气，感受身体的放松',
    '渐进式肌肉放松': '依次收紧和放松每个肌肉群，感受紧张与放松的差异',
    '正念冥想': '专注于呼吸，当杂念出现时，轻轻地将注意力拉回'
  }
  exerciseInstruction.value = instructions[currentExercise.value?.name] || '请保持放松，享受这段宁静的时光'
  
  showExerciseTimer.value = true
  
  // 启动倒计时
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
  }
  
  timerInterval.value = setInterval(() => {
    if (!isPaused.value && remainingTime.value > 0) {
      remainingTime.value--
    }
    
    if (remainingTime.value === 0) {
      completeExercise()
    }
  }, 1000)
}

const completeExercise = () => {
  clearInterval(timerInterval.value)
  timerInterval.value = null
  alert('🎉 恭喜您完成了本次放松练习！希望这能帮助您缓解压力，放松心情。')
  closeExerciseTimer()
}

const pauseTimer = () => {
  isPaused.value = true
}

const resumeTimer = () => {
  isPaused.value = false
}

const closeExerciseTimer = () => {
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
    timerInterval.value = null
  }
  showExerciseTimer.value = false
  remainingTime.value = 0
  totalTime.value = 0
  isPaused.value = false
  clearCurrentExercise()
}

// 格式化时间显示
const formatTimeDisplay = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 计算进度圈样式
const circleStyle = computed(() => {
  const circumference = 2 * Math.PI * 45
  const progress = totalTime.value > 0 ? remainingTime.value / totalTime.value : 0
  const offset = circumference * (1 - progress)
  return {
    strokeDasharray: `${circumference} ${circumference}`,
    strokeDashoffset: offset
  }
})

const closeExerciseDetail = () => {
  showExerciseDetail.value = false
}

const clearCurrentExercise = () => {
  currentExercise.value = null
}

onMounted(() => {
  loadChatHistory()
  document.addEventListener('click', closeMenu)
})

onUnmounted(() => {
  saveChatHistory()
  document.removeEventListener('click', closeMenu)
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
  }
})

watch(messages, () => {
  saveChatHistory()
}, { deep: true })
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.mental-health-wrapper {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
  position: relative;
}

.mental-health-wrapper::before {
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

.chat-header {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  padding: 12px 32px;
  flex-shrink: 0;
  position: relative;
  z-index: 100;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.08);
}

.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ai-avatar {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #5856d6 0%, #af52de 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(88, 86, 214, 0.3);
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.header-text h1 {
  color: #1d1d1f;
  margin: 0;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.status {
  color: #34c759;
  margin: 4px 0 0 0;
  font-size: 13px;
  font-weight: 500;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.dropdown-wrapper {
  position: relative;
  z-index: 999999999999;
}

.menu-btn {
  background: #007aff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 980px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-size: 15px;
}

.menu-btn:hover:not(:disabled) {
  background: #0077ed;
  transform: scale(1.02);
}

.menu-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #ffffff;
  border-radius: 16px;
  padding: 8px 0;
  min-width: 200px;
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.08),
    0 24px 64px rgba(0, 0, 0, 0.12);
  border: none;
  z-index: 999999999999;
  animation: slideDown 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #1d1d1f;
  font-size: 15px;
}

.menu-item:hover {
  background: #f5f5f7;
}

.menu-label {
  flex: 1;
  font-weight: 500;
}

.stats-btn {
  background: #f5f5f7;
  color: #1d1d1f;
  border: none;
  padding: 10px 20px;
  border-radius: 980px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-size: 15px;
}

.stats-btn:hover {
  background: #e8e8ed;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 20px 32px;
  gap: 16px;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.messages-container {
  flex: 1;
  background: #ffffff;
  border-radius: 20px;
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
  border: none;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  scrollbar-gutter: stable;
  min-height: 300px;
  position: relative;
}

.messages-container::-webkit-scrollbar {
  width: 8px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

.welcome-section {
  margin-bottom: 24px;
}

.welcome-card {
  background: #f5f5f7;
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  border: none;
}

.welcome-card h3 {
  color: #1d1d1f;
  margin: 0 0 12px 0;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.5px;
}

.welcome-card p {
  color: #86868b;
  margin: 0 0 28px 0;
  font-size: 17px;
}

.quick-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-item {
  background: #007aff;
  color: white;
  padding: 12px 24px;
  border-radius: 980px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.action-item:hover {
  background: #0077ed;
  transform: scale(1.02);
}

.message-row {
  margin-bottom: 20px;
  display: flex;
}

.message-row.from-user {
  justify-content: flex-end;
}

.message-row.from-ai {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 16px 20px;
  border-radius: 20px;
  position: relative;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.02);
  word-wrap: break-word;
  word-break: break-word;
}

.user-bubble {
  background: #007aff;
  color: white;
  border-bottom-right-radius: 6px;
}

.ai-bubble {
  background: #f5f5f7;
  color: #1d1d1f;
  border-bottom-left-radius: 6px;
  border: none;
}

.message-text {
  line-height: 1.6;
  margin-bottom: 8px;
  overflow-wrap: break-word;
  font-size: 15px;
}

.message-time {
  font-size: 12px;
  opacity: 0.6;
  text-align: right;
}

.loading-section {
  margin-bottom: 20px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
}

.typing-dots {
  display: flex;
  gap: 4px;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  background: #007aff;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) { animation-delay: -0.32s; }
.typing-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-footer {
  background: #ffffff;
  border-radius: 24px;
  padding: 20px;
  border: none;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  margin-top: auto;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  min-height: 48px;
  max-height: 140px;
  padding: 14px 18px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 16px;
  background: #f5f5f7;
  font-size: 15px;
  resize: none;
  outline: none;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  overflow-y: auto;
  line-height: 1.5;
  color: #1d1d1f;
  font-family: inherit;
}

.message-input:focus {
  border-color: #007aff;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}

.message-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-button {
  background: #007aff;
  color: white;
  border: none;
  padding: 14px 28px;
  border-radius: 980px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  height: fit-content;
}

.send-button:hover:not(:disabled) {
  background: #0077ed;
  transform: scale(1.02);
}

.send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.input-tip {
  color: #86868b;
  font-size: 13px;
  margin-top: 12px;
  text-align: center;
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
  align-items: center;
  z-index: 1000;
  padding: 24px;
  backdrop-filter: blur(10px);
}

.modal-content {
  background: #ffffff;
  border-radius: 24px;
  padding: 28px;
  width: 100%;
  max-width: 560px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 32px 64px rgba(0, 0, 0, 0.12);
  position: relative;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.modal-header h3 {
  color: #1d1d1f;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #86868b;
  transition: color 0.2s ease;
  line-height: 1;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-button:hover {
  color: #1d1d1f;
  background: #f5f5f7;
}

.modal-body {
  padding: 4px 0;
}

.assessment-questions {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-item {
  background: #f5f5f7;
  padding: 20px;
  border-radius: 16px;
  border: none;
}

.question-text {
  color: #1d1d1f;
  font-size: 15px;
  margin-bottom: 14px;
  line-height: 1.5;
}

.rating-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rating-btn {
  padding: 10px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 980px;
  background: #ffffff;
  color: #1d1d1f;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.rating-btn:hover {
  border-color: #007aff;
  color: #007aff;
}

.rating-btn.active {
  background: #007aff;
  color: white;
  border-color: transparent;
}

.assessment-actions {
  margin-top: 24px;
  text-align: center;
}

.btn {
  padding: 14px 28px;
  border: none;
  border-radius: 980px;
  font-size: 17px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.submit-assessment-btn {
  background: #34c759;
  color: white;
}

.submit-assessment-btn:hover {
  background: #30b350;
  transform: scale(1.02);
}

.start-exercise-btn {
  background: #007aff;
  color: white;
}

.start-exercise-btn:hover {
  background: #0077ed;
  transform: scale(1.02);
}

.relaxation-modal {
  max-width: 600px;
}

.relaxation-exercises {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exercise-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f5f5f7;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
}

.exercise-item:hover {
  background: #e8e8ed;
  transform: translateX(4px);
}

.exercise-info {
  flex: 1;
}

.exercise-info h3 {
  margin: 0 0 6px 0;
  color: #1d1d1f;
  font-size: 17px;
  font-weight: 600;
}

.exercise-info p {
  margin: 0 0 8px 0;
  color: #86868b;
  font-size: 14px;
}

.exercise-info .duration {
  color: #007aff;
  font-size: 13px;
  font-weight: 500;
}

.exercise-content {
  text-align: center;
}

.exercise-description {
  color: #86868b;
  font-size: 15px;
  margin-bottom: 24px;
  line-height: 1.6;
}

.exercise-instructions {
  text-align: left;
  background: #f5f5f7;
  padding: 20px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.exercise-instructions h4 {
  color: #1d1d1f;
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
}

.exercise-instructions ol {
  margin: 0;
  padding-left: 20px;
}

.exercise-instructions li {
  color: #1d1d1f;
  margin-bottom: 10px;
  line-height: 1.6;
  font-size: 15px;
}

.exercise-duration {
  display: flex;
  justify-content: center;
  gap: 8px;
  align-items: center;
  margin-bottom: 24px;
}

.duration-label {
  color: #86868b;
  font-size: 15px;
}

.duration-value {
  color: #007aff;
  font-weight: 600;
  font-size: 15px;
}

.exercise-actions {
  text-align: center;
}

/* 自定义时长选择样式 */
.custom-duration {
  margin-top: 24px;
  padding: 20px;
  background: #f5f5f7;
  border-radius: 16px;
  border: none;
}

.custom-duration h4 {
  color: #1d1d1f;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}

.duration-slider {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.slider {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 8px;
  border-radius: 4px;
  background: #e8e8ed;
  outline: none;
  cursor: pointer;
}

.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #007aff;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
  transition: all 0.2s ease;
}

.slider::-webkit-slider-thumb:hover {
  background: #0077ed;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.4);
  transform: scale(1.1);
}

.slider::-moz-range-thumb {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #007aff;
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
  transition: all 0.2s ease;
}

.slider::-moz-range-thumb:hover {
  background: #0077ed;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.4);
  transform: scale(1.1);
}

.duration-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.selected-time {
  color: #007aff;
  font-size: 18px;
  font-weight: 600;
}

.range-hint {
  color: #86868b;
  font-size: 13px;
}

.use-recommended-btn {
  width: 100%;
  padding: 12px;
  background: #34c759;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.use-recommended-btn:hover {
  background: #30b350;
  transform: scale(1.02);
}

/* 倒计时弹窗样式 */
.timer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10000;
  backdrop-filter: blur(8px);
}

.timer-content {
  background: #ffffff;
  border-radius: 24px;
  padding: 32px;
  width: 100%;
  max-width: 420px;
  box-shadow: 
    0 12px 48px rgba(0, 0, 0, 0.12),
    0 48px 96px rgba(0, 0, 0, 0.16);
  position: relative;
  animation: timerSlideUp 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

@keyframes timerSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.timer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.timer-header h3 {
  color: #1d1d1f;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: -0.3px;
  margin: 0;
}

.close-timer-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #86868b;
  transition: all 0.2s ease;
  line-height: 1;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-timer-btn:hover {
  color: #1d1d1f;
  background: #f5f5f7;
}

.timer-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.progress-circle {
  position: relative;
  width: 240px;
  height: 240px;
}

.circle-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.circle-bg {
  fill: none;
  stroke: #f5f5f7;
  stroke-width: 8;
}

.circle-progress {
  fill: none;
  stroke: #007aff;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.3s ease;
}

.timer-display {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.time-text {
  font-size: 48px;
  font-weight: 600;
  color: #1d1d1f;
  font-variant-numeric: tabular-nums;
  letter-spacing: -1px;
}

.timer-controls {
  display: flex;
  gap: 12px;
  width: 100%;
}

.control-btn {
  flex: 1;
  padding: 14px 20px;
  border: none;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.pause-btn,
.resume-btn {
  background: #007aff;
  color: white;
}

.pause-btn:hover,
.resume-btn:hover {
  background: #0077ed;
  transform: scale(1.02);
}

.close-btn {
  background: #f5f5f7;
  color: #1d1d1f;
}

.close-btn:hover {
  background: #e8e8ed;
}

.timer-instructions {
  width: 100%;
  padding: 16px 20px;
  background: #f5f5f7;
  border-radius: 16px;
  text-align: center;
}

.timer-instructions p {
  margin: 0;
  color: #86868b;
  font-size: 15px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .chat-header {
    padding: 16px 20px;
  }
  
  .header-inner {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .header-left {
    flex-direction: column;
    gap: 12px;
  }
  
  .header-right {
    width: 100%;
    justify-content: center;
  }
  
  .chat-main {
    padding: 16px 20px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .messages-container {
    padding: 16px;
    min-height: 200px;
    border-radius: 20px;
  }
  
  .message-bubble {
    padding: 12px 16px;
  }
  
  .input-footer {
    padding: 16px;
    border-radius: 20px;
  }
  
  .input-wrapper {
    flex-direction: column;
  }
  
  .send-button {
    align-self: flex-end;
  }
  
  .quick-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .action-item {
    width: 100%;
    max-width: 280px;
  }
  
  .welcome-card {
    padding: 28px 20px;
  }
  
  .welcome-card h3 {
    font-size: 22px;
  }
}

.history-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(10px);
}

.history-modal {
  background: #ffffff;
  border-radius: 24px;
  width: 90%;
  max-width: 480px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 32px 64px rgba(0, 0, 0, 0.12);
}

.history-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.history-modal-header h3 {
  margin: 0;
  color: #1d1d1f;
  font-size: 17px;
  font-weight: 600;
}

.close-modal-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #86868b;
  padding: 0;
  line-height: 1;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.close-modal-btn:hover {
  color: #1d1d1f;
  background: #f5f5f7;
}

.history-toolbar {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.batch-btn,
.select-all-btn,
.delete-selected-btn,
.cancel-btn {
  padding: 8px 16px;
  border-radius: 980px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.batch-btn {
  background: #007aff;
  color: white;
}

.batch-btn:hover:not(:disabled) {
  background: #0077ed;
}

.batch-btn:disabled {
  background: #d1d1d6;
  cursor: not-allowed;
}

.select-all-btn {
  background: #f5f5f7;
  color: #1d1d1f;
}

.select-all-btn:hover {
  background: #e8e8ed;
}

.delete-selected-btn {
  background: #ff3b30;
  color: white;
}

.delete-selected-btn:hover:not(:disabled) {
  background: #ff2d23;
}

.delete-selected-btn:disabled {
  background: #d1d1d6;
  cursor: not-allowed;
}

.cancel-btn {
  background: #86868b;
  color: white;
}

.cancel-btn:hover {
  background: #6e6e73;
}

.history-item.selected {
  background: rgba(0, 122, 255, 0.08);
}

.history-checkbox {
  display: flex;
  align-items: center;
}

.history-checkbox input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #007aff;
}

.history-delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px 12px;
  font-size: 13px;
  opacity: 0.5;
  transition: opacity 0.2s ease;
  color: #86868b;
  font-weight: 500;
}

.history-delete-btn:hover {
  opacity: 1;
  color: #ff3b30;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  font-size: 12px;
  opacity: 0.5;
  transition: opacity 0.2s ease;
  color: inherit;
  font-weight: 500;
}

.delete-btn:hover {
  opacity: 1;
}

.user-bubble .delete-btn {
  color: rgba(255, 255, 255, 0.7);
}

.user-bubble .delete-btn:hover {
  color: white;
}

.ai-bubble .delete-btn {
  color: #86868b;
}

.ai-bubble .delete-btn:hover {
  color: #ff3b30;
}

.search-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  font-size: 15px;
  outline: none;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  background: #f5f5f7;
  color: #1d1d1f;
  font-family: inherit;
}

.search-input:focus {
  border-color: #007aff;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.2s ease;
  gap: 12px;
}

.history-item:hover {
  background: #f5f5f7;
}

.history-index {
  color: #007aff;
  font-weight: 600;
  font-size: 13px;
  min-width: 40px;
}

.history-preview {
  flex: 1;
  color: #1d1d1f;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-time {
  color: #86868b;
  font-size: 12px;
}

.no-results {
  text-align: center;
  padding: 40px 20px;
  color: #86868b;
  font-size: 15px;
}

.highlighted {
  animation: highlight 3s ease-out;
}

@keyframes highlight {
  0% {
    background: rgba(0, 122, 255, 0.15);
  }
  100% {
    background: transparent;
  }
}
</style>
