<template>
  <CommonPage>
    <template #action>
      <NButton type="primary" @click="handleAddType">
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
          <DictDataList :dict-type="currentDictType" />
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
          <NInput v-model:value="typeForm.dictCode" />
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
import { h, onMounted, ref } from 'vue'
import { CommonPage, MeCrud, MeModal, MeQueryItem } from '@/components'
import { useCrud } from '@/composables'
import api from './api'
import DictDataList from './DictDataList.vue'

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
} = useCrud({
  name: '字典类型',
  initForm: { dictName: '', dictCode: '', remark: '' },
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
        { default: () => row.dictCode },
      )
    },
  },
  { title: '字典名称', key: 'dictName' },
  { title: '备注', key: 'remark' },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render(row) {
      return [
        h(
          NButton,
          {
            type: 'success',
            size: 'small',
            onClick: () => handleEditType(row),
            style: 'margin-right: 12px',
          },
          { default: () => '编辑' },
        ),
        h(
          NButton,
          {
            type: 'error',
            size: 'small',
            onClick: () => handleDeleteType(row.dictTypeId),
          },
          { default: () => '删除' },
        ),
      ]
    },
  },
]
</script>
