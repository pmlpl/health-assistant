<!-- src/components/layout/Navigation.vue — Bauhaus 主导航（备用） -->
<template>
  <nav class="navigation" aria-label="主导航">
    <router-link
      v-for="item in navItems"
      :key="item.name"
      :to="item.to"
      class="nav-button"
      :class="{ active: $route.name === item.name }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <path :d="item.iconPath" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      {{ item.label }}
    </router-link>
  </nav>
</template>

<script setup>
/** 导航项配置：Bauhaus 硬边导航 */
const navItems = [
  { to: '/', name: 'Dashboard', label: '首页', iconPath: 'M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z M9 22V12H15V22' },
  { to: '/recipes', name: 'Recipes', label: '健康食谱', iconPath: 'M6 9C6 9 6 5 9 5C12 5 12 7 12 9C12 9 12 5 15 5C18 5 18 9 18 9V11H6V9Z M4 11H20V19C20 19.5304 19.7893 20.0391 19.4142 20.4142C19.0391 20.7893 18.5304 21 18 21H6C5.46957 21 4.96086 20.7893 4.58579 20.4142C4.21071 20.0391 4 19.5304 4 19V11Z' },
  { to: '/diary', name: 'Diary', label: '饮食日记', iconPath: 'M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V20C2 20.5304 2.21071 21.0391 2.58579 21.4142C2.96086 21.7893 3.46957 22 4 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V13 M18.5 2.5C18.8978 2.1024 19.4374 1.87891 20 1.87891C20.5626 1.87891 21.1022 2.1024 21.4999 2.5C21.8978 2.89789 22.1213 3.43762 22.1213 4C22.1213 4.56284 21.8978 5.10257 21.4999 5.5L12 15L8 16L9 12L18.5 2.5Z' },
  { to: '/health-report', name: 'HealthReport', label: '健康报告', iconPath: 'M4 19H20 M6 16L9 11L13 14L17 8L20 12 M3 4H21V20H3V4Z' },
  { to: '/profile', name: 'Profile', label: '个人中心', iconPath: 'M12 8C14.2091 8 16 6.20914 16 4C16 1.79086 14.2091 0 12 0C9.79086 0 8 1.79086 8 4C8 6.20914 9.79086 8 12 8ZM20 21C20 16.5817 16.4183 13 12 13C7.58172 13 4 16.5817 4 21' },
  { to: '/fitness', name: 'Fitness', label: '健身记录', iconPath: 'M6.5 6.5L17.5 17.5 M21 21L15 15 M3 3L9 9 M9 9H15V15H9V9Z' },
  { to: '/calendar', name: 'Calendar', label: '健康日历', iconPath: 'M3 4H21V22H3V4Z M16 2V6 M8 2V6 M3 10H21' },
];
</script>

<style scoped>
.navigation {
  background: var(--color-card);
  padding: 0 var(--space-8);
  border-bottom: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-sm);
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  width: 100%;
  position: fixed;
  top: var(--header-height);
  left: 0;
  z-index: 999;
}

.nav-button {
  padding: var(--space-4) var(--space-5);
  border: none;
  background: none;
  cursor: pointer;
  font-family: var(--font-body);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: color var(--transition-fast), background var(--transition-fast);
  margin: 0;
  text-decoration: none;
  color: var(--color-muted-foreground);
  border-radius: 0;
  position: relative;
  letter-spacing: 0.02em;
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.nav-button::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 3px;
  background: var(--color-primary-red);
  transition: width var(--transition-fast);
}

.nav-button:hover {
  color: var(--color-foreground);
  background: var(--color-muted);
}

.nav-button:hover::after,
.nav-button.active::after {
  width: 100%;
}

.nav-button.active {
  color: var(--color-foreground);
  font-weight: var(--font-weight-bold);
}

@media (max-width: 768px) {
  .navigation {
    padding: 0 var(--space-5);
    overflow-x: auto;
    justify-content: flex-start;
    -webkit-overflow-scrolling: touch;
  }

  .nav-button {
    white-space: nowrap;
    padding: var(--space-3) var(--space-4);
    font-size: var(--font-size-xs);
  }
}

.navigation::-webkit-scrollbar {
  height: 0;
}
</style>
