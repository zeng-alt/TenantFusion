<template>
  <NDrawer
    :show="show"
    :width="680"
    placement="right"
    @update:show="value => emit('update:show', value)"
  >
    <NDrawerContent
      :title="title || '表单预览'"
      closable
      class="preview-drawer"
    >
      <div class="h-full flex flex-col">
        <!-- 预览画布 -->
        <div class="min-h-0 flex-1">
          <NScrollbar class="h-full">
            <div class="p-12">
              <div
                class="min-h-full border border-light_border rounded-8 bg-white p-14 transition-colors dark:border-dark_border dark:bg-[#1c1c20]"
              >
                <div class="mb-12 flex items-center gap-6 border-b border-light_border pb-10 transition-colors dark:border-dark_border">
                  <i class="i-material-symbols:description text-16 text-primary" />
                  <span class="text-14 font-600">{{ title || '未命名表单' }}</span>
                  <div class="flex-1" />
                  <NTag
                    size="small"
                    :bordered="false"
                    type="info"
                  >
                    {{ fieldCount }} 个字段
                  </NTag>
                </div>

                <FormRenderer
                  :fields="fields"
                  mode="preview"
                  :values="values"
                  :errors="errors"
                  :label-placement="labelPlacement"
                  :label-width="labelWidth"
                  :label-align="labelAlign"
                  :size="size"
                  @update:field-value="handleFieldValue"
                  @add-row="handleAddRow"
                  @remove-row="handleRemoveRow"
                />
              </div>
            </div>
          </NScrollbar>
        </div>

        <!-- 底部操作 -->
        <div class="flex shrink-0 items-center gap-6 border-t border-light_border px-12 py-8 transition-colors dark:border-dark_border">
          <div
            v-if="errorCount"
            class="flex items-center gap-4 text-12 text-red-500"
          >
            <i class="i-material-symbols:error-outline text-14" />
            {{ errorCount }} 处校验未通过
          </div>
          <div class="flex-1" />
          <NButton
            size="small"
            @click="reset"
          >
            <template #icon>
              <i class="i-material-symbols:refresh text-14" />
            </template>
            重置
          </NButton>
          <NButton
            size="small"
            type="primary"
            @click="handleValidate"
          >
            <template #icon>
              <i class="i-material-symbols:check-circle text-14" />
            </template>
            校验
          </NButton>
        </div>
      </div>
    </NDrawerContent>
  </NDrawer>
</template>

<script setup>
import { NButton, NDrawer, NDrawerContent, NScrollbar, NTag } from 'naive-ui'
import { computed, reactive, watch } from 'vue'
import FormRenderer from './FormRenderer.vue'
import { fieldValueKey } from './helpers'
import { collectErrors } from './validation'

defineOptions({ name: 'FormPreviewRenderer' })

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: '' },
  fields: { type: Array, default: () => [] },
  /** 表单级标签配置 */
  labelPlacement: { type: String, default: 'left' },
  labelWidth: { type: Number, default: 90 },
  labelAlign: { type: String, default: 'right' },
  /** 表单级控件尺寸 */
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:show'])

/** 预览表单值 */
const values = reactive({})

const errors = computed(() => collectErrors(props.fields, values))

const errorCount = computed(() => Object.keys(errors.value).length)

const fieldCount = computed(() => countFields(props.fields))

/** 打开时初始化默认值 */
watch(
  () => props.show,
  (visible) => {
    if (visible)
      reset()
  },
)

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
  return target
}

/** 构建一条 LIST 行（子字段默认值，嵌套 LIST 再生成行数组） */
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

function reset() {
  Object.keys(values).forEach((key) => {
    delete values[key]
  })
  initValues(props.fields, values)
}

/** 写入预览表单值 */
function handleFieldValue({ field, value }) {
  const key = fieldValueKey(field)
  if (key)
    values[key] = value
}

/** LIST 新增一行（向下追加） */
function handleAddRow(field) {
  const key = fieldValueKey(field)
  if (!key)
    return
  if (!Array.isArray(values[key]))
    values[key] = []
  values[key].push(buildRow(field))
}

/** LIST 删除一行 */
function handleRemoveRow(field, index) {
  const key = fieldValueKey(field)
  const rows = values[key]
  if (!Array.isArray(rows))
    return
  rows.splice(index, 1)
}

function handleValidate() {
  if (errorCount.value)
    $message.error(`校验未通过，共 ${errorCount.value} 处`)
  else
    $message.success('校验通过')
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
</script>
