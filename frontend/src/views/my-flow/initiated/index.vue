<template>
  <CommonPage>
    <template #action>
      <NButton type="primary" @click="handleStart">
        <i class="i-material-symbols:add mr-4 text-14" />
        发起流程
      </NButton>
    </template>

    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.initiated"
      row-key="id"
      expand
      :scroll-x="1600"
    >
      <MeQueryItem label="流程名称" :label-width="70">
        <NInput
          v-model:value="queryItems.name"
          type="text"
          placeholder="请输入流程名称"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="业务Key" :label-width="65">
        <NInput
          v-model:value="queryItems.businessKey"
          type="text"
          placeholder="请输入业务Key"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="流程状态" :label-width="65">
        <NSelect
          v-model:value="queryItems.status"
          clearable
          :options="statusOptions"
          placeholder="请选择流程状态"
        />
      </MeQueryItem>
    </MeCrud>

    <MeModal ref="startModalRef" width="760px">
      <div class="max-h-560 overflow-y-auto pr-4">
        <NForm
          ref="startFormRef"
          label-placement="left"
          :label-width="90"
          :model="startForm"
        >
          <n-form-item path="workflow" :rule="required">
            <template #label>
              发起流程
            </template>
            <WorkflowSelect
              v-model:value="startForm.workflow"
              placeholder="请选择要发起的流程"
              @change="handleProcessDefinitionChange"
            />
          </n-form-item>
          <n-form-item v-if="workflowVersionList.length" path="workflowVersionId" label="流程版本">
            <NSelect
              v-model:value="selectedWorkflowVersionId"
              :options="workflowVersionOptions"
              filterable
              placeholder="请选择流程版本"
            />
          </n-form-item>
          <n-form-item path="businessKey" label="业务Key">
            <NInputGroup>
              <NInput
                v-model:value="startForm.businessKey"
                type="text"
                placeholder="默认为业务编码，可修改（可选）"
                clearable
              />
              <NButton type="primary" ghost @click="generateBusinessKey">
                <template #icon>
                  <i class="i-carbon:renew text-14" />
                </template>
                自动生成
              </NButton>
            </NInputGroup>
          </n-form-item>
          <n-form-item v-if="formVersions.length" path="formVersionId" label="表单版本">
            <NSelect
              v-model:value="selectedVersionId"
              :options="formVersionOptions"
              filterable
              placeholder="请选择表单版本（默认为最新）"
              @update:value="handleVersionChange"
            />
          </n-form-item>
        </NForm>

        <template v-if="selectedBusiness">
          <NCard size="small" class="mt-4">
            <template #header>
              <span class="text-13">配置表单 · {{ selectedBusiness.name }}</span>
            </template>
            <NSpin :show="formLoading">
              <template v-if="formFields.length">
                <FormRenderer
                  :fields="formFields"
                  mode="preview"
                  :values="formValues"
                  :errors="formErrors"
                  :label-placement="formLabelPlacement"
                  :label-width="formLabelWidth"
                  :label-align="formLabelAlign"
                  :size="formSize"
                  empty-description="该配置表单暂无可用字段"
                  @update:field-value="handleFormValue"
                />
              </template>
              <NAlert
                v-else-if="!formLoading"
                type="info"
                :bordered="false"
                class="mt-4"
              >
                该业务未关联配置表单或尚无已发布版本，可直接发起流程
              </NAlert>
            </NSpin>
          </NCard>
        </template>

        <NDivider class="mt-4">
          <span class="text-13">自定义属性</span>
        </NDivider>
        <CustomAttrsBuilder v-model="customAttrs" />
      </div>
    </MeModal>
  </CommonPage>
</template>

<script setup>
import { NAlert, NButton, NCard, NDivider, NInput, NInputGroup, NSelect, NSpin } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CommonPage, MeCrud, MeModal, MeQueryItem, WorkflowSelect } from '@/components'
import { formatDate, formatDateTime, randomKey } from '@/utils'
import formConfigApi from '@/views/template/form-config/api'
import FormRenderer from '@/views/template/form-config/components/renderer/FormRenderer.vue'
import { fieldValueKey } from '@/views/template/form-config/components/renderer/helpers'
import { collectErrors } from '@/views/template/form-config/components/renderer/validation'
import api from '../api'
import CustomAttrsBuilder from '../components/CustomAttrsBuilder.vue'
import { PROCESS_STATUS_MAP, renderStatusTag } from '../renderers'

