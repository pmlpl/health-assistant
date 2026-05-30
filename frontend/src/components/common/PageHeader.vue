<template>
  <!-- 全站统一页头：蓝底、上中下垂直排列（标签 → 标题 → 统计条） -->
  <header class="page-shell-header">
    <!-- 上：小节标签（白底黑框） -->
    <div v-if="sectionLabel" class="page-shell-header__label">
      <SectionLabel :label="sectionLabel" :pulse="labelPulse" />
    </div>

    <!-- 中：图标 + 大标题（白字） -->
    <div class="page-shell-header__title-row">
      <h1 class="page-header__title page-title-gradient">
        <span v-if="$slots.icon || icon" class="page-title-gradient__icon" aria-hidden="true">
          <slot name="icon">{{ icon }}</slot>
        </span>
        <span class="page-title-gradient__text">{{ title }}</span>
      </h1>
    </div>

    <!-- 下：统计/说明条（#stats 插槽，白底标签） -->
    <div v-if="$slots.stats" class="page-shell-header__stats stats-bar">
      <slot name="stats" />
    </div>
  </header>
</template>

<script setup>
import SectionLabel from './SectionLabel.vue';

defineProps({
  /** 顶部 mono 标签文案，留空则不显示 */
  sectionLabel: { type: String, default: '' },
  /** 标签左侧标记是否脉冲（保留兼容） */
  labelPulse: { type: Boolean, default: false },
  /** 页面主标题（Playfair Display） */
  title: { type: String, required: true },
  /** 默认 emoji 图标；复杂图标请用 #icon 插槽 */
  icon: { type: String, default: '' },
});
</script>
