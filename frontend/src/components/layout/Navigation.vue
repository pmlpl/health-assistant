<!-- src/components/layout/Navigation.vue -->
<template>
  <nav class="navigation">
    <router-link
        to="/"
        class="nav-button bilibili-nav-button"
        :class="{ active: $route.name === 'Dashboard' }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M9 22V12H15V22" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      首页
    </router-link>
    <router-link
        to="/recipes"
        class="nav-button bilibili-nav-button"
        :class="{ active: $route.name === 'Recipes' }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M6 9C6 9 6 5 9 5C12 5 12 7 12 9C12 9 12 5 15 5C18 5 18 9 18 9V11H6V9Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M4 11H20V19C20 19.5304 19.7893 20.0391 19.4142 20.4142C19.0391 20.7893 18.5304 21 18 21H6C5.46957 21 4.96086 20.7893 4.58579 20.4142C4.21071 20.0391 4 19.5304 4 19V11Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <line x1="9" y1="14" x2="15" y2="14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <line x1="9" y1="17" x2="15" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      健康食谱
    </router-link>
    <router-link
        to="/diary"
        class="nav-button bilibili-nav-button"
        :class="{ active: $route.name === 'Diary' }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M11 4H4C3.46957 4 2.96086 4.21071 2.58579 4.58579C2.21071 4.96086 2 5.46957 2 6V20C2 20.5304 2.21071 21.0391 2.58579 21.4142C2.96086 21.7893 3.46957 22 4 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M18.5 2.50023C18.8978 2.1024 19.4374 1.87891 20 1.87891C20.5626 1.87891 21.1022 2.1024 21.4999 2.50023C21.8978 2.89789 22.1213 3.43762 22.1213 4.00023C22.1213 4.56284 21.8978 5.10257 21.4999 5.50023L12 15.0002L8 16.0002L9 12.0002L18.5 2.50023Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      饮食日记
    </router-link>
    <router-link
        to="/profile"
        class="nav-button bilibili-nav-button"
        :class="{ active: $route.name === 'Profile' }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="2"/>
        <path d="M20 21C20 16.5817 16.4183 13 12 13C7.58172 13 4 16.5817 4 21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      个人中心
    </router-link>
    <router-link
        to="/ai-consult"
        class="nav-button bilibili-nav-button"
        :class="{ active: $route.name === 'AIConsult' }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM12 6C13.66 6 15 7.34 15 9C15 10.66 13.66 12 12 12C10.34 12 9 10.66 9 9C9 7.34 10.34 6 12 6ZM12 18C9.33 18 7 16.33 6 14H18C17 16.33 14.67 18 12 18Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      AI 咨询
    </router-link>
    <router-link
        to="/fitness"
        class="nav-button bilibili-nav-button"
        :class="{ active: $route.name === 'Fitness' }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M6.5 6.5L17.5 17.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M21 21L15 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M3 3L9 9" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <rect x="9" y="9" width="6" height="6" rx="1" stroke="currentColor" stroke-width="2"/>
        <path d="M3 9V15C3 15.5304 3.21071 16.0391 3.58579 16.4142C3.96086 16.7893 4.46957 17 5 17H9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M15 9H19C19.5304 9 20.0391 9.21071 20.4142 9.58579C20.7893 9.96086 21 10.4696 21 11V15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      健身记录
    </router-link>
    <router-link
        to="/calendar"
        class="nav-button bilibili-nav-button"
        :class="{ active: $route.name === 'Calendar' }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="3" y="4" width="18" height="18" rx="2" ry="2" stroke="currentColor" stroke-width="2"/>
        <line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" stroke-width="2"/>
        <line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" stroke-width="2"/>
        <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/>
      </svg>
      健康日历
    </router-link>
    <router-link
        to="/mental-health"
        class="nav-button bilibili-nav-button"
        :class="{ active: $route.name === 'MentalHealth' }"
    >
      <svg class="nav-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 21.35L10.55 20.03C5.4 15.36 2 12.27 2 8.5C2 5.41 4.42 3 7.5 3C9.24 3 10.91 3.81 12 5.08C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.41 22 8.5C22 12.27 18.6 15.36 13.45 20.03L12 21.35Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      心理陪伴
    </router-link>
  </nav>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
}

.navigation {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  padding: 0 48px;
  box-shadow: 
    0 1px 0 rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  width: 100%;
  position: fixed;
  top: 52px;
  left: 0;
  z-index: 999;
}

.nav-button {
  padding: 16px 20px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  margin: 0;
  text-decoration: none;
  color: #1d1d1f;
  border-radius: 0;
  position: relative;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  gap: 8px;
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
  left: 50%;
  width: 0;
  height: 2px;
  background: #007aff;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  transform: translateX(-50%);
}

.nav-button:hover {
  color: #007aff;
}

.nav-button:hover::after {
  width: 100%;
}

.nav-button.active {
  color: #007aff;
}

.nav-button.active::after {
  width: 100%;
}

@media (max-width: 768px) {
  .navigation {
    padding: 0 20px;
    overflow-x: auto;
    justify-content: flex-start;
    -webkit-overflow-scrolling: touch;
    top: 48px;
  }

  .nav-button {
    white-space: nowrap;
    padding: 14px 16px;
    font-size: 13px;
  }
}

.navigation::-webkit-scrollbar {
  height: 0;
}

.navigation::-webkit-scrollbar-track {
  background: transparent;
}

.navigation::-webkit-scrollbar-thumb {
  background: transparent;
}
</style>