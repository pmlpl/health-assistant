<template>
  <div class="page-header">
    <div class="header-content">
      <span class="header-icon">{{ icon }}</span>
      <div class="header-text">
        <h1 class="header-title">{{ title }}</h1>
        <p class="header-subtitle" v-if="subtitle">{{ subtitle }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  }
});
</script>

<style scoped>
.page-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.header-icon {
  font-size: 36px;
  margin-right: 15px;
}

.header-text {
  flex: 1;
}

.header-title {
  margin: 0 0 5px 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.header-subtitle {
  margin: 0;
  font-size: 14px;
  color: #666;
}
</style>
