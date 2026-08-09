<template>
  <CommonPage>
    <template #action>
      <NButton type="primary" @click="handleAdd()">
        <i class="i-material-symbols:add mr-4 text-14" />
        新建流程
      </NButton>
    </template>

    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.list"
      row-key="id"
      :scroll-x="1200"
    >
      <MeQueryItem label="流程名称" :label-width="70">
        <NInput
          v-model:value="queryItems.workflowName"
          type="text"
          placeholder="请输入流程名称"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="流程Key" :label-width="60">
        <NInput
          v-model:value="queryItems.workflowKey"
          type="text"
          placeholder="请输入流程Key"
          clearable
        />
      </MeQueryItem>
    </MeCrud>

    <MeModal ref="modalRef" width="520px">
      <NForm
        ref="modalFormRef"
        label-placement="left"
        :label-width="100"
        :model="modalForm"
      >
        <n-form-item path="workflowName" :rule="required">
          <template #label>
            流程名称
          </template>
          <NInput v-model:value="modalForm.workflowName" :disabled="modalAction === 'edit'" />
        </n-form-item>
        <n-form-item path="workflowKey" :rule="required">
          <template #label>
            流程Key
          </template>
          <NInputGroup>
            <NInput
              v-model:value="modalForm.workflowKey"
              :disabled="modalAction === 'edit'"
              placeholder="如：leave-approval"
            />
            <NButton :disabled="modalAction === 'edit'" type="primary" ghost @click="generateKey">
              <template #icon>
                <i class="i-carbon:renew text-14" />
              </template>
              生成
            </NButton>
          </NInputGroup>
        </n-form-item>
        <n-form-item path="category" label="分类">
          <NTreeSelect
            v-model:value="categoryKey"
            :options="categoryTree"
            :loading="categoryLoading"
            clearable
            filterable
            placeholder="请选择业务分类"
            @update:value="handleCategorySelect"
          />
        </n-form-item>
        <n-form-item path="description" label="描述">
          <NInput v-model:value="modalForm.description" type="textarea" />
        </n-form-item>
        <n-form-item path="remark" label="备注">
          <NInput v-model:value="modalForm.remark" type="textarea" />
        </n-form-item>
      </NForm>
    </MeModal>
  </CommonPage>
</template>

<script setup>
import { NButton, NInput, NInputGroup, NTag, NTreeSelect } from 'naive-ui'
import { h, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { CommonPage, MeCrud, MeModal, MeQueryItem } from '@/components'
import { useCrud } from '@/composables'
import { randomKey } from '@/utils'
import api from './api'

defineOptions({ name: 'ProcessTemplateMgt' })

const router = useRouter()
const $table = ref(null)
const queryItems = ref({})

const categoryTree = ref([])
const categoryLoading = ref(false)
const categoryKey = ref(null)

onMounted(() => {
  $table.value?.handleSearch()
  loadBusinessTree()
})

async function loadBusinessTree() {
  categoryLoading.value = true
  try {
    const { data } = await api.businessTree()
    categoryTree.value = mapBusinessTree(data || [])
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '加载业务分类失败')
  }
  finally {
    categoryLoading.value = false
  }
}

function mapBusinessTree(nodes) {
  return (nodes || []).map(node => ({
    key: node.businessId,
    label: node.name + (node.code ? `（${node.code}）` : ''),
    name: node.name,
    code: node.code,
    children: mapBusinessTree(node.children),
  }))
}

function findNode(nodes, key) {
  for (const node of nodes || []) {
    if (node.key === key)
      return node
    const found = findNode(node.children, key)
    if (found)
      return found
  }
  return null
}

function findNodeKey(nodes, code) {
  if (!code)
    return null
  for (const node of nodes || []) {
    if (node.code === code || node.name === code || node.label === code)
      return node.key
    const found = findNodeKey(node.children, code)
    if (found)
      return found
  }
  return null
}

async function openDesigner(row) {
  const { data } = await api.versionDetailByVersion(row.workflowId, row.latestVersion)
  const { href } = router.resolve({
    path: '/template/process/design',
    query: {
      id: data.versionId,
      workflowId: row.workflowId,
      workflowKey: row.workflowKey,
      workflowName: row.workflowName,
      version: data.version,
    },
  })
  window.open(href, '_blank')
}

function openDetail(row) {
  router.push({ path: '/template/process/detail', query: { id: row.workflowId } })
}

const required = {
  required: true,
  message: '此为必填项',
  trigger: ['blur', 'change'],
}

const {
  modalRef,
  modalFormRef,
  modalAction,
  modalForm,
  handleAdd,
  handleDelete,
  handleEdit,
} = useCrud({
  name: '流程',
  initForm: { workflowName: '', workflowKey: '', category: '', description: '', remark: '' },
  doCreate: api.create,
  doUpdate: row => api.update(row.workflowId, row),
  doDelete: api.delete,
  refresh: () => $table.value?.handleSearch(),
})

function generateKey() {
  modalForm.value.workflowKey = randomKey('Process')
  modalFormRef.value?.restoreValidation()
}

function handleCategorySelect(key) {
  if (key == null) {
    modalForm.value.category = ''
    return
  }
  const node = findNode(categoryTree.value, key)
  modalForm.value.category = node?.code || node?.name || ''
}

// 编辑时按已存的分类编码回显树节点
watch(modalAction, (action) => {
  if (!action)
    return
  categoryKey.value = findNodeKey(categoryTree.value, modalForm.value.category)
})

const columns = [
  {
    title: '流程名称',
    key: 'workflowName',
    width: 200,
    ellipsis: { tooltip: true },
    render: row => h(NButton, {
      text: true,
      type: 'primary',
      onClick: () => openDetail(row),
    }, { default: () => row.workflowName }),
  },
  {
    title: '流程Key',
    key: 'workflowKey',
    width: 220,
    ellipsis: { tooltip: true },
  },
  {
    title: '版本',
    key: 'latestVersion',
    width: 80,
    render: ({ latestVersion }) => h(NTag, {
      size: 'small',
      type: 'primary',
      bordered: false,
    }, { default: () => `v${latestVersion}` }),
  },
  { title: '描述', key: 'description', minWidth: 200, ellipsis: { tooltip: true } },
  {
    title: '更新时间',
    key: 'lastModifiedDate',
    width: 180,
  },
  {
    title: '操作',
    key: 'actions',
    width: 300,
    align: 'right',
    fixed: 'right',
    render(row) {
      return [
        h(
          NButton,
          {
            size: 'small',
            type: 'primary',
            onClick: () => openDesigner(row),
          },
          {
            default: () => '设计',
            icon: () => h('i', { class: 'i-carbon:draw text-14' }),
          },
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'info',
            style: 'margin-left: 12px;',
            onClick: () => handleEdit(row),
          },
          {
            default: () => '编辑',
            icon: () => h('i', { class: 'i-material-symbols:edit-outline text-14' }),
          },
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'error',
            style: 'margin-left: 12px;',
            onClick: () => handleDelete(row.workflowId),
          },
          {
            default: () => '删除',
            icon: () => h('i', { class: 'i-material-symbols:delete-outline text-14' }),
          },
        ),
      ]
    },
  },
]
</script>
