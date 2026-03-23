<template>
  <div class="borrow-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>办理借书</span>
        </div>
      </template>

      <el-form ref="formRef" :model="borrowForm" :rules="rules" label-width="120px">
        <el-form-item label="借阅证号" prop="userId">
          <el-input v-model="borrowForm.userId" placeholder="请输入读者借阅证号" clearable @blur="handleSearchUser">
            <template #append>
              <el-button @click="handleSearchUser">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <!-- 读者信息展示 -->
        <el-card v-if="userInfo" style="margin-bottom: 20px" shadow="hover">
          <el-descriptions title="读者信息" :column="3" border>
            <el-descriptions-item label="姓名">{{ userInfo.cardName }}</el-descriptions-item>
            <el-descriptions-item label="借阅证号">{{ userInfo.cardNumber }}</el-descriptions-item>
            <el-descriptions-item label="可借数量">
              <el-tag type="success">{{ userInfo.canBorrow }}本</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="已借数量">
              <el-tag type="warning">{{ userInfo.borrowedCount }}本</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="超期数量">
              <el-tag v-if="userInfo.overdueCount > 0" type="danger">{{ userInfo.overdueCount }}本</el-tag>
              <el-tag v-else type="success">0 本</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="违章记录">
              <el-tag v-if="userInfo.violationCount > 0" type="danger">{{ userInfo.violationCount }}条</el-tag>
              <el-tag v-else type="success">无</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-form-item label="选择图书" prop="bookId" v-if="userInfo">
          <el-select v-model="borrowForm.bookId" placeholder="请选择图书" filterable style="width: 100%" @change="handleSelectBook">
            <el-option
              v-for="item in bookList"
              :key="item.bookId"
              :label="`${item.bookName} - ${item.bookAuthor} (${item.bookStatus || '在馆'})`"
              :value="item.bookId"
            />
          </el-select>
        </el-form-item>

        <!-- 图书信息展示 -->
        <el-card v-if="bookInfo" style="margin-bottom: 20px" shadow="hover">
          <el-descriptions title="图书信息" :column="3" border>
            <el-descriptions-item label="书名">{{ bookInfo.bookName }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ bookInfo.bookAuthor }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ bookInfo.bookType }}</el-descriptions-item>
            <el-descriptions-item label="图书馆">{{ bookInfo.bookLibrary }}</el-descriptions-item>
            <el-descriptions-item label="位置">{{ bookInfo.bookLocation }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-form-item label="应还日期" prop="returnDate" v-if="bookInfo">
          <el-date-picker
            v-model="borrowForm.returnDate"
            type="date"
            placeholder="选择应还日期"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark" v-if="bookInfo">
          <el-input v-model="borrowForm.remark" type="textarea" :rows="3" placeholder="备注信息（选填）" />
        </el-form-item>

        <el-form-item v-if="bookInfo">
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit" style="width: 120px">
            <el-icon><Check /></el-icon>
            确认借书
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserByCardId, getAvailableBooks, borrowBook } from '@/api/bookadmin'

const formRef = ref(null)
const submitLoading = ref(false)

const borrowForm = reactive({
  userId: '',
  bookId: null,
  returnDate: '',
  remark: '',
})

const rules = {
  userId: [{ required: true, message: '请输入借阅证号', trigger: 'blur' }],
  bookId: [{ required: true, message: '请选择图书', trigger: 'change' }],
  returnDate: [{ required: true, message: '请选择应还日期', trigger: 'change' }],
}

const userInfo = ref(null)
const bookInfo = ref(null)
const bookList = ref([])

// 查询读者信息
// 查询读者信息
const handleSearchUser = async () => {
  if (!borrowForm.userId) {
    ElMessage.warning('请输入借阅证号')
    return
  }

  try {
    const res = await getUserByCardId(borrowForm.userId)
    if ((res.code === 200 || res.status === 200) && res.data) {
      userInfo.value = {
        cardName: res.data.cardName,
        cardNumber: res.data.cardNumber, // 确保这里保存的是借阅证号
        canBorrow: res.data.canBorrowCount || 5,
        borrowedCount: res.data.borrowedCount || 0,
        overdueCount: res.data.overdueCount || 0,
        violationCount: res.data.violationCount || 0,
      }

      console.log('查询到的用户信息:', userInfo.value) // 添加调试日志
      console.log('借阅证号:', userInfo.value.cardNumber) // 添加调试日志

      loadBookList()
    } else {
      ElMessage.error(res.msg || '查询读者信息失败')
    }
  } catch (error) {
    console.error('查询读者信息失败:', error)
    ElMessage.error('查询读者信息失败：' + (error.message || '未知错误'))
  }
}

