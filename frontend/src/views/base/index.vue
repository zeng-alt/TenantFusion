<script setup lang="js">
import { BpmnProcessViewer } from '@zeng-alt/camunda7-ui'
import { ref } from 'vue'

const bpmnXml = '<?xml version="1.0" encoding="UTF-8"?>\n<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">\n  <bpmn:process id="Process_uye82uqzs" name="test1" isExecutable="true" camunda:versionTag="1" camunda:historyTimeToLive="180">\n    <bpmn:startEvent id="StartEvent_1" name="开始">\n      <bpmn:outgoing>Flow_18s9qhg</bpmn:outgoing>\n    </bpmn:startEvent>\n    <bpmn:endEvent id="EndEvent_1" name="结束">\n      <bpmn:incoming>Flow_11n216k</bpmn:incoming>\n    </bpmn:endEvent>\n    <bpmn:userTask id="Activity_0awh700" name="审批" camunda:candidateUsers="admin">\n      <bpmn:extensionElements>\n        <camunda:formData>\n          <camunda:formField id="agree" label="同意" type="boolean" defaultValue="true" />\n        </camunda:formData>\n        <camunda:taskListener delegateExpression="${taskLoggingListener}" event="create" />\n      </bpmn:extensionElements>\n      <bpmn:incoming>Flow_18s9qhg</bpmn:incoming>\n      <bpmn:outgoing>Flow_1bbke3j</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id="Flow_1bbke3j" sourceRef="Activity_0awh700" targetRef="Activity_08ftj2s" />\n    <bpmn:sequenceFlow id="Flow_18s9qhg" sourceRef="StartEvent_1" targetRef="Activity_0awh700" />\n    <bpmn:userTask id="Activity_08ftj2s" name="审批1" camunda:candidateUsers="admin">\n      <bpmn:extensionElements>\n        <camunda:formData>\n          <camunda:formField id="agree" label="同意" type="boolean" defaultValue="true" />\n        </camunda:formData>\n        <camunda:taskListener class="${taskLoggingListener}" event="assignment" />\n      </bpmn:extensionElements>\n      <bpmn:incoming>Flow_1bbke3j</bpmn:incoming>\n      <bpmn:outgoing>Flow_11n216k</bpmn:outgoing>\n    </bpmn:userTask>\n    <bpmn:sequenceFlow id="Flow_11n216k" sourceRef="Activity_08ftj2s" targetRef="EndEvent_1" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id="BPMNDiagram_1">\n    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_uye82uqzs">\n      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">\n        <dc:Bounds x="156" y="102" width="36" height="36" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">\n        <dc:Bounds x="692" y="102" width="36" height="36" />\n        <bpmndi:BPMNLabel>\n          <dc:Bounds x="699" y="138" width="23" height="14" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id="Activity_0awh700_di" bpmnElement="Activity_0awh700">\n        <dc:Bounds x="260" y="80" width="100" height="80" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id="Activity_08ftj2s_di" bpmnElement="Activity_08ftj2s">\n        <dc:Bounds x="430" y="80" width="100" height="80" />\n        <bpmndi:BPMNLabel />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id="Flow_1bbke3j_di" bpmnElement="Flow_1bbke3j">\n        <di:waypoint x="360" y="120" />\n        <di:waypoint x="430" y="120" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id="Flow_18s9qhg_di" bpmnElement="Flow_18s9qhg">\n        <di:waypoint x="192" y="120" />\n        <di:waypoint x="260" y="120" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id="Flow_11n216k_di" bpmnElement="Flow_11n216k">\n        <di:waypoint x="530" y="120" />\n        <di:waypoint x="692" y="120" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>\n'

const mockUsers = [
  { label: '张三', value: 'zhangsan' },
  { label: '李四', value: 'lisi' },
  { label: '王五', value: 'wangwu' },
]

const mockGroups = [
  { label: '管理层', value: 'management' },
  { label: '工程部', value: 'engineering' },
]

async function onSearchUsers(name) {
  if (!name)
    return mockUsers
  return mockUsers.filter(
    u => u.label.includes(name) || u.value.includes(name.toLowerCase()),
  )
}

async function onSearchUserGroups(name) {
  if (!name)
    return mockGroups
  return mockGroups.filter(
    g => g.label.includes(name) || g.value.includes(name.toLowerCase()),
  )
}

const executionState = {
  processInstanceId: '4f7e2be5-96e2-11f1-a88a-0a002700000f',
  elements: {
    StartEvent_1: {
      status: 'completed',
      visitCount: 1,
      rejectCount: 0,
      assignee: null,
      candidateUsers: null,
      candidateGroups: null,
    },
    Activity_0awh700: {
      status: 'completed',
      visitCount: 1,
      rejectCount: 0,
      assignee: null,
      candidateUsers: null,
      candidateGroups: null,
    },
    Activity_08ftj2s: {
      status: 'active',
      visitCount: 1,
      rejectCount: 0,
      assignee: null,
      candidateUsers: null,
      candidateGroups: null,
    },
  },
  executionOrder: [
    'StartEvent_1',
    'Activity_0awh700',
    'Activity_08ftj2s',
  ],
  timestamps: [
    '2026-08-13 14:43:36',
    '2026-08-13 15:01:46',
    '2026-08-13 15:01:46',
  ],
  results: null,
}
</script>

<template>
  <CommonPage show-footer>
    <div class="h-screen w-screen bg-#f5f5f5">
      <BpmnProcessViewer
        :process-xml="bpmnXml"
        :execution-state="executionState"
        :on-search-users="onSearchUsers"
        :on-search-user-groups="onSearchUserGroups"
        show-timeline
      />
    </div>
  </CommonPage>
</template>
