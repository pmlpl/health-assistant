<script setup>
defineProps({
  title: { type: String, default: '' },
  /** 是否可 hover 抬升 */
  hoverable: { type: Boolean, default: true },
  padding: { type: String, default: 'lg' },
});
</script>

<template>
  <section
    class="app-card"
    :class="[
      hoverable && 'app-card--hoverable',
      `app-card--pad-${padding}`,
    ]"
  >
    <header v-if="title || $slots.header" class="app-card__header">
      <slot name="header">
        <h2 class="app-card__title">{{ title }}</h2>
      </slot>
    </header>
    <div class="app-card__content">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.app-card {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-normal), transform var(--transition-normal);
}

.app-card--hoverable:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.app-card--pad-sm { padding: var(--space-4); }
.app-card--pad-md { padding: var(--space-5); }
.app-card--pad-lg { padding: var(--space-6); }

.app-card__header {
  margin-bottom: var(--space-4);
}

.app-card__title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.app-card__title::before {
  content: '';
  width: 3px;
  height: 1.25em;
  background: var(--color-primary);
  border-radius: 2px;
}

.app-card__content {
  color: var(--color-text-secondary);
}
</style>
