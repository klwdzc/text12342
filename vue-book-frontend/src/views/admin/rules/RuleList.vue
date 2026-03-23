<template>
  <div class="rule-list-container">
    <el-card>
      <!-- 操作按钮 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加规则
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="ruleId" label="ID" width="80" />
        <el-table-column prop="bookRuleId" label="规则编号" width="100" />
        <el-table-column prop="bookDays" label="借阅天数" width="100">
          <template #default="{ row }">
            {{ row.bookDays }} 天
          </template>
        </el-table-column>
        <el-table-column prop="bookLimitNumber" label="限借本数" width="100" />
        <el-table-column prop="bookLimitLibrary" label="限制图书馆" min-width="150" />
        <el-table-column prop="bookOverdueFee" label="逾期费用 (元/天)" width="120">
          <template #default="{ row }">
            ¥{{ row.bookOverdueFee?.toFixed(2) || '0.00' }}
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="规则编号" prop="bookRuleId">
          <el-input-number v-model="form.bookRuleId" :min="0" style="width: 100%" placeholder="请输入规则编号" />
        </el-form-item>
        <el-form-item label="借阅天数" prop="bookDays">
          <el-input-number v-model="form.bookDays" :min="1" :max="365" style="width: 100%" placeholder="请输入借阅天数" />
        </el-form-item>
        <el-form-item label="限借本数" prop="bookLimitNumber">
          <el-input-number v-model="form.bookLimitNumber" :min="1" style="width: 100%" placeholder="请输入限借本数" />
        </el-form-item>
        <el-form-item label="限制图书馆" prop="bookLimitLibrary">
          <el-input v-model="form.bookLimitLibrary" placeholder="请输入限制的图书馆名称" />
        </el-form-item>
        <el-form-item label="逾期费用" prop="bookOverdueFee">
          <el-input-number v-model="form.bookOverdueFee" :min="0" :precision="2" :step="0.1" style="width: 100%" placeholder="请输入每天逾期费用" />
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
import { getRuleListByPage, addBookRule, updateBookRule, deleteBookRule, getBookRuleById } from '@/api/rule'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('添加规则')
const formRef = ref(null)

const form = reactive({
  ruleId: null,
  bookRuleId: 0,
  bookDays: 7,
  bookLimitNumber: 5,
  bookLimitLibrary: '',
  bookOverdueFee: 0.1,
})

const rules = {
  bookRuleId: [{ required: true, message: '请输入规则编号', trigger: 'blur' }],
  bookDays: [{ required: true, message: '请输入借阅天数', trigger: 'blur' }],
  bookLimitNumber: [{ required: true, message: '请输入限借本数', trigger: 'blur' }],
  bookLimitLibrary: [{ required: true, message: '请输入限制图书馆', trigger: 'blur' }],
  bookOverdueFee: [{ required: true, message: '请输入逾期费用', trigger: 'blur' }],
}

// 加载规则列表
const loadRuleList = async () => {
  loading.value = true
  try {
    const res = await getRuleListByPage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      condition: '',
      query: '',
    })

    if (res.code === 200 || res.status === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载规则列表失败:', error)
    ElMessage.error('加载规则列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadRuleList()
}

// 重置
const handleReset = () => {
  handleSearch()
}

// 分页变化
const handleSizeChange = () => {
  loadRuleList()
}

const handleCurrentChange = () => {
  loadRuleList()
}

// 添加
const handleAdd = () => {
  dialogTitle.value = '添加规则'
  resetForm()
  dialogVisible.value = true
}

// ... existing code ...

// 编辑
const handleEdit = async (row) => {
  dialogTitle.value = '编辑规则'
  resetForm()

  try {
    const res = await getBookRuleById(row.ruleId)
    if (res.status === 200) {
      Object.assign(form, res.data)
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('获取规则详情失败:', error)
    ElMessage.error('获取规则详情失败')
  }
}

// ... existing code ...

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      const res = await deleteBookRule(row.ruleId)
      if (res.status === 200) {
        ElMessage.success('删除成功')
        loadRuleList()
      }
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
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
        const api = form.ruleId ? updateBookRule : addBookRule
        const res = await api(form)

        if (res.status === 200) {
          ElMessage.success(form.ruleId ? '更新成功' : '添加成功')
          dialogVisible.value = false
          loadRuleList()
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
    ruleId: null,
    bookRuleId: 0,
    bookDays: 7,
    bookLimitNumber: 5,
    bookLimitLibrary: '',
    bookOverdueFee: 0.1,
  })
  
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  loadRuleList()
})
</script>

<style scoped>
.rule-list-container {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
}
</style>
