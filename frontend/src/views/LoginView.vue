<template>
  <AuthLayout>
    <div class="auth-card bh-card">
      <header class="auth-card__head">
        <div class="auth-card__logo" aria-hidden="true">🌿</div>
        <h1 class="auth-card__title">欢迎回来</h1>
        <p class="auth-card__subtitle">登录以继续管理你的健康数据</p>
      </header>

      <el-form
        ref="loginFormRef"
        class="auth-card__form"
        :model="loginForm"
        :rules="rules"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="UserFilled"
            size="large"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
            size="large"
            autocomplete="current-password"
          />
        </el-form-item>

        <div class="auth-card__options">
          <el-button type="primary" link @click="showForgotModal = true">
            忘记密码？
          </el-button>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            size="large"
            class="btn-brand"
            style="width: 100%"
          >
            {{ loading ? '登录中…' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-social">
        <div class="auth-social__divider">或使用以下方式</div>
        <button type="button" class="auth-social__btn" disabled title="即将上线">微信登录</button>
        <button type="button" class="auth-social__btn" disabled title="即将上线">Apple 登录</button>
      </div>

      <footer class="auth-card__footer">
        <p>还没有账号？</p>
        <router-link to="/register" class="auth-card__link">立即注册</router-link>
      </footer>
    </div>

    <el-dialog v-model="showForgotModal" title="忘记密码" width="400px" destroy-on-close>
      <p class="auth-dialog-hint">
        在线重置暂未开放，请登录后在「个人中心」修改密码。
      </p>
      <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules">
        <el-form-item prop="username">
          <el-input
            v-model="forgotForm.username"
            placeholder="用户名"
            prefix-icon="UserFilled"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForgotModal = false">关闭</el-button>
        <el-button type="primary" :loading="forgotLoading" @click="handleForgotPassword">
          提交
        </el-button>
      </template>
    </el-dialog>
  </AuthLayout>
</template>

<script setup>
import { ref, reactive, inject, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import AuthLayout from '../components/auth/AuthLayout.vue';
import { healthApi } from '../api/healthApi';
import { useUserStore } from '../stores/userStore';
import { notifySuccess, notifyError, notifyWarning } from '../utils/notify';
const shakeForm = inject('authShakeForm', () => {});

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref(null);
const forgotFormRef = ref(null);

const loginForm = reactive({
  username: '',
  password: '',
});

const forgotForm = reactive({
  username: '',
});

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名至少 3 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' },
  ],
};

const forgotRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
};

const loading = ref(false);
const forgotLoading = ref(false);
const showForgotModal = ref(false);

/** 登录成功后等待 store 落盘再跳转，避免守卫误判未登录 */
const goHomeAfterLogin = async () => {
  await nextTick();
  const failure = await router.replace('/');
  if (failure || router.currentRoute.value.path === '/login') {
    window.location.assign('/');
  }
};

const handleLogin = async () => {
  try {
    await loginFormRef.value.validate();
    loading.value = true;
    userStore.clearError();

    try {
      const response = await healthApi.login({
        username: loginForm.username.trim(),
        password: loginForm.password,
      });

      if (response.success) {
        userStore.setUserData(
          {
            userId: response.user.userId,
            name: response.user.userId,
            goal: response.user.healthGoal || '未设置',
            currentWeight: response.user.weight || 0,
            targetWeight: response.user.weight || 0,
          },
          response.token
        );
        await goHomeAfterLogin();
        notifySuccess('欢迎回来！', '登录成功');
      } else {
        notifyError(response.message || '用户名或密码错误', '登录失败');
      }
    } catch {
      const testAccounts = [{ username: 'testuser', password: '123456' }];
      const ok = testAccounts.some(
        (a) => a.username === loginForm.username.trim() && a.password === loginForm.password
      );
      if (ok) {
        userStore.setUserData(
          {
            userId: loginForm.username.trim(),
            name: loginForm.username.trim(),
            goal: '健康饮食',
            currentWeight: 65,
            targetWeight: 60,
          },
          `demo-${loginForm.username.trim()}`
        );
        await goHomeAfterLogin();
        notifySuccess('欢迎回来！当前为演示模式', '登录成功（演示）');
      } else {
        notifyError('演示账户：testuser / 123456', '登录失败');
      }
    }
  } catch (error) {
    shakeForm();
    if (error?.response?.status === 401) {
      notifyError('用户名或密码错误', '登录失败');
    } else {
      notifyError(error?.message || '请稍后重试', '登录失败');
    }
  } finally {
    loading.value = false;
  }
};

const handleForgotPassword = async () => {
  try {
    await forgotFormRef.value.validate();
    forgotLoading.value = true;
    const response = await healthApi.forgotPassword({
      username: forgotForm.username.trim(),
    });
    if (!response.success) {
      notifyWarning(response.message || '暂不支持在线重置', '提示');
    }
    showForgotModal.value = false;
  } catch (error) {
    notifyError(error?.message || '请稍后重试', '操作失败');
  } finally {
    forgotLoading.value = false;
  }
};
</script>

<style scoped>
.auth-dialog-hint {
  margin: 0 0 var(--space-4);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>
