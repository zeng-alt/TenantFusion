<template>
  <div v-if="field" class="field-editor h-full flex flex-col">
    <!-- 头部 -->
    <div
      class="h-48 flex shrink-0 items-center gap-10 border-b border-light_border px-12 transition-colors dark:border-dark_border"
    >
      <span
        class="h-30 w-30 flex shrink-0 items-center justify-center rounded-8 auto-bg-highlight"
      >
        <i class="text-16 text-primary" :class="[FIELD_TYPE_META[field.fieldType]?.icon]" />
      </span>
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-6">
          <span class="truncate text-13 font-600">
            {{ FIELD_TYPE_META[field.fieldType]?.label || '字段' }}
          </span>
          <NTag
            v-if="invalid"
            size="tiny"
            :bordered="false"
            type="error"
          >
            需完善
          </NTag>
        </div>
        <div class="truncate text-11 text-gray-400 font-mono dark:text-gray-500">
          {{ field.fieldKey || '未设置字段标识' }}
        </div>
      </div>
    </div>

    <!-- 属性编辑区 -->
    <div class="min-h-0 flex-1">
      <NScrollbar class="h-full">
        <div class="flex flex-col gap-10 p-10">
          <!-- 基础属性 -->
          <NCard
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <div class="w-full flex items-center">
                <span class="text-12 text-gray-600 font-600 dark:text-gray-300">基础属性</span>
                <div class="flex-1" />
                <span
                  v-if="invalid"
                  class="flex items-center gap-4 text-11 text-red-500"
                >
                  <i class="i-material-symbols:error-outline text-12" />
                  {{ invalidCount }} 项未完善
                </span>
              </div>
            </template>
            <NForm
              label-placement="left"
              :label-width="80"
              size="small"
              class="compact-form"
            >
              <NFormItem
                label="字段标识"
                :feedback="fieldKeyError"
                :validation-status="fieldKeyError ? 'error' : undefined"
              >
                <NInput
                  v-model:value="field.fieldKey"
                  placeholder="camelCase，用于数据绑定"
                  :status="fieldKeyError ? 'error' : undefined"
                />
              </NFormItem>
              <NFormItem
                label="字段标签"
                :feedback="errors?.includes('字段标签必填') ? '字段标签必填' : undefined"
                :validation-status="errors?.includes('字段标签必填') ? 'error' : undefined"
              >
                <NInput
                  v-model:value="field.fieldLabel"
                  placeholder="显示名称"
                  :status="errors?.includes('字段标签必填') ? 'error' : undefined"
                />
              </NFormItem>
              <NFormItem label="字段类型">
                <NSelect
                  v-model:value="field.fieldType"
                  :options="typeOptions"
                  @update:value="handleTypeChange"
                />
              </NFormItem>

              <template v-if="!isComposite(field.fieldType)">
                <NFormItem label="默认值">
                  <component
                    :is="defaultValueEditor"
                    :value="field.defaultValue"
                    @update:value="val => (field.defaultValue = val)"
                  />
                </NFormItem>
                <NFormItem v-if="hasTextInput(field.fieldType)" label="占位提示">
                  <NInput v-model:value="field.placeholder" />
                </NFormItem>
              </template>

              <NFormItem label="帮助文本">
                <NInput v-model:value="field.helpText" />
              </NFormItem>
            </NForm>
          </NCard>

          <!-- 布局 -->
          <NCard
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <span class="text-12 text-gray-600 font-600 dark:text-gray-300">布局</span>
            </template>
            <NForm
              label-placement="left"
              :label-width="80"
              size="small"
              class="compact-form"
            >
              <NFormItem label="栅格列宽">
                <div class="w-full flex items-center gap-10">
                  <NSlider
                    v-model:value="field.colSpan"
                    :min="1"
                    :max="24"
                    :marks="{ 8: '1/3', 12: '1/2' }"
                    class="flex-1!"
                  />
                  <NTag
                    size="small"
                    :bordered="false"
                    round
                    type="primary"
                    class="justify-center w-30!"
                  >
                    {{ field.colSpan }}
                  </NTag>
                </div>
              </NFormItem>
            </NForm>
          </NCard>

          <!-- 状态 -->
          <NCard
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <span class="text-12 text-gray-600 font-600 dark:text-gray-300">状态</span>
            </template>
            <div class="grid grid-cols-3 gap-6">
              <label
                class="state-chip"
                :class="{ 'state-chip--active': field.required }"
              >
                <NCheckbox v-model:checked="field.required">
                  必填
                </NCheckbox>
              </label>
              <label
                class="state-chip"
                :class="{ 'state-chip--active': field.readonly }"
              >
                <NCheckbox v-model:checked="field.readonly">
                  只读
                </NCheckbox>
              </label>
              <label
                class="state-chip"
                :class="{ 'state-chip--active': field.hidden }"
              >
                <NCheckbox v-model:checked="field.hidden">
                  隐藏
                </NCheckbox>
              </label>
            </div>
          </NCard>

          <!-- 下拉选项 -->
          <NCard
            v-if="hasOptions(field.fieldType)"
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <div class="w-full flex items-center">
                <span class="text-12 text-gray-600 font-600 dark:text-gray-300">选项</span>
                <div class="flex-1" />
                <NButton
                  size="tiny"
                  type="primary"
                  dashed
                  @click="addOption"
                >
                  <template #icon>
                    <i class="i-material-symbols:add text-12" />
                  </template>
                  添加
                </NButton>
              </div>
            </template>
            <div
              v-for="(opt, index) in field.options"
              :key="index"
              class="mb-6 flex items-center gap-6 border border-light_border rounded-6 border-dashed px-8 py-6 transition-colors dark:border-dark_border"
            >
              <span class="w-14 shrink-0 text-center text-11 text-gray-400">
                {{ index + 1 }}
              </span>
              <NInput
                v-model:value="opt.label"
                size="small"
                placeholder="显示名"
                class="flex-1!"
              />
              <NInput
                v-model:value="opt.value"
                size="small"
                placeholder="值"
                class="flex-1!"
              />
              <NButton
                size="tiny"
                type="error"
                quaternary
                circle
                @click="removeOption(index)"
              >
                <template #icon>
                  <i class="i-material-symbols:delete-outline text-14" />
                </template>
              </NButton>
            </div>
          </NCard>

          <!-- 类型特定属性 -->
          <NCard
            v-if="typeSpecificFields.length"
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <span class="text-12 text-gray-600 font-600 dark:text-gray-300">类型属性</span>
            </template>
            <NForm
              label-placement="left"
              :label-width="110"
              size="small"
              class="compact-form"
            >
              <template v-for="(item, index) in typeSpecificFields" :key="index">
                <NFormItem :label="item.label">
                  <NInput
                    v-if="item.kind === 'text'"
                    v-model:value="fieldPropsModel[item.key]"
                    :placeholder="item.placeholder"
                  />
                  <NInputNumber
                    v-else-if="item.kind === 'number'"
                    v-model:value="fieldPropsModel[item.key]"
                    :placeholder="item.placeholder"
                    class="w-full!"
                  />
                  <NSwitch
                    v-else-if="item.kind === 'boolean'"
                    v-model:value="fieldPropsModel[item.key]"
                    size="small"
                  >
                    <template #checked>
                      是
                    </template>
                    <template #unchecked>
                      否
                    </template>
                  </NSwitch>
                </NFormItem>
              </template>
            </NForm>
          </NCard>

          <!-- 校验规则 -->
          <NCard
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <span class="text-12 text-gray-600 font-600 dark:text-gray-300">校验规则</span>
            </template>
            <ValidationRuleEditor
              :model-value="field.validationRules ? JSON.parse(field.validationRules) : []"
              @update:model-value="val => (field.validationRules = val.length ? JSON.stringify(val) : null)"
            />
          </NCard>

          <!-- 条件渲染 -->
          <NCard
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <span class="text-12 text-gray-600 font-600 dark:text-gray-300">条件渲染</span>
            </template>
            <NCheckbox
              v-model:checked="hasCondition"
              class="mb-8"
            >
              启用条件显示
            </NCheckbox>
            <ConditionalRenderEditor
              v-if="hasCondition"
              :model-value="field.visibilityCondition ? JSON.parse(field.visibilityCondition) : null"
              :fields="siblingFields"
              :current-field-key="field.fieldKey"
              @update:model-value="val => (field.visibilityCondition = val ? JSON.stringify(val) : null)"
            />
          </NCard>

          <!-- 自定义属性（v-bind 到 naive-ui 组件） -->
          <NCard
            v-if="!isComposite(field.fieldType)"
            size="small"
            :bordered="false"
            class="border border-gray-200 rounded-8 auto-bg-highlight dark:border-dark_border"
          >
            <template #header>
              <div class="w-full flex items-center">
                <span class="text-12 text-gray-600 font-600 dark:text-gray-300">自定义属性</span>
                <div class="flex-1" />
                <NTooltip trigger="hover">
                  <template #trigger>
                    <i class="i-material-symbols:info-outline text-13 text-gray-400" />
                  </template>
                  属性将 v-bind 到渲染的 naive-ui 组件上，属性名需与 naive-ui props 一致
                </NTooltip>
              </div>
            </template>
            <CustomAttrsEditor v-model="customAttrsRows" />
          </NCard>
        </div>
      </NScrollbar>
    </div>
  </div>
  <div v-else class="h-full flex flex-col items-center justify-center gap-8">
    <NEmpty
      size="small"
      description="请选择左侧字段进行配置"
    >
      <template #icon>
        <i class="i-material-symbols:tune text-36 text-gray-300 dark:text-gray-600" />
      </template>
    </NEmpty>
  </div>
