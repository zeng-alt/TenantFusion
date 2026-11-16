# 业务配置组件

面向动态开关、业务参数和 JSON 配置对象。一个 `dataId` 对应一个配置值，配置由 `appCode + group + dataId` 唯一定位。

当前仓库基线为 Spring Boot 4 / Java 21。业务异步同步使用 Spring `ThreadPoolTaskScheduler`，配置模块不使用 RxJava。

## 接入

```kotlin
// 消费配置的服务
implementation(project(":backend:components:config-component:client-config-component"))

// 服务端存储二选一，均已包含 server-config-component
implementation(project(":backend:components:config-component:jpa-config-component"))
// 或使用纯 JDBC
// implementation(project(":backend:components:config-component:jdbc-config-component"))
```

同一服务同时引入客户端与任一种服务端存储模块即可兼任两个角色。服务端会贡献 `ConfigSourceTemplate`，客户端自动优先读取本地已发布快照，启动时不请求自身 HTTP 接口。本地发布在事务提交后立即更新缓存、刷新 Bean，再通知业务监听器。

只有客户端的服务通过 HTTP 获取配置，长轮询结束后重新拉取完整快照；超时也会校准，断线期间的更新能够在恢复后获取。

```yaml
config:
  client:
    app-code: shop
    server-addr: http://config-server:8085 # 只有远程客户端需要设置
    cache-dir: ./config-cache
```

服务端无需扫描组件包或自行创建 Controller、Service、RestTemplate。

## 服务端模块

| 模块 | 职责 | 依赖 |
| --- | --- | --- |
| `server-config-component` | 管理接口、发布、回滚、通知与共享表结构 | 存储 SPI，不依赖 ORM |
| `jpa-config-component` | `JpaConfigStorageTemplate`，实体和 Repository | 共享服务端、Spring Data JPA |
| `jdbc-config-component` | `JdbcConfigStorageTemplate`，参数化 SQL 与记录映射 | 共享服务端、Spring JDBC，不依赖 JPA |

应用选择一个存储适配即可，也可仅引入共享服务端并提供自己的存储。两个内置适配意外同时存在时只装配 JPA；自定义 `ConfigStorageTemplate` Bean 的优先级最高。

## 数据库

JPA 和 JDBC 使用同一套表结构。新安装执行共享服务端 JAR 内的 [db/config/schema.sql](server-config-component/src/main/resources/db/config/schema.sql)。脚本使用 H2 / PostgreSQL 通用语法，可接入宿主的 Liquibase 变更集：

```yaml
databaseChangeLog:
  - changeSet:
      id: install-business-config
      author: zeng-alt
      changes:
        - sqlFile:
            path: db/config/schema.sql
            relativeToChangelogFile: false
```

已有配置表执行 [upgrade-snapshot.sql](server-config-component/src/main/resources/db/config/upgrade-snapshot.sql)，增加 `sys_config_release.snapshot` 列。旧发布记录没有保存当时的完整内容，升级后须把仍需生效的配置一起重新发布。旧记录不能用于恢复历史内容。

模块提供脚本，由宿主迁移工具执行。

## 自定义服务端存储

仅依赖服务端业务模块，并贡献一个 `ConfigStorageTemplate` Bean：

```kotlin
implementation(project(":backend:components:config-component:server-config-component"))
```

```java
@Bean
public ConfigStorageTemplate configStorageTemplate(MyConfigRepository repository) {
    return new MyConfigStorageTemplate(repository);
}
```

接口位于 [ConfigStorageTemplate.java](api-config-component/src/main/java/com/github/zeng/alt/config/storage/ConfigStorageTemplate.java)，使用与 JPA 无关的不可变模型。自定义存储不需要 JPA、数据源或继承组件的 Service。

配置 CRUD 的主要方法：

