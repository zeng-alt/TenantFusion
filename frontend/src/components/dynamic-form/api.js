/**********************************
 * 动态表单组件 API —— 对接 workflow-module 运行态表单接口
 * 后端路径：/v1/form/code、/v1/form-data（经 vite 代理 /api/camunda → 8081）
 **********************************/

import { request } from '@/utils'

export default {
  /** 按模板编码获取已发布表单定义（运行态填表用） */
  definitionByCode(code) {
    return request.get(`/camunda/v1/form/code/${code}`)
  },

  /** 提交表单数据（服务端按已发布定义校验） */
  submitFormData(payload) {
    return request.post('/camunda/v1/form-data', payload)
  },

  /** 预校验表单数据（不落库，返回 字段名 → 错误文案 映射） */
  validateFormData(payload) {
    return request.post('/camunda/v1/form-data/validate', payload)
  },
}
