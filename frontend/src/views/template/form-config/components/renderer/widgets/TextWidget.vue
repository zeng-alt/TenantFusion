<template>
  <NInput
    :value="modelValue"
    :placeholder="field.placeholder || `请输入${field.fieldLabel || '内容'}`"
    :disabled="disabled"
    :size="size"
    :maxlength="fieldProps.maxLength"
    clearable
    v-bind="bindProps"
    @update:value="value => emit('update:modelValue', value)"
  />
</template>

<script setup>
import { NInput } from 'naive-ui'
import { computed } from 'vue'
import { buildBindProps, parseFieldProps } from '../helpers'

defineOptions({ name: 'TextWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])

const fieldProps = computed(() => parseFieldProps(props.field))

const bindProps = computed(() => buildBindProps(props.field))
</script>
