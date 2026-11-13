# 事件组件 (event-component)

提供 Spring/Modulith/Namastack 事件上下文传播，以及 Redis、RabbitMQ、Kafka 传输适配。
Gradle 模块已改名为 `event-component` / `api-event-component` / `*-event-component`。
现有 `com.github.zeng.alt.message.*` 消息传输 API 保持兼容，新事件上下文 API 位于
`com.github.zeng.alt.event.*`。

## 普通事件直接发布

业务事件不需要继承基类、实现接口、增加 context 字段或改用专用发布器：

```java
@Externalized("orders.created::#{orderId}")
public record OrderCreatedEvent(String orderId) {
}

// 在业务事务中使用 Spring 原生 ApplicationEventPublisher。
eventPublisher.publishEvent(new OrderCreatedEvent(orderId));
```

`@EventListener`、`@TransactionalEventListener`、`@ApplicationModuleListener` 的类型化
监听方法仍接收原来的业务类型。处理前恢复上下文，返回或抛异常后还原原线程。
拦截顺序早于监听器事务，使数据库访问使用恢复后的租户。

## 发布、存储、消费三层边界

上下文固定在发布时，不能等监听器执行或重试时再从线程取值：

| 发布入口 | 捕获位置 | 保存位置 | 恢复位置 |
|---|---|---|---|
| `ApplicationEventPublisher` 普通内部事件 | `ApplicationEventMulticaster.multicastEvent` | 原事件的内存快照关联 | 单事件参数的 Spring 监听器方法 |
| `ApplicationEventPublisher` + Modulith 持久化监听器 | 同上 | `EventSerializer` 的持久化包装 | 首次监听及数据库重放时的监听器方法 |
| `ApplicationEventPublisher` + `@Externalized`（outbox 模式） | Spring 发布入口，然后由官方 recorder 调用 Outbox | Namastack 原生 `context` 列 | Outbox dispatch，包括重试和补偿 |
| 直接 `Outbox.schedule(...)`，所有四种重载 | Namastack `OutboxContextProvider` | Namastack 原生 `context` 列 | 同一个 Outbox dispatch 适配器 |

`EventContextAutoConfiguration` 只装配公共 SPI；`SpringEventContextConfiguration` 负责
发布与监听边界，Modulith 与 Namastack 配置分别适配各自的存储和消费生命周期。
原生 `ApplicationEventPublisher` 由容器提供，因此装饰它背后的广播器，业务无需替换发布器。
已有 Modulith 或自定义广播器保留原实现；纯 Spring 应用显式注册 Spring 默认广播器，
使其也经过发布入口的拦截。发布退出时和监听退出时分别还原当前线程。

普通异步单参数监听器即使使用没有 TaskDecorator 的执行器，也从事件关联读取发布快照。
提交前切换用户/租户后，事务监听器仍读取发布时的值。
无参异步监听器没有事件参数用于关联，需要为执行器配置下面的 TaskDecorator；
无参事务监听器不属于当前适配范围。一次发布应创建独立的事件对象，不跨发布复用同一实例。

直接 Outbox 发布无需 `@Externalized`：

```java
// 在业务事务中调用，自动捕获当前租户和用户。
outbox.schedule(new OrderCreatedEvent(orderId), orderId);
outbox.schedule(new OrderCreatedEvent(orderId), orderId, Map.of("business.source", "checkout"));
```

业务附加字段应使用自己的命名空间。Namastack 的合并契约是 `additionalContext` 覆盖
Provider 的同名键，因此业务字段不要占用组件的 `request.*` 键。

## 上下文扩展接口

注册 `EventContextHandler` Bean 即可贡献上下文，框架通过
`ObjectProvider<EventContextHandler>.orderedStream()` 收集并尊重 `@Order`：

```java
public interface EventContextHandler {
    String key();
    Map<String, String> capture();
    Scope restore(Map<String, String> contextByKey);

    interface Scope extends AutoCloseable {
        void close();
    }
}
```

