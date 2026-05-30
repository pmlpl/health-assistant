<template>
  <div class="dashboard-layout">
    <PageHeader
      title="个人中心"
      section-label="我的档案"
      :label-pulse="true"
      icon="👤"
    >
      <template #stats>
        <span class="stat-item">欢迎，{{ userInfo.username }}！</span>
        <span v-if="profileData.healthGoal" class="stat-item">
          🎯 目标：{{ profileData.healthGoal }}
        </span>
        <span v-else class="stat-item">🌟 完善您的健康档案吧！</span>
      </template>
    </PageHeader>

    <!-- 主要内容区域 - 上下三行布局 -->
    <div class="three-row-layout">
      <!-- 第一行：用户基本信息 -->
      <div class="row first-row">
        <div class="card profile-card bh-card">
          <div class="card-header-section">
            <SectionLabel label="账户信息" />
            <h2>🔐 用户基本信息</h2>
          </div>
          <div class="user-info-section">
            <div class="info-item">
              <label>👤 用户名:</label>
              <div class="info-display">
                <span>{{ userInfo.username }}</span>
                <button @click="showUsernameModal = true" class="edit-btn">✏️ 修改</button>
              </div>
            </div>
                
            <div class="info-item">
              <label>📅 注册时间:</label>
              <span>{{ formatDate(userInfo.createdAt) }}</span>
            </div>
          </div>
          
          <!-- 账户安全设置区域 -->
          <div class="section-divider"></div>
          <div class="security-section-inline">
            <h3 class="subsection-title">🔐 账户安全设置</h3>
            <div class="security-buttons">
              <button @click="showPasswordModal = true" class="security-btn">
                🔑 修改密码
              </button>
              
              <button @click="logoutUser" class="security-btn secondary">
                🚪 注销登录
              </button>
            </div>
          </div>
          
          <!-- 危险操作区域 -->
          <div class="section-divider"></div>
          <div class="danger-zone-inline">
            <h3 class="subsection-title danger-title">⚠️ 危险操作区</h3>
            <div class="danger-section-inline">
              <p class="warning-text">⚠️ 删除账户是永久性操作，将删除您的所有数据且无法恢复。</p>
              <button @click="showDeleteModal = true" class="danger-btn">
                🗑️ 删除账户
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 第二行：个人健康档案 -->
      <div class="row second-row">
        <div class="card profile-form-card bh-card">
          <div class="card-header-section">
            <SectionLabel label="健康档案" />
            <h2>📋 个人健康档案</h2>
          </div>
          
          <form @submit.prevent="saveProfile" class="profile-form">
            <!-- 基本身体信息 -->
            <div class="form-section">
              <h3 class="section-title">📏 身体信息</h3>
              <div class="form-row">
                <div class="form-group">
                  <label>📏 身高 (cm)</label>
                  <input 
                    type="number" 
                    v-model="profileData.height" 
                    placeholder="请输入身高" 
                    class="form-input"
                  />
                </div>
                <div class="form-group">
                  <label>⚖️ 体重 (kg)</label>
                  <input 
                    type="number" 
                    v-model="profileData.weight" 
                    placeholder="请输入体重" 
                    class="form-input"
                  />
                </div>
              </div>
              
              <div class="form-group">
                <label>🎂 年龄</label>
                <input 
                  type="number" 
                  v-model="profileData.age" 
                  placeholder="请输入年龄" 
                  class="form-input"
                />
              </div>
            </div>

            <!-- 生活习惯 -->
            <div class="form-section">
              <h3 class="section-title">🏃 生活习惯</h3>
              <div class="form-row">
                <div class="form-group">
                  <label>👫 性别</label>
                  <select v-model="profileData.gender" class="form-select">
                    <option value="">请选择性别</option>
                    <option value="M">男</option>
                    <option value="F">女</option>
                    <option value="O">其他</option>
                    <option value="N">不愿透露</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>💪 活动量</label>
                  <select v-model="profileData.activityLevel" class="form-select">
                    <option value="">请选择活动量</option>
                    <option value="久坐">久坐</option>
                    <option value="轻度运动">轻度运动</option>
                    <option value="中度运动">中度运动</option>
                    <option value="重度运动">重度运动</option>
                  </select>
                </div>
              </div>
            </div>

            <!-- 健康目标 -->
            <div class="form-section">
              <h3 class="section-title">🎯 健康目标</h3>
              <div class="form-group">
                <label>🎯 健康目标</label>
                <select v-model="profileData.healthGoal" class="form-select">
                  <option value="">请选择健康目标</option>
                  <option value="减脂">减脂</option>
                  <option value="增肌">增肌</option>
                  <option value="控糖">控糖</option>
                  <option value="养胃">养胃</option>
                  <option value="日常养生">日常养生</option>
                </select>
              </div>
            </div>

            <!-- 特殊需求 -->
            <div class="form-section">
              <h3 class="section-title">⚠️ 特殊需求</h3>
              <div class="form-group">
                <label>🚫 饮食禁忌</label>
                <textarea 
                  v-model="profileData.dietaryRestrictions" 
                  placeholder="如有食物过敏或禁忌，请填写，多个请用逗号分隔" 
                  class="form-textarea"
                ></textarea>
              </div>
              
              <div class="form-group">
                <label>😋 口味偏好</label>
                <input 
                  type="text" 
                  v-model="profileData.tastePreferences" 
                  placeholder="如：清淡、辣味、甜味等" 
                  class="form-input"
                />
                <p class="help-text">多个偏好请用逗号分隔</p>
              </div>
            </div>

            <!-- 提交按钮 -->
            <div class="form-actions">
              <button type="submit" class="btn-brand submit-btn" :disabled="loading">
                {{ loading ? '⏳ 保存中...' : '💾 保存档案' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 修改用户名模态框 -->
    <div v-if="showUsernameModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>✏️ 修改用户名</h3>
          <button @click="closeModals" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>👤 新用户名</label>
            <input 
              type="text" 
              v-model="newUsername" 
              placeholder="请输入新用户名" 
              class="form-input"
              maxlength="50"
            />
            <p class="help-text">用户名长度为1-50个字符</p>
          </div>
          <div class="modal-actions">
            <button @click="closeModals" class="cancel-btn">取消</button>
            <button @click="updateUsername" class="confirm-btn" :disabled="!newUsername.trim()">
              ✅ 确认修改
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改密码模态框 -->
    <div v-if="showPasswordModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>🔑 修改密码</h3>
          <button @click="closeModals" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>🔐 原密码</label>
            <input 
              type="password" 
              v-model="oldPassword" 
              placeholder="请输入原密码" 
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label>🆕 新密码</label>
            <input 
              type="password" 
              v-model="newPassword" 
              placeholder="请输入新密码" 
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label>🔐 确认新密码</label>
            <input 
              type="password" 
              v-model="confirmPassword" 
              placeholder="请再次输入新密码" 
              class="form-input"
            />
          </div>
          <div class="modal-actions">
            <button @click="closeModals" class="cancel-btn">取消</button>
            <button @click="updatePassword" class="confirm-btn" :disabled="!canUpdatePassword">
              ✅ 确认修改
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除账户确认模态框 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click="closeModals">
      <div class="modal-content danger-modal" @click.stop>
        <div class="modal-header">
          <h3>⚠️ 确认删除账户</h3>
          <button @click="closeModals" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <p class="warning-text">⚠️ 您确定要删除账户 "{{ userInfo.username }}" 吗？</p>
          <p class="warning-text">⚠️ 此操作将永久删除您的所有数据，包括：</p>
          <ul class="deletion-list">
            <li>📋 个人档案信息</li>
            <li>🍽️ 所有饮食记录</li>
            <li>📊 健康数据分析</li>
            <li>⚙️ 个性化设置</li>
          </ul>
          <div class="form-group">
            <label>⚠️ 请输入您的用户名确认删除</label>
            <input 
              type="text" 
              v-model="deleteConfirmation" 
              :placeholder="'请输入: ' + userInfo.username" 
              class="form-input"
            />
          </div>
          <div class="modal-actions">
            <button @click="closeModals" class="cancel-btn">取消</button>
            <button @click="deleteUserAccount" class="danger-confirm-btn" :disabled="deleteConfirmation !== userInfo.username">
              🗑️ 确认删除
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import PageHeader from '@/components/common/PageHeader.vue';
import SectionLabel from '@/components/common/SectionLabel.vue';
import { useRouter } from 'vue-router';
import { healthApi } from '../api/healthApi';
import { useUserStore } from '../stores/userStore';
import { ElNotification, ElMessageBox } from 'element-plus';

const router = useRouter();
const userStore = useUserStore();

// 用户信息
const userInfo = ref({
  username: '',
  createdAt: ''
});

// 档案数据
const profileData = ref({
  userId: '',
  height: null,
  weight: null,
  age: null,
  gender: '',
  activityLevel: '',
  healthGoal: '',
  dietaryRestrictions: '',
  tastePreferences: '',
  targetCalories: null,
  targetProtein: null,
  targetCarbs: null,
  targetFat: null
});

// 模态框状态
const showUsernameModal = ref(false);
const showPasswordModal = ref(false);
const showDeleteModal = ref(false);

// 修改用户名相关
const newUsername = ref('');

// 修改密码相关
const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');

// 删除账户相关
const deleteConfirmation = ref('');

const loading = ref(false);

// 计算属性
const canUpdatePassword = computed(() => {
  return oldPassword.value.trim() && 
         newPassword.value.trim() && 
         confirmPassword.value.trim() &&
         newPassword.value === confirmPassword.value &&
         newPassword.value.length >= 6;
});

// 页面加载时获取用户信息
onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login');
    return;
  }

  try {
    // 获取用户基本信息
    userInfo.value.username = userStore.userData.userId || userStore.userData.name || '';
    
    // 获取用户档案
    const existingProfile = await healthApi.getUserProfile(userInfo.value.username);
    if (existingProfile) {
      profileData.value = {
        ...profileData.value,
        ...existingProfile,
        userId: userInfo.value.username,
        dietaryRestrictions: existingProfile.dietaryRestrictions?.join(', ') || '',
        tastePreferences: existingProfile.tastePreferences?.join(', ') || ''
      };
      
      // 设置用户信息的时间字段
      userInfo.value.createdAt = existingProfile.createdAt;
    } else {
      // 如果没有档案，初始化基本数据
      profileData.value.userId = userInfo.value.username;
    }

  } catch (err) {
    console.log('获取用户信息失败:', err);
  }
});
const saveProfile = async () => {
  try {
    loading.value = true;
    userStore.clearError();

    const profilePayload = {
      userId: profileData.value.userId,
      height: profileData.value.height ? parseInt(profileData.value.height) : null,
      weight: profileData.value.weight ? parseFloat(profileData.value.weight) : null,
      age: profileData.value.age ? parseInt(profileData.value.age) : null,
      gender: profileData.value.gender || '',
      activityLevel: profileData.value.activityLevel || '',
      healthGoal: profileData.value.healthGoal || '',
      dietaryRestrictions: profileData.value.dietaryRestrictions ?
          profileData.value.dietaryRestrictions.split(',').map(item => item.trim()).filter(item => item) : [],
      tastePreferences: profileData.value.tastePreferences ?
          profileData.value.tastePreferences.split(',').map(item => item.trim()).filter(item => item) : []
    };

    await healthApi.createUserProfile(profilePayload);
    ElNotification({
      title: '✅ 保存成功',
      message: '个人档案保存成功！',
      type: 'success',
      duration: 3000,
      offset: 80
    });
    
    // 更新用户存储中的数据
    userStore.setUserData({
      name: profileData.value.userId,
      goal: profileData.value.healthGoal || '未设置'
    });
    
  } catch (err) {
    userStore.setError(err.message || '保存档案失败');
    console.error('保存档案失败:', err);
  } finally {
    loading.value = false;
  }
};

