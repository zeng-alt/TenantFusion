<template>
  <NLayout class="h-full min-h-0 flex flex-col" :native-scrollbar="false">
    <!-- 加载中 -->
    <div v-if="loading && !processInfo" class="min-h-0 flex flex-1 items-center justify-center">
      <NSpin />
    </div>
    <!-- 加载失败 / 无数据 -->
    <div v-else-if="!processInfo" class="min-h-0 flex flex-1 items-center justify-center">
      <NEmpty :description="loadError || '未找到流程信息'" />
    </div>

    <!-- 顶部区域：主内容 + 右侧用户任务表单（可折叠） -->
    <NLayoutHeader v-else class="h-[300px] min-h-[200px] shrink-0">
      <NLayout
        :has-sider="!isEnded"
        sider-placement="right"
        class="h-full"
      >
        <!-- 左侧主内容：流程信息 + 发起表单信息 -->
        <NLayoutContent class="h-full min-h-0 overflow-hidden">
          <NScrollbar class="h-full">
            <div class="flex flex-col gap-20 p-10">
              <!-- 流程信息 -->
              <section>
                <h3 class="mb-12 flex items-center gap-6 text-14 font-600">
                  <NIcon size="15">
                    <i class="i-carbon:document" />
                  </NIcon>
                  流程信息
                </h3>
                <div v-if="processRows.length" class="grid grid-cols-2 gap-x-24 gap-y-10 xl:grid-cols-4">
                  <div
                    v-for="row in processRows"
                    :key="row.label"
                    class="min-w-0 flex items-baseline gap-4"
                  >
                    <span class="shrink-0 whitespace-nowrap text-13 text-gray-500">{{ row.label }}:</span>
                    <NEllipsis class="min-w-0 flex-1 text-13 text-gray-800 dark:text-gray-200">
                      {{ row.value }}
                    </NEllipsis>
                    <NButton
                      v-if="row.claimable"
                      text
                      size="tiny"
                      type="success"
                      class="mr-8"
                      :loading="claiming"
                      @click="handleClaim(claimableTaskId)"
                    >
                      认领
                    </NButton>
                    <NButton
                      v-if="row.unclaimable"
                      text
                      size="tiny"
                      type="warning"
                      class="mr-8"
                      :loading="unclaiming"
                      @click="handleUnclaim(currentTaskId)"
                    >
                      取消认领
                    </NButton>
                  </div>
                </div>
                <div v-else class="py-8 text-13 text-gray-400">
                  暂无流程信息
                </div>
              </section>

              <!-- 发起表单信息 -->
              <section>
                <h3 class="mb-12 flex items-center gap-6 text-14 font-600">
                  <NIcon size="15">
                    <i class="i-carbon:data-table" />
                  </NIcon>
                  发起表单信息
                </h3>
                <div v-if="formRows.length" class="grid grid-cols-2 gap-x-24 gap-y-10 xl:grid-cols-4">
                  <template v-for="(row, index) in formRows" :key="index">
                    <div v-if="row.type === 'group'" class="col-span-2 xl:col-span-4">
                      <div class="flex items-center gap-6 text-13 text-gray-600 font-600 dark:text-gray-300">
                        <i class="i-carbon:folder-details text-13" />
                        {{ row.label }}
                      </div>
                    </div>
                    <div v-else class="min-w-0 flex items-baseline gap-4">
                      <span class="shrink-0 whitespace-nowrap text-13 text-gray-500">{{ row.label }}:</span>
                      <NEllipsis class="min-w-0 flex-1 text-13 text-gray-800 dark:text-gray-200">
                        {{ row.value }}
                      </NEllipsis>
                    </div>
                  </template>
                </div>
                <div v-else class="py-8 text-13 text-gray-400">
                  暂无发起表单信息
                </div>
              </section>
            </div>
          </NScrollbar>
        </NLayoutContent>

        <!-- 右侧：用户任务表单（流程结束后不展示） -->
        <NLayoutSider
          v-if="!isEnded"
          class="h-full"
          collapse-mode="transform"
          :collapsed-width="0"
          :width="280"
          :native-scrollbar="false"
          :collapsed="approvalCollapsed"
          show-trigger="bar"
          bordered
          @update:collapsed="val => emit('update:approvalCollapsed', val)"
        >
          <div class="h-full min-h-0 flex flex-col">
            <div class="shrink-0 border-b border-gray-100 px-10 py-8 text-13 font-600 dark:border-gray-800">
              用户任务表单
            </div>
            <div class="min-h-0 flex-1 overflow-y-auto p-10">
              <TaskFormPanel ref="formPanelRef" :task-forms="currentTaskForms" />
            </div>
            <div class="shrink-0 border-t border-gray-100 p-10 dark:border-gray-800">
              <NButton
                type="primary"
                block
                :loading="completing"
                @click="handleComplete"
              >
                提交
              </NButton>
            </div>
          </div>
        </NLayoutSider>
      </NLayout>
    </NLayoutHeader>

    <!-- 底部：Tabs 流程图信息 + 全局表单 -->
    <NLayoutContent
      bordered
      class="min-h-0 flex-1 overflow-hidden"
    >
      <NCard
        bordered
        size="small"
        class="h-full min-h-0 flex flex-col"
      >
        <NTabs
          type="bar"
          size="small"
          class="h-full min-h-0 flex flex-col"
        >
          <NTabPane
            name="process"
            tab="流程图信息"
            class="h-full min-h-0"
          >
            <div class="relative h-full min-h-400 w-full">
              <BuilderProvider :config="formBuilderConfig">
                <!--
                必须等 processXml 就绪后再渲染 BpmnProcessViewer，
                否则 processXml 与 executionState 会在同一 tick 内异步加载后同时变更，
                BpmnProcessViewer 的 executionState watcher 会在 BPMN XML 尚未导入完成时
                尝试添加高亮 marker，导致 elementRegistry 中找不到元素而报错。
                base/index.vue 中数据是静态的、在组件挂载时就位，因此不会触发该竞态。
              -->
                <div class="h-600">
                  <BpmnProcessViewer
                    v-if="processXml"
                    :theme="isDark ? 'dark' : 'light'"
                    :process-xml="processXml"
                    :show-timeline="true"
                    :execution-state="executionState"
                  />
                </div>
              </BuilderProvider>
            </div>
          </NTabPane>

          <NTabPane
            name="global-form"
            tab="全局表单"
            class="h-full min-h-0"
          >
            <NScrollbar class="h-full">
              <div class="px-8 pb-8">
                <FormSchemaRenderer
                  v-if="formDefinition || formSchema"
                  v-model="formValues"
                  :http="request"
                  :definition="formDefinition"
                  :schema="formSchema"
                  :actions="false"
                  label-position="top"
                />
                <div v-else class="flex items-center justify-center py-32">
                  <NEmpty
                    size="large"
                    description="暂无全局表单"
                  />
                </div>
              </div>
            </NScrollbar>
          </NTabPane>
        </NTabs>
      </NCard>
    </NLayoutContent>
  </NLayout>
