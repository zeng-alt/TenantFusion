/**********************************
 * 流程选择组件 API —— 对接 workflow-module 流程管理接口
 * 后端路径：/v1/workflow（经 vite 代理 /api/camunda → 8081）
 **********************************/

import { request } from '@/utils'

export default {
  /** 分页查询流程（后端约定 pageNo/pageSize，这里兼容 page 写法） */
  list: (params = {}) => request.get('/camunda/v1/workflow', { params }),
}