- `key()` 是稳定、唯一的命名空间。例如 `request`，持久化键形如 `request.tenantId`。
- `capture()` 返回当前线程的字符串快照；没有上下文时返回空 Map。
- `restore()` 接收本命名空间的字段，安装上下文，并返回还原原线程状态的 Scope。
  空 Map 表示清除本次上下文，不应沿用处理线程上一个任务的值。
- Scope 在处理完成后关闭，多贡献者按正序恢复、逆序还原。

内置 `RequestEventContextHandler` 捕获租户 ID、database/schema、完整 `TenantRouting`
路由及用户 ID/用户名。它恢复租户 ThreadLocal，并为本次处理安装独立的 SecurityContext，
使 `UserContextHolder.getId()`、`getUsername()`、`getSecurityUser()`、`getTenant()` 可直接读取。
恢复的 SecurityUser 同时包含快照中的 database/schema；密码、Token、角色权限不进入快照，
重建的身份不携带 authorities。缺少用户 ID 或用户名的快照按无用户上下文处理。
作用域退出时还原原来的 SecurityContext 对象，包括原认证凭据及权限，不修改共享上下文。

`EventContextTemplate` 同时实现 Spring `TaskDecorator`。Boot 默认执行器会使用它；
项目既有 `CompositeTaskDecorator` 会将它与其他装饰器组合。自行创建执行器的应用
需要把它加入该执行器的 TaskDecorator 配置。

## 持久化与重试

### Spring Modulith 2.1.2

此版本没有独立的业务 context 字段或 capture/restore SPI。
组件通过官方 `EventSerializer` 扩展点装饰现有序列化器：

```json
{
  "$alt.event.context": 1,
  "context": { "request.tenantId": "tenant-a" },
  "serialized": "{\"orderId\":\"123\"}"
}
```

包装只存在于 `event_publication.serialized_event`，原事件类型、路由和监听签名不变。
反序列化返回原业务对象；基础设施按对象身份临时关联快照，监听器执行时恢复。
该关联使用弱引用，不延长事件生命周期，也不会让两个相等 record 共用上下文。
读取数据库记录只建立关联，不会污染读取线程的 ThreadLocal。

新适配器兼容旧的裸事件 JSON；旧记录没有来源上下文，恢复为空上下文。
新记录的包装格式需要新适配器读取，部署时不要让旧版本实例消费新格式记录。

### Namastack 1.7.3

直接使用原生 `outbox_record.context`，业务 payload 保持原样。
`OutboxContextProvider` 负责捕获，适配器在 `OutboxHandlerInvoker.dispatch` 与
`OutboxFallbackHandlerInvoker.dispatch` 的可代理边界恢复和还原上下文。
因此 typed Handler、lambda Handler、注解 Handler 均无需增加 metadata 参数：

```java
@OutboxHandler
public void deliver(OrderCreatedEvent event) {
    // TenantContextHolder 已恢复，业务事件本身只有 orderId。
}
```

dispatch 适配针对项目锁定的 1.7.3，升级 Namastack 时需要运行组件的真实重试与补偿测试。
API 模块将 Modulith/Namastack 依赖声明为 `compileOnly`，不会强制启动持久化运行时。

## Admin 装配与边界

Admin 引入 `api-event-component` 和 `spring-modulith-starter-namastack`，开启
`spring.modulith.events.externalization.mode=outbox`。Modulith 2.1.2 带入 Namastack 1.7.3 JDBC。
`V0024` H2/PG Liquibase 脚本管理 Outbox 三张表，Namastack 自动建表关闭。

业务写入与 Outbox 入队应在同一数据源的同一事务内。本次没有扩展跨租户数据库/schema
的 Outbox 轮询；恢复处理上下文不等于扫描多个租户存储。
Admin 尚需选择具体 Broker 或自定义 Outbox Handler。Rabbit Outbox 模式需要
`spring.rabbitmq.publisher-confirm-type=correlated`。
现有 Redis `DelegatingEventExternalizer` 和 `MessageQueueTemplate.send()` 保持原来的直接发送路径。

