<!-- src/App.vue -->
<template>
  <div id="app" @click="handleClick">
    <!-- 只有在需要显示导航栏的页面才渲染 -->
    <template v-if="showNavigation">
      <Header />
      <Navigation />
    </template>

    <main class="main-content" :class="{ 'no-navigation': !showNavigation }">
      <LoadingSpinner :loading="userStore.loading" />

      <router-view />
    </main>

    <!-- 只有在需要显示导航栏的页面才渲染 -->
    <template v-if="showNavigation">
      <Footer />
    </template>

    <!-- 彩虹星星点击特效容器 -->
    <div class="click-effects-container">
      <div
        v-for="effect in clickEffects"
        :key="effect.id"
        class="star-effect"
        :style="effect.style"
      >
        ⭐
      </div>
    </div>

    <!-- 返回顶部按钮 -->
    <button
      v-if="showNavigation && showBackToTop"
      class="back-to-top"
      @click="scrollToTop"
      title="返回顶部"
    >
      <span class="back-to-top-icon">↑</span>
    </button>
  </div>
</template>

<script setup>
import { onMounted, computed, ref, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import Header from './components/layout/Header.vue';
import Navigation from './components/layout/Navigation.vue';
import Footer from './components/layout/Footer.vue';
import LoadingSpinner from './components/common/LoadingSpinner.vue';
import { useUserStore } from './stores/userStore';

const userStore = useUserStore();
const route = useRoute();

// 计算属性：判断是否显示导航栏
const showNavigation = computed(() => {
  // 登录和注册页面不显示导航栏
  const guestRoutes = ['/login', '/register'];
  return !guestRoutes.includes(route.path);
});

// 点击特效状态
const clickEffects = ref([]);
let effectId = 0;

// 返回顶部按钮显示状态
const showBackToTop = ref(false);

// 监听滚动事件
const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300;
};

// 返回顶部
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
};

// 彩虹颜色数组
const rainbowColors = [
  '#ff0000', // 红
  '#ff7f00', // 橙
  '#ffff00', // 黄
  '#00ff00', // 绿
  '#0000ff', // 蓝
  '#4b0082', // 靛
  '#9400d3', // 紫
  '#ff1493', // 深粉
  '#00ffff', // 青
  '#ff69b4', // 热粉
];

// 处理点击事件
const handleClick = (event) => {
  // 获取点击位置
  const x = event.clientX;
  const y = event.clientY;

  // 创建多个星星
  for (let i = 0; i < 8; i++) {
    createStar(x, y, i);
  }
};

// 创建单个星星
const createStar = (x, y, index) => {
  const id = effectId++;
  const angle = (index / 8) * 360;
  const color = rainbowColors[Math.floor(Math.random() * rainbowColors.length)];
  const size = 16 + Math.random() * 16;
  const duration = 600 + Math.random() * 400;

  const effect = {
    id,
    style: {
      left: `${x}px`,
      top: `${y}px`,
      '--angle': `${angle}deg`,
      '--color': color,
      '--size': `${size}px`,
      '--duration': `${duration}ms`,
      fontSize: `${size}px`,
      color: color,
      animationDuration: `${duration}ms`,
    },
  };

  clickEffects.value.push(effect);

  // 动画结束后移除
  setTimeout(() => {
    const idx = clickEffects.value.findIndex((e) => e.id === id);
    if (idx > -1) {
      clickEffects.value.splice(idx, 1);
    }
  }, duration);
};

onMounted(() => {
  // 应用初始化逻辑
  userStore.initializeStore();

  // 添加滚动监听
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  // 移除滚动监听
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
}

html {
  scroll-behavior: smooth;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
  color: #1d1d1f;
  background: #ffffff;
  overflow-x: hidden;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
  background: #ffffff;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  padding-top: 104px;
}

.main-content.no-navigation {
  flex: 1;
  display: flex;
  flex-direction: row;
  width: 100%;
  padding-top: 0;
}

.main-content.no-navigation > div {
  flex: 1;
  display: flex;
  width: 100%;
}

::selection {
  background: rgba(0, 122, 255, 0.2);
  color: #1d1d1f;
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #d1d1d6;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a1a1a6;
}

a {
  color: #007aff;
  text-decoration: none;
  transition: color 0.3s ease;
}

a:hover {
  color: #0066d6;
}

button {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
}

input, textarea, select {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
}

/* 点击特效容器 */
.click-effects-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 99999999999999;
  overflow: hidden;
}

/* 星星特效 */
.star-effect {
  position: absolute;
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: starBurst var(--duration) ease-out forwards;
  filter: drop-shadow(0 0 8px var(--color)) drop-shadow(0 0 16px var(--color));
  will-change: transform, opacity;
}

@keyframes starBurst {
  0% {
    transform: translate(-50%, -50%) scale(0) rotate(0deg);
    opacity: 1;
  }
  20% {
    transform: translate(-50%, -50%) scale(1.5) rotate(180deg);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) 
      translateX(calc(cos(var(--angle)) * 80px))
      translateY(calc(sin(var(--angle)) * 80px))
      scale(0.3) rotate(360deg);
    opacity: 0;
  }
}

/* 返回顶部按钮 */
.back-to-top {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 32px rgba(0, 122, 255, 0.4);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  z-index: 9999;
  animation: fadeInUp 0.3s ease;
}

.back-to-top:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 12px 40px rgba(0, 122, 255, 0.5);
}

.back-to-top:active {
  transform: translateY(-2px) scale(0.98);
}

.back-to-top-icon {
  font-size: 24px;
  color: white;
  font-weight: 700;
  line-height: 1;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .star-effect {
    font-size: 14px !important;
  }

  .back-to-top {
    width: 48px;
    height: 48px;
    bottom: 20px;
    right: 20px;
  }

  .back-to-top-icon {
    font-size: 20px;
  }

  .main-content {
    padding-top: 96px;
  }
}
</style>
