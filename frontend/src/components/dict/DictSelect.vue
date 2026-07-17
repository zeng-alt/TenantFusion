<template>
  <NSelect
    v-bind="bindAttrs"
    :value="value"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="filterable"
    :multiple="multiple"
    :disabled="disabled"
    :size="size"
    :options="options"
    :loading="loading"
    @update:value="handleUpdate"
  />
</template>

<script setup>
import { NSelect } from 'naive-ui'
import { computed, useAttrs } from 'vue'
import { useDict } from '@/composables'

defineOptions({ name: 'DictSelect' })

const props = defineProps({
  code: { type: String, required: true },
  value: { default: null },
  placeholder: { type: String, default: '请选择' },
  clearable: { type: Boolean, default: false },
  filterable: { type: Boolean, default: false },
  multiple: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const emit = defineEmits(['update:value'])
const attrs = useAttrs()

const { options, loading } = useDict(props.code)

const bindAttrs = computed(() => {
  const { onUpdateValue, ...rest } = attrs
  return rest
})

function handleUpdate(v) {
  emit('update:value', v)
}
</script>
