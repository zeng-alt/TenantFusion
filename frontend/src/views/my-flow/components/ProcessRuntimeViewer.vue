<template>
  <NLayout class="h-full min-h-0 flex flex-col">
    <!-- 顶部区域 -->
    <NLayoutHeader class="h-[360px] min-h-[200px] shrink-0">
      <NLayout
        has-sider
        sider-placement="right"
        class="h-full"
      >
        <!-- 左侧内容 -->
        <NLayoutContent class="h-full min-h-0 overflow-hidden">
          <NScrollbar class="h-full">
            <NCard embedded size="small">
              <div class="p-6">
                <div v-for="i in 50" :key="i">
                  主内容 {{ i }}
                </div>
              </div>
            </NCard>
          </NScrollbar>
        </NLayoutContent>
        <!-- 右侧 -->
        <NLayoutSider
          class="h-full"
          collapse-mode="transform"
          :collapsed-width="0"
          :width="240"
          :native-scrollbar="false"
          show-trigger="bar"
          bordered
        >
          <NScrollbar class="h-full">
            <div class="p-6 space-y-3">
              <div
                v-for="i in 50"
                :key="i"
              >
                海淀桥 {{ i }}
              </div>
            </div>
          </NScrollbar>
        </NLayoutSider>
      </NLayout>
    </NLayoutHeader>
    <!-- 底部 -->
    <NLayoutContent
      bordered
      class="min-h-0 flex-1 overflow-hidden"
    >
      <NCard
        bordered
        size="small"
        class="h-full min-h-0 flex flex-col"
      >
        <NTabs
          type="bar"
          size="small"
          class="h-full min-h-0 flex flex-col"
        >
          <!-- 全局表单 -->
          <NTabPane
            name="global-form"
            tab="全局表单"
            class="h-full min-h-0"
          >
            <NScrollbar class="h-full">
              <div class="px-8 pb-8">
                <FormSchemaRenderer
                  v-if="formDefinition || formSchema"
                  v-model="formValues"
                  :http="request"
                  :definition="formDefinition"
                  :schema="formSchema"
                  :actions="false"
                  label-position="top"
                />
                <div v-else class="min-h-[300px] flex items-center justify-center">
                  <NEmpty
                    size="large"
                    description="暂无全局表单"
                    class="flex items-center justify-center"
                  />
                </div>
              </div>
            </NScrollbar>
          </NTabPane>
          <NTabPane
            name="process"
            tab="流程信息"
            class="h-full min-h-0"
          >
            <div class="relative h-full min-h-0 w-full">
              <BuilderProvider :config="formBuilderConfig">
                <BpmnProcessViewer
                  :process-xml="processXml"
                  :execution-state="executionState"
                  :show-timeline="showTimeline"
                />
              </BuilderProvider>
            </div>
          </NTabPane>
        </NTabs>
      </NCard>
    </NLayoutContent>
  </NLayout>
</template>

<script setup>
import { BpmnProcessViewer } from '@zeng-alt/camunda7-ui'
import { BuilderProvider, FormSchemaRenderer } from '@zeng-alt/formkit-form-builder'
import { NEmpty, NLayout, NLayoutContent, NLayoutHeader, NLayoutSider, NScrollbar, NTabs } from 'naive-ui'
import { computed } from 'vue'
import { request } from '@/utils'
import { createFormBuilderConfig } from '@/views/template/form/formBuilderConfig'

defineOptions({ name: 'ProcessRuntimeViewer' })

const props = defineProps({
  formDefinition: {
    type: Object,
    default: null,
  },
  formSchema: {
    type: Array,
    default: null,
  },
  formValues: {
    type: Object,
    default: () => ({}),
  },
  processXml: {
    type: String,
    default: '',
  },
  executionState: {
    type: Object,
    default: null,
  },
  showTimeline: {
    type: Boolean,
    default: false,
  },
  approvalCollapsed: {
    type: Boolean,
    default: false,
  },
  approvalWidth: {
    type: Number,
    default: 300,
  },
  approveLoading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:formValues', 'update:approvalCollapsed', 'approve'])

const formBuilderConfig = createFormBuilderConfig()

// const isDark = useDark()

const formValues = computed({
  get: () => props.formValues,
  set: value => emit('update:formValues', value),
})
</script>
