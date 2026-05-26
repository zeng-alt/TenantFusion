# 项目规则文档

## 项目概览

这是一个开发中台项目，主要管理 RBAC、ABAC、用户、租户、流程、动态表单、文档模板、国际化管理的系统。

### 技术栈

#### 后端
- **语言**: Java 21+
- **框架**: Spring Boot 3.5.13
- **构建工具**: Gradle（Kotlin DSL）+ GraalVM Native Image 0.10.6
- **架构**: Spring Modulith（模块化单体架构）
- **ORM**: Spring Data JPA + Hibernate ORM 6.6.45.Final
- **安全**: Spring Security
- **校验**: Spring Validation
- **数据库**: PostgreSQL（生产）、H2（开发/测试）
- **缓存**: Redis（Spring Data Redis）
- **数据库迁移**: Liquibase（开发环境）
- **容器支持**: Docker Compose（开发环境）
- **工具库**: Lombok、Vavr、MapStruct、SLF4J+Logback、Apache Commons Lang3
- **基础包**: `com.github.zeng.alt`
- **作者**: zengJiaJun

#### 前端
- **语言**: JavaScript / Vue 3（Composition API + `<script setup>`）
- **构建工具**: Vite 7
- **UI 框架**: Naive UI 2.43
- **CSS**: UnoCSS（原子化 CSS）
- **状态管理**: Pinia
- **路由**: Vue Router 5
- **HTTP 客户端**: Axios
- **图表**: ECharts + vue-echarts
- **工具库**: VueUse、Lodash-es、Day.js、XLSX


## 后端规则

### 模块结构

项目采用 Gradle 多模块 + Spring Modulith 架构：

```
backend/
├── admin/                          # 启动模块（Spring Boot Application）
├── components/                     # 组件父模块
│   ├── api-component/              # API 抽象层（响应体、异常、分页）
│   ├── bean-component/             # Bean 工具（BeanHelper、ApplicationContextHelper）
│   ├── core-component/             # 核心模块（全局异常处理）
│   ├── domain-component/           # 领域模块（暂空）
│   ├── security-component/         # 安全模块（暂空）
│   ├── storage-component/          # 存储模块（暂空）
│   └── tenant-component/           # 租户模块（暂空）
```

### 模块依赖链

```
api-component (无依赖)
    ↓
bean-component (依赖 api-component)
    ↓
core-component (依赖 bean-component)
    ↓
domain-component / security-component / storage-component / tenant-component
    ↓
admin (聚合所有模块)
```


### 命名规范

#### 包命名
```
com.github.zeng.alt.{module}           # 组件模块
com.github.zeng.alt.admin              # Admin 启动模块
com.github.zeng.alt.api.{subpackage}   # API 组件
com.github.zeng.alt.bean               # Bean 组件
com.github.zeng.alt.core               # 核心组件
```

#### 类命名
- **Controller**: `XxxController`，统一放在 controller 包中
- **Service**: `XxxService`（接口），`XxxServiceImpl`（实现），统一放在 service 包中
- **Repository**: `XxxRepository`（Spring Data JPA），统一放在 repository 包中
- **Entity**: `XxxDO`（数据对象），统一放在 entity 包中
- **DTO**: `XxxDTO` / `XxxCmd`（命令）/ `XxxQry`（查询）
- **Converter**: `XxxConverter`（MapStruct 映射接口），统一放在 converter/mapstruct 包中
- **Exception**: 继承 `BaseException` 或 `BaseI18nException`
- **Config**: `XxxConfiguration` / `XxxProperties`


### 代码规范

#### 1. 响应体规范
- **统一响应**: 普通接口返回 `RestResponse<T>`，使用静态工厂方法构建
  - `RestResponse.success(data)` - 成功
  - `RestResponse.fail(message)` - 失败
  - `RestResponse.warn(message)` - 警告
- **分页响应**: 使用 `PageRestResponse<T>` 或 `PageResponseEntity<T>`
- **树形响应**: 使用 `TreeRestResponse<T, P>`、`TreeRestResponseEntity<T, P>`、`TreeTableRestResponse<T, P>`
- **翻页响应**: 使用 `TurnPageRestResponse<T, C>`、`TurnPageResponseEntity<T, C>`
- **错误响应**: 使用 `ErrorResponseEntity`（基于 RFC 9457 Problem Details）
- **Controller 返回**（`GlobalResponseAdvice` 自动包装）:
  - `"ok"` → `RestResponse.success()`
  - `"ok:{message}"` → `RestResponse.success().setMessage(message)`
  - `"fail:{message}"` → `ErrorResponseEntity`
  - `HttpEntity` 子类 → 不经过包装

#### 2. 异常处理规范
- **业务异常**: 继承 `BaseException`（带 code、title、message）
- **国际化异常**: 使用 `BaseI18nException`
- **工具异常**: 使用 `UtilException`
- 全局异常由 `GlobalExceptionAdvice` + `GlobalServletExceptionAdvice` 统一处理
- 异常返回 `ErrorResponseEntity`（ProblemDetail 格式）

#### 3. Bean 操作规范
- **属性拷贝**: 优先使用 `converter.convert()`（MapStruct），通过 `BaseController` 注入
- **Bean 创建/拷贝**: 使用 `BeanHelper`
  - `copyToObject(source, targetClz)` / `copyToObject(source, targetClz, consumer)`
  - `copyToList(source, targetClz)` - 批量拷贝（列表推荐 MapStruct）
  - `copyPropertiesIgnoringNull(source, target)` - 忽略 null 拷贝
  - `instantiateBean(clazz)` - 创建实例（支持 Record 类）
  - `copyToOptionObject(source, targetClz)` - 返回 `Option<T>`
