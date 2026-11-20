# RBAC Security 模块使用文档

## 概述

RBAC Security 模块提供了基于角色的访问控制（Role-Based Access Control）能力，包含 **服务端（Serve）** 和 **客户端（Client）** 两个子模块：

| 模块 | 职责 |
|------|------|
| `rbac-serve-security-component` | 请求鉴权中心。接收请求、匹配路由、查询权限、做出授权决策 |
| `rbac-client-security-component` | 路由收集器。自动收集业务服务的 `@RequestMapping` 路由模板，注册到服务端 |

配合以下模块协同工作：

| 模块 | 职责 |
|------|------|
| `api-security-component` | SPI 接口层。定义 `Resource`、`SecurityUser`、`AuthorizationManagerProvider` 等核心类型 |
| `core-security-component` | 核心安全配置。提供 `SecurityFilterChain`、`CompositeAuthorizationManager`、白名单等 |

---

## 快速开始

### 1. 添加依赖

#### Servlet 应用（Spring MVC）

```kotlin
// build.gradle.kts
dependencies {
    // RBAC 服务端（鉴权）
    implementation(project(":backend:components:security-component:rbac-security-component:rbac-serve-security-component"))
    // RBAC 客户端（路由收集，可选）
    implementation(project(":backend:components:security-component:rbac-security-component:rbac-client-security-component"))
}
```

#### Reactive 应用（Spring WebFlux）

依赖同上，无需额外配置。模块会根据 `@ConditionalOnWebApplication` 自动选择 Servlet 或 Reactive 分支。

#### 单体模式（Monolithic）

Client 和 Serve 在同一个 JVM 中，路由模板直接**进程内**注册到 `RouteTemplateManager`。

```kotlin
dependencies {
    implementation(project(":backend:components:security-component:rbac-security-component:rbac-serve-security-component"))
    implementation(project(":backend:components:security-component:rbac-security-component:rbac-client-security-component"))
}
```

#### 微服务模式（Microservices）

Client 和 Serve 在不同进程。Client 通过消息队列发送 `RouteTemplateEvent`，Serve 通过 `@EventSubscription` 接收。

**Client 侧：**
```kotlin
dependencies {
    implementation(project(":backend:components:security-component:rbac-security-component:rbac-client-security-component"))
    implementation(project(":backend:components:event-component:redis-event-component")) // 或 kafka / rabbit
    // 不引入 rbac-serve-security-component
}
```

**Serve 侧：**
```kotlin
dependencies {
    implementation(project(":backend:components:security-component:rbac-security-component:rbac-serve-security-component"))
    implementation(project(":backend:components:event-component:redis-event-component")) // 与 Client 一致的消息实现
}
```

### 2. 配置

```yaml
# application.yml
security:
  context:
    enabled-access: true    # 启用 RBAC 鉴权（默认 true）
  whitelist:
    permit-all:             # 匿名访问，省略 method 时匹配所有方法
      - path: /public/**
      - method: GET
        path: /api/open/**
    authenticated:          # 登录即可，不要求 HTTP RBAC / ABAC 权限
      - method: GET
        path: /api/profile

rbac:
  client:
    enabled: true           # 启用路由注册（默认 true）
    context-path: ""        # 上下文路径前缀
```

两组白名单按 `permit-all` → `authenticated` → 普通鉴权的顺序分类。登录规则命中后的拒绝是最终结果，
不会继续尝试 OR Provider；两组中完全相同的方法与路径会在启动时报告冲突。普通规则保留既有
RBAC、管理员及专用服务身份授权逻辑，无 Provider 时仍回退到登录检查。

白名单只豁免 HTTP 权限检查：认证过滤器、其他全局 `AuthorizationConstraint`、方法级授权和数据权限继续执行。
admin 的 HTTP ABAC 约束对公开请求跳过权限检查，对登录白名单只执行用户有效性和租户一致性检查。

