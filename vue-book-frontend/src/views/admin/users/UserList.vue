<template>
  <div class="user-list-container">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :model="searchForm" :inline="true" size="default">
        <el-form-item label="搜索条件">
          <el-select v-model="searchForm.condition" style="width: 120px">
            <el-option label="用户名" value="username" />
            <el-option label="姓名" value="cardName" />
            <el-option label="借阅证号" value="cardNumber" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchForm.content" placeholder="请输入搜索内容" clearable style="width: 200px" @keyup.enter="handleSearch" />
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

      <!-- 操作按钮 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加借阅证
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="userId" label="用户 ID" width="80" />
        <el-table-column prop="cardName" label="姓名" min-width="100" />
        <el-table-column prop="cardNumber" label="借阅证号" width="150" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="ruleNumber" label="规则编号" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="cardName">
          <el-input v-model="form.cardName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="借阅证号" prop="cardNumber">
          <el-input-number v-model="form.cardNumber" :min="10000000000" :max="99999999999" style="width: 100%" placeholder="请输入 11 位借阅证号" />
        </el-form-item>
        <el-form-item label="规则编号" prop="ruleNumber">
          <el-input-number v-model="form.ruleNumber" :min="0" style="width: 100%" placeholder="请输入规则编号" />
        </el-form-item>
        <el-form-item label="密码" :prop="form.userId ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="form.userId ? '不修改请留空' : '请输入密码'" show-password />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStatementList, addStatement, updateStatement, deleteStatementByUserId, getStatementByUserId } from '@/api/user'
import { loading as loadingManager, message, createSearchDebounce } from '@/utils'

const tableData = ref([])
const selectedIds = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  condition: 'username',
  content: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('添加借阅证')
const formRef = ref(null)

const form = reactive({
  userId: null,
  username: '',
  cardName: '',
  cardNumber: null,
  ruleNumber: 0,
  password: '',
  status: 1,
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  cardName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  cardNumber: [
    { required: true, message: '请输入借阅证号', trigger: 'blur' },
    { type: 'number', message: '借阅证号必须是数字', trigger: 'blur' },
  ],
  ruleNumber: [{ required: true, message: '请输入规则编号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// ... existing code ...

// 加载用户列表（优化版）
const loadUserList = async () => {
  await loadingManager.wrap(async () => {
    const res = await getStatementList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      condition: searchForm.condition,
      query: searchForm.content,
    })

    if (res.code === 200 || res.status === 200) {
      // 确保正确获取分页数据
      tableData.value = res.data?.records || res.data?.data || []
      total.value = res.data?.total || 0

      if (tableData.value.length === 0 && total.value > 0) {
        message.warning('暂无数据')
      }
    }
  }, '正在加载用户列表...')
}

// ... existing code ...


// 搜索（带防抖优化）
const handleSearch = createSearchDebounce(() => {
  currentPage.value = 1
  loadUserList()
}, 500)

// 重置
const handleReset = () => {
  searchForm.condition = 'username'
  searchForm.content = ''
  handleSearch()
}

// 分页变化
const handleSizeChange = () => {
  loadUserList()
}

const handleCurrentChange = () => {
  loadUserList()
}

// 添加
const handleAdd = () => {
  dialogTitle.value = '添加借阅证'
  resetForm()
  dialogVisible.value = true
}

// 编辑（优化错误处理）
// ... existing code ...

const handleEdit = async (row) => {
  await loadingManager.wrap(async () => {
    const res = await getStatementByUserId(row.userId)
    if (res.status === 200) {
      Object.assign(form, res.data)
      dialogVisible.value = true
    }
  }, '正在加载用户详情...')
}

// ... existing code ...

// 删除（优化错误处理）
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await loadingManager.wrap(async () => {
      const res = await deleteStatementByUserId(row.userId)
      if (res.status === 200) {
        message.success('删除成功')
        loadUserList()
      }
    }, '正在删除...')
  })
}

// ... existing code ...

// 提交表单（优化错误处理）
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      await loadingManager.wrap(async () => {
        const api = form.userId ? updateStatement : addStatement
        const res = await api(form)

        if (res.status === 200) {
          message.success(form.userId ? '更新成功' : '添加成功')
          dialogVisible.value = false
          loadUserList()
        }
      }, form.userId ? '正在更新...' : '正在添加...')
    }
  })
}

// ... existing code ...


// 重置表单
const resetForm = () => {
  Object.assign(form, {
    userId: null,
    username: '',
    cardName: '',
    cardNumber: null,
    ruleNumber: 0,
    password: '',
    status: 1,
  })
  
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  loadUserList()
})
</script>

<style scoped>
.user-list-container {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
}
</style>