// 修改用户名
const updateUsername = async () => {
  if (!newUsername.value.trim()) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请输入新用户名',
      type: 'warning',
      duration: 2000,
      offset: 80
    });
    return;
  }

  try {
    const result = await healthApi.updateUsername(userInfo.value.username, newUsername.value.trim());
    if (result.success) {
      ElNotification({
        title: '✅ 修改成功',
        message: '用户名修改成功！',
        type: 'success',
        duration: 3000,
        offset: 80
      });
      userInfo.value.username = newUsername.value.trim();
      profileData.value.userId = newUsername.value.trim();
      
      // 更新用户存储
      userStore.setUserData({
        userId: newUsername.value.trim(),
        name: newUsername.value.trim()
      });
      
      closeModals();
      newUsername.value = '';
    } else {
      ElNotification({
        title: '❌ 修改失败',
        message: result.message || '用户名修改失败',
        type: 'error',
        duration: 3000,
        offset: 80
      });
    }
  } catch (err) {
    ElNotification({
      title: '❌ 修改失败',
      message: '用户名修改失败：' + (err.message || '网络错误'),
      type: 'error',
      duration: 3000,
      offset: 80
    });
  }
};

// 修改密码
const updatePassword = async () => {
  if (!canUpdatePassword.value) {
    ElNotification({
      title: '⚠️ 提示',
      message: '请检查密码输入是否正确',
      type: 'warning',
      duration: 2000,
      offset: 80
    });
    return;
  }

  try {
    const result = await healthApi.updatePassword(
        userInfo.value.username,
        oldPassword.value,
        newPassword.value
    );

    if (result.success) {
      ElNotification({
        title: '✅ 修改成功',
        message: '密码修改成功！',
        type: 'success',
        duration: 3000,
        offset: 80
      });
      closeModals();
      clearPasswordFields();
    } else {
      ElNotification({
        title: '❌ 修改失败',
        message: result.message || '密码修改失败',
        type: 'error',
        duration: 3000,
        offset: 80
      });
    }
  } catch (err) {
    ElNotification({
      title: '❌ 修改失败',
      message: '密码修改失败：' + (err.message || '网络错误'),
      type: 'error',
      duration: 3000,
      offset: 80
    });
  }
};

