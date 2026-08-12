/**********************************
 * @Author: Ronnie Zhang
 * @LastEditor: Ronnie Zhang
 * @LastEditTime: 2023/12/05 21:30:57
 * @Email: zclzone@outlook.com
 * Copyright © 2023 Ronnie Zhang(大脸怪) | https://isme.top
 **********************************/

import { FileSystemIconLoader } from '@iconify/utils/lib/loader/node-loaders'
import presetRemToPx from '@unocss/preset-rem-to-px'
import { defineConfig, presetAttributify, presetIcons, presetWind3 } from 'unocss'
// import { FIELD_TYPE_META } from '@/views/template/form-config/constants.js'
import { getIcons } from './build/index.js'

const icons = getIcons()
// const fieldTypeIcons = Object.values(FIELD_TYPE_META).map(meta => meta.icon)
export default defineConfig({
  presets: [
    presetWind3(),
    presetAttributify(),
    presetIcons({
      warn: true,
      prefix: ['i-'],
      extraProperties: {
        display: 'inline-block',
        width: '1em',
        height: '1em',
      },
      collections: {
        me: FileSystemIconLoader('./src/assets/icons/isme'),
        fe: FileSystemIconLoader('./src/assets/icons/feather'),
        carbon: FileSystemIconLoader('./src/assets/icons/carbon'),
        ali: FileSystemIconLoader('./src/assets/icons/alibaba'),
        fa: FileSystemIconLoader('./src/assets/icons/fa'),
        ma: FileSystemIconLoader('./src/assets/icons/material'),
      },
    }),
    presetRemToPx({ baseFontSize: 4 }),
  ],
  safelist: [...icons.map(icon => `${icon} ${icon}?mask`.split(' ')).flat()],
  shortcuts: [
    ['wh-full', 'w-full h-full'],
    ['f-c-c', 'flex justify-center items-center'],
    ['flex-col', 'flex flex-col'],
    ['card-border', 'border border-solid border-light_border dark:border-dark_border'],
    ['auto-bg', 'bg-white dark:bg-dark'],
    ['auto-bg-hover', 'hover:bg-#eaf0f1 hover:dark:bg-#1b2429'],
    ['auto-bg-highlight', 'bg-#eaf0f1 dark:bg-#1b2429'],
    ['text-highlight', 'rounded-4 px-8 py-2 auto-bg-highlight'],
  ],
  rules: [
    [
      'card-shadow',
      { 'box-shadow': '0 1px 2px -2px #00000029, 0 3px 6px #0000001f, 0 5px 12px 4px #00000017' },
    ],
  ],
  theme: {
    colors: {
      primary: 'rgba(var(--primary-color))',
      dark: '#18181c',
      light_border: '#efeff5',
      dark_border: '#2d2d30',
    },
  },
})
