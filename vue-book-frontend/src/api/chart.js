import request from '@/utils/request'

/**
 * 图表生成 API
 */

// AI 生成图表
export function genChart(file, data) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('goal', data.goal)
  formData.append('chartType', data.chartType)
  formData.append('name', data.name)
  
  return request({
    url: '/admin/gen',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

// 获取借阅量数据
export function getBorrowData() {
  return request({
    url: '/admin/get_borrowdata',
    method: 'get',
  })
}

// 获取借阅分类统计数据
export function getBorrowTypeStatistic() {
  return request({
    url: '/admin/get_borrowtype_statistics',
    method: 'get',
  })
}
