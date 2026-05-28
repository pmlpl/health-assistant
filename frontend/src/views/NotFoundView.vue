<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在或已被移除</p>
    <router-link to="/" class="home-link">返回首页</router-link>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #606266;
}
h1 {
  font-size: 72px;
  margin: 0;
  color: #409eff;
}
.home-link {
  margin-top: 8px;
  color: #409eff;
  text-decoration: none;
}
</style>
