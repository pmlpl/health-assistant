<!-- src/components/layout/Header.vue -->
<template>
  <header class="header">
    <div class="logo-section">
      <img 
        src="@/assets/images/logo.svg" 
        alt="AI健康助手 Logo" 
        class="logo"
        @error="handleLogoError"
      />
      <h1 class="app-title">AI健康助手</h1>
    </div>
    <div class="user-actions">
      <!-- API 状态下拉菜单 -->
      <div class="api-status-dropdown">
        <button 
          @click="apiStatusOpen = !apiStatusOpen" 
          class="api-status-btn"
          :class="{ 'active': apiStatusOpen }"
        >
          <span class="api-icon">🔌</span>
          API 状态
        </button>
        <div v-if="apiStatusOpen" class="api-status-dropdown-content">
          <ApiStatusIndicator />
        </div>
      </div>
      <div class="user-info" v-if="userStore.isLoggedIn">
        <span class="username">{{ userStore.userData.userId }}</span>
        <span class="health-goal" v-if="userStore.userData.goal">| {{ userStore.userData.goal }}</span>
        <button 
          @click="handleLogout" 
          class="logout-btn"
          :disabled="isLoggingOut"
          :class="{ 'logging-out': isLoggingOut }"
        >
          <span v-if="isLoggingOut" class="loading-spinner"></span>
          {{ isLoggingOut ? '退出中...' : '🚪 退出' }}
        </button>
      </div>
      <div v-else class="login-prompt">
        <router-link to="/login" class="login-link">请登录</router-link>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useUserStore } from '../../stores/userStore';
import { useRouter } from 'vue-router';
import { ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import ApiStatusIndicator from '../common/ApiStatusIndicator.vue';

const userStore = useUserStore();
const router = useRouter();
const isLoggingOut = ref(false);
const apiStatusOpen = ref(false);

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '注销确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    );
    
    try {
      isLoggingOut.value = true;
      await userStore.logout();
      showMessage('退出登录成功', 'success');
      router.push('/login');
    } catch (error) {
      console.error('注销失败:', error);
      showMessage('退出登录失败，请重试', 'error');
    } finally {
      isLoggingOut.value = false;
    }
  } catch (err) {
    // 用户取消操作
    return;
  }
};

// 显示消息提示
const showMessage = (message, type) => {
  const messageEl = document.createElement('div');
  messageEl.className = `message-popup ${type}`;
  messageEl.textContent = message;
  
  Object.assign(messageEl.style, {
    position: 'fixed',
    top: '20px',
    right: '20px',
    padding: '15px 25px',
    borderRadius: '8px',
    color: 'white',
    fontWeight: '500',
    zIndex: '10000',
    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
    transform: 'translateX(100%)',
    transition: 'transform 0.3s ease'
  });
  
  if (type === 'success') {
    messageEl.style.backgroundColor = '#4CAF50';
  } else {
    messageEl.style.backgroundColor = '#f44336';
  }
  
  document.body.appendChild(messageEl);
  
  setTimeout(() => {
    messageEl.style.transform = 'translateX(0)';
  }, 100);
  
  setTimeout(() => {
    messageEl.style.transform = 'translateX(100%)';
    setTimeout(() => {
      document.body.removeChild(messageEl);
    }, 300);
  }, 3000);
};

const handleLogoError = (event) => {
  // 如果SVG加载失败，显示备用文本
  event.target.style.display = 'none';
  const logoSection = event.target.parentElement;
  const fallback = document.createElement('div');
  fallback.className = 'logo-fallback';
  fallback.innerHTML = '🏥';
  logoSection.appendChild(fallback);
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
}

.header {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  color: #1d1d1f;
  padding: 0 48px;
  height: 52px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.08);
  width: 100%;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 32px;
  height: 32px;
  transition: transform 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.logo:hover {
  transform: scale(1.05);
}

.app-title {
  margin: 0;
  font-size: 21px;
  font-weight: 600;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #1d1d1f 0%, #007aff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f5f5f7;
  padding: 8px 16px;
  border-radius: 980px;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  letter-spacing: -0.2px;
}

.health-goal {
  font-size: 13px;
  color: #86868b;
  font-weight: 500;
}

.logout-btn {
  background: #007aff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 980px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: -0.1px;
}

.logout-btn:hover {
  background: #0066d6;
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.logout-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.logout-btn.logging-out {
  background: #007aff;
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  display: inline-block;
  margin-right: 6px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.login-prompt .login-link {
  color: white;
  text-decoration: none;
  background: #007aff;
  padding: 8px 20px;
  border-radius: 980px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  letter-spacing: -0.1px;
}

.login-prompt .login-link:hover {
  background: #0066d6;
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.api-status-dropdown {
  position: relative;
  z-index: 10001;
}

.api-status-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f5f5f7;
  color: #1d1d1f;
  border: none;
  padding: 8px 16px;
  border-radius: 980px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  letter-spacing: -0.1px;
}

.api-status-btn:hover {
  background: #ebebf0;
}

.api-status-btn.active {
  background: #007aff;
  color: white;
}

.api-icon {
  font-size: 14px;
}

.api-status-dropdown-content {
  position: fixed;
  top: 60px;
  right: 48px;
  width: 360px;
  background: rgba(255, 255, 255, 1);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-radius: 18px;
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(0, 0, 0, 0.08);
  z-index: 9999999999999;
  overflow: hidden;
  padding: 8px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { 
    opacity: 0;
    transform: translateY(-8px);
  }
  to { 
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 20px;
    height: 48px;
  }

  .logo-section {
    gap: 8px;
  }

  .logo {
    width: 28px;
    height: 28px;
  }

  .app-title {
    font-size: 17px;
  }

  .user-info {
    padding: 6px 12px;
    gap: 8px;
  }
  
  .username {
    font-size: 13px;
  }

  .health-goal {
    display: none;
  }
  
  .api-status-dropdown-content {
    width: 300px;
    right: 20px;
  }
  
  .api-status-btn {
    padding: 6px 12px;
    font-size: 12px;
  }

  .user-actions {
    gap: 12px;
  }
}

.logo-fallback {
  width: 32px;
  height: 32px;
  font-size: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border-radius: 8px;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}
</style>

<style>
/* 全局Logo样式 */
.logo-simple {
  width: 30px;
  height: 30px;
  vertical-align: middle;
  margin-right: 8px;
}
</style>
