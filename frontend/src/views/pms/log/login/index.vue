<template>
  <CommonPage>
    <MeCrud
      ref="$table"
      v-model:query-items="queryItems"
      :scroll-x="1200"
      :columns="columns"
      :get-data="api.read"
    >
      <MeQueryItem label="用户名" :label-width="50">
        <n-input
          v-model:value="queryItems.username"
          type="text"
          placeholder="请输入用户名"
          clearable
        />
      </MeQueryItem>

      <MeQueryItem label="IP地址" :label-width="50">
        <n-input
          v-model:value="queryItems.ip"
          type="text"
          placeholder="请输入IP地址"
          clearable
        />
      </MeQueryItem>

      <MeQueryItem label="状态" :label-width="50">
        <n-select
          v-model:value="queryItems.status"
          clearable
          :options="statusOptions"
        />
      </MeQueryItem>
    </MeCrud>

    <MeModal ref="modalRef" width="680px" :show-footer="false">
      <n-descriptions :column="2" label-placement="left" bordered>
        <n-descriptions-item label="用户名">
          {{ detail.username }}
        </n-descriptions-item>
        <n-descriptions-item label="IP地址">
          {{ detail.ip || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="登录状态">
          <NTag :type="detail.status === '0' ? 'success' : 'error'" size="small">
            {{ detail.status === '0' ? '成功' : '失败' }}
          </NTag>
        </n-descriptions-item>
        <n-descriptions-item label="登录时间">
          {{ formatDateTime(detail.loginTime) }}
        </n-descriptions-item>
        <n-descriptions-item label="提示消息" :span="2">
          {{ detail.message || '-' }}
        </n-descriptions-item>
      </n-descriptions>
    </MeModal>
  </CommonPage>
</template>

<script setup>
import { NButton, NTag } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { MeCrud, MeModal, MeQueryItem } from '@/components'
import { formatDateTime } from '@/utils'
import api from './api'

defineOptions({ name: 'LoginLogMgt' })

const $table = ref(null)
const queryItems = ref({})

onMounted(() => {
  $table.value?.handleSearch()
})

const statusOptions = [
  { label: '成功', value: '0' },
  { label: '失败', value: '1' },
]

const modalRef = ref(null)
const detail = ref({})

function handleDetail(row) {
  detail.value = row
  modalRef.value?.open({ title: '登录日志详情' })
}

const columns = [
  { title: '用户名', key: 'username', width: 160, ellipsis: { tooltip: true } },
  { title: 'IP地址', key: 'ip', width: 180, ellipsis: { tooltip: true }, render: row => row.ip || '-' },
  {
    title: '登录状态',
    key: 'status',
    width: 120,
    render: row => h(NTag, {
      type: row.status === '0' ? 'success' : 'error',
      size: 'small',
    }, { default: () => (row.status === '0' ? '成功' : '失败') }),
  },
  { title: '提示消息', key: 'message', width: 240, ellipsis: { tooltip: true }, render: row => row.message || '-' },
  {
    title: '登录时间',
    key: 'loginTime',
    width: 180,
    render: row => h('span', formatDateTime(row.loginTime)),
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    align: 'right',
    fixed: 'right',
    hideInExcel: true,
    render: row => h(
      NButton,
      {
        size: 'small',
        type: 'primary',
        secondary: true,
        onClick: () => handleDetail(row),
      },
      {
        default: () => '详情',
        icon: () => h('i', { class: 'i-carbon:view text-14' }),
      },
    ),
  },
]
</script>
