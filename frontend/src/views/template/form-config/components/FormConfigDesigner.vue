<template>
  <div class="form-config-designer h-full w-full flex flex-col bg-[#f6f7f9] dark:bg-[#111114]">
    <!-- 顶部工具栏 -->
    <header
      class="h-44 flex shrink-0 items-center gap-6 border-b border-light_border auto-bg px-8 transition-colors dark:border-dark_border"
    >
      <NTooltip>
        <template #trigger>
          <NButton
            size="small"
            quaternary
            circle
            @click="emit('close')"
          >
            <template #icon>
              <i class="i-material-symbols:arrow-back text-16" />
            </template>
          </NButton>
        </template>
        返回列表
      </NTooltip>

      <NDivider vertical />

      <div class="min-w-0 flex items-center gap-6">
        <i class="i-material-symbols:draw text-16 text-primary" />
        <span class="truncate text-14 font-600">{{ formSettings.name }}</span>
        <NTag
          v-if="formSettings.code"
          size="small"
          :bordered="false"
          type="info"
          class="font-mono"
        >
          {{ formSettings.code }}
        </NTag>
      </div>

      <NTag
        v-if="version"
        size="small"
        :bordered="false"
        :type="statusMeta.type"
      >
        <template #icon>
          <span class="h-6 w-6 rounded-full" :class="statusMeta.dot" />
        </template>
        v{{ version.version }} · {{ statusMeta.text }}
      </NTag>

      <div class="flex-1" />

      <div class="flex items-center gap-6">
        <NTooltip>
          <template #trigger>
            <span class="flex items-center gap-4 text-12 text-gray-400 dark:text-gray-500">
              <i class="i-material-symbols:view-agenda text-14" />
              {{ fieldCount }} 个字段
            </span>
          </template>
          当前表单共 {{ fieldCount }} 个字段（含子字段）
        </NTooltip>
        <NDivider vertical />
        <NButton
          size="small"
          :disabled="!fields.length"
          @click="showPreview = true"
        >
          <template #icon>
            <i class="i-material-symbols:visibility text-14" />
          </template>
          预览
        </NButton>
        <NButton
          size="small"
          :loading="saving"
          @click="handleSave"
        >
          <template #icon>
            <i class="i-material-symbols:save text-14" />
          </template>
          保存草稿
        </NButton>
        <NButton
          size="small"
          type="primary"
          :loading="saving"
          @click="handlePublish"
        >
          <template #icon>
            <i class="i-material-symbols:publish text-14" />
          </template>
          保存并发布
        </NButton>
      </div>
    </header>

    <!-- 主体三栏 -->
    <div class="min-h-0 flex flex-1 gap-10 p-10">
      <!-- 左栏：字段类型面板 -->
      <aside
        class="w-216 flex flex-col shrink-0 overflow-hidden border border-gray-200 rounded-8 auto-bg transition-colors dark:border-dark_border"
      >
        <div
          class="h-48 flex shrink-0 items-center gap-8 border-b border-light_border px-10 transition-colors dark:border-dark_border"
        >
          <span
            class="h-30 w-30 flex shrink-0 items-center justify-center rounded-8 auto-bg-highlight"
          >
            <i class="i-material-symbols:apps text-15 text-primary" />
          </span>
          <span class="text-13 font-600">字段库</span>
        </div>

        <div class="shrink-0 px-10 pt-10">
          <NInput
            v-model:value="typeSearch"
            size="small"
            placeholder="搜索字段类型"
            clearable
          >
            <template #prefix>
              <i class="i-material-symbols:search text-13" />
            </template>
          </NInput>
        </div>

        <div class="min-h-0 flex-1">
          <NScrollbar class="h-full">
            <div class="flex flex-col gap-8 p-10">
              <NCollapse
                v-model:expanded-names="expandedGroups"
                :bordered="false"
                arrow-placement="right"
                class="type-collapse"
              >
                <div
                  v-for="group in filteredTypeGroups"
                  :key="group"
                  class="mb-8 border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
                >
                  <NCollapseItem
                    :name="group"
                    class="type-collapse-item"
                  >
                    <template #header>
                      <div class="w-full flex items-center gap-6">
                        <i
                          class="text-13"
                          :class="groupIcon(group)"
                        />
                        <span class="text-12 text-gray-600 font-600 dark:text-gray-300">{{ group }}</span>
                        <div class="flex-1" />
                        <NTag
                          size="tiny"
                          :bordered="false"
                          round
                          type="info"
                        >
                          {{ filteredGroupedTypes[group]?.length || 0 }}
                        </NTag>
                      </div>
                    </template>
                    <div class="grid grid-cols-2 gap-6">
                      <NButton
                        v-for="type in filteredGroupedTypes[group]"
                        :key="type"
                        size="small"
                        dashed
                        class="w-full !h-30 !px-6"
                        @click="addField(type)"
                      >
                        <template #icon>
                          <i class="text-15" :class="[FIELD_TYPE_META[type].icon, typeColor(type)]" />
                        </template>
                        <span class="truncate text-12">{{ FIELD_TYPE_META[type].label }}</span>
                      </NButton>
                    </div>
                  </NCollapseItem>
                </div>
              </NCollapse>

              <NEmpty
                v-if="!filteredTypeGroups.length"
                size="small"
                description="未找到匹配的字段类型"
                class="py-16"
              />
            </div>
          </NScrollbar>
        </div>
      </aside>

      <!-- 中栏：字段列表 -->
      <div class="min-w-0 flex flex-col flex-1 overflow-hidden border border-gray-200 rounded-8 auto-bg transition-colors dark:border-dark_border">
        <!-- 上区：头部条 -->
        <div
          class="h-48 flex shrink-0 items-center gap-8 border-b border-light_border px-10 transition-colors dark:border-dark_border"
        >
          <span
            class="h-30 w-30 flex shrink-0 items-center justify-center rounded-8 auto-bg-highlight"
          >
            <i class="i-material-symbols:view-agenda text-15 text-primary" />
          </span>
          <span class="text-13 font-600">表单字段</span>
          <NTag
            size="tiny"
            :bordered="false"
            round
            type="primary"
          >
            {{ fieldCount }}
          </NTag>
          <div class="flex-1" />
          <span
            v-if="selectedField && isComposite(selectedField.fieldType)"
            class="flex items-center gap-4 text-11 text-primary"
          >
            <i class="i-material-symbols:info-outline text-12" />
            已选中{{ FIELD_TYPE_META[selectedField.fieldType].label }}，从字段库点击字段将加入其内部
          </span>
        </div>

        <!-- 下区：画布卡片 -->
        <div class="min-h-0 flex-1 p-10">
          <div
            class="h-full overflow-hidden border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <FormCanvasRenderer
              :fields="fields"
              :selected-field="selectedField"
              :values="designValues"
              :label-placement="formSettings.labelPlacement"
              :label-width="formSettings.labelWidth"
              :label-align="formSettings.labelAlign"
              :size="formSettings.formSize"
              :structure-errors="structureErrors"
              @select="selectedField = $event"
              @deselect="selectedField = null"
              @delete="handleDeleteField"
              @move="handleMoveField"
              @add="addField"
              @update:field-value="handleFieldValue"
            />
          </div>
        </div>
      </div>

      <!-- 右栏：未选中字段时编辑表单设置，选中字段时编辑字段属性 -->
      <aside
        class="w-404 flex flex-col shrink-0 overflow-hidden border border-gray-200 rounded-8 auto-bg transition-colors dark:border-dark_border"
      >
        <FieldEditor
          v-if="selectedField"
          :field="selectedField"
          :siblings="siblingFields(selectedField)"
          :errors="structureErrors.get(selectedField)"
        />
        <FormSettingsEditor
          v-else
          v-model="formSettings"
        />
      </aside>
    </div>

    <!-- 预览抽屉 -->
    <FormPreviewRenderer
      v-model:show="showPreview"
      :title="formSettings.name"
      :fields="fields"
      :label-placement="formSettings.labelPlacement"
      :label-width="formSettings.labelWidth"
      :label-align="formSettings.labelAlign"
      :size="formSettings.formSize"
    />
  </div>
