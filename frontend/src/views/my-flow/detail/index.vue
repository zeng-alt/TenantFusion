<template>
  <CommonPage back>
    <template #title-suffix>
      <NTag v-if="statusCfg" class="ml-12" :type="statusCfg.type" size="small" bordered>
        {{ statusCfg.text }}
      </NTag>
    </template>

    <NSpin :show="loading">
      <template v-if="detail">
        <div class="h-full flex flex-col gap-12">
          <ProcessRuntimeViewer
            v-model:approval-collapsed="approvalCollapsed"
            class="min-h-0 flex-1"
            :process-xml="detail.processXml"
            :execution-state="detail.executionState"
          >
            <template #approveTitle>
              流转记录
            </template>

            <template #approve>
              <div>
                <NTimeline>
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
              </div>
            </template>
          </ProcessRuntimeViewer>
        </div>
      </template>

      <NEmpty v-else-if="!loading" class="py-40" description="未找到流程信息" />
    </NSpin>
  </CommonPage>
</template>

<script setup>
import { NEmpty, NSpin, NTag, NTimeline, NTimelineItem } from 'naive-ui'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CommonPage } from '@/components'
import { formatDateTime } from '@/utils'
import api from '../api'
import { ProcessRuntimeViewer } from '../components'
import { ACTION_MAP, PROCESS_STATUS_MAP } from '../renderers'

defineOptions({ name: 'MyFlowDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref(null)
const approvalCollapsed = ref(false)

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