defineOptions({ name: 'MyFlowInitiated' })

const router = useRouter()
const $table = ref(null)
const queryItems = ref({})

const statusOptions = Object.entries(PROCESS_STATUS_MAP).map(([value, cfg]) => ({
  label: cfg.text,
  value,
}))

onMounted(() => {
  $table.value?.handleSearch()
})

const required = {
  required: true,
  message: '此为必填项',
}

const startModalRef = ref(null)
const startFormRef = ref(null)
const startForm = ref({ workflow: null, businessKey: '' })
const customAttrs = ref([])
const businessListCache = ref([])
const selectedBusiness = ref(null)
const workflowVersionList = ref([])
const selectedWorkflowVersionId = ref(null)
const formLoading = ref(false)
const formFields = ref([])
const formVersions = ref([])
const selectedVersionId = ref(null)
const formValues = reactive({})
const formErrors = ref({})
const formLabelPlacement = ref('left')
const formLabelWidth = ref(90)
const formLabelAlign = ref('right')
const formSize = ref('medium')

/** 可用版本下拉选项（仅已发布版本，最新版本排最后） */
const formVersionOptions = computed(() =>
  formVersions.value.map(v => ({
    label: `v${v.version}${v.current ? '（当前）' : ''}`,
    value: v.versionId,
  })),
)

const WORKFLOW_VERSION_STATUS_TEXT = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }

/** 流程版本下拉选项（未发布状态禁用） */
const workflowVersionOptions = computed(() =>
  workflowVersionList.value.map(v => ({
    label: `v${v.version}${v.current ? '（当前）' : ''}${v.status === 'PUBLISHED' ? '' : `（${WORKFLOW_VERSION_STATUS_TEXT[v.status] || v.status}）`}`,
    value: v.versionId,
    disabled: v.status !== 'PUBLISHED',
  })),
)

async function loadBusinesses() {
  try {
    const res = await api.businessList()
    businessListCache.value = res.data || []
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '加载业务列表失败')
  }
}

/** 清空配置表单填写值与校验错误 */
function clearFormValues() {
  Object.keys(formValues).forEach((key) => {
    delete formValues[key]
  })
  formErrors.value = {}
}

function resetFormConfig() {
  selectedBusiness.value = null
  workflowVersionList.value = []
  selectedWorkflowVersionId.value = null
  formFields.value = []
  formVersions.value = []
  selectedVersionId.value = null
  clearFormValues()
}

/** 加载流程版本（非已发布状态禁用，默认选中当前/最新已发布版本） */
async function loadWorkflowVersions(workflowId) {
  workflowVersionList.value = []
  selectedWorkflowVersionId.value = null
  if (!workflowId)
    return
  try {
    const { data } = await api.versions(workflowId)
    const list = (data || []).slice().sort((a, b) => a.version - b.version)
    workflowVersionList.value = list
    const published = list.filter(v => v.status === 'PUBLISHED')
    const picked = published.find(v => v.current) || published[published.length - 1]
    if (picked)
      selectedWorkflowVersionId.value = picked.versionId
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '加载流程版本失败')
  }
}

/** 选择流程后：根据流程模板绑定的业务分类，加载其关联配置表单 */
async function handleProcessDefinitionChange(workflow) {
  resetFormConfig()
  startFormRef.value?.restoreValidation()
  if (!workflow)
    return
  loadWorkflowVersions(workflow.workflowId)
  if (!businessListCache.value.length) {
    await loadBusinesses()
  }
  const category = workflow.category || ''
  const business = businessListCache.value.find(item => item.code === category || item.name === category)
  if (!business) {
    $message.warning(category
      ? `未找到分类「${category}」对应的业务，可直接发起但无配置表单`
      : '该流程未绑定业务分类，可直接发起但无配置表单')
    return
  }
  await loadBusinessForm(business)
}

