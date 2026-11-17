# 日志模块 (log-component)

通过 `@Log` 记录方法执行结果。核心模块采集元数据并发布本地事件；JPA 和消息模块按需引入。

| 模块 | 职责 |
|---|---|
| `api-log-component` | `@Log`、操作日志事件、登录日志事件和枚举 |
| `core-log-component` | AOP、Jackson 3 字段过滤、请求与身份解析、事件发布 |
| `jpa-log-component` | MapStruct 映射及独立事务持久化 |
| `ip2region-log-component` | 可选的 IPv4/IPv6 离线 IP 归属地查询 |
| `message-log-component` | 通过 event-component 发送或接收操作与登录日志 |

## 引入依赖

```kotlin
implementation(project(":backend:components:log-component:core-log-component"))
// 本地持久化按需引入
implementation(project(":backend:components:log-component:jpa-log-component"))
```

核心模块自行注册 Advisor 代理基础设施，无需为了启用日志而引入 JPA。Servlet、非 Web 和 WebFlux 应用均可启动。
Servlet 应用自动采集 URI、HTTP 方法、IP 和 query/form 参数；其他环境采集方法参数及当前线程的身份、租户上下文。
WebFlux 的装配兼容不代表订阅完成日志：当前 `@Log` 记录同步方法的返回或抛错，不跟踪 Publisher 的异步完成状态。

## 使用注解

```java
@Log(title = "更新用户", businessType = BusinessType.UPDATE,
     excludeParamNames = {"privateField"}, isSaveResponseData = false)
public String update(UserCmd command) {
    return "ok";
}
```

支持在接口或实现方法上声明注解，兼容 JDK 代理和类代理。通过 Spring Bean 代理调用才会拦截，同类自调用沿用 Spring AOP 的语义。

| 参数 | 默认值 | 说明 |
|---|---|---|
| `title` | `""` | 操作名称 |
| `businessType` | `OTHER` | INSERT、UPDATE、DELETE、GRANT、EXPORT、IMPORT 等 |
| `operatorType` | `MANAGE` | 操作者类型 |
| `isSaveRequestData` | `true` | 是否记录请求与方法参数 |
| `isSaveResponseData` | `true` | 是否记录返回值；与请求开关独立 |
| `excludeParamNames` | `{}` | 本次操作额外排除的字段名，作用于请求和响应 |

## Jackson 3 与字段过滤

默认参数、结果序列化使用 Boot 4 的 `tools.jackson.databind.json.JsonMapper`，复用 json-component 的日期、大数和业务注解规则。宿主可贡献 `JsonMapperBuilderCustomizer` 或 Jackson 3 Module Bean。

请求参数记录为 JSON 对象：包含 query/form 参数和按实现方法参数名绑定的实参，二者同时保留。多值 HTTP 参数输出为数组。Servlet 请求/响应、上传文件、流和 Publisher 等框架参数不参与序列化。

字段过滤作用于嵌套对象及数组，按字段名忽略大小写匹配。默认排除 `password`、`oldPassword`、`newPassword`、`confirmPassword`、`token`、`accessToken`、`refreshToken`、`authorization` 和 `secret`。`@Log.excludeParamNames` 在默认集合上追加。

宿主可替换全局排除集合，例如：

```yaml
alt:
  log:
    excluded-fields:
      - password
      - oldPassword
      - newPassword
      - confirmPassword
      - token
      - accessToken
      - refreshToken
      - authorization
      - secret
      - internalCode
```

该列表替换默认集合；仅按名称过滤字段，不识别任意字符串里的凭据。返回值本身是凭据的接口应关闭响应记录。

序列化某个值失败时，该值记录为空字符串，其他元数据仍会发布。日志解析或处理失败会记录诊断信息，不改变业务返回值和原异常。

## 事件与事务

```text
@Log → 构建 OperLogEvent → 发布本地事件
                              ├─ JPA 监听器 → LogPersistenceService → REQUIRES_NEW 保存
                              └─ 消息生产者 → EventTemplate.to("log-oper").publish(...)
```

