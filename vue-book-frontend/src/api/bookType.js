import request from '@/utils/request'

/**
 * 图书分类管理 API
 */

export function getBookTypeListByPage(data) {
  return request({
    url: '/admin/get_booktype_page',
    method: 'post',
    data,
  })
}

export function addBookType(data) {
  return request({
    url: '/admin/add_booktype',
    method: 'post',
    data,
  })
}

export function getBookTypeById(typeId) {
  return request({
    url: `/admin/get_booktype/${typeId}`,
    method: 'get',
  })
}

export function updateBookType(data) {
  return request({
    url: '/admin/update_booktype',
    method: 'post',
    data,
  })
}

export function deleteBookType(typeId) {
  return request({
    url: `/admin/delete_booktype/${typeId}`,
    method: 'get',
  })
}