```java
ConfigEntryDto createConfig(ConfigEntryDto config);
Option<ConfigEntryDto> getConfig(Long configId);
Option<ConfigEntryDto> getConfig(Long appId, String dataId, String group);
List<ConfigEntryDto> listConfigs(Long appId);
List<ConfigEntryDto> listConfigs(List<Long> configIds);
ConfigEntryDto updateConfig(ConfigEntryDto config);
Option<ConfigEntryDto> deleteConfig(Long configId);
```

完整接口还包含应用创建/读取/列表、历史追加/读取、发布快照创建/更新/最近记录读取，以及 `execute(Supplier<T>)` 原子操作边界。这些操作是保留管理、发布和回滚行为所需的存储能力。

实现约定：

- 创建时分配 ID，更新配置时维护 `version`；返回持久化后的模型。
- 缺失返回 `Option.none()`，查询集合返回不可变集合；存储故障抛出 `ConfigException` 或底层真正的存储异常。
- `deleteConfig` 标记 `deleted` 并保留内容和 ID；发布后才从有效快照移除，便于回滚。
- `execute` 必须提供原子写入和异常回滚。自有事务提交后返回；如果加入外层 Spring 事务，则随外层事务提交或回滚。
- 业务层在存储操作成功后发出变更事件。若存在外层 Spring 事务，客户端仍等待事务提交后生效。

`JpaConfigStorageTemplate` 和 `JdbcConfigStorageTemplate` 均通过 Spring 事务执行原子操作，支持加入宿主事务。贡献自定义存储后，两个适配都会退让。

选择 JPA 时引入 `jpa-config-component`；选择纯 JDBC 时引入 `jdbc-config-component`。两者均传递包含服务端业务模块，并共用建表与升级脚本；切换适配不需要转换配置或发布快照。JDBC 更新使用相同的版本列，旧版本不会覆盖新内容。完全自定义存储时仅引入 `server-config-component`。

配置实体及 Repository 的 Java 包名保留，现位于 JPA 适配模块；服务端 Service 和 REST 使用中立的 `ConfigAppDto`、`ConfigEntryDto`、`ConfigReleaseDto`。REST 路径、返回包装及业务字段名保持原有约定。

## 直接读取

构造器注入 `ConfigTemplate`；应用身份在接入时绑定，读取时只需选择分组和键。

```java
@RequiredArgsConstructor
public class PaymentService {
    private final ConfigTemplate config;

    public Either<String, Boolean> isPaymentEnabled() {
        return config.group("payment").opsForValue().getBoolean("enabled", false);
    }

    public Either<String, PaymentRules> getRules() {
        return config.group("payment").opsForObject().get("rules", PaymentRules.class);
    }
}
```

- 字符串：`getString(key)` 返回 `Option<String>`，重载接受缺失时的默认值。
- 标量：`getBoolean`、`getInteger`、`getLong`、`getDecimal` 返回 `Either<String, T>`。
- 对象：`opsForObject().get(key, Type.class)` 将 JSON 绑定为 record 或业务类型。
- 默认值仅处理配置缺失。已有配置格式错误时返回 `Left`，不会被默认值掩盖。
- 布尔读取接受 `true` / `false`，忽略大小写和首尾空白。
- `group(...)` 返回独立视图，已有视图的分组不变。

JSON 配置内容示例：`{"limit":3,"currency":"CNY"}`。业务类型可声明为：

```java
public record PaymentRules(
    int limit,
    String currency
) {
}
```

## 推荐：像 Spring Boot 一样定义配置类

一个类级注解即可自动注册和刷新，字段不需要配置注解，默认值直接声明在 Java 字段上。配置类需要被应用正常的组件扫描覆盖。

```java
@Data
@ConfigProperties(prefix = "payment")
public class PaymentSettings {
    private boolean enabled = false;
    private PaymentRules rules = new PaymentRules(3, "CNY");
}
```

```java
public record PaymentRules(
    int limit,
    String currency
) {
}
```

