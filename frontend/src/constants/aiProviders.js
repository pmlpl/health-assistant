/** 文本对话服务商 */
export const TEXT_PROVIDERS = [
  { value: 'lmstudio', label: '本地 LM Studio' },
  { value: 'dashscope', label: '通义千问 (阿里云 DashScope)' },
  { value: 'deepseek', label: 'DeepSeek' },
  { value: 'doubao', label: '豆包 (火山方舟)' },
  { value: 'other', label: '其他 (OpenAI 兼容 API)' },
  { value: 'auto', label: '自动（先本地 LM，再云端）' },
];

export const CUSTOM_MODEL = '__custom__';

/** 各云厂商预设模型；最后一项为自定义 */
export const CLOUD_MODEL_OPTIONS = {
  dashscope: [
    { value: 'qwen-turbo', label: 'qwen-turbo' },
    { value: 'qwen-plus', label: 'qwen-plus' },
    { value: 'qwen-max', label: 'qwen-max' },
    { value: 'qwen-vl-flash', label: 'qwen-vl-flash（拍照识食·省）' },
    { value: 'qwen-vl-plus', label: 'qwen-vl-plus（拍照识食）' },
    { value: 'qwen-vl-max', label: 'qwen-vl-max（拍照识食·贵）' },
    { value: CUSTOM_MODEL, label: '自定义模型名…' },
  ],
  deepseek: [
    { value: 'deepseek-v4-flash', label: 'deepseek-v4-flash' },
    { value: 'deepseek-v4-pro', label: 'deepseek-v4-pro' },
    { value: CUSTOM_MODEL, label: '自定义模型名…' },
  ],
  doubao: [
    { value: 'doubao-seed-1-8-251228', label: 'doubao-seed-1-8-251228' },
    { value: CUSTOM_MODEL, label: '自定义 (推理接入点 ID)…' },
  ],
  other: [
    { value: 'gpt-4o-mini', label: 'gpt-4o-mini' },
    { value: 'gpt-3.5-turbo', label: 'gpt-3.5-turbo' },
    { value: CUSTOM_MODEL, label: '自定义模型名…' },
  ],
};

/** 拍照识食视觉服务商（与文本对话独立配置） */
export const VISION_PROVIDERS = [
  { value: 'doubao', label: '豆包 (火山方舟)' },
  { value: 'dashscope', label: '通义千问 (DashScope)' },
  { value: 'lmstudio', label: '本地 LM Studio 视觉模型' },
  { value: 'unset', label: '未指定（按系统默认顺序）' },
];

export const VISION_MODEL_OPTIONS = {
  doubao: [
    { value: 'ep-20260529143011-9jk6q', label: 'ep-…9jk6q（拍照识食·平台接入点）' },
    { value: 'doubao-seed-2-0-lite-260215', label: 'doubao-seed-2-0-lite-260215' },
    { value: CUSTOM_MODEL, label: '自定义 ep- 或模型名…' },
  ],
  dashscope: [
    { value: 'qwen-vl-flash', label: 'qwen-vl-flash（省）' },
    { value: 'qwen-vl-plus', label: 'qwen-vl-plus' },
    { value: CUSTOM_MODEL, label: '自定义…' },
  ],
  lmstudio: [
    { value: CUSTOM_MODEL, label: '填写 LM 中加载的视觉模型名（如 Qwen2-VL）' },
  ],
};

export function isCloudTextProvider(provider) {
  return ['dashscope', 'deepseek', 'doubao', 'other'].includes(provider);
}

export function defaultModelForProvider(provider) {
  const opts = CLOUD_MODEL_OPTIONS[provider];
  return opts?.[0]?.value || '';
}
