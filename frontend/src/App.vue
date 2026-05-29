<!-- src/App.vue — Bauhaus 应用壳层 -->
<template>
  <div id="app">
    <template v-if="showNavigation">
      <Header />
      <TopNav :active-route="route.name" />
      <BottomNav :active-route="route.name" />
    </template>

    <div
      v-if="showNavigation && (aiPageJobs.anyRunning || aiConsult.streaming)"
      class="ai-global-task-bar"
      role="status"
    >
      <span v-if="aiPageJobs.anyRunning">智能分析中...</span>
      <span v-else-if="aiConsult.streaming">AI 精灵正在回复…</span>
    </div>

    <main class="main-content" :class="{ 'main-content--guest': !showNavigation }">
      <LoadingSpinner :loading="userStore.loading" />
      <AnimatedRouterView />
    </main>

    <button
      v-if="showNavigation && showBackToTop"
      class="back-to-top"
      type="button"
      title="回到顶部"
      aria-label="回到顶部"
      @click="scrollToTop"
    >
      <span class="back-to-top__icon" aria-hidden="true">↑</span>
    </button>

    <AISpirit v-if="showNavigation" />
  </div>
</template>

<script setup>
import { onMounted, computed, ref, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import Header from './components/layout/Header.vue';
import TopNav from './components/layout/TopNav.vue';
import BottomNav from './components/layout/BottomNav.vue';
import LoadingSpinner from './components/common/LoadingSpinner.vue';
import AnimatedRouterView from './components/layout/AnimatedRouterView.vue';
import AISpirit from './components/AISpirit.vue';
import { useUserStore } from './stores/userStore';
import { useAiPageJobsStore } from './stores/aiPageJobsStore';
import { useAiConsultStore } from './stores/aiConsultStore';

const userStore = useUserStore();
const aiPageJobs = useAiPageJobsStore();
const aiConsult = useAiConsultStore();
const route = useRoute();

const showNavigation = computed(() => {
  const guestRoutes = ['/login', '/register'];
  return !guestRoutes.includes(route.path);
});

const showBackToTop = ref(false);

/** 滚动超过阈值时显示回到顶部 */
const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300;
};

/** 平滑回顶（原生 API，无 GSAP） */
const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

onMounted(() => {
  userStore.initializeStore();
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

body {
  font-family: var(--font-body);
  color: var(--color-text);
  background: var(--color-bg);
  overflow-x: hidden;
  -webkit-font-smoothing: antialiased;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.main-content {
  flex: 1;
  width: 100%;
  padding-top: var(--header-height);
  padding-bottom: var(--bottom-nav-height);
}

.main-content--guest {
  padding-top: 0;
  padding-bottom: 0;
}

@media (min-width: 1024px) {
  .main-content:not(.main-content--guest) {
    width: 100%;
    margin-left: 0;
    padding-top: var(--shell-top-offset);
    padding-inline: var(--space-6);
    padding-bottom: var(--space-8);
    box-sizing: border-box;
  }
}

.main-content--guest {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
}

.main-content--guest > * {
  flex: 1;
  width: 100%;
  min-height: 0;
}

::selection {
  background: var(--color-muted);
  color: var(--color-foreground);
}

a {
  color: var(--color-foreground);
  text-decoration: none;
  transition: color var(--transition-fast);
}

a:hover {
  color: var(--color-muted-foreground);
}

/* 回到顶部：Bauhaus 红块 + 硬阴影 */
.back-to-top {
  position: fixed;
  bottom: calc(var(--bottom-nav-height) + var(--space-4));
  right: var(--space-4);
  width: 48px;
  height: 48px;
  padding: 0;
  border-radius: 0;
  background: var(--color-primary-red);
  color: #fff;
  border: var(--border-width) solid var(--color-border);
  cursor: pointer;
  font-size: 1.1rem;
  font-weight: var(--font-weight-bold);
  box-shadow: var(--shadow-md);
  z-index: 999;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-to-top:hover {
  transform: translateY(-2px);
}

.back-to-top:active {
  transform: translate(2px, 2px);
  box-shadow: none;
}

.back-to-top__icon {
  line-height: 1;
}

@media (min-width: 1024px) {
  .back-to-top {
    bottom: var(--space-8);
  }
}

.ai-global-task-bar {
  position: fixed;
  top: calc(var(--header-height) + var(--space-2));
  left: 50%;
  transform: translateX(-50%);
  z-index: 9998;
  padding: var(--space-2) var(--space-5);
  background: var(--color-primary-yellow);
  color: var(--color-foreground);
  font-family: var(--font-body);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  border-radius: 0;
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-md);
  pointer-events: none;
}

@media (min-width: 1024px) {
  .ai-global-task-bar {
    top: calc(var(--shell-top-offset) + var(--space-2));
  }
}
</style>
