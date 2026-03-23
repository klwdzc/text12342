<template>
  <div class="home-container">
    <el-card class="welcome-card" v-loading="loading">
      <h1>欢迎使用 AI 智能图书馆管理系统</h1>
      <p>当前角色：读者用户</p>
    </el-card>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #409EFF">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.currentBorrow }}</div>
            <div class="stat-label">在借图书</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #67C23A">
            <el-icon><DocumentChecked /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.historyBorrow }}</div>
            <div class="stat-label">历史借阅</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #E6A23C">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.willExpire }}</div>
            <div class="stat-label">即将超期</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #F56C6C">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.violations }}</div>
            <div class="stat-label">违章记录</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <el-space direction="vertical" style="width: 100%">
            <el-button type="primary" style="width: 100%" @click="$router.push('/user/books')">
              <el-icon><Search /></el-icon>
              查找图书
            </el-button>
            <el-button type="success" style="width: 100%" @click="$router.push('/user/borrow')">
              <el-icon><List /></el-icon>
              我的借阅
            </el-button>
            <el-button type="warning" style="width: 100%" @click="$router.push('/user/violation')">
              <el-icon><Warning /></el-icon>
              我的违章
            </el-button>
            <el-button type="info" style="width: 100%" @click="$router.push('/user/ai')">
              <el-icon><ChatDotRound /></el-icon>
              AI 咨询
            </el-button>
            <el-button type="danger" style="width: 100%" @click="$router.push('/user/profile')">
              <el-icon><User /></el-icon>
              个人中心
            </el-button>
          </el-space>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>当前借阅</span>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in currentBorrows"
              :key="index"
              :timestamp="item.dueDate"
              placement="top"
              :type="item.type"
            >
              <el-card>
                <p>《{{ item.bookName }}》</p>
                <p style="font-size: 12px; color: #909399">应还日期：{{ item.dueDate }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Reading,
  DocumentChecked,
  Clock,
  WarningFilled,
  Search,
  List,
  Warning,
  ChatDotRound,
  User
} from '@element-plus/icons-vue'
import { getUserDashboardStatistics } from '@/api/statistics'
import request from '@/utils/request'

let refreshTimer = null
const loading = ref(false)

const statistics = reactive({
  currentBorrow: 0,
  historyBorrow: 0,
  willExpire: 0,
  violations: 0,
})

const currentBorrows = ref([])

// 加载统计数据
const loadStatistics = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      ElMessage.error('请先登录')
      return
    }
    
    const res = await getUserDashboardStatistics(userId)
    if (res.code === 200 || res.status === 200) {
      const data = res.data
      statistics.currentBorrow = data.currentBorrow || 0
      statistics.historyBorrow = data.historyBorrow || 0
      statistics.willExpire = data.willExpire || 0
      statistics.violations = data.violations || 0
    } else {
      ElMessage.warning(res.msg || '加载统计数据失败')
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

// 加载当前借阅
const loadCurrentBorrows = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      ElMessage.error('请先登录')
      return
    }
    
    const res = await request({
      url: '/user/get_current_borrows',
      method: 'get',
      params: { userId: parseInt(userId) }
    })
    
    if (res.code === 200 && res.data) {
      currentBorrows.value = res.data.map(item => ({
        bookName: item.bookName,
        dueDate: item.dueDate,
        type: item.type || 'normal'
      }))
    } else {
      currentBorrows.value = []
    }
  } catch (error) {
    console.error('加载当前借阅失败:', error)
    ElMessage.error('加载借阅记录失败')
    currentBorrows.value = []
  }
}

// 刷新数据
const refreshData = () => {
  loadStatistics()
  loadCurrentBorrows()
}

onMounted(() => {
  loadStatistics()
  loadCurrentBorrows()
  
  // 每 30 秒自动刷新一次
  refreshTimer = setInterval(() => {
    refreshData()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.home-container {
  padding: 20px;
}

.welcome-card {
  text-align: center;
  padding: 20px;
}

.welcome-card h1 {
  color: #303133;
  margin-bottom: 10px;
}

.welcome-card p {
  color: #909399;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.stat-icon .el-icon {
  font-size: 30px;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
