<template>
  <div
    class="group relative min-h-0 rounded-6 transition-all duration-150"
    :class="[
      selectedField === field
        ? 'bg-primary/5 ring-2 ring-primary/40'
        : mode === 'design' ? 'hover:bg-primary/5 hover:ring-1 hover:ring-primary/30' : '',
      hiddenDimmed ? 'opacity-50' : '',
      isInvalid ? 'bg-red-50/60 ring-2 ring-red-500/60 dark:bg-red-500/10' : '',
    ]"
    @click.stop="handleSelect"
  >
    <!-- labelPlacement=left：标签与控件左右排列 -->
    <div
      v-if="labelPlacement === 'left'"
      class="flex items-start gap-8 px-4 py-6"
    >
      <div
        class="flex flex-col shrink-0 items-start pt-4"
        :style="{ width: `${labelWidth}px`, alignItems: labelAlign === 'right' ? 'flex-end' : 'flex-start' }"
      >
        <span class="max-w-full truncate text-12 text-gray-600 font-500 dark:text-gray-300">
          <span v-if="field.required" class="mr-2 text-13 text-red-500">*</span>
          {{ field.fieldLabel || '未命名字段' }}
        </span>
        <NTooltip
          v-if="isInvalid && mode === 'design'"
          :show-arrow="false"
          content-style="white-space: normal; word-break: break-word; max-width: 260px"
        >
          <template #trigger>
            <span class="mt-2 flex shrink-0 items-center gap-2 text-11 text-red-500">
              <i class="i-material-symbols:error-outline text-12" />
              <span class="whitespace-nowrap">{{ structureErrorMessages.length }} 项未完善</span>
            </span>
          </template>
          {{ structureErrorMessages.join('；') }}
        </NTooltip>
      </div>
      <div class="min-w-0 flex-1">
        <component
          :is="widget"
          :field="field"
          :model-value="fieldValue"
          :disabled="widgetDisabled"
          :size="size"
          @update:model-value="value => emit('update:fieldValue', { field, value })"
        />
        <div
          v-if="errorMessage"
          class="mt-4 flex items-center gap-2 text-11 text-red-500"
        >
          <i class="i-material-symbols:error-outline text-12" />
          {{ errorMessage }}
        </div>
        <div
          v-else-if="field.helpText"
          class="mt-4 text-11 text-gray-400 dark:text-gray-500"
        >
          {{ field.helpText }}
        </div>
      </div>
      <NTag
        v-if="hiddenDimmed"
        size="tiny"
        :bordered="false"
        type="warning"
        class="shrink-0"
      >
        已隐藏
      </NTag>
    </div>

    <!-- labelPlacement=top：标签在上、控件在下 -->
    <div v-else>
      <div class="flex items-start gap-4 px-4 pt-6">
        <span
          class="min-w-0 flex-1 truncate text-12 text-gray-600 font-500 dark:text-gray-300"
          :style="{ textAlign: labelAlign === 'right' ? 'right' : 'left' }"
        >
          <span v-if="field.required" class="mr-2 text-13 text-red-500">*</span>
          {{ field.fieldLabel || '未命名字段' }}
        </span>
        <NTooltip
          v-if="isInvalid && mode === 'design'"
          :show-arrow="false"
          content-style="white-space: normal; word-break: break-word; max-width: 260px"
        >
          <template #trigger>
            <span class="ml-4 flex shrink-0 items-center gap-2 text-11 text-red-500">
              <i class="i-material-symbols:error-outline text-12" />
              <span class="whitespace-nowrap">{{ structureErrorMessages.length }} 项未完善</span>
            </span>
          </template>
          {{ structureErrorMessages.join('；') }}
        </NTooltip>
        <NTag
          v-if="hiddenDimmed"
          size="tiny"
          :bordered="false"
          type="warning"
        >
          已隐藏
        </NTag>
      </div>

      <div class="px-4 pb-4 pt-4">
        <component
          :is="widget"
          :field="field"
          :model-value="fieldValue"
          :disabled="widgetDisabled"
          :size="size"
          @update:model-value="value => emit('update:fieldValue', { field, value })"
        />
        <div
          v-if="errorMessage"
          class="mt-4 flex items-center gap-2 text-11 text-red-500"
        >
          <i class="i-material-symbols:error-outline text-12" />
          {{ errorMessage }}
        </div>
        <div
          v-else-if="field.helpText"
          class="mt-4 text-11 text-gray-400 dark:text-gray-500"
        >
          {{ field.helpText }}
        </div>
      </div>
    </div>

    <!-- 设计态悬浮操作条 -->
    <div
      v-if="mode === 'design'"
      class="absolute right-6 top-6 z-10 flex items-center gap-2 rounded-4 bg-white/95 px-4 py-2 opacity-0 shadow-sm transition-opacity dark:bg-[#2a2a2f] group-hover:opacity-100"
    >
      <NTooltip>
        <template #trigger>
          <NButton
            size="tiny"
            quaternary
            circle
            :disabled="index === 0"
            @click.stop="emit('move', field, -1)"
          >
            <template #icon>
              <i class="i-material-symbols:arrow-upward text-12" />
            </template>
          </NButton>
        </template>
        上移
      </NTooltip>
      <NTooltip>
        <template #trigger>
          <NButton
            size="tiny"
            quaternary
            circle
            :disabled="index === total - 1"
            @click.stop="emit('move', field, 1)"
          >
            <template #icon>
              <i class="i-material-symbols:arrow-downward text-12" />
            </template>
          </NButton>
        </template>
        下移
      </NTooltip>
      <NTooltip>
        <template #trigger>
          <NButton
            size="tiny"
            quaternary
            type="error"
            circle
            @click.stop="emit('delete', field)"
          >
            <template #icon>
              <i class="i-material-symbols:delete-outline text-12" />
            </template>
          </NButton>
        </template>
        删除
      </NTooltip>
    </div>
  </div>
