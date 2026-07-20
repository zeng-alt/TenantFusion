<template>
  <AppPage show-footer>
    <CommonPage :show-header="false">
      <div class="menu-container">
        <div class="menu-content">
          <!-- 搜索框 -->
          <div class="search-section">
            <div class="search-wrapper">
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
          <div class="menu-grid">
            <div
              v-for="item in filteredMenuItems"
              :key="item.code"
              class="menu-card-wrapper"
            >
              <n-card
                class="menu-card"
                hoverable
                @click="handleCardClick(item)"
              >
                <div class="card-content">
                  <!-- 图标区域 -->
                  <div class="icon-wrapper">
                    <div class="icon-container">
                      <i
                        :class="`${item.icon}?mask`"
                        :style="{ color: themeVars.primaryColor }"
                        class="menu-icon"
                      />
                    </div>
                  </div>

                  <!-- 文字区域 -->
                  <div class="text-content">
                    <div class="menu-title">
                      {{ item.name }}
                    </div>
                    <div v-if="item.description" class="menu-description">
                      {{ item.description }}
                    </div>
                  </div>
                </div>
              </n-card>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="filteredMenuItems.length === 0" class="empty-state">
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
/* 主容器 - 设置固定高度和滚动 */
.menu-container {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.menu-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  /* 自定义滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: v-bind('themeVars.scrollbarColor') transparent;
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

/* 搜索区域 */
.search-section {
  margin-bottom: 24px;
  display: flex;
  justify-content: flex-end;
  position: sticky;
  top: 0;
  z-index: 10;
  padding-bottom: 8px;
}

.search-wrapper {
  width: 100%;
  max-width: 400px;
}

/* 响应式网格布局 */
.menu-grid {
  display: grid;
  gap: 20px;
  /* 手机：1列 */
  grid-template-columns: 1fr;
  padding-bottom: 24px;
}

/* 平板：2列 */
@media (min-width: 640px) {
  .menu-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }
}

/* 小屏幕：3列 */
@media (min-width: 968px) {
  .menu-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 中等屏幕：4列 */
@media (min-width: 1200px) {
  .menu-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

/* 大屏幕：5列 */
@media (min-width: 1600px) {
  .menu-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}

/* 超大屏幕：6列 */
@media (min-width: 1920px) {
  .menu-grid {
    grid-template-columns: repeat(6, 1fr);
  }
}

/* 卡片容器 */
.menu-card-wrapper {
  width: 100%;
}

/* 卡片样式 */
.menu-card {
  height: 100%;
  min-height: 120px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
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

/* 卡片内容布局 */
.card-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px;
  text-align: center;
}

/* 图标区域 */
.icon-wrapper {
  margin-bottom: 12px;
}

.icon-container {
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.menu-card:hover .icon-container {
  transform: scale(1.1);
}

.menu-icon {
  font-size: 28px;
  color: v-bind('themeVars.primaryColor') !important;
  transition: all 0.3s ease;
}

.menu-card:hover .menu-icon {
  color: v-bind('themeVars.primaryColorHover') !important;
  transform: scale(1.1);
}

/* 文字内容 */
.text-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.menu-title {
  font-size: 14px;
  font-weight: 600;
  color: v-bind('themeVars.textColor1');
  margin-bottom: 4px;
  line-height: 1.4;
}

.menu-description {
  font-size: 12px;
  color: v-bind('themeVars.textColor3');
  line-height: 1.3;
  opacity: 0.8;
  max-height: 2.6em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

/* 小屏幕优化 */
@media (max-width: 639px) {
  .card-content {
    padding: 16px 12px;
  }

  .menu-icon {
    font-size: 24px;
  }

  .menu-title {
    font-size: 13px;
  }

  .menu-description {
    font-size: 11px;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .menu-card:hover {
    box-shadow:
      0 12px 24px -8px rgba(0, 0, 0, 0.3),
      0 4px 16px -4px rgba(0, 0, 0, 0.2);
  }
}
</style>