</template>

<script setup>
import { useDark } from '@vueuse/core'
import { BpmnProcessViewer } from '@zeng-alt/camunda7-ui'
import { BuilderProvider, FormSchemaRenderer } from '@zeng-alt/formkit-form-builder'
import { NButton, NCard, NEllipsis, NEmpty, NIcon, NLayout, NLayoutContent, NLayoutHeader, NLayoutSider, NScrollbar, NSpin, NTabPane, NTabs } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useUserStore } from '@/store'
import { formatDateTime, request } from '@/utils'
import { isAdmin, isSuperAdmin } from '@/utils/auth'
import { fieldValueKey, isComposite } from '@/views/template/form-config/components/renderer/helpers'
import { createFormBuilderConfig } from '@/views/template/form/formBuilderConfig'
import api from '../api'
import TaskFormPanel from './TaskFormPanel.vue'

defineOptions({ name: 'ProcessRuntimeViewer' })

const props = defineProps({
  /** 流程实例ID，组件自加载详情数据 */
  processInstanceId: {
    type: String,
    required: true,
  },
  showTimeline: {
    type: Boolean,
    default: false,
  },
  formDefinition: {
    type: Object,
    default: null,
  },
  formSchema: {
    type: Array,
    default: null,
  },
  formValues: {
    type: Object,
    default: () => ({}),
  },
  approvalCollapsed: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:formValues', 'update:approvalCollapsed', 'status'])

const isDark = useDark()
const formBuilderConfig = createFormBuilderConfig()

const loading = ref(false)
const loadError = ref('')
const claiming = ref(false)
const unclaiming = ref(false)
const completing = ref(false)
/** TaskFormPanel 实例引用，用于收集表单数据 */
const formPanelRef = ref(null)
/** 由 api.detail 返回的详情数据 */
const loadedDetail = ref(null)

const formValues = computed({
  get: () => props.formValues,
  set: value => emit('update:formValues', value),
})

/** 流程信息（详情数据） */
const processInfo = computed(() => loadedDetail.value)

/** 流程是否已结束或挂起（非 running 状态下不再展示用户节点表单，避免用户操作） */
const isEnded = computed(() => {
  const status = processInfo.value?.status
  return status !== 'running'
})

/** 通知父组件流程状态变化（用于标题状态标签等） */
watch(processInfo, (info) => {
  emit('status', info?.status || '')
}, { immediate: true })

const currentTaskForms = computed(() => {
  if (isEnded.value)
    return []
  return loadedDetail.value?.currentTaskForms || []
})

/** 当前活动任务ID（用于认领/取消认领/完成） */
const currentTaskId = computed(() => currentTaskForms.value?.[0]?.taskId || '')

/** 当前活动任务ID（用于认领） */
const claimableTaskId = computed(() => currentTaskId.value)

/** 可完成任务ID（用于提交表单） */
const completeTaskId = computed(() => currentTaskId.value)

const processXml = computed(() => loadedDetail.value?.processXml || '')
const executionState = computed(() => loadedDetail.value?.executionState || null)
const configForm = computed(() => loadedDetail.value?.configForm || null)
const processForm = computed(() => loadedDetail.value?.processForm || {})

async function loadDetail() {
  if (!props.processInstanceId)
    return
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await api.detail(props.processInstanceId)
    loadedDetail.value = data
  }
  catch (error) {
    console.error(error)
    loadError.value = error?.message || '加载失败'
  }
  finally {
    loading.value = false
  }
}

watch(() => props.processInstanceId, loadDetail, { immediate: true })

function canUnclaim(assignee) {
  const userStore = useUserStore()
  return !!assignee && (userStore?.username === assignee || isAdmin() || isSuperAdmin())
}

async function handleClaim(taskId) {
  if (!taskId || claiming.value)
    return
  claiming.value = true
  try {
    await api.claim(taskId, useUserStore()?.username || '')
    $message.success('认领成功')
    await loadDetail()
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '认领失败')
  }
  finally {
    claiming.value = false
  }
}

