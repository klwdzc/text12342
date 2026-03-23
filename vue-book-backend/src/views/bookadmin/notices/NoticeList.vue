<template>
  <div class="notice-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加公告
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline style="margin-bottom: 20px">
        <el-form-item label="搜索内容">
          <el-input v-model="queryForm.content" placeholder="请输入公告标题或内容" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadNoticeList">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 公告列表 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="noticeId" label="公告 ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.noticeId)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadNoticeList"
        @current-change="loadNoticeList"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="noticeForm" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="noticeForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="noticeForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNoticeList, addNotice, getNotice, updateNotice, deleteNotice } from '@/api/bookadmin'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)

const queryForm = reactive({
  content: '',
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
})

const tableData = ref([])

const noticeForm = reactive({
  noticeId: null,
  title: '',
  content: '',
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
}

// 加载公告列表
const loadNoticeList = async () => {
  loading.value = true
  try {
    const res = await getNoticeList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      condition: queryForm.content,
    })
    
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载公告列表失败:', error)
    ElMessage.error('加载公告列表失败')
  } finally {
    loading.value = false
  }
}

// 添加公告
const handleAdd = () => {
  dialogTitle.value = '添加公告'
  dialogVisible.value = true
}

// 编辑公告
const handleEdit = async (row) => {
  dialogTitle.value = '编辑公告'
  try {
    const res = await getNotice(row.noticeId)
    if (res.code === 200 && res.data) {
      const data = res.data
      noticeForm.noticeId = data.noticeId
      noticeForm.title = data.title
      noticeForm.content = data.content
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('获取公告详情失败:', error)
    ElMessage.error('获取公告详情失败')
  }
}

// 删除公告
const handleDelete = (noticeId) => {
  ElMessageBox.confirm('确定要删除该公告吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      const res = await deleteNotice(noticeId)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadNoticeList()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除公告失败:', error)
      ElMessage.error('删除公告失败')
    }
  })
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        let res
        if (noticeForm.noticeId) {
          // 更新公告
          res = await updateNotice(noticeForm.noticeId, {
            title: noticeForm.title,
            content: noticeForm.content,
          })
        } else {
          // 添加公告
          res = await addNotice({
            title: noticeForm.title,
            content: noticeForm.content,
          })
        }
        
        if (res.code === 200) {
          ElMessage.success(noticeForm.noticeId ? '更新成功' : '添加成功')
          dialogVisible.value = false
          loadNoticeList()
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败：' + (error.message || '未知错误'))
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  noticeForm.noticeId = null
  noticeForm.title = ''
  noticeForm.content = ''
}

onMounted(() => {
  loadNoticeList()
})
</script>

<style scoped>
.notice-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
</style>