- **Bean 注册/获取**: 使用 `ApplicationContextHelper`
  - `getBean(clazz)` / `registerBean(name, bean)` / `publisher()`

#### 4. 控制器规范
- 所有 Controller 继承 `BaseController`，提供：
  - `converter`（MapStruct Converter 注入）
  - `convert(S, Class<T>)` - 对象转换
  - `beanConvert(S)` - Bean 拷贝转换
  - `convertRun(S, Class<T>)` - 转换后执行消费

#### 5. 分页规范
- **查询参数**: 继承 `PageQuery`（page/size/orderByColumn/isAsc）或 `TurnPageQuery<T>`（size/current）
- **分页实体**: `PageEntity<T>`（pageNum/pageSize/total/data）
- **翻页实体**: `TurnPageEntity<T, C>`（hasNext/hasPre/currentCursor/nextCursor/data）

#### 6. 树形结构规范
- 实现 `Parent<P>` 接口（`parent()` 返回父 ID，`current()` 返回当前 ID）
- `isRoot()` 默认判断 `parent() == null`
- 使用 `TreeRestResponse.apply(data)` / `TreeTableRestResponse.apply(data)` 构建树形响应


#### 7. 配置规范
- 使用 YAML 格式配置文件
- 开发环境：`application-dev.yml`（H2 内存库）
- 生产环境：`application-prod.yml`（PostgreSQL）
- 激活配置：`spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}`
- 支持通过 `-Dspring.profiles.active` 或 `-Pprofiles.active` 切换

#### 8. 注释规范
```java
/**
 * @author zengJiaJun
 * @since 2026年01月01日
 * @version 1.0
 */
```
或
```java
/**
 * @author zengJiaJun
 * @crateTime 2024年06月16日 20:19
 * @version 1.0
 */
```

#### 9. 模块设计规范
- 使用 Spring Modulith 确保模块边界
- 模块间通过 API 层交互，避免循环依赖
- 每个组件模块应定义清晰的 module-info 或使用 `@NamedInterface` 标注暴露的包

#### 10. GraalVM Native Image 规范
- 使用 `graalvmNative { binaries { named("main") { buildArgs } } }` 配置
- 已配置：`--no-fallback`、`--install-exit-handlers`、`--enable-url-protocols=http,https`
- 字符编码：`-Dfile.encoding=UTF-8`、`-Duser.country=CN`、`-Duser.language=zh`
- 启用 Metadata Repository（v0.3.15）

## 前端规则

### 目录结构

```
frontend/src/
├── api/              # API 封装
├── assets/           # 静态资源（图标、图片）
├── components/       # 公共组件
│   ├── common/       # 通用组件（AppCard、AppPage、CommonPage 等）
│   └── me/           # 中台专用组件
│       ├── crud/     # CRUD 组件
│       └── modal/    # 模态框组件
├── composables/      # 组合式函数（useCrud、useForm、useModal、useAliveData）
├── directives/       # 自定义指令
├── layouts/          # 布局组件（empty/full/normal/simple）
├── router/           # 路由配置 + 路由守卫
├── store/            # 状态管理（Pinia modules）
├── styles/           # 全局样式
├── utils/            # 工具函数（http、storage）
└── views/            # 页面（home/login/pms/profile 等）
```

### 代码规范

#### 1. Vue 组件规范
- 使用 **Composition API** + **`<script setup>`** 语法
- 文件名使用 **kebab-case**（如 `common-page.vue`）
- 组件名使用 **PascalCase**
- 模板中使用 **PascalCase** 组件标签（如 `<CommonPage />`）

#### 2. 状态管理
- 使用 **Pinia** + `defineStore` 定义 Store
- Store 文件放在 `store/modules/` 目录下
- 持久化使用 `pinia-plugin-persistedstate`

#### 3. API 封装
- 统一使用 `@/utils/http` 封装的 Axios 实例
- API 文件放在 `api/` 目录下，按模块拆分

#### 4. 路由规范
- 基本路由定义在 `router/basic-routes.js`
- 路由守卫在 `router/guards/` 中定义
- 支持：page-loading-guard、page-title-guard、permission-guard、tab-guard

#### 5. 样式规范
- 优先使用 **UnoCSS** 原子化类名
- 全局样式在 `styles/global.css` 和 `styles/reset.css` 中定义
- 支持深色模式（`dark` class）

#### 6. 组合式函数
- 通用组合式逻辑放在 `composables/` 目录
- 已封装的组合式函数：`useCrud`、`useForm`、`useModal`、`useAliveData`

#### 7. HTTP 请求规范
- 使用 `request` 实例（基于 Axios），拦截器在 `utils/http/interceptors.js`
- 支持自动 token 刷新

## 通用规则

### Git 提交规范
- 使用 `git-commit` 工具自动生成规范化提交信息
- 遵循约定式提交（Conventional Commits）格式

### 开发流程
1. **后端开发**: `cd backend/admin && ../../gradlew bootRun`（或通过 IDE）
2. **前端开发**: `cd frontend && pnpm dev`
3. **生产构建**: `./gradlew build`（自动构建前端并复制到后端静态资源目录）
4. **Native Image 构建**: `./gradlew nativeCompile`
5. **测试**: `./gradlew test`（JUnit 5 + Spring Modulith test）

### 项目配置要求
- **JDK**: 21+
- **Node.js**: 20+
- **包管理器**: pnpm
- **IDE**: 推荐 IntelliJ IDEA + Vue.js 插件
