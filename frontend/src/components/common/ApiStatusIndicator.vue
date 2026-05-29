<template>
  <div class="api-status-indicator">
    <div class="api-status-header">
      <h3>AI 设置</h3>
      <button type="button" class="refresh-button" @click="loadSettings">刷新</button>
    </div>

    <p v-if="deploymentMode === 'prod'" class="hint prod-hint">
      平台默认：<strong>DeepSeek</strong> 对话（不限次）· <strong>豆包</strong> 拍照识食与食谱配图（各 10 次试用）。
      额度用尽或未配置时请自备 Key。详见「使用手册」。
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
        <input v-model="form.cloudModelCustom" type="text" placeholder="如 deepseek-v4-flash 或 ep-xxx" />
      </div>

      <div v-if="form.textProvider === 'dashscope'" class="form-group">
        <label>通义 DashScope Key <span v-if="settings.hasDashscopeKey" class="masked">({{ settings.dashscopeKeyMasked }})</span></label>
        <input v-model="form.dashscopeApiKey" type="password" placeholder="留空则不修改；对话与拍照识食共用" />
        <p class="hint pexels-hint">若已配置豆包 Key，拍照会优先走豆包（更省）。仅用通义时请选择 qwen-vl-flash。</p>
      </div>
      <div v-if="form.textProvider === 'deepseek'" class="form-group">
        <label>DeepSeek API Key <span v-if="settings.hasDeepseekKey" class="masked">({{ settings.deepseekKeyMasked }})</span></label>
        <input v-model="form.deepseekApiKey" type="password" placeholder="留空则使用平台 DeepSeek（不限次）" />
      </div>
      <div v-if="form.textProvider === 'doubao'" class="form-group">
        <label>豆包 API Key <span v-if="settings.hasDoubaoKey" class="masked">({{ settings.doubaoKeyMasked }})</span></label>
        <input v-model="form.doubaoApiKey" type="password" placeholder="留空则使用平台豆包（识图/食谱配图各 10 次）" />
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

    <!-- 拍照识食：独立于文本对话 -->
    <div class="photo-vision-block">
      <h4 class="section-title">拍照识食（视觉模型）</h4>
      <p class="hint lm-hint">
        与文本对话<strong>分开配置</strong>。新用户可使用平台有限试用；额度用尽后请在下方填写您自己的 Key。
        <router-link to="/guide" class="guide-link">查看使用手册</router-link>
      </p>
      <p v-if="settings.platformQuota?.trialEnabled" class="hint quota-hint">
        平台试用 —
        对话：{{ settings.platformQuota.textUnlimited ? '不限' : `${settings.platformQuota.textRemaining}/${settings.platformQuota.textLimit}` }}；
        拍照：{{ settings.platformQuota.imageRemaining }}/{{ settings.platformQuota.imageLimit }}；
        食谱配图：{{ settings.platformQuota.recipeImageRemaining }}/{{ settings.platformQuota.recipeImageLimit }}
      </p>
      <div class="form-group">
        <label>视觉服务商</label>
        <select v-model="form.visionProvider">
          <option v-for="p in VISION_PROVIDERS" :key="p.value" :value="p.value">{{ p.label }}</option>
        </select>
      </div>
      <template v-if="form.visionProvider && form.visionProvider !== 'unset'">
        <div class="form-group">
          <label>视觉模型</label>
          <select v-if="visionModelOptions.length > 1" v-model="visionModelSelect">
            <option v-for="m in visionModelOptions" :key="m.value" :value="m.value">{{ m.label }}</option>
          </select>
          <input
            v-else
            v-model="form.visionModelCustom"
            type="text"
            placeholder="如 doubao-seed-2-0-lite-260215 或 Qwen2-VL"
          />
        </div>
        <div v-if="visionModelSelect === CUSTOM_MODEL" class="form-group">
          <label>自定义视觉模型 / 接入点 ID</label>
          <input v-model="form.visionModelCustom" type="text" placeholder="doubao-seed-2-0-lite-260215 或 ep-xxx" />
        </div>
        <template v-if="form.visionProvider === 'lmstudio'">
          <div class="form-group">
            <label>LM Studio 地址（视觉）</label>
            <input v-model="form.visionLmstudioBaseUrl" type="text" placeholder="http://127.0.0.1:1234/v1" />
          </div>
        </template>
        <template v-else>
          <div class="form-group">
            <label>拍照 API Key <span v-if="settings.hasVisionApiKey" class="masked">({{ settings.visionApiKeyMasked }})</span></label>
            <input v-model="form.visionApiKey" type="password" placeholder="留空则不修改；与对话 Key 可相同" />
          </div>
        </template>
      </template>
      <div class="vision-test-row">
        <button
          v-if="form.visionProvider === 'doubao'"
          type="button"
          class="test-btn"
          @click="testConnection('doubao-vision')"
        >测试豆包视觉</button>
        <button
          v-if="form.visionProvider === 'dashscope'"
          type="button"
          class="test-btn"
          @click="testConnection('dashscope-vision')"
        >测试通义视觉</button>
        <button
          v-if="form.visionProvider === 'lmstudio'"
          type="button"
          class="test-btn"
          @click="testConnection('lmstudio')"
        >测试 LM 视觉</button>
      </div>
      <span :class="['badge', settings.visionConfigured ? 'ok' : 'warn']">
        {{ settings.visionConfigured ? '拍照已配置' : '拍照未配置（可试用平台额度）' }}
      </span>
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
  VISION_PROVIDERS,
  VISION_MODEL_OPTIONS,
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
const visionModelSelect = ref('');

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
  visionProvider: 'doubao',
  visionModelCustom: '',
  visionApiKey: '',
  visionLmstudioBaseUrl: '',
});

