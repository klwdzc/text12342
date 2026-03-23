import request from '@/utils/request'

/**
 * AI 智能推荐 API
 */

// AI 推荐图书
export function aiRecommend(data) {
  return request({
    url: '/user/ai_intelligent',
    method: 'post',
    data,
  })
}

// 获取用户 AI 聊天历史记录
export function getAiHistory(userId) {
  return request({
    url: `/user/ai_list_information/${userId}`,
    method: 'get',
  })
}