// 注销用户
const logoutUser = async () => {
    try {
      await ElMessageBox.confirm(
          '确定要注销登录吗？',
          '注销确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }
      );

      try {
        await healthApi.logout(userInfo.value.username);
      } catch (err) {
        console.error('注销 API 调用失败:', err);
        // 即使 API 失败也继续执行注销流程
      }

      // 执行本地注销
      userStore.logout();

      // 直接跳转到登录页面
      router.push('/login');
    } catch (err) {
      // 用户取消操作
      return;
    }
  };

// 删除用户账户
const deleteUserAccount = async () => {
    if (deleteConfirmation.value !== userInfo.value.username) {
      ElNotification({
        title: '⚠️ 提示',
        message: '用户名输入不正确',
        type: 'warning',
        duration: 2000,
        offset: 80
      });
      return;
    }

    try {
      await ElMessageBox.confirm(
          '最后一次确认：确定要永久删除您的账户吗？此操作无法撤销！',
          '⚠️ 危险操作',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'error'
          }
      );

      try {
        const result = await healthApi.deleteUser(userInfo.value.username);
        if (result.success) {
          ElNotification({
            title: '✅ 已删除',
            message: '账户删除成功，感谢您曾经的使用！',
            type: 'success',
            duration: 3000,
            offset: 80
          });
          userStore.logout();
          router.push('/login');
        } else {
          ElNotification({
            title: '❌ 删除失败',
            message: result.message || '账户删除失败',
            type: 'error',
            duration: 3000,
            offset: 80
          });
        }
      } catch (err) {
        ElNotification({
          title: '❌ 删除失败',
          message: '账户删除失败：' + (err.message || '网络错误'),
          type: 'error',
          duration: 3000,
          offset: 80
        });
      }
    } catch (err) {
      // 用户取消操作
      return;
    }
  };

