<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue';

const props = defineProps({
  activeRoute: { type: String, default: '' },
});

const navRef = ref(null);

/** 主导航入口 */
const primaryItems = [
  { to: '/', name: 'Dashboard', label: '首页', icon: '🏠' },
  { to: '/diary', name: 'Diary', label: '记录', icon: '📝' },
  { to: '/health-report', name: 'HealthReport', label: '洞察', icon: '📊' },
  { to: '/recipes', name: 'Recipes', label: '食谱', icon: '🥗' },
  { to: '/profile', name: 'Profile', label: '我的', icon: '👤' },
];

/** 次级入口 */
const secondaryItems = [
  { to: '/fitness', name: 'Fitness', label: '健身记录' },
  { to: '/calendar', name: 'Calendar', label: '健康日历' },
  { to: '/guide', name: 'UserGuide', label: '使用手册' },
];

const indicatorStyle = ref({ opacity: 0 });

/** 更新底部线条指示器位置 */
const updateIndicator = async () => {
  await nextTick();
  const inner = navRef.value?.querySelector('.top-nav__inner');
  const activeEl = navRef.value?.querySelector('.top-nav__link--active');
  if (!inner || !activeEl) {
    indicatorStyle.value = { opacity: 0 };
    return;
  }
  const innerRect = inner.getBoundingClientRect();
  const activeRect = activeEl.getBoundingClientRect();
  indicatorStyle.value = {
    opacity: 1,
    transform: `translateX(${activeRect.left - innerRect.left + inner.scrollLeft}px)`,
    width: `${activeRect.width}px`,
  };
};

watch(() => props.activeRoute, updateIndicator);
onMounted(() => {
  updateIndicator();
  window.addEventListener('resize', updateIndicator);
});
onUnmounted(() => {
  window.removeEventListener('resize', updateIndicator);
});
</script>

<template>
  <nav ref="navRef" class="top-nav" aria-label="顶部导航">
    <div class="top-nav__inner">
      <div class="top-nav__indicator" :style="indicatorStyle" aria-hidden="true" />

      <div class="top-nav__group top-nav__group--primary">
        <router-link
          v-for="item in primaryItems"
          :key="item.name"
          :to="item.to"
          class="top-nav__link"
          :class="{ 'top-nav__link--active': activeRoute === item.name }"
        >
          <span class="top-nav__icon" aria-hidden="true">{{ item.icon }}</span>
          {{ item.label }}
        </router-link>
      </div>

      <span class="top-nav__divider" aria-hidden="true" />

      <div class="top-nav__group top-nav__group--secondary">
        <router-link
          v-for="item in secondaryItems"
          :key="item.name"
          :to="item.to"
          class="top-nav__link top-nav__link--sub"
          :class="{ 'top-nav__link--active': activeRoute === item.name }"
        >
          {{ item.label }}
        </router-link>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.top-nav {
  position: fixed;
  top: var(--header-height);
  left: 0;
  right: 0;
  height: var(--top-nav-height);
  z-index: 1000;
  display: none;
  background: var(--color-card);
  border-bottom: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.top-nav__inner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  height: 100%;
  max-width: var(--content-area-max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.top-nav__indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  background: var(--color-primary-red);
  transition:
    transform 100ms ease,
    width 100ms ease,
    opacity 100ms ease;
  pointer-events: none;
}

.top-nav__group {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.top-nav__divider {
  width: 1px;
  height: 20px;
  background: var(--color-border-light);
  margin: 0 var(--space-2);
  flex-shrink: 0;
}

.top-nav__link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: var(--space-2) var(--space-4);
  text-decoration: none;
  color: var(--color-muted-foreground);
  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
  border: var(--border-width) solid transparent;
  transition: color var(--transition-fast), background var(--transition-fast);
}

.top-nav__link--sub {
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-xs);
  font-family: var(--font-mono);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.top-nav__link:hover {
  color: var(--color-foreground);
  background: var(--color-muted);
}

.top-nav__link--active {
  color: var(--color-foreground);
  font-weight: var(--font-weight-semibold);
  border-color: var(--color-border);
}

.top-nav__icon {
  font-size: 1rem;
  line-height: 1;
}

@media (min-width: 1024px) {
  .top-nav {
    display: block;
  }
}

@media (max-width: 1200px) {
  .top-nav__inner {
    justify-content: flex-start;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .top-nav__inner::-webkit-scrollbar {
    display: none;
  }
}
</style>