/** 加载业务关联的配置表单（仅已发布版本可选，默认最新） */
async function loadBusinessForm(business) {
  selectedBusiness.value = business
  if (!startForm.value.businessKey?.trim())
    startForm.value.businessKey = business.code || ''

  const configId = business.formConfigId
  if (!configId) {
    formVersions.value = []
    selectedVersionId.value = null
    return
  }

  formLoading.value = true
  try {
    const { data: versions } = await formConfigApi.versions(configId)
    const usable = (versions || [])
      .filter(v => v.status === 'PUBLISHED')
      .sort((a, b) => a.version - b.version)
    formVersions.value = usable
    const picked = usable[usable.length - 1]
    if (!picked) {
      selectedVersionId.value = null
      formFields.value = []
      return
    }
    selectedVersionId.value = picked.versionId
    await applyVersionById(picked.versionId)
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '加载配置表单失败')
  }
  finally {
    formLoading.value = false
  }
}

/** 按版本ID加载并应用表单定义 */
async function applyVersionById(versionId) {
  formLoading.value = true
  try {
    const { data } = await formConfigApi.versionDetail(versionId)
    clearFormValues()
    applyVersion(data)
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '加载配置表单失败')
  }
  finally {
    formLoading.value = false
  }
}

function handleVersionChange(versionId) {
  if (versionId == null) {
    formFields.value = []
    clearFormValues()
    return
  }
  applyVersionById(versionId)
}

function applyVersion(version) {
  formFields.value = version?.fields || []
  formLabelPlacement.value = version?.labelPlacement || 'left'
  formLabelWidth.value = version?.labelWidth ?? 90
  formLabelAlign.value = version?.labelAlign || 'right'
  formSize.value = version?.formSize || 'medium'
  initValues(formFields.value, formValues)
}

/** 依据字段默认值填充表单值（LIST 创建行数组，OBJECT 子字段平铺） */
function initValues(list, target) {
  for (const field of list || []) {
    if (field.hidden)
      continue
    const key = fieldValueKey(field)
    if (field.fieldType === 'LIST') {
      if (key)
        target[key] = [buildRow(field)]
    }
    else if (field.children?.length) {
      initValues(field.children, target)
    }
    else if (key) {
      target[key] = field.defaultValue ?? null
    }
  }
}

function buildRow(listField) {
  const row = {}
  for (const child of listField.children || []) {
    if (child.hidden)
      continue
    const key = fieldValueKey(child)
    if (child.fieldType === 'LIST') {
      if (key)
        row[key] = [buildRow(child)]
    }
    else if (key) {
      row[key] = child.defaultValue ?? null
    }
  }
  return row
}

/** 收集配置表单填写值 */
function handleFormValue({ field, value }) {
  const key = fieldValueKey(field)
  if (key)
    formValues[key] = value
}

/** 将自定义属性归一化为流程变量（日期/日期时间转字符串） */
function normalizeCustomAttrs() {
  const vars = {}
  for (const attr of customAttrs.value || []) {
    const key = attr?.name?.trim()
    if (!key)
      continue
    let value = attr.value
    if (attr.type === 'date' && value != null)
      value = formatDate(value)
    else if (attr.type === 'datetime' && value != null)
      value = formatDateTime(value)
    vars[key] = value
  }
  return vars
}

/** 自动生成业务Key（以业务编码为前缀，未选业务时用 BIZ） */
function generateBusinessKey() {
  const prefix = selectedBusiness.value?.code
    || startForm.value.businessKey?.split('_')[0]
    || 'BIZ'
  startForm.value.businessKey = randomKey(prefix)
}

function handleStart() {
  startForm.value = { workflow: null, businessKey: '' }
  customAttrs.value = []
  resetFormConfig()
  startFormRef.value?.restoreValidation()
  loadBusinesses()
  startModalRef.value?.open({
    title: '发起流程',
    onOk: handleSubmit,
  })
}