// 关闭所有模态框
const closeModals = () => {
    showUsernameModal.value = false;
    showPasswordModal.value = false;
    showDeleteModal.value = false;
    clearFormFields();
  };

// 清空表单字段
const clearFormFields = () => {
    newUsername.value = '';
    deleteConfirmation.value = '';
  };

// 清空密码字段
const clearPasswordFields = () => {
    oldPassword.value = '';
    newPassword.value = '';
    confirmPassword.value = '';
  };

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch (e) {
    return dateString;
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

.dashboard-layout {
  max-width: 100%;
  margin: 0 20px;
  padding: 0;
  background: #ffffff;
  min-height: 100vh;
  position: relative;
}




.page-header {
  text-align: center;
  padding: 48px 24px 36px;
  position: relative;
  z-index: 1;
  background: var(--color-background);
}

.page-header h1 {
  margin-bottom: 12px;
  font-weight: 700;
  letter-spacing: -0.5px;
  font-size: 36px;
  line-height: 1.1;
}

.stats-bar {
  display: flex;
  justify-content: center;
  gap: 10px;
  align-items: center;
  font-size: 15px;
  color: var(--color-muted-foreground);
  flex-wrap: wrap;
  margin-top: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  letter-spacing: -0.2px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 0;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: none;
  -webkit-transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: none;
  transform: translateY(-2px);
}

/* 新的三行布局样式 */
.three-row-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 0 40px 48px;
  position: relative;
  z-index: 1;
  max-width: calc(100% - 80px);
  margin: 0 auto;
}