操作和登录日志使用独立写事务，覆盖无事务调用、成功操作、失败操作以及外层业务事务回滚。记录的是方法执行结果，不是外层事务的最终提交状态；外层回滚后仍可能存在状态为成功的方法执行记录。

事件在当前线程同步处理。JPA 持久化失败会传播到发布边界：本地 `@Log` 拦截器会隔离它；MQ 消费时会交给 event-component 的失败处理策略，避免将未保存的日志当作消费成功。登录日志也在认证发布入口隔离失败，保存失败不会打断登录处理；MQ 登录日志消费失败则向消息框架传播，由绑定的重试和死信策略处理。

日志包含租户 ID、操作者、方法名、操作时间、耗时、成功/失败状态及错误消息。租户默认取 `TenantContextHolder.getTenantId()`，操作者取 `UserContextHolder.getUsername()`。

## 登录来源、租户与时间

Cookie 与 JWT 的登录成功、失败处理器均发布 `LoginInfoEvent`，自动装配的 `LoginHelper.login()` 也会发布一次登录事件。JSON、表单和自定义用户名参数均受支持；过滤器在认证前保存用户名和请求租户，认证过程清理上下文后仍可记录失败日志。事件不携带密码或令牌。

成功日志优先取认证结果 `SecurityUser.getTenantName()`，未提供时回退到请求已解析的租户；失败日志使用认证前的请求租户。不单独读取未经租户组件解析的请求头，无法确定租户时记录空字符串。手动构造 Cookie 处理器或 Cookie/JWT LoginHelper 时，使用带 `ApplicationEventPublisher` 的新构造器以启用审计；旧构造器保留原行为。

源端设置 `loginTime`，MQ 传递和 JPA 保存均保留该时间；旧版本或手动事件缺少时间时才使用保存时刻。租户、IP、地点、浏览器、操作系统随消息负载传递；消费端按缺失字段补全地点和客户端信息。当前登录日志表已有租户与时间列，无需新增迁移。

## 登录 IP 与登录地点

登录事件保留 `ip` 原值，`loginLocation` 单独记录归属地。默认 JPA 保存时，若事件未提供地点则调用 `IpLocationResolver` 补全；失败或未命中时保存空字符串，仍保留登录记录和 IP。历史记录不会被自动回填。

引入可选适配器即可使用 ip2region，admin 应用已引入：

```kotlin
implementation(project(":backend:components:log-component:ip2region-log-component"))
```

