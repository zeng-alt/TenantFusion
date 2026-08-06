/**********************************
 * 我的流程 mock API —— 无数据库表，localStorage 建模
 * 后端 workflow-module 就绪后替换为真实接口：
 *   todo:      request.get('/workflow/tasks', { params: { assignee: 'me', ... } })
 *   done:      request.get('/workflow/tasks/history', { params })
 *   initiated: request.get('/workflow/process-instances', { params: { startedBy: 'me', ... } })
 *   copied:    request.get('/workflow/tasks/copied', { params })
 *   detail:    request.get('/workflow/instances/{processInstanceId}')
 *   process:   request.get('/workflow/tasks/{taskId}')
 *   complete:  request.post('/workflow/tasks/{taskId}/complete', { action, comment, variables })
 *   cancel:    request.delete('/workflow/instances/{processInstanceId}', { params: { reason } })
 **********************************/

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

function addHours(dateStr, hours) {
  const d = new Date(dateStr)
  d.setHours(d.getHours() + hours)
  return d.toISOString().slice(0, 19).replace('T', ' ')
}

function now() {
  return new Date().toISOString().slice(0, 19).replace('T', ' ')
}

const DEFINITION_STEPS = {
  Process_leave_approval: [
    { name: '部门经理审批', assignee: '部门经理' },
    { name: 'HR复核', assignee: 'HR专员' },
  ],
  Process_expense_claim: [
    { name: '财务审核', assignee: '财务' },
  ],
}

function buildHistory(record) {
  const steps = DEFINITION_STEPS[record.processDefinitionKey] || []
  const startTime = record.startTime || record.createTime || ''
  const currentName = record.taskName || record.currentTaskName || ''
  const finished = record.status === 'completed'
  const history = [{
    nodeName: '流程发起',
    assignee: record.initiator || record.startUserName || '我',
    startTime,
    endTime: startTime,
    result: 'start',
    status: 'completed',
    comment: '发起流程',
  }]

  if (finished) {
    steps.forEach((step, index) => {
      const handled = step.name === currentName
      history.push({
        nodeName: step.name,
        assignee: step.assignee,
        startTime: addHours(startTime, (index + 1) * 6),
        endTime: record.endTime || addHours(startTime, (index + 1) * 6 + 1),
        result: handled ? (record.action || 'approve') : 'approve',
        status: 'completed',
        comment: handled ? (record.comment || '') : '同意',
      })
    })
  }
  else {
    for (const [index, step] of steps.entries()) {
      const isCurrent = step.name === currentName
      history.push({
        nodeName: step.name,
        assignee: step.assignee,
        startTime: isCurrent ? record.createTime || startTime : addHours(startTime, (index + 1) * 6),
        endTime: isCurrent ? '' : addHours(startTime, (index + 1) * 6 + 1),
        result: isCurrent ? '' : 'approve',
        status: isCurrent ? 'running' : 'completed',
        comment: isCurrent ? '' : '同意',
      })
      if (isCurrent)
        break
    }
  }
  return history
}

