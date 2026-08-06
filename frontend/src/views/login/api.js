/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/05 21:28:30
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { request } from '@/utils'

export default {
  captcha: () => request.get('/v1/admin/auth/captcha', { needToken: false }),
  login: (data, config) => request.post('/v1/admin/login/jwt', data, { needToken: false, ...config }),
  getUser: () => request.get('/v1/admin/user/detail'),
}
