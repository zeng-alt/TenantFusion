<template>
  <NUpload
    :default-file-list="fileList"
    :max="fieldProps.maxCount"
    :disabled="disabled"
    :list-type="field.fieldType === 'IMAGE' ? 'image-card' : 'text'"
    v-bind="bindProps"
    accept=""
  >
    <NButton
      size="small"
      :disabled="disabled"
    >
      <template #icon>
        <i class="text-14" :class="[field.fieldType === 'IMAGE' ? 'i-material-symbols:image' : 'i-material-symbols:attach-file']" />
      </template>
      {{ field.fieldType === 'IMAGE' ? '上传图片' : '上传文件' }}
    </NButton>
  </NUpload>
</template>

<script setup>
import { NButton, NUpload } from 'naive-ui'
import { computed } from 'vue'
import { buildBindProps, parseFieldProps } from '../helpers'

defineOptions({ name: 'UploadWidget' })

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: null, default: null },
  disabled: { type: Boolean, default: false },
  size: { type: String, default: 'medium' },
})

const fieldProps = computed(() => parseFieldProps(props.field))

const bindProps = computed(() => buildBindProps(props.field, ['default-file-list', 'defaultFileList', 'list-type', 'listType']))

/** 文件列表：modelValue 为 URL 数组或 file 数组 */
const fileList = computed(() => {
  const value = props.modelValue
  if (Array.isArray(value)) {
    return value.map((item, index) => {
      if (typeof item === 'string')
        return { id: `f${index}`, url: item, name: item.split('/').pop() || item }
      return item
    })
  }
  return []
})
</script>