</template>

<script setup>
import {
  NButton,
  NCard,
  NCheckbox,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NScrollbar,
  NSelect,
  NSlider,
  NSwitch,
  NTag,
  NTooltip,
} from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { FIELD_TYPE_META, hasOptions, isComposite } from '../constants'
import ConditionalRenderEditor from './ConditionalRenderEditor.vue'
import CustomAttrsEditor from './CustomAttrsEditor.vue'
import ValidationRuleEditor from './ValidationRuleEditor.vue'

defineOptions({ name: 'FieldEditor' })

const props = defineProps({
  field: {
    type: Object,
    default: null,
  },
  /** 同级兄弟字段（用于条件渲染引用） */
  siblings: {
    type: Array,
    default: () => [],
  },
  /** 字段结构校验错误数组（字段标识/字段标签必填） */
  errors: {
    type: Array,
    default: () => [],
  },
})

const field = computed(() => props.field)

const siblingFields = computed(() => props.siblings || [])

/** 是否有结构校验错误 */
const invalid = computed(() => (props.errors?.length || 0) > 0)

const invalidCount = computed(() => props.errors?.length || 0)

/** 字段标识相关的结构校验错误（必填/重复） */
const fieldKeyError = computed(() => props.errors?.find(e => e.includes('字段标识')) || '')

