import request from '@/utils/request'

/**
 * 图书管理相关 API
 */

// 获取图书列表（分页 + 搜索）
export function getBookList(data) {
  return request({
    url: '/admin/get_booklist',
    method: 'post',
    data,
  })
}

// 获取图书分类列表
export function getBookTypeList() {
  return request({
    url: '/admin/get_type',
    method: 'get',
  })
}

// 添加图书
export function addBook(data) {
  return request({
    url: '/admin/add_book',
    method: 'post',
    data,
  })
}

// 删除图书
export function deleteBook(bookId) {
  return request({
    url: `/admin/delete_book/${bookId}`,
    method: 'get',
  })
}

// 批量删除图书
export function deleteBookBatch(data) {
  return request({
    url: '/admin/delete_book_batch',
    method: 'delete',
    data,
  })
}

// 获取图书详情
export function getBookInfo(bookId) {
  return request({
    url: `/admin/get_bookinformation/${bookId}`,
    method: 'get',
  })
}

// 更新图书
export function updateBook(data) {
  return request({
    url: '/admin/update_book',
    method: 'post',
    data,
  })
}

// Excel 批量导入图书
export function uploadExcel(file) {
  const formData = new FormData()
  formData.append('files', file)
  return request({
    url: '/admin/updown',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
