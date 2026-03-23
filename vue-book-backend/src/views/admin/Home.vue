<template>
  <div class="home-container">
    <el-card class="welcome-card">
      <h1>欢迎使用 AI 智能图书馆管理系统</h1>
      <p>当前角色：系统管理员</p>
    </el-card>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #409EFF">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalBooks }}</div>
            <div class="stat-label">图书总数</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #67C23A">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalUsers }}</div>
            <div class="stat-label">读者数量</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #E6A23C">
            <el-icon><TakeawayBox /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.todayBorrow }}</div>
            <div class="stat-label">今日借阅</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background-color: #F56C6C">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.violations }}</div>
            <div class="stat-label">违章记录</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表分析 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>借阅量趋势</span>
              <el-button type="primary" size="small" @click="refreshData">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <div ref="borrowChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>图书分类分布</span>
            </div>
          </template>
          <div ref="typeChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStatistics, getBorrowData, getBorrowTypeStatistic } from '@/api/statistics'

const borrowChartRef = ref(null)
const typeChartRef = ref(null)

let borrowChart = null
let typeChart = null
let refreshTimer = null

const statistics = reactive({
  totalBooks: 0,
  totalUsers: 0,
  todayBorrow: 0,
  violations: 0,
})

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getDashboardStatistics()
    if (res.code === 200 || res.status === 200) {
      const data = res.data
      statistics.totalBooks = data.totalBooks || 0
      statistics.totalUsers = data.totalUsers || 0
      statistics.todayBorrow = data.todayBorrow || 0
      statistics.violations = data.violations || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// ... existing code ...

// 初始化借阅量趋势图
const initBorrowChart = async () => {
  try {
    const res = await getBorrowData()
    if (res.code === 200 || res.status === 200) {
      const data = res.data

      if (!borrowChartRef.value) return
      borrowChart = echarts.init(borrowChartRef.value)

      // 修复：使用正确的后端字段名
      const categories = data.borrowDates || []
      const values = data.borrowNumber || []

      const option = {
        tooltip: {
          trigger: 'axis',
        },
        xAxis: {
          type: 'category',
          data: categories,
        },
        yAxis: {
          type: 'value',
          name: '借阅量',
        },
        series: [
          {
            name: '借阅量',
            type: 'bar',
            data: values,
            itemStyle: {
              color: '#409EFF',
            },
          },
        ],
      }

      borrowChart.setOption(option)
    }
  } catch (error) {
    console.error('加载借阅量数据失败:', error)
  }
}

// ... existing code ...

// 初始化分类统计饼图
const initTypeChart = async () => {
  try {
    const res = await getBorrowTypeStatistic()
    if (res.code === 200 || res.status === 200) {
      const data = res.data || []

      console.log('图书分类数据:', data)

      if (!typeChartRef.value) return
      typeChart = echarts.init(typeChartRef.value)

      // 转换数据格式为 ECharts 需要的格式
      const chartData = data.map(item => ({
        value: item.borrowNumbers || 0,
        name: item.bookTypes || '未分类'
      }))

      console.log('转换后的图表数据:', chartData)

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)',
        },
        legend: {
          orient: 'vertical',
          left: 'left',
        },
        series: [
          {
            name: '图书分类',
            type: 'pie',
            radius: '50%',
            data: chartData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
              },
            },
          },
        ],
      }

      typeChart.setOption(option)
    }
  } catch (error) {
    console.error('加载分类统计数据失败:', error)
  }
}

// ... existing code ...


// 刷新数据
const refreshData = () => {
  loadStatistics()
  initBorrowChart()
  initTypeChart()
}

// 窗口大小变化时重新渲染图表
const handleResize = () => {
  borrowChart?.resize()
  typeChart?.resize()
}

onMounted(() => {
  loadStatistics()
  initBorrowChart()
  initTypeChart()
  window.addEventListener('resize', handleResize)
  
  // 每 30 秒自动刷新一次
  refreshTimer = setInterval(() => {
    refreshData()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  borrowChart?.dispose()
  typeChart?.dispose()
  window.removeEventListener('resize', handleResize)
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
