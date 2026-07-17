<template>
  <NSwitch
    v-bind="bindAttrs"
    :value="value"
    @update:value="handleUpdate"
  >
    <template #checked>
      {{ checkedLabel }}
    </template>
    <template #unchecked>
      {{ uncheckedLabel }}
    </template>
  </NSwitch>
</template>

<script setup>
import { NSwitch } from 'naive-ui'
import { computed, useAttrs } from 'vue'

defineOptions({ name: 'EnableSwitch' })

defineProps({
  value: { type: Boolean, default: true },
  checkedLabel: { type: String, default: '启用' },
  uncheckedLabel: { type: String, default: '停用' },
})

const emit = defineEmits(['update:value'])
const attrs = useAttrs()

const bindAttrs = computed(() => {
  const { onUpdateValue, ...rest } = attrs
  return rest
})

function handleUpdate(v) {
  emit('update:value', v)
}
</script>
