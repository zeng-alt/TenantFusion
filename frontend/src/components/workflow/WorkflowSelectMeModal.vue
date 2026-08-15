<script setup>
import { NRadio } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { MeCrud, MeModal, MeQueryItem } from '@/components'
import api from './api'

defineOptions({ name: 'WorkflowSelect' })

const props = defineProps({
  /** 选中值：单选为流程对象，多选为流程对象数组（v-model:value 绑定） */
  value: {
    type: [Object, Array],
    default: null,
  },
  /** 是否多选 */
  multiple: {
    type: Boolean,
    default: false,
  },
  /** 行唯一键字段 */
  valueKey: {
    type: String,
    default: 'workflowId',
  },
  /** 输入框展示字段 */
  labelKey: {
    type: String,
    default: 'workflowName',
  },
  placeholder: {
    type: String,
    default: '请选择流程',
  },
  title: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  clearable: {
    type: Boolean,
    default: true,
  },
  /** 附加查询参数（如 category、workflowKey 等） */
  queryParams: {
    type: Object,
    default: () => ({}),
  },
  modalWidth: {
    type: String,
    default: '800px',
  },
})

const emit = defineEmits(['update:value', 'change'])

const modalRef = ref(null)
const $table = ref(null)
const queryItems = ref({ workflowName: '', workflowKey: '' })
/** 当前页数据（供多选跨页合并选中行） */
const pageRows = ref([])
const selectedRows = ref([])

const dialogTitle = computed(() => props.title || '选择流程')

const displayText = computed(() => {
  const rows = props.multiple
    ? Array.isArray(props.value) ? props.value : []
    : props.value ? [props.value] : []
  return rows.map(row => row?.[props.labelKey]).filter(Boolean).join('、')
})

function getData(params) {
  return api.list({ ...props.queryParams, ...params })
}

const columns = computed(() => {
  const selection = props.multiple
    ? { type: 'selection', width: 48, fixed: 'left' }
    : {
        key: 'selector',
        width: 48,
        fixed: 'left',
        render: row => h(NRadio, {
          checked: selectedRows.value[0]?.[props.valueKey] === row[props.valueKey],
          onUpdateChecked: () => { selectedRows.value = [row] },
        }),
      }
  return [
    selection,
    { title: '流程名称', key: 'workflowName', minWidth: 160, ellipsis: { tooltip: true } },
    { title: '流程Key', key: 'workflowKey', minWidth: 160, ellipsis: { tooltip: true } },
    { title: '分类', key: 'category', width: 120 },
    { title: '当前版本', key: 'currentVersion', width: 90 },
    { title: '最新版本', key: 'latestVersion', width: 90 },
  ]
})

/** 多选：当前页勾选变化时合并跨页已选行 */
function handleChecked(keys) {
  if (!props.multiple)
    return
  const currentKeys = new Set(pageRows.value.map(row => row[props.valueKey]))
  let next = selectedRows.value.filter(
    row => !currentKeys.has(row[props.valueKey]) || keys.includes(row[props.valueKey]),
  )
  for (const key of keys) {
    const row = pageRows.value.find(item => item[props.valueKey] === key)
    if (row && !next.some(item => item[props.valueKey] === key))
      next = [...next, row]
  }
  selectedRows.value = next
}

function handleDataChange(data) {
  pageRows.value = data || []
}

function syncFromModel() {
  if (props.multiple) {
    selectedRows.value = Array.isArray(props.value) ? [...props.value] : []
  }
  else {
    selectedRows.value = props.value ? [props.value] : []
  }
}

async function handleOpen() {
  syncFromModel()
  queryItems.value = { workflowName: '', workflowKey: '' }
  await modalRef.value.open({
    title: dialogTitle.value,
    width: props.modalWidth,
    showCancel: true,
    onOk: handleConfirm,
  })
  $table.value?.handleSearch()
}

/** 提交选中值（v-model:value 更新 + change 事件） */
function commit(rows) {
  const value = props.multiple ? rows : rows[0] || null
  emit('update:value', value)
  emit('change', value)
}

function handleConfirm() {
  commit(selectedRows.value)
  $message.success('选择成功')
}

function handleClear() {
  commit(props.multiple ? [] : null)
}

defineExpose({
  handleOpen,
  handleClear,
})
</script>

<template>
  <div class="wf-select">
    <n-input-group class="wf-select__group">
      <n-input
        :value="displayText"
        readonly
        :placeholder="placeholder"
        :clearable="clearable && !!displayText"
        class="wf-select__input"
        @clear="handleClear"
      />
      <n-button type="primary" ghost :disabled="disabled" @click="handleOpen">
        <template #icon>
          <i class="i-material-symbols:list-alt-outline text-14" />
        </template>
        选择
      </n-button>
    </n-input-group>

    <MeModal ref="modalRef" :width="modalWidth" :show-cancel="true">
      <MeCrud
        ref="$table"
        v-model:query-items="queryItems"
        expand
        :row-key="valueKey"
        :scroll-x="800"
        :columns="columns"
        :get-data="getData"
        class="wf-select__table"
        @on-checked="handleChecked"
        @on-data-change="handleDataChange"
      >
        <MeQueryItem label="流程名称" :label-width="70">
          <n-input
            v-model:value="queryItems.workflowName"
            type="text"
            status="default"
            placeholder="请输入流程名称"
            clearable
          />
        </MeQueryItem>
        <MeQueryItem label="流程Key" :label-width="65">
          <n-input
            v-model:value="queryItems.workflowKey"
            type="text"
            status="default"
            placeholder="请输入流程Key"
            clearable
          />
        </MeQueryItem>
      </MeCrud>
    </MeModal>
  </div>
</template>

<style scoped>
.wf-select {
  width: 100%;
}

.wf-select__group {
  width: 100%;
}

.wf-select__input {
  flex: 1;
}

.wf-select__table {
  height: 480px;
}
</style>
