// src/services/messageService.js
import { createApp } from 'vue'
import MessageBox from '../components/MessageBox.vue'

class MessageService {
  constructor() {
    this.container = null
    this.instances = []
  }

  // 创建消息容器
  createContainer() {
    if (!this.container) {
      this.container = document.createElement('div')
      this.container.id = 'message-container'
      document.body.appendChild(this.container)
      console.log('消息容器已创建');
    }
  }

  // 显示消息
  showMessage(options) {
    console.log('显示消息:', options);
    this.createContainer()
    
    const app = createApp(MessageBox, {
      ...options,
      modelValue: true,
      'onUpdate:modelValue': (value) => {
        if (!value) {
          console.log('消息框关闭');
          this.destroyInstance(app)
          if (options.onClose) options.onClose()
        }
      },
      onAction: (action) => {
        console.log('消息框操作:', action);
        if (options.onAction) options.onAction(action)
      }
    })

    const instance = app.mount(document.createElement('div'))
    this.container.appendChild(instance.$el)
    this.instances.push({ app, instance })
    console.log('消息实例已创建');

    return instance
  }

  // 销毁实例
  destroyInstance(appInstance) {
    const index = this.instances.findIndex(item => item.app === appInstance)
    if (index > -1) {
      const { app, instance } = this.instances[index]
      app.unmount()
      instance.$el.remove()
      this.instances.splice(index, 1)
    }
  }

  // 信息提示
  info(message, options = {}) {
    return this.showMessage({
      title: '提示',
      message,
      type: 'info',
      ...options
    })
  }

  // 成功提示
  success(message, options = {}) {
    console.log('显示成功消息:', message);
    return this.showMessage({
      title: '成功',
      message,
      type: 'success',
      ...options
    })
  }

  // 警告提示
  warning(message, options = {}) {
    console.log('显示警告消息:', message);
    return this.showMessage({
      title: '警告',
      message,
      type: 'warning',
      ...options
    })
  }

  // 错误提示
  error(message, options = {}) {
    console.log('显示错误消息:', message);
    return this.showMessage({
      title: '错误',
      message,
      type: 'error',
      ...options
    })
  }

  // 确认对话框
  confirm(message, title = '确认') {
    console.log('显示确认对话框:', message, title);
    return new Promise((resolve) => {
      this.showMessage({
        title: title,
        message: message,
        type: 'warning',
        showActions: true,
        actions: [
          { id: 'cancel', text: '取消', type: 'secondary' },
          { id: 'confirm', text: '确定', type: 'primary' }
        ],
        onAction: (action) => {
          console.log('确认对话框操作:', action);
          resolve(action.id === 'confirm')
        },
        onClose: () => {
          console.log('确认对话框关闭');
          resolve(false)
        }
      })
    })
  }

  // 带输入的提示框
  prompt(message, defaultValue = '', options = {}) {
    return new Promise((resolve) => {
      const inputHtml = `
        <div style="margin-bottom: 15px;">
          <p style="margin: 0 0 10px 0;">${message}</p>
          <input 
            id="prompt-input" 
            type="text" 
            value="${defaultValue}"
            style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;"
            placeholder="请输入内容"
          />
        </div>
      `

      this.showMessage({
        title: '输入',
        message: inputHtml,
        type: 'info',
        showActions: true,
        dangerouslyUseHTMLString: true, // 允许渲染 HTML
        actions: [
          { id: 'cancel', text: '取消', type: 'secondary' },
          { id: 'confirm', text: '确定', type: 'primary' }
        ],
        onAction: (action) => {
          if (action.id === 'confirm') {
            const input = document.getElementById('prompt-input')
            resolve(input ? input.value : null)
          } else {
            resolve(null)
          }
        },
        ...options
      })

      // 自动聚焦输入框
      setTimeout(() => {
        const input = document.getElementById('prompt-input')
        if (input) input.focus()
      }, 100)
    })
  }

  // 清除所有消息
  clearAll() {
    this.instances.forEach(({ app }) => {
      this.destroyInstance(app)
    })
    this.instances = []
  }
}

// 创建全局实例
const messageService = new MessageService()

// 导出服务
export default messageService

// 同时导出便捷方法
export const { info, success, warning, error, confirm, prompt, clearAll } = messageService