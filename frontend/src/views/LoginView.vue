<template>
  <div class="auth-layout">
    <div class="carousel-section">
      <div class="carousel-container">
        <div class="carousel-slide" :class="{ 'active': currentSlide === 0 }">
          <div class="slide-content">
            <div class="slide-icon">🤖</div>
            <h2>AI 智能分析</h2>
            <p>通过先进的人工智能技术，智能识别您的饮食内容，提供精准的营养分析和健康建议</p>
          </div>
        </div>
        
        <div class="carousel-slide" :class="{ 'active': currentSlide === 1 }">
          <div class="slide-content">
            <div class="slide-icon">📊</div>
            <h2>健康数据追踪</h2>
            <p>全面记录您的饮食习惯、营养摄入和身体指标变化，生成详细的健康报告</p>
          </div>
        </div>
        
        <div class="carousel-slide" :class="{ 'active': currentSlide === 2 }">
          <div class="slide-content">
            <div class="slide-icon">🎯</div>
            <h2>个性化目标</h2>
            <p>根据您的身体状况和健康需求，制定专属的营养目标和饮食计划</p>
          </div>
        </div>
        
        <div class="carousel-slide" :class="{ 'active': currentSlide === 3 }">
          <div class="slide-content">
            <div class="slide-icon">🥗</div>
            <h2>智能食谱推荐</h2>
            <p>基于您的口味偏好和营养需求，推荐适合的健康食谱和饮食搭配</p>
          </div>
        </div>
      </div>
      
      <div class="carousel-indicators">
        <div 
          v-for="(slide, index) in slides" 
          :key="index"
          class="indicator"
          :class="{ 'active': currentSlide === index }"
          @click="currentSlide = index"
        ></div>
      </div>
    </div>

    <div class="form-section">
      <div class="form-container">
        <div class="form-card">
          <div class="logo-area">
            <div class="logo-icon">🏥</div>
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-subtitle">请登录您的账号</p>
          </div>

          <el-form @submit.prevent="handleLogin" class="auth-form" :model="loginForm" :rules="rules" ref="loginFormRef">
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                prefix-icon="UserFilled"
                size="large"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                :show-password="showPassword"
                size="large"
              />
            </el-form-item>

            <div class="form-options">
              <el-button type="link" @click="showForgotModal = true" class="forgot-link">
                忘记密码？
              </el-button>
            </div>

            <el-form-item>
              <el-button
                type="primary"
                native-type="submit"
                :loading="loading"
                style="width: 100%"
                size="large"
              >
                {{ loading ? '登录中...' : '立即登录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <p>还没有账号？</p>
            <router-link to="/register" class="link-primary">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showForgotModal"
      title="🔑 忘记密码"
      width="400px"
      destroy-on-close
    >
      <p class="modal-desc">请输入用户名，密码将重置为：12345678</p>
      
      <el-form @submit.prevent="handleForgotPassword" :model="forgotForm" :rules="forgotRules" ref="forgotFormRef">
        <el-form-item prop="username">
          <el-input
            v-model="forgotForm.username"
            placeholder="请输入用户名"
            prefix-icon="UserFilled"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showForgotModal = false">取消</el-button>
          <el-button type="primary" @click="handleForgotPassword" :loading="forgotLoading">
            {{ forgotLoading ? '重置中...' : '重置密码' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { healthApi } from '../api/healthApi';
import { useUserStore } from '../stores/userStore';
import { ElNotification, ElMessage } from 'element-plus';

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref(null);
const forgotFormRef = ref(null);

const loginForm = reactive({
  username: '',
  password: ''
});

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名至少 3 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ]
};

const loading = ref(false);
const showPassword = ref(false);
const showForgotModal = ref(false);

const currentSlide = ref(0);
const slides = ref([
  { icon: '🤖', title: 'AI 智能分析', desc: '通过先进的人工智能技术，智能识别您的饮食内容，提供精准的营养分析和健康建议' },
  { icon: '📊', title: '健康数据追踪', desc: '全面记录您的饮食习惯、营养摄入和身体指标变化，生成详细的健康报告' },
  { icon: '🎯', title: '个性化目标', desc: '根据您的身体状况和健康需求，制定专属的营养目标和饮食计划' },
  { icon: '🥗', title: '智能食谱推荐', desc: '基于您的口味偏好和营养需求，推荐适合的健康食谱和饮食搭配' }
]);

let carouselInterval;

const startCarousel = () => {
  carouselInterval = setInterval(() => {
    currentSlide.value = (currentSlide.value + 1) % slides.value.length;
  }, 4000);
};

const stopCarousel = () => {
  if (carouselInterval) {
    clearInterval(carouselInterval);
  }
};

onMounted(() => {
  startCarousel();
});

onUnmounted(() => {
  stopCarousel();
});

const handleLogin = async () => {
  try {
    await loginFormRef.value.validate();
    loading.value = true;
    userStore.clearError();

    try {
      const response = await healthApi.login({
        username: loginForm.username.trim(),
        password: loginForm.password
      });

      if (response.success) {
        userStore.setUserData({
          userId: response.user.userId,
          name: response.user.userId,
          goal: response.user.healthGoal || '未设置',
          currentWeight: response.user.weight || 0,
          targetWeight: response.user.weight || 0
        });

        ElNotification({
          title: '登录成功',
          message: '欢迎回来！',
          type: 'success',
          duration: 3000
        });

        setTimeout(() => {
          router.push('/');
        }, 500);
      } else {
        ElNotification({
          title: '登录失败',
          message: response.message || '用户名或密码错误',
          type: 'error',
          duration: 3000
        });
      }
    } catch (error) {
      console.log('后端连接失败，使用模拟登录');
      
      const testAccounts = [
        { username: 'testuser', password: '123456' }
      ];

      const isValidAccount = testAccounts.some(
        account => account.username === loginForm.username.trim() && 
                  account.password === loginForm.password
      );

      if (isValidAccount) {
        userStore.setUserData({
          userId: loginForm.username.trim(),
          name: loginForm.username.trim(),
          goal: '健康饮食',
          currentWeight: 65,
          targetWeight: 60
        });

        ElNotification({
          title: '登录成功（演示模式）',
          message: '欢迎回来！当前为演示模式，部分功能可能受限',
          type: 'success',
          duration: 3000
        });

        setTimeout(() => {
          router.push('/');
        }, 500);
      } else {
        ElNotification({
          title: '登录失败',
          message: '演示账户：testuser/123456',
          type: 'error',
          duration: 3000
        });
      }
    }
  } catch (error) {
    console.error('登录错误:', error);
    
    if (error.response) {
      const status = error.response.status;
      if (status === 401) {
        ElNotification({
          title: '登录失败',
          message: '用户名或密码错误',
          type: 'error',
          duration: 3000
        });
      } else if (status === 403) {
        ElNotification({
          title: '账号锁定',
          message: '账号已被锁定，请联系管理员',
          type: 'error',
          duration: 3000
        });
      } else if (status === 500) {
        ElNotification({
          title: '服务器错误',
          message: '请稍后重试',
          type: 'error',
          duration: 3000
        });
      } else {
        ElNotification({
          title: '登录失败',
          message: error.response.data?.message || `错误代码：${status}`,
          type: 'error',
          duration: 3000
        });
      }
    } else {
      ElNotification({
        title: '登录失败',
        message: error.message || '请稍后重试',
        type: 'error',
        duration: 3000
      });
    }
  } finally {
    loading.value = false;
  }
};

const handleForgotPassword = async () => {
  try {
    await forgotFormRef.value.validate();
    const loadingLocal = ref(true);

    const response = await healthApi.forgotPassword({
      username: loginForm.username.trim()
    });

    if (response.success) {
      ElNotification({
        title: '重置成功',
        message: '密码已重置为：12345678',
        type: 'success',
        duration: 3000
      });
      showForgotModal.value = false;
    } else {
      ElNotification({
        title: '重置失败',
        message: response.message || '用户不存在',
        type: 'error',
        duration: 3000
      });
    }
  } catch (error) {
    ElNotification({
      title: '操作失败',
      message: error.message || '请稍后重试',
      type: 'error',
      duration: 3000
    });
  } finally {
    loadingLocal.value = false;
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

* {
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Inter', 'Segoe UI', Roboto, sans-serif;
}

.auth-layout {
  display: flex;
  min-height: 100vh;
  background: #ffffff;
}

.carousel-section {
  flex: 1;
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  min-height: 100vh;
}

.carousel-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    radial-gradient(ellipse at 30% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 80%, rgba(255, 255, 255, 0.08) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.carousel-container {
  position: relative;
  width: 100%;
  max-width: 520px;
  padding: 32px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.carousel-slide {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) translateY(20px);
  width: 100%;
  opacity: 0;
  transition: all 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  text-align: center;
  color: white;
}

.carousel-slide.active {
  opacity: 1;
  transform: translate(-50%, -50%) translateY(0);
}

.slide-content {
  padding: 28px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  background: rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
}

.slide-icon {
  font-size: 3.5rem;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

.carousel-slide h2 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 12px 0;
  letter-spacing: -0.3px;
}

.carousel-slide p {
  font-size: 1rem;
  line-height: 1.6;
  margin: 0;
  opacity: 0.9;
  max-width: 400px;
  margin: 0 auto;
  letter-spacing: -0.1px;
}

.carousel-indicators {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 3;
}

.indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.indicator.active {
  background: white;
  transform: scale(1.2);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.3);
}

.indicator:hover {
  background: rgba(255, 255, 255, 0.6);
}

.form-section {
  flex: 1;
  background: linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.form-container {
  width: 100%;
  max-width: 380px;
}

.form-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 8px 32px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.logo-area {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon {
  font-size: 3rem;
  margin-bottom: 12px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.form-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0 0 6px 0;
  letter-spacing: -0.3px;
}

.form-subtitle {
  color: #86868b;
  font-size: 0.9rem;
  margin: 0;
  letter-spacing: -0.1px;
}

.auth-form {
  margin-bottom: 20px;
}

.form-options {
  text-align: right;
  margin-bottom: 16px;
}

.forgot-link {
  color: #007aff;
  font-size: 0.875rem;
  font-weight: 500;
}

.forgot-link:hover {
  color: #0066d6;
}

.form-footer {
  text-align: center;
  color: #86868b;
  font-size: 0.875rem;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.form-footer p {
  margin: 0 0 6px 0;
}

.link-primary {
  color: #007aff;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.link-primary:hover {
  color: #0066d6;
}

.modal-desc {
  color: #86868b;
  font-size: 0.9rem;
  line-height: 1.6;
  margin: 0 0 20px 0;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.12);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.3);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
  border: none;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: -0.1px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.3);
}

@media (max-width: 900px) {
  .auth-layout {
    flex-direction: column;
  }

  .carousel-section {
    min-height: 50vh;
    clip-path: none;
  }

  .form-section {
    padding: 24px;
  }

  .form-card {
    padding: 24px;
  }
}
</style>
