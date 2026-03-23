import request from '@/utils/request'

/**
 * 系统管理员登录相关 API
 */
export function adminLogin(data) {
  return request({
    url: '/admin/login',
    method: 'post',
    data,
  })
}

export function getAdminData(data) {
  return request({
    url: '/admin/getData',
    method: 'post',
    data,
  })
}

/**
 * 图书管理员登录相关 API
 */
export function bookAdminLogin(data) {
  return request({
    url: '/bookadmin/login',
    method: 'post',
    data,
  })
}

export function getBookAdminData(data) {
  return request({
    url: '/bookadmin/getData',
    method: 'post',
    data,
  })
}

/**
 * 读者用户登录相关 API
 */
export function userLogin(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data,
  })
}

export function getUserData(data) {
  return request({
    url: '/user/getData',
    method: 'post',
    data,
  })
}
