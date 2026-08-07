<template>
  <NSwitch
    :value="!!modelValue"
    :disabled="disabled"
    :size="size"
    @update:value="value => emit('update:modelValue', value)"
  />
</template>

<script setup>
import { NSwitch } from 'naive-ui'

defineOptions({ name: 'BooleanWidget' })

defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])
</script>
