<template>
  <CommonPage>
    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.done"
      row-key="id"
      :scroll-x="1400"
    >
      <MeQueryItem label="流程名称" :label-width="70">
        <NInput
          v-model:value="queryItems.name"
          type="text"
          placeholder="请输入流程名称"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="业务Key" :label-width="65">
        <NInput
          v-model:value="queryItems.businessKey"
          type="text"
          placeholder="请输入业务Key"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="发起人" :label-width="50">
        <NInput
          v-model:value="queryItems.initiator"
          type="text"
          placeholder="请输入发起人"
          clearable
        />
      </MeQueryItem>
      <MeQueryItem label="处理结果" :label-width="65">
        <NSelect
          v-model:value="queryItems.action"
          clearable
          :options="actionOptions"
          placeholder="请选择处理结果"
        />
      </MeQueryItem>
    </MeCrud>
  </CommonPage>
</template>

<script setup>
import { NButton, NInput, NSelect } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CommonPage, MeCrud, MeQueryItem } from '@/components'
import api from '../api'
import { ACTION_MAP, renderActionTag } from '../renderers'

defineOptions({ name: 'MyFlowDone' })

const router = useRouter()
const $table = ref(null)
const queryItems = ref({})

const actionOptions = Object.entries(ACTION_MAP).map(([value, cfg]) => ({
  label: cfg.text,
  value,
}))

onMounted(() => {
  $table.value?.handleSearch()
})

function handleDetail(processInstanceId) {
  router.push({ path: `/my-flow/detail/${processInstanceId}` })
}

const columns = [
  {
    title: '流程名称',
    key: 'processDefinitionName',
    width: 180,
    ellipsis: { tooltip: true },
  },
  {
    title: '流程定义Key',
    key: 'processDefinitionKey',
    width: 180,
    ellipsis: { tooltip: true },
  },
  {
    title: '业务Key',
    key: 'businessKey',
    width: 160,
    ellipsis: { tooltip: true },
  },
  {
    title: '发起人',
    key: 'initiator',
    width: 100,
  },
  {
    title: '处理节点',
    key: 'taskName',
    width: 140,
  },
  {
    title: '处理结果',
    key: 'action',
    width: 100,
    render: ({ action }) => renderActionTag(action),
  },
  {
    title: '处理意见',
    key: 'comment',
    minWidth: 140,
    ellipsis: { tooltip: true },
  },
  {
    title: '完成时间',
    key: 'endTime',
    width: 200,
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    align: 'center',
    fixed: 'right',
    render({ processInstanceId }) {
      return [
        h(
          NButton,
          {
            size: 'small',
            type: 'info',
            onClick: () => handleDetail(processInstanceId),
          },
          {
            default: () => '详情',
            icon: () => h('i', { class: 'i-carbon:overflow-menu-vertical text-14' }),
          },
        ),
      ]
    },
  },
]
</script>