.row {
  width: 100%;
}

.first-row .card,
.second-row .card,
.third-row .card {
  margin-bottom: 0;
  width: 100%;
}

/* 原有卡片样式保持不变 */
.card {
  background: #ffffff;
  border-radius: 0;
  padding: 24px;
  margin-bottom: 0;
  border: none;
  box-shadow: none;
  transition: all 0.5s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
  overflow: hidden;
}

.card::before {
  display: none;
}

.card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: none;
}

.card h2 {
  color: var(--color-foreground);
  margin-bottom: 20px;
  padding-bottom: 0;
  border-bottom: none;
  font-weight: 600;
  letter-spacing: -0.3px;
  font-size: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card h2::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 24px;
  background: var(--color-foreground);
  border-radius: 2px;
}

.section-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 24px 0;
}

.subsection-title {
  color: var(--color-foreground);
  font-size: 18px;
  margin-bottom: 16px;
  font-weight: 600;
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.danger-title {
  color: #000000;
}

.security-section-inline,
.danger-zone-inline {
  padding-top: 8px;
}

.security-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.danger-section-inline {
  padding: 20px;
  background: var(--color-muted);
  border-radius: 0;
  border: 1px solid rgba(255, 59, 48, 0.15);
}

.user-info-section {
  display: grid;
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--color-muted);
  border-radius: 0;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
  position: relative;
  overflow: hidden;
}

.info-item:hover {
  background: #ebebf0;
  transform: translateY(-3px);
  box-shadow: none;
}

.info-item label {
  font-weight: 600;
  color: var(--color-foreground);
  font-size: 17px;
  letter-spacing: -0.3px;
}

.info-display {
  display: flex;
  align-items: center;
  gap: 16px;
}

.edit-btn {
  padding: 8px 16px;
  background: var(--color-foreground);
  color: white;
  border: none;
  border-radius: 0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  box-shadow: none;
}

.edit-btn:hover {
  background: #0077ed;
  transform: scale(1.02);
  box-shadow: none;
}

.profile-form {
  width: 100%;
}

.form-section {
  margin-bottom: 32px;
  padding: 24px;
  background: var(--color-muted);
  border-radius: 0;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
  position: relative;
  overflow: hidden;
}

.form-section:hover {
  background: #ebebf0;
  transform: translateY(-2px);
  box-shadow: none;
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  color: var(--color-foreground);
  font-size: 21px;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  font-weight: 600;
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--color-foreground);
  font-size: 15px;
  letter-spacing: -0.1px;
}

.form-input, .form-select, .form-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 0;
  font-size: 17px;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  box-sizing: border-box;
  background: #ffffff;
  color: var(--color-foreground);
  font-family: inherit;
}

.form-input:focus, .form-select:focus, .form-textarea:focus {
  outline: none;
  border-color: var(--color-foreground);
  box-shadow: none;
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386868b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 16px center;
  background-size: 16px;
  padding-right: 48px;
  cursor: pointer;
}

.form-textarea {
  min-height: 100px;
  resize: vertical;
  line-height: 1.5;
}

.help-text {
  margin-top: 8px;
  color: var(--color-muted-foreground);
  font-size: 13px;
  font-weight: 400;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 60px 24px;
  color: var(--color-muted-foreground);
  font-size: 17px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  background: var(--color-muted);
  border-radius: 0;
}

.nutrition-goals {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.goal-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--color-muted);
  border-radius: 0;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  border: none;
  position: relative;
  overflow: hidden;
}

.goal-item:hover {
  background: #ebebf0;
  transform: translateY(-3px);
  box-shadow: none;
}

.goal-label {
  font-weight: 500;
  color: var(--color-foreground);
  font-size: 15px;
}

.goal-value {
  font-weight: 600;
  color: var(--color-foreground);
  font-size: 17px;
}

.security-btn {
  padding: 14px 28px;
  background: var(--color-foreground);
  color: white;
  border: none;
  border-radius: 0;
  cursor: pointer;
  font-size: 17px;
  font-weight: 500;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: none;
  letter-spacing: -0.2px;
}

.security-btn:hover:not(:disabled) {
  background: var(--color-foreground);
  transform: scale(1.02);
  box-shadow: none;
}

