<template>
  <CommonPage>
    <MeCrud
      ref="$table"
      expand
      v-model:query-items="queryItems"
      :scroll-x="1600"
      :columns="columns"
      :get-data="api.read"
    >
      <MeQueryItem label="操作模块" :label-width="80">
        <n-input
          v-model:value="queryItems.title"
          type="text"
          placeholder="请输入操作模块"
          clearable
        />
      </MeQueryItem>

      <MeQueryItem label="操作人员" :label-width="80">
        <n-input
          v-model:value="queryItems.operName"
          type="text"
          placeholder="请输入操作人员"
          clearable
        />
      </MeQueryItem>

      <MeQueryItem label="业务类型" :label-width="80">
        <n-select
          v-model:value="queryItems.businessType"
          clearable
          :options="businessTypeOptions"
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

    <MeModal ref="modalRef" width="860px" :show-footer="false">
      <n-descriptions :column="2" label-placement="left" bordered>
        <n-descriptions-item label="操作模块">
          {{ detail.title || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="业务类型">
          {{ businessTypeText(detail.businessType) }}
        </n-descriptions-item>
        <n-descriptions-item label="操作人员">
          {{ detail.operName || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="部门名称">
          {{ detail.deptName || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="操作IP">
          {{ detail.operIp || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="操作地点">
          {{ detail.operLocation || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="请求方式">
          {{ detail.requestMethod || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="请求URL">
          {{ detail.operUrl || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="操作状态">
          <NTag :type="detail.status === 0 ? 'success' : 'error'" size="small">
            {{ detail.status === 0 ? '成功' : '失败' }}
          </NTag>
        </n-descriptions-item>
        <n-descriptions-item label="操作时间">
          {{ formatDateTime(detail.operTime) }}
        </n-descriptions-item>
        <n-descriptions-item label="消耗时间">
          {{ detail.costTime != null ? `${detail.costTime} ms` : '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="请求方法">
          {{ detail.method || '-' }}
        </n-descriptions-item>
      </n-descriptions>

      <div class="mt-20">
        <div class="mb-8 text-14 font-bold">
          请求参数
        </div>
        <pre class="log-pre">{{ prettyJson(detail.operParam) }}</pre>
      </div>
      <div class="mt-20">
        <div class="mb-8 text-14 font-bold">
          返回参数
        </div>
        <pre class="log-pre">{{ prettyJson(detail.jsonResult) }}</pre>
      </div>
      <div v-if="detail.errorMsg" class="mt-20">
        <div class="mb-8 text-14 font-bold">
          错误消息
        </div>
        <pre class="log-pre">{{ detail.errorMsg }}</pre>
      </div>
    </MeModal>
  </CommonPage>
</template>

<script setup>
import { NButton, NTag } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { MeCrud, MeModal, MeQueryItem } from '@/components'
import { formatDateTime } from '@/utils'
import api from './api'

defineOptions({ name: 'OperLogMgt' })

const $table = ref(null)
const queryItems = ref({})

onMounted(() => {
  $table.value?.handleSearch()
})

const businessTypeOptions = [
  { label: '其它', value: 0 },
  { label: '新增', value: 1 },
  { label: '修改', value: 2 },
  { label: '删除', value: 3 },
  { label: '授权', value: 4 },
  { label: '导出', value: 5 },
  { label: '导入', value: 6 },
  { label: '强退', value: 7 },
  { label: '清空数据', value: 8 },
]

const statusOptions = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 },
]

const businessTypeMap = Object.fromEntries(businessTypeOptions.map(item => [item.value, item.label]))

function businessTypeText(value) {
  return businessTypeMap[value] || '其它'
}

function prettyJson(value) {
  if (!value)
    return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  }
  catch {
    return value
  }
}

const modalRef = ref(null)
const detail = ref({})

function handleDetail(row) {
  detail.value = row
  modalRef.value?.open({ title: '操作日志详情' })
}

const columns = [
  { title: '操作模块', key: 'title', width: 150, ellipsis: { tooltip: true }, render: row => row.title || '-' },
  {
    title: '业务类型',
    key: 'businessType',
    width: 110,
    render: row => h(NTag, { size: 'small' }, { default: () => businessTypeText(row.businessType) }),
  },
  { title: '操作人员', key: 'operName', width: 130, ellipsis: { tooltip: true }, render: row => row.operName || '-' },
  { title: '操作IP', key: 'operIp', width: 150, ellipsis: { tooltip: true }, render: row => row.operIp || '-' },
  {
    title: '请求方式/URL',
    key: 'operUrl',
    width: 320,
    ellipsis: { tooltip: true },
    render: row => `${row.requestMethod || ''} ${row.operUrl || ''}`,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => h(NTag, {
      type: row.status === 0 ? 'success' : 'error',
      size: 'small',
    }, { default: () => (row.status === 0 ? '成功' : '失败') }),
  },
  {
    title: '消耗时间',
    key: 'costTime',
    width: 110,
    render: row => (row.costTime != null ? `${row.costTime} ms` : '-'),
  },
  {
    title: '操作时间',
    key: 'operTime',
    width: 180,
    render: row => h('span', formatDateTime(row.operTime)),
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

<style scoped>
.log-pre {
  margin: 0;
  padding: 12px;
  max-height: 300px;
  overflow: auto;
  background-color: #fafafc;
  border: 1px solid #e0e0e6;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
