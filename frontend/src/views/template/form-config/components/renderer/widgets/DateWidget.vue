<template>
  <NDatePicker
    :value="modelValue"
    :type="field.fieldType === 'DATETIME' ? 'datetime' : 'date'"
    :format="fieldProps.dateFormat"
    :placeholder="field.placeholder || '请选择日期'"
    :disabled="disabled"
    :size="size"
    clearable
    class="w-full!"
    @update:value="value => emit('update:modelValue', value)"
  />
</template>

<script setup>
import { NDatePicker } from 'naive-ui'
import { computed } from 'vue'
import { parseFieldProps } from '../helpers'

defineOptions({ name: 'DateWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])

const fieldProps = computed(() => parseFieldProps(props.field))
</script>
