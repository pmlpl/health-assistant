<template>
  <div class="ai-chat-wrapper">
    <!-- 固定头部 -->
    <header class="chat-header">
      <div class="header-inner">
        <div class="header-left">
          <div class="ai-avatar">🤖</div>
          <div class="header-text">
            <h1>AI 营养师</h1>
            <p class="status">🟢 在线为您提供专业建议</p>
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

    <!-- 聊天记录搜索弹窗 -->
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

    <!-- 主要内容区域 -->
    <main class="chat-main">
      <!-- 消息显示区域 - 可滚动 -->
      <div class="messages-container" ref="messagesRef">
        <!-- 欢迎消息 -->
        <div class="welcome-section">
          <div class="welcome-card">
            <div class="welcome-icon">👋</div>
            <h3>您好！我是您的 AI 营养师</h3>
            <p>我可以为您提供专业的饮食建议和营养分析</p>
            <div class="quick-actions">
              <div class="action-item" @click="handleQuickAction('减脂饮食规划')">🥗 减脂饮食规划</div>
              <div class="action-item" @click="handleQuickAction('增肌营养搭配')">💪 增肌营养搭配</div>
              <div class="action-item" @click="handleQuickAction('控糖饮食建议')">🍎 控糖饮食建议</div>
            </div>
          </div>
        </div>

        <!-- 聊天消息列表 -->
        <div 
          v-for="(msg, index) in messages" 
          :key="index" 
          :id="'message-' + index"
          class="message-row"
          :class="{ 'from-user': msg.sender === 'user', 'from-ai': msg.sender === 'ai', 'highlighted': highlightedIndex === index }"
        >
          <div class="message-content">
            <div class="message-bubble" :class="{ 'user-bubble': msg.sender === 'user', 'ai-bubble': msg.sender === 'ai' }">
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div class="message-footer">
                <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
                <button class="delete-btn" @click="deleteMessage(index)" title="删除此条消息">🗑️ 删除</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-section">
          <div class="message-row from-ai">
            <div class="message-content">
              <div class="message-bubble ai-bubble typing-indicator">
                <div class="typing-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <div class="message-text">🤔 AI营养师正在思考中...</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 固定输入区域 -->
      <footer class="input-footer">
        <div class="input-wrapper">
          <textarea
            v-model="inputText"
            placeholder="💭 请输入您的饮食相关问题..."
            class="message-input"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact="addLineBreak"
            :disabled="loading"
          ></textarea>
          <button 
            class="send-button" 
            @click="sendMessage"
            :disabled="!inputText.trim() || loading"
          >
            {{ loading ? '⏳ 发送中' : '📤 发送' }}
          </button>
        </div>
        <div class="input-tip">
          ⌨️ 按 Enter 发送，Shift + Enter 换行
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useUserStore } from '../stores/userStore'
import messageService from '../services/messageService'

// 状态管理
const userStore = useUserStore()
const inputText = ref('')
const messages = ref([])
const loading = ref(false)
const messagesRef = ref(null)
const showMenu = ref(false)
const menuButtonRef = ref(null)

// 聊天记录搜索相关
const showHistoryModal = ref(false)
const searchKeyword = ref('')
const highlightedIndex = ref(-1)

// 菜单选项
const menuOptions = ref([
  { id: 'save', label: ' 保存聊天记录', icon: '💾' },
  { id: 'load', label: ' 加载聊天记录', icon: '📂' },
  { id: 'export', label: ' 导出为文本', icon: '📤' },
  { id: 'clear', label: ' 清除历史记录', icon: '🗑️' }
])

// 消息格式化
const formatMessage = (content) => {
  return content.replace(/\n/g, '<br>')
}

// 时间格式化
const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 获取用户消息列表
const userMessages = computed(() => {
  return messages.value
    .map((msg, index) => ({ ...msg, originalIndex: index }))
    .filter(msg => msg.sender === 'user')
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
      // 3 秒后取消高亮
      setTimeout(() => {
        highlightedIndex.value = -1
      }, 3000)
    }
  })
}

