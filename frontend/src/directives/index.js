/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/05 21:23:01
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { withDirectives } from 'vue'
import { router } from '@/router'
import { isAdmin, isSuperAdmin } from '@/utils'

const permission = {
  mounted(el, binding) {
    if (isAdmin()) {
      return
    }
    const currentRoute = unref(router.currentRoute)
    const btns = currentRoute.meta?.btns?.map(item => item.code) || []
    if (!btns.includes(binding.value)) {
      el.remove()
    }
  },
}

const superAdmin = {
  mounted(el, binding) {
    if (!isSuperAdmin(binding.value)) {
      el.remove()
    }
  },
}

const admin = {
  mounted(el, binding) {
    if (!isAdmin(binding.value)) {
      el.remove()
    }
  },
}

export function setupDirectives(app) {
  app.directive('permission', permission)
  app.directive('superAdmin', superAdmin)
  app.directive('admin', admin)
}

/**
 * 用于h函数使用自定义权限指令
 *
 * @param {import('vue').VNode} vnode 虚拟节点
 * @param {string} code 权限码
 * @returns {import('vue').VNode} 返回一个包含权限指令的vnode
 *
 * 使用示例：withPermission(h('button', {class: 'text-red-500'}, '删除'), 'user:delete')
 *
 */
export function withPermission(vnode, code) {
  return withDirectives(vnode, [[permission, code]])
}

/**
 * 用于h函数使用判断超级管理员指令
 *
 * @param {import('vue').VNode} vnode 虚拟节点
 * @param {string} [id] 可选的用户ID，不传则使用配置的超级管理员ID
 * @returns {import('vue').VNode} 返回一个包含superAdmin指令的vnode
 *
 * 使用示例：withSuperAdmin(h('button', {class: 'text-red-500'}, '删除'), '1')
 *
 */
export function withSuperAdmin(vnode, id) {
  return withDirectives(vnode, [[superAdmin, id]])
}

/**
 * 用于h函数使用判断管理员指令
 *
 * @param {import('vue').VNode} vnode 虚拟节点
 * @param {string} [value] 可选的ID或角色code，不传则使用配置的管理员ID或角色code
 * @returns {import('vue').VNode} 返回一个包含admin指令的vnode
 *
 * 使用示例：withAdmin(h('button', {class: 'text-red-500'}, '删除'), '1')
 *
 */
export function withAdmin(vnode, value) {
  return withDirectives(vnode, [[admin, value]])
}
