import request from '@/utils/request'

/**
 * 图书管理员功能 API
 */

// 借阅图书
export function borrowBook(data) {
  return request({
    url: '/bookadmin/borrow_book',
    method: 'post',
    data,
  })
}

// 查询图书是否逾期 (是否有借出)
export function queryBookExpire(bookNumber) {
  return request({
    url: `/bookadmin/query_book/${bookNumber}`,
    method: 'get',
  })
}

// 获取图书逾期信息
export function queryExpireInfo(bookNumber) {
  return request({
    url: `/bookadmin/query_expire/${bookNumber}`,
    method: 'get',
  })
}

// 归还图书
export function returnBook(data) {
  return request({
    url: '/bookadmin/return_book',
    method: 'post',
    data,
  })
}

// 获取还书报表
export function getReturnStatement(data) {
  return request({
    url: '/bookadmin/get_return_statement',
    method: 'post',
    data,
  })
}

// 获取借书报表
export function getBorrowStatement(data) {
  return request({
    url: '/bookadmin/get_borrow_statement',
    method: 'post',
    data,
  })
}

// 获取公告列表
export function getNoticeList(data) {
  return request({
    url: '/bookadmin/get_noticelist',
    method: 'post',
    data,
  })
}

// 添加公告
export function addNotice(data) {
  return request({
    url: '/bookadmin/add_notice',
    method: 'post',
    data,
  })
}

// 删除公告
export function deleteNotice(noticeId) {
  return request({
    url: `/bookadmin/delete_notice/${noticeId}`,
    method: 'get',
  })
}

// 获取公告详情
export function getNotice(noticeId) {
  return request({
    url: `/bookadmin/get_notice/${noticeId}`,
    method: 'get',
  })
}

// 更新公告
export function updateNotice(noticeId, data) {
  return request({
    url: `/bookadmin/update_notice/${noticeId}`,
    method: 'put',
    data,
  })
}

// 获取用户信息 (通过借阅证号)
export function getUserByCardId(cardId) {
  return request({
    url: `/user/get_user_by_card_number/${cardId}`,
    method: 'get',
  })
}


// 获取图书列表 (用于借书选择)
export function getAvailableBooks(data) {
  return request({
    url: '/user/search_book_page',
    method: 'post',
    data,
  })
}
