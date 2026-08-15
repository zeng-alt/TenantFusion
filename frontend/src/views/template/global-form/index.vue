<template>
  <CommonPage>
    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.read"
      row-key="globalFormDataId"
      :scroll-x="1100"
    >
      <MeQueryItem label="流程编码" :label-width="70">
        <NInput
          v-model:value="queryItems.workflowCode"
          type="text"
          placeholder="请输入流程模板编码"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="流程实例" :label-width="70">
        <NInput
          v-model:value="queryItems.processInstanceId"
          type="text"
          placeholder="请输入流程实例ID"
          clearable
        />
      </MeQueryItem>
    </MeCrud>

    <MeModal ref="previewRef" width="720px" :show-footer="false">
      <div v-if="previewLoading" class="flex justify-center py-32">
        <NSpin />
      </div>
      <template v-else>
        <div class="mb-12 flex flex-wrap items-center gap-8 text-13 text-gray-500">
          <NTag size="small" type="info" bordered>
            {{ previewWorkflowCode }}
          </NTag>
          <span>流程实例：{{ previewProcessInstanceId }}</span>
          <span v-if="previewSubmittedDate">
            提交时间：{{ formatDateTime(previewSubmittedDate) }}
          </span>
        </div>
        <BuilderProvider v-if="previewDefinition" :config="formBuilderConfig">
          <FormSchemaRenderer
            :model-value="previewData"
            :definition="previewDefinition"
            :http="request"
            :actions="false"
            label-position="top"
          />
        </BuilderProvider>
        <template v-else-if="previewFields">
          <div class="mb-8 text-13 font-600">
            表单定义（GENERATED）
          </div>
          <pre class="whitespace-pre-wrap break-all text-12 text-gray-500">{{ JSON.stringify(previewFields, null, 2) }}</pre>
          <div class="mb-8 mt-12 text-13 font-600">
            表单数据
          </div>
          <pre class="whitespace-pre-wrap break-all text-12 text-gray-500">{{ JSON.stringify(previewData, null, 2) }}</pre>
        </template>
        <template v-else-if="previewFormKey">
          <div class="mb-8 text-13 font-600">
            外部表单（EXTERNAL）
          </div>
          <NTag size="small" type="info" bordered>
            {{ previewFormKey }}
          </NTag>
          <div class="mb-8 mt-12 text-13 font-600">
            表单数据
          </div>
          <pre class="whitespace-pre-wrap break-all text-12 text-gray-500">{{ JSON.stringify(previewData, null, 2) }}</pre>
        </template>
        <NEmpty v-else description="该流程未配置全局表单定义" />
      </template>
    </MeModal>
  </CommonPage>
</template>

<script setup>
import { BuilderProvider, FormSchemaRenderer } from '@zeng-alt/formkit-form-builder'
import { NButton, NEmpty, NInput, NSpin, NTag } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { CommonPage, MeCrud, MeModal, MeQueryItem } from '@/components'
import { formatDateTime, request } from '@/utils'
import { createFormBuilderConfig } from '@/views/template/form/formBuilderConfig'
import api from './api'

defineOptions({ name: 'GlobalFormMgt' })

const $table = ref(null)
const queryItems = ref({})
const previewRef = ref(null)
const previewLoading = ref(false)
const previewDefinition = ref(null)
const previewFields = ref(null)
const previewFormKey = ref('')
const previewData = ref({})
const previewWorkflowCode = ref('')
const previewProcessInstanceId = ref('')
const previewSubmittedDate = ref('')
const formBuilderConfig = createFormBuilderConfig()

onMounted(() => {
  $table.value?.handleSearch()
})

/** 将后端返回的 JsonNode 数据转成普通对象（非对象时返回空对象） */
function toPlainObject(data) {
  return data && typeof data === 'object' && !Array.isArray(data) ? { ...data } : {}
}

/** 预览：EXTERNAL/GENERATED 用发起时保存的定义快照；CAMUNDA 实时解析最新版本，并用记录数据回填只读渲染 */
async function openPreview(row) {
  previewLoading.value = true
  previewDefinition.value = null
  previewFields.value = null
  previewFormKey.value = ''
  previewData.value = {}
  previewWorkflowCode.value = row.workflowCode
  previewProcessInstanceId.value = row.processInstanceId
  previewSubmittedDate.value = row.submittedDate
  previewRef.value?.open({ title: '预览全局表单数据' })
  try {
    previewData.value = toPlainObject(row.data)
    const snapshot = toPlainObject(row.definition)
    if (snapshot?.type === 'EXTERNAL') {
      previewFormKey.value = snapshot.formKey || ''
    }
    else if (snapshot?.type === 'GENERATED') {
      previewFields.value = snapshot.fields || null
    }
    else {
      const { data: definition } = await api.definition(row.workflowCode)
      previewDefinition.value = definition?.definition || null
      if (!previewDefinition.value)
        $message.warning('该流程未配置全局表单定义')
    }
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '加载表单定义失败')
  }
  finally {
    previewLoading.value = false
  }
}

const columns = [
  {
    title: '流程模板编码',
    key: 'workflowCode',
    width: 200,
    ellipsis: { tooltip: true },
  },
  {
    title: '流程实例ID',
    key: 'processInstanceId',
    minWidth: 220,
    ellipsis: { tooltip: true },
  },
  {
    title: '提交人',
    key: 'lastModifiedBy',
    width: 120,
    render: row => row.lastModifiedBy || row.createdBy || '—',
  },
  {
    title: '提交时间',
    key: 'submittedDate',
    width: 180,
    render: row => formatDateTime(row.submittedDate),
  },
  {
    title: '更新时间',
    key: 'lastModifiedDate',
    width: 180,
    render: row => formatDateTime(row.lastModifiedDate),
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    align: 'right',
    fixed: 'right',
    render(row) {
      return h(
        NButton,
        {
          size: 'small',
          type: 'primary',
          onClick: () => openPreview(row),
        },
        {
          default: () => '预览',
          icon: () => h('i', { class: 'i-carbon:view text-14' }),
        },
      )
    },
  },
]
</script>
