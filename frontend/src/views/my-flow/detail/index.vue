<template>
  <CommonPage back>
    <template #title-suffix>
      <NTag v-if="statusCfg" class="ml-12" :type="statusCfg.type" size="small" bordered>
        {{ statusCfg.text }}
      </NTag>
    </template>

    <NSpin :show="loading">
      <template v-if="detail">
        <AppCard bordered>
          <NDescriptions
            :title="detail.processDefinitionName"
            bordered
            label-placement="left"
            :column="3"
            class="p-16"
          >
            <NDescriptionsItem label="流程定义Key" :span="2">
              {{ detail.processDefinitionKey }}
            </NDescriptionsItem>
            <NDescriptionsItem label="业务Key">
              {{ detail.businessKey || '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="发起人">
              {{ detail.initiator || detail.startUserName || '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="发起时间">
              {{ formatDateTime(detail.startTime || detail.createTime) }}
            </NDescriptionsItem>
            <NDescriptionsItem label="当前节点">
              {{ detail.currentTaskName || detail.taskName || '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="当前处理人">
              {{ detail.currentAssignee || detail.assignee || '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="结束时间">
              {{ detail.endTime ? formatDateTime(detail.endTime) : '—' }}
            </NDescriptionsItem>
          </NDescriptions>
        </AppCard>

        <AppCard bordered class="mt-16">
          <div class="px-16 pt-16 text-16 font-500">
            流转记录
          </div>
          <NTimeline class="px-16 pb-16">
            <NTimelineItem
              v-for="(item, index) in detail.history"
              :key="index"
              :type="timelineType(item)"
              :title="item.nodeName"
            >
              <div class="flex items-center justify-between">
                <span class="text-13 font-500">{{ item.nodeName }}</span>
                <NTag
                  v-if="item.result"
                  size="small"
                  :type="actionCfg(item.result).type"
                  :bordered="false"
                >
                  {{ actionCfg(item.result).text }}
                </NTag>
              </div>
              <div class="mt-4 text-13 text-gray-400">
                {{ item.assignee }}
              </div>
              <div v-if="item.startTime" class="text-13 text-gray-400">
                {{ formatDateTime(item.startTime) }}
                <template v-if="item.endTime">
                  → {{ formatDateTime(item.endTime) }}
                </template>
              </div>
              <div v-if="item.comment" class="mt-4 text-13">
                {{ item.comment }}
              </div>
            </NTimelineItem>
          </NTimeline>
        </AppCard>
      </template>

      <NEmpty v-else-if="!loading" class="py-40" description="未找到流程信息" />
    </NSpin>
  </CommonPage>
</template>

<script setup>
import { NDescriptions, NDescriptionsItem, NEmpty, NSpin, NTag, NTimeline, NTimelineItem } from 'naive-ui'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppCard, CommonPage } from '@/components'
import { formatDateTime } from '@/utils'
import api from '../api'
import { ACTION_MAP, PROCESS_STATUS_MAP } from '../renderers'

defineOptions({ name: 'MyFlowDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref(null)

const statusCfg = computed(() => {
  const status = detail.value?.status
  return PROCESS_STATUS_MAP[status] || { type: 'default', text: status || '' }
})

function actionCfg(action) {
  return ACTION_MAP[action] || { type: 'default', text: action || '—' }
}

function timelineType(item) {
  if (item.status === 'running')
    return 'info'
  return actionCfg(item.result).type
}

onMounted(async () => {
  const processInstanceId = route.params.processInstanceId
  try {
    const { data } = await api.detail(processInstanceId)
    detail.value = data
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '加载失败')
    router.back()
  }
  finally {
    loading.value = false
  }
})
</script>
