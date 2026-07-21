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

Client 和 Serve 在不同进程。Client 通过消息队列发送 `RouteTemplateEvent`，Serve 通过 `MessageListener` 接收。

**Client 侧：**
```kotlin
dependencies {
    implementation(project(":backend:components:security-component:rbac-security-component:rbac-client-security-component"))
    implementation(project(":backend:components:message-component:redis-message-component")) // 或 kafka / rabbit
    // 不引入 rbac-serve-security-component
}
```

**Serve 侧：**
```kotlin
dependencies {
    implementation(project(":backend:components:security-component:rbac-security-component:rbac-serve-security-component"))
    implementation(project(":backend:components:message-component:redis-message-component")) // 与 Client 一致的消息实现
}
```

### 2. 配置

```yaml
# application.yml
security:
  context:
    enabled-access: true    # 启用 RBAC 鉴权（默认 true）
  filter:
    ignore-url:             # 白名单路径（跳过鉴权）
      - /public/**
      - /api/open/**

rbac:
  client:
    enabled: true           # 启用路由注册（默认 true）
    context-path: ""        # 上下文路径前缀
```

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
│                   → messageQueueTemplate.send(topic, event)     │   │
│                                                                 │   │
└─────────────────────────────────────────────────────────────────┘   │
                                                                      │
          ┌───────────────────────────────────────────────────────────┘
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Serve (鉴权中心)                              │
│                                                                     │
│  RouteTemplateManager (MessageListener)                             │
│       │                                                             │
│       ├── onMessage(event)                                          │
│       │   → RouteTemplateTrie.insert(template)                      │
│       │                                                             │
│  HTTP Request                                                       │
│       │                                                             │
│       ▼                                                             │
│  CompositeAuthorizationManager                                      │
│       │                                                             │
│       ├── 1. WhiteListAuthorizationManager                          │
│       │       → 检查白名单，命中则放行                                 │
│       │                                                             │
│       ├── 2. RbacAccessAuthorizationManager (Servlet) /             │
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
│       └── 3. AuthenticatedAuthorizationManager (fallback)           │
│               → 仅检查是否已认证                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### Servlet 请求鉴权流程

```
请求 → SecurityFilterChain
         → CompositeAuthorizationManager
             ├─ WhiteListAuthorizationManager       命中白名单？→ 放行
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
         → CompositeReactiveAuthorizationManager
             ├─ ReactiveWhiteListAuthorizationManager      白名单
             ├─ ReactiveAdminAuthorizationManager @Order(5)
             │   └─ username == "superAdmin" → 放行
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

  filter:
    ignore-url:                             # 白名单路径列表
      - /public/**
      - /h2-console/**

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
| Serve 接收 | 同一 `RouteTemplateManager` 实例 | 通过 `MessageListener` 接收消息 |
| 自动选择条件 | Classpath 有 `RouteTemplateManager` | Classpath 无 `RouteTemplateManager`，有 `MessageQueueTemplate` |
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
  ├── api-message-component (MessageQueueTemplate, compileOnly)
  ├── rbac-serve-security-component (RouteTemplateManager, compileOnly)
  └── spring-boot-actuator (compileOnly)

rbac-serve-security-component
  ├── api-security-component (SPI)
  ├── core-security-component (AuthorizationManagerProvider)
  ├── api-storage-component (StorageTemplate)
  ├── api-message-component (MessageQueueTemplate)
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
