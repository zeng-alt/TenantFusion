<template>
  <CommonPage :show-header="showHeader">
    <template v-if="showHeader" #action>
      <NButton v-permission="'AddUser'" type="primary" @click="handleAdd()">
        <i class="i-material-symbols:add mr-4 text-18" />
        创建HTTP资源
      </NButton>
    </template>

    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      row-key="permissionId"
      :scroll-x="1200"
      :columns="columns"
      :get-data="httpApi.page"
      expand
      @update:checked-row-keys="onChecked"
    >
      <MeQueryItem label="所属菜单">
        <n-tree-select
          v-model:value="queryItems.menuId"
          :options="treeData"
          label-field="name"
          key-field="id"
          placeholder="根菜单"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="协议" :label-width="50">
        <n-select
          v-model:value="queryItems.method"
          clearable
          :options="option"
        />
      </MeQueryItem>
      <MeQueryItem label="编码" :label-width="50">
        <NInput
          v-model:value="queryItems.code"
          type="text"
          placeholder="请输入编码"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="名称" :label-width="50">
        <NInput
          v-model:value="queryItems.name"
          type="text"
          placeholder="请输入名称"
          clearable
        />
      </MeQueryItem>
    </MeCrud>

    <MeModal ref="modalRef" width="800px">
      <n-form
        ref="modalFormRef"
        label-placement="left"
        require-mark-placement="left"
        :label-width="100"
        :model="modalForm"
      >
        <n-grid :cols="24" :x-gap="24">
          <n-form-item-gi :span="12" label="所属菜单" path="menuId">
            <n-tree-select
              v-model:value="modalForm.menuId"
              :options="treeData"
              label-field="name"
              key-field="id"
              placeholder="根菜单"
              clearable
            />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="name" :rule="required">
            <template #label>
              <QuestionLabel label="名称" content="标题" />
            </template>
            <NInput v-model:value="modalForm.name" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="code" :rule="required">
            <template #label>
              <QuestionLabel label="编码" content="如果是菜单则对应前端路由的name，使用大驼峰" />
            </template>
            <NInput v-model:value="modalForm.code" :disabled="modalAction === 'edit'" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="enable">
            <template #label>
              <QuestionLabel label="状态" content="如果是菜单，禁用后将不添加到路由表，无法进入此页面" />
            </template>
            <NSwitch v-model:value="modalForm.enabled">
              <template #checked>
                启用
              </template>
              <template #unchecked>
                禁用
              </template>
            </NSwitch>
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="method" :rule="required">
            <template #label>
              <QuestionLabel label="协议" />
            </template>
            <n-select
              v-model:value="modalForm.method"
              size="small"
              clearable
              :options="option"
            />
          </n-form-item-gi>
          <n-form-item-gi :span="24" path="path">
            <template #label>
              <QuestionLabel label="接口路径" content="后端接口URL路径" />
            </template>
            <NInput v-model:value="modalForm.path" />
          </n-form-item-gi>
        </n-grid>
      </n-form>
    </MeModal>
  </CommonPage>
</template>

<script setup>
import { NButton, NInput, NSwitch, NTag, NTooltip } from 'naive-ui'
import { MeCrud, MeModal, MeQueryItem } from '@/components'
import { useCrud } from '@/composables'
import api from '@/views/pms/resource/menu/api'
import QuestionLabel from '@/views/pms/resource/menu/components/QuestionLabel.vue'
import httpApi from './api.js'

defineOptions({ name: 'HttpResourceMgt' })

defineProps({
  showHeader: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['checked'])

function onChecked(rowKeys) {
  emit('checked', rowKeys || [])
}

const $table = ref(null)
const queryItems = ref({
  code: '',
  name: '',
  method: null,
  menuId: null,
})

const option = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'PATCH', value: 'PATCH' },
  { label: 'HEAD', value: 'HEAD' },
  { label: 'OPTIONS', value: 'OPTIONS' },
  { label: 'TRACE', value: 'TRACE' },
]

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
  name: '参数',
  initForm: { enabled: true },
  doCreate: httpApi.create,
  doDelete: httpApi.delete,
  doUpdate: httpApi.update,
  refresh: () => $table.value?.handleSearch(),
})

const treeData = ref([])
async function initData() {
  const res = await api.getMenuTree()
  treeData.value = res?.data || []
}

initData()

async function handleEnable(item) {
  try {
    await api.updateHttp(item.permissionId, { enabled: !item.enabled })
    $message.success('操作成功')
    $table.value?.handleSearch()
  }
  catch (error) {
    console.error(error)
  }
}

const columns = [
  { type: 'selection', fixed: 'left' },
  {
    title: '编码',
    key: 'code',
    width: 150,
    ellipsis: { tooltip: true },
  },
  {
    title: '名称',
    key: 'name',
    width: 150,
    ellipsis: { tooltip: true },
  },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    render: row =>
      h(
        NSwitch,
        {
          size: 'small',
          rubberBand: false,
          value: row.enabled,
          loading: !!row.enableLoading,
          onUpdateValue: () => handleEnable(row),
        },
        {
          checked: () => '启用',
          unchecked: () => '停用',
        },
      ),
  },
  {
    title: '协议',
    key: 'method',
    width: 100,
    render: row => h(NTag, { type: 'success' }, { default: () => row.method }),
  },
  {
    title: '路径',
    key: 'path',
    ellipsis: { tooltip: true },
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    align: 'right',
    fixed: 'right',
    hideInExcel: true,
    render(row) {
      return [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            text: true,
            size: 'large',
            style: 'margin-left: 12px;',
            type: 'info',
            onClick: () => handleEdit(row),
          }, { icon: () => h('i', { class: 'i-material-symbols:edit-outline text-14' }) }),
          default: () => '修改',
        }),
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            text: true,
            size: 'large',
            style: 'margin-left: 12px;',
            type: 'error',
            onClick: () => handleDelete(row.permissionId),
          }, { icon: () => h('i', { class: 'i-material-symbols:delete-outline text-14' }) }),
          default: () => '删除',
        }),
      ]
    },
  },
]

onMounted(() => {
  $table.value?.handleSearch()
})
</script>
