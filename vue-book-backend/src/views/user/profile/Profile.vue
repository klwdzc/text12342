<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 用户基本信息 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
            </div>
          </template>

          <div v-if="userInfo" class="user-info">
            <div class="avatar">
              <el-avatar :size="100" :icon="UserFilled" />
            </div>
            <el-descriptions :column="1" border style="margin-top: 20px">
              <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ userInfo.cardName }}</el-descriptions-item>
              <el-descriptions-item label="借阅证号">{{ userInfo.cardId }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ userInfo.email || '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'">
                  {{ userInfo.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- 修改密码 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>

          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="120px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入原密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdatePassword" :loading="updateLoading">
                <el-icon><Check /></el-icon>
                确认修改
              </el-button>
              <el-button @click="handleResetPassword">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 阅读记录统计 -->
        <el-card style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>阅读统计</span>
            </div>
          </template>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-statistic title="当前借阅" :value="stats.currentBorrow" />
            </el-col>
            <el-col :span="8">
              <el-statistic title="历史借阅" :value="stats.historyBorrow" />
            </el-col>
            <el-col :span="8">
              <el-statistic title="违章记录" :value="stats.violationCount" />
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserData } from '@/api/auth'
import { updatePassword } from '@/api/user'
import { getUserDashboardStatistics } from '@/api/statistics'

const store = useUserStore()
const passwordFormRef = ref(null)
const updateLoading = ref(false)

const userInfo = ref(null)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const stats = reactive({
  currentBorrow: 0,
  historyBorrow: 0,
  violationCount: 0,
})

// 验证密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

// 获取用户信息
const loadUserInfo = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      ElMessage.warning('请先登录')
      return
    }
    
    const res = await getUserData({ userId })
    
    if (res.code === 200 && res.data) {
      userInfo.value = res.data
    } else {
      // 没有数据时不显示错误
      userInfo.value = null
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    userInfo.value = null
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      return
    }
    
    const res = await getUserDashboardStatistics(userId)
    
    if (res.code === 200 && res.data) {
      stats.currentBorrow = Number(res.data.currentBorrow) || 0
      stats.historyBorrow = Number(res.data.historyBorrow) || 0
      stats.violationCount = Number(res.data.violations) || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 修改密码
const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      updateLoading.value = true
      try {
        const userId = store.userInfo?.userId || localStorage.getItem('userId')
        const res = await updatePassword({
          userId: userId,
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword,
        })
        
        if (res.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          handleResetPassword()
          // 退出登录
          setTimeout(() => {
            localStorage.removeItem('token')
            localStorage.removeItem('userRole')
            localStorage.removeItem('userId')
            window.location.href = '/login'
          }, 1500)
        } else {
          ElMessage.error(res.msg || '密码修改失败')
        }
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error('修改密码失败：' + (error.message || '未知错误'))
      } finally {
        updateLoading.value = false
      }
    }
  })
}

// 重置密码表单
const handleResetPassword = () => {
  if (passwordFormRef.value) {
    passwordFormRef.value.resetFields()
  }
}

onMounted(() => {
  loadUserInfo()
  loadStatistics()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
}

.card-header {
  font-weight: bold;
}

.user-info {
  text-align: center;
}

.avatar {
  margin-bottom: 20px;
}
</style>
