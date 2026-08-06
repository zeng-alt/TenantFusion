<template>
  <CommonPage>
    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.initiated"
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
      <MeQueryItem label="流程状态" :label-width="65">
        <NSelect
          v-model:value="queryItems.status"
          clearable
          :options="statusOptions"
          placeholder="请选择流程状态"
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
import { PROCESS_STATUS_MAP, renderStatusTag } from '../renderers'

defineOptions({ name: 'MyFlowInitiated' })

const router = useRouter()
const $table = ref(null)
const queryItems = ref({})

const statusOptions = Object.entries(PROCESS_STATUS_MAP).map(([value, cfg]) => ({
  label: cfg.text,
  value,
}))

onMounted(() => {
  $table.value?.handleSearch()
})

function handleDetail(processInstanceId) {
  router.push({ path: `/my-flow/detail/${processInstanceId}` })
}

function handleCancel(processInstanceId) {
  const dialog = $dialog.warning({
    title: '撤回流程',
    content: '确认撤回该流程实例？撤回后流程将被终止，不可恢复。',
    positiveText: '确认撤回',
    negativeText: '再想想',
    async onPositiveClick() {
      try {
        dialog.loading = true
        await api.cancel(processInstanceId)
        $message.success('撤回成功')
        dialog.loading = false
        $table.value?.handleSearch()
      }
      catch (error) {
        console.error(error)
        $message.error(error?.message || '撤回失败')
        dialog.loading = false
      }
    },
  })
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
    key: 'startUserName',
    width: 100,
  },
  {
    title: '流程状态',
    key: 'status',
    width: 120,
    render: ({ status }) => renderStatusTag(status),
  },
  {
    title: '当前节点',
    key: 'currentTaskName',
    width: 140,
  },
  {
    title: '当前处理人',
    key: 'currentAssignee',
    width: 120,
  },
  {
    title: '发起时间',
    key: 'createTime',
    width: 160,
  },
  {
    title: '结束时间',
    key: 'endTime',
    width: 160,
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    align: 'center',
    fixed: 'right',
    render({ processInstanceId, status }) {
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
        status === 'running'
          ? h(
              NButton,
              {
                size: 'small',
                type: 'warning',
                style: 'margin-left: 8px;',
                onClick: () => handleCancel(processInstanceId),
              },
              {
                default: () => '撤回',
                icon: () => h('i', { class: 'i-carbon:close text-14' }),
              },
            )
          : null,
      ]
    },
  },
]
</script>
