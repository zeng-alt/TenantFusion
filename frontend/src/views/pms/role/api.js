/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/05 21:29:27
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { request } from '@/utils'

export default {
  create: data => request.post('/admin/v1/role', data),
  read: (params = {}) => request.get('/admin/v1/role', { params }),
  update: data => request.patch(`/admin/v1/role/${data.roleId}`, data),
  delete: id => request.delete(`/admin/v1/role/${id}`),
  sortData: data => request.put('/admin/v1/role/sort', data),
  authorizePermission: data => request.post('/admin/v1/role/authorizePermission', data),

  getAllPermissionTree: () => request.get('/admin/v1/menu/resource/tree/all'),
  getAllUsers: (params = {}) => request.get('/admin/v1/user', { params }),
  addRoleUsers: (roleId, data) => request.patch(`/admin/v1/role/users/add/${roleId}`, data),
  removeRoleUsers: (roleId, data) => request.patch(`/admin/v1/role/users/remove/${roleId}`, data),
}
