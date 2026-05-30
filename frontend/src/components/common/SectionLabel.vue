<template>
  <div class="section-label" :class="{ 'section-label--pulse': pulse }">
    <!-- 方形标记点，无脉冲动画 -->
    <span v-if="showDot" class="section-label__dot" aria-hidden="true" />
    <span class="section-label__text">{{ label }}</span>
  </div>
</template>

<script setup>
/**
 * 区块标签 — Minimalist Monochrome 等宽字体标签
 * 黑框、大写、宽字距，无彩色圆点动画
 */
defineProps({
  label: { type: String, required: true },
  /** 是否显示左侧方形标记 */
  showDot: { type: Boolean, default: true },
  /** 保留 prop 兼容，单色系统不启用脉冲 */
  pulse: { type: Boolean, default: false },
});
</script>
