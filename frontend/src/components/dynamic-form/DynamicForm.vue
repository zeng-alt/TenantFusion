<template>
  <div class="w-full">
    <NSpin :show="loading" class="py-24">
      <template v-if="definition">
        <BuilderProvider :config="config">
          <FormSchemaRenderer
            ref="rendererRef"
            :definition="definition"
            :model-value="formData"
            :http="request"
            :actions="false"
            label-position="top"
            @update:model-value="onDataUpdate"
            @submit="onValidSubmit"
          />
        </BuilderProvider>

        <div
          v-if="actions && !readonly"
          class="flex justify-end gap-12 pt-16"
        >
          <NButton :loading="submitting" @click="reset">
            {{ resetLabel }}
          </NButton>
          <NButton type="primary" :loading="submitting" @click="submit">
            {{ submitLabel }}
          </NButton>
        </div>
      </template>

      <NEmpty v-else-if="!loading" description="表单定义为空" />
    </NSpin>
  </div>
</template>

<script setup>
import { BuilderProvider, FormSchemaRenderer } from '@zeng-alt/formkit-form-builder'
import { NButton, NEmpty, NSpin } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { request } from '@/utils'
import { createFormBuilderConfig } from '@/views/template/form/formBuilderConfig'
import api from './api'

defineOptions({ name: 'DynamicForm' })

const props = defineProps({
  /** 表单模板编码（优先于 definition：拉取当前已发布定义） */
  code: {
    type: String,
    default: '',
  },
  /** 直接传入的 FormDefinition（未传 code 时使用） */
  definition: {
    type: Object,
    default: null,
  },
  /** 初始 / 回填表单数据 */
  modelValue: {
    type: Object,
    default: null,
  },
  /** 是否只读（不渲染操作区，仅回填展示） */
  readonly: {
    type: Boolean,
    default: false,
  },
  /** 是否渲染提交 / 重置操作区 */
  actions: {
    type: Boolean,
    default: true,
  },
  /** 提交按钮文案 */
  submitLabel: {
    type: String,
    default: '提交',
  },
  /** 重置按钮文案 */
  resetLabel: {
    type: String,
    default: '重置',
  },
  /** 提交后是否自动写入 /v1/form-data；false 时仅 emit('submit') 交由外部处理 */
  submitToBackend: {
    type: Boolean,
    default: true,
  },
  /** 表单模板ID（提交时使用；缺省取按 code 加载到的模板ID） */
  formTemplateId: {
    type: Number,
    default: null,
  },
  /** 表单版本快照（提交时使用；缺省取按 code 加载到的版本号） */
  formVersion: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue', 'submit', 'error'])

const config = createFormBuilderConfig()

const loading = ref(false)
const submitting = ref(false)
const rendererRef = ref(null)
const loaded = ref(null)
const formData = ref({})

const definition = computed(() => props.definition ?? loaded.value?.definition ?? null)
const formTemplateId = computed(() => props.formTemplateId ?? loaded.value?.formTemplateId ?? null)
const formVersion = computed(() => props.formVersion ?? loaded.value?.version ?? null)

watch(
  () => props.modelValue,
  (value) => {
    if (value && value !== formData.value)
      formData.value = { ...value }
  },
  { deep: true },
)

watch(
  () => props.code,
  (code) => {
    if (code)
      loadDefinition(code)
    else
      loaded.value = null
  },
  { immediate: true },
)

async function loadDefinition(code) {
  loading.value = true
  try {
    const { data } = await api.definitionByCode(code)
    loaded.value = data || null
  }
  catch (error) {
    console.error(error)
    loaded.value = null
    $message.error(error?.message || '加载表单定义失败')
  }
  finally {
    loading.value = false
  }
}

function onDataUpdate(value) {
  formData.value = value || {}
  emit('update:modelValue', formData.value)
}

/** 触发渲染器提交：FormKit 校验通过才会回调 onValidSubmit */
function submit() {
  rendererRef.value?.submit?.()
}

function reset() {
  rendererRef.value?.reset?.()
}

async function onValidSubmit(formValue) {
  const meta = {
    formTemplateId: formTemplateId.value,
    formVersion: formVersion.value,
  }
  if (!props.submitToBackend) {
    emit('submit', formValue, meta)
    return
  }
  if (!meta.formTemplateId) {
    $message.warning('缺少表单模板ID，无法提交')
    emit('error', null)
    return
  }
  submitting.value = true
  try {
    await api.submitFormData({
      formTemplateId: meta.formTemplateId,
      formVersion: meta.formVersion,
      data: JSON.stringify(formValue),
      status: 'SUBMITTED',
    })
    $message.success('提交成功')
    emit('submit', formValue, meta)
  }
  catch (error) {
    // 服务端字段级校验错误（Problem Details 的 errors 扩展属性）
    const errors = error?.error?.errors
    if (errors && typeof errors === 'object' && Object.keys(errors).length) {
      emit('error', errors)
      const first = Object.values(errors)[0]
      if (first)
        $message.warning(first)
    }
    else {
      emit('error', null)
      $message.error(error?.message || '提交失败')
    }
  }
  finally {
    submitting.value = false
  }
}

defineExpose({ submit, reset, formData })
</script>
