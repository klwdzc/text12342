import { ElLoading } from 'element-plus'

/**
 * 🎨 精美的 Loading 管理器
 * 
 * 特性：
 * - 智能计数器，支持嵌套调用
 * - 自定义样式和文案
 * - 自动防止内存泄漏
 * - 优雅的过渡动画
 */

class LoadingManager {
  constructor() {
    this.instance = null
    this.counter = 0
    this.config = {
      lock: true,
      background: 'rgba(255, 255, 255, 0.92)',
      customClass: 'elegant-loading',
      timeout: null
    }
  }

  /**
   * 显示 Loading
   * @param {string} text - 提示文字
   * @param {object} options - 自定义配置
   */
  show(text = '加载中...', options = {}) {
    // 如果已存在，增加计数
    if (this.instance) {
      this.counter++
      // 更新文字
      if (this.instance.setText) {
        this.instance.setText(text)
      }
      return
    }

    // 合并配置
    const finalConfig = {
      ...this.config,
      text,
      ...options
    }

    // 创建实例
    this.instance = ElLoading.service(finalConfig)
    this.counter = 1

    // 设置超时保护（30 秒自动关闭）
    if (finalConfig.timeout !== false) {
      const timeoutMs = typeof finalConfig.timeout === 'number' ? finalConfig.timeout : 30000
      this.config.timeout = setTimeout(() => {
        console.warn('Loading 超时，自动关闭')
        this.hide()
      }, timeoutMs)
    }
  }

  /**
   * 隐藏 Loading
   */
  hide() {
    if (!this.instance || this.counter <= 0) {
      return
    }

    this.counter--
    
    // 计数器归零时关闭
    if (this.counter <= 0) {
      // 清除超时定时器
      if (this.config.timeout) {
        clearTimeout(this.config.timeout)
        this.config.timeout = null
      }

      // 关闭实例
      this.instance.close()
      this.instance = null
      this.counter = 0
    }
  }

  /**
   * 强制关闭（忽略计数器）
   */
  forceClose() {
    if (this.config.timeout) {
      clearTimeout(this.config.timeout)
      this.config.timeout = null
    }

    if (this.instance) {
      this.instance.close()
      this.instance = null
    }
    this.counter = 0
  }

  /**
   * 更新提示文字
   */
  updateText(text) {
    if (this.instance && this.instance.setText) {
      this.instance.setText(text)
    }
  }

  /**
   * 包裹异步操作（推荐用法）
   * @param {Function} fn - 异步函数
   * @param {string} text - Loading 文字
   * @returns Promise
   */
  async wrap(fn, text = '加载中...') {
    try {
      this.show(text)
      return await fn()
    } catch (error) {
      console.error('Loading 期间发生错误:', error)
      throw error
    } finally {
      this.hide()
    }
  }
}

// 导出单例
const loading = new LoadingManager()

// 导出 Composition API Hook
export function useLoading() {
  const isLoading = ref(false)

  const withLoading = async (fn, text = '加载中...') => {
    isLoading.value = true
    try {
      return await loading.wrap(fn, text)
    } finally {
      isLoading.value = false
    }
  }

  return {
    isLoading,
    withLoading,
    loading
  }
}

export default loading
