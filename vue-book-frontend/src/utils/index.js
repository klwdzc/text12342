/**
 * 🎨 前端工具库统一导出
 * 
 * 使用示例：
 * import { loading, message, debounce, apiWrapper } from '@/utils'
 */

// Loading 管理器
export { default as loading, useLoading } from './loading'

// 消息提示
export { 
  message, 
  notification, 
  handleError, 
  showMessage, 
  showNotification 
} from './message'

// 防抖节流
export { 
  debounce, 
  throttle, 
  rafThrottle,
  createSearchDebounce,
  createValidateDebounce
} from './debounce'

// API 包装器
export {
  callApi,
  fetchApi,
  fetchWithLoading,
  fetchSilent,
  createApiChain,
  ApiChain
} from './apiWrapper'

// 默认导出所有
export default {
  loading: () => import('./loading'),
  message: () => import('./message'),
  debounce: () => import('./debounce'),
  apiWrapper: () => import('./apiWrapper')
}