从 [ip2region 官方数据目录](https://github.com/lionsoul2014/ip2region/tree/master/data) 获取对应的 `ip2region_v4.xdb` / `ip2region_v6.xdb`，部署到本地目录后配置：

```yaml
alt:
  log:
    ip2region:
      enabled: true
      ipv4-database: file:/opt/ip2region/ip2region_v4.xdb
      ipv6-database: file:/opt/ip2region/ip2region_v6.xdb # 可选
```

admin 也可通过 `IP2REGION_IPV4_DATABASE` / `IP2REGION_IPV6_DATABASE` 环境变量设置上述资源地址。支持 `file:` 与 `classpath:`（例如 `classpath:ip2region/ip2region_v4.xdb`）。xdb 不随组件打包，也不会在运行时联网下载；更新文件后重启服务加载。

适配器启动时将数据载入内存，使用 ip2region 官方查询服务；输出形如 `中国 广东省 深圳市 电信`。IPv4/IPv6 可分别配置，数据库缺失、损坏或地址无法定位均不会阻断登录日志。可通过 `alt.log.ip2region.enabled=false` 关闭，或贡献自己的 `IpLocationResolver` Bean 替换。JPA 宿主需添加 `sys_logininfor.login_location VARCHAR(255)`；admin 已提供 H2/PG 的 V0025 增量迁移。

IP 来源仍由登录处理器采集；反向代理部署请通过宿主的转发头配置传递客户端 IP。ip2region 用于查询地址归属地，不负责识别代理链。

## 登录浏览器与操作系统

Cookie 与 JWT 登录成功、失败处理器均采集请求的 `User-Agent`。日志组件使用 ua-parser 统一解析为 `browser` 和 `os`，由 MapStruct 映射后保存到 `sys_logininfor`。默认显示名称和解析到的版本，例如 `Chrome 131.0.0` / `Windows 10`。原始 User-Agent 只随事件传递，不直接存入登录日志表。

缺失、未知或解析失败时，相应字段为空，登录记录仍会保存。事件来源可以直接提供浏览器或系统字段，默认只补齐缺失的字段；也可贡献 `UserAgentResolver` Bean 自定义解析。admin 已提供 H2/PG 的 V0026 增量迁移，列表与详情均展示浏览器和操作系统，历史记录显示 `-`。

## 多服务部署

```kotlin
implementation(project(":backend:components:log-component:message-log-component"))
// 按部署需要选择 event-component 的 Redis、Kafka 或 Rabbit 实现
```

业务服务默认启用生产者，消费者默认关闭。日志聚合服务开启消费并引入 JPA 模块：

```yaml
# 业务服务
log.message.producer-enabled: true
log.message.consumer-enabled: false

# 日志聚合服务（配置于另一个应用）
# log.message.producer-enabled: false
# log.message.consumer-enabled: true
```

按需配置以下 binding，只有配置了相应 `transport` 才注册该类日志的生产者/消费者；仅配置原有 `log-oper` 的应用不会启用登录日志分发。

| 日志 | binding | 消费端点 ID |
|---|---|---|
| 操作日志 | `log-oper` | `operation-log` |
| 登录日志 | `log-login` | `login-log` |

例如，使用 `redis-event-component` 分发登录日志时，发送端和消费端均配置：

```yaml
alt:
  event:
    bindings:
      log-login:
        transport: redis-stream
        redis:
          destination: audit:login
          group: login-log-writers
        failure:
          destination: audit:login:dead-letter
```

消费者还需设置 `log.message.consumer-enabled=true`。操作日志可配置独立的 `log-oper` stream；Kafka 或 Rabbit 使用各自的 topic/group 或 exchange/queue 配置，详见 [event-component](../event-component/README.md)。

消费回流事件会在本地立即处理，生产者会跳过它，同一服务同时启用发送和接收也不会把收到的事件重新发送。发送是同步的独立动作，不与业务数据库事务组成原子提交；Broker 发送失败记录诊断信息。

## 扩展与兼容

可贡献 `UserResolver`、`TenantResolver`、`IpResolver`、`RequestResolver`、`RequestParameterResolver`、`LogRecordFactory` 或 `LogHandler` Bean，替换相应默认行为。JPA 层可贡献 `LogPersistenceService`。

`TenantResolver.currentTenant()` 使用 Vavr `Option<String>` 表示是否存在租户。其余既有 SPI 保留原签名，包括 `LogHandler` / `LogRecordFactory` 的历史 checked 异常声明；默认实现使用 Jackson 3。直接构造默认 Factory 或参数 Resolver 的代码需要使用新的构造器依赖，建议通过 SPI Bean 扩展。

JPA 实体为 `sys_oper_log`、`sys_logininfor`；主键列是 `id`，由 SnowflakeId 生成，不使用数据库 AUTO_INCREMENT。生产数据库应由宿主提供匹配实体的迁移脚本。

## 验证

```shell
./gradlew :backend:components:log-component:core-log-component:check \
  :backend:components:log-component:jpa-log-component:check \
  :backend:components:log-component:message-log-component:check
```

测试覆盖注解解析、日志失败隔离、Jackson 3、参数/响应开关、敏感字段过滤、三类应用装配、H2/JPA 事务回滚隔离、登录日志源端时间保留、MQ 实际编解码、租户上下文恢复及消费回流。三个实现模块的业务代码行覆盖率门槛为 80%，不计 Lombok 数据模型和生成的 mapper。

Native hints 包含组件日志事件的绑定元数据、`ua_parser/regexes.yaml` 与 `classpath:ip2region/*.xdb` 资源；自定义 classpath 数据目录与宿主业务 DTO 需提供相应资源或绑定提示。此次验证为 JVM 测试，未执行 native-image 构建。
