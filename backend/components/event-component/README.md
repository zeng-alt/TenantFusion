# 事件组件

`api-event-component` 提供统一的监听、发送和上下文契约。Kafka、RabbitMQ、Redis 适配器可同时存在，按逻辑 binding 选择，不使用全局唯一的消息模板。

旧 `MessageListener`、`MessageHandler`、`MessageQueueTemplate`、消息包格式及隐式 Redis 外部化/订阅已移除，不提供兼容层。业务事件不需要继承或携带上下文字段。

## 监听与处理结果

```java
@EventSubscription(id = "inventory-order-created", binding = "orders")
public EventOutcome onOrderCreated(OrderCreatedEvent event, EventDelivery delivery) {
    if (inventoryService.isTemporarilyUnavailable()) {
        return delivery.retry(Duration.ofSeconds(10));
    }
    if (!inventoryService.supports(event.productId())) {
        return delivery.reject("商品不支持库存预留");
    }
    inventoryService.reserve(event);
    return delivery.ack();
}
```

`EventDelivery` 的 `ack()` / `retry()` / `retry(Duration)` / `reject(reason)` 返回不可变的 `EventOutcome`，不会立即操作 Broker。必须把结果从监听方法返回。

- `Acknowledged`：业务处理成功，适配器完成确认。
- `Retry`：按策略安排重试；显式延迟表示最小等待时间。
- `Rejected`：不再进行业务重试，转入配置的死信目的地，或显式配置的丢弃策略。
- `void` 方法正常结束视为成功；异常进入同一个重试预算。
- `EventOutcome` 方法返回 `null` 是处理失败，不会被确认为成功。
- `max-attempts` 包含首次处理。异常重试与主动重试共用预算。
- 不支持 `@Async`、Future 或响应式返回值，启动时拒绝。框架必须观察业务完成后才能确认。

方法支持普通泛型 payload、`org.springframework.messaging.Message<T>`、`@Header`、`@Headers`、`EventDelivery`、`EventContext` 和无参签名。一个方法最多一个 payload。
`List<Order>` 表示业务负载本身是列表，不隐式开启 Kafka 批量消费。

注解的 `id` 和 `binding` 支持 `${...}` 占位符。端点 ID 全局唯一，多个端点可以引用同一 binding；实际广播或竞争关系仍取决于消费组/队列配置。

## 上下文

进入监听器前，框架从消息提取来源快照，恢复 `TenantContextHolder` 及独立的 `SecurityContextHolder`。方法结束、异常、重试和拒绝都会还原工作线程原来的上下文。

```java
String tenantId = TenantContextHolder.getTenantId();
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String userId = UserContextHolder.getId();

EventContext context = delivery.context();
Option<String> tenant = context.attribute("request.tenantId");
Map<String, String> attributes = context.attributes();
Map<String, String> headers = context.headers();
```

`EventContext` 还提供 `messageId()`、`listenerId()`、`retryCount()` 和 `nativeContext(Class<T>)`。只需要上下文的方法可直接声明 `EventContext` 参数，无需声明 `EventDelivery`。
`retryCount` 是组件已安排的重试次数，从 0 开始，不代表 Broker 因断线等原因产生的全部重复投递次数。

内置 `RequestEventContextHandler` 恢复租户 ID、database/schema、`TenantRouting`、用户 ID/用户名及 `SecurityUser`。密码、Token、角色权限不进入快照；身份快照不等同于完整登录会话。

新增上下文通过 `EventContextHandler` Bean 贡献：

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

贡献者通过 `ObjectProvider.orderedStream()` 收集，按顺序恢复、逆序还原。空快照表示清除本次上下文，不能沿用线程上一次任务的身份。

Kafka/Rabbit 的 payload 是业务 JSON，元数据保存在 `alt-event-metadata` header；Redis 使用包含 payload 和元数据的 JSON 信封。读取负载按监听方法声明的类型进行，不根据网络消息加载 Java 类。
没有组件元数据的 Kafka/Rabbit 消息按空上下文处理；外部生产者需要遵循该元数据协议才能传播上下文。

## 发送

```java
String messageId = eventTemplate.to("orders")
        .key(orderId)
        .header("source", "checkout")
        .publish(event);
```

