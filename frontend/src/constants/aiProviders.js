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
    { value: CUSTOM_MODEL, label: '自定义模型名…' },
  ],
  deepseek: [
    { value: 'deepseek-chat', label: 'deepseek-chat' },
    { value: 'deepseek-reasoner', label: 'deepseek-reasoner' },
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

export function isCloudTextProvider(provider) {
  return ['dashscope', 'deepseek', 'doubao', 'other'].includes(provider);
}

export function defaultModelForProvider(provider) {
  const opts = CLOUD_MODEL_OPTIONS[provider];
  return opts?.[0]?.value || '';
}
