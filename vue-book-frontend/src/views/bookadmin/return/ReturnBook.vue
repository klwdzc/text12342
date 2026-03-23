<template>
  <div class="return-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>办理还书</span>
        </div>
      </template>

      <el-form ref="formRef" :model="returnForm" :rules="rules" label-width="120px">
        <el-form-item label="图书编号" prop="bookNumber">
          <el-input v-model="returnForm.bookNumber" placeholder="请输入图书编号" clearable @blur="handleQueryBook">
            <template #append>
              <el-button @click="handleQueryBook">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <!-- 图书信息展示 -->
        <el-card v-if="bookInfo" style="margin-bottom: 20px" shadow="hover">
          <el-descriptions title="图书信息" :column="3" border>
            <el-descriptions-item label="书名">{{ bookInfo.bookName }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ bookInfo.bookAuthor }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ bookInfo.bookType }}</el-descriptions-item>
            <el-descriptions-item label="图书馆">{{ bookInfo.bookLibrary }}</el-descriptions-item>
            <el-descriptions-item label="位置">{{ bookInfo.bookLocation }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="bookInfo.bookStatus === '在馆' ? 'success' : 'warning'">{{ bookInfo.bookStatus }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 借阅信息展示 -->
        <el-card v-if="borrowInfo" style="margin-bottom: 20px" shadow="hover">
          <el-descriptions title="借阅信息" :column="3" border>
            <el-descriptions-item label="借阅证号">{{ borrowInfo.cardNumber }}</el-descriptions-item>
            <el-descriptions-item label="读者姓名">{{ borrowInfo.cardName }}</el-descriptions-item>
            <el-descriptions-item label="借书日期">{{ borrowInfo.borrowDate }}</el-descriptions-item>
            <el-descriptions-item label="应还日期">{{ borrowInfo.returnDate }}</el-descriptions-item>
            <el-descriptions-item label="实还日期">
              <el-date-picker
                  v-model="returnForm.actualReturnDate"
                  type="date"
                  placeholder="选择实还日期"
                  :disabled-date="disabledDate"
                  style="width: 100%"
              />
            </el-descriptions-item>
            <el-descriptions-item label="是否逾期">
              <el-tag :type="borrowInfo.isOverdue ? 'danger' : 'success'">
                {{ borrowInfo.isOverdue ? '已逾期' : '未逾期' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 违章信息（如果逾期） -->
        <el-alert
            v-if="violationInfo && violationInfo.isOverdue"
            title="逾期提示"
            type="warning"
            :closable="false"
            style="margin-bottom: 20px"
        >
          <p>该图书已逾期，逾期天数：{{ violationInfo.overdueDays }}天</p>
          <p>违章罚款金额：¥{{ parseFloat(violationInfo.fineAmount || 0).toFixed(2) }}</p>
        </el-alert>

        <el-form-item label="备注" prop="remark" v-if="borrowInfo">
          <el-input v-model="returnForm.remark" type="textarea" :rows="3" placeholder="备注信息（选填）" />
        </el-form-item>

        <el-form-item v-if="borrowInfo">
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit" style="width: 120px">
            <el-icon><Check /></el-icon>
            确认还书
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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { queryBookExpire, queryExpireInfo, returnBook } from '@/api/bookadmin'

const formRef = ref(null)
const submitLoading = ref(false)

const returnForm = reactive({
  bookNumber: '',
  actualReturnDate: new Date(),
  remark: '',
})

const rules = {
  bookNumber: [{ required: true, message: '请输入图书编号', trigger: 'blur' }],
  actualReturnDate: [{ required: true, message: '请选择实还日期', trigger: 'change' }],
}

const bookInfo = ref(null)
const borrowInfo = ref(null)
const violationInfo = ref(null)

// 查询图书信息
const handleQueryBook = async () => {
  if (!returnForm.bookNumber) {
    ElMessage.warning('请输入图书编号')
    return
  }

  try {
    // 查询图书是否被借出
    const res = await queryBookExpire(returnForm.bookNumber)
    if (res.code === 200 || res.status === 200) {
      // 图书已借出，继续查询借阅和图书信息
      await loadBorrowInfo()
    } else {
      // 图书未借出或不存在
      ElMessage.error(res.msg || '该图书未被借出，无需归还')
      bookInfo.value = null
      borrowInfo.value = null
      violationInfo.value = null
    }
  } catch (error) {
    console.error('查询图书失败:', error)
    ElMessage.error('查询图书失败：' + (error.message || '未知错误'))
  }
}

// 加载借阅信息
const loadBorrowInfo = async () => {
  try {
    const res = await queryExpireInfo(returnForm.bookNumber)
    if ((res.code === 200 || res.status === 200) && res.data) {
      const data = res.data
      bookInfo.value = {
        bookName: data.bookName || '未知图书',
        bookAuthor: data.bookAuthor || '未知',
        bookType: data.bookType || '未知',
        bookLibrary: data.bookLibrary || '未知',
        bookLocation: data.bookLocation || '未知',
        bookStatus: data.bookStatus || '已借出',
      }

      borrowInfo.value = {
        cardNumber: data.cardNumber,
        cardName: data.cardName || '未知',
        borrowDate: data.borrowDate,
        returnDate: data.shouldReturnDate || data.closeDate || data.returnDate,
        isOverdue: Boolean(data.isOverdue),
      }

      if (data.isOverdue) {
        violationInfo.value = {
          isOverdue: true,
          overdueDays: data.overdueDays || 0,
          fineAmount: data.fineAmount || 0,
        }
      } else {
        violationInfo.value = null
      }
    }
  } catch (error) {
    console.error('加载借阅信息失败:', error)
  }
}

// 禁用日期（只能选择今天及以前的日期）
const disabledDate = (time) => {
  return time.getTime() > Date.now()
}

// 提交还书
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true

      try {
        const bookAdminId = parseInt(localStorage.getItem('bookAdminId') || '0', 10)
        const res = await returnBook({
          bookNumber: returnForm.bookNumber,
          returnDate: formatDateTime(returnForm.actualReturnDate),
          violationMessage: returnForm.remark || `办理还书${violationInfo.value?.isOverdue ? '（逾期）' : ''}`,
          violationAdminId: Number.isNaN(bookAdminId) ? null : bookAdminId,
        })

        if (res.code === 200) {
          ElMessage.success('还书成功')
          handleReset()
        } else {
          ElMessage.error(res.msg || '还书失败')
        }
      } catch (error) {
        console.error('还书失败:', error)
        ElMessage.error('还书失败：' + (error.message || '未知错误'))
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 格式化日期为 yyyy-MM-dd HH:mm:ss
const formatDateTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 重置表单
const handleReset = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  bookInfo.value = null
  borrowInfo.value = null
  violationInfo.value = null
}
</script>

<style scoped>
.return-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
</style>