<template>
  <div class="h-full w-full flex flex-col">
    <div
      class="flex items-center justify-between border-b border-gray-200 px-16 py-8 dark:border-gray-700"
    >
      <div class="flex items-center">
        <i class="i-carbon:draw text-18" />
        <span class="ml-8 text-16 font-600">
          {{ form?.name || '未命名表单' }}
        </span>
        <NTag v-if="form" size="small" type="primary" bordered class="ml-8">
          v{{ form?.version ?? 1 }}
        </NTag>
      </div>
      <NSpace>
        <NButton @click="handleClose">
          取消
        </NButton>
        <NButton type="primary" ghost :loading="saving" @click="handleSave">
          保存
        </NButton>
        <NButton type="primary" :loading="saving" @click="handleSaveAndPublish">
          保存并发布
        </NButton>
      </NSpace>
    </div>

    <div v-if="loading" class="flex flex-1 items-center justify-center">
      <NSpin />
    </div>
    <div v-else class="min-h-0 flex-1 overflow-hidden">
      <BuilderProvider :config="formBuilderConfig">
        <FormBuilder v-model="definition" />
      </BuilderProvider>
    </div>
  </div>
</template>

<script setup>
import { BuilderProvider, FormBuilder } from '@zeng-alt/formkit-form-builder'
import {
  NButton,
  NSpace,
  NSpin,
  NTag,
} from 'naive-ui'
import { onMounted, ref } from 'vue'
import { randomInt } from '@/utils'
import api from '../api'
import { createFormBuilderConfig } from '../formBuilderConfig'

defineOptions({ name: 'FormDesigner' })

const props = defineProps({
  template: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close', 'saved'])

const DEFAULT_DEFINITION = {
  version: 1,
  id: `form_${randomInt()}`,
  name: '未命名表单',
  settings: { layout: 'vertical', labelWidth: 80 },
  root: {
    id: 'root',
    type: 'group',
    category: 'container',
    renderAs: 'cmp',
    name: 'form',
    children: [
      {
        id: 'f-name',
        type: 'text',
        category: 'field',
        renderAs: 'cmp',
        name: 'name',
        label: '名称',
        layout: { colspan: 12 },
        validation: [{ rule: 'required', message: '名称为必填项' }],
      },
    ],
  },
}

function deepClone(value) {
  return value ? JSON.parse(JSON.stringify(value)) : value
}

const loading = ref(true)
const saving = ref(false)

const form = ref(props.template)

/** 无版本数据时，用表单模板自身的 name/code/版本号 填充默认定义 */
function createDefaultDefinition() {
  const f = form.value || {}
  return {
    ...deepClone(DEFAULT_DEFINITION),
    id: f.code || DEFAULT_DEFINITION.id,
    name: f.name || DEFAULT_DEFINITION.name,
    version: f.latestVersion || DEFAULT_DEFINITION.version,
  }
}

const definition = ref(createDefaultDefinition())

const formBuilderConfig = createFormBuilderConfig()

onMounted(async () => {
  try {
    definition.value = await loadDefinition()
  }
  catch (error) {
    console.error(error)
    $message.error('加载表单定义失败')
    definition.value = createDefaultDefinition()
  }
  finally {
    loading.value = false
  }
})

async function loadDefinition() {
  const f = form.value
  if (!f?.formTemplateId)
    return createDefaultDefinition()
  const { data } = await api.versionDetail(f.versionId)
  f.version = data.version
  return data?.definition || createDefaultDefinition()
}

function handleClose() {
  emit('close')
}

async function doSave(publish) {
  if (saving.value)
    return
  saving.value = true
  try {
    await persist(form.value?.formTemplateId, publish)
    onPersisted(publish)
  }
  catch (error) {
    console.error(error)
    $message.error(`${publish ? '保存并发布' : '保存'}失败: ${error?.message || error}`)
  }
  finally {
    saving.value = false
  }
}

/** 统一入口：单个接口原子完成「保存」或「保存并发布」，新建 (id=0) 与编辑共用 */
async function persist(id, publish) {
  const action = publish ? api.publishDraft : api.saveDraft
  const { data } = await action(id, { definition: definition.value })
  return data
}

function onPersisted(publish) {
  if (publish) {
    emit('saved')
    emit('close')
    $message.success('保存并发布成功')
  }
  else {
    $message.success('保存成功')
  }
}

function handleSave() {
  doSave(false)
}

function handleSaveAndPublish() {
  doSave(true)
}
</script>
