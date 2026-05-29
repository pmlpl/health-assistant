<template>
  <div class="api-status-indicator">
    <div class="api-status-header">
      <h3>API 状态</h3>
      <button @click="refreshStatus" class="refresh-button">
        <span class="refresh-icon">🔄</span>
        刷新
      </button>
    </div>
    <div class="api-status-list">
      <div class="api-status-item">
        <span class="api-name">通义千问</span>
        <span :class="['status-indicator', qwenStatus.available ? 'available' : 'unavailable']">
          {{ qwenStatus.available ? '可用' : '不可用' }}
        </span>
        <span class="api-message">{{ qwenStatus.message }}</span>
      </div>
      <div class="api-status-item">
        <span class="api-name">豆包</span>
        <span :class="['status-indicator', doubaoStatus.available ? 'available' : 'unavailable']">
          {{ doubaoStatus.available ? '可用' : '不可用' }}
        </span>
        <span class="api-message">{{ doubaoStatus.message }}</span>
      </div>
    </div>
    <div class="mode-indicator">
      <span class="mode-label">当前模式:</span>
      <span :class="['mode-value', isUsingLocalMode ? 'local' : 'api']">
        {{ isUsingLocalMode ? '本地演示模式' : '真实 API 模式' }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { healthApi } from '../../api/healthApi';

const qwenStatus = ref({
  available: false,
  message: '检查中...',
  hasApiKey: false
});

const doubaoStatus = ref({
  available: false,
  message: '检查中...',
  hasApiKey: false
});

const isUsingLocalMode = computed(() => {
  return !qwenStatus.value.available || !doubaoStatus.value.available;
});

const refreshStatus = async () => {
  try {
    const status = await healthApi.getApiStatus();
    qwenStatus.value = status.qwen;
    doubaoStatus.value = status.doubao;
  } catch (error) {
    console.error('刷新 API 状态失败:', error);
  }
};

onMounted(() => {
  refreshStatus();
});
</script>

<style scoped>
.api-status-indicator {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.api-status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.api-status-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.refresh-button {
  display: flex;
  align-items: center;
  gap: 4px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.refresh-button:hover {
  background-color: #0069d9;
}

.refresh-icon {
  font-size: 12px;
}

.api-status-list {
  margin-bottom: 12px;
}

.api-status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding: 8px;
  background-color: white;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.api-name {
  font-weight: 500;
  width: 80px;
  flex-shrink: 0;
}

.status-indicator {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  width: 60px;
  text-align: center;
  flex-shrink: 0;
}

.status-indicator.available {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-indicator.unavailable {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.api-message {
  font-size: 14px;
  color: #6c757d;
  flex: 1;
}

.mode-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background-color: white;
  border-radius: 4px;
  border: 1px solid #e9ecef;
  font-size: 14px;
}

.mode-label {
  font-weight: 500;
  color: #333;
}

.mode-value {
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.mode-value.local {
  background-color: #fff3cd;
  color: #856404;
  border: 1px solid #ffeeba;
}

.mode-value.api {
  background-color: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
}
</style>