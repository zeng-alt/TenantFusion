<template>
  <CommonPage>
    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.todo"
      row-key="id"
      :scroll-x="1200"
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
    </MeCrud>
  </CommonPage>
</template>

<script setup>
import { NButton, NInput } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CommonPage, MeCrud, MeQueryItem } from '@/components'
import api from '../api'
import { renderDueTimeTag } from '../renderers'

defineOptions({ name: 'MyFlowTodo' })

const router = useRouter()
const $table = ref(null)
const queryItems = ref({})

onMounted(() => {
  $table.value?.handleSearch()
})

function handleProcess(taskId) {
  router.push({ path: `/my-flow/process/${taskId}` })
}

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
    title: '当前节点',
    key: 'taskName',
    width: 140,
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160,
  },
  {
    title: '截止时间',
    key: 'dueTime',
    width: 160,
    render: ({ dueTime }) => renderDueTimeTag(dueTime),
  },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    align: 'center',
    fixed: 'right',
    render({ id, processInstanceId }) {
      return [
        h(
          NButton,
          {
            size: 'small',
            type: 'primary',
            onClick: () => handleProcess(id),
          },
          {
            default: () => '办理',
            icon: () => h('i', { class: 'i-carbon:task text-14' }),
          },
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'info',
            style: 'margin-left: 8px;',
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
