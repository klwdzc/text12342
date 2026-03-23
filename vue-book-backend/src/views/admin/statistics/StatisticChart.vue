<template>
  <div class="statistics-container">
    <el-row :gutter="20">
      <!-- 借阅量统计 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>借阅量趋势</span>
            </div>
          </template>
          <div ref="borrowChartRef" style="height: 400px"></div>
        </el-card>
      </el-col>

      <!-- 图书分类统计 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>图书分类借阅分布</span>
            </div>
          </template>
          <div ref="typeChartRef" style="height: 400px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AI 图表生成 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>AI 智能图表生成</span>
        </div>
      </template>
      <el-form :model="aiForm" label-width="100px">
        <el-form-item label="分析目标">
          <el-input v-model="aiForm.goal" placeholder="例如：分析各类图书的借阅情况" />
        </el-form-item>
        <el-form-item label="图表类型">
          <el-select v-model="aiForm.chartType" placeholder="请选择图表类型">
            <el-option label="柱状图" value="bar" />
            <el-option label="折线图" value="line" />
            <el-option label="饼图" value="pie" />
            <el-option label="散点图" value="scatter" />
          </el-select>
        </el-form-item>
        <el-form-item label="图表名称">
          <el-input v-model="aiForm.name" placeholder="请输入图表名称" />
        </el-form-item>
        <el-form-item label="数据文件">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept=".csv,.xlsx,.xls"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">支持 csv、xlsx、xls 格式文件</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleGenChart">
            <el-icon><MagicStick /></el-icon>
            生成图表
          </el-button>
        </el-form-item>
      </el-form>

      <!-- AI 生成的图表展示 -->
      <div v-if="aiChartData" style="margin-top: 20px">
        <div ref="aiChartRef" style="height: 500px"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getBorrowData, getBorrowTypeStatistic, genChart } from '@/api/chart'

const borrowChartRef = ref(null)
const typeChartRef = ref(null)
const aiChartRef = ref(null)
const uploadRef = ref(null)

const aiForm = reactive({
  goal: '',
  chartType: '',
  name: '',
})

const uploadFile = ref(null)
const aiChartData = ref(null)

let borrowChart = null
let typeChart = null
let aiChart = null

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




// 处理文件变化
const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

// 生成 AI 图表
const handleGenChart = async () => {
  if (!aiForm.goal || !aiForm.chartType || !aiForm.name) {
    ElMessage.warning('请填写完整的表单信息')
    return
  }
  
  if (!uploadFile.value) {
    ElMessage.warning('请上传数据文件')
    return
  }
  
  try {
    const res = await genChart(uploadFile.value, {
      goal: aiForm.goal,
      chartType: aiForm.chartType,
      name: aiForm.name,
    })
    
    if (res.code === 200 && res.data) {
      // 解析后端返回的图表数据
      const chartData = res.data
      
      if (!aiChartRef.value) return
      
      if (aiChart) {
        aiChart.dispose()
      }
      aiChart = echarts.init(aiChartRef.value)
      
      let option = {}
      
      // 根据图表类型生成配置
      if (aiForm.chartType === 'bar') {
        option = {
          title: {
            text: aiForm.name,
            left: 'center',
          },
          tooltip: {
            trigger: 'axis',
          },
          xAxis: {
            type: 'category',
            data: chartData.categories || [],
          },
          yAxis: {
            type: 'value',
          },
          series: [
            {
              data: chartData.values || [],
              type: 'bar',
              itemStyle: {
                color: '#409EFF',
              },
            },
          ],
        }
      } else if (aiForm.chartType === 'line') {
        option = {
          title: {
            text: aiForm.name,
            left: 'center',
          },
          tooltip: {
            trigger: 'axis',
          },
          xAxis: {
            type: 'category',
            data: chartData.categories || [],
          },
          yAxis: {
            type: 'value',
          },
          series: [
            {
              data: chartData.values || [],
              type: 'line',
              smooth: true,
            },
          ],
        }
      } else if (aiForm.chartType === 'pie') {
        option = {
          title: {
            text: aiForm.name,
            left: 'center',
          },
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)',
          },
          series: [
            {
              name: aiForm.name,
              type: 'pie',
              radius: '50%',
              data: chartData.data || [],
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
      }
      
      aiChart.setOption(option)
      aiChartData.value = option
      
      ElMessage.success('图表生成成功')
    } else {
      ElMessage.error(res.msg || '图表生成失败')
    }
  } catch (error) {
    console.error('生成图表失败:', error)
    ElMessage.error('生成图表失败：' + (error.message || '未知错误'))
  }
}

// 窗口大小变化时重新渲染图表
const handleResize = () => {
  borrowChart?.resize()
  typeChart?.resize()
  aiChart?.resize()
}

onMounted(() => {
  initBorrowChart()
  initTypeChart()
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped>
.statistics-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
</style>
