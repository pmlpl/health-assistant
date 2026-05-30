<template>
  <footer class="page-cta inverted-section">
    <hr class="rule-thick page-cta__rule" aria-hidden="true" />
    <div class="page-cta__inner">
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
      <router-link v-if="to" :to="to" class="btn-brand">{{ buttonText }}</router-link>
      <slot />
    </div>
  </footer>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  to: { type: String, default: '' },
  buttonText: { type: String, default: '立即前往' },
});
</script>

<style scoped>
/* 反色区块顶部的粗分割线 */
.page-cta__rule {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: var(--color-inverted-fg);
}
</style>
