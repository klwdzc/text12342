import request from '@/utils/request'

/**
 * 图书管理员管理 API (系统管理员使用)
 */

// 获取图书管理员列表 (分页)
export function getBookAdminListByPage(data) {
  return request({
    url: '/admin/get_bookadminlist',
    method: 'post',
    data,
  })
}

// 添加图书管理员
export function addBookAdmin(data) {
  return request({
    url: '/admin/add_bookadmin',
    method: 'post',
    data,
  })
}

// 获取图书管理员详情
export function getBookAdminById(bookAdminId) {
  return request({
    url: `/admin/get_bookadmin/${bookAdminId}`,
    method: 'get',
  })
}

// 更新图书管理员
export function updateBookAdmin(data) {
  return request({
    url: '/admin/update_bookadmin',
    method: 'post',
    data,
  })
}

// 删除图书管理员
export function deleteBookAdminById(bookAdminId) {
  return request({
    url: `/admin/delete_bookadmin/${bookAdminId}`,
    method: 'delete',
  })
}
