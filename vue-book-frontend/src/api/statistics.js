import request from '@/utils/request'

/**
 * 数据统计 API
 */

export function getBorrowData() {
  return request({
    url: '/admin/get_borrowdata',
    method: 'get',
  })
}

export function getBorrowTypeStatistic() {
  return request({
    url: '/admin/get_borrowtype_statistics',
    method: 'get',
  })
}

/**
 * 获取系统管理员首页统计数据
 * @returns {Promise} 包含图书总数、读者数量、今日借阅、违章记录
 */
export function getDashboardStatistics() {
  return request({
    url: '/admin/get_dashboard_statistics',
    method: 'get',
  })
}

/**
 * 获取图书管理员首页统计数据
 * @param bookAdminId 图书管理员 ID
 * @returns {Promise} 包含今日借阅、今日归还、待处理借阅、逾期图书
 */
export function getBookAdminDashboardStatistics(bookAdminId) {
  return request({
    url: '/bookadmin/get_dashboard_statistics',
    method: 'get',
    params: { bookAdminId }
  })
}

/**
 * 获取用户首页统计数据
 * @param userId 用户 ID
 * @returns {Promise} 包含当前借阅、历史借阅、即将逾期、违章记录
 */
export function getUserDashboardStatistics(userId) {
  return request({
    url: '/user/get_dashboard_statistics',
    method: 'get',
    params: { userId }
  })
}
