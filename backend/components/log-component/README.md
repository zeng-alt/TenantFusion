# 日志模块 (log-component)

通过 `@Log` 记录方法执行结果。核心模块采集元数据并发布本地事件；JPA 和消息模块按需引入。

| 模块 | 职责 |
|---|---|
| `api-log-component` | `@Log`、操作日志事件、登录日志事件和枚举 |
| `core-log-component` | AOP、Jackson 3 字段过滤、请求与身份解析、事件发布 |
| `jpa-log-component` | MapStruct 映射及独立事务持久化 |
| `message-log-component` | 通过 event-component 发送或接收操作日志 |

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

事件在当前线程同步处理。JPA 持久化失败会传播到发布边界：本地 `@Log` 拦截器会隔离它；MQ 消费时会交给 event-component 的失败处理策略，避免将未保存的日志当作消费成功。登录日志沿用失败隔离行为，保存失败不会打断登录处理。

日志包含租户 ID、操作者、方法名、操作时间、耗时、成功/失败状态及错误消息。租户默认取 `TenantContextHolder.getTenantId()`，操作者取 `UserContextHolder.getUsername()`。

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

发送和消费需要配置 `alt.event.bindings.log-oper`，指定 transport 及对应的队列、topic 或 stream；消费者端点 ID 为 `operation-log`。

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

测试覆盖注解解析、日志失败隔离、Jackson 3、参数/响应开关、敏感字段过滤、三类应用装配、H2/JPA 事务回滚隔离及消息回流。三个实现模块的业务代码行覆盖率门槛为 80%，不计 Lombok 数据模型和生成的 mapper。

Native hints 包含组件日志事件的绑定元数据；宿主业务 DTO 仍需提供相应绑定提示。此次验证为 JVM 测试，未执行 native-image 构建。
