import loading from './loading'
import { handleError, showMessage } from './message'

/**
 * 🎨 精美的 API 响应包装器
 * 
 * 特性：
 * - 自动 Loading 管理
 * - 统一错误处理
 * - 链式调用
 * - 优雅的成功提示
 */

/**
 * API 调用包装器
 * @param {Function} apiFn - API 函数
 * @param {object} options - 配置选项
 * @returns Promise
 */
export function callApi(apiFn, options = {}) {
  const {
    showLoading = true,
    showError = true,
    showSuccess = false,
    loadingText = '加载中...',
    successMessage = '操作成功',
    errorMessage = '操作失败'
  } = options

  return new Promise((resolve, reject) => {
    // 显示 Loading
    if (showLoading) {
      loading.show(loadingText)
    }

    // 执行 API 调用
    apiFn()
      .then(response => {
        // 隐藏 Loading
        if (showLoading) {
          loading.hide()
        }

        // 检查响应状态
        if (response.code === 200 || response.status === 200) {
          // 显示成功提示
          if (showSuccess) {
            showMessage.success(successMessage)
          }
          resolve(response)
        } else {
          // 业务错误
          if (showError) {
            showMessage.error(response.msg || errorMessage)
          }
          reject(new Error(response.msg || errorMessage))
        }
      })
      .catch(error => {
        // 隐藏 Loading
        if (showLoading) {
          loading.hide()
        }

        // 显示错误
        if (showError) {
          handleError(error)
        }
        reject(error)
      })
  })
}

/**
 * 简化的 API 调用（无 Loading）
 */
export function fetchApi(apiFn) {
  return apiFn()
}

/**
 * 带 Loading 的 API 调用
 */
export function fetchWithLoading(apiFn, text = '加载中...') {
  return callApi(apiFn, { showLoading: true, loadingText: text })
}

/**
 * 静默 API 调用（无提示）
 */
export function fetchSilent(apiFn) {
  return callApi(apiFn, { showLoading: false, showError: false })
}

/**
 * 构建链式调用
 */
export class ApiChain {
  constructor(apiFn) {
    this.apiFn = apiFn
    this.options = {
      showLoading: false,
      showError: false,
      showSuccess: false
    }
  }

  withLoading(text = '加载中...') {
    this.options.showLoading = true
    this.options.loadingText = text
    return this
  }

  withErrorTip(message) {
    this.options.showError = true
    this.options.errorMessage = message
    return this
  }

  withSuccessTip(message) {
    this.options.showSuccess = true
    this.options.successMessage = message
    return this
  }

  autoError() {
    this.options.showError = true
    return this
  }

  execute() {
    return callApi(this.apiFn, this.options)
  }
}

/**
 * 创建 API 链
 */
export function createApiChain(apiFn) {
  return new ApiChain(apiFn)
}

// 默认导出
export default {
  callApi,
  fetchApi,
  fetchWithLoading,
  fetchSilent,
  createApiChain,
  ApiChain
}
