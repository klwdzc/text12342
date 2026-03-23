import request from '@/utils/request'

/**
 * 规则管理 API
 */

// 获取规则列表 (分页)
export function getRuleListByPage(data) {
  return request({
    url: '/admin/get_rulelist_page',
    method: 'post',
    data,
  })
}

// 添加规则
export function addBookRule(data) {
  return request({
    url: '/admin/add_rule',
    method: 'post',
    data,
  })
}

// 获取规则详情
export function getBookRuleById(ruleId) {
  return request({
    url: `/admin/get_rule_ruleid/${ruleId}`,
    method: 'get',
  })
}

// 更新规则
export function updateBookRule(data) {
  return request({
    url: '/admin/update_rule',
    method: 'post',
    data,
  })
}

// 删除规则
export function deleteBookRule(ruleId) {
  return request({
    url: `/admin/delete_rule/${ruleId}`,
    method: 'delete',
  })
}

// 获取所有规则 (不分页，用于用户端显示)
export function getAllRules() {
  return request({
    url: '/user/get_rulelist',
    method: 'get',
  })
}
