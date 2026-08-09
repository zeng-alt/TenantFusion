<template>
  <NInput
    :value="modelValue"
    type="textarea"
    :rows="fieldProps.rows || 3"
    :maxlength="fieldProps.maxLength"
    :show-count="fieldProps.showCount"
    :placeholder="field.placeholder || `请输入${field.fieldLabel || '内容'}`"
    :disabled="disabled"
    :size="size"
    v-bind="bindProps"
    @update:value="value => emit('update:modelValue', value)"
  />
</template>

<script setup>
import { NInput } from 'naive-ui'
import { computed } from 'vue'
import { buildBindProps, parseFieldProps } from '../helpers'

defineOptions({ name: 'TextareaWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])

const fieldProps = computed(() => parseFieldProps(props.field))

const bindProps = computed(() => buildBindProps(props.field, ['type']))
</script>