.security-btn.secondary {
  background: var(--color-foreground);
  box-shadow: none;
}

.security-btn.secondary:hover:not(:disabled) {
  background: #424245;
  box-shadow: none;
}

.warning-text {
  color: #000000;
  font-weight: 500;
  margin-bottom: 12px;
  line-height: 1.6;
  font-size: 15px;
}

.deletion-list {
  margin: 16px 0;
  padding-left: 24px;
  color: #000000;
}

.deletion-list li {
  margin-bottom: 8px;
  font-size: 15px;
  line-height: 1.5;
}

.danger-btn {
  padding: 14px 28px;
  background: #000000;
  color: white;
  border: none;
  border-radius: 0;
  cursor: pointer;
  font-size: 17px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: none;
}

.danger-btn:hover:not(:disabled) {
  background: #ff2d23;
  transform: scale(1.02);
  box-shadow: none;
}

.form-actions {
  text-align: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.submit-btn {
  padding: 16px 48px;
  background: var(--gradient-brand);
  color: white;
  border: none;
  border-radius: 0;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 0 auto;
  box-shadow: none;
  letter-spacing: -0.2px;
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-foreground);
  transform: scale(1.02);
  box-shadow: none;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: #ffffff;
  border-radius: 0;
  width: 90%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: none;
  animation: modalSlideIn 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.modal-content.danger-modal {
  border: 1px solid rgba(255, 59, 48, 0.3);
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.modal-header h3 {
  margin: 0;
  color: var(--color-foreground);
  font-size: 21px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.close-btn {
  background: var(--color-muted);
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--color-muted-foreground);
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.close-btn:hover {
  background: #e8e8ed;
  color: var(--color-foreground);
}

.modal-body {
  padding: 24px 28px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.cancel-btn {
  padding: 12px 24px;
  background: var(--color-muted);
  color: var(--color-foreground);
  border: none;
  border-radius: 0;
  cursor: pointer;
  font-size: 17px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.cancel-btn:hover {
  background: #e8e8ed;
}

.confirm-btn {
  padding: 12px 24px;
  background: var(--color-foreground);
  color: white;
  border: none;
  border-radius: 0;
  cursor: pointer;
  font-size: 17px;
  font-weight: 500;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  box-shadow: none;
}

.confirm-btn:hover:not(:disabled) {
  background: var(--color-foreground);
  transform: scale(1.02);
  box-shadow: none;
}

.confirm-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.danger-confirm-btn {
  padding: 12px 24px;
  background: #000000;
  color: white;
  border: none;
  border-radius: 0;
  cursor: pointer;
  font-size: 17px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.danger-confirm-btn:hover:not(:disabled) {
  background: #ff2d23;
}

.danger-confirm-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .dashboard-layout {
    padding: 0;
  }
  
  .page-header {
    padding: 48px 20px 40px;
  }
  
  .page-header h1 {
    font-size: 32px;
  }
  
  .stats-bar {
    flex-direction: column;
    gap: 8px;
  }
  
  .stat-item {
    width: 100%;
    justify-content: center;
  }
  
  .main-container {
    padding: 0 20px 48px;
    grid-template-columns: 1fr;
  }
  
  .card {
    padding: 20px;
  }
  
  .card h2 {
    font-size: 22px;
  }
  
  .form-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .form-section {
    padding: 20px;
  }
  
  .section-title {
    font-size: 19px;
  }
  
  .security-section {
    flex-direction: column;
  }
  
  .security-btn {
    width: 100%;
    justify-content: center;
  }
  
  .modal-content {
    width: 95%;
    margin: 20px;
    border-radius: 0;
  }
  
  .modal-header {
    padding: 20px 20px 16px;
  }
  
  .modal-body {
    padding: 20px;
  }
  
  .modal-actions {
    flex-direction: column;
  }
  
  .cancel-btn, .confirm-btn, .danger-confirm-btn {
    width: 100%;
    justify-content: center;
  }
  
  .nutrition-goals {
    grid-template-columns: 1fr;
  }
  
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .page-header {
    padding: 40px 16px 32px;
  }
  
  .page-header h1 {
    font-size: 28px;
  }
  
  .profile-card {
    padding: 20px;
  }
  
  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
  }
  
  .submit-btn {
    width: 100%;
    padding: 14px;
  }
  
  .form-input, .form-select, .form-textarea {
    font-size: 16px;
    padding: 12px 14px;
  }
}
</style>