/** 类型特定属性（同步回 field.fieldProps JSON） */
const fieldPropsModel = ref({})

/** 自定义属性行（fieldProps.customAttrs 的文本 key-value 映射） */
const customAttrsRows = ref([])

watch(
  () => [field.value?.fieldId, field.value?.fieldType],
  () => loadFieldProps(),
  { immediate: true },
)

function loadFieldProps() {
  try {
    fieldPropsModel.value = field.value?.fieldProps ? JSON.parse(field.value.fieldProps) : {}
  }
  catch {
    fieldPropsModel.value = {}
  }
  customAttrsRows.value = toAttrRows(fieldPropsModel.value.customAttrs)
}

/** 对象映射 {key: value} -> 行数组 [{key, value}] */
function toAttrRows(map) {
  if (!map || typeof map !== 'object' || Array.isArray(map))
    return []
  return Object.entries(map).map(([key, value]) => ({ key, value }))
}

watch(fieldPropsModel, (val) => {
  if (field.value) {
    field.value.fieldProps = val && Object.keys(val).length ? JSON.stringify(val) : null
  }
}, { deep: true })

/** 行数组 -> 对象映射，写回 fieldPropsModel.customAttrs（空时移除） */
watch(customAttrsRows, (rows) => {
  const map = {}
  for (const { key, value } of rows || []) {
    if (key)
      map[key] = value
  }
  if (Object.keys(map).length)
    fieldPropsModel.value.customAttrs = map
  else
    delete fieldPropsModel.value.customAttrs
}, { deep: true })

