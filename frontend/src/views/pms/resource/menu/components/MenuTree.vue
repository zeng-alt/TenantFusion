<!--------------------------------
 - @Author: Ronnie Zhang
 - @LastEditor: Ronnie Zhang
 - @LastEditTime: 2024/04/01 15:51:34
 - @Email: zclzone@outlook.com
 - Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 --------------------------------->

<template>
  <div>
    <n-space vertical :size="12">
      <div class="flex items-center justify-between">
        <h3>菜单</h3>
        <div class="flex gap-2">
          <n-tooltip trigger="hover">
            <template #trigger>
              <NButton type="primary" quaternary @click="assignMenu()">
                <i class="i-material-symbols:person-check mr-4 text-14" />
              </NButton>
            </template>
            分配菜单
          </n-tooltip>
        </div>
      </div>
      <div class="flex">
        <n-input v-model:value="pattern" placeholder="搜索" clearable />
        <NButton class="ml-12" type="primary" @click="handleAdd()">
          <i class="i-material-symbols:add mr-4 text-14" />
          新增
        </NButton>
      </div>

      <n-tree
        :show-irrelevant-nodes="false"
        :pattern="pattern"
        :data="treeData"
        :selected-keys="[currentMenu?.code]"
        :render-prefix="renderPrefix"
        :render-suffix="renderSuffix"
        :on-update:selected-keys="onSelect"
        key-field="code"
        label-field="name"

        block-line default-expand-all
      />
    </n-space>

    <ResAddOrEdit ref="modalRef" :menus="treeData" @refresh="(data) => emit('refresh', data)" />
    <AssignMenu ref="assignMenuRef" width="1200px" />
  </div>
</template>

<script setup>
import { NButton } from 'naive-ui'
import { ref, withModifiers } from 'vue'
import api from '../api'
import AssignMenu from './AssignMenu.vue'
import ResAddOrEdit from './ResAddOrEdit.vue'

const props = defineProps({
  treeData: {
    type: Array,
    default: () => [],
  },
  currentMenu: {
    type: Object,
    default: () => null,
  },
})
const emit = defineEmits(['refresh', 'update:currentMenu'])

const pattern = ref('')

const modalRef = ref(null)
async function handleAdd(data = {}) {
  modalRef.value?.handleOpen({
    action: 'add',
    type: 'MENU',
    title: '新增菜单',
    row: { type: 'MENU', ...data },
    okText: '保存',
  })
}

function onSelect(keys, option, { action, node }) {
  emit('update:currentMenu', action === 'select' ? node : null)
}

function renderPrefix({ option }) {
  return h('i', { class: `${option.icon}?mask text-16` })
}

function renderSuffix({ option }) {
  return [
    h(
      NButton,
      {
        text: true,
        type: 'primary',
        title: '新增下级菜单',
        size: 'tiny',
        onClick: withModifiers(() => handleAdd({ menuId: option.id }), ['stop']),
      },
      { default: () => '新增' },
    ),

    h(
      NButton,
      {
        text: true,
        type: 'error',
        size: 'tiny',
        style: 'margin-left: 12px;',
        onClick: withModifiers(() => handleDelete(option), ['stop']),
      },
      { default: () => '删除' },
    ),
  ]
}

function handleDelete(item) {
  $dialog.confirm({
    content: `确认删除【${item.name}】？`,
    async confirm() {
      try {
        $message.loading('正在删除', { key: 'deleteMenu' })
        await api.deleteMenu(item.id)
        $message.success('删除成功', { key: 'deleteMenu' })
        emit('refresh')
        if (item.id === props.currentMenu?.id)
          emit('update:currentMenu', null)
      }
      catch (error) {
        console.error(error)
        $message.error('删除失败', { key: 'deleteMenu' })
      }
    },
  })
}

const assignMenuRef = ref(null)

function assignMenu() {
  assignMenuRef.value?.handleOpen({
    action: 'assign',
    title: '分配菜单权限',
    okText: '分配',
  })
}
</script>
