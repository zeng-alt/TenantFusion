<template>
  <CommonPage>
    <div class="h-full min-h-0">
      <NLayout has-sider class="h-full overflow-hidden border border-light_border rounded-8 transition-colors dark:border-dark_border">
        <!-- 左侧：业务树 -->
        <NLayoutSider
          bordered
          class="auto-bg"
          :width="280"
          :native-scrollbar="false"
          collapse-mode="width"
          :collapsed-width="0"
          show-trigger="arrow-circle"
        >
          <div class="h-full p-10">
            <BusinessTree
              ref="treeRef"
              :tree="treeData"
              :selected-id="current?.businessId"
              @select="handleSelect"
              @refresh="handleTreeRefresh"
            />
          </div>
        </NLayoutSider>

        <!-- 右侧：关联配置表单渲染 -->
        <NLayoutContent class="min-h-0 auto-bg-highlight">
          <BusinessFormPanel
            ref="formPanelRef"
            :business="current"
            @refresh="handleFormRefresh"
          />
        </NLayoutContent>
      </NLayout>
    </div>
  </CommonPage>
</template>

<script setup>
import { NLayout, NLayoutContent, NLayoutSider } from 'naive-ui'
import { onMounted, ref } from 'vue'
import { CommonPage } from '@/components'
import api from './api'
import BusinessFormPanel from './components/BusinessFormPanel.vue'
import BusinessTree from './components/BusinessTree.vue'

defineOptions({ name: 'BusinessMgt' })

const treeData = ref([])
const current = ref(null)
const treeRef = ref(null)
const formPanelRef = ref(null)

onMounted(async () => {
  await loadTree()
})

async function loadTree() {
  try {
    const { data } = await api.tree()
    treeData.value = data || []
    // 默认选中第一个根节点
    if (!current.value && treeData.value.length) {
      current.value = treeData.value[0]
      formPanelRef.value?.load()
    }
  }
  catch (error) {
    console.error(error)
    $message.error('加载业务树失败')
  }
}

function handleSelect(node) {
  if (!node) {
    current.value = null
    return
  }
  current.value = node
  formPanelRef.value?.load()
}

function handleTreeRefresh(node) {
  loadTree()
  if (node)
    current.value = node
}

function handleFormRefresh(node) {
  loadTree().then(() => {
    if (node?.businessId) {
      current.value = findById(treeData.value, node.businessId) || node
      formPanelRef.value?.load()
    }
  })
}

function findById(list, id) {
  for (const item of list || []) {
    if (String(item.businessId) === String(id))
      return item
    const found = findById(item.children, id)
    if (found)
      return found
  }
  return null
}
</script>
