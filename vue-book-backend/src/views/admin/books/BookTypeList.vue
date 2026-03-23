<template>
  <div class="book-type-container">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :model="searchForm" :inline="true" size="default">
        <el-form-item label="分类名称">
          <el-input v-model="searchForm.content" placeholder="请输入分类名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
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
          添加分类
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="typeId" label="ID" width="80" />
        <el-table-column prop="typeName" label="分类名称" min-width="150" />
        <el-table-column prop="typeContent" label="描述" min-width="200" show-overflow-tooltip />
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="typeName">
          <el-input v-model="form.typeName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述" prop="typeContent">
          <el-input v-model="form.typeContent" type="textarea" :rows="3" placeholder="请输入分类描述" />
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
import { getBookTypeListByPage, addBookType, updateBookType, deleteBookType, getBookTypeById } from '@/api/bookType'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  content: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('添加分类')
const formRef = ref(null)

const form = reactive({
  typeId: null,
  typeName: '',
  typeContent: '',
})

const rules = {
  typeName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

// ... existing code ...

// 加载分类列表
const loadTypeList = async () => {
  loading.value = true
  try {
    const res = await getBookTypeListByPage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      condition: 'typeName',
      query: searchForm.content,
    })

    if (res.code === 200 || res.status === 200) {
      // 确保正确获取分页数据
      tableData.value = res.data?.records || res.data?.data || []
      total.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载分类列表失败:', error)
    ElMessage.error('加载分类列表失败')
  } finally {
    loading.value = false
  }
}

// ... existing code ...


// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadTypeList()
}

// 重置
const handleReset = () => {
  searchForm.content = ''
  handleSearch()
}

// 分页变化
const handleSizeChange = () => {
  loadTypeList()
}

const handleCurrentChange = () => {
  loadTypeList()
}

// 添加
const handleAdd = () => {
  dialogTitle.value = '添加分类'
  resetForm()
  dialogVisible.value = true
}

// ... existing code ...

// 编辑
const handleEdit = async (row) => {
  dialogTitle.value = '编辑分类'
  resetForm()

  try {
    const res = await getBookTypeById(row.typeId)
    if (res.status === 200) {
      Object.assign(form, res.data)
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('获取分类详情失败:', error)
  }
}

// ... existing code ...

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该分类吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      const res = await deleteBookType(row.typeId)
      if (res.status === 200) {
        ElMessage.success('删除成功')
        loadTypeList()
      }
    } catch (error) {
      console.error('删除失败:', error)
    }
  })
}

// ... existing code ...

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const api = form.typeId ? updateBookType : addBookType
        const res = await api(form)

        if (res.status === 200) {
          ElMessage.success(form.typeId ? '更新成功' : '添加成功')
          dialogVisible.value = false
          loadTypeList()
        }
      } catch (error) {
        console.error('提交失败:', error)
      }
    }
  })
}

// ... existing code ...


// 重置表单
const resetForm = () => {
  Object.assign(form, {
    typeId: null,
    typeName: '',
    typeContent: '',
  })
  
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  loadTypeList()
})
</script>

<style scoped>
.book-type-container {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
}
</style>
