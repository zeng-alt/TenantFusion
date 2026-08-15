<template>
  <NModal
    v-model:show="visible"
    preset="card"
    title="办理任务"
    style="width: 800px; max-width: 90vw;"
    :mask-closable="false"
    :closable="!completing"
  >
    <div class="max-h-[60vh] min-h-[200px] overflow-y-auto">
      <NSpin v-if="loading" class="w-full py-20" />
      <TaskFormPanel v-else ref="formPanelRef" :task-forms="taskForms" />
    </div>
    <template #footer>
      <div class="flex justify-end gap-10">
        <NButton :disabled="completing" @click="visible = false">
          取消
        </NButton>
        <NButton
          type="primary"
          :loading="completing"
          @click="handleSubmit"
        >
          提交
        </NButton>
      </div>
    </template>
  </NModal>
</template>

<script setup>
import { NButton, NModal, NSpin } from 'naive-ui'
import { ref, watch } from 'vue'
import api from '../api'
import TaskFormPanel from './TaskFormPanel.vue'

defineOptions({ name: 'TaskFormModal' })

const props = defineProps({
  /** 当前任务ID */
  taskId: {
    type: String,
    default: '',
  },
  /** 流程实例ID（成功后父组件刷新用） */
  processInstanceId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['success'])

const visible = defineModel('visible', {
  type: Boolean,
  default: false,
})

const loading = ref(false)
const completing = ref(false)
const taskForms = ref([])
const formPanelRef = ref(null)

async function loadTaskForms() {
  if (!props.taskId)
    return
  loading.value = true
  taskForms.value = []
  try {
    const { data } = await api.taskForms(props.taskId)
    taskForms.value = data || []
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '加载任务表单失败')
    taskForms.value = []
  }
  finally {
    loading.value = false
  }
}

watch(visible, (show) => {
  if (show)
    loadTaskForms()
})

async function handleSubmit() {
  if (!props.taskId || completing.value)
    return
  completing.value = true
  try {
    const variables = formPanelRef.value?.getFormValues() || {}
    await api.complete({ taskId: props.taskId, variables })
    $message.success('提交成功')
    visible.value = false
    emit('success')
  }
  catch (error) {
    console.error(error)
    $message.error(error?.message || '提交失败')
  }
  finally {
    completing.value = false
  }
}
</script>