参考：[Modulith EventSerializer](https://docs.spring.io/spring-modulith/reference/events.html#_event_serializer)、
[Namastack Context Propagation](https://www.namastack.io/docs/reference/context-propagation/)。

---

## 模块结构

```
event-component/
├── api-event-component/                 # API 模块（必选）
│   ├── Message<T>                          # 消息体
│   ├── MessageQueueTemplate                # 统一收发模板
│   ├── MessageSender                       # 发送接口
│   ├── MessageListener<T>                  # 监听器函数式接口
│   ├── MessageHandler<T>                   # 接口式订阅（类似 ApplicationListener）
│   ├── annotation/@MessageListener         # 注解式订阅
│   ├── codec/
│   │   ├── MessagePacket                   # 拆包：消息传输单元（含分片）
│   │   ├── MessagePacketCodec              # 编解码器接口
│   │   └── JacksonMessagePacketCodec       # Jackson JSON 实现
│   ├── config/MessageAutoConfiguration     # 自动配置
│   └── subscription/
│       └── MessageListenerBeanPostProcessor # 订阅注册器
│
├── redis-event-component/               # Redis 实现（可选）
├── rabbit-event-component/              # RabbitMQ 实现（可选）
└── kafka-event-component/               # Kafka 实现（可选）
```

---

## 快速开始

### 1. 引入依赖

选择一种实现模块引入：

```kotlin
// build.gradle.kts

// Redis 实现
implementation(project(":backend:components:event-component:redis-event-component"))
// 或 RabbitMQ
implementation(project(":backend:components:event-component:rabbit-event-component"))
// 或 Kafka
implementation(project(":backend:components:event-component:kafka-event-component"))
```

> 只引入 `api-event-component` 不会自动配置 `MessageQueueTemplate` Bean，必须配合一个实现包使用。

### 2. 发送消息

```java
@Autowired
private MessageQueueTemplate messageQueue;

// 简单发送（仅负载）
Order order = new Order().setId("123");
messageQueue.send("order.created", order);

// 带头部信息
Map<String, String> headers = new HashMap<>();
headers.put("source", "web");
messageQueue.send("order.created", order, headers);

// 发送完整 Message
Message<Order> msg = new Message<>("order.created", order);
msg.setHeaders(headers);
messageQueue.send("order.created", msg);
```

### 3. 接收消息

**拉模式（主动拉取）：**

```java
// 阻塞直到有消息
Message<Order> msg = messageQueue.receive("order.created");

// 带超时
Message<Order> msg = messageQueue.receive("order.created", 5, TimeUnit.SECONDS);
```

**推模式（订阅）：** 见下方两种订阅方式。

---

## 两种订阅方式

### 方式一：接口式订阅（推荐）

实现 `MessageHandler<T>` 接口，类似 Spring 的 `ApplicationListener`：

```java
@Component
public class OrderCreatedHandler implements MessageHandler<Order> {

    @Override
    public String getTopic() {
        return "order.created";
    }

    @Override
    public void onMessage(Message<Order> message) {
        Order order = message.getPayload();
        System.out.println("收到订单: " + order.getId());
    }
}
```

> **优点：** 类型安全、编译期检查、无需反射、自然支持 AOT。

### 方式二：注解式订阅

使用 `@MessageListener` 标注任意方法：

```java
@Component
public class OrderHandler {

    // 只接收负载（最常用）
    @MessageListener(topic = "order.created")
    public void handleOrder(Order order) {
        System.out.println("收到订单: " + order.getId());
    }

    // 接收完整 Message 对象
    @MessageListener(topic = "order.paid")
    public void handlePaid(Message<Order> message) {
        System.out.println("消息 ID: " + message.getId());
        System.out.println("订单号: " + message.getPayload().getId());
    }

    // 无参数（仅触发）
    @MessageListener(topic = "order.refresh")
    public void refreshCache() {
        cacheManager.evictAll();
    }

    // 主题支持占位符
    @MessageListener(topic = "${app.order.topic}")
    public void handleWithConfig(Order order) { }
}
```

> **注意：** 方法可以定义在任意 Spring Bean 中，支持 `@Component`、`@Service` 等。

### 取消订阅

```java
// 编程式
messageQueue.unsubscribe("order.created");
```

---

## 拆包机制 (MessagePacket)

```
发送：Message<T> → JacksonMessagePacketCodec.encodeMessage() → MessagePacket → JSON bytes → 中间件
接收：中间件 bytes → JacksonMessagePacketCodec.decodeMessage() → Message<T>
```

`MessagePacket` 是消息的网络传输单元：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | 消息唯一 ID（UUID，自动生成） |
| `topic` | `String` | 主题 / 路由键 |
| `payload` | `byte[]` | 序列化后的负载字节 |
| `payloadClassName` | `String` | 负载类全名，用于反序列化 |
| `timestamp` | `long` | 创建时间戳（epoch millis） |
| `headers` | `Map<String,String>` | 头部元数据 |
| `totalParts` | `int` | 总分片数（大消息拆分，默认 1） |
| `partIndex` | `int` | 当前分片索引（从 0 开始） |

> 分片支持：当消息体积较大时，可将 `totalParts` 设为大于 1，并按 `partIndex` 分片发送。接收方按 `id` 聚合重组。

---

## 各实现对比

| 特性 | Redis | RabbitMQ | Kafka |
|---|---|---|---|
| 发送 | `RTopic.publish()` | `RabbitTemplate.convertAndSend()` | `KafkaTemplate.send()` |
| 拉取 | `RBlockingDeque.poll()` | `RabbitTemplate.receive()` | `Consumer.poll()` |
| 推送 | `RTopic.addListener()` | `SimpleMessageListenerContainer` | `ConcurrentMessageListenerContainer` |
| 持久化 | ✅ Redis Stream（List 无） | ✅ | ✅ |
| 广播 | ✅ Pub/Sub | ✅ Exchange | ✅ Consumer Group |
| 顺序保证 | ❌ | ❌ | ✅ 分区内有序 |
| 依赖 | Redisson | spring-boot-starter-amqp | spring-kafka |

### 配置示例

**Redis** (由 Redisson 自动配置)：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**RabbitMQ**：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

**Kafka**：

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
```

---

## API 一览

### MessageQueueTemplate

```java
// ===== 发送 =====
void send(String topic, T payload);
void send(String topic, T payload, Map<String, String> headers);
<T> void send(String topic, Message<T> message);

// ===== 拉取 =====
<T> Message<T> receive(String topic);                               // 阻塞
<T> Message<T> receive(String topic, long timeout, TimeUnit unit);   // 带超时

// ===== 订阅/取消 =====
<T> void subscribe(String topic, MessageListener<T> listener);
void unsubscribe(String topic);
```

### Message<T>

```java
String getId();                       // 消息唯一 ID
String getTopic();                    // 主题
T getPayload();                       // 负载
<R> R getPayloadAs(Class<R> type);    // 类型安全转换
long getTimestamp();                  // 时间戳
Map<String, String> getHeaders();     // 头部
```

---

## GraalVM Native Image 支持

所有模块均已配置：

- `MessageRuntimeHints` — 注册所有 API 类型到反射白名单
- 各实现模块的 `native-image.properties` — 配置 `--initialize-at-build-time`

**注意事项：**
- 注解式订阅 (`@MessageListener`) 通过 Spring `ReflectionUtils` 调用，AOT 会生成对应反射配置
- 接口式订阅 (`MessageHandler`) 通过类型安全调用，**零反射**，AOT 最佳
- 自定义负载类型需确保在反射配置中（Spring AOT 会自动处理 Bean 中出现的类型）

---

## 设计原则

1. **API/SPI 分离** — `api-event-component` 定义接口契约，实现包按需选择
2. **拆包透明** — `MessagePacketCodec` 在模板内部自动完成，用户无感知
3. **AOT 优先** — 接口式订阅零反射；注解式通过 Spring 受控反射调用
4. **三种消费模式** — 发送、拉取（Pull）、推送（Push/Pub-Sub）
5. **占用即用** — 引入实现依赖即可，`MessageAutoConfiguration` 自动装配
