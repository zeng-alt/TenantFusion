<template>
  <div class="h-full flex flex-col">
    <div class="flex items-center justify-between gap-8 px-4 pb-8">
      <span class="text-14 font-600">业务树</span>
      <div class="flex gap-4">
        <NTooltip>
          <template #trigger>
            <NButton size="tiny" quaternary circle @click="emit('refresh')">
              <template #icon>
                <i class="i-material-symbols:refresh text-14" />
              </template>
            </NButton>
          </template>
          刷新
        </NTooltip>
        <NButton size="tiny" type="primary" ghost @click="handleAdd()">
          <template #icon>
            <i class="i-material-symbols:add text-14" />
          </template>
          新增
        </NButton>
      </div>
    </div>

    <NInput
      v-model:value="pattern"
      size="small"
      placeholder="搜索业务"
      clearable
      class="mb-8"
    >
      <template #prefix>
        <i class="i-material-symbols:search text-13" />
      </template>
    </NInput>

    <div class="min-h-0 flex flex-col flex-1">
      <template v-if="treeData.length">
        <NScrollbar class="h-full">
          <NTree
            :data="treeData"
            :pattern="pattern"
            :show-irrelevant-nodes="false"
            :selected-keys="selectedKeys"
            :default-expand-all="true"
            :render-prefix="renderPrefix"
            :render-suffix="renderSuffix"
            :on-update:selected-keys="onSelect"
            key-field="businessId"
            label-field="name"
            block-line
          />
        </NScrollbar>
      </template>

      <!-- 空状态 -->
      <div
        v-else
        class="flex flex-col flex-1 items-center justify-center gap-10 text-center"
      >
        <div
          class="h-64 w-64 flex items-center justify-center rounded-full auto-bg-highlight"
        >
          <i class="i-material-symbols:account-tree-outline text-28 text-primary" />
        </div>
        <div class="text-14 font-500">
          暂无业务数据
        </div>
        <div class="max-w-180 text-12 text-gray-400 leading-5 dark:text-gray-500">
          创建第一个业务节点，用于关联配置表单并在右侧渲染展示
        </div>
        <NButton type="primary" size="small" class="mt-4" @click="handleAdd()">
          <template #icon>
            <i class="i-material-symbols:add text-14" />
          </template>
          新增业务
        </NButton>
      </div>
    </div>

    <BusinessEditModal ref="modalRef" :tree="treeData" @refresh="handleModalRefresh" />
  </div>
</template>

<script setup>
import { NButton, NInput, NScrollbar, NTooltip, NTree } from 'naive-ui'
import { h, ref, withModifiers } from 'vue'
import api from '../api'
import BusinessEditModal from './BusinessEditModal.vue'

defineOptions({ name: 'BusinessTree' })

const props = defineProps({
  tree: {
    type: Array,
    default: () => [],
  },
  selectedId: {
    type: [Number, String],
    default: null,
  },
})
const emit = defineEmits(['select', 'refresh'])

const pattern = ref('')
const modalRef = ref(null)

const treeData = computed(() => props.tree || [])
const selectedKeys = computed(() => (props.selectedId ? [props.selectedId] : []))

function onSelect(keys) {
  const id = keys?.[0]
  if (!id)
    return
  const node = findNode(treeData.value, id)
  emit('select', node)
}

function findNode(list, id) {
  for (const item of list || []) {
    if (String(item.businessId) === String(id))
      return item
    const found = findNode(item.children, id)
    if (found)
      return found
  }
  return null
}

function renderPrefix() {
  return h('i', { class: 'i-material-symbols:account-tree-outline text-14 text-primary' })
}

function renderSuffix({ option }) {
  return [
    h(
      NButton,
      {
        text: true,
        type: 'primary',
        size: 'tiny',
        title: '新增下级',
        onClick: withModifiers(() => handleAdd({ parentId: option.businessId }), ['stop']),
      },
      { default: () => '新增' },
    ),
    h(
      NButton,
      {
        text: true,
        type: 'info',
        size: 'tiny',
        style: 'margin-left: 8px;',
        title: '编辑',
        onClick: withModifiers(() => handleEdit(option), ['stop']),
      },
      { default: () => '编辑' },
    ),
    h(
      NButton,
      {
        text: true,
        type: 'error',
        size: 'tiny',
        style: 'margin-left: 8px;',
        title: '删除',
        onClick: withModifiers(() => handleDelete(option), ['stop']),
      },
      { default: () => '删除' },
    ),
  ]
}

function handleAdd(row = {}) {
  modalRef.value?.handleOpen({
    action: 'add',
    title: '新增业务',
    row: { ...row, parentId: row.parentId ?? null },
  })
}

function handleEdit(row) {
  modalRef.value?.handleOpen({
    action: 'edit',
    title: `编辑业务 - ${row.name}`,
    row,
  })
}

function handleDelete(item) {
  $dialog.warning({
    content: `确认删除【${item.name}】？`,
    title: '提示',
    positiveText: '确定',
    negativeText: '取消',
    async onPositiveClick() {
      try {
        await api.delete(item.businessId)
        $message.success('删除成功')
        if (String(item.businessId) === String(props.selectedId))
          emit('select', null)
        emit('refresh')
      }
      catch (error) {
        console.error(error)
      }
    },
  })
}

function handleModalRefresh(node) {
  emit('refresh', node)
}

defineExpose({
  handleAdd,
})
</script>
