/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/04 22:50:38
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { request } from '@/utils'

export default {
  // 获取用户信息
  getUser: () => request.get('/admin/v1/user/detail'),
  // 刷新token
  // refreshToken: () => request.get('/auth/refresh/token'),
  // 登出
  logout: () => request.post('/admin/v1/logout/jwt', {}, { needTip: false }),
  // 切换当前角色
  switchCurrentRole: (role, isRemember) => request.post(`/admin/v1/auth/current-role/switch/${role}/${isRemember}`),
  // 获取角色权限
  getRolePermissions: () => request.get('/admin/v1/menu/resource/tree'),
  // 获取管理员信息
  getAdmin: () => request.get('/admin/v1/auth/admin'),
  // 验证菜单路径
  validateMenuPath: path => request.get(`/admin/v1/menu/resource/validate?path=${path}`),
}