</template>

<script setup>
import { NButton, NTag, NTooltip } from 'naive-ui'
import { computed } from 'vue'
import { fieldValueKey } from './helpers'
import { getFieldWidget } from './widgets'

defineOptions({ name: 'FieldWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  mode: { type: String, default: 'design' },
  selectedField: { type: Object, default: null },
  values: { type: Object, default: null },
  disabled: { type: Boolean, default: false },
  errors: { type: Object, default: () => ({}) },
  index: { type: Number, default: 0 },
  total: { type: Number, default: 1 },
  /** 表单级标签配置 */
  labelPlacement: { type: String, default: 'left' },
  labelWidth: { type: Number, default: 90 },
  labelAlign: { type: String, default: 'right' },
  /** 表单级控件尺寸 */
  size: { type: String, default: 'medium' },
  /** 字段结构校验错误映射：field -> 错误数组 */
  structureErrors: { type: Object, default: () => new Map() },
})

const emit = defineEmits(['select', 'delete', 'move', 'update:fieldValue'])

const widget = computed(() => getFieldWidget(props.field.fieldType))

/** 隐藏字段在设计态也展示（淡显），预览态直接不渲染 */
const hiddenDimmed = computed(() => props.field.hidden && props.mode === 'design')

const widgetDisabled = computed(() => props.disabled || props.field.readonly)

const fieldValue = computed(() => props.values?.[fieldValueKey(props.field)] ?? props.field.defaultValue ?? null)

/** 当前字段校验错误信息 */
const errorMessage = computed(() => props.errors?.[fieldValueKey(props.field)] || '')

/** 字段结构是否校验失败（字段标识/字段标签必填、字段标识重复） */
const isInvalid = computed(() => !!props.structureErrors?.get(props.field))

/** 字段结构校验错误内容 */
const structureErrorMessages = computed(() => props.structureErrors?.get(props.field) || [])

function handleSelect() {
  if (props.mode === 'design')
    emit('select', props.field)
}
</script>
