# 日志模块 (log-component)

操作日志记录组件，通过 `@Log` 注解自动记录用户操作日志，支持 **JPA 持久化** 和 **消息队列分发** 两种输出方式。

---

## 模块结构

```
log-component/
├── api-log-component/            # API 模块（必选）
│   ├── @Log                      # 操作日志注解
│   ├── OperLogEvent              # 操作日志事件
│   ├── LoginInfoEvent            # 登录日志事件
│   ├── BusinessType              # 业务操作类型枚举
│   ├── BusinessStatus            # 操作状态枚举
│   └── OperatorType              # 操作人类别枚举
│
├── core-log-component/           # 核心模块（必选）
│   ├── LogMethodInterceptor      # @Log AOP 拦截器
│   ├── LogHandler / DefaultLogHandler    # 日志处理器 → 发布事件
│   ├── LogRecordFactory          # OperLogEvent 构建工厂
│   ├── UserResolver / DefaultUserResolver          # 用户解析
│   ├── IpResolver / ServletIpResolver              # IP 解析
│   ├── RequestResolver / ServletRequestResolver    # 请求解析
│   └── RequestParameterResolver / Default...       # 参数序列化
│
├── jpa-log-component/            # JPA 持久化（可选：单服务常用）
│   ├── LogEntity                 # sys_oper_log 实体
│   ├── LogRepository             # JPA Repository
│   └── LogEventPersistenceListener  # 事件监听 → 存库
│
└── message-log-component/        # 消息队列（可选：多服务）
    ├── MessageLogProducer        # 本地事件 → 发送到 MQ
    ├── MessageLogConsumer        # 从 MQ 接收 → 重新发布
    └── MessageLogMessageHandler  # 桥接 MessageHandler 接口
```

---

## 快速开始

### 1. 引入依赖

```kotlin
// build.gradle.kts

// 单服务模式（核心 + JPA 持久化）
implementation(project(":backend:components:log-component:core-log-component"))
implementation(project(":backend:components:log-component:jpa-log-component"))

// 多服务模式（核心 + 消息队列）
implementation(project(":backend:components:log-component:core-log-component"))
implementation(project(":backend:components:log-component:message-log-component"))
// 选择一个消息实现
implementation(project(":backend:components:message-component:redis-message-component"))
```

> `api-log-component` 会被 `core-log-component` 自动传递依赖，无需手动引入。

### 2. 使用 `@Log` 注解

```java
@RestController
@RequestMapping("/system/user")
public class UserController {

    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysUser user) {
        // 业务逻辑...
        return success();
    }

    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return success();
    }

    @Log(title = "用户管理", businessType = BusinessType.UPDATE,
         isSaveRequestData = false)   // 不保存请求参数
    @PutMapping
    public AjaxResult edit(@RequestBody SysUser user) {
        return success();
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT,
         isSaveResponseData = false)  // 不保存响应结果
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        // 导出逻辑...
    }
}
```

### 3. 查看自动保存的日志

引入 `jpa-log-component` 后，日志会自动写入 `sys_oper_log` 表。

### 4. 多服务场景

```yaml
# 业务服务 application.yml（仅发送到消息队列）
log.message.producer-enabled: true      # 默认 true
log.message.consumer-enabled: false     # 默认 false

# 日志聚合服务 application.yml（接收并保存到 DB）
log.message.producer-enabled: false
log.message.consumer-enabled: true
```

---

## @Log 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `title` | `String` | `""` | 操作模块名称（如"用户管理"） |
| `businessType` | `BusinessType` | `OTHER` | 业务操作类型 |
| `operatorType` | `OperatorType` | `MANAGE` | 操作人类别 |
| `isSaveRequestData` | `boolean` | `true` | 是否保存请求参数 |
| `isSaveResponseData` | `boolean` | `true` | 是否保存响应结果 |
| `excludeParamNames` | `String[]` | `{}` | 排除的请求参数名 |

### BusinessType 枚举

| 值 | 说明 |
|---|---|
| `OTHER` | 其它 |
| `INSERT` | 新增 |
| `UPDATE` | 修改 |
| `DELETE` | 删除 |
| `GRANT` | 授权 |
| `EXPORT` | 导出 |
| `IMPORT` | 导入 |
| `FORCE` | 强退 |
| `CLEAN` | 清空 |

### OperatorType 枚举

| 值 | 说明 |
|---|---|
| `OTHER` | 其它 |
| `MANAGE` | 后台用户 |
| `MOBILE` | 手机端用户 |

---

## OperLogEvent 事件字段

