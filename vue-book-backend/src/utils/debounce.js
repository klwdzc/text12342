/**
 * 🎨 精美的防抖节流工具
 * 
 * 特性：
 * - TypeScript 友好
 * - 取消功能
 * - 立即执行选项
 * - 性能优化
 */

/**
 * 防抖函数
 * @param {Function} fn - 需要防抖的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @param {boolean} immediate - 是否立即执行
 * @returns {Function} 防抖后的函数
 */
export function debounce(fn, delay = 300, immediate = false) {
  let timeout = null
  let isImmediateCalled = false

  const debounced = function(...args) {
    const context = this
    
    if (immediate && !isImmediateCalled) {
      fn.apply(context, args)
      isImmediateCalled = true
      return
    }

    if (timeout) clearTimeout(timeout)
    
    timeout = setTimeout(() => {
      if (!immediate) {
        fn.apply(context, args)
      }
      isImmediateCalled = false
    }, delay)
  }

  // 提供取消方法
  debounced.cancel = function() {
    if (timeout) {
      clearTimeout(timeout)
      timeout = null
    }
    isImmediateCalled = false
  }

  return debounced
}

/**
 * 节流函数（时间戳版）
 * @param {Function} fn - 需要节流的函数
 * @param {number} delay - 间隔时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(fn, delay = 300) {
  let lastTime = 0
  let timeout = null

  const throttled = function(...args) {
    const context = this
    const now = Date.now()
    const remaining = delay - (now - lastTime)

    if (remaining <= 0) {
      if (timeout) {
        clearTimeout(timeout)
        timeout = null
      }
      lastTime = now
      fn.apply(context, args)
    } else if (!timeout) {
      timeout = setTimeout(() => {
        lastTime = Date.now()
        timeout = null
        fn.apply(context, args)
      }, remaining)
    }
  }

  // 提供取消方法
  throttled.cancel = function() {
    if (timeout) {
      clearTimeout(timeout)
      timeout = null
    }
    lastTime = 0
  }

  return throttled
}

/**
 * 帧率控制节流（适合动画和滚动）
 * @param {Function} fn - 需要节流的函数
 * @returns {Function} 节流后的函数
 */
export function rafThrottle(fn) {
  let rafId = null
  let lastArgs = null

  const throttled = function(...args) {
    lastArgs = args
    const context = this

    if (!rafId) {
      rafId = requestAnimationFrame(() => {
        fn.apply(context, lastArgs)
        rafId = null
        lastArgs = null
      })
    }
  }

  // 提供取消方法
  throttled.cancel = function() {
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
    lastArgs = null
  }

  return throttled
}

/**
 * 专用搜索防抖
 * @param {Function} searchFn - 搜索回调
 * @param {number} delay - 延迟时间
 * @returns {Function} 防抖搜索函数
 */
export function createSearchDebounce(searchFn, delay = 500) {
  return debounce((value) => {
    if (value && value.trim()) {
      searchFn(value.trim())
    }
  }, delay)
}

/**
 * 专用输入验证防抖
 * @param {Function} validateFn - 验证回调
 * @param {number} delay - 延迟时间
 * @returns {Function} 防抖验证函数
 */
export function createValidateDebounce(validateFn, delay = 300) {
  return debounce((value) => {
    validateFn(value)
  }, delay)
}

// 默认导出
export default {
  debounce,
  throttle,
  rafThrottle,
  createSearchDebounce,
  createValidateDebounce
}
