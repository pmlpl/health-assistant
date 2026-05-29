<template>
  <div class="api-status-indicator">
    <div class="api-status-header">
      <h3>AI 设置</h3>
      <button type="button" class="refresh-button" @click="loadSettings">刷新</button>
    </div>

    <p v-if="deploymentMode === 'prod'" class="hint prod-hint">
      云部署请自备各厂商 API Key；平台不代付费用。失败时不会使用模拟回复，便于排查。
    </p>
    <p v-else class="hint">开发模式默认 LM Studio；请按所选服务商填写对应 Key 与模型。</p>

    <p
      v-if="deploymentMode === 'dev' && form.textProvider !== 'lmstudio'"
      class="hint warn-hint"
    >
      当前选择的是「{{ providerLabel }}」，对话不会走本地 LM Studio。要用本地模型请选「本地 LM Studio」或点下方一键配置。
    </p>

    <div v-if="deploymentMode === 'dev'" class="quick-lm-row">
      <button type="button" class="test-btn" @click="applyLocalLmDefaults">一键使用本地 LM Studio</button>
    </div>

    <div class="form-group">
      <label>文本对话服务商</label>
      <select v-model="form.textProvider" @change="onProviderChange">
        <option v-for="p in TEXT_PROVIDERS" :key="p.value" :value="p.value">{{ p.label }}</option>
      </select>
    </div>

    <!-- LM Studio -->
    <template v-if="form.textProvider === 'lmstudio' || form.textProvider === 'auto'">
      <p class="hint lm-hint">
        LM Studio：填服务地址 + 模型 ID（与 LM Studio 界面一致）。
        地址须为<strong>运行 Java 后端的电脑</strong>能访问的地址；填局域网 IP 时，后端必须能 ping 通该 IP，与浏览器能否打开无关。
      </p>
      <div class="form-group">
        <label>LM Studio 地址</label>
        <input v-model="form.lmstudioBaseUrl" type="text" placeholder="http://127.0.0.1:1234/v1" />
      </div>
      <div class="form-group">
        <label>LM Studio 模型名称</label>
        <input v-model="form.lmstudioModel" type="text" placeholder="qwen3.5-9b" />
      </div>
    </template>

    <!-- 云端：模型 + Key -->
    <template v-if="isCloudProvider">
      <div class="form-group">
        <label>模型</label>
        <select v-model="modelSelect">
          <option v-for="m in currentModelOptions" :key="m.value" :value="m.value">{{ m.label }}</option>
        </select>
      </div>
      <div v-if="modelSelect === CUSTOM_MODEL" class="form-group">
        <label>自定义模型名 / 接入点 ID</label>
        <input v-model="form.cloudModelCustom" type="text" placeholder="如 deepseek-chat 或 ep-xxx" />
      </div>

      <div v-if="form.textProvider === 'dashscope'" class="form-group">
        <label>通义 DashScope Key <span v-if="settings.hasDashscopeKey" class="masked">({{ settings.dashscopeKeyMasked }})</span></label>
        <input v-model="form.dashscopeApiKey" type="password" placeholder="留空则不修改" />
      </div>
      <div v-if="form.textProvider === 'deepseek'" class="form-group">
        <label>DeepSeek API Key <span v-if="settings.hasDeepseekKey" class="masked">({{ settings.deepseekKeyMasked }})</span></label>
        <input v-model="form.deepseekApiKey" type="password" placeholder="留空则不修改" />
      </div>
      <div v-if="form.textProvider === 'doubao'" class="form-group">
        <label>豆包 API Key <span v-if="settings.hasDoubaoKey" class="masked">({{ settings.doubaoKeyMasked }})</span></label>
        <input v-model="form.doubaoApiKey" type="password" placeholder="与食物识别可共用，留空则不修改" />
      </div>
      <template v-if="form.textProvider === 'other'">
        <div class="form-group">
          <label>API 根地址</label>
          <input v-model="form.customApiBaseUrl" type="text" placeholder="https://api.example.com" />
        </div>
        <div class="form-group">
          <label>API Key <span v-if="settings.hasCustomKey" class="masked">({{ settings.customKeyMasked }})</span></label>
          <input v-model="form.customApiKey" type="password" placeholder="留空则不修改" />
        </div>
      </template>
    </template>

    <div class="form-group">
      <label>Pexels Key（食谱配图） <span v-if="settings.hasPexelsKey" class="masked">({{ settings.pexelsKeyMasked }})</span></label>
      <input v-model="form.pexelsApiKey" type="password" placeholder="留空则不修改" />
    </div>

    <div class="actions">
      <button type="button" class="save-btn" @click="saveSettings">保存</button>
    </div>

    <div class="test-row">
      <button v-if="form.textProvider === 'lmstudio' || form.textProvider === 'auto'" type="button" class="test-btn" @click="testConnection('lmstudio')">测试 LM Studio</button>
      <button v-if="form.textProvider === 'dashscope' || form.textProvider === 'auto'" type="button" class="test-btn" @click="testConnection('dashscope')">测试通义</button>
      <button v-if="form.textProvider === 'deepseek' || form.textProvider === 'auto'" type="button" class="test-btn" @click="testConnection('deepseek')">测试 DeepSeek</button>
      <button v-if="form.textProvider === 'doubao' || form.textProvider === 'auto'" type="button" class="test-btn" @click="testConnection('doubao')">测试豆包</button>
      <button v-if="form.textProvider === 'other'" type="button" class="test-btn" @click="testConnection('other')">测试自定义 API</button>
      <button type="button" class="test-btn" @click="testConnection('pexels')">测试 Pexels</button>
    </div>

    <div class="mode-indicator">
      <span class="mode-label">当前服务商:</span>
      <span class="mode-value">{{ providerLabel }}</span>
      <span v-if="form.textProvider === 'lmstudio'" class="badge ok">对话走本地 LM</span>
      <span v-else-if="form.textProvider === 'auto'" class="badge warn">自动（优先本地）</span>
      <span v-else class="badge warn">不走本地 LM</span>
      <span :class="['badge', settings.configured ? 'ok' : 'warn']">
        {{ settings.configured ? '已配置' : '未配置' }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { healthApi } from '../../api/healthApi';
import { notifySuccess, notifyWarning, notifyError } from '../../utils/notify';
import {
  TEXT_PROVIDERS,
  CLOUD_MODEL_OPTIONS,
  CUSTOM_MODEL,
  isCloudTextProvider,
  defaultModelForProvider,
} from '../../constants/aiProviders';

const settings = ref({
  configured: false,
  activeTextBackend: '',
  deploymentMode: 'dev',
});

const deploymentMode = ref('dev');
const modelSelect = ref('');

const form = reactive({
  textProvider: 'lmstudio',
  lmstudioBaseUrl: 'http://127.0.0.1:1234/v1',
  lmstudioModel: 'qwen3.5-9b',
  cloudModelCustom: '',
  customApiBaseUrl: '',
  dashscopeApiKey: '',
  deepseekApiKey: '',
  customApiKey: '',
  doubaoApiKey: '',
  pexelsApiKey: '',
});

const isCloudProvider = computed(() => isCloudTextProvider(form.textProvider));

const currentModelOptions = computed(() => CLOUD_MODEL_OPTIONS[form.textProvider] || []);

const providerLabel = computed(() => {
  const p = TEXT_PROVIDERS.find((x) => x.value === form.textProvider);
  return p?.label || form.textProvider;
});

function resolveCloudModelForSave() {
  if (!isCloudProvider.value) return undefined;
  if (modelSelect.value === CUSTOM_MODEL) {
    return form.cloudModelCustom?.trim() || undefined;
  }
  return modelSelect.value || undefined;
}

function syncModelSelectFromSaved(cloudModel, provider) {
  if (!isCloudTextProvider(provider)) {
    modelSelect.value = '';
    return;
  }
  const opts = CLOUD_MODEL_OPTIONS[provider] || [];
  const preset = opts.find((o) => o.value === cloudModel);
  if (preset && preset.value !== CUSTOM_MODEL) {
    modelSelect.value = cloudModel;
    form.cloudModelCustom = '';
  } else if (cloudModel) {
    modelSelect.value = CUSTOM_MODEL;
    form.cloudModelCustom = cloudModel;
  } else {
    modelSelect.value = defaultModelForProvider(provider);
    form.cloudModelCustom = '';
  }
}

const onProviderChange = () => {
  if (isCloudProvider.value && !modelSelect.value) {
    modelSelect.value = defaultModelForProvider(form.textProvider);
  }
};

const applyLocalLmDefaults = async () => {
  form.textProvider = 'lmstudio';
  form.lmstudioBaseUrl = 'http://127.0.0.1:1234/v1';
  form.lmstudioModel = 'qwen3.5-9b';
  try {
    const data = await healthApi.updateAiSettings(buildPayload());
    settings.value = data;
    syncModelSelectFromSaved(data.cloudModel, form.textProvider);
    notifySuccess(
      '已切换为「本地 LM Studio」并保存。请确认 LM Studio 已开启 Local Server，再点「测试 LM Studio」。',
      '本地模型'
    );
  } catch (e) {
    notifyError(e?.response?.data?.message || e?.message || '保存失败', '本地模型');
  }
};

const loadSettings = async () => {
  try {
    const data = await healthApi.getAiSettings();
    settings.value = data;
    deploymentMode.value = data.deploymentMode || 'dev';
    const isDev = deploymentMode.value === 'dev';
    let provider = data.textProvider;
    if (!provider || provider === 'unset') {
      provider = isDev ? 'lmstudio' : 'unset';
    }
    form.textProvider = provider;
    form.lmstudioBaseUrl = data.lmstudioBaseUrl || (isDev ? 'http://127.0.0.1:1234/v1' : '');
    form.lmstudioModel = data.lmstudioModel || (isDev ? 'qwen3.5-9b' : '');
    form.customApiBaseUrl = data.customApiBaseUrl || '';
    syncModelSelectFromSaved(data.cloudModel, form.textProvider);
    form.dashscopeApiKey = '';
    form.deepseekApiKey = '';
    form.customApiKey = '';
    form.doubaoApiKey = '';
    form.pexelsApiKey = '';
  } catch (e) {
    notifyWarning('请先登录后再配置 AI 设置', '未登录');
  }
};

const buildPayload = () => ({
  textProvider: form.textProvider,
  lmstudioBaseUrl: form.lmstudioBaseUrl,
  lmstudioModel: form.lmstudioModel,
  cloudModel: resolveCloudModelForSave(),
  customApiBaseUrl: form.customApiBaseUrl,
  dashscopeApiKey: form.dashscopeApiKey || undefined,
  deepseekApiKey: form.deepseekApiKey || undefined,
  customApiKey: form.customApiKey || undefined,
  doubaoApiKey: form.doubaoApiKey || undefined,
  pexelsApiKey: form.pexelsApiKey || undefined,
});

const saveSettings = async () => {
  try {
    const data = await healthApi.updateAiSettings(buildPayload());
    settings.value = data;
    syncModelSelectFromSaved(data.cloudModel, form.textProvider);
    form.dashscopeApiKey = '';
    form.deepseekApiKey = '';
    form.customApiKey = '';
    form.doubaoApiKey = '';
    form.pexelsApiKey = '';
    notifySuccess('AI 设置已保存', '保存成功');
  } catch (e) {
    notifyError(e?.response?.data?.message || e?.message || '保存失败', '保存失败');
  }
};

const testConnection = async (type) => {
  try {
    const result = await healthApi.testAiConnection({
      type,
      ...buildPayload(),
    });
    const ok = result?.reachable === true;
    const detail = result?.message || (ok ? '连接成功' : '连接失败，请检查配置');
    if (ok) {
      notifySuccess(detail, '连接成功');
    } else {
      notifyError(detail, '连接失败');
    }
  } catch (e) {
    notifyError(e?.response?.data?.message || e?.message || '测试请求失败', '连接失败');
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
  width: 340px;
  max-height: 85vh;
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

.lm-hint {
  background: #e8f4fd;
  padding: 8px;
  border-radius: 4px;
  color: #0c5460;
}

.warn-hint {
  color: #856404;
  background: #fff3cd;
  padding: 8px;
  border-radius: 4px;
}

.quick-lm-row {
  margin: 0 0 12px;
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
