<template>
  <div class="auth-page">
    <AuthHeroCarousel />
    <section class="auth-panel">
      <div ref="panelRef" class="auth-panel__inner">
        <slot />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, provide } from 'vue';
import AuthHeroCarousel from './AuthHeroCarousel.vue';

const panelRef = ref(null);

/** 表单校验失败时轻微抖动（CSS，无 GSAP） */
const shakeForm = () => {
  const card = panelRef.value?.querySelector('.auth-card');
  if (!card) return;
  card.classList.add('auth-card--shake');
  setTimeout(() => card.classList.remove('auth-card--shake'), 400);
};

provide('authShakeForm', shakeForm);
</script>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--color-background);
}

.auth-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-10) clamp(var(--space-8), 6vw, 80px);
  border-left: 1px solid var(--color-border-light);
}

.auth-panel__inner {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 420px;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }

  .auth-panel {
    border-left: none;
    border-top: 1px solid var(--color-border-light);
    padding: var(--space-8) var(--space-6) var(--space-12);
  }
}
</style>

<style>
/* 全局：认证卡片抖动 */
.auth-card--shake {
  animation: auth-shake 0.4s ease;
}

@keyframes auth-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

@media (prefers-reduced-motion: reduce) {
  .auth-card--shake {
    animation: none;
  }
}
</style>
