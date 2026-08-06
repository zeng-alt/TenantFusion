/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/05 21:30:03
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { request } from '@/utils'

export default {
  changePassword: data => request.post('/admin/v1/auth/password', data),
  updateProfile: data => request.patch(`/admin/v1/user/profile/${data.id}`, data),
}
