<template>
  <CommonPage>
    <template #action>
      <NButton
        type="primary"
        @click="handleAdd()"
      >
        <i class="i-material-symbols:add mr-4 text-14" />
        新建配置表单
      </NButton>
    </template>

    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.read"
      row-key="formConfigId"
      :scroll-x="1200"
    >
      <MeQueryItem label="表单名称" :label-width="70">
        <NInput
          v-model:value="queryItems.name"
          type="text"
          placeholder="请输入表单名称"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="表单编码" :label-width="70">
        <NInput
          v-model:value="queryItems.code"
          type="text"
          placeholder="请输入表单编码"
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
        :disabled="modalAction === 'view'"
      >
        <n-form-item path="name" :rule="required">
          <template #label>
            表单名称
          </template>
          <NInput v-model:value="modalForm.name" :disabled="modalAction === 'edit'" />
        </n-form-item>
        <n-form-item path="code" :rule="required">
          <template #label>
            表单编码
          </template>
          <NInputGroup>
            <NInput v-model:value="modalForm.code" :disabled="modalAction === 'edit'" />
            <NButton type="primary" ghost :disabled="modalAction === 'edit'" @click="generateCode">
              <template #icon>
                <i class="i-carbon:renew text-14" />
              </template>
              生成
            </NButton>
          </NInputGroup>
        </n-form-item>
        <n-form-item path="category" label="分类">
          <NInput v-model:value="modalForm.category" placeholder="如：人事 / 财务" />
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
import { NButton, NInput, NInputGroup, NTag } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CommonPage, MeCrud, MeModal, MeQueryItem } from '@/components'
import { useCrud } from '@/composables'
import { formatDateTime, randomKey } from '@/utils'
import api from './api'

defineOptions({ name: 'FormConfigMgt' })

const router = useRouter()
const $table = ref(null)
const queryItems = ref({})

onMounted(() => {
  $table.value?.handleSearch()
})

const required = {
  required: true,
  message: '此为必填项',
  trigger: ['blur', 'change'],
}

const {
  modalRef,
  modalFormRef,
  modalForm,
  modalAction,
  handleAdd,
  handleDelete,
  handleEdit,
} = useCrud({
  name: '配置表单',
  initForm: { name: '', code: '', category: '', description: '', remark: '' },
  doCreate: api.create,
  doUpdate: api.update,
  doDelete: api.delete,
  refresh: () => $table.value?.handleSearch(),
})

function generateCode() {
  modalForm.value.code = randomKey('fc')
  modalFormRef.value?.restoreValidation()
}

async function openDesigner(row) {
  try {
    // 找到最新版本（优先草稿，其次当前发布版本）
    const { data: versions } = await api.versions(row.formConfigId)
    const target = versions?.[0]
    if (!target) {
      $message.error('该表单暂无版本，请先创建')
      return
    }
    const { href } = router.resolve({
      path: '/template/form-config/design',
      query: {
        id: target.versionId,
        formConfigId: row.formConfigId,
        code: row.code,
        name: row.name,
      },
    })
    window.open(href, '_blank')
  }
  catch (error) {
    console.error(error)
    $message.error('打开设计器失败')
  }
}

const columns = [
  {
    title: '表单名称',
    key: 'name',
    width: 200,
    ellipsis: { tooltip: true },
    render: row => h(
      NButton,
      {
        text: true,
        type: 'primary',
        onClick: () => router.push({
          path: '/template/form-config/detail',
          query: { id: row.formConfigId },
        }),
      },
      { default: () => row.name },
    ),
  },
  {
    title: '表单编码',
    key: 'code',
    width: 180,
    ellipsis: { tooltip: true },
  },
  { title: '分类', key: 'category', width: 100 },
  {
    title: '版本',
    key: 'latestVersion',
    width: 80,
    render: ({ latestVersion }) => h(NTag, {
      size: 'small',
      type: 'primary',
      bordered: false,
    }, { default: () => `v${latestVersion ?? 1}` }),
  },
  { title: '描述', key: 'description', minWidth: 200, ellipsis: { tooltip: true } },
  {
    title: '更新时间',
    key: 'lastModifiedDate',
    width: 180,
    render: row => h('span', formatDateTime(row.lastModifiedDate)),
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
            onClick: () => handleDelete(row.formConfigId),
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
