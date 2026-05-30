<template>
  <div class="auth-hero bauhaus-block bauhaus-block--blue">
    <div class="auth-hero__inner">
      <SectionLabel label="AI 健康助手" />
      <div class="auth-hero__rule" aria-hidden="true" />
      <Transition name="auth-slide" mode="out-in">
        <article :key="activeIndex" class="auth-hero__slide">
          <AuthSlideIcon :name="slides[activeIndex].icon" class="auth-hero__icon" />
          <h2 class="auth-hero__title bh-heading-on-color">{{ slides[activeIndex].title }}</h2>
          <p class="auth-hero__desc">{{ slides[activeIndex].desc }}</p>
        </article>
      </Transition>
      <div class="auth-hero__dots" aria-label="功能轮播">
        <button
          v-for="(_, index) in slides"
          :key="index"
          type="button"
          class="auth-hero__dot"
          :class="{ 'auth-hero__dot--active': activeIndex === index }"
          :aria-label="`第 ${index + 1} 项`"
          @click="activeIndex = index"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import SectionLabel from '../common/SectionLabel.vue';
import AuthSlideIcon from './icons/AuthSlideIcon.vue';
import { AUTH_SLIDES } from '../../constants/authSlides';

const slides = AUTH_SLIDES;
const activeIndex = ref(0);
const AUTO_PLAY_MS = 4000;
let autoTimer = null;

/** 自动轮播（纯 CSS 淡入淡出，无 GSAP） */
onMounted(() => {
  autoTimer = window.setInterval(() => {
    activeIndex.value = (activeIndex.value + 1) % slides.length;
  }, AUTO_PLAY_MS);
});

onUnmounted(() => {
  if (autoTimer) clearInterval(autoTimer);
});
</script>

<style scoped>
.auth-hero {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) clamp(var(--space-8), 6vw, 80px);
  min-height: 100%;
  border-right: 1px solid var(--color-border-light);
}

.auth-hero__inner {
  max-width: 420px;
  width: 100%;
}

.auth-hero__rule {
  width: 48px;
  height: 4px;
  background: #fff;
  margin: var(--space-4) 0 var(--space-8);
}

.auth-hero__slide {
  min-height: 200px;
}

.auth-hero__icon {
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-6);
  color: #fff;
}

.auth-hero__title {
  font-family: var(--font-display);
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-black);
  margin: 0 0 var(--space-4);
  line-height: 1.2;
  text-transform: uppercase;
}

.auth-hero__desc {
  font-family: var(--font-body);
  font-size: var(--font-size-lg);
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
  line-height: 1.6;
}

.auth-hero__dots {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-10);
}

.auth-hero__dot {
  width: 8px;
  height: 8px;
  padding: 0;
  border: 2px solid #fff;
  background: transparent;
  border-radius: 0;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.auth-hero__dot--active {
  width: 24px;
  background: #fff;
}

.auth-slide-enter-active,
.auth-slide-leave-active {
  transition: opacity 0.2s ease;
}

.auth-slide-enter-from,
.auth-slide-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .auth-hero {
    min-height: auto;
    padding: var(--space-10) var(--space-6);
    border-right: none;
    border-bottom: 1px solid var(--color-border-light);
  }

  .auth-hero__slide {
    min-height: 140px;
  }
}
</style>
