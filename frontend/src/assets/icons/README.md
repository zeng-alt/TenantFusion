# 菜单图标

`menu-icons.json` 是从 [Iconify JSON 的 Lucide 图标集](https://github.com/iconify/icon-sets/blob/master/json/lucide.json)
下载并提取的 29 个图标，保留原始 SVG 数据和 ISC 授权信息。
菜单使用 `i-lucide:图标名`；`dynamic-icons.js` 将这些名字加入 UnoCSS safelist，
`uno.config.js` 加载本地 JSON，因此菜单图标随前端构建，不需要运行时请求 Iconify 服务。

29 个内置菜单的图标直接配置在
`backend/admin/src/main/resources/db/changelog/{pg,h2}/` 原有的菜单初始化 SQL 中。
