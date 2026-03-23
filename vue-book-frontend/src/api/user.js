import request from '@/utils/request'

/**
 * 用户管理 API
 */

// 获取借阅证列表 (用户列表)
export function getStatementList(data) {
  return request({
    url: '/admin/get_statementlist',
    method: 'post',
    data,
  })
}

// 添加借阅证
export function addStatement(data) {
  return request({
    url: '/admin/add_statement',
    method: 'post',
    data,
  })
}

// 获取用户信息 (用于回显借阅证)
export function getStatementByUserId(userId) {
  return request({
    url: `/admin/get_statement/${userId}`,
    method: 'get',
  })
}

// 更新借阅证信息
export function updateStatement(data) {
  return request({
    url: '/admin/update_statement',
    method: 'post',
    data,
  })
}

// 删除借阅证
export function deleteStatementByUserId(userId) {
  return request({
    url: `/admin/delete_statement/${userId}`,
    method: 'delete',
  })
}

// 获取用户信息 (通过用户 ID)
export function getUserByUserId(userId) {
  return request({
    url: `/user/get_information/${userId}`,
    method: 'get',
  })
}

// 修改密码
export function updatePassword(data) {
  return request({
    url: '/user/update_password',
    method: 'post',
    data,
  })
}

// 获取我的借阅记录
export function getMyBorrowPage(data) {
  return request({
    url: '/user/get_bookborrow',
    method: 'post',
    data,
  })
}

// 获取违章信息
export function getViolationList(data) {
  return request({
    url: '/user/get_violation',
    method: 'post',
    data,
  })
}

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
