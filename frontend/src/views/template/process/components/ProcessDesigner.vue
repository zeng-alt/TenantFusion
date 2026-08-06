<template>
  <div class="relative h-full w-full flex flex-col">
    <div
      class="flex items-center justify-between border-b border-gray-200 px-16 py-8 dark:border-gray-700"
    >
      <div class="flex items-center">
        <i class="i-carbon:flow-modeler text-18" />
        <span class="ml-8 text-16 font-600">
          {{ workflow?.workflowName || '新建流程' }}
        </span>
        <NTag v-if="workflow" size="small" type="primary" bordered class="ml-8">
          v{{ workflow?.latestVersion ?? 1 }}
        </NTag>
      </div>
      <NSpace>
        <NButton @click="handleClose">
          取消
        </NButton>
        <NButton :loading="saving" type="primary" ghost @click="handleSave">
          保存
        </NButton>
        <NButton :loading="saving" type="primary" @click="handleSaveAndPublish">
          保存并发布
        </NButton>
      </NSpace>
    </div>

    <div v-if="loading" class="flex flex-1 items-center justify-center">
      <NSpin />
    </div>
    <div v-else class="relative min-h-0 flex-1">
      <BpmnModelerProcess
        ref="$bpmnRef"
        :xml="xml"
        :theme="isDark ? 'dark' : 'light'"
        size="small"
        :on-search-users="onSearchUsers"
        :on-search-user-groups="onSearchUserGroups"
        :on-search-java-classes="onSearchJavaClasses"
        :on-search-delegate-expressions="onSearchDelegateExpressions"
        :on-search-external-topics="onSearchExternalTopics"
        :on-fetch-process-list="onFetchProcessList"
        :on-search-decision-refs="onSearchDecisionRefs"
        :on-search-form-refs="onSearchFormRefs"
        :on-search-form-keys="onSearchFormKeys"
      />
    </div>
  </div>
</template>

<script setup>
import { useDark } from '@vueuse/core'
import { BpmnModelerProcess } from '@zeng-alt/camunda7-ui'
import { NButton, NSpace, NSpin, NTag } from 'naive-ui'
import { onMounted, ref } from 'vue'
import api, { defaultBpmnXml } from '../api'

defineOptions({ name: 'ProcessDesigner' })

const props = defineProps({
  template: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close', 'saved'])

const $bpmnRef = ref()
const isDark = useDark()
const loading = ref(true)
const saving = ref(false)
const workflow = ref(props.template)
const xml = ref(props.template ? '' : defaultBpmnXml())

onMounted(async () => {
  try {
    xml.value = await loadBpmnXml()
  }
  catch (error) {
    console.error(error)
    $message.error('加载流程定义失败')
    xml.value = defaultBpmnXml(workflow.value.workflowKey, workflow.value.workflowName, workflow.value.version)
  }
  finally {
    loading.value = false
  }
})

function delay(ms = 150) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

const mockUsers = [
  { label: '张三', value: 'zhangsan' },
  { label: '李四', value: 'lisi' },
  { label: '王五', value: 'wangwu' },
  { label: '赵六', value: 'zhaoliu' },
]
const mockGroups = [
  { label: '管理层', value: 'management' },
  { label: '工程部', value: 'engineering' },
  { label: '人力资源', value: 'hr' },
  { label: '财务部', value: 'finance' },
]
const mockJavaClasses = [
  { label: '通知服务', value: 'com.example.service.NotificationService' },
  { label: '审批监听器', value: 'com.example.listener.ApprovalListener' },
  { label: '邮件发送器', value: 'com.example.util.MailSender' },
]
const mockDelegateExpressions = [
  { label: '用户服务委托', value: `\${userService}` },
  { label: '审批服务委托', value: `\${approvalService}` },
]
const mockTopics = [
  { label: '订单处理', value: 'order-processing' },
  { label: '审批流转', value: 'approval-workflow' },
]
const mockProcessList = [
  { label: '请假审批流程', value: 'Process_leave_approval', version: ['1.0', '2.0'] },
  { label: '报销流程', value: 'Process_expense_claim', version: ['1.0'] },
]
const mockDecisions = [
  { label: '信用评估', value: 'Decision_credit_score', version: ['1.0'] },
]
const mockFormRefs = [
  { label: '请假申请表单', value: 'form-leave-request', version: ['1.0'] },
]
const mockFormKeys = [
  { label: '请假申请', value: 'embedded:app:leave-request.html' },
]

function filterItems(list, name) {
  if (!name)
    return list
  return list.filter(item => item.label.includes(name) || item.value.toLowerCase().includes(name.toLowerCase()))
}

async function onSearchUsers(name, pageNo = 1, pageSize = 20) {
  await delay()
  const filtered = filterItems(mockUsers, name)
  const start = (pageNo - 1) * pageSize
  return {
    pageNum: pageNo,
    pageSize,
    total: filtered.length,
    data: filtered.slice(start, start + pageSize),
  }
}

async function onSearchUserGroups(name) {
  await delay()
  return filterItems(mockGroups, name)
}

async function onSearchJavaClasses(name) {
  await delay()
  return filterItems(mockJavaClasses, name)
}

async function onSearchDelegateExpressions(name) {
  await delay()
  return filterItems(mockDelegateExpressions, name)
}

async function onSearchExternalTopics(name) {
  await delay()
  return filterItems(mockTopics, name)
}

async function onFetchProcessList() {
  await delay()
  return mockProcessList
}

async function onSearchDecisionRefs(name) {
  await delay()
  return filterItems(mockDecisions, name)
}

async function onSearchFormRefs(name) {
  await delay()
  return filterItems(mockFormRefs, name)
}

async function onSearchFormKeys(name) {
  await delay()
  return filterItems(mockFormKeys, name)
}

function handleClose() {
  emit('close')
}

async function loadBpmnXml() {
  const wf = workflow.value
  if (!wf?.workflowId)
    return defaultBpmnXml()
  const { data } = await api.versionDetail(workflow.value.id)
  return data?.bpmnXml || defaultBpmnXml(workflow.value.workflowKey, workflow.value.workflowName, workflow.value.version)
}

async function saveDraft() {
  const { xml } = await $bpmnRef.value?.getProcessInfo() || {}
  if (!xml)
    throw new Error('未能获取当前流程 XML')
  const { data } = await api.saveDraft(workflow.value?.workflowId, { bpmnXml: xml })
  return data
}

async function handleSave() {
  if (saving.value)
    return
  saving.value = true
  try {
    await saveDraft()
    $message.success('保存成功')
  }
  catch (error) {
    console.error(error)
    $message.error(`保存失败: ${error?.message || error}`)
  }
  finally {
    saving.value = false
  }
}

async function handleSaveAndPublish() {
  if (saving.value)
    return
  saving.value = true
  try {
    await api.saveAndPublish(workflow.value?.workflowId, { bpmnXml: xml })
    $message.success('保存并发布成功')
    emit('saved')
    emit('close')
  }
  catch (error) {
    console.error(error)
    $message.error(`保存并发布失败: ${error?.message || error}`)
  }
  finally {
    saving.value = false
  }
}
</script>
