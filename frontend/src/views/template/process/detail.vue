<template>
  <CommonPage back>
    <NSpin :show="loading">
      <template v-if="workflow">
        <AppCard bordered>
          <NDescriptions
            :title="workflow.workflowName"
            bordered
            label-placement="left"
            :column="3"
            class="p-16"
          >
            <NDescriptionsItem label="流程Key">
              {{ workflow.workflowKey }}
            </NDescriptionsItem>
            <NDescriptionsItem label="最新版本">
              v{{ workflow.latestVersion }}
            </NDescriptionsItem>
            <NDescriptionsItem label="当前版本">
              {{ workflow.currentVersion ? `v${workflow.currentVersion}` : '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="流程分类">
              {{ workflow.category || '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="更新时间">
              {{ formatDateTime(workflow.lastModifiedDate) }}
            </NDescriptionsItem>
            <NDescriptionsItem label="备注">
              {{ workflow.remark || '—' }}
            </NDescriptionsItem>
          </NDescriptions>
        </AppCard>

        <NCard title="版本列表" size="small" :bordered="true" class="mt-16">
          <template #header-extra>
            <NButton size="small" type="primary" text @click="load">
              <template #icon>
                <i class="i-material-symbols:refresh text-14" />
              </template>
            </NButton>
          </template>
          <NDataTable
            :columns="versionColumns"
            :data="versions"
            :row-key="row => row.versionId"
            :loading="loading"
            :scroll-x="1000"
            size="small"
          />
        </NCard>
      </template>

      <NEmpty v-else-if="!loading" class="py-40" description="流程不存在或已被删除" />
    </NSpin>

    <BpmnPreviewModal ref="previewModalRef" title="流程预览" :theme="isDark ? 'dark' : 'light'" />
  </CommonPage>
</template>

<script setup>
import { useDark } from '@vueuse/core'
import { BpmnPreviewModal } from '@zeng-alt/camunda7-ui'
import { NButton, NCard, NDataTable, NDescriptions, NDescriptionsItem, NEmpty, NSpin, NTag } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppCard, CommonPage } from '@/components'
import { formatDateTime } from '@/utils'
import api from './api'

defineOptions({ name: 'ProcessTemplateDetail' })

const route = useRoute()
const router = useRouter()
const isDark = useDark()

const loading = ref(true)
const workflow = ref(null)
const versions = ref([])
const previewModalRef = ref(null)

const VERSION_STATUS_MAP = {
  DRAFT: { text: '草稿', type: 'warning' },
  PUBLISHED: { text: '已发布', type: 'success' },
  OFFLINE: { text: '已下线', type: 'default' },
}

function versionStatusCfg(status) {
  return VERSION_STATUS_MAP[status] || { text: status || '—', type: 'default' }
}

async function load() {
  const id = route.query.id
  if (!id) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const [detailRes, versionsRes] = await Promise.all([api.detail(id), api.versions(id)])
    workflow.value = detailRes.data
    versions.value = versionsRes.data
  }
  catch (error) {
    console.error(error)
    $message.error('加载流程详情失败')
  }
  finally {
    loading.value = false
  }
}

async function handlePublish(row) {
  try {
    await api.publish(row.versionId)
    $message.success('上线成功')
    load()
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '上线失败')
  }
}

async function handleOffline(row) {
  try {
    await api.offline(row.versionId)
    $message.success('挂成成功')
    load()
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '挂成失败')
  }
}

async function handlePreview(row) {
  try {
    const { data } = await api.versionDetail(row.versionId)
    previewModalRef.value?.open(data?.bpmnXml || '')
  }
  catch (error) {
    console.error(error)
    $message.error('加载流程预览失败')
  }
}

function openDesigner(row) {
  const { href } = router.resolve({
    path: '/template/process/design',
    query: {
      id: row.versionId,
      workflowId: row.workflowId,
      workflowKey: workflow.value?.workflowKey,
      workflowName: workflow.value?.workflowName,
      version: row.version,
    },
  })
  window.open(href, '_blank')
}

const versionColumns = [
  {
    title: '版本号',
    key: 'version',
    width: 90,
    render: row => h(NTag, {
      size: 'small',
      type: 'primary',
      bordered: false,
    }, { default: () => `v${row.version}` }),
  },
  {
    title: '状态',
    key: 'status',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      type: versionStatusCfg(row.status).type,
      bordered: false,
    }, { default: () => versionStatusCfg(row.status).text }),
  },
  {
    title: '当前版本',
    key: 'current',
    width: 90,
    render: row => (row.current
      ? h(NTag, { size: 'small', type: 'success', bordered: false }, { default: () => '当前' })
      : '—'),
  },
  {
    title: '发布时间',
    key: 'publishedDate',
    width: 180,
    render: row => (row.publishedDate ? formatDateTime(row.publishedDate) : '—'),
  },
  {
    title: '发布人',
    key: 'publishedBy',
    width: 120,
    render: row => row.publishedBy || '—',
  },
  {
    title: '备注',
    key: 'remark',
    minWidth: 180,
    ellipsis: { tooltip: true },
    render: row => row.remark || '—',
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    align: 'right',
    render: row => [
      h(
        NButton,
        {
          text: true,
          type: 'primary',
          onClick: () => openDesigner(row),
        },
        {
          default: () => '设计',
          icon: () => h('i', { class: 'i-carbon:draw text-14' }),
        },
      ),
      h(
        NButton,
        {
          text: true,
          type: 'info',
          style: 'margin-left: 12px;',
          onClick: () => handlePreview(row),
        },
        {
          default: () => '预览',
          icon: () => h('i', { class: 'i-material-symbols:visibility-outline text-14' }),
        },
      ),
      (row.status === 'DRAFT' || row.status === 'OFFLINE')
        ? h(
            NButton,
            {
              text: true,
              type: 'success',
              style: 'margin-left: 12px;',
              onClick: () => handlePublish(row),
            },
            {
              default: () => (row.status === 'DRAFT' ? '发布' : '激活'),
              icon: () => h('i', {
                class: row.status === 'DRAFT'
                  ? 'i-material-symbols:publish text-14'
                  : 'i-material-symbols:bolt text-14',
              }),
            },
          )
        : null,
      row.status === 'PUBLISHED'
        ? h(
            NButton,
            {
              text: true,
              type: 'warning',
              style: 'margin-left: 12px;',
              onClick: () => handleOffline(row),
            },
            {
              default: () => '挂起',
              icon: () => h('i', { class: 'i-material-symbols:pause-circle-outline text-14' }),
            },
          )
        : null,
    ],
  },
]

onMounted(load)
</script>
