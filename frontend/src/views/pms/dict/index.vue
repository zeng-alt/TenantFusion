<template>
  <CommonPage>
    <template #action>
      <NButton v-permission="'POST:/v1/dict/type'" type="primary" @click="handleAddType">
        <i class="i-material-symbols:add mr-4 text-14" />
        新增字典
      </NButton>
    </template>

    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.read"
      row-key="dictTypeId"
      :scroll-x="800"
    >
      <MeQueryItem label="字典编码" :label-width="70">
        <NInput
          v-model:value="queryItems.dictCode"
          type="text"
          placeholder="请输入字典编码"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="字典名称" :label-width="70">
        <NInput
          v-model:value="queryItems.dictName"
          type="text"
          placeholder="请输入字典名称"
          clearable
        />
      </MeQueryItem>
    </MeCrud>

    <n-drawer v-model:show="showDrawer" width="65%" placement="right">
      <n-drawer-content>
        <div v-if="currentDictType" class="h-full flex flex-col">
          <DictData :dict-type="currentDictType" />
        </div>
      </n-drawer-content>
    </n-drawer>

    <MeModal ref="typeModalRef" width="520px">
      <NForm
        ref="typeFormRef"
        label-placement="left"
        require-mark-placement="left"
        :label-width="100"
        :model="typeForm"
        :disabled="typeModalAction === 'view'"
      >
        <n-form-item path="dictName" :rule="required">
          <template #label>
            字典名称
          </template>
          <NInput v-model:value="typeForm.dictName" />
        </n-form-item>
        <n-form-item path="dictCode" :rule="required">
          <template #label>
            字典编码
          </template>
          <NInput v-model:value="typeForm.dictCode" :disabled="!isAdmin() && typeForm.isDefault" />
        </n-form-item>
        <n-form-item path="remark" label="备注">
          <NInput v-model:value="typeForm.remark" type="textarea" />
        </n-form-item>
      </NForm>
    </MeModal>
  </CommonPage>
</template>

<script setup>
import { NButton, NForm, NInput } from 'naive-ui'
import { renderProCopyableText } from 'pro-naive-ui'
import { h, onMounted, ref } from 'vue'
import { CommonPage, EnableSwitch, MeCrud, MeModal, MeQueryItem } from '@/components'
import { useCrud } from '@/composables'
import { hasMenu, isAdmin } from '@/utils'
import api from './api'
import DictData from './dict-data.vue'

defineOptions({ name: 'DictMgt' })

const $table = ref(null)
const showDrawer = ref(false)
const currentDictType = ref(null)

onMounted(() => {
  $table.value?.handleSearch()
})

const queryItems = ref({
  dictCode: '',
  dictName: '',
})

const required = {
  required: true,
  message: '此为必填项',
  trigger: ['blur', 'change'],
}

const {
  modalRef: typeModalRef,
  modalFormRef: typeFormRef,
  modalForm: typeForm,
  modalAction: typeModalAction,
  handleAdd: handleAddType,
  handleDelete: handleDeleteType,
  handleEdit: handleEditType,
  handleEnable,
} = useCrud({
  name: '字典类型',
  initForm: { dictName: '', dictCode: '', isDefault: false, remark: '' },
  doCreate: api.create,
  doUpdate: api.update,
  doDelete: api.delete,
  refresh: () => $table.value?.handleSearch(),
})

const columns = [
  {
    title: '字典编码',
    key: 'dictCode',
    render(row) {
      if (!hasMenu('DictDataMgt'))
        return renderProCopyableText(row.dictCode)
      return h(
        NButton,
        {
          text: true,
          type: 'primary',
          onClick: () => {
            currentDictType.value = row
            showDrawer.value = true
          },
        },
        { default: () => renderProCopyableText(row.dictCode) },
      )
    },
  },
  { title: '字典名称', key: 'dictName' },
  {
    title: '默认',
    key: 'isDefault',
    render: row => h(EnableSwitch, {
      value: row.isDefault,
      loading: !!row.enabledLoading,
      disabled: !isAdmin(),
      onUpdateValue: () => handleEnable(row, 'dictTypeId', 'isDefault'),
    }),
  },
  { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    align: 'right',
    fixed: 'right',
    render(row) {
      return [
        h(
          NButton,
          {
            type: 'success',
            size: 'small',
            onClick: () => handleEditType(row),
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
            disabled: !isAdmin() && row.isDefault,
            onClick: () => handleDeleteType(row.dictTypeId),
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