async function handleUnclaim(taskId) {
  if (!taskId || unclaiming.value)
    return
  unclaiming.value = true
  try {
    await api.unclaim(taskId)
    $message.success('已取消认领')
    await loadDetail()
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '取消认领失败')
  }
  finally {
    unclaiming.value = false
  }
}

async function handleComplete() {
  const taskId = completeTaskId.value
  if (!taskId || completing.value)
    return
  completing.value = true
  try {
    const variables = formPanelRef.value?.getFormValues() || {}
    await api.complete({ taskId, variables })
    $message.success('提交成功')
    await loadDetail()
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '提交失败')
  }
  finally {
    completing.value = false
  }
}

/** 供父组件在认领后刷新 */
defineExpose({ reload: loadDetail })

const STATUS_TEXT = {
  running: '进行中',
  completed: '已完成',
  terminated: '已终止',
  suspended: '已挂起',
}

/** 流程信息（标题 + 值，一行最多四列） */
const processRows = computed(() => {
  const p = processInfo.value || {}
  const running = p.status === 'running' || p.state === 'active'
  const assignee = p.currentAssignee || ''
  const rows = [
    { label: '流程名称', value: p.processDefinitionName },
    { label: '流程定义Key', value: p.processDefinitionKey },
    { label: '流程实例ID', value: p.id },
    { label: '业务Key', value: p.businessKey },
    { label: '发起人', value: p.initiator || p.startUserName || p.startUserId },
    { label: '发起时间', value: p.startTime ? formatDateTime(p.startTime) : '' },
    { label: '结束时间', value: p.endTime ? formatDateTime(p.endTime) : '' },
    { label: '状态', value: p.status ? STATUS_TEXT[p.status] || p.status : '' },
    { label: '当前节点', value: p.currentTaskName },
    { label: '当前审核人', value: assignee || '未认领', claimable: running && !assignee && !!currentTaskId.value, unclaimable: running && !!assignee && canUnclaim(assignee) && !!currentTaskId.value },
  ]
  return rows.filter(row => row.value !== null && row.value !== undefined && row.value !== '')
})

const formRows = computed(() => buildRows(configForm.value?.fields || [], processForm.value || {}))

/** 将配置表单字段树 + 提交值 展开为紧凑排版行（group 分组 / field 键值对） */
function buildRows(fields, values) {
  const rows = []
  for (const field of fields || []) {
    if (field.hidden)
      continue
    const key = fieldValueKey(field)
    const value = values?.[key]
    if (field.fieldType === 'LIST' && field.children?.length) {
      rows.push({ type: 'group', label: field.fieldLabel || '列表' })
      const list = Array.isArray(value) ? value : []
      if (!list.length) {
        rows.push({ type: 'field', label: '内容', value: '—' })
      }
      else {
        list.forEach((row, i) => {
          rows.push({ type: 'group', label: `${field.fieldLabel || '行'} ${i + 1}` })
          rows.push(...buildRows(field.children, row))
        })
      }
    }
    else if (isComposite(field.fieldType) && field.children?.length) {
      if (field.fieldLabel)
        rows.push({ type: 'group', label: field.fieldLabel })
      rows.push(...buildRows(field.children, values))
    }
    else {
      rows.push({ type: 'field', label: field.fieldLabel || key || '—', value: formatValue(field, value) })
    }
  }
  return rows
}

function formatValue(field, value) {
  if (value === null || value === undefined || value === '')
    return '—'
  if (field.fieldType === 'BOOLEAN')
    return value === true || value === 'true' ? '是' : '否'
  if (field.fieldType === 'SELECT' || field.fieldType === 'MULTI_SELECT') {
    const list = Array.isArray(value) ? value : [value]
    return list.map(v => optionLabel(field.options, v) ?? v).join('、')
  }
  if (Array.isArray(value))
    return value.map(v => (v && typeof v === 'object' ? (v.name || v.url || JSON.stringify(v)) : v)).join('、')
  if (value && typeof value === 'object')
    return value.name || value.url || JSON.stringify(value)
  return value
}

function optionLabel(options, value) {
  return (options || []).find(opt => String(opt.value) === String(value))?.label ?? null
}
</script>
