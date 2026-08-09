<template>
  <div class="custom-attrs-editor w-full">
    <div
      v-for="(attr, index) in attrs"
      :key="index"
      class="mb-6 flex items-center gap-6 border border-light_border rounded-6 border-dashed px-8 py-6 transition-colors dark:border-dark_border"
    >
      <span class="w-16 shrink-0 text-center text-11 text-gray-400">
        {{ index + 1 }}
      </span>
      <NInput
        v-model:value="attr.key"
        size="small"
        placeholder="属性名，如 maxlength"
        class="w-150!"
      />
      <NInput
        v-model:value="attr.value"
        size="small"
        placeholder="属性值（文本）"
        class="flex-1!"
      />
      <NButton
        size="tiny"
        type="error"
        quaternary
        circle
        @click="removeAttr(index)"
      >
        <template #icon>
          <i class="i-material-symbols:delete-outline text-14" />
        </template>
      </NButton>
    </div>

    <NButton
      size="small"
      type="primary"
      dashed
      block
      @click="addAttr"
    >
      <template #icon>
        <i class="i-material-symbols:add text-14" />
      </template>
      添加自定义属性
    </NButton>
  </div>
</template>

<script setup>
import { NButton, NInput } from 'naive-ui'
import { ref, watch } from 'vue'

defineOptions({ name: 'CustomAttrsEditor' })

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const attrs = ref(props.modelValue || [])

watch(
  () => props.modelValue,
  (val) => {
    attrs.value = val || []
  },
  { deep: true },
)

watch(
  attrs,
  (val) => {
    emit('update:modelValue', val)
  },
  { deep: true },
)

function addAttr() {
  attrs.value = [...attrs.value, { key: '', value: '' }]
}

function removeAttr(index) {
  attrs.value = attrs.value.filter((_, i) => i !== index)
}
</script>