在 `DEFAULT_GROUP` 保存这些属性即可：

| dataId | content | format |
| --- | --- | --- |
| `payment.enabled` | `true` | `text` |
| `payment.rules.limit` | `9` | `text` |
| `payment.rules.currency` | `USD` | `text` |

内部使用 Spring Boot Binder，支持宽松属性名（例如 `max-retries` → `maxRetries`）、嵌套对象、record、集合、枚举和 Duration。可在类上加 `@Validated`，使用 Jakarta Validation 约束。

同样支持将 `payment.rules` 或整个 `payment` 存成一个 `format=json` 的配置条目，自动展开对象与集合；具体属性条目优先覆盖上级 JSON 中的同名值。

配置变化后销毁旧目标实例，下次调用代理时重新绑定；已通过构造器注入配置类的单例服务继续持有同一个代理。属性缺失时回退到本地 `application.yml`，再使用字段默认值。删除配置后也会重新创建实例，避免残留旧属性。

`prefix` 是属性名前缀，`group` 是存储分组，默认 `DEFAULT_GROUP`。需要其他分组时设置 `@ConfigProperties(prefix = "payment", group = "checkout")`，该分组中的属性键仍为 `payment.*`。不同前缀或分组的变化不会刷新此 Bean。

默认使用类代理，配置类和需要代理调用的方法需可覆写。final 实现可提供业务接口，设置 `proxyMode = ScopedProxyMode.INTERFACES`，并按接口注入。

### 配置驱动的 SDK / 业务 Bean

需要在配置变化时重建一个资源 Bean 时，继续用 `@ConfigBean` 声明它依赖的存储条目。例如 `payment.rules` 作为一个 JSON 条目保存：

```java
@Bean(destroyMethod = "close")
@ConfigBean(keys = "payment.rules")
public PaymentClient paymentClient(PaymentSettings settings) {
    return new PaymentClient(settings.getRules());
}
```

旧实例的 Spring 销毁回调会执行。`keys` 使用实际存储的 `dataId`；若逐个属性保存，则声明相关属性键。已有 `@ConfigBean + @ConfigValue` 写法继续兼容；新配置类优先使用 `@ConfigProperties`。

## 监听变化

方法注解按分组和键订阅；方法必须为 public，支持无参数方法或一个 `ConfigChangeEvent` 参数的方法。事件中的键是发布条目的 `dataId`，JSON 文档内部字段不会额外生成变更键。同一发布同时修改多个匹配键时，该方法调用一次。

```java
@Component
@RequiredArgsConstructor
public class PaymentListener {
    private final PaymentSettings settings;

    @ConfigListener(keys = "payment.enabled")
    public void onPaymentChanged(ConfigChangeEvent event) {
        // 此时 settings.isEnabled() 会读取新配置。
    }
}
```

实现 `ConfigChangeListener` 并注册为 Spring Bean，可监听当前应用的全部配置变化；多个 Bean 按 `@Order` 分发。

```java
@Bean
public ConfigChangeListener configAuditListener() {
    return event -> recordChangedKeys(event.getChangedKeys());
}
```

也可在运行时订阅并取消：

```java
final ConfigSubscriptionHandler subscription = config.group("payment")
        .subscribe("enabled", event -> reloadPaymentPolicy());
subscription.close();
```

内容或格式实际变化、新增和删除都会通知；重复同步相同快照不通知。单个监听器失败会记录错误并继续通知其他监听器。监听器在配置缓存更新和相关 Bean 失效后执行，回调中可读取新值。

## 编辑、发布与回滚

接口返回继续使用项目的 `RestResponse`，预期业务失败由 `Either` 折叠为失败响应。

