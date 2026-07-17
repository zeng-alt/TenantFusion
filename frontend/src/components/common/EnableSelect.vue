<template>
  <NSelect
    v-bind="bindAttrs"
    :value="value"
    :options="options"
    @update:value="handleUpdate"
  />
</template>

<script setup>
import { NSelect } from 'naive-ui'
import { computed, useAttrs } from 'vue'

defineOptions({ name: 'EnableSelect' })

const props = defineProps({
  value: { default: true },
  activeValue: { default: true },
  inactiveValue: { default: false },
  activeLabel: { type: String, default: '启用' },
  inactiveLabel: { type: String, default: '停用' },
})

const emit = defineEmits(['update:value'])
const attrs = useAttrs()

const bindAttrs = computed(() => {
  const { onUpdateValue, ...rest } = attrs
  return rest
})

const options = computed(() => [
  { label: props.activeLabel, value: props.activeValue },
  { label: props.inactiveLabel, value: props.inactiveValue },
])

function handleUpdate(v) {
  emit('update:value', v)
}
</script>
