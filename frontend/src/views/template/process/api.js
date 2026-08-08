/**********************************
 * 流程模板 API —— 对接 workflow-module 流程管理接口
 * 后端路径：/v1/workflow（经 vite 代理 /api/workflow → 8081）
 **********************************/

import { request } from '@/utils'

export function newProcessKey() {
  return `Process_new_${Date.now()}`
}

export function defaultBpmnXml(key = newProcessKey(), name = '未命名流程', version = '1.0', historyTimeToLive = 180) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="${key}" name="${name}" isExecutable="true" camunda:versionTag="${version}" camunda:historyTimeToLive="${historyTimeToLive}">
    <bpmn:startEvent id="StartEvent_1" name="开始" />
    <bpmn:userTask id="Task_1" name="审批" />
    <bpmn:endEvent id="EndEvent_1" name="结束" />
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1" />
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="EndEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${key}">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="156" y="102" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Task_1_di" bpmnElement="Task_1">
        <dc:Bounds x="252" y="80" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="412" y="102" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="192" y="120" />
        <di:waypoint x="252" y="120" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="352" y="120" />
        <di:waypoint x="412" y="120" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
}

export default {
  /** 分页查询流程 */
  list: params => request.get('/camunda/v1/workflow', { params }),

  /** 流程详情 */
  detail: id => request.get(`/camunda/v1/workflow/${id}`),

  /** 创建流程 */
  create: data => request.post('/camunda/v1/workflow', data),

  /** 更新流程主数据 */
  update: (id, data) => request.put(`/camunda/v1/workflow/${id}`, data),

  /** 删除流程 */
  delete: id => request.delete(`/camunda/v1/workflow/${id}`),

  /** 流程版本列表 */
  versions: id => request.get(`/camunda/v1/workflow/${id}/versions`),

  /** 版本详情（含 BPMN XML） */
  versionDetail: versionId => request.get(`/camunda/v1/workflow/versions/${versionId}`),

  /** 版本详情（不含 BPMN XML） */
  versionDetailByVersion(templateId, version) {
    return request.get(`/camunda/v1/workflow/versions/${templateId}/${version}`)
  },

  /** 保存流程草稿 */
  saveDraft: (id, data) => request.post(`/camunda/v1/workflow/${id}/draft`, data),

  /** 保存并发布流程 */
  saveAndPublish: (id, data) => request.post(`/camunda/v1/workflow/${id}/publish`, data),

  /** 发布流程版本 */
  publish: versionId => request.post(`/camunda/v1/workflow/versions/${versionId}/publish`),

  /** 挂起流程 */
  offline: id => request.post(`/camunda/v1/workflow/${id}/offline`),
}
