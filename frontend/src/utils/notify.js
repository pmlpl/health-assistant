/**
 * 统一通知 — 顶部中心滑入
 */
import { ElNotification, ElMessage } from 'element-plus';

const base = {
  position: 'top-right',
  duration: 4000,
  offset: 72,
  customClass: '',
};

function show(type, title, message) {
  try {
    ElNotification({
      ...base,
      type,
      title: title || defaultTitle(type),
      message: message || '',
      customClass: `notify-${type} notify-top-center`,
    });
  } catch (err) {
    console.warn('通知展示失败:', err);
  }
}

function defaultTitle(type) {
  const map = {
    success: '成功',
    warning: '请注意',
    error: '出错了',
    info: '提示',
  };
  return map[type] || '提示';
}

export function notifySuccess(message, title) {
  show('success', title, message);
}

export function notifyWarning(message, title) {
  show('warning', title, message);
}

export function notifyError(message, title) {
  show('error', title, message);
}

export function notifyInfo(message, title) {
  show('info', title, message);
}

/** 从 axios 错误对象提取可读文案 */
export function messageFromError(error, fallback = '操作失败，请稍后重试') {
  if (!error) return fallback;
  const data = error.response?.data;
  if (typeof data === 'string' && data) return data;
  if (data?.message) return data.message;
  if (error.message && !error.message.startsWith('Request failed')) return error.message;
  const status = error.response?.status;
  if (status === 401) return '用户名或密码错误';
  if (status === 428) return data?.message || '请先在 AI 设置中配置模型与 API Key';
  if (status === 403) return '没有权限执行此操作';
  if (status === 404) return '请求的资源不存在';
  if (status >= 500) return '服务器繁忙，请稍后重试';
  if (error.code === 'ECONNABORTED') return '请求超时，请检查网络或稍后重试';
  return fallback;
}

export function toastError(message) {
  ElMessage({
    type: 'error',
    message,
    duration: 3500,
    showClose: true,
    grouping: true,
    offset: 72,
  });
}