const typeOptions = computed(() =>
  Object.keys(FIELD_TYPE_META).map(type => ({
    label: FIELD_TYPE_META[type].label,
    value: type,
  })),
)

/** 根据字段类型选择默认值编辑器 */
const defaultValueEditor = computed(() => {
  const type = field.value?.fieldType
  if (type === 'BOOLEAN')
    return NSwitch
  if (type === 'NUMBER')
    return NInputNumber
  return NInput
})

const typeSpecificFields = computed(() => {
  const type = field.value?.fieldType
  switch (type) {
    case 'NUMBER':
      return [
        { key: 'min', label: '最小值', kind: 'number', placeholder: '不限' },
        { key: 'max', label: '最大值', kind: 'number', placeholder: '不限' },
        { key: 'step', label: '步长', kind: 'number', placeholder: '1' },
        { key: 'precision', label: '小数位数', kind: 'number', placeholder: '0' },
      ]
    case 'DATE':
    case 'DATETIME':
      return [
        { key: 'dateFormat', label: '日期格式', kind: 'text', placeholder: 'yyyy-MM-dd' },
      ]
    case 'TEXTAREA':
      return [
        { key: 'rows', label: '行数', kind: 'number', placeholder: '3' },
        { key: 'showCount', label: '显示字数', kind: 'boolean' },
      ]
    case 'FILE':
      return [
        { key: 'fileTypes', label: '文件类型', kind: 'text', placeholder: '如 jpg,png,pdf' },
        { key: 'maxSize', label: '单文件大小(MB)', kind: 'number', placeholder: '5' },
        { key: 'maxCount', label: '最大数量', kind: 'number', placeholder: '1' },
      ]
    case 'IMAGE':
      return [
        { key: 'maxCount', label: '最大数量', kind: 'number', placeholder: '1' },
      ]
    case 'LIST':
      return [
        { key: 'minItems', label: '最小项数', kind: 'number', placeholder: '0' },
        { key: 'maxItems', label: '最大项数', kind: 'number', placeholder: '不限' },
      ]
    default:
      return []
  }
})

const hasCondition = computed({
  get: () => !!field.value?.visibilityCondition,
  set: (val) => {
    if (val)
      field.value.visibilityCondition = JSON.stringify({ logic: 'and', conditions: [] })
    else
      field.value.visibilityCondition = null
  },
})

function hasTextInput(type) {
  return ['STRING', 'TEXTAREA', 'FILE'].includes(type)
}

function handleTypeChange() {
  const type = field.value.fieldType
  field.value.options = hasOptions(type) ? (field.value.options || []) : null
  if (isComposite(type)) {
    field.value.children = field.value.children || []
    field.value.defaultValue = null
  }
  else {
    field.value.children = null
  }
  field.value.fieldProps = null
}

function addOption() {
  field.value.options.push({ label: '', value: '' })
}

function removeOption(index) {
  field.value.options.splice(index, 1)
}
</script>

<style scoped>
.state-chip {
  @apply flex cursor-pointer items-center rounded-6 border border-light_border px-6 py-4 transition-colors hover:border-primary/40 dark:border-dark_border;
}

.state-chip--active {
  @apply border-primary/50 bg-primary/5;
}

/* 紧凑表单：行间距收紧 */
.compact-form :deep(.n-form-item) {
  @apply mb-8;
}

.compact-form :deep(.n-form-item:last-child) {
  @apply mb-0;
}

/* NCard 小尺寸下内容间距收紧 */
:deep(.n-card--content__inner) {
  padding: 4px;
}
</style>
