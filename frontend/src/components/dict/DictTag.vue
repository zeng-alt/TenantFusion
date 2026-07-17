<template>
  <NTag
    v-bind="bindAttrs"
    :type="tagType"
    :size="size"
    :closable="closable"
    :bordered="bordered"
    :round="round"
  >
    {{ label }}
  </NTag>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, useAttrs } from 'vue'
import { useDict } from '@/composables'

defineOptions({ name: 'DictTag' })

const props = defineProps({
  code: { type: String, required: true },
  value: { default: null },
  size: { type: String, default: 'medium' },
  closable: { type: Boolean, default: false },
  bordered: { type: Boolean, default: true },
  round: { type: Boolean, default: false },
})

const attrs = useAttrs()
const { getLabel, getTagType, dictData } = useDict(props.code)

const bindAttrs = computed(() => {
  const { type, ...rest } = attrs
  return rest
})

const label = computed(() => {
  if (dictData.value.length)
    return getLabel(props.value)
  return props.value
})

const tagType = computed(() => {
  if (dictData.value.length)
    return getTagType(props.value)
  return 'default'
})
</script>
