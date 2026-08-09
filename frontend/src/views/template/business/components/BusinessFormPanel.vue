<template>
  <div class="business-form-panel h-full min-h-0 p-12">
    <NCard
      class="h-full min-h-0 flex flex-col"
      content-class="min-h-0 flex-1"
      content-style="display: flex; flex-direction: column; min-height: 0;"
      :bordered="true"
      size="small"
    >
      <!-- 卡片标题：业务名称 -->
      <template #header>
        <div class="min-w-0 flex items-center gap-8">
          <i class="i-material-symbols:account-tree text-16 text-primary" />
          <span class="truncate text-14 font-600">{{ business?.name || '配置表单' }}</span>
          <NTag
            v-if="business?.code"
            size="small"
            :bordered="false"
            type="info"
            class="font-mono"
          >
            {{ business.code }}
          </NTag>
        </div>
      </template>

      <!-- 卡片头部右侧：版本下拉 + 设计按钮 -->
      <template #header-extra>
        <template v-if="business">
          <template v-if="formConfigId">
            <NSelect
              v-model:value="selectedVersionId"
              size="small"
              class="w-200"
              placeholder="选择版本"
              :options="versionOptions"
              :loading="loading"
              @update:value="handleVersionChange"
            />
            <NButton
              size="small"
              type="primary"
              class="ml-8"
              :disabled="!selectedVersionId"
              @click="openDesigner"
            >
              <template #icon>
                <i class="i-carbon:draw text-14" />
              </template>
              设计
            </NButton>
          </template>
          <NTag
            v-else
            size="small"
            type="warning"
            :bordered="false"
          >
            未关联配置表单
          </NTag>
        </template>
      </template>

      <!-- 卡片内容：表单渲染 -->
      <NSpin
        :show="loading"
        class="h-full"
        content-style="height: 100%; display: flex; flex-direction: column;"
      >
        <!-- 未选择业务 -->
        <div
          v-if="!business"
          class="flex flex-col flex-1 items-center justify-center gap-10 text-center"
        >
          <div
            class="h-72 w-72 flex items-center justify-center rounded-full auto-bg-highlight"
          >
            <i class="i-material-symbols:view-agenda text-32 text-gray-400 dark:text-gray-500" />
          </div>
          <div class="text-14 text-gray-500 font-500 dark:text-gray-400">
            请选择左侧业务节点
          </div>
          <div class="max-w-240 text-12 text-gray-400 leading-5 dark:text-gray-500">
            选中业务后，将在此渲染其关联配置表单的最新版本
          </div>
        </div>

        <!-- 已选业务 + 已关联表单 -->
        <template v-else-if="business && formConfigId">
          <div
            v-if="selectedVersion"
            class="min-h-full flex flex-col"
          >
            <div class="mb-12 flex items-center gap-6">
              <i class="i-material-symbols:description text-16 text-primary" />
              <span class="text-14 font-600">{{ formName || business.name }}</span>
              <div class="flex-1" />
              <NTag
                size="small"
                :bordered="false"
                :type="versionStatusCfg(selectedVersion.status).type"
              >
                v{{ selectedVersion.version }} · {{ versionStatusCfg(selectedVersion.status).text }}
              </NTag>
            </div>

            <div class="min-h-0 flex flex-col flex-1">
              <FormRenderer
                class="min-h-0 flex-1"
                :fields="fields"
                mode="preview"
                :values="values"
                :disabled="true"
                :label-placement="labelPlacement"
                :label-width="labelWidth"
                :label-align="labelAlign"
                :size="formSize"
                empty-centered
                @update:field-value="handleFieldValue"
              />
            </div>
          </div>

          <!-- 已关联但无可用版本 -->
          <div
            v-else-if="!loading"
            class="flex flex-col flex-1 items-center justify-center gap-10 text-center"
          >
            <div
              class="h-64 w-64 flex items-center justify-center rounded-full auto-bg-highlight"
            >
              <i class="i-material-symbols:layers-outline text-28 text-gray-400 dark:text-gray-500" />
            </div>
            <div class="text-14 text-gray-500 font-500 dark:text-gray-400">
              暂无可用版本
            </div>
            <div class="max-w-220 text-12 text-gray-400 leading-5 dark:text-gray-500">
              该配置表单还没有已保存的版本，点击上方「设计」按钮创建版本
            </div>
            <NButton size="small" type="primary" @click="openDesigner">
              <template #icon>
                <i class="i-carbon:draw text-14" />
              </template>
              去设计
            </NButton>
          </div>
        </template>

        <!-- 已选业务但未关联表单 -->
        <div
          v-else-if="!loading"
          class="flex flex-col flex-1 items-center justify-center gap-10 text-center"
        >
          <div
            class="h-64 w-64 flex items-center justify-center rounded-full auto-bg-highlight"
          >
            <i class="i-material-symbols:link-off text-28 text-gray-400 dark:text-gray-500" />
          </div>
          <div class="text-14 text-gray-500 font-500 dark:text-gray-400">
            未关联配置表单
          </div>
          <div class="max-w-240 text-12 text-gray-400 leading-5 dark:text-gray-500">
            该业务尚未关联配置表单，可新建一个配置表单并自动关联
          </div>
          <div class="flex items-center gap-8">
            <NButton
              size="small"
              type="primary"
              @click="openBindModal"
            >
              <template #icon>
                <i class="i-material-symbols:add text-14" />
              </template>
              新增并关联
            </NButton>
          </div>
        </div>
      </NSpin>
    </NCard>

    <BusinessBindFormModal ref="bindModalRef" @success="handleBindSuccess" />
  </div>
