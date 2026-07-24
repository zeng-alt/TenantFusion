/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/05 21:25:47
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { hyphenate } from '@vueuse/core'
import { defineStore } from 'pinia'
import { h } from 'vue'
import { isExternal } from '@/utils'

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    accessRoutes: [],
    permissions: [],
    menus: [],
    admin: {},
  }),
  getters: {
    getSuperAdminIdentity() {
      return this.admin?.id
    },
    getAdminIdentity() {
      return this.admin?.code
    },
  },
  actions: {
    setAdmin(admin) {
      this.admin = admin
    },
    setPermissions(permissions) {
      this.permissions = permissions
      this.menus = this.permissions
        .filter(item => item.type === 'MENU')
        .map(item => this.getMenuItem(item))
        .filter(item => !!item)
        .sort((a, b) => a.order - b.order)
    },
    getMenuItem(item, parent) {
      const route = this.generateRoute(item, item.show ? null : parent?.key)
      if (item.enabled && route.path && !route.path.startsWith('http'))
        this.accessRoutes.push(route)
      const menuItem = {
        label: route.meta.title,
        key: route.name,
        path: route.path,
        originPath: route.meta.originPath,
        icon: () => h('i', { class: `${route.meta.icon} text-16` }),
        order: item.order ?? 0,
        menuStyle: item.menuStyle || 'default',
      }
      const children = item.children?.filter(item => item.type === 'MENU') || []
      if (children.length) {
        if (menuItem.menuStyle === 'list') {
          menuItem.children = undefined
          children.forEach((child) => {
            this.getMenuItem(child, menuItem)
          })
        }
        else {
          menuItem.children = children
            .map(child => this.getMenuItem(child, menuItem))
            .filter(item => !!item)
            .sort((a, b) => a.order - b.order)
          if (!menuItem.children.length)
            delete menuItem.children
        }
      }
      if (!item.show)
        return null
      return menuItem
    },
    generateRoute(item, parentKey) {
      let originPath
      if (isExternal(item.path)) {
        originPath = item.path
        item.component = '/src/views/iframe/index.vue'
        item.path = `/iframe/${hyphenate(item.code)}`
      }
      if (item.menuStyle === 'list') {
        item.component = '/src/views/system/index.vue'
        if (!item.path) {
          item.path = `/system/${hyphenate(item.code)}`
        }
      }
      return {
        name: item.code,
        path: item.path,
        redirect: item.redirect,
        component: item.component,
        meta: {
          originPath,
          icon: `${item.icon}?mask`,
          title: item.name,
          layout: item.layout,
          keepAlive: !!item.keepAlive,
          parentKey,
          menuStyle: item.menuStyle || 'default',
          btns: item.children
            ?.filter(item => item.type === 'HTTP' || item.type === 'BUTTON')
            .map(item => ({ code: item.code, name: item.name })),
        },
      }
    },
    resetPermission() {
      this.$reset()
    },
  },
})
