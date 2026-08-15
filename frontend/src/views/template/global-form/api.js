/**********************************
 * 全局表单 API —— 全局表单数据查询 + 按流程模板解析表单定义
 * 后端路径：/v1/global-form-data、/v1/global-form（经 vite 代理 /api/camunda → 8081）
 **********************************/

import { request } from '@/utils'

export default {
  /** 分页查询全局表单数据 */
  read(params = {}) {
    return request.get('/camunda/v1/global-form-data', {
      params: { ...params, pageNo: params.pageNo ?? params.page, pageSize: params.pageSize },
    })
  },

  /** 按流程模板编码解析全局表单定义（用于数据预览） */
  definition(workflowCode) {
    return request.get('/camunda/v1/global-form/definition', { params: { workflowCode } })
  },
}
