<template>
  <NInputNumber
    :value="modelValue"
    :min="fieldProps.min"
    :max="fieldProps.max"
    :step="fieldProps.step"
    :precision="fieldProps.precision"
    :placeholder="field.placeholder || '请输入数字'"
    :disabled="disabled"
    :size="size"
    class="w-full!"
    @update:value="value => emit('update:modelValue', value)"
  />
</template>

<script setup>
import { NInputNumber } from 'naive-ui'
import { computed } from 'vue'
import { parseFieldProps } from '../helpers'

defineOptions({ name: 'NumberWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])

const fieldProps = computed(() => parseFieldProps(props.field))
</script>
