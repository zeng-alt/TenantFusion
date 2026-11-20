# ABAC

ABAC 只负责属性约束，调用方先完成账户、租户及基础权限校验。所有适用策略按 AND 执行；缺失属性、查询失败、解析失败或求值失败不会降级为许可。未要求规则时，合法的空规则集保留原有可选策略语义；`requirePolicy()` 或 `security.abac.required-permissions` 要求存在适用规则。

## 模块与分包

| 模块 | 包 | 职责 |
| --- | --- | --- |
| api-abac-security-component | `abac`、`abac.policy`、`abac.event` | 门面、属性上下文、纯文本查询协议和变更事件 |
| core-abac-security-component | `abac.core` | 授权执行、规则筛选、求值器和加载器 SPI |
| method-abac-security-component | `abac.method`、`abac.spel` | 方法拦截、受限 SpEL、加载阶段预解析和表达式缓存 |
| client-abac-security-component | `abac.client.cache/remote/listener/config` | 策略缓存、Feign 来源、通知刷新和默认门面装配 |
| server-abac-security-component | `abac.server.service/spi/web/event/config/security` | 权威查询、规则管理、提交后通知和查询端点 |

SpEL 与方法拦截已合并，不再发布独立的 `spel-abac-security-component`。
`core` 不创建执行门面；只启用服务端时不会强制要求客户端策略来源或方法主体 SPI。

## 同进程部署

Admin 已使用此模式：服务端查询实现直接注入客户端缓存，缓存未命中才查 JPA，不绕 HTTP。

```yaml
security:
  abac:
    server:
      enabled: true
    client:
      enabled: true
      source: local
      maximum-size: 10000
      cache-ttl: 5m
    spel:
      maximum-size: 10000
      cache-ttl: 30m
```

宿主提供 `AbacPolicyRepository` 和 `AbacPolicyStore`。Admin 的 `JpaAbacPolicyRepository`、`JpaAbacPolicyStore` 沿用原表、MapStruct 映射和数据租户校验。

管理规则通过 `AbacPolicyService.create/update/delete`，由它在写入前准备表达式、发布受影响键。`AbacPolicyStore` 是持久化 SPI；直接调用存储不会产生组件通知。规则修改归属权限时同时通知旧、新权限，删除最后一条规则会清空相应执行快照。

## 独立客户端与服务端

服务端配置 `server.enabled=true`、`client.enabled=false`，依赖 server 和 method 模块，method 提供 SpEL 校验；客户端关闭后不装配方法 Advisor。

业务客户端依赖 client 和 method 模块，配置：

```yaml
security:
  abac:
    server:
      enabled: false
    client:
      enabled: true
      source: remote
      service-id: admin-application
      client-id: workflow
      service-key: ${ABAC_WORKFLOW_SERVICE_KEY}
      connect-timeout: 3s
      read-timeout: 5s
spring:
  cloud:
    refresh:
      enabled: false
    openfeign:
      client:
        refresh-enabled: false
      lazy-attributes-resolution: false
```

远程客户端使用 OpenFeign + Spring Cloud LoadBalancer，通过注册中心按 `service-id` 查找实例；无注册中心的开发环境可使用 `spring.cloud.discovery.client.simple.instances.<service-id>[0].uri`。不配置固定 Feign URL。远程模式缺失 `client-id` 或 `service-key` 时启动失败，本地模式不需要服务凭据。Feign 命名子上下文不继承父级用户凭据拦截器，服务拦截器移除用户 `Authorization`，只发送 `X-Abac-Client`、`X-Abac-Service-Key` 与查询对应的 `X-Tenant-Id`。后台通知刷新同样使用此服务身份。

服务端登记独立的服务账户与允许租户，例如：

```yaml
security:
  abac:
    server:
      enabled: true
      clients:
        workflow:
          key: ${ABAC_WORKFLOW_SERVICE_KEY}
          tenants: [t001, t002]
    client:
      enabled: false
```

服务端查询端点为 `GET /internal/abac/policies?tenant=...&permission=...`，返回策略快照本身，未加入公开白名单。服务身份只获得此 GET 端点的读取能力，不冒充管理员或用户，不获得策略写入与其他业务接口权限。无效服务凭据返回 401；租户未授权、租户头与查询不一致返回 403。校验后建立独立租户上下文，清除旧数据库/schema/路由，让已有租户组件按该租户选择真实数据源；JPA 仓库继续检查数据租户。请求结束还原全部上下文，其他全局约束继续执行。

方法授权仍需贡献 `AbacSubjectProvider`、`AbacEnvironmentProvider` 及所需资源解析器。仅使用 `AbacTemplate` 时设置 `security.abac.method.enabled=false`。

## 缓存与预解析

策略快照按 `(tenant, permission)` 存入有界内存缓存。`revision` 是排序后策略内容的 SHA-256 指纹，表达式文本和授权阶段变化都会改变指纹，不要求新增数据库版本表。

首次加载、过期或变更刷新时，客户端检查快照作用域，调用 `AbacEvaluator.prepare`。全部准备成功才缓存快照。SpEL 实现将解析对象与结构校验结果放入有界表达式缓存；表达式按内容复用，运行时每次检查属性存在性并使用本次上下文求值，不缓存用户授权结论。表达式缓存容量应按规则总数设置；表达式被淘汰后会重新解析。

服务器错误、认证失败、空 HTTP 响应与成功的空规则集分别处理，失败不作为空规则缓存。刷新先使全部受影响键失效；刷新失败不会继续使用旧许可，后续请求重新查询。

此处的预解析是将字符串转换为可复用 `Expression`，不启用 SpEL JVM 字节码生成，保留既有受限语法和 Native 可达性。HTTP 与事件只传表达式字符串，协议模型已注册 Native 绑定提示。

