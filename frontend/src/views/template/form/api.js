/**********************************
 * 表单模板 API —— 对接 workflow-module 动态表单管理接口
 * 后端路径：/v1/form（经 vite 代理 /api/camunda → 8081）
 **********************************/

import { request } from '@/utils'

export default {
  /** 分页查询表单模板（MeCrud 传 pageNo，后端约定 page/pageSize，这里做映射） */
  read(params = {}) {
    return request.get('/camunda/v1/form', {
      params: { ...params, page: params.pageNo ?? params.page, pageSize: params.pageSize },
    })
  },

  /** 表单模板详情 */
  detail: id => request.get(`/camunda/v1/form/${id}`),

  /** 创建表单模板 */
  create: data => request.post('/camunda/v1/form', data),

  /** 更新表单模板主数据 */
  update: data => request.put(`/camunda/v1/form/${data.formTemplateId}`, data),

  /** 删除表单模板 */
  delete: id => request.delete(`/camunda/v1/form/${id}`),

  /** 表单模板版本列表 */
  versions: id => request.get(`/camunda/v1/form/${id}/versions`),

  /** 版本详情（含 definition） */
  versionDetail: versionId => request.get(`/camunda/v1/form/versions/${versionId}`),

  /** 版本详情（不含 definition） */
  versionDetailByVersion(templateId, version) {
    return request.get(`/camunda/v1/form/versions/${templateId}/${version}`)
  },

  /** 保存表单草稿（id 为 0 时新建模板，definition 对象转 JSON 字符串存储） */
  saveDraft(id, data) {
    return request.post(`/camunda/v1/form/${id || 0}/draft`, {
      ...data,
      definition: data.definition ? JSON.stringify(data.definition) : data.definition,
    })
  },

  /** 保存并发布表单草稿（id 为 0 时新建模板，单个接口原子完成） */
  publishDraft(id, data) {
    return request.post(`/camunda/v1/form/${id || 0}/publish-draft`, {
      ...data,
      definition: data.definition ? JSON.stringify(data.definition) : data.definition,
    })
  },

  /** 发布表单版本 */
  publish: versionId => request.post(`/camunda/v1/form/versions/${versionId}/publish`),

  /** 下线表单版本 */
  offline: versionId => request.post(`/camunda/v1/form/versions/${versionId}/offline`),
}
