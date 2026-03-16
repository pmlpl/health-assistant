<template>
  <div v-if="isVisible" class="message-overlay" @click="handleOverlayClick">
    <div class="message-box" @click.stop>
      <div class="message-header">
        <div class="header-icon" :class="type">
          {{ getIcon() }}
        </div>
        <h3 class="message-title">{{ title }}</h3>
        <button class="close-btn" @click="close" v-if="showClose">×</button>
      </div>
      
      <div class="message-content">
        <p class="message-text" v-if="!dangerouslyUseHTMLString">{{ message }}</p>
        <p class="message-text" v-else v-html="message"></p>
      </div>
      
      <div class="message-footer" v-if="showActions">
        <button 
          v-for="action in actions" 
          :key="action.id"
          class="action-btn"
          :class="action.type || 'primary'"
          @click="handleAction(action)"
        >
          {{ action.text }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '提示'
  },
  message: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'info', // info, success, warning, error
    validator: (value) => ['info', 'success', 'warning', 'error'].includes(value)
  },
  showActions: {
    type: Boolean,
    default: false
  },
  actions: {
    type: Array,
    default: () => []
  },
  showClose: {
    type: Boolean,
    default: true
  },
  autoClose: {
    type: Boolean,
    default: false
  },
  autoCloseDelay: {
    type: Number,
    default: 3000
  },
  closeOnOverlay: {
    type: Boolean,
    default: true
  },
  dangerouslyUseHTMLString: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'action', 'close'])

const isVisible = ref(false)

// 监听modelValue变化
watch(() => props.modelValue, (newValue) => {
  isVisible.value = newValue
  if (newValue && props.autoClose) {
    setTimeout(() => {
      close()
    }, props.autoCloseDelay)
  }
}, { immediate: true })

// 图标映射
const getIcon = () => {
  const icons = {
    info: 'ℹ️',
    success: '✅',
    warning: '⚠️',
    error: '❌'
  }
  return icons[props.type] || 'ℹ️'
}

// 关闭消息框
const close = () => {
  isVisible.value = false
  emit('update:modelValue', false)
  emit('close')
}

// 处理遮罩层点击
const handleOverlayClick = () => {
  if (props.closeOnOverlay) {
    close()
  }
}

// 处理动作按钮点击
const handleAction = (action) => {
  emit('action', action)
  if (action.close !== false) {
    close()
  }
}

// 暴露方法给父组件
defineExpose({
  open: () => {
    isVisible.value = true
    emit('update:modelValue', true)
  },
  close
})

onMounted(() => {
})
</script>

<style scoped>
.message-overlay {
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
  backdrop-filter: blur(5px);
}

.message-box {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 400px;
  overflow: hidden;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateY(-50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.message-header {
  display: flex;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  position: relative;
}

.header-icon {
  font-size: 24px;
  margin-right: 12px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
}

.message-title {
  flex: 1;
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.close-btn {
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
  transition: background 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.message-content {
  padding: 25px 20px;
}

.message-text {
  margin: 0;
  font-size: 1rem;
  line-height: 1.6;
  color: #333;
  text-align: center;
}

.message-footer {
  display: flex;
  gap: 12px;
  padding: 0 20px 20px;
  justify-content: center;
}

.action-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 25px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 80px;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.action-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.action-btn.secondary {
  background: #f8f9fa;
  color: #667eea;
  border: 1px solid #667eea;
}

.action-btn.secondary:hover {
  background: #667eea;
  color: white;
}

.action-btn.danger {
  background: linear-gradient(135deg, #ff6b6b, #ee5a52);
  color: white;
}

.action-btn.danger:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
}

.action-btn.success {
  background: linear-gradient(135deg, #4CAF50, #45a049);
  color: white;
}

.action-btn.success:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(76, 175, 80, 0.4);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .message-box {
    width: 95%;
    margin: 20px;
  }
  
  .message-header {
    padding: 15px;
  }
  
  .message-content {
    padding: 20px 15px;
  }
  
  .message-footer {
    padding: 0 15px 15px;
    flex-direction: column;
  }
  
  .action-btn {
    width: 100%;
  }
}
</style>