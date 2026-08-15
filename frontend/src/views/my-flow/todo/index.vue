<template>
  <CommonPage>
    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :columns="columns"
      :get-data="api.todo"
      row-key="id"
      expand
      :scroll-x="1600"
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

    <TaskFormModal
      v-model:visible="modalVisible"
      :task-id="selectedTask?.id"
      :process-instance-id="selectedTask?.processInstanceId"
      @success="handleSuccess"
    />
  </CommonPage>
</template>

<script setup>
import { NButton, NInput } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CommonPage, MeCrud, MeQueryItem, UserPopover } from '@/components'
import { useUserStore } from '@/store'
import { formatDateTime } from '@/utils'
import { isAdmin, isSuperAdmin } from '@/utils/auth'
import api from '../api'
import TaskFormModal from '../components/TaskFormModal.vue'

defineOptions({ name: 'MyFlowTodo' })

const router = useRouter()
const $table = ref(null)
const queryItems = ref({})
const modalVisible = ref(false)
const selectedTask = ref(null)

onMounted(() => {
  $table.value?.handleSearch()
})

function handleProcess(row) {
  selectedTask.value = row
  modalVisible.value = true
}

function handleDetail(processInstanceId) {
  router.push({ path: `/my-flow/detail/${processInstanceId}` })
}

function handleSuccess() {
  $table.value?.handleSearch()
}

async function handleClaim(row) {
  const userStore = useUserStore()
  const userId = userStore?.username || ''
  await api.claim(row.id, userId)
  $message?.success('认领成功')
  $table.value?.handleSearch()
}

async function handleUnclaim(row) {
  await api.unclaim(row.id)
  $message?.success('已取消认领')
  $table.value?.handleSearch()
}

function canUnclaim(assignee) {
  const userStore = useUserStore()
  return !!assignee && (userStore?.username === assignee || isAdmin() || isSuperAdmin())
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
    width: 120,
    render: row => row.initiator
      ? h(UserPopover, { username: row.initiator }, {
          default: () => h('span', { class: 'cursor-pointer text-13 text-primary' }, row.initiator),
        })
      : '—',
  },
  {
    title: '当前节点',
    key: 'name',
    width: 140,
  },
  {
    title: '审核人',
    key: 'assignee',
    width: 160,
    align: 'center',
    render(row) {
      if (row.assignee) {
        const children = [h('span', null, row.assignee)]
        if (canUnclaim(row.assignee)) {
          children.push(
            h(
              NButton,
              {
                size: 'small',
                text: true,
                type: 'warning',
                onClick: () => handleUnclaim(row),
              },
              {
                default: () => '取消认领',
                icon: () => h('i', { class: 'i-carbon:close text-14' }),
              },
            ),
          )
        }
        return h('div', { class: 'flex items-center justify-center gap-8' }, children)
      }
      return h(
        NButton,
        {
          size: 'small',
          text: true,
          type: 'primary',
          onClick: () => handleClaim(row),
        },
        {
          default: () => '认领',
          icon: () => h('i', { class: 'i-carbon:checkmark-outline text-14' }),
        },
      )
    },
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160,
    render: row => (row.createTime ? formatDateTime(row.createTime) : '—'),
  },
  {
    title: '截止时间',
    key: 'dueDate',
    width: 160,
    render: row => (row.dueDate ? formatDateTime(row.dueDate) : '—'),
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    align: 'center',
    fixed: 'right',
    render(row) {
      const { processInstanceId } = row
      return [
        h(
          NButton,
          {
            size: 'small',
            type: 'primary',
            onClick: () => handleProcess(row),
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
