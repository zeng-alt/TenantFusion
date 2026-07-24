<template>
  <div class="h-full flex flex-col">
    <div class="m-8 w-full flex items-center justify-between">
      <span class="text-16 font-600">
        字典数据 - {{ dictType?.dictName || '' }}
      </span>
      <div>
        <NButton type="primary" class="ml-20" @click="handleAddData">
          <i class="i-material-symbols:add mr-4 text-14" />
          新增数据
        </NButton>
      </div>
    </div>

    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.readData"
      row-key="dictDataId"
      :scroll-x="800"
      :batch-sort="handleSort"
    >
      <MeQueryItem label="字典标签" :label-width="70">
        <NInput
          v-model:value="queryItems.dictLabel"
          type="text"
          placeholder="请输入字典标签"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="字典值" :label-width="60">
        <NInput
          v-model:value="queryItems.dictValue"
          type="text"
          placeholder="请输入字典值"
          clearable
        />
      </MeQueryItem>
    </MeCrud>

    <MeModal ref="modalRef" width="520px">
      <NForm
        ref="modalFormRef"
        label-placement="left"
        label-align="left"
        :label-width="80"
        :model="modalForm"
        :disabled="modalAction === 'view'"
      >
        <NFormItem
          label="字典标签"
          path="dictLabel"
          :rule="{
            required: true,
            message: '请输入字典标签',
            trigger: ['input', 'blur'],
          }"
        >
          <NInput v-model:value="modalForm.dictLabel" :disabled="modalAction === 'view'" />
        </NFormItem>
        <NFormItem
          label="字典键值"
          path="dictValue"
          :rule="{
            required: true,
            message: '请输入字典键值',
            trigger: ['input', 'blur'],
          }"
        >
          <NInput v-model:value="modalForm.dictValue" :disabled="!isAdmin() && modalForm.isDefault" />
        </NFormItem>
        <NFormItem
          label="样式属性"
          path="cssClass"
          :rule="{
            message: '请输入样式属性',
            trigger: ['input', 'blur'],
          }"
        >
          <NInput v-model:value="modalForm.cssClass" />
        </NFormItem>
        <NGrid :cols="24" :x-gap="24">
          <NFormItemGi
            :span="12" label="回显样式" path="listClass"
            :rule="{
              message: '请输入回显样式',
              trigger: ['input', 'blur'],
            }"
          >
            <NSelect v-model:value="modalForm.listClass" :options="tagOptions" />
          </NFormItemGi>
          <NFormItemGi :span="12">
            <NTag :show="modalForm.dictLabel" :type="modalForm.listClass" :bordered="false">
              {{ modalForm.dictLabel }}
            </NTag>
          </NFormItemGi>
        </NGrid>
        <NGrid :cols="24" :x-gap="24">
          <NFormItemGi :span="12" label="系统默认:" path="isDefault">
            <NSwitch v-model:value="modalForm.isDefault" :disabled="!isAdmin()">
              <template #checked>
                是
              </template>
              <template #unchecked>
                否
              </template>
            </NSwitch>
          </NFormItemGi>
          <NFormItemGi :span="12" label="状态:" path="enabled">
            <EnableSwitch v-model:value="modalForm.enabled" />
          </NFormItemGi>
        </NGrid>
        <NFormItem
          label="备注"
          :rule="{
            message: '请输入备注',
            trigger: ['input', 'blur'],
          }"
        >
          <NInput v-model:value="modalForm.remark" type="textarea" />
        </NFormItem>
      </NForm>
    </MeModal>
  </div>
</template>

<script setup>
import { NButton, NForm, NFormItem, NFormItemGi, NGrid, NInput, NSelect, NTag } from 'naive-ui'
import { h, nextTick, ref, watch } from 'vue'
import { EnableSwitch, MeCrud, MeModal, MeQueryItem } from '@/components'
import { useCrud } from '@/composables'
import { isAdmin } from '@/utils'
import api from './api'

defineOptions({ name: 'DictDataMgt' })

const props = defineProps({
  dictType: {
    type: Object,
    required: true,
  },
})

const $table = ref(null)
const queryItems = ref({
  dictCode: props.dictType?.dictCode || '',
  dictLabel: '',
  dictValue: '',
})

const tagOptions = [
  { label: '默认', value: 'default' },
  { label: '次要', value: 'tertiary' },
  { label: '主要', value: 'primary' },
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
]

const initForm = {
  dictCode: props.dictType?.dictCode || '',
  listClass: 'tertiary',
  isDefault: false,
  enabled: true,
  cssClass: '',
  remark: '',
  dictLabel: '',
  dictValue: '',
}

const {
  modalRef,
  modalFormRef,
  modalForm,
  modalAction,
  handleAdd: handleAddRaw,
  handleDelete,
  handleEdit,
  handleEnable,
  handleSort,
} = useCrud({
  name: '字典数据',
  initForm,
  doCreate: api.createData,
  doDelete: api.deleteData,
  doUpdate: api.updateData,
  doSort: api.sortData,
  enableDraft: false,
  refresh: () => $table.value?.handleSearch(),
})

function handleAddData() {
  if (!props.dictType)
    return
  initForm.dictCode = props.dictType?.dictCode
  handleAddRaw()
  modalForm.value.dictCode = props.dictType?.dictCode
}

const columns = [
  { path: 'dragSort', key: 'dictSort' },
  { title: '字典标签', key: 'dictLabel' },
  { title: '字典值', key: 'dictValue' },
  { title: '排序', key: 'dictSort' },
  {
    title: '状态',
    key: 'enabled',
    render: row => h(EnableSwitch, {
      value: row.enabled,
      loading: !!row.enabledLoading,
      onUpdateValue: () => handleEnable(row, 'dictDataId', 'enabled'),
    }),
  },
  {
    title: '默认',
    key: 'isDefault',
    render: row => h(EnableSwitch, {
      value: row.isDefault,
      loading: !!row.enabledLoading,
      disabled: !isAdmin(),
      onUpdateValue: () => handleEnable(row, 'dictDataId', 'isDefault'),
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
            onClick: () => handleEdit(row),
            style: 'margin-right: 12px',
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
            onClick: () => handleDelete(row.dictDataId),
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

watch(() => props.dictType, (newVal) => {
  if (newVal) {
    queryItems.value.dictCode = newVal.dictCode
    nextTick(() => {
      $table.value?.handleSearch()
    })
  }
}, { immediate: true })
</script>
