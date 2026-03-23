<template>
  <div class="login-container">
    <div class="login-box">
      <h1 class="login-title">AI 智能图书馆管理系统</h1>
      
      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="系统管理员" name="admin">
          <el-form :model="loginForm" :rules="rules" ref="formRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">登 录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="图书管理员" name="bookadmin">
          <el-form :model="loginForm" :rules="rules" ref="formRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">登 录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="读者用户" name="user">
          <el-form :model="loginForm" :rules="rules" ref="formRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入借阅证号" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">登 录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { adminLogin, bookAdminLogin, userLogin, getAdminData, getBookAdminData, getUserData } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const activeTab = ref('admin')

const loginForm = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于 6 位', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      
      try {
        let loginApi, getDataApi, role
        
        // 根据不同的 tab 调用不同的登录接口
        if (activeTab.value === 'admin') {
          loginApi = adminLogin
          getDataApi = getAdminData
          role = 'admin'
        } else if (activeTab.value === 'bookadmin') {
          loginApi = bookAdminLogin
          getDataApi = getBookAdminData
          role = 'bookadmin'
        } else {
          loginApi = userLogin
          getDataApi = getUserData
          role = 'user'
        }
        
        // 登录
        const loginRes = await loginApi(loginForm)
        
        if (loginRes.status === 200) {
          // 获取 token 和用户 ID（从 map 中获取）
          const token = loginRes.map?.token || loginRes.data?.token
          const userId = loginRes.map?.id || loginRes.data?.id
          
          if (!token) {
            ElMessage.error('登录失败：未获取到 token')
            return
          }
          
          // 存储到 pinia 和 localStorage
          userStore.setToken(token)
          
          // 保存用户 ID 到 pinia 和 localStorage（用于后续 API 调用）
          if (userId) {
            userStore.setUserId(userId)
          }
          
          // 设置用户角色
          userStore.setUserRole(role)
          
          ElMessage.success(loginRes.msg || '登录成功')
          
          // 跳转到对应的首页
          if (role === 'admin') {
            router.push('/admin/home')
          } else if (role === 'bookadmin') {
            router.push('/bookadmin/home')
          } else {
            router.push('/user/home')
          }
        }
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 450px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 24px;
}

.login-tabs {
  margin-top: 20px;
}
</style>
