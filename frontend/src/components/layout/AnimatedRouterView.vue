<script setup>
/**
 * 路由视图包裹层 — 无 GSAP，即时切换
 */
import { useRoute } from 'vue-router';

const route = useRoute();
const guestPaths = ['/login', '/register'];

const isGuestRoute = (path) => guestPaths.includes(path);
</script>

<template>
  <router-view v-slot="{ Component, route: activeRoute }">
    <div
      class="route-page"
      :class="{ 'route-page--guest': isGuestRoute(activeRoute.path) }"
    >
      <component :is="Component" :key="activeRoute.fullPath" />
    </div>
  </router-view>
</template>

<style scoped>
.route-page {
  width: 100%;
  min-height: 0;
}

.route-page--guest {
  max-width: none;
  margin-inline: 0;
}

@media (min-width: 1024px) {
  .route-page:not(.route-page--guest) {
    max-width: var(--content-area-max-width);
    margin-inline: auto;
  }
}
</style>
