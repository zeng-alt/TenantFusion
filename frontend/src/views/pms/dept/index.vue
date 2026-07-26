<template>
  <CommonPage>
    <template #action>
      <NButton v-permission="'POST:/v1/dept'" type="primary" @click="handleAdd()">
        <i class="i-material-symbols:add mr-4 text-14" />
        新增部门
      </NButton>
    </template>

    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="getDeptTreeData"
      :is-pagination="false"
      row-key="deptId"
      :scroll-x="900"
    >
      <MeQueryItem label="部门名称" :label-width="70">
        <NInput
          v-model:value="queryItems.deptName"
          type="text"
          placeholder="请输入部门名称"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="状态" :label-width="50">
        <NSelect
          v-model:value="queryItems.enabled"
          clearable
          :options="[
            { label: '启用', value: true },
            { label: '停用', value: false },
          ]"
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
        <n-form-item path="deptName" :rule="required">
          <template #label>
            部门名称
          </template>
          <NInput v-model:value="modalForm.deptName" />
        </n-form-item>
        <n-form-item path="parentId" label="上级部门">
          <NTreeSelect
            v-model:value="modalForm.parentId"
            :options="deptTreeOptions"
            key-field="deptId"
            label-field="deptName"
            placeholder="根部门"
            clearable
            filterable
          />
        </n-form-item>
        <n-form-item path="deptSort" label="排序">
          <NInputNumber v-model:value="modalForm.deptSort" :min="0" />
        </n-form-item>
        <n-form-item path="enabled" label="状态">
          <NSwitch v-model:value="modalForm.enabled">
            <template #checked>
              启用
            </template>
            <template #unchecked>
              停用
            </template>
          </NSwitch>
        </n-form-item>
        <n-form-item path="remark" label="备注">
          <NInput v-model:value="modalForm.remark" type="textarea" />
        </n-form-item>
      </NForm>
    </MeModal>
  </CommonPage>
</template>

<script setup>
import { NButton, NInput, NInputNumber, NSelect, NSwitch, NTreeSelect } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { CommonPage, EnableSwitch, MeCrud, MeModal, MeQueryItem } from '@/components'
import { useCrud } from '@/composables'
import api from './api'

defineOptions({ name: 'DeptMgt' })

const $table = ref(null)

onMounted(() => {
  $table.value?.handleSearch()
})

const queryItems = ref({})

const required = {
  required: true,
  message: '此为必填项',
  trigger: ['blur', 'change'],
}

const deptTreeOptions = ref([])

async function getDeptTreeData() {
  const res = await api.tree()
  deptTreeOptions.value = res?.data || []
  return res
}

const {
  modalRef,
  modalFormRef,
  modalForm,
  modalAction,
  handleAdd,
  handleDelete,
  handleEdit,
  handleEnable,
} = useCrud({
  name: '部门',
  initForm: { deptName: '', parentId: null, deptSort: 0, enabled: true, remark: '' },
  doCreate: api.create,
  doUpdate: api.update,
  doDelete: api.delete,
  refresh: () => $table.value?.handleSearch(),
})

function handleAddChild(row) {
  handleAdd()
  modalForm.value.parentId = row.deptId
}

const columns = [
  { title: '部门名称', key: 'deptName', minWidth: 200 },
  { title: '排序', key: 'deptSort', width: 80 },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    render: row => h(EnableSwitch, {
      value: row.enabled,
      loading: !!row.enabledLoading,
      onUpdateValue: () => handleEnable(row, 'deptId'),
    }),
  },
  { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 290,
    align: 'right',
    fixed: 'right',
    render(row) {
      return [
        h(
          NButton,
          {
            type: 'primary',
            size: 'small',
            onClick: () => handleAddChild(row),
          },
          {
            default: () => '新增',
            icon: () => h('i', { class: 'i-material-symbols:add text-14' }),
          },
        ),
        h(
          NButton,
          {
            type: 'success',
            size: 'small',
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
            type: 'error',
            size: 'small',
            style: 'margin-left: 12px;',
            onClick: () => handleDelete(row.deptId),
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