`publish` 时捕获当前上下文。默认生成消息 ID；已有稳定的业务事件 ID 时可调用 `.messageId(eventId)`。重试和死信转发保留 ID、上下文及业务 headers。
普通发送不自动加入 Outbox，应在需要可靠持久化发布的业务流程中使用 Namastack。

## Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
    consumer:
      auto-offset-reset: earliest
alt:
  event:
    bindings:
      orders:
        transport: kafka
        kafka:
          topic: orders.created
          group-id: inventory
          retry-topic: inventory.orders.retry
          concurrency: 3
        retry:
          max-attempts: 5
          initial-delay: 1s
          multiplier: 2
          max-delay: 30s
        failure:
          action: dead-letter
          destination: inventory.orders.dead
```

topic、retry-topic 和死信 topic 由部署端或 KafkaAdmin 声明。不同消费组应使用独立 retry-topic。

适配器使用 `ConcurrentMessageListenerContainer` 和 `MANUAL_IMMEDIATE` 确认，关闭 auto commit。重试转发收到发送确认后才提交源 offset；转发失败保留源记录，不由默认 recoverer 跳过。
重试记录带持久化的最早执行时间；未到期时使用容器 nack/pause。延迟精度受 poll timeout 影响。

当前统一接口使用独立重试 topic，会改变失败消息相对于后续消息的顺序；不提供跨 source/retry topic 的严格业务顺序保证。
Kafka key 随重试保留。通过 `context.nativeContext(ConsumerRecord.class)` 可读取原生记录。

## RabbitMQ

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
alt:
  event:
    bindings:
      orders:
        transport: rabbit
        rabbit:
          queue: inventory.orders
          exchange: orders
          routing-key: created
          retry-queue: inventory.orders.retry
          prefetch: 50
          concurrency: 2
        retry:
          max-attempts: 5
          initial-delay: 1s
        failure:
          action: dead-letter
          destination: inventory.orders.dead
```

主队列、exchange、binding 和失败队列由应用的 `Declarables` 或部署端声明；失败目的地是通过默认 exchange 路由的队列名称。
重试队列由组件声明为 quorum queue，使用 TTL、默认 exchange 的死信路由及 at-least-once dead-lettering。已有同名队列必须符合此定义。
延迟是最小等待时间，逐消息 TTL 可能受队头消息影响。

容器采用 MANUAL 模式。重试/死信转发必须获得 publisher confirm 且没有 unroutable return，之后才 ACK 源消息。发送失败时 NACK 源消息以便重新投递。
`EventContext.nativeContext` 可读取 AMQP `Message` 和当前 `Channel`。直接操作原生 Channel 会绕开统一确认契约，应仅用于明确的原生扩展。

## Redis Streams

```yaml
alt:
  event:
    bindings:
      orders:
        transport: redis-stream
        redis:
          destination: orders.created
          group: inventory
          poll-interval: 200ms
          claim-idle: 30s
          batch-size: 50
        retry:
          max-attempts: 5
          initial-delay: 1s
        failure:
          action: dead-letter
          destination: inventory.orders.dead
```

使用 `StreamMessageListenerContainer`；成功后 XACK。组件创建消费组，从 `0-0` 开始读取已有消息。
独立重试流命名为 `<destination>:retry:<group>`，不把某个消费组的重试重新广播给其他组。
重试计数和最早执行时间保存在消息信封中，重启后通过 pending/claim 恢复。claim-idle 应大于正常业务处理时长；重试精度也受该阈值影响。
Streams 的保留期限、裁剪和 Redis 持久化策略由部署配置管理。

## Redis Pub/Sub

```yaml
alt:
  event:
    bindings:
      notifications:
        transport: redis-pubsub
        redis:
          destination: cache.invalidate
        retry:
          max-attempts: 1
        failure:
          action: drop
```

仅用于在线通知。支持同样的上下文恢复，但没有持久化确认、离线补投或可靠重试。可靠重试/死信配置在启动时被拒绝，应改用 Streams。
`ack()` 在这里仅表示业务处理成功，不表示 Broker 保存了消费进度。

## 事务、生命周期及原生扩展

