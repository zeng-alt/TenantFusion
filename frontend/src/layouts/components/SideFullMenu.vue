<!--------------------------------
 - @Author: Ronnie Zhang
 - @LastEditor: Ronnie Zhang
 - @LastEditTime: 2023/12/16 18:50:35
 - @Email: zclzone@outlook.com
 - Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 --------------------------------->

<template>
  <div class="h-full">
    <n-menu
      v-if="!appStore.collapsed"
      ref="menu"
      class="side-menu"
      accordion
      :indent="18"
      :options="permissionStore.menus"
      :render-label="renderLabel"
      :value="activeKey"
      @update:value="handleMenuSelect"
    />
    <!-- 折叠状态下的自定义菜单 -->
    <div v-else class="p-2">
      <div
        v-for="item in permissionStore.menus"
        :key="item.key"
        class="pt-2"
      >
        <!-- 有子菜单的项目 -->
        <template v-if="item.children?.length">
          <NDropdown
            v-if="!item.disabled"
            trigger="hover"
            :options="item.children"
            :inverted="inverted"
            placement="right-start"
            :show-arrow="true"
            @select="handleMenuSelect"
          >
            <div
              class="f-c-c flex-col cursor-pointer rounded-6 py-10 transition-all-300"
              :class="collapsedItemClass(item)"
            >
              <div class="mb-2 f-c-c text-18">
                <component :is="item.icon" v-if="item.icon" />
              </div>
              <div
                class="w-full overflow-hidden whitespace-nowrap text-center text-10 font-medium"
                :title="item.label"
              >
                {{ item.label }}
              </div>
            </div>
          </NDropdown>
          <div
            v-else
            class="f-c-c flex-col cursor-not-allowed rounded-6 py-10 op-50"
            :class="{ 'auto-bg-highlight': isCollapsedActive(item) }"
          >
            <div class="mb-2 f-c-c text-18">
              <component :is="item.icon" v-if="item.icon" />
            </div>
            <div
              class="w-full overflow-hidden whitespace-nowrap text-center text-10 font-medium"
              :title="item.label"
            >
              {{ item.label }}
            </div>
          </div>
        </template>
        <!-- 没有子菜单的项目 -->
        <template v-else>
          <div
            class="f-c-c flex-col cursor-pointer rounded-6 py-10 transition-all-300"
            :class="collapsedItemClass(item)"
            :title="item.label"
            @click="!item.disabled && handleMenuSelect(item.key, item)"
          >
            <div class="mb-2 f-c-c text-18">
              <component :is="item.icon" v-if="item.icon" />
            </div>
            <div
              class="w-full overflow-hidden whitespace-nowrap text-center text-10 font-medium"
            >
              {{ item.label }}
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { NDropdown } from 'naive-ui'
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore, usePermissionStore } from '@/store'
import { isExternal } from '@/utils'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

const activeKey = computed(() => route.meta?.parentKey || route.name)

const inverted = ref(false)

const menu = ref(null)

watch(route, async () => {
  await nextTick()
  menu.value?.showOption()
})

function renderLabel(option) {
  return option.label
}

function isCollapsedActive(item) {
  if (item.key === activeKey.value)
    return true
  if (item.children?.length) {
    return item.children.some(child => child.key === activeKey.value)
  }
  return false
}

function collapsedItemClass(item) {
  return {
    'auto-bg-hover': !item.disabled,
    'auto-bg-highlight': isCollapsedActive(item),
    'disabled op-50 cursor-not-allowed': item.disabled,
  }
}

function handleMenuSelect(key, item) {
  if (isExternal(item?.originPath)) {
    $dialog.confirm({
      type: 'info',
      title: `请选择打开方式`,
      positiveText: '外链打开',
      negativeText: '在本站内嵌打开',
      confirm() {
        window.open(item?.originPath)
      },
      cancel: () => {
        router.push(item.path)
      },
    })
  }
  else {
    if (!item.path)
      return
    router.push(item.path)
  }
}
</script>

<style>
</style>
