/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/05 21:29:51
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { request } from '@/utils'

export default {
  create: data => request.post('/admin/v1/user', data),
  read: (params = {}) => request.get('/admin/v1/user', { params }),
  update: data => request.patch(`/admin/v1/user/${data.userId}`, data),
  delete: id => request.delete(`/admin/v1/user/${id}`),
  resetPwd: (id, data) => request.patch(`/admin/v1/user/password/reset/${id}`, data),
  logoff: id => request.get(`/admin/v1/auth/logoff/${id}`),
  getAllRoles: () => request.get('/admin/v1/role/all?enabled=true'),
  getAllDepts: () => request.get('/admin/v1/dept/tree'),
}
