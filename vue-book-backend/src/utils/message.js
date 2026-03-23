import { ElMessage, ElNotification } from 'element-plus'

/**
 * 🎨 精美的消息提示管理器
 * 
 * 特性：
 * - 智能分类提示
 * - 优雅的动画效果
 * - 防重复显示
 * - 自动队列管理
 */

class MessageManager {
  constructor() {
    this.instances = new Map()
    this.counter = 0
    this.maxVisible = 3 // 最多同时显示 3 个
  }

  /**
   * 显示消息
   */
  show(message, type = 'info', options = {}) {
    // 如果消息过多，不显示新的
    if (this.instances.size >= this.maxVisible) {
      return null
    }

    const defaultOptions = {
      message,
      type,
      duration: options.duration || 3000,
      showClose: true,
      offset: 60 + (this.instances.size * 60),
      customClass: 'elegant-message'
    }

    const instance = ElMessage(defaultOptions)
    const id = `msg_${Date.now()}_${this.counter++}`
    
    this.instances.set(id, instance)

    // 自动移除
    setTimeout(() => this.remove(id), defaultOptions.duration)

    return id
  }

  /**
   * 移除消息
   */
  remove(id) {
    const instance = this.instances.get(id)
    if (instance) {
      instance.close()
      this.instances.delete(id)
    }
  }

  /**
   * 清空所有消息
   */
  clearAll() {
    this.instances.forEach(instance => instance.close())
    this.instances.clear()
  }

  // ========== 快捷方法 ==========
  
  success(message, options) {
    return this.show(message, 'success', options)
  }

  error(message, options) {
    return this.show(message, 'error', options)
  }

  warning(message, options) {
    return this.show(message, 'warning', options)
  }

  info(message, options) {
    return this.show(message, 'info', options)
  }
}

/**
 * 通知管理器（右上角）
 */
class NotificationManager {
  constructor() {
    this.instances = []
  }

  /**
   * 显示通知
   */
  show(options) {
    const defaultOptions = {
      title: '提示',
      message: '',
      type: 'info',
      duration: 4500,
      position: 'top-right',
      showClose: true,
      offset: 20,
      customClass: 'elegant-notification'
    }

    const finalOptions = { ...defaultOptions, ...options }
    return ElNotification(finalOptions)
  }

  /**
   * 成功通知
   */
  success(title, message = '') {
    return this.show({ title, message, type: 'success' })
  }

  /**
   * 错误通知
   */
  error(title, message = '') {
    return this.show({ title, message, type: 'error' })
  }

  /**
   * 警告通知
   */
  warning(title, message = '') {
    return this.show({ title, message, type: 'warning' })
  }

  /**
   * 信息通知
   */
  info(title, message = '') {
    return this.show({ title, message, type: 'info' })
  }
}

// 导出单例
export const message = new MessageManager()
export const notification = new NotificationManager()

/**
 * 智能错误处理
 */
export function handleError(error) {
  // 网络错误
  if (!navigator.onLine) {
    notification.error('网络连接失败', '请检查您的网络连接')
    return
  }

  // HTTP 错误
  if (error.response) {
    const status = error.response.status
    const errorMap = {
      401: ['登录已过期', '请重新登录'],
      403: ['拒绝访问', '您没有权限执行此操作'],
      404: ['资源不存在', '请求的资源找不到'],
      500: ['服务器错误', '请稍后重试']
    }

    const [title, msg] = errorMap[status] || ['请求失败', `错误代码：${status}`]
    notification.error(title, msg)
    return
  }

  // 超时错误
  if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
    notification.error('请求超时', '网络响应时间过长，请重试')
    return
  }

  // 业务错误
  if (error.message || error.msg) {
    message.error(error.message || error.msg)
    return
  }

  // 默认错误
  message.error('操作失败，请稍后重试')
}

/**
 * 快捷方法
 */
export const showMessage = {
  success: (msg, opt) => message.success(msg, opt),
  error: (msg, opt) => message.error(msg, opt),
  warning: (msg, opt) => message.warning(msg, opt),
  info: (msg, opt) => message.info(msg, opt)
}

export const showNotification = {
  success: (title, msg) => notification.success(title, msg),
  error: (title, msg) => notification.error(title, msg),
  warning: (title, msg) => notification.warning(title, msg),
  info: (title, msg) => notification.info(title, msg)
}

export default {
  message,
  notification,
  handleError,
  showMessage,
  showNotification
}