`security.context.admin.enabled=true` 时，用户 ID 匹配 `admin.id`，或当前角色代码匹配 `admin.code`
（代码忽略大小写），即按管理员处理；用户名叫 `admin` 本身不构成豁免。admin 宿主先检查有效登录用户及
认证租户与当前数据租户一致，再豁免管理员的 HTTP RBAC / ABAC 权限检查，不要求接口已有 HTTP 权限记录。
该豁免不跳过账户认证、租户隔离、其他全局约束、方法级 `@AbacAuthorize` 或数据权限；关闭管理员开关后走普通授权。

业务校验通过 `AuthHelper.hasAdmin()` 判断当前身份是否具备管理员及以上权限：管理员开关开启时，
配置的内置超级用户 ID 或当前管理员角色任一匹配即可。内置超级用户没有绑定角色时仍可管理默认字典类型和字典数据。
`hasAdminRole(code)` 与 `hasSuperUser(id)` 用于保护被修改的内置角色、用户；只匹配目标身份，
不读取当前操作者，也不受管理员豁免开关影响。角色码与用户 ID 分别匹配，不能交叉比较。

通用校验提供 `hasRole(code)`、`hasAnyRole(codes...)`、`hasAuthority(code)`、`hasAnyAuthority(codes...)`。
角色沿用项目原有代码（如 `ADMIN`），不自动添加 `ROLE_` 前缀；已选择角色时只检查当前角色，未选择时检查已授予角色。
权限 code 精确匹配当前角色的 RBAC 权限，复用既有 `PermissionLocator`；没有 RBAC 贡献者时匹配认证对象中的
`GrantedAuthority`。其他权限源可贡献 `AuthorityResolver` Bean，组件按顺序查询。通用校验不隐式套用管理员豁免，
需要管理员及以上权限的业务入口显式调用 `hasAdmin()`。

代码规则通过窄粒度 customizer 贡献，组件按 `@Order` 收集，贡献者规则先于配置默认规则应用：

```java
@Bean
public WhiteListCustomizer whiteList() {
    return rules -> rules
            .permitAll(HttpMethod.GET, "/module/public")
            .authenticated(HttpMethod.PATCH, "/module/profile/{id}");
}
```

旧 `security.filter.ignore-url` 继续按所有方法的公开路径兼容，与新配置合并。已有
`WhiteListService` lambda 保持兼容，`getWhiteList()` 提供旧公开路径；自定义规则源可进一步实现
`getPermitAll()` / `getAuthenticated()` 返回带方法的规则。动态规则变更在下一次请求生效，路径只在规则变化时重新编译。

### 3. 提供 UserDetailsService