`DefaultLogHandler` 将拦截到的日志信息封装为 `OperLogEvent`，通过 `ApplicationEventPublisher` 发布。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `operId` | `Long` | 主键 | 自动生成 |
| `tenantId` | `String` | 租户ID | `UserContextHolder` |
| `title` | `String` | 操作模块 | `@Log.title` |
| `businessType` | `Integer` | 业务类型 | `@Log.businessType` |
| `method` | `String` | 请求方法全名 | AOP 拦截 |
| `requestMethod` | `String` | HTTP 方法 | `ServletRequestResolver` |
| `operatorType` | `Integer` | 操作类别 | `@Log.operatorType` |
| `operName` | `String` | 操作人员 | `UserResolver` |
| `deptName` | `String` | 部门名称 | 可扩展 |
| `operUrl` | `String` | 请求 URL | `ServletRequestResolver` |
| `operIp` | `String` | 操作 IP | `IpResolver` |
| `operLocation` | `String` | 操作地点 | 可扩展 |
| `operParam` | `String` | 请求参数(JSON) | `RequestParameterResolver` |
| `jsonResult` | `String` | 响应结果(JSON) | AOP 拦截 |
| `status` | `Integer` | 0正常 / 1异常 | 自动判断 |
| `errorMsg` | `String` | 错误消息 | 异常捕获 |
| `operTime` | `LocalDateTime` | 操作时间 | 自动 |
| `costTime` | `Long` | 耗时(ms) | AOP 计时 |

---

## 自定义扩展

### 自定义日志处理器

```java
@Component
public class MyLogHandler implements LogHandler {
    @Override
    public void handle(LogInvocation invocation) {
        // 自定义处理逻辑：如发送到 Elasticsearch
        OperLogEvent event = logRecordFactory.create(invocation);
        elasticsearchClient.index(event);
    }
}
```

### 自定义用户解析

```java
@Component
public class MyUserResolver implements UserResolver {
    @Override
    public String currentUser() {
        // 从 Token / Header 中获取用户名
        return MyContextHolder.getUsername();
    }
}
```

> 自定义实现注册为 Spring Bean 后，`@ConditionalOnMissingBean` 会自动替换默认实现。

---

## 数据流架构

### 单服务模式

```
@Log
  ↓
LogMethodInterceptor (AOP)
  ↓
DefaultLogRecordFactory → 构建 OperLogEvent
  ↓
DefaultLogHandler → publishEvent(OperLogEvent)
  ↓
LogEventPersistenceListener (@TransactionalEventListener)
  ↓
LogRepository.save() → sys_oper_log 表
```

### 多服务模式

```
Service A（业务服务）:
@Log → 拦截 → DefaultLogHandler → publish Event
                                      ↓
                               MessageLogProducer → MQ (log.oper topic)

Service B（日志聚合服务）:
MQ → MessageLogMessageHandler (MessageHandler)
      ↓
    MessageLogConsumer → re-publish Event (设置回流标记)
                          ↓
                   LogEventPersistenceListener → DB
```

> 回流标记由 `MessageLogProducer.setFromMessage()` / `isFromMessage()` 控制，
> 防止"收到 → 转发 → 再收到 → 再转发"的无限循环。

---

## 数据库表结构

```sql
CREATE TABLE sys_oper_log (
    oper_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id      VARCHAR(20),
    title          VARCHAR(50)    COMMENT '操作模块',
    business_type  INT            COMMENT '业务类型',
    method         VARCHAR(200)   COMMENT '方法名',
    request_method VARCHAR(10)    COMMENT '请求方式',
    operator_type  INT            COMMENT '操作类别',
    oper_name      VARCHAR(50)    COMMENT '操作人员',
    dept_name      VARCHAR(50)    COMMENT '部门名称',
    oper_url       VARCHAR(255)   COMMENT '请求URL',
    oper_ip        VARCHAR(128)   COMMENT '主机地址',
    oper_location  VARCHAR(255)   COMMENT '操作地点',
    oper_param     TEXT           COMMENT '请求参数',
    json_result    TEXT           COMMENT '返回参数',
    status         INT            COMMENT '操作状态',
    error_msg      TEXT           COMMENT '错误消息',
    oper_time      DATETIME       COMMENT '操作时间',
    cost_time      BIGINT         COMMENT '消耗时间(ms)'
);
```

> 若使用 Liquibase / Flyway，可在项目中添加此表结构变更脚本。

---

## GraalVM Native Image 支持

- `LogRuntimeHints` 已注册 `@Log` 注解和 `LogOperation` 的反射提示
- `core-log-component` 使用 `@ConditionalOnWebApplication` 控制 Servlet 组件，非 Web 环境自动跳过
- 所有支持组件均可通过 `@ConditionalOnMissingBean` 自定义替换