</template>

<script setup>
import { NButton, NCard, NSelect, NSpin, NTag } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import formConfigApi from '../../form-config/api'
import FormRenderer from '../../form-config/components/renderer/FormRenderer.vue'
import { fieldValueKey } from '../../form-config/components/renderer/helpers'
import BusinessBindFormModal from './BusinessBindFormModal.vue'

defineOptions({ name: 'BusinessFormPanel' })

const props = defineProps({
  business: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['refresh'])

const router = useRouter()

const loading = ref(false)
const versions = ref([])
const selectedVersionId = ref(null)
const selectedVersion = ref(null)
const fields = ref([])
const values = reactive({})
const formName = ref('')
const bindModalRef = ref(null)

const formConfigId = computed(() => props.business?.formConfigId || null)

const VERSION_STATUS_MAP = {
  DRAFT: { text: '草稿', type: 'warning' },
  PUBLISHED: { text: '已发布', type: 'success' },
  OFFLINE: { text: '已下线', type: 'default' },
}

function versionStatusCfg(status) {
  return VERSION_STATUS_MAP[status] || { text: status || '—', type: 'default' }
}

/** 版本下拉选项：最新版本带 last 标签 */
const versionOptions = computed(() =>
  (versions.value || []).map((v, index) => ({
    label: `v${v.version} · ${versionStatusCfg(v.status).text}${index === 0 ? ' (last)' : ''}`,
    value: v.versionId,
  })),
)

const labelPlacement = computed(() => selectedVersion.value?.labelPlacement || 'left')
const labelWidth = computed(() => selectedVersion.value?.labelWidth ?? 90)
const labelAlign = computed(() => selectedVersion.value?.labelAlign || 'right')
const formSize = computed(() => selectedVersion.value?.formSize || 'medium')

watch(
  () => props.business,
  (node) => {
    if (!node || !node.businessId) {
      reset()
      return
    }
    load()
  },
  { immediate: true },
)

function reset() {
  versions.value = []
  selectedVersionId.value = null
  selectedVersion.value = null
  fields.value = []
  formName.value = ''
  Object.keys(values).forEach((key) => {
    delete values[key]
  })
}

async function load() {
  reset()
  const configId = formConfigId.value
  if (!configId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const { data: list } = await formConfigApi.versions(configId)
    versions.value = list || []
    if (versions.value.length) {
      selectedVersionId.value = versions.value[0].versionId
      await loadVersionDetail(selectedVersionId.value)
    }
  }
  catch (error) {
    console.error(error)
    $message.error('加载版本列表失败')
  }
  finally {
    loading.value = false
  }
}

async function handleVersionChange(versionId) {
  if (!versionId)
    return
  await loadVersionDetail(versionId)
}

async function loadVersionDetail(versionId) {
  loading.value = true
  try {
    const { data } = await formConfigApi.versionDetail(versionId)
    selectedVersion.value = data
    formName.value = props.business?.name || ''
    fields.value = data?.fields || []
    initValues(fields.value, values)
  }
  catch (error) {
    console.error(error)
    $message.error('加载表单定义失败')
  }
  finally {
    loading.value = false
  }
}

/** 依据字段默认值填充预览值（LIST 创建行数组，OBJECT 子字段平铺） */
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

/** 写入只读预览值（disabled 下控件不会触发，但保留兼容） */
function handleFieldValue({ field, value }) {
  const key = fieldValueKey(field)
  if (key)
    values[key] = value
}

function openDesigner() {
  if (!selectedVersionId.value)
    return
  const { href } = router.resolve({
    path: '/template/form-config/design',
    query: {
      id: selectedVersionId.value,
      formConfigId: formConfigId.value,
      code: props.business?.code || '',
      name: props.business?.name || '',
    },
  })
  window.open(href, '_blank')
}

function openBindModal() {
  bindModalRef.value?.handleOpen({
    id: props.business?.businessId,
    title: `新增并关联配置表单 - ${props.business?.name || ''}`,
  })
}

function handleBindSuccess() {
  emit('refresh', props.business)
}

defineExpose({
  load,
})
</script>
