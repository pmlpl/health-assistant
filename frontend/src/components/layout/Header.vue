<!-- 顶栏 — Minimalist Monochrome 线条布局 -->
<template>
  <header class="header">
    <div class="logo-section">
      <div class="bh-logo-shapes" aria-hidden="true">
        <span class="bh-logo-shapes__circle" />
        <span class="bh-logo-shapes__square" />
        <span class="bh-logo-shapes__triangle" />
      </div>
      <h1 class="app-title">AI健康助手</h1>
    </div>
    <div class="user-actions">
      <router-link to="/guide" class="guide-nav-link">使用手册</router-link>
      <!-- API 状态下拉 -->
      <div class="api-status-dropdown">
        <button
          type="button"
          class="api-status-btn"
          :class="{ 'api-status-btn--active': apiStatusOpen }"
          @click="apiStatusOpen = !apiStatusOpen"
        >
          <span class="api-icon" aria-hidden="true">▣</span>
          API 状态
        </button>
        <div v-if="apiStatusOpen" class="api-status-dropdown-content">
          <ApiStatusIndicator />
        </div>
      </div>
      <div v-if="userStore.isLoggedIn" class="user-info">
        <span class="username">{{ userStore.userData.userId }}</span>
        <span v-if="userStore.userData.goal" class="health-goal">| {{ userStore.userData.goal }}</span>
        <button
          type="button"
          class="logout-btn"
          :disabled="isLoggingOut"
          :class="{ 'logout-btn--loading': isLoggingOut }"
          @click="handleLogout"
        >
          <span v-if="isLoggingOut" class="loading-spinner" aria-hidden="true" />
          {{ isLoggingOut ? '退出中…' : '退出' }}
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
import { notifySuccess, notifyError } from '../../utils/notify';

const userStore = useUserStore();
const router = useRouter();
const isLoggingOut = ref(false);
const apiStatusOpen = ref(false);

/** 确认后注销并跳转登录页 */
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '注销确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    });

    try {
      isLoggingOut.value = true;
      await userStore.logout();
      notifySuccess('退出登录成功');
      router.push('/login');
    } catch (error) {
      console.error('注销失败:', error);
      notifyError('退出登录失败，请重试');
    } finally {
      isLoggingOut.value = false;
    }
  } catch {
    // 用户取消
  }
};

/** Logo 加载失败时显示文字占位 */
const handleLogoError = (event) => {
  event.target.style.display = 'none';
  const logoSection = event.target.parentElement;
  const fallback = document.createElement('div');
  fallback.className = 'logo-fallback';
  fallback.textContent = 'HA';
  logoSection.appendChild(fallback);
};
</script>

<style scoped>
.header {
  background: var(--color-background);
  color: var(--color-foreground);
  padding: 0 var(--space-8);
  height: var(--header-height);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid var(--color-border);
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1001;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.logo {
  width: 32px;
  height: 32px;
}

.app-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-black);
  letter-spacing: -0.02em;
  text-transform: uppercase;
  color: var(--color-foreground);
}

.user-actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.guide-nav-link {
  font-family: var(--font-body);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-foreground);
  text-decoration: none;
  padding: var(--space-2) var(--space-3);
  border: var(--border-width) solid var(--color-border);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.guide-nav-link:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border-light);
  background: var(--color-muted);
}

.username {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-foreground);
}

.health-goal {
  font-size: var(--font-size-sm);
  color: var(--color-muted-foreground);
}

.logout-btn {
  background: var(--color-primary-red);
  color: #fff;
  border: var(--border-width) solid var(--color-border);
  padding: var(--space-2) var(--space-4);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.logout-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.logout-btn:active:not(:disabled) {
  transform: translate(2px, 2px);
  box-shadow: none;
}

.logout-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid var(--color-border-light);
  border-top-color: var(--color-foreground);
  display: inline-block;
  margin-right: var(--space-2);
  vertical-align: middle;
}

.login-prompt .login-link {
  display: inline-flex;
  align-items: center;
  padding: var(--space-2) var(--space-5);
  border: 2px solid var(--color-foreground);
  background: var(--color-foreground);
  color: var(--color-background);
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  text-decoration: none;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.login-prompt .login-link:hover {
  background: var(--color-background);
  color: var(--color-foreground);
}

.api-status-dropdown {
  position: relative;
  z-index: 10001;
}

.api-status-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  background: var(--color-background);
  color: var(--color-foreground);
  border: 1px solid var(--color-border);
  padding: var(--space-2) var(--space-4);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.api-status-btn:hover {
  background: var(--color-muted);
}

.api-status-btn--active {
  background: var(--color-foreground);
  color: var(--color-background);
}

.api-status-dropdown-content {
  position: fixed;
  top: calc(var(--header-height) + var(--space-2));
  right: var(--space-8);
  width: 360px;
  background: var(--color-background);
  border: 2px solid var(--color-border);
  z-index: 9999;
  padding: var(--space-2);
}

.logo-fallback {
  width: 32px;
  height: 32px;
  font-family: var(--font-mono);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-foreground);
  color: var(--color-background);
}

@media (min-width: 1024px) {
  .guide-nav-link {
    display: none;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 var(--space-5);
    height: 48px;
  }

  .app-title {
    font-size: var(--font-size-lg);
  }

  .health-goal {
    display: none;
  }

  .api-status-dropdown-content {
    width: min(300px, calc(100vw - var(--space-8)));
    right: var(--space-4);
  }

  .user-actions {
    gap: var(--space-2);
  }
}
</style>
