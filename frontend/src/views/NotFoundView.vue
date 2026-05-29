<template>
  <div class="not-found">
    <div class="not-found__panel bh-card">
      <SectionLabel label="404" />
      <h1 class="not-found__code">404</h1>
      <p class="not-found__text">页面不存在或已被移除</p>
      <router-link to="/" class="btn-brand">返回首页 →</router-link>
    </div>
  </div>
</template>

<script setup>
import SectionLabel from '../components/common/SectionLabel.vue';
</script>

<style scoped>
/* Bauhaus 404 页：居中卡片 + 硬阴影 */
.not-found {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  background: var(--color-background);
}

.not-found__panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-12) var(--space-10);
  text-align: center;
  max-width: 420px;
  width: 100%;
}

.not-found__code {
  font-family: var(--font-display);
  font-size: var(--font-size-8xl);
  margin: 0;
  color: var(--color-primary-red);
  line-height: 1;
  font-weight: var(--font-weight-black);
}

.not-found__text {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--color-muted-foreground);
}
</style>
