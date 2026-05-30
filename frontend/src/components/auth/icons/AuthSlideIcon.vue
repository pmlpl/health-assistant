<template>
  <!-- 登录轮播专用图标：圆底 + 线性插画，适配蓝紫渐变背景 -->
  <div
    class="auth-slide-icon"
    :class="`auth-slide-icon--${name}`"
    :style="{ width: `${size}px`, height: `${size}px` }"
    aria-hidden="true"
  >
    <svg
      :width="size"
      :height="size"
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
    >
      <defs>
        <linearGradient :id="`${uid}-glow`" x1="8" y1="8" x2="56" y2="56" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95" />
          <stop offset="100%" stop-color="#a5f3fc" stop-opacity="0.85" />
        </linearGradient>
        <linearGradient :id="`${uid}-accent`" x1="16" y1="48" x2="48" y2="16" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stop-color="#5eead4" />
          <stop offset="100%" stop-color="#ffffff" />
        </linearGradient>
      </defs>

      <!-- 统一柔光底 -->
      <circle cx="32" cy="32" r="28" fill="rgba(255,255,255,0.08)" />
      <circle cx="32" cy="32" r="28" :stroke="`url(#${uid}-glow)`" stroke-width="1.25" stroke-opacity="0.35" />

      <!-- AI 智能分析：芯片 + 神经节点 -->
      <g v-if="name === 'ai'">
        <rect x="20" y="22" width="24" height="20" rx="6" :stroke="`url(#${uid}-glow)`" stroke-width="2" />
        <path
          d="M26 32h12M32 26v12"
          stroke="white"
          stroke-width="1.75"
          stroke-linecap="round"
          opacity="0.9"
        />
        <circle cx="32" cy="32" r="3" :fill="`url(#${uid}-accent)`" />
        <circle cx="14" cy="18" r="2.5" fill="#5eead4" />
        <circle cx="50" cy="18" r="2.5" fill="#5eead4" />
        <circle cx="14" cy="46" r="2.5" fill="#5eead4" />
        <circle cx="50" cy="46" r="2.5" fill="#5eead4" />
        <path
          d="M16 18h4M44 18h4M16 46h4M44 46h4M20 24v-2M44 24v-2M20 42v2M44 42v2"
          stroke="white"
          stroke-width="1.5"
          stroke-linecap="round"
          opacity="0.55"
        />
        <path
          d="M17 14l3 2M47 14l-3 2"
          stroke="white"
          stroke-width="1.5"
          stroke-linecap="round"
          opacity="0.75"
        />
      </g>

      <!-- 健康数据追踪：折线 + 柱状 -->
      <g v-else-if="name === 'chart'">
        <rect x="14" y="16" width="36" height="32" rx="8" :stroke="`url(#${uid}-glow)`" stroke-width="2" />
        <path
          d="M20 38l6-8 5 4 8-12 5 6"
          stroke="white"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
        <path
          d="M20 38l6-8 5 4 8-12 5 6"
          :stroke="`url(#${uid}-accent)`"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          opacity="0.45"
        />
        <rect x="21" y="40" width="4" height="6" rx="1" fill="white" fill-opacity="0.35" />
        <rect x="29" y="36" width="4" height="10" rx="1" :fill="`url(#${uid}-accent)`" fill-opacity="0.8" />
        <rect x="37" y="32" width="4" height="14" rx="1" fill="white" fill-opacity="0.55" />
        <circle cx="44" cy="22" r="3" fill="#5eead4" />
        <path d="M44 19v6M41 22h6" stroke="white" stroke-width="1.25" stroke-linecap="round" />
      </g>

      <!-- 个性化目标：靶心 + 轨迹 -->
      <g v-else-if="name === 'target'">
        <circle cx="32" cy="34" r="16" :stroke="`url(#${uid}-glow)`" stroke-width="2" />
        <circle cx="32" cy="34" r="10" stroke="white" stroke-width="1.75" stroke-opacity="0.55" />
        <circle cx="32" cy="34" r="4.5" :fill="`url(#${uid}-accent)`" />
        <circle cx="32" cy="34" r="1.5" fill="white" />
        <path
          d="M46 18l-8 8"
          stroke="white"
          stroke-width="2"
          stroke-linecap="round"
        />
        <path
          d="M46 18h-6v6"
          stroke="#5eead4"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
        <path
          d="M18 20c2-3 5-5 9-5"
          stroke="white"
          stroke-width="1.5"
          stroke-linecap="round"
          opacity="0.4"
        />
      </g>

      <!-- 智能食谱：碗 + 叶片 -->
      <g v-else-if="name === 'recipe'">
        <path
          d="M16 36c0 10 7.2 14 16 14s16-4 16-14"
          :stroke="`url(#${uid}-glow)`"
          stroke-width="2"
          stroke-linecap="round"
        />
        <path
          d="M14 36h36"
          stroke="white"
          stroke-width="2"
          stroke-linecap="round"
          opacity="0.85"
        />
        <ellipse cx="32" cy="36" rx="18" ry="5" fill="white" fill-opacity="0.08" />
        <circle cx="24" cy="33" r="2.5" fill="#5eead4" fill-opacity="0.9" />
        <circle cx="32" cy="31" r="2.5" fill="white" fill-opacity="0.7" />
        <circle cx="40" cy="33" r="2.5" fill="#5eead4" fill-opacity="0.7" />
        <path
          d="M38 18c6 2 8 8 4 12-3 3-8 1-9-3 3 1 6 0 8-2-2-1-4-2-3-7z"
          :fill="`url(#${uid}-accent)`"
          fill-opacity="0.95"
        />
        <path
          d="M22 22c-1 4 1 7 4 8"
          stroke="white"
          stroke-width="1.5"
          stroke-linecap="round"
          opacity="0.45"
        />
        <path
          d="M28 14c0-2 1.5-3 3-3s3 1 3 3"
          stroke="white"
          stroke-width="1.5"
          stroke-linecap="round"
          opacity="0.35"
        />
      </g>
    </svg>
  </div>
</template>

<script setup>
import { useId } from 'vue';

/** 支持的图标名称 */
defineProps({
  name: {
    type: String,
    required: true,
    validator: (v) => ['ai', 'chart', 'target', 'recipe'].includes(v),
  },
  /** 显示尺寸（像素） */
  size: { type: Number, default: 64 },
});

/** 渐变 id 后缀，避免同页多实例冲突 */
const uid = useId();
</script>

<style scoped>
.auth-slide-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.auth-slide-icon svg {
  display: block;
  filter: drop-shadow(0 6px 16px rgba(0, 0, 0, 0.18));
}
</style>
