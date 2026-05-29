<script setup>
import { ref, watch, onMounted, nextTick } from 'vue';

const props = defineProps({
  activeRoute: { type: String, default: '' },
});

const navRef = ref(null);

const navItems = [
  { to: '/', name: 'Dashboard', label: '首页', icon: '🏠' },
  { to: '/diary', name: 'Diary', label: '记录', icon: '📝' },
  { to: '/health-report', name: 'HealthReport', label: '洞察', icon: '📊' },
  { to: '/recipes', name: 'Recipes', label: '食谱', icon: '🥗' },
  { to: '/profile', name: 'Profile', label: '我的', icon: '👤' },
];

const indicatorStyle = ref({ opacity: 0 });

/** 更新底部指示线位置 */
const updateIndicator = async () => {
  await nextTick();
  const activeEl = navRef.value?.querySelector('.bottom-nav__item--active');
  if (!activeEl) return;
  indicatorStyle.value = {
    opacity: 1,
    transform: `translateX(${activeEl.offsetLeft}px)`,
    width: `${activeEl.offsetWidth}px`,
  };
};

watch(() => props.activeRoute, updateIndicator);
onMounted(updateIndicator);
</script>

<template>
  <nav ref="navRef" class="bottom-nav" aria-label="主导航">
    <div class="bottom-nav__indicator" :style="indicatorStyle" aria-hidden="true" />
    <router-link
      v-for="item in navItems"
      :key="item.name"
      :to="item.to"
      class="bottom-nav__item"
      :class="{ 'bottom-nav__item--active': activeRoute === item.name }"
    >
      <span class="bottom-nav__icon" aria-hidden="true">{{ item.icon }}</span>
      <span class="bottom-nav__label">{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--bottom-nav-height);
  display: flex;
  align-items: stretch;
  justify-content: space-around;
  background: var(--color-card);
  border-top: var(--border-width) solid var(--color-border);
  box-shadow: 0 -4px 0 0 var(--color-border);
  z-index: 1000;
  padding-bottom: env(safe-area-inset-bottom, 0);
}

.bottom-nav__indicator {
  position: absolute;
  top: 0;
  left: 0;
  height: 3px;
  background: var(--color-primary-red);
  transition:
    transform 100ms ease,
    width 100ms ease,
    opacity 100ms ease;
  pointer-events: none;
}

.bottom-nav__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  text-decoration: none;
  color: var(--color-muted-foreground);
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  letter-spacing: 0.05em;
  transition: color var(--transition-fast), background var(--transition-fast);
  position: relative;
  z-index: 1;
}

.bottom-nav__item:hover {
  color: var(--color-foreground);
  background: var(--color-muted);
}

.bottom-nav__item--active {
  color: var(--color-foreground);
  font-weight: var(--font-weight-semibold);
}

.bottom-nav__icon {
  font-size: 1.25rem;
  line-height: 1;
}

@media (min-width: 1024px) {
  .bottom-nav {
    display: none;
  }
}
</style>