RBAC 需要从认证信息中提取用户名、租户、角色等。实现 `UserDetailsService` 并返回 `SecurityUser`：

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) {
        // 查询用户、角色、租户信息
        SecurityUser user = new SecurityUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("xxx"));
        user.setTenant("tenant_001");
        user.setRoles(Set.of(new RoleGrantedAuthority("ROLE_admin")));
        return user;
    }
}
```

### 4. 填充权限数据

`DefaultRbacResourceService` 使用 `StorageTemplate` 存储权限数据，需预先填充三种数据：

#### 用户资源列表

存储该用户可访问的 HTTP 资源列表。

```
Key:   rbac:resources:{tenant}:{username}
Value: List<HttpResource> (JSON)
```

```json
[
  {"uri": "/api/users/**", "method": "GET"},
  {"uri": "/api/orders/**", "method": "POST"}
]
```

#### 资源权限映射

每个资源对应的权限标识。

```
Key:   rbac:permission:{tenant}:{uri:method}
Value: String (权限编码)
```

```
rbac:permission:tenant_001:/api/users/**\:GET → user:list
rbac:permission:tenant_001:/api/orders/**\:POST → order:create
```

#### 角色权限集合

每个角色拥有的权限标识集合。

```
Key:   rbac:role:permissions:{tenant}:{roleAuthority}
Value: Set<String> (JSON)
```

```
rbac:role:permissions:tenant_001:ROLE_admin → ["user:list", "order:create", "report:view"]
```

> 三种 Key 均可通过 `DefaultRbacResourceService` 的 setter 方法写入：`setResources()`、`setPermissionForResource()`、`setPermissionsForRole()`。

---

## 架构

### 整体结构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Client (业务服务)                             │
│                                                                     │
│  Application Startup                                                │
│       │                                                             │
│       ▼                                                             │
│  RouteTemplateRegistrar (SmartInitializingSingleton)                │
│       │                                                             │
│       ├── RouteTemplateCollector.collectTemplates()                 │
│       │       └── 遍历所有 @RequestMapping 提取 pattern              │
│       │                                                             │
│       └── doRegister(contextPath, templates)                        │
│               │                                                     │
│               ├── 单体模式 ─────────────────────────────────────┐   │
│               │   DirectRouteTemplateRegistrar                  │   │
│               │   → routeTemplateManager.addRouteTemplate()     │   │
│               │                                                 │   │
│               └── 微服务模式                                     │   │
│                   MessageRouteTemplateRegistrar                 │   │
│                   → eventTemplate.to("route-template").publish(event)     │   │
│                                                                 │   │
└─────────────────────────────────────────────────────────────────┘   │
                                                                      │
          ┌───────────────────────────────────────────────────────────┘
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Serve (鉴权中心)                              │
│                                                                     │
│  RouteTemplateEventListener (@EventSubscription)                             │
│       │                                                             │
│       ├── onMessage(event)                                          │
│       │   → RouteTemplateTrie.insert(template)                      │
│       │                                                             │
│  HTTP Request                                                       │
│       │                                                             │
│       ▼                                                             │
│  WhiteListAuthorizationManager                                      │
│       │                                                             │
│       ├── permit-all → 公开访问                                     │
│       ├── authenticated → 必须登录，拒绝后不再尝试 Provider           │
│       └── 普通接口 → CompositeAuthorizationManager                  │
│       │                                                             │
│       ├── RbacAccessAuthorizationManager (Servlet) /                │
│       │    ReactiveRbacAccessAuthorizationManager (Reactive)        │
│       │       │                                                     │
│       │       ├── ParseManager.parse(request)                       │
│       │       │   → 匹配 ResourceHandler → HttpResourceHandler       │
│       │       │                                                     │
│       │       └── HttpResourceHandler.handler()                     │
│       │           → ResourceQueryManager.query()                    │
│       │           → HttpResourceLocator.load()                      │
│       │           → RbacResourceService.findAllHttpResource()       │
│       │           → storageTemplate.get("rbac:resources:{t}:{u}")   │
│       │           → resource.compareTo(request) 匹配 → 放行          │
│       │                                                             │
│       └── AuthenticatedAuthorizationManager (无 Provider 时)        │
│               → 仅检查是否已认证                                    │
└─────────────────────────────────────────────────────────────────────┘
```

### Servlet 请求鉴权流程

```
请求 → SecurityFilterChain
         → WhiteListAuthorizationManager
             ├─ permit-all                         公开访问
             ├─ authenticated                      登录检查，结果终结
             └─ 普通接口
         → CompositeAuthorizationManager
             ├─ RbacAccessAuthorizationManager      RBAC 鉴权
             │   ├─ ParseManager.parse(request)     匹配 ResourceHandler
             │   │   └─ HttpResourceHandler         默认 fallback
             │   └─ handler(authentication, object)
             │       ├─ 提取 URI + Method → HttpResource
             │       ├─ ResourceQueryManager.query()
             │       │   └─ HttpResourceLocator.load()
             │       │       └─ RbacResourceService.findAllHttpResource()
             │       │           → 查 StorageTemplate 获取用户资源列表
             │       └─ 遍历资源，compareTo(request) 匹配 → 放行
             └─ (无 Provider 时) AuthenticatedAuthorizationManager
                     → 仅检查认证状态
```

### Reactive 请求鉴权流程

```
请求 → SecurityWebFilterChain
         → ReactiveWhiteListAuthorizationManager
             ├─ permit-all                                公开访问
             ├─ authenticated                             登录检查，结果终结
             └─ 普通接口
         → CompositeReactiveAuthorizationManager
             ├─ ReactiveAdminAuthorizationManager @Order(5)
             │   └─ 匹配配置的管理员 ID / 当前角色代码 → 放行
             ├─ ReactiveRbacAccessAuthorizationManager @Order(10)
             │   ├─ ReactiveParseManager.parse(exchange)
             │   │   └─ ReactiveHttpResourceHandler        默认 fallback
             │   └─ handler(authentication, object)
             │       ├─ RouteTemplateManager.match(path)   路径模板匹配
             │       ├─ ReactiveResourceQueryManager.queryPermissionForResource()
             │       │   └─ RbacResourceService.findPermissionByResource()
             │       ├─ ReactivePermissionLocator.load()
             │       │   └─ RbacResourceService.findPermission()
             │       └─ 用户权限 contains 所需权限？→ 放行
             └─ (无 Provider 时) AuthenticatedReactiveAuthorizationManager
```

---

## 配置参考

### 全部配置项

```yaml
# core-security-component
security:
  context:
    enabled-access: true                    # 鉴权总开关（默认 true）
    abac-prefix: "/"                        # ABAC 路径前缀（默认 "/"）
    admin:
      id: "1001"
      code: "superAdmin"
      name: "超级管理员"
      enabled: true

  whitelist:
    permit-all:
      - path: /public/**
      - path: /h2-console/**
    authenticated:
      - method: GET
        path: /api/profile

  username-login:                           # 用户名密码登录配置
    enabled: true
    username-parameter: "username"
    password-parameter: "password"

# rbac-client-security-component
rbac:
  client:
    enabled: true                           # 路由注册开关
    context-path: ""                        # 上下文路径前缀
```

---

## 扩展点

### 自定义 AuthorizationManager

实现 `AuthorizationManagerProvider`（Servlet）或 `ReactiveAuthorizationManagerProvider`（Reactive），声明为 `@Bean` + `@Order`：

```java
@Bean
@Order(15)
public AuthorizationManagerProvider<RequestAuthorizationContext> myAuthProvider() {
    return () -> (supplier, object) -> {
        // 自定义鉴权逻辑
        return new AuthorizationDecision(true);
    };
}
```

### 自定义 ResourceLocator

实现 `ResourceLocator`（Servlet）或 `ReactiveResourceLocator`（Reactive）：

```java
@Component
public class MyResourceLocator extends AbstractResourceLocator {
    @Override
    protected List<Resource> list(Object principal) {
        // 从自定义数据源加载用户资源
        return List.of(new HttpResource("/api/my/**", "GET"));
    }

    @Override
    public boolean supports(Class<?> resource) {
        return HttpResource.class.isAssignableFrom(resource);
    }
}
```

### 自定义 ResourceHandler

实现 `ResourceHandler`（Servlet）或 `ReactiveResourceHandler`（Reactive），用于自定义路由匹配逻辑：

```java
@Component
@Order(1)
public class GraphqlResourceHandler implements ResourceHandler {
    @Override
    public boolean matcher(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/graphql");
    }

    @Override
    public Boolean handler(Authentication authentication, RequestAuthorizationContext object) {
        // 自定义 GraphQL 鉴权
        return true;
    }
}
```

### 自定义 RbacResourceService

实现 `RbacResourceService` 接口替换默认的 `DefaultRbacResourceService`（基于 StorageTemplate）：

```java
@Bean
public RbacResourceService myRbacResourceService() {
    return new RbacResourceService() {
        @Override
        public List<Resource> findAllHttpResource(String username, String tenantName, List<String> authorities) {
            // 从数据库查询用户资源
        }
        @Override
        public String findPermissionByResource(String tenantName, String resourceKey) {
            // 查询资源所需权限
        }
        @Override
        public Set<String> findPermission(List<String> authorities, String tenantName) {
            // 查询角色拥有的权限
        }
    };
}
```

### 自定义 LoginHelper

实现登录方式（JWT、Cookie、LDAP 等）：

```java
@Component
public class MyLoginHelper implements LoginHelper {
    @Override
    public String name() { return "myLogin"; }

    @Override
    public LoginResponse login(HttpServletRequest request) {
        // 登录逻辑
        return LoginResponse.success(...);
    }

    @Override
    public void logout() { }

    @Override
    public LoginResponse getCurrentUser() { return null; }
}
```

### 自定义 SecurityBuilderCustomizer

对 Spring Security 的 `HttpSecurity` 做额外定制：

```java
@Bean
public SecurityBuilderCustomizer myCustomizer() {
    return http -> http
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(c -> c.configurationSource(myCorsSource()));
}
```

---

## Actuator 端点

启用 `rbac-client-security-component` 且 classpath 包含 `spring-boot-actuator` 时，自动注册：

```
POST /actuator/rbac
```

触发 `RouteTemplateRegistrar.reRegister()`，重新收集并注册所有路由模板。适用于运行时新增/修改 `@RequestMapping` 后刷新路由表。

---

## 单体 vs 微服务模式对照

| 维度 | 单体模式 | 微服务模式 |
|------|----------|------------|
| Client & Serve | 同一 JVM | 不同 JVM |
| 路由注册 | `DirectRouteTemplateRegistrar` 直接调用 | `MessageRouteTemplateRegistrar` 发消息 |
| Serve 接收 | 同一 `RouteTemplateManager` 实例 | 通过 `@EventSubscription` 接收消息 |
| 自动选择条件 | Classpath 有 `RouteTemplateManager` | Classpath 无 `RouteTemplateManager`，有 `EventTemplate` |
| 数据存储 | 共享 `StorageTemplate` | 需共享存储（Redis/DB） |

---

## Native Image 支持

两个模块均支持 GraalVM Native Image 编译：

- 每个模块包含 `RuntimeHintsRegistrar` 实现（`RbacServeRuntimeHints.java` / `RbacClientRuntimeHints.java`）
- Auto-configuration 类标注 `@ImportRuntimeHints`
- 注册所有模块类的反射 hint（`INTROSPECT_DECLARED_METHODS` + `DECLARED_FIELDS` + `INVOKE_DECLARED_CONSTRUCTORS` + `INVOKE_DECLARED_METHODS`）

---

## 模块依赖关系

```
rbac-client-security-component
  ├── api-security-component (SPI)
  ├── core-security-component (SecurityFilterChain)
  ├── api-event-component (EventTemplate, compileOnly)
  ├── rbac-serve-security-component (RouteTemplateManager, compileOnly)
  └── spring-boot-actuator (compileOnly)

rbac-serve-security-component
  ├── api-security-component (SPI)
  ├── core-security-component (AuthorizationManagerProvider)
  ├── api-storage-component (StorageTemplate)
  ├── api-event-component (EventTemplate)
  └── json-component
```

`core-security-component` 依赖 `api-security-component`，引入时自动装载 `SecurityFilterChain` / `SecurityWebFilterChain` 和 `CompositeAuthorizationManager`。

---

## 常见问题

**Q: 为什么不走 URL 直接匹配而要维护 RouteTemplateTrie？**

支持路径变量（Path Variable）路由。`/users/{id}` 在 HTTP 请求到来时为 `/users/123`，Trie 能将其匹配回 `/users/{id}` 模板，从而查询资源权限数据。

**Q: Reactive 和 Servlet 的鉴权逻辑为何不同？**

Servlet 端遍历用户资源列表逐一调用 `compareTo(request)` 匹配；Reactive 端则先通过 `RouteTemplateManager.match(path)` 将实际路径归一化为模板，再查该模板所需的权限标识，最后比对用户权限集合。前者按资源匹配，后者按权限标识匹配。

**Q: 如何禁用默认的 InMemoryUserDetailsManager？**

提供自定义 `UserDetailsService` bean 即可自动替换：

```java
@Bean
public UserDetailsService userDetailsService() {
    return username -> { ... };
}
```

微服务路由事件通过 `alt.event.bindings.route-template` 配置。Serve 配置该 binding 后注册 `rbac-route-template` 监听端点，本地 RouteTemplateManager 不再直接持有传输模板。
