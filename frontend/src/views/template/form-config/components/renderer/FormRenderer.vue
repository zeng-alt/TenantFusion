<template>
  <div
    class="form-renderer grid w-full gap-8"
    style="grid-template-columns: repeat(24, minmax(0, 1fr))"
  >
    <template
      v-for="(field, index) in visibleFields"
      :key="field.fieldId ?? field._key"
    >
      <!-- 复合字段 -->
      <div
        v-if="isComposite(field.fieldType)"
        :style="gridSpanStyle(field.colSpan)"
        class="min-w-0"
      >
        <CompositeWidget
          :field="field"
          :mode="mode"
          :selected-field="selectedField"
          :values="values"
          :disabled="disabled"
          :index="index"
          :total="visibleFields.length"
          :label-placement="labelPlacement"
          :label-width="labelWidth"
          :label-align="labelAlign"
          :size="size"
          :structure-errors="structureErrors"
          @select="emit('select', $event)"
          @delete="emit('delete', $event)"
          @move="(field, direction) => emit('move', field, direction)"
          @add-row="(field) => emit('addRow', field)"
          @remove-row="(field, index) => emit('removeRow', field, index)"
          @update:field-value="payload => emit('update:fieldValue', payload)"
        />
      </div>
      <!-- 叶子字段 -->
      <div
        v-else
        :style="gridSpanStyle(field.colSpan)"
        class="min-w-0"
      >
        <FieldWidget
          :field="field"
          :mode="mode"
          :selected-field="selectedField"
          :values="values"
          :disabled="disabled"
          :index="index"
          :total="visibleFields.length"
          :errors="errors"
          :label-placement="labelPlacement"
          :label-width="labelWidth"
          :label-align="labelAlign"
          :size="size"
          :structure-errors="structureErrors"
          @select="emit('select', $event)"
          @delete="emit('delete', $event)"
          @move="(field, direction) => emit('move', field, direction)"
          @update:field-value="handleFieldValue"
        />
      </div>
    </template>

    <NEmpty
      v-if="!visibleFields.length"
      class="col-span-24 py-16"
      size="small"
      :description="emptyDescription || (mode === 'design' ? '暂无字段，请从左侧字段库添加' : '暂无可见字段')"
    />
  </div>
</template>

<script setup>
import { NEmpty } from 'naive-ui'
import { computed } from 'vue'
import { isComposite } from '../../constants'
import FieldWidget from './FieldWidget.vue'
import { gridSpanStyle } from './helpers'
import { evaluateVisibility } from './validation'
import CompositeWidget from './widgets/CompositeWidget.vue'

defineOptions({ name: 'FormRenderer' })

const props = defineProps({
  /** 规范化后的字段树 */
  fields: { type: Array, default: () => [] },
  /** design: 画布预览（禁交互）；preview: 表单填写 */
  mode: { type: String, default: 'design' },
  /** 画布中当前选中字段 */
  selectedField: { type: Object, default: null },
  /** 预览态表单值 { [fieldKey]: value } */
  values: { type: Object, default: null },
  /** 全局只读（预览态生效） */
  disabled: { type: Boolean, default: false },
  /** 已收集的校验错误 { [fieldKey]: message } */
  errors: { type: Object, default: () => ({}) },
  /** 表单级标签配置 */
  labelPlacement: { type: String, default: 'left' },
  labelWidth: { type: Number, default: 90 },
  labelAlign: { type: String, default: 'right' },
  /** 表单级控件尺寸 */
  size: { type: String, default: 'medium' },
  /** 空状态描述（复合字段内部可定制提示） */
  emptyDescription: { type: String, default: '' },
  /** 字段结构校验错误映射：field -> 错误数组 */
  structureErrors: { type: Object, default: () => new Map() },
})

const emit = defineEmits(['select', 'delete', 'move', 'update:fieldValue', 'addRow', 'removeRow'])

/** 将字段值变更转发给父级（由父级写入预览态 values） */
function handleFieldValue({ field, value }) {
  emit('update:fieldValue', { field, value })
}

/** 预览态按条件渲染过滤字段；设计态全部展示 */
const visibleFields = computed(() => {
  if (props.mode === 'design')
    return props.fields || []
  return (props.fields || []).filter((field) => {
    if (field.hidden)
      return false
    return evaluateVisibility(field, props.values)
  })
})
</script>
