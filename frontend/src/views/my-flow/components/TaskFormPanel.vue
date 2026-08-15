<template>
  <div v-if="taskForms?.length" class="space-y-12">
    <div
      v-for="task in taskForms"
      :key="task.taskDefinitionKey"
      class="mb-12 last:mb-0"
    >
      <div class="mb-6 text-13 font-600">
        {{ task.taskName || task.taskDefinitionKey }}
      </div>
      <BuilderProvider v-if="task.formType === 'FORM_TEMPLATE' && task.definition" :config="formBuilderConfig">
        <FormSchemaRenderer
          :model-value="taskFormValues[task.taskDefinitionKey]"
          :definition="task.definition"
          :http="request"
          :actions="false"
          label-position="top"
          @update:model-value="val => taskFormValues[task.taskDefinitionKey] = val"
        />
      </BuilderProvider>
      <component
        :is="getFormKeyComponent(task.formKey)"
        v-else-if="task.formType === 'FORM_KEY' && task.formKey"
        :ref="el => setFormKeyRef(el, task.taskDefinitionKey)"
        :task-id="task.taskId"
      />
      <FormPreview
        v-else-if="task.formType === 'FORM_DATA' && task.fields?.length"
        :fields="task.fields"
        :show-reset="false"
        :show-submit="false"
      />
      <div v-else class="text-12 text-gray-400">
        未定义表单
      </div>
    </div>
  </div>
  <div v-else class="py-20 text-center text-12 text-gray-400">
    暂无任务表单
  </div>
</template>

<script setup>
import { FormPreview } from '@zeng-alt/camunda7-ui'
import { BuilderProvider, FormSchemaRenderer } from '@zeng-alt/formkit-form-builder'
import { defineAsyncComponent, markRaw, onBeforeUpdate, ref, watch } from 'vue'
import { request } from '@/utils'
import { createFormBuilderConfig } from '@/views/template/form/formBuilderConfig'

defineOptions({ name: 'TaskFormPanel' })

const props = defineProps({
  /** 任务表单定义列表 */
  taskForms: {
    type: Array,
    default: () => [],
  },
})

const formBuilderConfig = createFormBuilderConfig()

/** 动态加载 FORM_KEY 对应的外部表单组件（路径以 /src/views/ 开头） */
const viewModules = import.meta.glob('/src/views/**/*.vue')
const formKeyComponentCache = new Map()

function getFormKeyComponent(formKey) {
  if (!formKey)
    return null
  const key = formKey.startsWith('/') ? formKey : `/src/views/${formKey}`
  if (formKeyComponentCache.has(key))
    return formKeyComponentCache.get(key)
  const loader = viewModules[key]
  if (!loader)
    return null
  const component = markRaw(defineAsyncComponent(loader))
  formKeyComponentCache.set(key, component)
  return component
}

/** FORM_TEMPLATE 表单填写值：key = taskDefinitionKey，value = 表单数据对象 */
const taskFormValues = ref({})

/** FORM_KEY 组件实例引用：key = taskDefinitionKey */
const formKeyRefs = ref({})

onBeforeUpdate(() => {
  formKeyRefs.value = {}
})

watch(() => props.taskForms, () => {
  taskFormValues.value = {}
  formKeyRefs.value = {}
}, { deep: true })

function setFormKeyRef(el, key) {
  if (el)
    formKeyRefs.value[key] = el
}

/** 收集所有表单数据并合并为流程变量对象 */
function getFormValues() {
  const variables = {}
  props.taskForms.forEach((task) => {
    const key = task.taskDefinitionKey
    if (task.formType === 'FORM_TEMPLATE') {
      const data = taskFormValues.value[key]
      if (data && typeof data === 'object')
        Object.assign(variables, data)
    }
    else if (task.formType === 'FORM_KEY') {
      const formKeyRef = formKeyRefs.value[key]
      if (formKeyRef && typeof formKeyRef.getData === 'function') {
        const data = formKeyRef.getData()
        if (data && typeof data === 'object')
          Object.assign(variables, data)
      }
    }
  })
  return variables
}

defineExpose({ getFormValues })
</script>
