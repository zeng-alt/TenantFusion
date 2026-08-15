<template>
  <CommonPage back>
    <template #title-suffix>
      <NTag v-if="statusCfg" class="ml-12" :type="statusCfg.type" size="small" bordered>
        {{ statusCfg.text }}
      </NTag>
    </template>

    <div class="h-full flex flex-col gap-12">
      <ProcessRuntimeViewer
        v-model:approval-collapsed="approvalCollapsed"
        class="min-h-0 flex-1"
        :process-instance-id="processInstanceId"
        @status="status = $event"
      />
    </div>
  </CommonPage>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CommonPage } from '@/components'
import { ProcessRuntimeViewer } from '../components'
import { PROCESS_STATUS_MAP } from '../renderers'

defineOptions({ name: 'MyFlowDetail' })

const route = useRoute()

const processInstanceId = route.params.processInstanceId
const approvalCollapsed = ref(false)
const status = ref('')

const statusCfg = computed(() => PROCESS_STATUS_MAP[status.value] || { type: 'default', text: status.value || '' })
</script>
