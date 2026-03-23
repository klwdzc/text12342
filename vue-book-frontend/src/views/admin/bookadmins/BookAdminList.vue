 <template>
  <div class="book-admin-list-container">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :model="searchForm" :inline="true" size="default">
        <el-form-item label="搜索条件">
          <el-select v-model="searchForm.condition" style="width: 120px">
            <el-option label="用户名" value="username" />
            <el-option label="姓名" value="bookAdminName" />
            <el-option label="邮箱" value="email" />
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
          添加图书管理员
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="bookAdminId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="bookAdminName" label="姓名" min-width="100" />
        <el-table-column prop="email" label="邮箱" min-width="150" />
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
        <el-form-item label="姓名" prop="bookAdminName">
          <el-input v-model="form.bookAdminName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" :prop="form.bookAdminId ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="form.bookAdminId ? '不修改请留空' : '请输入密码'" show-password />
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
import { getBookAdminListByPage, addBookAdmin, updateBookAdmin, deleteBookAdminById, getBookAdminById } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  condition: 'username',
  content: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('添加图书管理员')
const formRef = ref(null)

const form = reactive({
  bookAdminId: null,
  username: '',
  bookAdminName: '',
  email: '',
  password: '',
  status: 1,
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  bookAdminName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// ... existing code ...

// 加载图书管理员列表
const loadBookAdminList = async () => {
  loading.value = true
  try {
    const res = await getBookAdminListByPage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      condition: searchForm.condition,
      query: searchForm.content,
    })

    if (res.code === 200 || res.status === 200) {
      // 确保正确获取分页数据
      tableData.value = res.data?.records || res.data?.data || []
      total.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载图书管理员列表失败:', error)
    ElMessage.error('加载图书管理员列表失败')
  } finally {
    loading.value = false
  }
}

// ... existing code ...

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadBookAdminList()
}

// 重置
const handleReset = () => {
  searchForm.condition = 'username'
  searchForm.content = ''
  handleSearch()
}

// 分页变化
const handleSizeChange = () => {
  loadBookAdminList()
}

const handleCurrentChange = () => {
  loadBookAdminList()
}

// 添加
const handleAdd = () => {
  dialogTitle.value = '添加图书管理员'
  resetForm()
  dialogVisible.value = true
}

// 编辑
// ... existing code ...

const handleEdit = async (row) => {
  dialogTitle.value = '编辑图书管理员'
  resetForm()

  try {
    const res = await getBookAdminById(row.bookAdminId)
    if (res.status === 200) {
      Object.assign(form, res.data)
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('获取图书管理员详情失败:', error)
    ElMessage.error('获取图书管理员详情失败')
  }
}

// ... existing code ...
//删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该图书管理员吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      const res = await deleteBookAdminById(row.bookAdminId)
      if (res.status === 200) {
        ElMessage.success('删除成功')
        loadBookAdminList()
      }
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

// ... existing code ...
//新增
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const api = form.bookAdminId ? updateBookAdmin : addBookAdmin
        const res = await api(form)

        if (res.status === 200) {
          ElMessage.success(form.bookAdminId ? '更新成功' : '添加成功')
          dialogVisible.value = false
          loadBookAdminList()
        }
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error('提交失败')
      }
    }
  })
}

// ... existing code ...

// 重置表单
const resetForm = () => {
  Object.assign(form, {
    bookAdminId: null,
    username: '',
    bookAdminName: '',
    email: '',
    password: '',
    status: 1,
  })
  
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  loadBookAdminList()
})
</script>

<style scoped>
.book-admin-list-container {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
}
</style>
