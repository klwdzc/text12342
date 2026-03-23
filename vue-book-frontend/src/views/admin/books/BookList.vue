<template>
  <div class="book-list-container">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :model="searchForm" :inline="true" size="default">
        <el-form-item label="搜索条件">
          <el-select v-model="searchForm.condition" style="width: 120px">
            <el-option label="书名" value="bookName" />
            <el-option label="作者" value="bookAuthor" />
            <el-option label="分类" value="bookType" />
            <el-option label="图书馆" value="bookLibrary" />
            <el-option label="位置" value="bookLocation" />
            <el-option label="状态" value="bookStatus" />
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
        <div class="toolbar-left">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加图书
          </el-button>
          <el-button type="success" @click="handleUpload">
            <el-icon><Upload /></el-icon>
            Excel 导入
          </el-button>
          <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </div>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="bookId" label="ID" width="80" />
        <el-table-column prop="bookNumber" label="图书编号" width="120" />
        <el-table-column prop="bookName" label="书名" min-width="150" />
        <el-table-column prop="bookAuthor" label="作者" min-width="120" />
        <el-table-column prop="bookType" label="分类" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.bookType || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bookLibrary" label="图书馆" width="120" />
        <el-table-column prop="bookLocation" label="位置" width="100" />
        <el-table-column prop="bookStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.bookStatus === '在馆' ? 'success' : 'warning'">
              {{ row.bookStatus || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
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
        <el-form-item label="图书编号" prop="bookNumber">
          <el-input-number v-model="form.bookNumber" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="书名" prop="bookName">
          <el-input v-model="form.bookName" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者" prop="bookAuthor">
          <el-input v-model="form.bookAuthor" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="分类" prop="bookType">
          <el-select v-model="form.bookType" placeholder="请选择图书分类" style="width: 100%">
            <el-option
              v-for="item in typeList"
              :key="item.typeId"
              :label="item.typeName"
              :value="item.typeName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图书馆" prop="bookLibrary">
          <el-input v-model="form.bookLibrary" placeholder="请输入图书馆名称" />
        </el-form-item>
        <el-form-item label="位置" prop="bookLocation">
          <el-input v-model="form.bookLocation" placeholder="请输入图书位置" />
        </el-form-item>
        <el-form-item label="状态" prop="bookStatus">
          <el-select v-model="form.bookStatus" placeholder="请选择图书状态" style="width: 100%">
            <el-option label="在馆" value="在馆" />
            <el-option label="借出" value="借出" />
            <el-option label="丢失" value="丢失" />
            <el-option label="损坏" value="损坏" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="bookDescription">
          <el-input v-model="form.bookDescription" type="textarea" :rows="3" placeholder="请输入图书描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Excel 导入对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="Excel 批量导入图书" width="400px">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">只能上传 xlsx/xls 文件</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUploadConfirm">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBookList, addBook, updateBook, deleteBook, deleteBookBatch, getBookInfo, getBookTypeList, uploadExcel } from '@/api/book'
import { loading as loadingManager, message, createSearchDebounce } from '@/utils'

const tableData = ref([])
const typeList = ref([])
const selectedIds = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  condition: 'bookName',
  content: '',
})

const dialogVisible = ref(false)
const dialogTitle = ref('添加图书')
const formRef = ref(null)
const uploadRef = ref(null)
const uploadDialogVisible = ref(false)
const uploadFile = ref(null)

const form = reactive({
  bookId: null,
  bookNumber: 0,
  bookName: '',
  bookAuthor: '',
  bookType: '',
  bookLibrary: '',
  bookLocation: '',
  bookStatus: '在馆',
  bookDescription: '',
})

const rules = {
  bookNumber: [{ required: true, message: '请输入图书编号', trigger: 'blur' }],
  bookName: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  bookAuthor: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  bookType: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  bookLibrary: [{ required: true, message: '请输入图书馆', trigger: 'blur' }],
  bookLocation: [{ required: true, message: '请输入位置', trigger: 'blur' }],
  bookStatus: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

// ... existing code ...

// 加载图书列表（优化版）
const loadBookList = async () => {
  await loadingManager.wrap(async () => {
    const res = await getBookList({
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
  }, '正在加载图书列表...')
}

// ... existing code ...

const loadTypeList = async () => {
  try {
    const res = await getBookTypeList()
    if (res.code === 200 || res.status === 200) {
      typeList.value = res.data || []
    }
  } catch (error) {
    console.error('加载分类列表失败:', error)
  }
}

// 搜索（带防抖优化）
const handleSearch = createSearchDebounce(() => {
  currentPage.value = 1
  loadBookList()
}, 500)

// 重置
const handleReset = () => {
  searchForm.condition = 'bookName'
  searchForm.content = ''
  handleSearch()
}

// 分页变化
const handleSizeChange = () => {
  loadBookList()
}

const handleCurrentChange = () => {
  loadBookList()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.bookId)
}

// 添加
const handleAdd = () => {
  dialogTitle.value = '添加图书'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row) => {
  dialogTitle.value = '编辑图书'
  resetForm()

  try {
    const res = await getBookInfo(row.bookId)
    if (res.status === 200) {
      Object.assign(form, res.data)
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('获取图书详情失败:', error)
  }
}

// ... existing code ...

// 删除（优化错误处理）
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该图书吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await loadingManager.wrap(async () => {
      const res = await deleteBook(row.bookId)
      if (res.status === 200) {
        message.success('删除成功')
        loadBookList()
      }
    }, '正在删除...')
  })
}

// ... existing code ...

// 批量删除（优化错误处理）
const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 本图书吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await loadingManager.wrap(async () => {
      const booksToDelete = tableData.value.filter(book => selectedIds.value.includes(book.bookId))
      const res = await deleteBookBatch(booksToDelete)
      if (res.status === 200) {
        message.success('批量删除成功')
        loadBookList()
      }
    }, '正在批量删除...')
  })
}

// ... existing code ...

// 提交表单（优化错误处理）
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      await loadingManager.wrap(async () => {
        const api = form.bookId ? updateBook : addBook
        const res = await api(form)

        if (res.status === 200) {
          message.success(form.bookId ? '更新成功' : '添加成功')
          dialogVisible.value = false
          loadBookList()
        }
      }, form.bookId ? '正在更新...' : '正在添加...')
    }
  })
}

// Excel 导入
const handleUpload = () => {
  uploadDialogVisible.value = true
  uploadFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const handleUploadConfirm = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  try {
    const res = await uploadExcel(uploadFile.value)
    if (res.status === 200) {
      ElMessage.success('导入成功')
      uploadDialogVisible.value = false
      loadBookList()
    }
  } catch (error) {
    console.error('导入失败:', error)
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(form, {
    bookId: null,
    bookNumber: 0,
    bookName: '',
    bookAuthor: '',
    bookType: '',
    bookLibrary: '',
    bookLocation: '',
    bookStatus: '在馆',
    bookDescription: '',
  })

  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  loadBookList()
  loadTypeList()
})
</script>

<style scoped>
.book-list-container {
  padding: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}
</style>
