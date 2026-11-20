/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/04 22:50:49
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

// 需要动态渲染的iconify图标，以i-开头
import menuIcons from './menu-icons.json'

export default [
  'i-simple-icons:juejin',
  ...Object.keys(menuIcons.icons).map(name => `i-${menuIcons.prefix}:${name}`),
]
