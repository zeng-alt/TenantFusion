<template>
  <NSelect
    :value="modelValue"
    :options="options"
    :multiple="field.fieldType === 'MULTI_SELECT'"
    :placeholder="field.placeholder || '请选择'"
    :disabled="disabled"
    :size="size"
    clearable
    filterable
    @update:value="value => emit('update:modelValue', value)"
  />
</template>

<script setup>
import { NSelect } from 'naive-ui'
import { computed } from 'vue'

defineOptions({ name: 'SelectWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])

const options = computed(() =>
  (props.field.options || []).map(opt => ({
    label: opt.label,
    value: opt.value,
  })),
)
</script>
