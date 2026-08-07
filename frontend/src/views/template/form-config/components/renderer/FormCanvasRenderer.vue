<template>
  <div
    class="form-canvas-renderer h-full w-full"
    @click="emit('deselect')"
  >
    <!-- 空状态 -->
    <div
      v-if="!fields.length"
      class="h-full flex flex-col items-center justify-center gap-14 px-16"
    >
      <NEmpty
        size="large"
        description="暂无字段，请从左侧字段库添加"
      >
        <template #icon>
          <i class="i-material-symbols:add-box text-48 text-gray-300 dark:text-gray-600" />
        </template>
      </NEmpty>
      <div class="flex flex-wrap items-center justify-center gap-6">
        <span class="text-12 text-gray-400 dark:text-gray-500">快速添加：</span>
        <NButton
          v-for="type in QUICK_TYPES"
          :key="type"
          size="small"
          type="primary"
          secondary
          @click.stop="emit('add', type)"
        >
          <template #icon>
            <i class="text-14" :class="[FIELD_TYPE_META[type].icon]" />
          </template>
          {{ FIELD_TYPE_META[type].label }}
        </NButton>
      </div>
    </div>

    <!-- 画布 -->
    <NScrollbar
      v-else
      class="h-full"
    >
      <div class="p-10">
        <FormRenderer
          :fields="fields"
          mode="design"
          :selected-field="selectedField"
          :values="values"
          :label-placement="labelPlacement"
          :label-width="labelWidth"
          :label-align="labelAlign"
          :size="size"
          :structure-errors="structureErrors"
          @select="emit('select', $event)"
          @delete="emit('delete', $event)"
          @move="(field, direction) => emit('move', field, direction)"
          @update:field-value="payload => emit('update:fieldValue', payload)"
        />
      </div>
    </NScrollbar>
  </div>
</template>

<script setup>
import { NButton, NEmpty, NScrollbar } from 'naive-ui'
import { FIELD_TYPE_META } from '../../constants'
import FormRenderer from './FormRenderer.vue'

defineOptions({ name: 'FormCanvasRenderer' })

defineProps({
  fields: { type: Array, default: () => [] },
  selectedField: { type: Object, default: null },
  /** 设计态画布输入值 { [fieldKey]: value } */
  values: { type: Object, default: null },
  /** 表单级标签配置 */
  labelPlacement: { type: String, default: 'left' },
  labelWidth: { type: Number, default: 90 },
  labelAlign: { type: String, default: 'right' },
  /** 表单级控件尺寸 */
  size: { type: String, default: 'medium' },
  /** 字段结构校验错误映射：field -> 错误数组 */
  structureErrors: { type: Object, default: () => new Map() },
})

const emit = defineEmits(['select', 'delete', 'move', 'add', 'deselect', 'update:fieldValue'])

/** 空状态快捷添加的常用字段类型 */
const QUICK_TYPES = ['STRING', 'TEXTAREA', 'NUMBER', 'BOOLEAN', 'SELECT', 'DATE']
</script>