</template>

<script setup>
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NDivider,
  NEmpty,
  NInput,
  NScrollbar,
  NTag,
  NTooltip,
} from 'naive-ui'
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'
import { createField, FIELD_TYPE_META, isComposite } from '../constants'
import FieldEditor from './FieldEditor.vue'
import FormSettingsEditor from './FormSettingsEditor.vue'
import FormCanvasRenderer from './renderer/FormCanvasRenderer.vue'
import FormPreviewRenderer from './renderer/FormPreviewRenderer.vue'
import { fieldValueKey } from './renderer/helpers'
import { collectStructureErrors } from './renderer/validation'

defineOptions({ name: 'FormConfigDesigner' })

const props = defineProps({
  /** 模板信息 { formConfigId, versionId, code, name } */
  template: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['close', 'saved'])

/** 表单级设置（名称、编码、标签布局、尺寸） */
const formSettings = ref({
  name: props.template.name || '未命名表单',
  code: props.template.code || '',
  labelWidth: 90,
  labelPlacement: 'left',
  labelAlign: 'right',
  formSize: 'medium',
})
const version = ref(null)
const fields = ref([])
const selectedField = ref(null)
const saving = ref(false)
const showPreview = ref(false)
const formConfigId = ref(props.template.formConfigId)
const typeSearch = ref('')
const expandedGroups = ref([])

/** 设计态画布输入值（仅用于画布交互，不随保存提交） */
const designValues = reactive({})

const typeGroups = computed(() => [...new Set(Object.values(FIELD_TYPE_META).map(m => m.group))])

/** 搜索过滤后的类型分组 */
const filteredGroupedTypes = computed(() => {
  const q = typeSearch.value.trim().toLowerCase()
  const map = {}
  for (const [type, meta] of Object.entries(FIELD_TYPE_META)) {
    const matched = !q || meta.label.toLowerCase().includes(q) || type.toLowerCase().includes(q)
    if (!matched)
      continue
    if (!map[meta.group])
      map[meta.group] = []
    map[meta.group].push(type)
  }
  return map
})

const filteredTypeGroups = computed(() =>
  typeGroups.value.filter(group => filteredGroupedTypes.value[group]?.length),
)

const fieldCount = computed(() => countFields(fields.value))

/** 字段结构校验错误映射：field -> 错误数组（用于画布红框 + 保存拦截） */
const structureErrors = computed(() => collectStructureErrors(fields.value))

const statusMeta = computed(() => {
  const s = version.value?.status
  if (s === 'PUBLISHED')
    return { type: 'success', text: '已发布', dot: 'bg-green-500' }
  if (s === 'OFFLINE')
    return { type: 'default', text: '已下线', dot: 'bg-gray-400' }
  return { type: 'warning', text: '草稿', dot: 'bg-orange-400' }
})

onMounted(() => {
  expandedGroups.value = [...typeGroups.value]
  load()
})

async function load() {
  if (!props.template.versionId) {
    version.value = null
    return
  }
  try {
    const { data } = await api.versionDetail(props.template.versionId)
    version.value = data
    formSettings.value = {
      name: props.template.name || '未命名表单',
      code: props.template.code || '',
      labelWidth: data.labelWidth ?? 90,
      labelPlacement: data.labelPlacement || 'left',
      labelAlign: data.labelAlign || 'right',
      formSize: data.formSize || 'medium',
    }
    formConfigId.value = data.formConfigId
    fields.value = normalizeFields(data.fields || [])
  }
  catch (error) {
    console.error(error)
    $message.error('加载表单配置失败')
  }
}

/** 将后端返回的字段规范化为可编辑结构 */
function normalizeFields(list) {
  return (list || []).map(f => ({
    fieldId: f.fieldId,
    parentFieldId: f.parentFieldId,
    fieldKey: f.fieldKey || '',
    fieldLabel: f.fieldLabel || '',
    fieldType: f.fieldType,
    defaultValue: f.defaultValue ?? null,
    placeholder: f.placeholder || '',
    helpText: f.helpText || '',
    sortOrder: f.sortOrder ?? 0,
    colSpan: f.colSpan ?? 24,
    required: !!f.required,
    readonly: !!f.readonly,
    hidden: !!f.hidden,
    validationRules: f.validationRules || null,
    visibilityCondition: f.visibilityCondition || null,
    fieldProps: f.fieldProps || null,
    options: f.options || null,
    children: f.children ? normalizeFields(f.children) : null,
  }))
}

/** 收集去重后的顶层字段 key（用于条件渲染引用） */
function collectFieldKeys(list) {
  const keys = []
  function walk(arr) {
    for (const f of arr) {
      if (f.fieldKey)
        keys.push(f.fieldKey)
      if (f.children?.length)
        walk(f.children)
    }
  }
  walk(list)
  return [...new Set(keys)]
}

function siblingFields(field) {
  if (!field)
    return []
  const all = collectFieldKeys(fields.value)
  return all.map(key => ({ fieldKey: key, fieldLabel: key }))
}

/** 点击字段库添加字段：若选中了复合字段则加入其内部，否则加入顶层 */
function addField(type) {
  const parent = selectedField.value
  if (parent && isComposite(parent.fieldType)) {
    if (parent.fieldType === 'LIST' && (parent.children?.length ?? 0) >= 1) {
      $message.warning('列表最多只能包含一个字段')
      return
    }
    if (!parent.children)
      parent.children = []
    const field = createField(type, parent.children.length)
    field._key = `${type}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
    field.parentFieldId = parent.fieldId
    parent.children.push(field)
    return
  }
  const field = createField(type, fields.value.length)
  field._key = `${type}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
  fields.value.push(field)
  selectedField.value = field
}

function handleDeleteField(field) {
  const remove = (list) => {
    const idx = list.findIndex(f => f === field || (field.fieldId && f.fieldId === field.fieldId))
    if (idx >= 0) {
      list.splice(idx, 1)
      if (selectedField.value === field)
        selectedField.value = null
      return true
    }
    for (const item of list) {
      if (item.children?.length && remove(item.children))
        return true
    }
    return false
  }
  remove(fields.value)
}

/** 在兄弟字段列表中上移/下移 */
function handleMoveField(field, direction) {
  const move = (list) => {
    const idx = list.findIndex(f => f === field || (field.fieldId && f.fieldId === field.fieldId))
    if (idx < 0) {
      for (const item of list) {
        if (item.children?.length && move(item.children))
          return true
      }
      return false
    }
    const target = idx + direction
    if (target < 0 || target >= list.length)
      return false
    const [item] = list.splice(idx, 1)
    list.splice(target, 0, item)
    list.forEach((f, i) => {
      f.sortOrder = i
    })
    return true
  }
  move(fields.value)
}

/** 画布输入值写入设计态容器 */
function handleFieldValue({ field, value }) {
  const key = fieldValueKey(field)
  if (key)
    designValues[key] = value
}

function countFields(list) {
  let count = 0
  for (const f of list || []) {
    count += 1
    if (f.children?.length)
      count += countFields(f.children)
  }
  return count
}

/** 字段类型图标配色，用于区分类型语义 */
function typeColor(type) {
  const meta = FIELD_TYPE_META[type]
  if (!meta)
    return 'text-gray-400'
  if (meta.composite)
    return 'text-primary'
  if (['BOOLEAN', 'SELECT', 'MULTI_SELECT', 'DATE', 'DATETIME'].includes(type))
    return 'text-green-600 dark:text-green-500'
  if (['FILE', 'IMAGE', 'RICH_TEXT'].includes(type))
    return 'text-orange-500 dark:text-orange-400'
  return 'text-sky-500 dark:text-sky-400'
}

/** 字段库分组图标 */
function groupIcon(group) {
  const icons = {
    基础: 'i-material-symbols:edit-note',
    选择: 'i-material-symbols:checklist',
    高级: 'i-material-symbols:auto-awesome',
    复合: 'i-material-symbols:category',
  }
  return icons[group] || 'i-material-symbols:widgets'
}

async function handleSave() {
  await save('draft')
}

async function handlePublish() {
  await save('publish')
}

async function save(mode) {
  if (!formSettings.value.name || !formSettings.value.code) {
    $message.warning('表单名称和编码不能为空')
    return
  }
  if (structureErrors.value.size > 0) {
    const [firstField] = structureErrors.value.keys()
    selectedField.value = firstField
    $message.error(`存在 ${structureErrors.value.size} 个字段未填写完整（字段标识/字段标签必填），已定位到第一个`)
    return
  }
  const payload = {
    fields: sanitizeFields(fields.value),
    labelWidth: formSettings.value.labelWidth,
    labelPlacement: formSettings.value.labelPlacement,
    labelAlign: formSettings.value.labelAlign,
    formSize: formSettings.value.formSize,
  }
  saving.value = true
  try {
    // 表单名称变更同步到主表
    if (formConfigId.value && formSettings.value.name !== props.template.name)
      await api.update({ formConfigId: formConfigId.value, name: formSettings.value.name })
    const fn = mode === 'publish' ? api.publishDraft : api.saveDraft
    const { data } = await fn(formConfigId.value, payload)
    formConfigId.value = data.formConfigId
    $message.success(mode === 'publish' ? '保存并发布成功' : '保存成功')
    emit('saved', { formConfigId: data.formConfigId, versionId: data.versionId })
  }
  catch (error) {
    console.error(error)
    $message.error(mode === 'publish' ? '保存并发布失败' : '保存失败')
  }
  finally {
    saving.value = false
  }
}

/** 提交前清理客户端临时 key */
function sanitizeFields(list) {
  return (list || []).map(({ _key, ...f }) => ({
    ...f,
    options: f.options || null,
    children: f.children?.length ? sanitizeFields(f.children) : null,
  }))
}
</script>

<style scoped>
.type-collapse :deep(.n-collapse-item__header) {
  padding: 8px 10px;
  border-radius: 8px;
}

.type-collapse :deep(.n-collapse-item__content-inner) {
  padding: 2px 10px 10px;
}

/* 分组卡片内展开态圆角调整 */
.type-collapse-item:not(.n-collapse-item--active) :deep(.n-collapse-item__header) {
  border-radius: 8px;
}

.type-collapse-item :deep(.n-collapse-item__header) {
  border-radius: 8px;
}

/* 隐藏折叠展开箭头（V/>） */
.type-collapse :deep(.n-collapse-item-arrow) {
  display: none;
}
</style>
