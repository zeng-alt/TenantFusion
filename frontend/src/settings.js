/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/13 20:54:36
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

export const defaultLayout = 'normal'

export const defaultPrimaryColor = '#316C72'

// 控制 LayoutSetting 组件是否可见
export const layoutSettingVisible = true

export const naiveThemeOverrides = {
  common: {
    primaryColor: '#316C72FF',
    primaryColorHover: '#316C72E3',
    primaryColorPressed: '#2B4C59FF',
    primaryColorSuppl: '#316C72E3',
  },
}

export const basePermissions = [
  {
    code: 'MyFlow',
    name: '我的流程',
    type: 'MENU',
    icon: 'i-fe:git-branch',
    order: 10,
    enabled: true,
    show: true,
    children: [
      {
        code: 'MyFlowTodo',
        name: '待办任务',
        type: 'MENU',
        path: '/my-flow/todo',
        component: '/src/views/my-flow/todo/index.vue',
        icon: 'i-fe:clipboard-list',
        order: 1,
        enabled: true,
        show: true,
      },
      {
        code: 'MyFlowInitiated',
        name: '我发起的',
        type: 'MENU',
        path: '/my-flow/initiated',
        component: '/src/views/my-flow/initiated/index.vue',
        icon: 'i-fe:send',
        order: 2,
        enabled: true,
        show: true,
      },
      {
        code: 'MyFlowDone',
        name: '已办任务',
        type: 'MENU',
        path: '/my-flow/done',
        component: '/src/views/my-flow/done/index.vue',
        icon: 'i-fe:check-square',
        order: 3,
        enabled: true,
        show: true,
      },
      {
        code: 'MyFlowCopied',
        name: '抄送我',
        type: 'MENU',
        path: '/my-flow/copied',
        component: '/src/views/my-flow/copied/index.vue',
        icon: 'i-fe:copy',
        order: 4,
        enabled: true,
        show: true,
      },
      {
        code: 'MyFlowProcess',
        name: '任务办理',
        type: 'MENU',
        path: '/my-flow/process/:taskId',
        component: '/src/views/my-flow/process/index.vue',
        icon: 'i-fe:edit',
        order: 5,
        enabled: true,
        show: false,
      },
      {
        code: 'MyFlowDetail',
        name: '流程详情',
        type: 'MENU',
        path: '/my-flow/detail/:processInstanceId',
        component: '/src/views/my-flow/detail/index.vue',
        icon: 'i-fe:eye',
        order: 6,
        enabled: true,
        show: false,
      },
    ],
  },
  {
    code: 'ExternalLink',
    name: '外链(可内嵌打开)',
    type: 'MENU',
    icon: 'i-fe:external-link',
    order: 98,
    enabled: true,
    show: true,
    children: [
      {
        code: 'ShowDocs',
        name: '项目文档',
        type: 'MENU',
        path: 'https://isme.top',
        icon: 'i-me:docs',
        order: 1,
        enabled: true,
        show: true,
      },
      {
        code: 'ApiFoxDocs',
        name: '接口文档',
        type: 'MENU',
        path: 'https://apifox.com/apidoc/shared-ff4a4d32-c0d1-4caf-b0ee-6abc130f734a',
        icon: 'i-me:apifox',
        order: 2,
        enabled: true,
        show: true,
      },
      {
        code: 'NaiveUI',
        name: 'Naive UI',
        type: 'MENU',
        path: 'https://www.naiveui.com/zh-CN/os-theme',
        icon: 'i-me:naiveui',
        order: 3,
        enabled: true,
        show: true,
      },
      {
        code: 'MyBlog',
        name: '博客-掘金',
        type: 'MENU',
        path: 'https://juejin.cn/user/1961184475483255/posts',
        icon: 'i-simple-icons:juejin',
        order: 4,
        enabled: true,
        show: true,
      },
      {
        code: 'Form',
        name: '动态表单',
        type: 'MENU',
        path: '/external/from',
        component: '/src/views/base/form.vue',
        icon: 'i-simple-icons:juejin',
        order: 5,
        enabled: true,
        show: true,
      },
      {
        code: 'Camunda',
        name: '流程引擎',
        type: 'MENU',
        path: '/external/camunda',
        component: '/src/views/base/camunda.vue',
        icon: 'i-simple-icons:juejin',
        order: 5,
        enabled: true,
        show: true,
      },
    ],
  },
]