const isCloudProvider = computed(() => isCloudTextProvider(form.textProvider));

const currentModelOptions = computed(() => CLOUD_MODEL_OPTIONS[form.textProvider] || []);

const visionModelOptions = computed(() => VISION_MODEL_OPTIONS[form.visionProvider] || []);

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

function resolveVisionModelForSave() {
  if (!form.visionProvider || form.visionProvider === 'unset') return undefined;
  if (visionModelSelect.value === CUSTOM_MODEL || visionModelOptions.value.length <= 1) {
    return form.visionModelCustom?.trim() || undefined;
  }
  return visionModelSelect.value || undefined;
}

function syncVisionModelFromSaved(visionModel, provider) {
  if (!provider || provider === 'unset') {
    visionModelSelect.value = '';
    form.visionModelCustom = '';
    return;
  }
  const opts = VISION_MODEL_OPTIONS[provider] || [];
  const preset = opts.find((o) => o.value === visionModel);
  if (preset && preset.value !== CUSTOM_MODEL) {
    visionModelSelect.value = visionModel;
    form.visionModelCustom = '';
  } else if (visionModel) {
    visionModelSelect.value = CUSTOM_MODEL;
    form.visionModelCustom = visionModel;
  } else if (opts.length > 0 && opts[0].value !== CUSTOM_MODEL) {
    visionModelSelect.value = opts[0].value;
    form.visionModelCustom = '';
  } else {
    visionModelSelect.value = CUSTOM_MODEL;
    form.visionModelCustom = '';
  }
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
    form.visionProvider = data.visionProvider || 'doubao';
    form.visionLmstudioBaseUrl = data.visionLmstudioBaseUrl || '';
    syncVisionModelFromSaved(data.visionModel, form.visionProvider);
    form.visionApiKey = '';
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
  visionProvider: form.visionProvider || undefined,
  visionModel: resolveVisionModelForSave(),
  visionApiKey: form.visionApiKey || undefined,
  visionLmstudioBaseUrl: form.visionLmstudioBaseUrl || undefined,
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
    form.visionApiKey = '';
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
      // 测试使用表单中的地址/模型，对话使用已保存配置；LM 测试成功后自动保存，避免「测试通、聊天无请求」
      if (type === 'lmstudio') {
        try {
          await saveSettings();
        } catch {
          notifyWarning('连接成功，但自动保存失败，请手动点击「保存设置」后再咨询。', '请保存设置');
        }
      }
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

.pexels-hint {
  margin-top: 6px;
  margin-bottom: 0;
  font-size: 11px;
  color: #666;
}

.photo-vision-block {
  margin-bottom: 12px;
  padding: 10px;
  background: #f0f7ff;
  border-radius: 4px;
  border: 1px solid #cce5ff;
}

.section-title {
  margin: 0 0 8px;
  font-size: 14px;
  color: #004085;
}

.quota-hint {
  background: #fff3cd;
  padding: 6px 8px;
  border-radius: 4px;
  color: #856404;
}

.guide-link {
  color: #007bff;
  margin-left: 4px;
}

.vision-test-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0;
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
