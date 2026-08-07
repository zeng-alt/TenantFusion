export const basicRoutes = [
  {
    name: 'Login',
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: {
      title: '登录页',
      layout: 'empty',
    },
  },

  {
    name: 'Home',
    path: '/',
    component: () => import('@/views/home/index.vue'),
    meta: {
      title: '首页',
    },
  },

  {
    name: '404',
    path: '/404',
    component: () => import('@/views/error-page/404.vue'),
    meta: {
      title: '页面飞走了',
      layout: 'empty',
    },
  },

  {
    name: '403',
    path: '/403',
    component: () => import('@/views/error-page/403.vue'),
    meta: {
      title: '没有权限',
      layout: 'empty',
    },
  },

  {
    name: 'FormDesignPage',
    path: '/template/form/design',
    component: () => import('@/views/template/form/design.vue'),
    meta: {
      title: '表单设计',
      layout: 'empty',
    },
  },

  {
    name: 'FormTemplateDetailPage',
    path: '/template/form/detail',
    component: () => import('@/views/template/form/detail.vue'),
    meta: {
      title: '表单详情',
    },
  },

  {
    name: 'FormConfigDesignPage',
    path: '/template/form-config/design',
    component: () => import('@/views/template/form-config/design.vue'),
    meta: {
      title: '配置表单设计',
      layout: 'empty',
    },
  },

  {
    name: 'FormConfigDetailPage',
    path: '/template/form-config/detail',
    component: () => import('@/views/template/form-config/detail.vue'),
    meta: {
      title: '配置表单详情',
    },
  },

  {
    name: 'ProcessDesignPage',
    path: '/template/process/design',
    component: () => import('@/views/template/process/design.vue'),
    meta: {
      title: '流程设计',
      layout: 'empty',
    },
  },

  {
    name: 'ProcessDetailPage',
    path: '/template/process/detail',
    component: () => import('@/views/template/process/detail.vue'),
    meta: {
      title: '流程详情',
    },
  },
]
