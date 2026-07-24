<!--------------------------------
 - @Author: Ronnie Zhang
 - @LastEditor: Ronnie Zhang
 - @LastEditTime: 2023/12/16 18:49:42
 - @Email: zclzone@outlook.com
 - Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 --------------------------------->

<template>
  <ProConfigProvider
    class="wh-full"
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme="appStore.isDark ? darkTheme : undefined"
    :theme-overrides="appStore.naiveThemeOverrides"
  >
    <router-view v-if="Layout" v-slot="{ Component, route: curRoute }">
      <component :is="Layout">
        <transition name="fade-slide" mode="out-in" appear>
          <KeepAlive :include="keepAliveNames">
            <component :is="Component" v-if="!tabStore.reloading" :key="curRoute.fullPath" />
          </KeepAlive>
        </transition>
      </component>

      <LayoutSetting v-if="layoutSettingVisible" class="fixed right-12 top-1/2 z-999" />
    </router-view>
  </ProConfigProvider>
</template>

<script setup>
import { darkTheme, dateZhCN } from 'naive-ui'
import { ProConfigProvider, zhCN } from 'pro-naive-ui'
import { LayoutSetting } from '@/components'
import { useAppStore, useTabStore } from '@/store'
import { layoutSettingVisible } from './settings'

const layouts = new Map()
const layoutModules = import.meta.glob('@/layouts/**/index.vue')

function getLayout(name) {
  if (layouts.get(name))
    return layouts.get(name)
  const key = `/src/layouts/${name}/index.vue`
  const loader = layoutModules[key]
  if (!loader) {
    console.error(`Layout not found: ${key}`)
    return null
  }
  const layout = markRaw(
    defineAsyncComponent(loader),
  )
  layouts.set(name, layout)
  return layout
}

const route = useRoute()
const appStore = useAppStore()
if (appStore.layout === 'default')
  appStore.setLayout('')
const Layout = computed(() => {
  if (!route.matched?.length)
    return null
  const temp = route.meta?.layout || appStore.layout
  return getLayout(temp === 'default' ? appStore.layout : temp)
})

const tabStore = useTabStore()
const keepAliveNames = computed(() => {
  return tabStore.tabs.filter(item => item.keepAlive).map(item => item.name)
})

watchEffect(() => {
  appStore.setThemeColor(appStore.primaryColor, appStore.isDark)
})
</script>
