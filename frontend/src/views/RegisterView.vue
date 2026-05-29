<template>

  <AuthLayout>

    <div ref="cardRef" class="auth-card bh-card">

      <div v-if="registerSuccess" class="register-success">

        <span class="register-success__icon">✅</span>

        <h2 class="auth-card__title">注册成功！</h2>

        <p class="auth-card__subtitle">正在跳转到个人中心…</p>

      </div>



      <template v-else>

        <header class="auth-card__head">

          <div class="auth-card__logo" aria-hidden="true">🌿</div>

          <h1 class="auth-card__title">创建账号</h1>

          <p class="auth-card__subtitle">开始记录饮食、追踪健康目标</p>

        </header>



        <div class="auth-steps" aria-label="注册步骤">

          <span

            v-for="i in 3"

            :key="i"

            class="auth-steps__dot"

            :class="{ 'auth-steps__dot--active': currentStep === i - 1 }"

          />

        </div>



        <el-form

          ref="registerFormRef"

          class="auth-card__form"

          :model="registerForm"

          :rules="rules"

          @submit.prevent="handleNextOrSubmit"

        >

          <Transition name="auth-step" mode="out-in">

            <div v-if="currentStep === 0" key="step0">

              <el-form-item prop="username">

                <el-input

                  v-model="registerForm.username"

                  placeholder="用户名（3–20 位字母数字下划线）"

                  prefix-icon="UserFilled"

                  size="large"

                  autocomplete="username"

                />

              </el-form-item>

            </div>



            <div v-else-if="currentStep === 1" key="step1">

              <el-form-item prop="password">

                <el-input

                  v-model="registerForm.password"

                  type="password"

                  placeholder="密码（至少 6 位）"

                  prefix-icon="Lock"

                  show-password

                  size="large"

                  autocomplete="new-password"

                />

              </el-form-item>

              <div v-if="registerForm.password" class="password-strength">

                <div
                  class="password-strength__fill"
                  :class="passwordStrengthClass"
                  :style="{ width: passwordStrengthPercent }"
                />

              </div>

            </div>



            <div v-else key="step2">

              <el-form-item prop="confirmPassword">

                <el-input

                  v-model="registerForm.confirmPassword"

                  type="password"

                  placeholder="确认密码"

                  prefix-icon="Lock"

                  show-password

                  size="large"

                  autocomplete="new-password"

                />

              </el-form-item>

            </div>

          </Transition>



          <el-form-item>

            <el-button

              type="primary"

              native-type="submit"

              :loading="loading"

              size="large"

              class="btn-brand"

              style="width: 100%"

            >

              {{ submitLabel }}

            </el-button>

          </el-form-item>



          <el-button

            v-if="currentStep > 0"

            type="default"

            size="large"

            style="width: 100%"

            @click="prevStep"

          >

            上一步

          </el-button>

        </el-form>



        <footer class="auth-card__footer">

          <p>已有账号？</p>

          <router-link to="/login" class="auth-card__link">立即登录</router-link>

        </footer>



        <div class="auth-card__benefits">

          <h4>注册后即可使用</h4>

          <ul>

            <li>饮食日记与拍照识食</li>

            <li>AI 营养咨询</li>

            <li>健康报告与日历</li>

            <li>智能食谱推荐</li>

          </ul>

        </div>

      </template>

    </div>

  </AuthLayout>

</template>



<script setup>

import { ref, reactive, computed, inject } from 'vue';

import { useRouter } from 'vue-router';

import AuthLayout from '../components/auth/AuthLayout.vue';

import { healthApi } from '../api/healthApi';

import { useUserStore } from '../stores/userStore';

import { notifySuccess, notifyError } from '../utils/notify';



const shakeForm = inject('authShakeForm', () => {});

const router = useRouter();

const userStore = useUserStore();

const registerFormRef = ref(null);

const cardRef = ref(null);

const currentStep = ref(0);

const registerSuccess = ref(false);



