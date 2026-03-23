<template>
  <div class="search-books-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图书查询</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="searchForm" :inline="true" size="default">
        <el-form-item label="搜索条件">
          <el-select v-model="searchForm.condition" style="width: 120px">
            <el-option label="书名" value="bookName" />
            <el-option label="作者" value="bookAuthor" />
            <el-option label="分类" value="bookType" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchForm.content" placeholder="请输入搜索内容" clearable style="width: 300px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="bookNumber" label="图书编号" width="120" />
        <el-table-column prop="bookName" label="书名" min-width="150" />
        <el-table-column prop="bookAuthor" label="作者" min-width="120" />
        <el-table-column prop="bookType" label="分类" width="120" />
        <el-table-column prop="bookLibrary" label="图书馆" width="120" />
        <el-table-column prop="bookLocation" label="位置" width="100" />
        <el-table-column prop="bookStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.bookStatus === '在馆' ? 'success' : 'warning'">
              {{ row.bookStatus || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBookList } from '@/api/book'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  condition: 'bookName',
  content: '',
})

// 加载图书列表
const loadBookList = async () => {
  loading.value = true
  try {
    const res = await getBookList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      condition: searchForm.condition,
      query: searchForm.content,
    })
    
    if (res.code === 200 || res.status === 200) {
      tableData.value = res.data.records || []
      total.value = Number(res.data.total) || 0
    } else {
      // 没有数据时不显示错误
      tableData.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('加载图书列表失败:', error)
    // 只在真正出错时显示提示
    ElMessage.error('加载图书列表失败')
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadBookList()
}

// 重置
const handleReset = () => {
  searchForm.content = ''
  searchForm.condition = 'bookName'
  handleSearch()
}

// 分页变化
const handleSizeChange = () => {
  loadBookList()
}

const handleCurrentChange = () => {
  loadBookList()
}

onMounted(() => {
  loadBookList()
})
</script>

<style scoped>
.search-books-container {
  padding: 20px;
}

.card-header {
  font-weight: bold;
}
</style>
