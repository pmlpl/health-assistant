<template>
  <div class="api-status-indicator">
    <div class="api-status-header">
      <h3>AI 设置</h3>
      <button type="button" class="refresh-button" @click="loadSettings">刷新</button>
    </div>

    <p v-if="deploymentMode === 'prod'" class="hint prod-hint">
      云部署模式：请自备 API Key 或 LM Studio 穿透地址，平台不代付 AI 费用。
    </p>
    <p v-else class="hint">开发模式：默认使用本机 LM Studio（Qwen3.5:9b）。</p>

    <div class="form-group">
      <label>文本模型</label>
      <select v-model="form.textProvider">
        <option value="lmstudio">本地 LM Studio</option>
        <option value="dashscope">云端 DashScope</option>
        <option value="auto">自动（先本地后云端）</option>
        <option v-if="deploymentMode === 'prod'" value="unset">未配置</option>
      </select>
    </div>

    <div class="form-group">
      <label>LM Studio 地址</label>
      <input v-model="form.lmstudioBaseUrl" type="text" placeholder="http://127.0.0.1:1234/v1" />
    </div>
    <div class="form-group">
      <label>模型名称</label>
      <input v-model="form.lmstudioModel" type="text" placeholder="qwen3.5-9b" />
    </div>

    <div class="form-group">
      <label>DashScope Key <span v-if="settings.hasDashscopeKey" class="masked">({{ settings.dashscopeKeyMasked }})</span></label>
      <input v-model="form.dashscopeApiKey" type="password" placeholder="留空则不修改" />
    </div>
    <div class="form-group">
      <label>豆包 Key <span v-if="settings.hasDoubaoKey" class="masked">({{ settings.doubaoKeyMasked }})</span></label>
      <input v-model="form.doubaoApiKey" type="password" placeholder="留空则不修改" />
    </div>
    <div class="form-group">
      <label>Pexels Key（食谱配图） <span v-if="settings.hasPexelsKey" class="masked">({{ settings.pexelsKeyMasked }})</span></label>
      <input v-model="form.pexelsApiKey" type="password" placeholder="留空则不修改" />
    </div>

    <div class="actions">
      <button type="button" class="save-btn" @click="saveSettings">保存</button>
    </div>

    <div class="test-row">
      <button type="button" class="test-btn" @click="testConnection('lmstudio')">测试 LM Studio</button>
      <button type="button" class="test-btn" @click="testConnection('dashscope')">测试 DashScope</button>
      <button type="button" class="test-btn" @click="testConnection('pexels')">测试 Pexels</button>
    </div>

    <div class="mode-indicator">
      <span class="mode-label">当前后端:</span>
      <span class="mode-value">{{ settings.activeTextBackend || '—' }}</span>
      <span :class="['badge', settings.configured ? 'ok' : 'warn']">
        {{ settings.configured ? '已配置' : '未配置' }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { healthApi } from '../../api/healthApi';

const settings = ref({
  configured: false,
  activeTextBackend: '',
  deploymentMode: 'dev',
  hasDashscopeKey: false,
  hasDoubaoKey: false,
  hasPexelsKey: false,
  dashscopeKeyMasked: '',
  doubaoKeyMasked: '',
  pexelsKeyMasked: ''
});

const deploymentMode = ref('dev');

const form = reactive({
  textProvider: 'lmstudio',
  lmstudioBaseUrl: 'http://127.0.0.1:1234/v1',
  lmstudioModel: 'qwen3.5-9b',
  dashscopeApiKey: '',
  doubaoApiKey: '',
  pexelsApiKey: ''
});

const loadSettings = async () => {
  try {
    const data = await healthApi.getAiSettings();
    settings.value = data;
    deploymentMode.value = data.deploymentMode || 'dev';
    form.textProvider = data.textProvider || 'lmstudio';
    form.lmstudioBaseUrl = data.lmstudioBaseUrl || '';
    form.lmstudioModel = data.lmstudioModel || '';
    form.dashscopeApiKey = '';
    form.doubaoApiKey = '';
    form.pexelsApiKey = '';
  } catch (e) {
    ElMessage.warning('请先登录后配置 AI 设置');
  }
};

const saveSettings = async () => {
  try {
    const data = await healthApi.updateAiSettings({ ...form });
    settings.value = data;
    form.dashscopeApiKey = '';
    form.doubaoApiKey = '';
    form.pexelsApiKey = '';
    ElMessage.success('AI 设置已保存');
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败');
  }
};

const testConnection = async (type) => {
  try {
    const result = await healthApi.testAiConnection(type);
    if (result.reachable) {
      ElMessage.success(result.message);
    } else {
      ElMessage.warning(result.message);
    }
  } catch (e) {
    ElMessage.error('测试失败');
  }
};

onMounted(() => {
  loadSettings();
});
</script>

<style scoped>
.api-status-indicator {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  width: 360px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.api-status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.api-status-header h3 {
  margin: 0;
  font-size: 16px;
}

.hint {
  font-size: 12px;
  color: #666;
  margin: 0 0 12px;
}

.prod-hint {
  color: #856404;
  background: #fff3cd;
  padding: 8px;
  border-radius: 4px;
}

.form-group {
  margin-bottom: 10px;
}

.form-group label {
  display: block;
  font-size: 12px;
  margin-bottom: 4px;
  color: #333;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 13px;
  box-sizing: border-box;
}

.masked {
  color: #888;
  font-weight: normal;
}

.actions {
  margin: 12px 0;
}

.save-btn,
.refresh-button,
.test-btn {
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 13px;
}

.save-btn {
  background: #28a745;
  color: #fff;
  width: 100%;
}

.refresh-button {
  background: #007bff;
  color: #fff;
}

.test-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.test-btn {
  background: #e9ecef;
  color: #333;
}

.mode-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
}

.badge.ok {
  background: #d4edda;
  color: #155724;
}

.badge.warn {
  background: #f8d7da;
  color: #721c24;
}
</style>