// 处理快速操作按钮点击
const handleQuickAction = (action) => {
  // 将快速操作的内容填充到输入框并自动发送
  inputText.value = `请为我提供${action}的专业建议`
  sendMessage()
}

// 发送消息
const sendMessage = async () => {
  if (!inputText.value.trim() || loading.value) return

  // 添加用户消息
  const userMessage = {
    sender: 'user',
    content: inputText.value.trim(),
    timestamp: new Date()
  }
  messages.value.push(userMessage)
  
  const messageToSend = inputText.value.trim()
  inputText.value = ''
  
  // 滚动到底部
  scrollToBottom()

  try {
    loading.value = true
    
    // 调用AI服务
    const response = await fetch('/api/ai/nutrition-advice', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        userId: userStore.userData.userId,
        query: messageToSend
      })
    })

    if (response.ok) {
      const result = await response.json()
      const aiMessage = {
        sender: 'ai',
        content: result.advice || '抱歉，我暂时无法回答这个问题。',
        timestamp: new Date()
      }
      messages.value.push(aiMessage)
    } else {
      throw new Error('AI服务响应失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    const errorMessage = {
      sender: 'ai',
      content: '抱歉，网络连接出现问题，请稍后再试。',
      timestamp: new Date()
    }
    messages.value.push(errorMessage)
    messageService.error('发送消息失败：' + error.message)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 换行处理
const addLineBreak = () => {
  inputText.value += '\n'
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}



// 菜单控制
const toggleMenu = () => {
  showMenu.value = !showMenu.value
}

const closeMenu = () => {
  showMenu.value = false
}

// 菜单选项处理
const handleMenuOption = async (option) => {
  closeMenu()
  
  switch (option.id) {
    case 'save':
      saveChatHistory()
      messageService.success('聊天记录已保存')
      break
    case 'load':
      loadChatHistory()
      messageService.success('聊天记录已加载')
      break
    case 'export':
      exportChatHistory()
      break
    case 'clear':
      await clearChatHistory()
      break
  }
}

// 显示统计信息
const showStats = () => {
  messageService.info(`当前共有 ${messages.value.length} 条聊天记录`)
}

// 聊天记录管理
const saveChatHistory = () => {
  try {
    const historyData = {
      messages: messages.value,
      timestamp: new Date().toISOString(),
      userId: userStore.userData?.userId || 'anonymous'
    }
    localStorage.setItem('aiChatHistory', JSON.stringify(historyData))
  } catch (error) {
    console.error('保存聊天记录失败:', error)
    messageService.error('保存聊天记录失败')
  }
}

const loadChatHistory = () => {
  try {
    const savedData = localStorage.getItem('aiChatHistory')
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
    messageService.error('加载聊天记录失败')
  }
}

const exportChatHistory = () => {
  if (messages.value.length === 0) {
    messageService.warning('暂无聊天记录可导出')
    return
  }

  try {
    let exportText = '=== AI营养师聊天记录 ===\n\n'
    exportText += `导出时间: ${new Date().toLocaleString()}\n`
    exportText += `记录总数: ${messages.value.length} 条\n\n`
    exportText += '='.repeat(50) + '\n\n'

    messages.value.forEach((msg, index) => {
      const sender = msg.sender === 'user' ? '👤 用户' : '🤖 AI营养师'
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
    link.download = `AI_chat_history_${new Date().getTime()}.txt`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    messageService.success('聊天记录已导出')
  } catch (error) {
    console.error('导出聊天记录失败:', error)
    messageService.error('导出聊天记录失败')
  }
}

const clearChatHistory = async () => {
  if (messages.value.length === 0) {
    messageService.warning('暂无聊天记录需要清除')
    return
  }

  const confirmed = await messageService.confirm('确定要清除所有聊天记录吗？此操作不可撤销。')
  
  if (confirmed) {
    messages.value = []
    localStorage.removeItem('aiChatHistory')
    messageService.success('聊天记录已清除')
  }
}

// 生命周期
onMounted(() => {
  loadChatHistory()
  document.addEventListener('click', closeMenu)
})

onUnmounted(() => {
  saveChatHistory()
  document.removeEventListener('click', closeMenu)
})

// 监听消息变化自动保存
watch(messages, () => {
  saveChatHistory()
}, { deep: true })

// 删除单条消息
const deleteMessage = async (index) => {
  const confirmed = await messageService.confirm('确定要删除这条消息吗？')
  if (confirmed) {
    messages.value.splice(index, 1)
    messageService.success('消息已删除')
  }
}

// 批量删除相关
const selectedMessages = ref(new Set())
const isSelectMode = ref(false)

// 切换选择模式
const toggleSelectMode = () => {
  isSelectMode.value = !isSelectMode.value
  if (!isSelectMode.value) {
    selectedMessages.value.clear()
  }
}

// 全选/取消全选
const toggleSelectAll = () => {
  if (selectedMessages.value.size === messages.value.length) {
    selectedMessages.value.clear()
  } else {
    selectedMessages.value = new Set(messages.value.map((_, i) => i))
  }
}

// 切换单条消息选择
const toggleMessageSelection = (index) => {
  if (selectedMessages.value.has(index)) {
    selectedMessages.value.delete(index)
  } else {
    selectedMessages.value.add(index)
  }
  // 触发响应式更新
  selectedMessages.value = new Set(selectedMessages.value)
}

// 批量删除选中的消息
const deleteSelectedMessages = async () => {
  if (selectedMessages.value.size === 0) {
    messageService.warning('请先选择要删除的消息')
    return
  }
  
  const confirmed = await messageService.confirm(`确定要删除选中的 ${selectedMessages.value.size} 条消息吗？`)
  if (confirmed) {
    const indicesToDelete = Array.from(selectedMessages.value).sort((a, b) => b - a)
    indicesToDelete.forEach(index => {
      messages.value.splice(index, 1)
    })
    selectedMessages.value.clear()
    isSelectMode.value = false
    messageService.success('已删除选中的消息')
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
  const confirmed = await messageService.confirm('确定要删除这条记录吗？')
  if (confirmed) {
    // 删除该用户消息及其对应的AI回复
    messages.value.splice(index, 1)
    // 如果下一条是AI回复，也删除
    if (messages.value[index] && messages.value[index].sender === 'ai') {
      messages.value.splice(index, 1)
    }
    messageService.success('记录已删除')
  }
}

// 批量删除选中的历史记录
const deleteSelectedHistory = async () => {
  if (selectedHistoryItems.value.size === 0) {
    messageService.warning('请先选择要删除的记录')
    return
  }
  
  const confirmed = await messageService.confirm(`确定要删除选中的 ${selectedHistoryItems.value.size} 条记录吗？`)
  if (confirmed) {
    const indicesToDelete = Array.from(selectedHistoryItems.value).sort((a, b) => b - a)
    indicesToDelete.forEach(index => {
      messages.value.splice(index, 1)
      // 如果下一条是AI回复，也删除
      if (messages.value[index] && messages.value[index].sender === 'ai') {
        messages.value.splice(index, 1)
      }
    })
    selectedHistoryItems.value.clear()
    isBatchDeleteMode.value = false
    messageService.success('已删除选中的记录')
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.ai-chat-wrapper {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
  position: relative;
}

.ai-chat-wrapper::before {
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
  font-size: 18px;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
  color: white;
  font-weight: 700;
  letter-spacing: -0.5px;
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
  display: flex;
  align-items: center;
  gap: 6px;
}

.status::before {
  content: '';
  width: 8px;
  height: 8px;
  background: #34c759;
  border-radius: 50%;
  display: inline-block;
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
  font-size: 14px;
}

.menu-btn:hover:not(:disabled) {
  background: #0077ed;
  transform: scale(1.02);
}

.menu-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #ffffff;
  border-radius: 16px;
  padding: 8px 0;
  min-width: 220px;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.08);
  border: none;
  z-index: 999999999999;
  animation: slideDown 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
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

.menu-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
  animation: iconFloat 2.5s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
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
  font-size: 14px;
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
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  border: none;
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
  background: linear-gradient(135deg, #f5f5f7 0%, #ffffff 100%);
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  border: none;
  box-shadow: none;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 20px;
  animation: iconFloat 3s ease-in-out infinite;
}

.welcome-card h3 {
  color: #1d1d1f;
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.3px;
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

.message-row.highlighted {
  animation: highlightPulse 2s ease-out;
}

@keyframes highlightPulse {
  0% { background: rgba(0, 122, 255, 0.1); }
  100% { background: transparent; }
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

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.message-time {
  font-size: 12px;
  opacity: 0.6;
  text-align: right;
}

.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  font-size: 12px;
  opacity: 0.4;
  transition: opacity 0.2s ease;
}

.delete-btn:hover {
  opacity: 1;
}

.user-bubble .delete-btn {
  color: rgba(255, 255, 255, 0.8);
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

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
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
  border-radius: 20px;
  padding: 20px;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  margin-top: auto;
  border: none;
}

.input-wrapper {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  min-height: 52px;
  max-height: 120px;
  padding: 14px 18px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 16px;
  background: #f5f5f7;
  font-size: 16px;
  resize: none;
  outline: none;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  overflow-y: auto;
  font-family: inherit;
  color: #1d1d1f;
}

.message-input:focus {
  border-color: #007aff;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}

.message-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-button {
  background: #007aff;
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 980px;
  font-size: 17px;
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
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

.input-tip {
  color: #86868b;
  font-size: 13px;
  margin-top: 12px;
  text-align: center;
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
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .chat-main {
    padding: 16px 20px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .messages-container {
    padding: 16px;
    border-radius: 16px;
    min-height: 200px;
  }
  
  .message-bubble {
    padding: 12px 16px;
  }
  
  .input-wrapper {
    flex-direction: column;
  }
  
  .send-button {
    align-self: flex-end;
  }
  
  .messages-container::-webkit-scrollbar {
    width: 6px;
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
  -webkit-backdrop-filter: blur(10px);
}

.history-modal {
  background: #ffffff;
  border-radius: 24px;
  width: 90%;
  max-width: 520px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  box-shadow: 
    0 24px 80px rgba(0, 0, 0, 0.2),
    0 8px 32px rgba(0, 0, 0, 0.1);
  animation: modalSlideIn 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.history-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.history-modal-header h3 {
  margin: 0;
  color: #1d1d1f;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.close-modal-btn {
  background: #f5f5f7;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #86868b;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.close-modal-btn:hover {
  background: #e8e8ed;
  color: #1d1d1f;
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
  gap: 14px;
}

.history-item:hover {
  background: #f5f5f7;
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

.history-index {
  color: #007aff;
  font-weight: 600;
  font-size: 14px;
  min-width: 40px;
}

.history-preview {
  flex: 1;
  color: #1d1d1f;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-time {
  color: #86868b;
  font-size: 13px;
  min-width: 45px;
  text-align: right;
}

.no-results {
  padding: 48px 20px;
  text-align: center;
  color: #86868b;
  font-size: 15px;
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
  gap: 10px;
  flex-wrap: wrap;
}

.batch-btn,
.select-all-btn,
.delete-selected-btn,
.cancel-btn {
  padding: 10px 18px;
  border-radius: 980px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
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
  background: #f5f5f7;
  color: #1d1d1f;
}

.cancel-btn:hover {
  background: #e8e8ed;
}

.history-delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  font-size: 14px;
  opacity: 0.4;
  transition: opacity 0.2s ease;
}

.history-delete-btn:hover {
  opacity: 1;
}

/* 消息高亮样式 */
.message-row.highlighted {
  animation: highlightPulse 0.5s ease-out;
  background: rgba(102, 126, 234, 0.1);
}

@keyframes highlightPulse {
  0% {
    background: rgba(102, 126, 234, 0.3);
  }
  100% {
    background: rgba(102, 126, 234, 0.1);
  }
}
</style>