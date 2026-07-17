<template>
  <MeModal ref="modalRef">
    <div class="flex gap-4">
      <!-- 左侧菜单树 -->
      <div class="w-300 flex-shrink-0">
        <NCard title="菜单权限" :bordered="false" hoverable size="small">
          <n-spin :show="loading">
            <n-tree
              key-field="id"
              label-field="name"
              :selectable="false"
              :data="permissionTree"
              :checked-keys="permissionIds"
              :on-update:checked-keys="(keys) => (permissionIds = keys)"
              :cascade="true"
              default-expand-all
              checkable
              check-on-click
              class="cus-scroll max-h-600 w-full"
            />
          </n-spin>
        </NCard>
      </div>

      <!-- 右侧角色表格 -->
      <div class="flex-1">
        <NCard title="角色列表" hoverable size="small">
          <MeCrud
            ref="$table"
            v-model:query-items="queryItems"
            size="small"
            :scroll-x="-1"
            :columns="columns"
            class="min-h-450"
            :get-data="handlePageRole"
            @update:checked-row-keys="onChecked"
          >
            <MeQueryItem label="角色名" :label-width="50">
              <n-input v-model:value="queryItems.name" size="small" type="text" placeholder="请输入角色名" clearable />
            </MeQueryItem>
            <MeQueryItem label="状态" :label-width="50">
              <n-select
                v-model:value="queryItems.enabled"
                size="small"
                clearable
                :options="[
                  { label: '启用', value: true },
                  { label: '停用', value: false },
                ]"
              />
            </MeQueryItem>
          </MeCrud>
        </NCard>
      </div>
    </div>
  </MeModal>
</template>

<script setup>
import { NCard, NSwitch } from 'naive-ui'
import { ref } from 'vue'
import { MeCrud, MeModal, MeQueryItem } from '@/components'
import { useModal } from '@/composables'
import api from '@/views/pms/role/api.js'

const emit = defineEmits(['refresh'])

const $table = ref(null)
const [modalRef, okLoading] = useModal()
const modalAction = ref('')

function handlePageRole(params) {
  const { pageNo, pageSize, ...rest } = params
  return api.read({ page: pageNo, size: pageSize, ...rest })
}

/** QueryBar筛选参数（可选） */
const queryItems = ref({
  name: '',
  enabled: null,
})

const permissionTree = ref([])
const permissionIds = ref([])
const roleIds = ref([])
const loading = ref(false)

function onChecked(rowKeys) {
  roleIds.value = rowKeys || []
}

async function loadPermissionTree() {
  loading.value = true
  try {
    const res = await api.getAllPermissionEnableTree()
    permissionTree.value = res ?? []
  }
  catch (error) {
    console.error('加载权限树失败:', error)
    $message.error('加载权限树失败')
  }
  finally {
    loading.value = false
  }
}

function handleOpen(options = {}) {
  const { action, ids = [], ...rest } = options
  modalAction.value = action
  permissionIds.value = ids
  loadPermissionTree()
  modalRef.value.open({ ...rest, onOk: onSave })
}

async function onSave() {
  if (roleIds.value.length === 0) {
    $message.error('请选择角色')
    return false
  }

  okLoading.value = true
  try {
    await api.authorizePermission({ roleIds: roleIds.value, permissionIds: permissionIds.value })
    okLoading.value = false
    $message.success('分配菜单成功')
    emit('refresh')
    return true
  }
  catch (error) {
    console.error(error)
    okLoading.value = false
    return false
  }
}

async function handleEnable(row) {
  try {
    await api.update({ id: row.id, enabled: !row.enabled })
    $message.success('操作成功')
    $table.value?.handleSearch()
  }
  catch (error) {
    console.error(error)
  }
}

const columns = [
  { type: 'selection', fixed: 'left' },
  { title: '角色名', key: 'name' },
  { title: '角色编码', key: 'code' },
  {
    title: '状态',
    key: 'enabled',
    render: row =>
      h(
        NSwitch,
        {
          size: 'small',
          rubberBand: false,
          value: row.enabled,
          loading: !!row.enableLoading,
          disabled: row.code === 'SUPER_ADMIN',
          onUpdateValue: () => handleEnable(row),
        },
        {
          checked: () => '启用',
          unchecked: () => '停用',
        },
      ),
  },
]

defineExpose({
  handleOpen,
})
</script>

<style scoped>
.cus-scroll {
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