| 操作 | 接口 | 请求内容 |
| --- | --- | --- |
| 创建应用 | `POST /api/config/admin/app` | `{"appCode":"shop","appName":"Shop"}` |
| 创建草稿 | `POST /api/config/admin/config` | `{"appCode":"shop","dataId":"enabled","group":"payment","content":"true","format":"text"}` |
| 修改草稿 | `PUT /api/config/admin/config/{id}` | `{"content":"false"}` |
| 查看编辑内容 | `GET /api/config/admin/config/list/{appCode}` | — |
| 发布所选配置 | `POST /api/config/admin/publish` | `{"appCode":"shop","configIds":[1],"releaseNote":"启用支付"}` |
| 回滚上一份发布快照 | `POST /api/config/admin/rollback` | `{"appCode":"shop"}` |
| 标记删除 | `DELETE /api/config/admin/config/{id}` | — |
| 拉取已发布配置 | `GET /api/config/fetch?appCode=shop` | 可加 `dataIds` 过滤 |
| 拉取单个已发布配置 | `GET /api/config/fetch/{dataId}?appCode=shop&group=payment` | — |

编辑内容和已发布内容分开保存。部分发布会合并到上一份完整快照，其余配置继续沿用已发布值。删除先形成待发布标记，随后发布该配置 ID 才会从有效快照移除。回滚生成新的发布记录并恢复上一份快照，同时通知客户端；编辑区内容保留。

发布集合中不存在或属于其他应用的 ID 会使整个请求失败。

## 同步与扩展配置

| `config.client` 属性 | 默认值 | 用途 |
| --- | --- | --- |
| `enabled` | `true` | 客户端开关；同时需要 `app-code` |
| `default-group` | `DEFAULT_GROUP` | `ConfigTemplate` 初始视图的分组 |
| `refresh-enabled` | `true` | 自动初始获取及后台同步；关闭后可从磁盘或应用事件更新 |
| `long-poll-timeout-ms` | `30000` | 远程长轮询超时，范围 1000–60000 |
| `connect-timeout-ms` | `3000` | HTTP 建连超时 |
| `request-timeout-ms` | `5000` | 普通获取超时；长轮询读取超时额外加上长轮询时间 |
| `retry-delay-ms` | `1000` | 远程同步两次尝试之间的间隔 |
| `refresh-interval-ms` | `30000` | 使用本地数据源时的定期校准间隔 |
| `cache-dir` | `${user.home}/.config-cache` | 应用快照持久化目录 |

缓存按分组和键保存，使用 Jackson 编解码。存在磁盘缓存也会尝试获取当前快照；获取失败保留原值，后台继续尝试。完整快照替换同时处理删除和空配置集。

宿主可贡献有序的 `ConfigHttpClientCustomizer` Bean，为组件持有的 HTTP 客户端添加认证拦截器等配置。组件负责 HTTP 超时和客户端创建。

保留旧 `ConfigClientService` 入口：单个配置仍支持指定分组；旧批量接口固定读取 `DEFAULT_GROUP`。传入其他 `appCode` 会明确报错。新业务使用 `ConfigTemplate`。

`@ConfigBean` 使用独立的业务作用域。兼容旧 Spring Cloud 刷新桥时，可开启 `refresh-scope-enabled`；只有宿主存在 `ContextRefresher` Bean 时才装配该桥。

## Native Image 与检查

`@ConfigProperties` 配置类及其嵌套类型、旧注解字段和监听方法由 AOT 处理器注册反射提示。直接通过 `opsForObject()` 读取的业务类型需由宿主声明 `@RegisterReflectionForBinding`。

```shell
./gradlew :backend:components:config-component:client-config-component:check \
  :backend:components:config-component:server-config-component:check \
  :backend:components:config-component:jpa-config-component:check \
  :backend:components:config-component:jdbc-config-component:check
```

检查包含自定义存储测试、JPA / JDBC 互读及事务测试、H2（含 PostgreSQL 模式）集成测试和 JaCoCo 行覆盖率门槛（80%）。实体、命令模型、生成映射及 AOT 元数据不计入业务代码覆盖率。
