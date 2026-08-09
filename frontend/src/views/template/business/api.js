/**********************************
 * 业务管理 API —— 对接 workflow-module 业务管理接口
 * 后端路径：/v1/business（经 vite 代理 /api/camunda → 8081）
 **********************************/

import { request } from '@/utils'

export default {
  /** 业务扁平列表 */
  list: () => request.get('/camunda/v1/business'),

  /** 业务树 */
  tree: () => request.get('/camunda/v1/business/tree'),

  /** 业务详情 */
  detail: id => request.get(`/camunda/v1/business/${id}`),

  /** 创建业务 */
  create: data => request.post('/camunda/v1/business', data),

  /** 更新业务 */
  update: (id, data) => request.put(`/camunda/v1/business/${id}`, data),

  /** 删除业务 */
  delete: id => request.delete(`/camunda/v1/business/${id}`),

  /** 创建配置表单并关联到业务 */
  createAndBindFormConfig: (id, data) => request.post(`/camunda/v1/business/${id}/form-config`, data),
}
