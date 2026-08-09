/**********************************
 * 我的流程 API
 *   todo / process / complete / detail / initiated / cancel：对接 workflow-module（8081，经 vite 代理 /api/camunda）
 *   done / copied：仍为 localStorage mock，后端就绪后替换
 **********************************/

import { useUserStore } from '@/store'
import { request } from '@/utils'

export const MY_FLOW_STORAGE_KEY = 'zeng-alt:my-flow:v1'

const STORAGE_KEY = MY_FLOW_STORAGE_KEY

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function addDays(dateStr, days) {
  const d = new Date(dateStr)
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 19).replace('T', ' ')
}

function seed() {
  const baseTime = '2026-08-01 09:00:00'
  return [
    // 待办任务 (当前用户为 assignee/candidate)
    { id: 'task-1001', type: 'todo', processInstanceId: 'pi-2001', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', taskId: 'task-1001', taskName: '部门经理审批', assignee: 'currentUser', candidateUsers: '', createTime: baseTime, dueTime: addDays(baseTime, 2), status: 'pending', businessKey: 'LEAVE-20260801-001', initiator: '张三' },

    // 已办任务 (当前用户已处理)
    { id: 'task-2001', type: 'done', processInstanceId: 'pi-3001', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', taskId: 'task-2001', taskName: '部门经理审批', assignee: 'currentUser', candidateUsers: '', createTime: addDays(baseTime, -5), dueTime: addDays(baseTime, -3), endTime: addDays(baseTime, -3), status: 'completed', action: 'approve', comment: '同意', businessKey: 'LEAVE-20260727-001', initiator: '赵六' },

    // 我发起的流程实例
    { id: 'pi-4001', type: 'initiated', processInstanceId: 'pi-4001', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', businessKey: 'LEAVE-20260801-002', createTime: baseTime, startUserId: 'currentUser', startUserName: '我', status: 'running', currentTaskName: '部门经理审批', currentAssignee: '部门经理' },

    // 抄送/知会
    { id: 'task-5001', type: 'copied', processInstanceId: 'pi-5001', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', taskId: 'task-5001', taskName: '部门经理审批', createTime: addDays(baseTime, -2), status: 'completed', action: 'approve', comment: '同意', businessKey: 'LEAVE-20260730-001', initiator: '周九' },
  ]
}

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw)
      return JSON.parse(raw)
  }
  catch {
    // ignore
  }
  return seed()
}

const list = load()

function filterByType(type) {
  return list.filter(item => item.type === type)
}

function paginate(data, params = {}) {
  const { pageNo = 1, pageSize = 10 } = params
  const start = (pageNo - 1) * pageSize
  return { total: data.length, pageData: data.slice(start, start + pageSize) }
}

function applyFilters(data, params = {}) {
  const { name = '', key = '', status, initiator = '', businessKey = '' } = params
  return data.filter((item) => {
    const hitName = !name || (item.processDefinitionName || '').includes(name)
    const hitKey = !key || (item.processDefinitionKey || '').includes(key)
    const hitStatus = status === undefined || status === null || status === '' || item.status === status
    const hitInitiator = !initiator || (item.initiator || item.startUserName || '').includes(initiator)
    const hitBizKey = !businessKey || (item.businessKey || '').includes(businessKey)
    return hitName && hitKey && hitStatus && hitInitiator && hitBizKey
  })
}

/** 由历史活动构建 BPMN 执行状态（供 BpmnProcessViewer 高亮/时间线使用） */
function buildExecutionState(processInstanceId, activities = []) {
  const elements = {}
  const sequenceFlows = {}
  const executionOrder = []
  const timestamps = []
  for (const act of activities) {
    const id = act.activityId
    if (!id)
      continue
    const done = !!act.endTime
    if (act.activityType === 'sequenceFlow') {
      const prev = sequenceFlows[id] || { status: 'pending', visitCount: 0 }
      sequenceFlows[id] = {
        status: done ? 'completed' : 'active',
        visitCount: prev.visitCount + 1,
      }
      continue
    }
    const prev = elements[id] || { status: 'pending', visitCount: 0, rejectCount: 0 }
    elements[id] = {
      status: done ? 'completed' : 'active',
      visitCount: prev.visitCount + 1,
      rejectCount: prev.rejectCount,
      assignee: act.assignee || prev.assignee,
    }
    executionOrder.push(id)
    timestamps.push(done ? act.endTime : act.startTime)
  }
  return { processInstanceId, elements, sequenceFlows, executionOrder, timestamps }
}

/** 当前登录用户（作为 Camunda 用户标识） */
function currentUserId() {
  return useUserStore()?.username || ''
}

export default {
  /** 待办任务：分页查询当前用户可办理/候选的任务 */
  todo(params = {}) {
    const { pageNo, pageSize, name, businessKey, initiator } = params
    return request.get('/camunda/v1/workflow/tasks', {
      params: {
        page: pageNo || 1,
        pageSize: pageSize || 10,
        userId: currentUserId(),
        processDefinitionName: name || undefined,
        businessKey: businessKey || undefined,
        initiator: initiator || undefined,
      },
    })
  },

  /** 任务详情 */
  process: taskId => request.get(`/camunda/v1/workflow/tasks/${taskId}`),

  /** 完成任务 */
  complete(taskId, { action = 'approve', comment = '' } = {}) {
    return request.post(`/camunda/v1/workflow/tasks/${taskId}/complete`, {
      comment,
      variables: { action },
    })
  },

  /** 流程实例流转记录（审批链） */
  activities: processInstanceId => request.get('/camunda/v1/workflow/history/activities', {
    params: { processInstanceId },
  }),

  /** 流程实例详情（实例信息 + 流转记录 + BPMN XML + 执行状态） */
  async detail(processInstanceId) {
    const [hpiRes, actRes] = await Promise.all([
      request.get(`/camunda/v1/workflow/history/process-instances/${processInstanceId}`),
      request.get('/camunda/v1/workflow/history/activities', {
        params: { processInstanceId },
      }),
    ])
    const hpi = hpiRes.data || {}
    const activities = actRes.data || []
    const runningAct = activities.find(act => !act.endTime)
    const STATUS_MAP = { active: 'running', completed: 'completed', deleted: 'terminated', suspended: 'suspended' }

    let processXml = ''
    const definitionId = activities.find(act => act.processDefinitionId)?.processDefinitionId
    if (definitionId) {
      try {
        const xmlRes = await request.get(`/camunda/v1/workflow/definitions/${definitionId}/bpmn-xml`)
        processXml = xmlRes.data || ''
      }
      catch (error) {
        console.error('加载BPMN XML失败', error)
      }
    }

    return {
      data: {
        ...hpi,
        initiator: hpi.startUserId,
        status: hpi.status || STATUS_MAP[hpi.state] || 'running',
        currentTaskName: hpi.currentTaskName || runningAct?.activityName || '',
        currentAssignee: hpi.currentAssignee || runningAct?.assignee || '',
        processXml,
        executionState: buildExecutionState(processInstanceId, activities),
        history: activities.map(act => ({
          nodeName: act.activityName,
          assignee: act.assignee,
          startTime: act.startTime,
          endTime: act.endTime,
          status: act.endTime ? 'completed' : 'running',
          result: '',
          comment: '',
        })),
      },
    }
  },

  /** 可发起的流程定义（仅最新、未挂起） */
  definitions(params = {}) {
    return request.get('/camunda/v1/workflow/definitions', {
      params: {
        latestVersion: true,
        suspended: false,
        pageNum: 1,
        pageSize: 100,
        ...params,
      },
    })
  },

  /** 业务列表（发起流程时选择业务，取业务关联的配置表单） */
  businessList() {
    return request.get('/camunda/v1/business')
  },

  /** 按编码查询流程模板（用于取绑定的业务分类） */
  workflowByKey(key) {
    return request.get('/camunda/v1/workflow', {
      params: { workflowKey: key, page: 1, pageSize: 20 },
    })
  },

  /** 发起流程 */
  startProcess(payload) {
    return request.post('/camunda/v1/workflow/instances', {
      ...payload,
      startUserId: currentUserId(),
    })
  },

  /** 我发起的流程实例：分页查询当前用户发起的实例 */
  initiated(params = {}) {
    const { pageNo, pageSize, name, businessKey, status } = params
    return request.get('/camunda/v1/workflow/history/process-instances', {
      params: {
        pageNum: pageNo || 1,
        pageSize: pageSize || 10,
        startUserId: currentUserId(),
        processDefinitionName: name || undefined,
        businessKey: businessKey || undefined,
        state: status || undefined,
      },
    })
  },

  done(params = {}) {
    return delay().then(() => {
      const filtered = applyFilters(filterByType('done'), params)
      return { data: paginate(filtered, params) }
    })
  },
  copied(params = {}) {
    return delay().then(() => {
      const filtered = applyFilters(filterByType('copied'), params)
      return { data: paginate(filtered, params) }
    })
  },
  cancel(processInstanceId, reason = '发起人撤回') {
    return request.delete(`/camunda/v1/workflow/instances/${processInstanceId}`, {
      params: { reason },
    })
  },
}