const registerForm = reactive({

  username: '',

  password: '',

  confirmPassword: '',

});



const stepFields = [['username'], ['password'], ['confirmPassword']];



const rules = {

  username: [

    { required: true, message: '请输入用户名', trigger: 'blur' },

    { min: 3, message: '用户名至少 3 个字符', trigger: 'blur' },

    { max: 20, message: '用户名不能超过 20 个字符', trigger: 'blur' },

    {

      pattern: /^[a-zA-Z0-9_]+$/,

      message: '仅支持字母、数字和下划线',

      trigger: 'blur',

    },

  ],

  password: [

    { required: true, message: '请输入密码', trigger: 'blur' },

    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' },

    { max: 20, message: '密码不能超过 20 个字符', trigger: 'blur' },

  ],

  confirmPassword: [

    { required: true, message: '请确认密码', trigger: 'blur' },

    {

      validator: (_rule, value, callback) => {

        if (value !== registerForm.password) {

          callback(new Error('两次输入的密码不一致'));

        } else {

          callback();

        }

      },

      trigger: 'blur',

    },

  ],

};



const loading = ref(false);



const passwordStrengthClass = computed(() => {

  const p = registerForm.password;

  if (!p) return '';

  let score = 0;

  if (p.length >= 8) score += 1;

  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score += 1;

  if (/\d/.test(p)) score += 1;

  if (/[^a-zA-Z0-9]/.test(p)) score += 1;

  if (score <= 1) return 'password-strength__fill--weak';

  if (score <= 2) return 'password-strength__fill--medium';

  return 'password-strength__fill--strong';

});

/** 密码强度百分比（CSS 宽度过渡，无 GSAP） */
const passwordStrengthPercent = computed(() => {
  const cls = passwordStrengthClass.value;
  if (cls.includes('weak')) return '33%';
  if (cls.includes('medium')) return '66%';
  if (cls.includes('strong')) return '100%';
  return '0%';
});

const submitLabel = computed(() => {

  if (loading.value) return '注册中…';

  return currentStep.value < 2 ? '下一步' : '完成注册';

});



const prevStep = async () => {
  currentStep.value -= 1;
};

const playSuccessCelebration = () => {
  registerSuccess.value = true;
};

const handleNextOrSubmit = async () => {

  try {

    const fields = stepFields[currentStep.value];
    for (const field of fields) {
      await registerFormRef.value.validateField(field);
    }



    if (currentStep.value < 2) {

      currentStep.value += 1;

      return;

    }



    loading.value = true;

    userStore.clearError();



    const response = await healthApi.register({

      username: registerForm.username.trim(),

      password: registerForm.password,

    });



    if (response.success) {

      userStore.setUserData(

        {

          userId: response.user?.userId || registerForm.username.trim(),

          name: registerForm.username.trim(),

          goal: '',

          currentWeight: 0,

          targetWeight: 0,

        },

        response.token

      );

      playSuccessCelebration();

      notifySuccess('账号创建成功', '注册成功');

      setTimeout(() => router.push('/profile'), 1200);

    } else {

      notifyError(response.message || '请稍后重试', '注册失败');

      shakeForm();

    }

  } catch (error) {

    shakeForm();

    const status = error?.response?.status;

    if (status === 400) {

      notifyError('用户名已存在', '注册失败');

    } else if (error?.request) {

      notifyError('无法连接服务器，请检查网络', '网络错误');

    } else if (!error?.fields) {

      notifyError(error?.message || '请稍后重试', '注册失败');

    }

  } finally {

    loading.value = false;

  }

};

</script>



<style scoped>

.auth-step-enter-active,

.auth-step-leave-active {

  transition: opacity 0.25s ease, transform 0.25s ease;

}



.auth-step-enter-from {

  opacity: 0;

  transform: translateX(16px);

}



.auth-step-leave-to {

  opacity: 0;

  transform: translateX(-16px);

}

</style>

