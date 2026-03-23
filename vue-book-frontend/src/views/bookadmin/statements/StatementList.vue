<template>
  <div class="statement-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="借书报表" name="borrow"></el-tab-pane>
      <el-tab-pane label="还书报表" name="return"></el-tab-pane>
    </el-tabs>

    <!-- 借书报表 -->
    <el-card v-if="activeTab === 'borrow'" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>借书记录</span>
        </div>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="搜索内容">
          <el-input v-model="queryForm.content" placeholder="请输入借阅证号或书名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadBorrowStatement">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="borrowTableData" border stripe v-loading="loading">
        <el-table-column prop="cardNumber" label="借阅证号" />
        <el-table-column prop="cardName" label="读者姓名" />
        <el-table-column prop="bookName" label="图书名称" />
        <el-table-column prop="author" label="作者" />
        <el-table-column prop="borrowDate" label="借阅日期" />
        <el-table-column prop="shouldReturnDate" label="应还日期" />
        <el-table-column prop="remark" label="备注" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadBorrowStatement"
        @current-change="loadBorrowStatement"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 还书报表 -->
    <el-card v-if="activeTab === 'return'" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>还书记录</span>
        </div>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="搜索内容">
          <el-input v-model="queryForm.content" placeholder="请输入图书编号或书名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadReturnStatement">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="returnTableData" border stripe v-loading="loading">
        <el-table-column prop="bookNumber" label="图书编号" />
        <el-table-column prop="bookName" label="图书名称" />
        <el-table-column prop="cardNumber" label="借阅证号" />
        <el-table-column prop="cardName" label="读者姓名" />
        <el-table-column prop="borrowDate" label="借阅日期" />
        <el-table-column prop="actualReturnDate" label="归还日期" />
        <el-table-column prop="isOverdue" label="是否逾期">
          <template #default="{ row }">
            <el-tag :type="row.isOverdue ? 'danger' : 'success'">
              {{ row.isOverdue ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadReturnStatement"
        @current-change="loadReturnStatement"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getBorrowStatement, getReturnStatement } from '@/api/bookadmin'

const activeTab = ref('borrow')
const loading = ref(false)

const queryForm = reactive({
  content: '',
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
})

const borrowTableData = ref([])
const returnTableData = ref([])

// 加载借书报表
const loadBorrowStatement = async () => {
  loading.value = true
  try {
    const res = await getBorrowStatement({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      query: queryForm.content,
      condition: 'cardNumber',
    })

    if (res.code === 200 || res.status === 200) {
      borrowTableData.value = res.data?.records || []
      pagination.total = res.data?.total || 0
    } else {
      // 没有数据时清空表格
      borrowTableData.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('加载借书报表失败:', error)
    borrowTableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}


// 加载还书报表
const loadReturnStatement = async () => {
  loading.value = true
  try {
    const res = await getReturnStatement({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      query: queryForm.content,
      condition: 'bookNumber',
    })

    if (res.code === 200 || res.status === 200) {
      returnTableData.value = res.data?.records || []
      pagination.total = res.data?.total || 0
    } else {
      // 没有数据时清空表格
      returnTableData.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('加载还书报表失败:', error)
    returnTableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}


// 切换标签页
const handleTabChange = () => {
  pagination.currentPage = 1
  pagination.pageSize = 10
  queryForm.content = ''
  
  if (activeTab.value === 'borrow') {
    loadBorrowStatement()
  } else {
    loadReturnStatement()
  }
}

onMounted(() => {
  loadBorrowStatement()
})
</script>

<style scoped>
.statement-container {
  padding: 20px;
}

.card-header {
  font-weight: bold;
}
</style>
