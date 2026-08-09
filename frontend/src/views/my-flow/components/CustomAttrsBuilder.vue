<template>
  <div class="custom-attrs-builder w-full">
    <div
      v-for="(attr, index) in attrs"
      :key="index"
      class="mb-6 border border-light_border rounded-6 border-dashed px-8 py-6 transition-colors dark:border-dark_border"
    >
      <div class="flex items-center gap-6">
        <span class="w-16 shrink-0 text-center text-11 text-gray-400">
          {{ index + 1 }}
        </span>
        <NInput
          v-model:value="attr.name"
          size="small"
          placeholder="属性名"
          class="w-140!"
        />
        <NSelect
          v-model:value="attr.type"
          size="small"
          :options="typeOptions"
          class="w-110!"
          @update:value="value => handleTypeChange(attr, value)"
        />

        <template v-if="attr.type !== 'select'">
          <NInput
            v-if="attr.type === 'text'"
            v-model:value="attr.value"
            size="small"
            placeholder="属性值"
            class="flex-1!"
          />
          <NInput
            v-else-if="attr.type === 'textarea'"
            v-model:value="attr.value"
            size="small"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 3 }"
            placeholder="属性值"
            class="flex-1!"
          />
          <NInputNumber
            v-else-if="attr.type === 'number'"
            v-model:value="attr.value"
            size="small"
            placeholder="属性值"
            class="flex-1!"
          />
          <div v-else-if="attr.type === 'boolean'" class="pl-8 flex-1!">
            <NSwitch v-model:value="attr.value" />
          </div>
          <NDatePicker
            v-else-if="attr.type === 'date'"
            v-model:value="attr.value"
            size="small"
            type="date"
            class="flex-1!"
          />
          <NDatePicker
            v-else-if="attr.type === 'datetime'"
            v-model:value="attr.value"
            size="small"
            type="datetime"
            class="flex-1!"
          />
        </template>
        <NInput
          v-else
          v-model:value="attr.options"
          size="small"
          placeholder="选项：逗号分隔，如 同意=approve,驳回=reject"
          class="flex-1!"
        />

        <NButton
          size="tiny"
          type="error"
          quaternary
          circle
          @click="removeAttr(index)"
        >
          <template #icon>
            <i class="i-material-symbols:delete-outline text-14" />
          </template>
        </NButton>
      </div>

      <!-- 下拉选择的取值控件 -->
      <div v-if="attr.type === 'select'" class="mt-6 flex items-center gap-6 pl-30">
        <span class="shrink-0 text-11 text-gray-400">
          值
        </span>
        <NSelect
          v-model:value="attr.value"
          size="small"
          :options="selectOptions(attr)"
          clearable
          filterable
          placeholder="请选择值"
          class="flex-1!"
        />
      </div>
    </div>

    <NButton
      size="small"
      type="primary"
      dashed
      block
      @click="addAttr"
    >
      <template #icon>
        <i class="i-material-symbols:add text-14" />
      </template>
      添加自定义属性
    </NButton>
  </div>
</template>

<script setup>
import { NButton, NDatePicker, NInput, NInputNumber, NSelect, NSwitch } from 'naive-ui'
import { ref, watch } from 'vue'

defineOptions({ name: 'CustomAttrsBuilder' })

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const CUSTOM_ATTR_TYPES = [
  { value: 'text', label: '文本' },
  { value: 'textarea', label: '多行文本' },
  { value: 'number', label: '数字' },
  { value: 'boolean', label: '布尔' },
  { value: 'date', label: '日期' },
  { value: 'datetime', label: '日期时间' },
  { value: 'select', label: '下拉选择' },
]

const typeOptions = CUSTOM_ATTR_TYPES

const attrs = ref(props.modelValue || [])

watch(
  () => props.modelValue,
  (val) => {
    attrs.value = val || []
  },
  { deep: true },
)

watch(
  attrs,
  (val) => {
    emit('update:modelValue', val)
  },
  { deep: true },
)

function addAttr() {
  attrs.value = [...attrs.value, { name: '', type: 'text', value: '', options: '' }]
}

function removeAttr(index) {
  attrs.value = attrs.value.filter((_, i) => i !== index)
}

function handleTypeChange(attr, type) {
  if (type === 'boolean')
    attr.value = false
  else if (type === 'number' || type === 'date' || type === 'datetime')
    attr.value = null
  else
    attr.value = ''
}

/** 解析下拉选项：支持 “标签=值, 标签=值” 或纯标签列表 */
function selectOptions(attr) {
  return String(attr.options || '')
    .split(/[,，\n]/)
    .map(item => item.trim())
    .filter(Boolean)
    .map((item) => {
      const sep = item.indexOf('=')
      if (sep > -1) {
        return { label: item.slice(0, sep).trim(), value: item.slice(sep + 1).trim() }
      }
      return { label: item, value: item }
    })
}
</script>
