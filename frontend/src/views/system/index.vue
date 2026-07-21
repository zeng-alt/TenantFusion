<template>
  <AppPage show-footer>
    <CommonPage :show-header="false">
      <div class="h-screen flex flex-col overflow-hidden">
        <div class="menu-content flex-1 overflow-y-auto p-24">
          <!-- 搜索框 -->
          <div class="sticky top-0 z-10 mb-24 flex justify-end pb-8">
            <div class="max-w-400 w-full">
              <n-input
                v-model:value="searchText"
                placeholder="搜索菜单..."
                clearable
                size="small"
                round
              >
                <template #prefix>
                  <i class="i-carbon:search text-18 opacity-60" />
                </template>
              </n-input>
            </div>
          </div>

          <!-- 响应式网格 -->
          <div class="sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 grid grid-cols-1 gap-20 pb-24 3xl:grid-cols-6">
            <div
              v-for="item in filteredMenuItems"
              :key="item.code"
            >
              <n-card
                class="menu-card h-full cursor-pointer rounded-12 transition-all duration-300 ease-out"
                hoverable
                @click="handleCardClick(item)"
              >
                <div class="h-full flex flex-col items-center justify-center gap-10 p-12 text-center">
                  <!-- 图标区域 -->
                  <div class="flex items-center justify-center transition-all duration-300 group-hover:scale-110">
                    <i
                      :class="`${item.icon}?mask`"
                      :style="{ color: themeVars.primaryColor }"
                      class="sm:text-28 text-24 transition-all duration-300"
                    />
                  </div>

                  <!-- 文字区域 -->
                  <div class="flex flex-col flex-1 justify-center gap-2">
                    <div class="menu-title sm:text-14 text-13 font-600 leading-1.4">
                      {{ item.name }}
                    </div>
                    <div v-if="item.description" class="menu-description sm:text-12 line-clamp-2 text-11 leading-1.3 opacity-80">
                      {{ item.description }}
                    </div>
                  </div>
                </div>
              </n-card>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="filteredMenuItems.length === 0" class="min-h-200 flex items-center justify-center">
            <n-empty description="没有找到匹配的菜单" size="huge" />
          </div>
        </div>
      </div>
    </CommonPage>
  </AppPage>
</template>

<script setup>
import { hyphenate } from '@vueuse/core'
import { useThemeVars } from 'naive-ui'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePermissionStore } from '@/store/index.js'

const router = useRouter()
const permissionStore = usePermissionStore()
const themeVars = useThemeVars()
const searchText = ref('')

// 获取当前路由对应的菜单项
const menuItems = computed(() => {
  const currentRoute = router.currentRoute.value
  const parentMenu = permissionStore.permissions.find(p => p.code === currentRoute.name)
  if (!parentMenu)
    return []

  return parentMenu.children
    ?.filter(item => item.type === 'MENU')
    .map(item => ({
      ...item,
      path: item.path || `/${hyphenate(item.code)}`,
    })) || []
})

// 过滤后的菜单项
const filteredMenuItems = computed(() => {
  if (!searchText.value)
    return menuItems.value

  const searchLower = searchText.value.toLowerCase()
  return menuItems.value.filter(item =>
    item.name.toLowerCase().includes(searchLower)
    || (item.description && item.description.toLowerCase().includes(searchLower)),
  )
})

function handleCardClick(item) {
  if (item.originPath) {
    window.open(item.originPath)
  }
  else {
    router.push(item.path)
  }
}
</script>

<style scoped>
.menu-card {
  background: v-bind('themeVars.cardColor');
  border: 1px solid v-bind('themeVars.borderColor');
}
.menu-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 12px 24px -8px rgba(0, 0, 0, 0.15),
    0 4px 16px -4px rgba(0, 0, 0, 0.1);
  border-color: v-bind('themeVars.primaryColorHover');
}

.menu-title {
  color: v-bind('themeVars.textColor1');
}
.menu-description {
  color: v-bind('themeVars.textColor3');
}

.menu-content::-webkit-scrollbar {
  width: 6px;
}
.menu-content::-webkit-scrollbar-track {
  background: transparent;
}
.menu-content::-webkit-scrollbar-thumb {
  background-color: v-bind('themeVars.scrollbarColor');
  border-radius: 3px;
  transition: background-color 0.3s ease;
}
.menu-content::-webkit-scrollbar-thumb:hover {
  background-color: v-bind('themeVars.scrollbarColorHover');
}

@media (prefers-color-scheme: dark) {
  .menu-card:hover {
    box-shadow:
      0 12px 24px -8px rgba(0, 0, 0, 0.3),
      0 4px 16px -4px rgba(0, 0, 0, 0.2);
  }
}
</style>
