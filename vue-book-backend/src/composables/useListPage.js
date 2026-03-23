import { ref, reactive } from 'vue'
import { createSearchDebounce } from './debounce'

/**
 * 🎨 列表页面通用组合式函数
 * 
 * 特性：
 * - 统一的搜索防抖
 * - 统一的分页逻辑
 * - 统一的 Loading 管理
 */

export function useListPage(loadDataFn, options = {}) {
  const {
    debounceDelay = 500,
    defaultPageSize = 10,
    pageSizes = [10, 20, 50, 100]
  } = options

  // 分页状态
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)

  // 搜索表单
  const searchForm = reactive({
    condition: 'name',
    content: ''
  })

  // 表格数据
  const tableData = ref([])
  const selectedRows = ref([])

  // 加载数据（由外部实现）
  const loadData = async () => {
    if (loadDataFn) {
      const result = await loadDataFn({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        condition: searchForm.condition,
        query: searchForm.content
      })
      
      if (result) {
        tableData.value = result.records || []
        total.value = result.total || 0
      }
    }
  }

  // 带防抖的搜索
  const debouncedSearch = createSearchDebounce(() => {
    currentPage.value = 1
    loadData()
  }, debounceDelay)

  // 搜索处理
  const handleSearch = () => {
    debouncedSearch()
  }

  // 重置搜索
  const handleReset = () => {
    searchForm.condition = 'name'
    searchForm.content = ''
    currentPage.value = 1
    loadData()
  }

  // 分页变化
  const handleSizeChange = () => {
    loadData()
  }

  const handleCurrentChange = () => {
    loadData()
  }

  // 选择变化
  const handleSelectionChange = (selection) => {
    selectedRows.value = selection
  }

  return {
    // 状态
    currentPage,
    pageSize,
    total,
    tableData,
    selectedRows,
    searchForm,
    
    // 方法
    loadData,
    handleSearch,
    handleReset,
    handleSizeChange,
    handleCurrentChange,
    handleSelectionChange
  }
}

export default useListPage
