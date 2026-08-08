<template>
  <CommonPage back>
    <NSpin :show="loading">
      <template v-if="template">
        <AppCard bordered>
          <NDescriptions
            :title="template.name"
            bordered
            label-placement="left"
            :column="3"
            class="p-16"
          >
            <NDescriptionsItem label="模板编码">
              {{ template.code }}
            </NDescriptionsItem>
            <NDescriptionsItem label="最新版本">
              v{{ template.latestVersion ?? 1 }}
            </NDescriptionsItem>
            <NDescriptionsItem label="当前版本">
              {{ template.currentVersion ? `v${template.currentVersion}` : '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="模板分类">
              {{ template.category || '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="更新时间">
              {{ formatDateTime(template.lastModifiedDate) }}
            </NDescriptionsItem>
            <NDescriptionsItem label="备注">
              {{ template.remark || '—' }}
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

      <NEmpty v-else-if="!loading" class="py-40" description="表单模板不存在或已被删除" />
    </NSpin>

    <FormDefinitionPreview
      v-model:show="previewShow"
      :form-definition="previewDefinition"
      :title="previewDescription"
      :show-data-panel="false"
    />
  </CommonPage>
</template>

<script setup>
import { FormDefinitionPreview } from '@zeng-alt/formkit-form-builder'
import { NButton, NCard, NDataTable, NDescriptions, NDescriptionsItem, NEmpty, NSpin, NTag } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppCard, CommonPage } from '@/components'
import { formatDateTime } from '@/utils'
import api from './api'

defineOptions({ name: 'FormTemplateDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const template = ref(null)
const versions = ref([])
const previewShow = ref(false)
const previewDefinition = ref(null)
const previewDescription = ref('')

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
    template.value = detailRes.data
    versions.value = versionsRes.data
  }
  catch (error) {
    console.error(error)
    $message.error('加载表单详情失败')
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
    $message.success('下线成功')
    load()
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '下线失败')
  }
}

async function handlePreview(row) {
  try {
    const { data } = await api.versionDetail(row.versionId)
    if (!data?.definition) {
      $message.warning('该版本暂无表单定义')
      return
    }
    previewDefinition.value = data.definition
    previewDescription.value = `${template.value?.name || '表单'} - v${row.version}`
    previewShow.value = true
  }
  catch (error) {
    console.error(error)
    $message.error('加载表单预览失败')
  }
}

function openDesigner(row) {
  const { href } = router.resolve({
    path: '/template/form/design',
    query: {
      id: row.versionId,
      name: template.value.name,
      code: template.value.code,
      formTemplateId: template.value.formTemplateId,
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
              default: () => (row.status === 'DRAFT' ? '发布' : '上线'),
              icon: () => h('i', { class: 'i-material-symbols:publish text-14' }),
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
              default: () => '下线',
              icon: () => h('i', { class: 'i-material-symbols:arrow-downward text-14' }),
            },
          )
        : null,
    ],
  },
]

onMounted(load)
</script>
