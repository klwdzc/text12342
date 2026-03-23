<template>
  <div class="violation-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的违章</span>
        </div>
      </template>

      <el-table :data="violationList" style="width: 100%" v-loading="loading">
        <el-table-column prop="violationType" label="违章类型" />
        <el-table-column prop="violationDesc" label="违章描述" />
        <el-table-column prop="violationDate" label="违章日期" />
        <el-table-column prop="fineAmount" label="罚款金额">
          <template #default="{ row }">
            <span style="color: red">¥{{ row.fineAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="处理状态">
          <template #default="{ row }">
            <el-tag :type="row.status === '已处理' ? 'success' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; text-align: right">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const violationList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 加载违章记录
const loadViolationList = async () => {
  try {
    loading.value = true
    const userId = localStorage.getItem('userId')
    if (!userId) {
      ElMessage.error('未找到用户 ID')
      return
    }

    const res = await request({
      url: '/user/get_violation',
      method: 'post',
      data: {
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        userId: parseInt(userId),
      },
    })

    if (res.code === 200 || res.status === 200) {
      violationList.value = res.data.records || []
      total.value = Number(res.data.total) || 0
    } else {
      // 没有数据时不显示错误提示，正常业务场景
      violationList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('加载违章记录失败:', error)
    // 只在真正出错时显示提示
    if (error.message && !error.message.includes('没有')) {
      ElMessage.error('加载违章记录失败')
    }
    violationList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  loadViolationList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadViolationList()
}

onMounted(() => {
  loadViolationList()
})
</script>

<style scoped>
.violation-container {
  height: 100%;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
