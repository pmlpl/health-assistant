<script setup>
import { ref } from 'vue';
import { gsap } from '../../plugins/gsap';
import { motionDuration, prefersReducedMotion } from '../../utils/motion';

defineProps({
  activeRoute: { type: String, default: '' },
});

const logoRef = ref(null);

const onLogoHover = () => {
  if (prefersReducedMotion() || !logoRef.value) return;
  gsap.to(logoRef.value, { rotation: 15, duration: motionDuration(0.5), ease: 'power2.out' });
};

const onLogoLeave = () => {
  if (!logoRef.value) return;
  gsap.to(logoRef.value, { rotation: 0, duration: motionDuration(0.4), ease: 'power2.out' });
};

const navItems = [
  { to: '/', name: 'Dashboard', label: '首页', icon: '🏠' },
  { to: '/diary', name: 'Diary', label: '记录', icon: '📝' },
  { to: '/health-report', name: 'HealthReport', label: '洞察', icon: '📊' },
  { to: '/recipes', name: 'Recipes', label: '食谱', icon: '🥗' },
  { to: '/profile', name: 'Profile', label: '我的', icon: '👤' },
];

/** 次级入口：仍保留独立路由，从侧栏访问 */
const secondaryItems = [
  { to: '/fitness', name: 'Fitness', label: '健身记录' },
  { to: '/calendar', name: 'Calendar', label: '健康日历' },
  { to: '/guide', name: 'UserGuide', label: '使用手册' },
];
</script>

<template>
  <aside class="sidebar" aria-label="侧边导航">
    <div class="sidebar__brand" @mouseenter="onLogoHover" @mouseleave="onLogoLeave">
      <span ref="logoRef" class="sidebar__logo">🌿</span>
      <span class="sidebar__title">健康助手</span>
    </div>

    <nav class="sidebar__primary">
      <router-link
        v-for="item in navItems"
        :key="item.name"
        :to="item.to"
        class="sidebar__link"
        :class="{ 'sidebar__link--active': activeRoute === item.name }"
      >
        <span class="sidebar__link-icon">{{ item.icon }}</span>
        {{ item.label }}
      </router-link>
    </nav>

    <div class="sidebar__section-label">更多</div>
    <nav class="sidebar__secondary">
      <router-link
        v-for="item in secondaryItems"
        :key="item.name"
        :to="item.to"
        class="sidebar__link sidebar__link--sub"
        :class="{ 'sidebar__link--active': activeRoute === item.name }"
      >
        {{ item.label }}
      </router-link>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  position: fixed;
  top: var(--header-height);
  left: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--color-bg-elevated);
  border-right: 1px solid var(--color-border);
  padding: var(--space-6) var(--space-4);
  display: none;
  flex-direction: column;
  z-index: 900;
  overflow-y: auto;
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-3) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--space-4);
}

.sidebar__logo {
  font-size: 1.5rem;
  display: inline-block;
}

.sidebar__title {
  font-weight: 700;
  font-size: var(--font-size-lg);
  color: var(--color-text);
}

.sidebar__primary,
.sidebar__secondary {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.sidebar__section-label {
  margin: var(--space-4) var(--space-3) var(--space-2);
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.sidebar__link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: var(--color-text-secondary);
  font-weight: 500;
  font-size: var(--font-size-sm);
  transition: color var(--transition-fast);
  position: relative;
  overflow: hidden;
  z-index: 0;
}

.sidebar__link::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--color-foreground), transparent);
  transform: translateX(-100%);
  transition: transform 0.35s var(--ease-smooth-out);
  z-index: -1;
}

.sidebar__link:hover::before {
  transform: translateX(0);
}

.sidebar__link--sub {
  padding-left: var(--space-6);
  font-size: var(--font-size-sm);
}

.sidebar__link:hover {
  color: var(--color-text);
}

.sidebar__link--active {
  background: var(--color-primary-muted);
  color: var(--color-primary);
  font-weight: 600;
}

.sidebar__link-icon {
  font-size: 1.1rem;
  width: 1.5rem;
  text-align: center;
}

@media (min-width: 1024px) {
  .sidebar {
    display: flex;
  }
}
</style>