function seed() {
  const baseTime = '2026-08-01 09:00:00'
  return [
    // 待办任务 (当前用户为 assignee/candidate)
    { id: 'task-1001', type: 'todo', processInstanceId: 'pi-2001', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', taskId: 'task-1001', taskName: '部门经理审批', assignee: 'currentUser', candidateUsers: '', createTime: baseTime, dueTime: addDays(baseTime, 2), status: 'pending', businessKey: 'LEAVE-20260801-001', initiator: '张三' },
    { id: 'task-1002', type: 'todo', processInstanceId: 'pi-2002', processDefinitionKey: 'Process_expense_claim', processDefinitionName: '报销流程', taskId: 'task-1002', taskName: '财务审核', assignee: 'currentUser', candidateUsers: '', createTime: addDays(baseTime, 1), dueTime: addDays(baseTime, 3), status: 'pending', businessKey: 'EXP-20260802-001', initiator: '李四' },
    { id: 'task-1003', type: 'todo', processInstanceId: 'pi-2003', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', taskId: 'task-1003', taskName: 'HR复核', assignee: '', candidateUsers: 'currentUser,hr-group', createTime: addDays(baseTime, 2), dueTime: addDays(baseTime, 4), status: 'pending', businessKey: 'LEAVE-20260803-001', initiator: '王五' },

    // 已办任务 (当前用户已处理)
    { id: 'task-2001', type: 'done', processInstanceId: 'pi-3001', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', taskId: 'task-2001', taskName: '部门经理审批', assignee: 'currentUser', candidateUsers: '', createTime: addDays(baseTime, -5), dueTime: addDays(baseTime, -3), endTime: addDays(baseTime, -3), status: 'completed', action: 'approve', comment: '同意', businessKey: 'LEAVE-20260727-001', initiator: '赵六' },
    { id: 'task-2002', type: 'done', processInstanceId: 'pi-3002', processDefinitionKey: 'Process_expense_claim', processDefinitionName: '报销流程', taskId: 'task-2002', taskName: '财务审核', assignee: 'currentUser', candidateUsers: '', createTime: addDays(baseTime, -4), dueTime: addDays(baseTime, -2), endTime: addDays(baseTime, -2), status: 'completed', action: 'reject', comment: '发票不符', businessKey: 'EXP-20260728-001', initiator: '钱七' },
    { id: 'task-2003', type: 'done', processInstanceId: 'pi-3003', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', taskId: 'task-2003', taskName: 'HR复核', assignee: 'currentUser', candidateUsers: '', createTime: addDays(baseTime, -3), dueTime: addDays(baseTime, -1), endTime: addDays(baseTime, -1), status: 'completed', action: 'approve', comment: '', businessKey: 'LEAVE-20260729-001', initiator: '孙八' },

    // 我发起的流程实例
    { id: 'pi-4001', type: 'initiated', processInstanceId: 'pi-4001', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', businessKey: 'LEAVE-20260801-002', createTime: baseTime, startUserId: 'currentUser', startUserName: '我', status: 'running', currentTaskName: '部门经理审批', currentAssignee: '部门经理' },
    { id: 'pi-4002', type: 'initiated', processInstanceId: 'pi-4002', processDefinitionKey: 'Process_expense_claim', processDefinitionName: '报销流程', businessKey: 'EXP-20260802-002', createTime: addDays(baseTime, 1), startUserId: 'currentUser', startUserName: '我', status: 'completed', currentTaskName: '', currentAssignee: '', endTime: addDays(baseTime, 3) },
    { id: 'pi-4003', type: 'initiated', processInstanceId: 'pi-4003', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', businessKey: 'LEAVE-20260803-002', createTime: addDays(baseTime, 2), startUserId: 'currentUser', startUserName: '我', status: 'running', currentTaskName: 'HR复核', currentAssignee: 'HR专员' },

    // 抄送/知会
    { id: 'task-5001', type: 'copied', processInstanceId: 'pi-5001', processDefinitionKey: 'Process_leave_approval', processDefinitionName: '请假审批流程', taskId: 'task-5001', taskName: '部门经理审批', createTime: addDays(baseTime, -2), status: 'completed', action: 'approve', comment: '同意', businessKey: 'LEAVE-20260730-001', initiator: '周九' },
    { id: 'task-5002', type: 'copied', processInstanceId: 'pi-5002', processDefinitionKey: 'Process_expense_claim', processDefinitionName: '报销流程', taskId: 'task-5002', taskName: '财务审核', createTime: addDays(baseTime, -1), status: 'running', action: '', comment: '', businessKey: 'EXP-20260731-001', initiator: '吴十' },
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

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
}

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

export default {
  todo(params = {}) {
    return delay().then(() => {
      const filtered = applyFilters(filterByType('todo'), params)
      return { data: paginate(filtered, params) }
    })
  },
  done(params = {}) {
    return delay().then(() => {
      const filtered = applyFilters(filterByType('done'), params)
      return { data: paginate(filtered, params) }
    })
  },
  initiated(params = {}) {
    return delay().then(() => {
      const filtered = applyFilters(filterByType('initiated'), params)
      return { data: paginate(filtered, params) }
    })
  },
  copied(params = {}) {
    return delay().then(() => {
      const filtered = applyFilters(filterByType('copied'), params)
      return { data: paginate(filtered, params) }
    })
  },
  detail(processInstanceId) {
    return delay().then(() => {
      const hit = list.find(item => item.processInstanceId === processInstanceId)
      if (!hit)
        throw new Error('流程不存在')
      return { data: { ...hit, history: buildHistory(hit) } }
    })
  },
  process(taskId) {
    return delay().then(() => {
      const hit = list.find(item => item.taskId === taskId)
      if (!hit || hit.type !== 'todo')
        throw new Error('任务不存在或已处理')
      return { data: { ...hit, history: buildHistory(hit) } }
    })
  },
  complete(taskId, { action = 'approve', comment = '' } = {}) {
    return delay(500).then(() => {
      const index = list.findIndex(item => item.taskId === taskId)
      if (index === -1)
        throw new Error('任务不存在或已处理')
      const task = list[index]
      const record = {
        ...task,
        type: 'done',
        status: 'completed',
        action,
        comment,
        endTime: now(),
        assignee: 'currentUser',
        candidateUsers: '',
      }
      list.splice(index, 1, record)
      const inst = list.find(item => item.processInstanceId === task.processInstanceId && item.type === 'initiated')
      if (inst && inst.status === 'running') {
        const steps = DEFINITION_STEPS[inst.processDefinitionKey] || []
        const lastStep = steps[steps.length - 1]
        if (!lastStep || lastStep.name === task.taskName) {
          inst.status = 'completed'
          inst.currentTaskName = ''
          inst.currentAssignee = ''
          inst.endTime = now()
        }
      }
      persist()
      return { data: record }
    })
  },
  cancel(processInstanceId, reason = '发起人撤回') {
    return delay(500).then(() => {
      const inst = list.find(item => item.processInstanceId === processInstanceId && item.type === 'initiated')
      if (!inst)
        throw new Error('流程不存在')
      if (inst.status !== 'running')
        throw new Error('仅进行中的流程可撤回')
      inst.status = 'terminated'
      inst.currentTaskName = ''
      inst.currentAssignee = ''
      inst.endTime = now()
      inst.deleteReason = reason
      persist()
      return { data: inst }
    })
  },
}
