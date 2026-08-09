<template>
  <!-- 空状态上下居中模式（preview 无可见字段时占满容器居中） -->
  <div
    v-if="emptyCentered && !visibleFields.length"
    class="w-full flex items-center justify-center"
  >
    <NEmpty
      size="small"
      :description="emptyDescription || (mode === 'design' ? '暂无字段，请从左侧字段库添加' : '暂无可见字段')"
    />
  </div>

  <!-- 设计态：自定义 24 栅格（选中/悬浮操作条依赖 FieldWidget 包裹层） -->
  <div
    v-else-if="mode === 'design'"
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
          mode="design"
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
          mode="design"
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
      :description="emptyDescription || '暂无字段，请从左侧字段库添加'"
    />
  </div>

  <!-- 预览态：naive-ui n-grid + n-form-item-gi，控件自动撑满所在列宽 -->
  <NGrid
    v-else
    :cols="24"
    :x-gap="gridGap"
    :y-gap="gridGap"
    item-responsive
    :item-style="{ minWidth: 0 }"
    class="w-full"
  >
    <template
      v-for="(field, index) in visibleFields"
      :key="field.fieldId ?? field._key"
    >
      <NGridItem
        v-if="isComposite(field.fieldType)"
        :span="fieldSpan(field)"
      >
        <CompositeWidget
          :field="field"
          mode="preview"
          :values="values"
          :disabled="disabled"
          :index="index"
          :total="visibleFields.length"
          :label-placement="labelPlacement"
          :label-width="labelWidth"
          :label-align="labelAlign"
          :size="size"
          @update:field-value="payload => emit('update:fieldValue', payload)"
          @add-row="(field) => emit('addRow', field)"
          @remove-row="(field, index) => emit('removeRow', field, index)"
        />
      </NGridItem>
      <NFormItemGi
        v-else
        :span="fieldSpan(field)"
        :label="field.fieldLabel || '未命名字段'"
        :show-required-mark="field.required"
        :label-placement="labelPlacement"
        :label-width="labelPlacement === 'left' ? labelWidth : undefined"
        :label-align="labelAlign"
        :show-feedback="false"
      >
        <FieldWidget
          :field="field"
          mode="preview"
          control-only
          :values="values"
          :disabled="disabled"
          :errors="errors"
          :size="size"
          @update:field-value="handleFieldValue"
        />
      </NFormItemGi>
    </template>

    <NGridItem
      v-if="!visibleFields.length"
      :span="24"
      class="py-16"
    >
      <NEmpty
        size="small"
        :description="emptyDescription || '暂无可见字段'"
      />
    </NGridItem>
  </NGrid>
</template>

<script setup>
import { NEmpty, NFormItemGi, NGrid, NGridItem } from 'naive-ui'
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
  /** 空状态是否占满容器并上下居中（用于独立预览场景） */
  emptyCentered: { type: Boolean, default: false },
  /** 字段结构校验错误映射：field -> 错误数组 */
  structureErrors: { type: Object, default: () => new Map() },
})

const emit = defineEmits(['select', 'delete', 'move', 'update:fieldValue', 'addRow', 'removeRow'])

/** 栅格间距（px）：24 栅格下间隙必须小，否则 23 个间隙会挤占/超出容器宽度 */
const gridGap = 4

/** 将字段值变更转发给父级（由父级写入预览态 values） */
function handleFieldValue({ field, value }) {
  emit('update:fieldValue', { field, value })
}

/** 24 栅格列宽 -> 栅格 span */
function fieldSpan(field) {
  return Math.min(Math.max(Number(field?.colSpan) || 24, 1), 24)
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
