<template>
  <NInput
    :value="modelValue"
    type="textarea"
    :rows="6"
    :placeholder="field.placeholder || '请输入富文本内容'"
    :disabled="disabled"
    :size="size"
    @update:value="value => emit('update:modelValue', value)"
  />
</template>

<script setup>
import { NInput } from 'naive-ui'

defineOptions({ name: 'RichTextWidget' })

defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])
</script>
