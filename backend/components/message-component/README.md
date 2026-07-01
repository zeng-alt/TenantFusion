# 消息模块 (message-component)

统一消息收发组件，支持 **Redis** / **RabbitMQ** / **Kafka** 三种实现，提供 API 包和多种实现包的可插拔架构。

---

## 模块结构

```
message-component/
├── api-message-component/                 # API 模块（必选）
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
├── redis-message-component/               # Redis 实现（可选）
├── rabbit-message-component/              # RabbitMQ 实现（可选）
└── kafka-message-component/               # Kafka 实现（可选）
```

---

## 快速开始

### 1. 引入依赖

选择一种实现模块引入：

```kotlin
// build.gradle.kts

// Redis 实现
implementation(project(":backend:components:message-component:redis-message-component"))
// 或 RabbitMQ
implementation(project(":backend:components:message-component:rabbit-message-component"))
// 或 Kafka
implementation(project(":backend:components:message-component:kafka-message-component"))
```

> 只引入 `api-message-component` 不会自动配置 `MessageQueueTemplate` Bean，必须配合一个实现包使用。

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

1. **API/SPI 分离** — `api-message-component` 定义接口契约，实现包按需选择
2. **拆包透明** — `MessagePacketCodec` 在模板内部自动完成，用户无感知
3. **AOT 优先** — 接口式订阅零反射；注解式通过 Spring 受控反射调用
4. **三种消费模式** — 发送、拉取（Pull）、推送（Push/Pub-Sub）
5. **占用即用** — 引入实现依赖即可，`MessageAutoConfiguration` 自动装配
