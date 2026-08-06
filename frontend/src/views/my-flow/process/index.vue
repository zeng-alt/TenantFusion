<template>
  <CommonPage back>
    <template #title-suffix>
      <NTag v-if="task" class="ml-12" type="processing" size="small" bordered>
        {{ task.taskName }}
      </NTag>
    </template>

    <NSpin :show="loading">
      <template v-if="task">
        <AppCard bordered>
          <NDescriptions
            :title="task.processDefinitionName"
            bordered
            label-placement="left"
            :column="3"
            class="p-16"
          >
            <NDescriptionsItem label="流程定义Key" :span="2">
              {{ task.processDefinitionKey }}
            </NDescriptionsItem>
            <NDescriptionsItem label="业务Key">
              {{ task.businessKey || '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="发起人">
              {{ task.initiator || '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="任务创建时间">
              {{ formatDateTime(task.createTime) }}
            </NDescriptionsItem>
            <NDescriptionsItem label="截止时间">
              {{ task.dueTime ? formatDateTime(task.dueTime) : '—' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="当前处理人">
              {{ task.assignee || (task.candidateUsers ? '候选任务（可认领）' : '未分配') }}
            </NDescriptionsItem>
          </NDescriptions>
        </AppCard>

        <AppCard bordered class="mt-16">
          <div class="px-16 pt-16 text-16 font-500">
            流转记录
          </div>
          <NTimeline class="px-16 pb-16">
            <NTimelineItem
              v-for="(item, index) in task.history"
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

        <AppCard bordered class="mt-16">
          <div class="px-16 pt-16 text-16 font-500">
            处理意见
          </div>
          <div class="p-16">
            <NInput
              v-model:value="comment"
              type="textarea"
              :autosize="{ minRows: 4, maxRows: 8 }"
              maxlength="200"
              show-count
              placeholder="请输入处理意见（驳回时必填）"
            />
            <div class="mt-16 flex justify-end">
              <NButton type="error" :loading="submitting" @click="handleComplete('reject')">
                <i class="i-carbon:close mr-4 text-14" />
                驳回
              </NButton>
              <NButton
                type="success"
                class="ml-12"
                :loading="submitting"
                @click="handleComplete('approve')"
              >
                <i class="i-carbon:checkmark-outline mr-4 text-14" />
                同意
              </NButton>
            </div>
          </div>
        </AppCard>
      </template>

      <NEmpty v-else-if="!loading" class="py-40" description="任务不存在或已处理" />
    </NSpin>
  </CommonPage>
</template>

<script setup>
import { NButton, NDescriptions, NDescriptionsItem, NEmpty, NInput, NSpin, NTag, NTimeline, NTimelineItem } from 'naive-ui'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppCard, CommonPage } from '@/components'
import { formatDateTime } from '@/utils'
import api from '../api'
import { ACTION_MAP } from '../renderers'

defineOptions({ name: 'MyFlowProcess' })

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const task = ref(null)
const comment = ref('')

function actionCfg(action) {
  return ACTION_MAP[action] || { type: 'default', text: action || '—' }
}

function timelineType(item) {
  if (item.status === 'running')
    return 'info'
  return actionCfg(item.result).type
}

onMounted(async () => {
  const taskId = route.params.taskId
  try {
    const { data } = await api.process(taskId)
    task.value = data
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

async function handleComplete(action) {
  if (action === 'reject' && !comment.value.trim())
    return $message.warning('驳回时请填写处理意见')
  submitting.value = true
  try {
    await api.complete(task.value.taskId, { action, comment: comment.value.trim() })
    $message.success(action === 'approve' ? '已同意，流程继续流转' : '已驳回')
    router.push({ path: '/my-flow/todo' })
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '操作失败')
    submitting.value = false
  }
}
</script>