// 加载图书列表
const loadBookList = async () => {
  try {
    const res = await getAvailableBooks({ 
      pageNum: 1, 
      pageSize: 100,
      condition: '',
      query: '',
    })
    if (res.code === 200 || res.status === 200) {
      bookList.value = res.data.records || []
    } else {
      // 没有数据时不显示错误
      bookList.value = []
    }
  } catch (error) {
    console.error('加载图书列表失败:', error)
    ElMessage.error('加载图书列表失败')
    bookList.value = []
  }
}

// 选择图书
const handleSelectBook = async () => {
  if (!borrowForm.bookId) return

  try {
    // 从已加载的图书列表中查找
    const selectedBook = bookList.value.find(book => book.bookId === borrowForm.bookId)
    if (selectedBook) {
      bookInfo.value = {
        bookName: selectedBook.bookName,
        bookAuthor: selectedBook.bookAuthor,
        bookType: selectedBook.bookType || '未分类',
        bookLibrary: selectedBook.bookLibrary || '未知',
        bookLocation: selectedBook.bookLocation || '未知',
        bookNumber: selectedBook.bookNumber, // ← 新增这一行
        bookStatus: selectedBook.bookStatus, // ← 新增这一行
      }
      
      // 设置默认归还日期（30 天后）
      const date = new Date()
      date.setDate(date.getDate() + 30)
      borrowForm.returnDate = date
    }
  } catch (error) {
    console.error('获取图书详情失败:', error)
    ElMessage.error('获取图书详情失败')
  }
}

// 禁用日期（只能选择今天及以后的日期）
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 86400000
}

// 提交借书
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true

      try {
        // 检查是否有用户信息和图书信息
        if (!userInfo.value || !bookInfo.value) {
          ElMessage.error('请先查询读者信息和选择图书')
          submitLoading.value = false
          return
        }

        console.log('准备借书的参数:', {
          cardNumber: userInfo.value.cardNumber,
          bookNumber: bookInfo.value.bookNumber,
          borrowDate: new Date().toISOString().slice(0, 19).replace('T', ' '),
          closeDate: new Date(borrowForm.returnDate).toISOString().slice(0, 19).replace('T', ' '),
          bookAdminId: parseInt(localStorage.getItem('bookAdminId') || '1'),
        }) // 添加调试日志

        const res = await borrowBook({
          cardNumber: userInfo.value.cardNumber, // 使用借阅证号
          bookNumber: bookInfo.value.bookNumber, // 使用图书编号
          borrowDate: new Date().toISOString().slice(0, 19).replace('T', ' '), // 当前时间
          closeDate: new Date(borrowForm.returnDate).toISOString().slice(0, 19).replace('T', ' '), // 应还日期
          returnDate: null, // 归还日期为空
          bookAdminId: parseInt(localStorage.getItem('bookAdminId') || '1'), // 图书管理员 ID
        })

        if (res.code === 200 || res.status === 200) {
          ElMessage.success('借书成功')
          // 借书成功后，重新查询用户信息以更新已借数量
          if (borrowForm.userId) {
            setTimeout(() => {
              handleSearchUser()
            }, 500)
          }
          handleReset()
        } else {
          ElMessage.error(res.msg || '借书失败')
        }

      } catch (error) {
        console.error('借书失败:', error)
        ElMessage.error('借书失败：' + (error.message || '未知错误'))
      } finally {
        submitLoading.value = false
      }
    }
  })
}


// 格式化日期为 yyyy-MM-dd
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 重置表单
const handleReset = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  userInfo.value = null
  bookInfo.value = null
  bookList.value = []
}

onMounted(() => {
  // 可以在这里预加载一些数据
})
</script>

<style scoped>
.borrow-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
</style>
