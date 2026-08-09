<template>
  <NSwitch
    :value="!!modelValue"
    :disabled="disabled"
    :size="size"
    v-bind="bindProps"
    @update:value="value => emit('update:modelValue', value)"
  />
</template>

<script setup>
import { NSwitch } from 'naive-ui'
import { computed } from 'vue'
import { buildBindProps } from '../helpers'

defineOptions({ name: 'BooleanWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:modelValue'])

const bindProps = computed(() => buildBindProps(props.field))
</script>
