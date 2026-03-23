import request from '@/utils/request'

/**
 * 弹幕评论 API
 */

// 获取弹幕列表
export function getCommentList() {
  return request({
    url: '/user/get_commentlist',
    method: 'get',
  })
}

// 添加弹幕
export function addComment(data) {
  return request({
    url: '/user/add_comment',
    method: 'post',
    data,
  })
}
