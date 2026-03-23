<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <h1>欢迎使用 AI 智能图书馆管理系统</h1>
      <p>当前角色：图书管理员</p>
    </el-card>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #409EFF">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.todayBorrow }}</div>
            <div class="stat-label">今日借书</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #67C23A">
            <el-icon><Download /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.todayReturn }}</div>
            <div class="stat-label">今日还书</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #E6A23C">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.pendingBorrow }}</div>
            <div class="stat-label">待处理借阅</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #F56C6C">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.overdueBooks }}</div>
            <div class="stat-label">超期未还</div>
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
            <el-button type="primary" style="width: 100%" @click="$router.push('/bookadmin/borrow')">
              <el-icon><Plus /></el-icon>
              办理借书
            </el-button>
            <el-button type="success" style="width: 100%" @click="$router.push('/bookadmin/return')">
              <el-icon><Download /></el-icon>
              办理还书
            </el-button>
            <el-button type="warning" style="width: 100%" @click="$router.push('/bookadmin/statements')">
              <el-icon><Document /></el-icon>
              查看报表
            </el-button>
            <el-button type="info" style="width: 100%" @click="$router.push('/bookadmin/notices')">
              <el-icon><Bell /></el-icon>
              公告管理
            </el-button>
          </el-space>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近借阅</span>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in recentBorrows"
              :key="index"
              :timestamp="item.time"
              placement="top"
            >
              <el-card>
                <p>{{ item.userName }} 借阅了《{{ item.bookName }}》</p>
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
import { getBookAdminDashboardStatistics } from '@/api/statistics'
import request from '@/utils/request'

let refreshTimer = null

const statistics = reactive({
  todayBorrow: 0,
  todayReturn: 0,
  pendingBorrow: 0,
  overdueBooks: 0,
})

const recentBorrows = ref([])

// 加载统计数据
const loadStatistics = async () => {
  try {
    const bookAdminId = localStorage.getItem('bookAdminId')
    if (!bookAdminId) {
      console.error('未找到图书管理员 ID')
      return
    }
    
    const res = await getBookAdminDashboardStatistics(bookAdminId)
    if (res.code === 200 || res.status === 200) {
      const data = res.data
      statistics.todayBorrow = data.todayBorrow || 0
      statistics.todayReturn = data.todayReturn || 0
      statistics.pendingBorrow = data.pendingBorrow || 0
      statistics.overdueBooks = data.overdueBooks || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载最近借阅
const loadRecentBorrows = async () => {
  try {
    // 调用后端 API 获取最近借阅记录
    const res = await request({
      url: '/bookadmin/get_recent_borrows',
      method: 'get',
      params: { limit: 5 }
    })
    
    if (res.code === 200 && res.data) {
      recentBorrows.value = res.data.map(item => ({
        userName: item.userName,
        bookName: item.bookName,
        time: item.time
      }))
    } else {
      // 使用模拟数据作为降级方案
      recentBorrows.value = [
        { userName: '张三丰', bookName: 'Java 编程思想', time: new Date().toLocaleString() },
        { userName: '李四光', bookName: '设计模式', time: new Date().toLocaleString() },
        { userName: '王老五', bookName: '算法导论', time: new Date().toLocaleString() },
      ]
    }
  } catch (error) {
    console.error('加载最近借阅失败:', error)
    // 出错时使用模拟数据
    recentBorrows.value = [
      { userName: '张三丰', bookName: 'Java 编程思想', time: new Date().toLocaleString() },
      { userName: '李四光', bookName: '设计模式', time: new Date().toLocaleString() },
      { userName: '王老五', bookName: '算法导论', time: new Date().toLocaleString() },
    ]
  }
}

// 刷新数据
const refreshData = () => {
  loadStatistics()
  loadRecentBorrows()
}

onMounted(() => {
  loadStatistics()
  loadRecentBorrows()
  
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
