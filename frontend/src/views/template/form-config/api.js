/**********************************
 * 配置表单 API —— 对接 workflow-module 配置表单管理接口
 * 后端路径：/v1/form-config（经 vite 代理 /api/camunda → 8081）
 **********************************/

import { request } from '@/utils'

export default {
  /** 分页查询配置表单（MeCrud 传 pageNo，后端约定 pageNo/pageSize，这里兼容 page 写法） */
  read(params = {}) {
    return request.get('/camunda/v1/form-config', {
      params: { ...params, pageNo: params.pageNo ?? params.page, pageSize: params.pageSize },
    })
  },

  /** 配置表单下拉选项（供业务关联选择） */
  options() {
    return request.get('/camunda/v1/form-config/options')
  },

  /** 配置表单详情 */
  detail: id => request.get(`/camunda/v1/form-config/${id}`),

  /** 创建配置表单 */
  create: data => request.post('/camunda/v1/form-config', data),

  /** 更新配置表单主数据 */
  update: data => request.put(`/camunda/v1/form-config/${data.formConfigId}`, data),

  /** 删除配置表单 */
  delete: id => request.delete(`/camunda/v1/form-config/${id}`),

  /** 配置表单版本列表 */
  versions: id => request.get(`/camunda/v1/form-config/${id}/versions`),

  /** 版本详情（含字段树） */
  versionDetail: versionId => request.get(`/camunda/v1/form-config/versions/${versionId}`),

  /** 版本详情（按模板+版本号） */
  versionDetailByVersion(templateId, version) {
    return request.get(`/camunda/v1/form-config/versions/${templateId}/${version}`)
  },

  /** 保存草稿（id 为 0 时自动创建模板主数据） */
  saveDraft(id, data) {
    return request.post(`/camunda/v1/form-config/${id || 0}/draft`, data)
  },

  /** 保存并发布草稿 */
  publishDraft(id, data) {
    return request.post(`/camunda/v1/form-config/${id || 0}/publish-draft`, data)
  },

  /** 发布版本 */
  publish: versionId => request.post(`/camunda/v1/form-config/versions/${versionId}/publish`),

  /** 下线版本 */
  offline: versionId => request.post(`/camunda/v1/form-config/versions/${versionId}/offline`),
}