指定 `alt.event.bindings.<name>.transaction-manager` 可启用托管事务，值为已有 `PlatformTransactionManager` Bean 名称。
上下文先恢复，再开启事务并调用业务代理：成功提交后才确认；Retry/Rejected 回滚参与当前事务的业务操作。独立 REQUIRES_NEW 事务不在这个回滚范围内。
不指定该选项时，组件不替业务开启事务；监听方法返回 retry 并不会撤销此前独立提交的业务操作。

数据库提交与 Broker 确认不是一个分布式原子事务，转发与源确认之间也可能重复投递，业务应使用稳定事件 ID 实现幂等。

`EventSubscriptionTemplate.ids()` / `start(id)` / `stop(id)` 按端点管理订阅。
`auto-startup: false` 可保留注册但不自动启动。

原生扩展通过 `KafkaEventCustomizer`、`RabbitEventCustomizer`、`RedisEventCustomizer` Bean 贡献，收集时尊重 `@Order`。
例如调整 Kafka poll timeout：

```java
@Bean
KafkaEventCustomizer kafkaPolling() {
    return (endpoint, container) -> container.getContainerProperties().setPollTimeout(200);
}
```

统一接口保留确认模式的所有权，Customizer 不得改成自动确认。需要原生批量监听、特殊事务容器或其他专用协议时，可以使用框架的 `@KafkaListener` / `@RabbitListener`；同一方法不要再叠加 `@EventSubscription`。

## Spring Modulith / Namastack

已有 Spring 原生发布与持久化上下文适配继续生效：

- 发布广播入口捕获快照，普通单事件参数监听器执行时恢复。
- Modulith 2.1.2 的 `EventSerializer` 包装 `event_publication.serialized_event`，反序列化后仍返回原始业务类型；旧裸 JSON 可读取，新包装不能由旧适配器读取。
- Namastack 1.7.3 使用原生 context 列，dispatch 和 fallback 执行时恢复上下文。
- 自定义 `@OutboxHandler` 中调用 `EventTemplate`，会把恢复的上下文写入 Broker 消息。可通过 `.messageId(...)` 传入稳定业务事件 ID，使 Outbox 多次发送也使用相同 ID。

Spring Modulith 自带的 Kafka/AMQP 外部化发送器并不自动使用本组件的传输元数据协议。如果使用统一监听器获取远程上下文，应由 Outbox Handler 调用 `EventTemplate`，或让发送器显式写入该协议的 header，不能只依赖 Outbox 数据库里的 context 列。
旧 Redis 隐式 Pub/Sub 外部化和按远程类名重新发布的监听器已删除；应通过明确 binding 和业务类型的监听方法接入。

普通 Spring 事件的内存快照关联按对象身份保存，每次发布应创建独立事件实例。无参事务监听器不在原有 Spring 事件适配范围；这与 `@EventSubscription` 的无参方法支持是两个独立入口。

## 验证

```bash
./gradlew :backend:components:event-component:api-event-component:check \
  :backend:components:event-component:kafka-event-component:check \
  :backend:components:event-component:rabbit-event-component:check \
  :backend:components:event-component:redis-event-component:check
```

Kafka 使用嵌入式 KRaft Broker；RabbitMQ 和 Redis 使用 Testcontainers，需要 Docker。
复用本地已有的无认证 Redis，并跳过 Kafka 时，可运行：

```bash
EVENT_TEST_REDIS_HOST=127.0.0.1 EVENT_TEST_REDIS_PORT=16379 ./gradlew \
  :backend:components:event-component:api-event-component:check \
  :backend:components:event-component:rabbit-event-component:check \
  :backend:components:event-component:redis-event-component:check --rerun-tasks
```

Redis 测试仅删除自身随机命名的测试键；RabbitMQ 测试容器在结束后自动移除。
指定外部 Redis 地址后，连接失败会使测试失败，不会回退到临时容器。
四个模块的 check 都包含 JaCoCo 行覆盖率 80% 门槛。
`EventSubscriptionAotProcessor` 注册具体监听方法和负载类型，`EventRuntimeHints` 注册传输信封。本次测试包括提示注册验证，不等同于完整 Native Image 验证。