async function handleSubmit() {
  startModalRef.value.okLoading = true
  try {
    await startFormRef.value?.validate()
  }
  catch {
    startModalRef.value.okLoading = false
    return false
  }
  const errors = collectErrors(formFields.value, formValues)
  if (Object.keys(errors).length) {
    formErrors.value = errors
    $message.warning('请完善配置表单中的必填项')
    startModalRef.value.okLoading = false
    return false
  }
  try {
    const selectedVersion = workflowVersionList.value.find(
      v => v.versionId === selectedWorkflowVersionId.value,
    )
    const configFormVersion = formVersions.value.find(v => v.versionId === selectedVersionId.value)?.version
    const variables = {
      businessId: selectedBusiness.value?.businessId,
      businessCode: selectedBusiness.value?.code,
      businessName: selectedBusiness.value?.name,
      formConfigVersion: configFormVersion,
      ...normalizeCustomAttrs(),
    }
    if (formFields.value.length)
      variables.processForm = { ...formValues }
    await api.startProcess({
      processDefinitionKey: startForm.value.workflow?.workflowKey,
      processDefinitionId: selectedVersion?.processDefinitionId || undefined,
      businessKey: startForm.value.businessKey?.trim()
        || selectedBusiness.value?.code
        || undefined,
      variables,
    })
    $message.success('流程发起成功')
    $table.value?.handleSearch()
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '发起失败')
    throw error
  }
  finally {
    startModalRef.value.okLoading = false
  }
}

function handleDetail(processInstanceId) {
  router.push({ path: `/my-flow/detail/${processInstanceId}` })
}

function handleCancel(processInstanceId) {
  const dialog = $dialog.warning({
    title: '撤回流程',
    content: '确认撤回该流程实例？撤回后流程将被终止，不可恢复。',
    positiveText: '确认撤回',
    negativeText: '再想想',
    async onPositiveClick() {
      try {
        dialog.loading = true
        await api.cancel(processInstanceId)
        $message.success('撤回成功')
        dialog.loading = false
        $table.value?.handleSearch()
      }
      catch (error) {
        console.error(error)
        $message.error(error?.message || '撤回失败')
        dialog.loading = false
      }
    },
  })
}

const columns = [
  {
    title: '流程名称',
    key: 'processDefinitionName',
    width: 180,
    ellipsis: { tooltip: true },
  },
  {
    title: '流程定义Key',
    key: 'processDefinitionKey',
    width: 180,
    ellipsis: { tooltip: true },
  },
  {
    title: '业务Key',
    key: 'businessKey',
    width: 160,
    ellipsis: { tooltip: true },
  },
  {
    title: '发起人',
    key: 'startUserId',
    width: 100,
    render: row => row.startUserName || row.startUserId || '—',
  },
  {
    title: '流程状态',
    key: 'status',
    width: 120,
    render: ({ status }) => renderStatusTag(status),
  },
  {
    title: '当前节点',
    key: 'currentTaskName',
    width: 140,
  },
  {
    title: '当前处理人',
    key: 'currentAssignee',
    width: 120,
  },
  {
    title: '发起时间',
    key: 'startTime',
    width: 160,
  },
  {
    title: '结束时间',
    key: 'endTime',
    width: 160,
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    align: 'left',
    fixed: 'right',
    render({ id, processInstanceId, status }) {
      const instanceId = processInstanceId || id
      return [
        h(
          NButton,
          {
            size: 'small',
            type: 'info',
            onClick: () => handleDetail(instanceId),
          },
          {
            default: () => '详情',
            icon: () => h('i', { class: 'i-carbon:overflow-menu-vertical text-14' }),
          },
        ),
        status === 'running'
          ? h(
              NButton,
              {
                size: 'small',
                type: 'warning',
                style: 'margin-left: 8px;',
                onClick: () => handleCancel(instanceId),
              },
              {
                default: () => '撤回',
                icon: () => h('i', { class: 'i-carbon:close text-14' }),
              },
            )
          : null,
      ]
    },
  },
]
</script>