方法元数据使用 `ConcurrentHashMap<MethodClassKey, AbacAuthorizeAttribute>`，首次访问统一解析实现方法、桥接方法、参数名/位置、泛型参数类型及泛型返回类型。没有注解的方法也缓存缺失结果；每次调用只把本次参数值绑定到已缓存的索引，不重复发现参数名，不缓存参数值、返回值或授权结论。返回类型元数据为类型解析与 AOT 使用，拦截器执行前置授权、业务方法、事务内后置授权。

Spring AOT 处理器在构建阶段发现授权方法，注册方法调用和参数/返回 DTO（包含泛型元素）的反射提示，并生成 `META-INF/abac/method-parameters.properties`。Native 优先读取该资源，避免依赖运行时参数名反射；JVM 回退 Spring 参数名发现器。无授权方法的 Feign 子上下文跳过资源生成。服务模式与服务名等装配选项在 AOT 构建和运行时保持一致；组件通过 `@LoadBalancerClient` 同步注册相同服务名，保证负载均衡子上下文进入 AOT。上述三个 Spring Cloud 选项是 [OpenFeign Native 的要求](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html)，显式服务名是 [LoadBalancer Native 的要求](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)。

## 变更通知

同进程通知使用提交后的本地事务事件；外层事务回滚不刷新缓存。跨进程通知复用已有 `EventTemplate` 和 `@EventSubscription`，独立开启：

```yaml
security:
  abac:
    server:
      notifications-enabled: true
      notification-binding: abac-policies
    client:
      notifications-enabled: true
      notification-binding: abac-policies
```

在宿主 `alt.event.bindings.abac-policies` 声明传输。通知必须到达每个持有本地缓存的实例：Kafka/Streams 使用每实例独立消费组，RabbitMQ 使用每实例独立绑定队列；Redis Pub/Sub 自然广播，但不补投离线消息。

事件只携带受影响的租户、权限键。客户端始终回源最新快照，重复或较旧通知不会把消息中的旧表达式覆盖到缓存。远程刷新失败进入事件组件的重试预算；发送失败由服务端记录，规则提交不反转。TTL 到期后回源用于补偿漏通知，因此一致性是有界缓存下的最终一致性，不提供即时撤权的强一致承诺。

## 定制与验证

`AbacClientCustomizer` 调整客户端缓存属性，`AbacTemplateCustomizer` 保留 required 权限配置。贡献者通过 `ObjectProvider.orderedStream()` 收集，自定义 `AbacTemplate` 时默认客户端装配退让。

```shell
./gradlew :backend:components:security-component:abac-security-component:check \
  :backend:components:security-component:abac-security-component:jacocoTestReport
```

覆盖率门槛保持每模块 80%。测试覆盖本地/远程来源、缓存与预解析复用、租户隔离、空集合与失败区分、TTL、变更刷新、提交/回滚通知，以及方法授权、并发元数据复用、Spring AOT 资源/DTO 提示和现有 JPA/H2 契约。Feign 集成测试通过真实 HTTP 及服务发现验证用户 Token 拦截器隔离。


本次另以 GraalVM CE 25.0.2（macOS ARM64）实际构建并运行独立 ABAC Native 冒烟程序，验证服务发现、服务凭据与租户头、协议 DTO 反序列化、受限 SpEL 预解析、策略缓存、CGLIB 类代理、泛型接口 JDK 代理、参数名及泛型返回类型，以及不同调用独立作出授权判断。此验证范围为 ABAC 组件；宿主应用的其他依赖仍遵循各自 Native 构建要求。

## 变量、元数据与事务内后置授权

`principal`、`resource`、`env` 和只读 `authentication` 是统一根对象；方法参数自动绑定为 `#参数名`、`#p0` / `#a0`。后置阶段提供解包后的 `returnObject`。参数与结果经缓存的 getter/record 元数据投影为不可变 JSON 树，不暴露 class、password、secret、token、credentials。不保留旧变量别名或旧协议构造器。

ABAC 统一在方法调用时执行，前置与后置阶段分别执行策略；`requirePolicy` 允许只配置后置规则。后置规则要求实际活动事务；事务 Advisor 包围 ABAC Advisor，使校验发生在方法调用后、提交前。拒绝抛异常并触发回滚；没有事务直接报错，业务方法不执行。`@Transactional` 支持写方法，`void` 返回仍会校验；事务传播与自调用遵循 Spring 代理规则，业务 `Either.left` / `Option.none` 不执行后置授权。

`AbacMethodResolver` 缓存字段目录、参数索引、泛型返回类型和事务声明；`AbacSchemaCustomizer` 可贡献宿主主体字段，资源解析器通过 `attributes()` 声明字段。保存校验与工作台补全共用目录，`AbacFunctionResolver` 可贡献明确注册的授权函数。内置支持 hasRole/hasAnyRole/hasAuthority/hasAnyAuthority/isAuthenticated/isAnonymous 以及受限字符串、集合方法，不开放任意反射调用。

工作台只列出已绑定 `@AbacAuthorize` 的方法权限。策略仅包含权限、表达式和前后置阶段，具体业务记录的约束直接写在表达式中。

Admin 的 `/v1/abac` 提供权限目录、字段定义、规则 CRUD、试算和缓存刷新。每个入口验证当前身份租户及对应管理权限，模拟 JSON 不会进入真实业务调用。前端使用 `@zeng-alt/vue-spel-query-builder` 代码编辑器和规则树；样本求值与保存由服务端完成。可运行示例和 SQL 见 `scripts/examples/abac-user-update`。
