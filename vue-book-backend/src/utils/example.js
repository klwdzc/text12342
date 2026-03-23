/**
 * 🎨 精美工具库使用示例
 * 
 * 此文件展示了如何使用新创建的前端工具库
 * 实际使用时，请参考这些示例优化现有组件
 */

import { ref, onMounted } from 'vue'
import { getBookList } from '@/api/book'

// ========== 示例 1: 使用 Loading 管理器 ==========
import loading from '@/utils/loading'

const example1 = async () => {
  // 基础用法
  loading.show('正在加载图书列表...')
  
  try {
    const data = await getBookList({ pageNum: 1, pageSize: 10 })
    console.log(data)
  } finally {
    loading.hide()
  }
}

// 包装异步操作（推荐）
const example1_2 = async () => {
  const result = await loading.wrap(
    () => getBookList({ pageNum: 1, pageSize: 10 }),
    '正在加载...'
  )
  return result
}


// ========== 示例 2: 使用消息提示 ==========
import { message, notification, handleError } from '@/utils/message'

const example2 = () => {
  // 成功提示
  message.success('添加成功！')
  
  // 错误提示
  message.error('删除失败，请重试')
  
  // 警告提示
  message.warning('还有 3 本书即将逾期')
  
  // 信息提示
  message.info('系统将于今晚维护')
  
  // 通知（右上角）
  notification.success('操作成功', '图书已添加到系统')
  notification.error('导入失败', '共有 3 条记录格式错误')
  
  // 智能错误处理
  try {
    await someApi()
  } catch (error) {
    handleError(error) // 自动识别错误类型并显示相应提示
  }
}


// ========== 示例 3: 使用防抖节流 ==========
import { debounce, throttle, createSearchDebounce } from '@/utils/debounce'

// 搜索防抖（推荐）
const handleSearch = (keyword) => {
  console.log('搜索:', keyword)
}

const debouncedSearch = createSearchDebounce(handleSearch, 500)
// <el-input @input="debouncedSearch" />

// 自定义防抖
const handleClick = debounce(() => {
  console.log('按钮被点击')
}, 300)

// 节流（适合滚动）
const handleScroll = throttle(() => {
  console.log('滚动事件')
}, 200)


// ========== 示例 4: 使用 API 包装器 ==========
import { callApi, fetchWithLoading, createApiChain } from '@/utils/apiWrapper'

// 方式 1: callApi
const example4_1 = async () => {
  const response = await callApi(
    () => getBookList({ pageNum: 1, pageSize: 10 }),
    {
      showLoading: true,
      showError: true,
      showSuccess: false,
      loadingText: '正在加载图书...',
      successMessage: '加载成功',
      errorMessage: '加载失败'
    }
  )
  return response
}

// 方式 2: fetchWithLoading（简化版）
const example4_2 = async () => {
  const response = await fetchWithLoading(
    () => getBookList({ pageNum: 1, pageSize: 10 }),
    '加载中...'
  )
  return response
}

// 方式 3: 链式调用（最优雅）
const example4_3 = async () => {
  const response = await createApiChain(getBookList({ pageNum: 1, pageSize: 10 }))
    .withLoading('正在加载图书列表...')
    .autoError()
    .withSuccessTip('加载成功')
    .execute()
  return response
}


// ========== 示例 5: 完整的列表页面实现 ==========
const setupBookListPage = () => {
  const tableData = ref([])
  const currentPage = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const searchForm = ref({ condition: 'bookName', content: '' })
  
  // 加载列表（使用 Loading 包装）
  const loadList = async () => {
    await loading.wrap(async () => {
      const res = await getBookList({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        condition: searchForm.value.condition,
        query: searchForm.value.content
      })
      
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
      
      if (tableData.value.length === 0) {
        message.warning('暂无数据')
      }
    }, '正在加载图书列表...')
  }
  
  // 搜索（带防抖）
  const debouncedSearch = createSearchDebounce(() => {
    currentPage.value = 1
    loadList()
  }, 500)
  
  // 分页变化
  const handleSizeChange = () => loadList()
  const handleCurrentChange = () => loadList()
  
  // 初始化
  onMounted(() => {
    loadList()
  })
  
  return {
    tableData,
    currentPage,
    pageSize,
    total,
    searchForm,
    debouncedSearch,
    handleSizeChange,
    handleCurrentChange
  }
}


// ========== 示例 6: 统一的导入方式 ==========
import { loading, message, debounce, callApi } from '@/utils'

const allInOne = async () => {
  // Loading
  loading.show('加载中...')
  
  try {
    // API 调用
    const result = await callApi(
      () => getBookList({ pageNum: 1 }),
      { showLoading: false, showError: true }
    )
    
    // 消息提示
    message.success('加载成功')
    
    // 防抖搜索
    const searchFn = debounce(console.log, 300)
    searchFn('test')
    
  } catch (error) {
    // 错误处理
    message.error('操作失败')
  } finally {
    loading.hide()
  }
}


// ========== 导出示例 ==========
export {
  example1,
  example1_2,
  example2,
  example4_1,
  example4_2,
  example4_3,
  setupBookListPage,
  allInOne
}

export default {
  description: '精美工具库使用示例',
  features: [
    '✨ 优雅的 Loading 管理',
    '🎨 精美的消息提示',
    '⚡ 防抖节流优化',
    '🔗 链式 API 调用',
    '🛡️ 智能错误处理'
  ]
}
