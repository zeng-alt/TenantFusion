# Spring Boot 4.1.2 后端升级验证

## 当前结果（升级与验证完成）

分支：`feat/zeng-alt-2026-11-12-boot-4.1.2-upgrade`；基于 `685ed85`。
正式项目依赖下的全后端 main/test 编译与 check 通过，两个应用已打包。
827 项测试通过，失败、错误、跳过均为 0，覆盖 128 个测试 suite；包括 23 项隔离 Docker 回归（登录/安全 9 项、数据权限 13 项、完整 workflow 宿主启动 1 项）。
ABAC 四个模块的 JaCoCo 80% 门槛通过，admin 主应用和测试 AOT 生成与编译通过。
当前盘点为 87 个后端项目、1012 个主源码和 136 个测试 Java 文件；全部 168 个 main/test 编译任务已纳入正式检查。
Native 使用 GraalVM CE 25.5.5.0.3（JDK 25.0.5.0.3、macOS ARM64）完成 quick-build，
10 项真实 HTTP 烟测通过：匿名拒绝、无验证码拒绝、两次验证码 PNG、真实登录、授权访问、篡改令牌拒绝、
停用账户拒绝、重新启用后登录、撤销缓存令牌拒绝。未重复执行完整优化模式构建。
实际 Native 二进制路径和检查结果记录在 `native-smoke-results.json`，诊断用 JVM agent 结果不计为 Native 通过。
这些结果来自普通 Gradle 项目构建，不再使用占位 JAR 探测来判定通过。
存量 JSON/Security 文件已有 Tab 缩进，与全局默认不同，本轮沿用邻居格式；新增 Java 使用 4 空格。

| 范围 | 最终修改 |
| --- | --- |
| 平台 | Boot 4.1.2、Framework 7.0.10、Security 7.1.2、Hibernate 7.4.15.Final、Modulith 2.1.2、Springdoc 3.2.0 |
| Cloud / Redis / JPA helper | Cloud 2025.1.3（Context/OpenFeign 5.0.3）、Redisson 4.8.0 + spring-data-41、JPA helper 3.6.4；未解析依赖为 0 |
| Boot 模块 | 自动配置、Hibernate、Liquibase、缓存、MVC/WebFlux、HTTP codec/converter 与测试模块迁移；Excel 保留可选 Web 栈依赖 |
| JSON | 保留已有 Jackson 2 公共 API，使用 Boot 4 官方兼容模块；组件属性默认值指定 MVC/WebFlux mapper，宿主仍可覆盖 |
| Security | 统一迁移 `authorize`、通配 Supplier 和 Reactive `AuthorizationResult`；保留自定义授权结果及 OR/AND 短路规则 |
| RBAC | 支持新版本谓词 Visitor，方法与路径继续作为权限键 |
| Operaton | 2.2.0 引擎、模型、插件、身份 SPI、委派、配置键；BPMN 的 `getCamunda*` 访问器迁移为 `getOperaton*`，既有业务 DTO/包名保留 |
| Holunda | 远程 REST 使用 Boot 4 专用 `starter-feign-4`，去掉重复 Feign 扫描；嵌入式变量读取直接使用 Operaton API |
| Modulith 数据库 | 新增 H2/PG V0023 增量迁移，添加状态、完成尝试次数和重提交时间；按旧 completion_date 回填状态，保留事件 |
| AOT | Advisor 延迟取得拦截器，避免分析切点时提前初始化数据库；关闭不支持 Vavr Option 的 JPA 查询源码优化，保留其余 AOT 提示和处理 |
| Native | 插件 1.1.14，解除旧 metadata 0.3.15 锁定；明确选择 GraalVM 25 工具链，业务源码仍为 Java 21；固定元数据 1.1.18 和 Netty 稳定条目 |

新增/扩充回归包括：Jackson 双栈 HTTP 和业务格式、Servlet/Reactive 版本路由、Security 自定义结果与短路、
真实 Operaton 启动及旧 BPMN 租户流程/Spin JSON 变量/身份查询、双命名空间表单、远程 Feign REST 解码、
Modulith 旧事件升级/重复执行/回滚、Advisor 延迟装配、完整 workflow 宿主启动及 Native 可达性。
实际 HTTP 测试另发现 Excel 的懒上传流在 WebFlux 事件线程上打开，已将外层订阅调度到 RxJava IO，现有 9 项 WebFlux 回归通过。

### 兼容与部署约束

- Jackson 2 兼容模块已被 Boot 标为废弃；本次为保留既有对外类型及序列化契约而明确保留，**没有宣称完成 Jackson 3 业务迁移**。
- Hibernate 增强插件和运行时共同使用 7.4.15.Final，因为 Boot 默认 7.4.13 的对应增强插件未发布。
- JPA AOT 查询源码生成会为 `Option<Permission>` 生成错误返回类型，故通过官方 `spring.aot.jpa.repositories.enabled=false` 关闭此项优化；Vavr 查询契约及 AOT 反射提示保留。
- 部署前需执行应用 H2/PG V0023 迁移，以及 Operaton 7.24 → 7.25 的版本记录脚本；本轮只操作隔离测试数据库。
- `TaskLoggingListener.BUSINESS_KEY` 不再暴露 Holunda 的 VariableFactory，改为原生变量键字符串；仓库中没有该常量的调用方。

### Native 中额外发现并处理的问题

- 独立租户库由自建 Liquibase 执行器装配，Native 下动态 `includeAll` 枚举失败。
  改为显式包含三个租户 SQL，并保留原路径、author 和 id；H2/PG 解析回归核对已有 changeset 身份。
  自建迁移资源由 `TenantStoreRuntimeHints` 注册。
- Native 元数据默认快照为未覆盖的 Netty 4.2.19 回退到了 5 alpha 条目。
  [上游诊断](https://github.com/oracle/graalvm-reachability-metadata/issues/11228)
  进一步确认 `java.nio.Bits.UNALIGNED` 的条件匿名类编号不匹配。
  固定正式元数据 ZIP 1.1.18，明确选择 Netty common/transport 的 4.2.1、resolver 的 4.1.80、codec 的 4.1.105 条目；
  `RedissonRuntimeHints` 只补充缺失的 UNALIGNED 字段。运行库保持 Netty 4.2.19，没有降级。
- 元数据映射调整后，Logback 默认配置器还缺少反射构造提示；`LoggingRuntimeHints` 注册该构造器。
  最终原生程序已走通启动、真实验证码和登录链路。
- AOT 的条件装配在构建时固定，运行时关闭验证码不会删除已有过滤器。
  烟测保持验证开启，通过真实 PNG/Cookie 流程，并只从本次隔离 Redis 读取挑战答案。

### Native 验证入口

```bash
# 安装 GraalVM 25，并让 Gradle 工具链能发现它；常规 Java 编译仍使用 21。
./gradlew -Porg.gradle.java.installations.paths=/path/to/graalvm25 :backend:admin:nativeCompile
python3 scripts/verify-backend-native.py /path/to/admin-native-executable
```

Native 脚本创建独立 PostgreSQL/Redis，生成测试密钥，完成验证后清理进程和容器。
它不连接开发或生产数据库。新增迁移脚本仍需按部署流程在目标库应用。
本轮 quick-build 的临时 init 脚本只改变验证镜像名称和优化级别；项目默认优化设置保留。

### 复现最终检查

```bash
scripts/verify-security-docker.sh --tests '*Test' \
  -I scripts/boot4-upgrade-audit.init.gradle \
  boot4BackendCompile boot4BackendCheck --continue --console=plain

./gradlew -I scripts/boot4-upgrade-audit.init.gradle boot4DependencyInventory
```

需要 Docker；脚本创建临时 PostgreSQL/Redis，随机绑定本机端口并在退出时清理。
`boot4BackendCheck` 汇总所有后端 check 及两个 bootJar；常规回归按仓库 `*Test` 约定，手动 SQL 导出器不作为回归执行。
本机 Java TLS 下载异常，实际检查另外加载临时 `mirror.init.gradle`，Native 元数据 ZIP 通过 `metadata-local.init.gradle` 指向已下载的正式构件。
未关闭上游 HTTPS 证书校验，未把本机 Maven 转发地址写入仓库；Native 元数据的正式 URL 为 Maven Central。
最终日志：`build/reports/boot4-upgrade/check-docker-final.log`；逐项目依赖：`dependencies.json`。
Native 日志：`native-build-5.log`、`native-smoke.log`；结果：`native-smoke-results.json`。
日志和 JSON 被 Git 忽略；本报告、审计脚本和回归测试保留可审查证据。

---

## 以下为第一轮试编译记录（历史结果，问题已按上表处理）

分支：`feat/zeng-alt-2026-11-12-boot-4.1.2-upgrade`；起点：`685ed85`。

## 范围与上下文

这是升级试编译分支，目标是确认兼容构件、实际编译错误和后续改动顺序，不代表完成迁移。
用户确认仅使用社区版，随后指定改用 Operaton 支持 Boot 4.1.x。
无关的 `frontend/pnpm-workspace.yaml` 保持原样。

| 文件或模块 | 职责 | 本轮处理 |
| --- | --- | --- |
| `gradle/libs.versions.toml` | 公共版本目录 | 设置 Boot 4.1.2、对应 Hibernate 增强插件、Modulith 与 Native 插件基线 |
| `build.gradle.kts` | 所有后端模块的 BOM | 移除 Boot 3.5 补丁覆盖，使用 Boot 4.1.2 和 Springdoc 3.x 基线 |
| `scripts/boot4-upgrade-audit.init.gradle` | 诊断入口 | 枚举全部后端模块，试编译 main/test；不执行测试、前端或 Native 构建 |
| `embedded-engine-component`、`api-camunda-component`、`workflow-module`、`workflow` | 引擎与工作流 | 候选依赖切为 Operaton 2.2.0；保留组件目录和宿主公开包名，收集 Java API 迁移点 |
| `json`、`security`、`domain`、`tenant`、`storage`、`rest-apt` | 直接依赖框架 API 的公共组件 | 收集实际错误及下游受阻范围，暂不批量改写业务源码 |

关联验证包括现有 Security、ABAC、数据权限、多租户、JSON 与工作流测试源码。
本轮只编译，不运行数据库迁移，不连接现有开发数据库，不启动业务服务。
已有 Spring Boot 3.5 测试通过记录不能用作 Boot 4.1 验证结果。

## Camunda 构件确认

- [官方兼容表](https://docs.camunda.org/manual/7.24/user-guide/spring-boot-integration/version-compatibility/)：
  7.24.0–7.24.2 对应 Boot 3.5；7.24.10+ 维护版本开始支持 Boot 4.1；
  7.24.3–7.24.16 的 Boot 4 集成使用 `-4` 后缀 Starter，7.24.17+ 又改回原名。
- 实际查询 [Maven Central 元数据](https://repo.maven.apache.org/maven2/org/camunda/bpm/springboot/camunda-bpm-spring-boot-starter/maven-metadata.xml)：
  社区版最新正式版为 `7.24.0`；`camunda-bpm-spring-boot-starter-4` 元数据及
  `7.24.10` / `7.24.10-ee` 的所查 POM 均返回 404。
- 官方企业仓库对所查兼容 POM 返回 401，公开 Central 返回 404。
  因用户仅使用社区版，不配置企业仓库，不写入企业版依赖。

公开构件检查记录：`build/reports/boot4-upgrade/artifact-probes.json`。

## 选定的 Operaton 路线

[Operaton 2.2 发布说明](https://docs.operaton.org/docs/documentation/reference/release-notes/2_2/)
明确使用 Spring Boot 4.1，发布时基线为 4.1.2。实际从 Central 取得以下 2.2.0 POM，均返回 200：

- `org.operaton.bpm:operaton-engine`
- `org.operaton.bpm.springboot:operaton-bpm-spring-boot-starter`
- `org.operaton.bpm.springboot:operaton-bpm-spring-boot-starter-webapp`
- `org.operaton.bpm.springboot:operaton-bpm-spring-boot-starter-rest`

记录：`build/reports/boot4-upgrade/operaton-probes.json`。
版本集中在 `libs.versions.toml` 的 `operaton` 条目。已将四个构建文件的直接引擎、模型、Connect、Spin 依赖切换到 Operaton。
为了避免 Holunda 等依赖把旧引擎间接带回，诊断基线排除了旧 Camunda 引擎的相关依赖组。
这不是完整迁移：Java 导入、方法改名、配置前缀和第三方适配仍待编译与运行验证。

[官方 Camunda 迁移指引](https://docs.operaton.org/docs/documentation/update/camunda-to-operaton/)
提供 OpenRewrite 配方和命名映射；其中通用的“无需数据转换”不能替代目标版本的升级说明。
Operaton 2.2 的发布说明要求记录 schema 7.25 的升级步骤。本轮不运行迁移脚本、不连接业务数据库。

## 构建配置阶段的实际发现

1. Boot 4.1.2 BOM 默认 Hibernate 7.4.13.Final 的核心 POM 存在，但对应 Gradle 插件 marker 和实现 POM 返回 404。
   插件 Portal 元数据列出了 7.4.10、7.4.15，但缺少 7.4.13。
   因此本分支将增强插件和 Hibernate 运行时共同对齐为已发布的 7.4.15.Final，保留 Boot 4.1.2 其余基线。
2. 本机 Java 下载公开依赖出现 TLS 握手失败；Python 正常 HTTPS 获取同一公开 POM 成功。
   试编译使用仅监听 `127.0.0.1` 的临时 Maven 缓存转发，上游正常校验证书，未关闭 TLS 检查。
   临时服务和本机配置位于 `/private/tmp/tenantfusion-boot4-audit`，未加入项目仓库配置。
3. 原有 Boot 3.5 的 Tomcat、Netty、Jackson 等补丁覆盖已移除，避免旧版本覆盖 Boot 4.1 依赖。
   Springdoc 改为 3.2.0，Modulith 改为 2.1.2，Native 插件候选为 1.1.14。

## 全后端普通编译结果

扫描 87 个后端 Gradle 项目，其中 84 个有 Java 编译任务，共 168 个 main/test 编译任务。
源码为 1009 个 main、126 个测试 Java 文件，共 1135 个。本次普通编译失败，耗时约 4 分钟。

| 状态 | 任务数 | 含义 |
| --- | ---: | --- |
| 已编译 | 33 | 25 个 main、8 个 test；没有执行 JUnit |
| 无源码 | 45 | 不计为有源码模块通过 |
| 直接失败 | 5 | 首批阻塞见下表 |
| 因依赖失败未执行 | 85 | 40 个 main、45 个 test，不能判定兼容 |

| 首批失败模块 | 实际原因 | 后续修改 |
| --- | --- | --- |
| `json-component` | `JsonConfiguration.java:23` 引用旧 `JacksonAutoConfiguration` 包 | 调整 Jackson 模块、导入及自动配置顺序 |
| `api-lock-component` | `spring-boot-starter-aop` 没有 Boot 4 管理版本，依赖未解析 | 按 Boot 4 指引切换 AspectJ Starter，验证锁切面 |
| `api-message-component` | `JacksonMessagePacketCodec.java:3–4` 缺少 Jackson 2 API | 与主 JSON 组件统一编解码器和依赖 |
| `core-tenant-component` | `TenantAutoConfiguration.java:21` 引用旧 Hibernate 定制接口包 | 更新 Boot Hibernate 模块依赖及导入 |
| `embedded-engine-component` | 仍导入 `org.camunda.bpm.*` | 完成 Operaton 命名空间和类型迁移 |

证据：`build/reports/boot4-upgrade/compile-operaton.log`、`task-status.json`、`initial-errors.json`。

## 补充源码探测与修改清单

为继续检查被上游阻断的模块，执行了 168 个独立源码探测任务，发现 27 个失败任务
（21 个 main、6 个 test），诊断涉及 66 个不同源码/测试文件。
**探测借用了已有组件薄 JAR，并宽松解析外部依赖，仅用于暴露更多错误。**
74 个探测编译成功任务不能当成真实迁移通过，67 个无源码任务也不计通过。
测试可能引用旧组件字节码，因此必须在正式编译恢复后复验。
干净检出没有旧 JAR 时，不能期待相同探测结果。

以下按建议处理顺序排列；编译器存在级联错误和单次错误数限制，清单仍可能继续增加。

| 顺序 | 范围 | 需要修改 | 验证重点 |
| --- | --- | --- | --- |
| 1 | 构建依赖 | 替换旧 AOP Starter；统一 Cloud；校准 Redisson、JPA helper、Vavr Jackson | 全部应用 runtimeClasspath，旧适配器不能与新框架混用 |
| 2 | JSON、消息、Redis | Boot Jackson 自动配置迁包；消息编解码不再隐式依赖 starter-json 提供 Jackson 2 | Long、日期、字典、脱敏、Vavr、缓存值兼容性 |
| 3 | core、domain、excel、tenant、log、oss、data-permission、cache | Boot 4 模块拆分及自动配置类型迁包；补齐所属模块依赖和排序注解 | 装配条件、Servlet/WebFlux 隔离、多租户 Hibernate 属性 |
| 4 | security、RBAC、tenant-local、admin | Servlet `check` 改 `authorize`，参数为 `Supplier<? extends Authentication>`；Reactive 返回 `Mono<AuthorizationResult>` | 六项 Security 修复的回归测试、组合授权取消/空值/拒绝行为 |
| 5 | RBAC 路由收集器 | Servlet 与 Reactive 的 `RequestPredicates.Visitor` 新增 `version(String)` | 版本化路由、模板及权限资源收集 |
| 6 | 引擎、workflow-module、workflow | `org.camunda` → `org.operaton`、插件/身份 SPI、BPMN 扩展访问器、配置键和资源；适配 Holunda | 身份/租户查询、部署、表单、变量、委派、REST/Feign |
| 7 | 测试及发布构建 | Boot 测试模块和注解迁移；恢复普通 main/test 编译后执行测试、Docker、AOT、Native | 启动、事务、schema、序列化契约、Native 可达性 |

### 已确认的 Boot 4 类型位置

以下从本次解析到的 4.1.2 JAR 中确认。不能只改 import 而忽略新增模块依赖。

| 类型 | Boot 4 包 / 模块 |
| --- | --- |
| `JacksonAutoConfiguration` | `org.springframework.boot.jackson.autoconfigure` / `spring-boot-jackson` |
| `Jackson2ObjectMapperBuilderCustomizer` | `org.springframework.boot.jackson2.autoconfigure` / `spring-boot-jackson2` |
| `HibernateJpaAutoConfiguration`、`HibernatePropertiesCustomizer` | `org.springframework.boot.hibernate.autoconfigure` / `spring-boot-hibernate` |
| `WebMvcAutoConfiguration` | `org.springframework.boot.webmvc.autoconfigure` / `spring-boot-webmvc` |
| `WebFluxAutoConfiguration` | `org.springframework.boot.webflux.autoconfigure` / `spring-boot-webflux` |
| `ErrorController`、`DefaultErrorAttributes` | `org.springframework.boot.webmvc.error` / `spring-boot-webmvc` |
| `CacheManagerCustomizer` | `org.springframework.boot.cache.autoconfigure` / `spring-boot-cache` |
| `ValidationAutoConfiguration` | `org.springframework.boot.validation.autoconfigure` / `spring-boot-validation` |
| `HttpMessageConvertersAutoConfiguration` | `org.springframework.boot.http.converter.autoconfigure` / `spring-boot-http-converter` |
| `CodecsAutoConfiguration` | `org.springframework.boot.http.codec.autoconfigure` / `spring-boot-http-codec` |
| `ServletWebServerApplicationContext` | `org.springframework.boot.web.server.servlet.context` / `spring-boot-web-server` |

另有 Liquibase 自动配置和 MockMvc 注解的旧位置实际报错，须随测试及自动配置模块迁移。
本轮未把 Hibernate 的 `IdentifierGenerator`、`ConnectionProvider`、`StatementInspector` 全部列为必改：
应依据正式编译和行为验证处理，不能仅凭主版本变化推断签名不兼容。

### 实际依赖落点与待校准项

| 构件 | 本次解析版本 | 判断 |
| --- | --- | --- |
| Boot / Framework / Security | 4.1.2 / 7.0.10 / 7.1.2 | 已进入目标大版本 |
| Hibernate | 7.4.15.Final | 插件与运行时对齐，偏离 Boot 默认值的原因见前文 |
| Operaton Engine | 2.2.0 | 已解析，没有旧 Camunda Engine/Model 混入 |
| Jackson 3 / Jackson 2 databind | 3.1.7 / 2.21.7 | 两套共存，不能证明自定义模块已支持 Jackson 3 |
| Redisson 适配器 / Spring Data Redis | `redisson-spring-data-35:3.52.0` / 4.1.2 | 明确跨代，需要匹配的适配器及 Redisson 版本 |
| Spring Cloud Context | 4.2.0、4.3.2、4.3.3，分散在不同模块 | 需统一至支持 Boot 4.1 的发布列车 |
| OpenFeign Core | 4.3.2、4.3.3 | 结合 Holunda Feign 客户端一起处理 |
| `app.tozzi:jpa-search-helper` | 3.5.5 | 仍为 Boot 3.5 基线，换兼容版本并验证查询 |
| Holunda Data / C7 REST Client | 2026.04.2 | Camunda 构件；排除旧引擎不会修正二进制类型，需要适配或替换 |

Vavr 与 `vavr-jackson` 当前共用 0.10.3 版本变量；迁移 Jackson 3 时须拆开并选择对应模块。
本轮只解析编译依赖，未完成运行时冲突及依赖安全检查；还需审查宿主 BOM 是否覆盖 Operaton 的依赖约束。
逐项目记录见 `build/reports/boot4-upgrade/dependencies.json`，其中唯一未解析依赖为旧 AOP Starter。

### Operaton 迁移检查

可用官方 OpenRewrite 配方生成可审查的代码修改，核对 SPI、BPMN 方法、配置键和 ServiceLoader 内容。
本轮未运行源码自动改写。项目自身 `com.github.zeng.alt.camunda` 包和组件目录属于既有公开面，无需随供应商批量改名。
Holunda 变量 API 和 Feign 客户端须分别验证，不能仅排除传递依赖就算迁移完成。

目标 2.2 将 schema 7.24 记为 7.25；官方说明不改变表、列和索引，但需执行数据库版本记录脚本或 Liquibase changelog。
保留 `ACT_` 表和旧 BPMN 扩展兼容能力，并不等于省略该步骤。
流程、历史数据及执行中实例应在数据库副本上验证。本轮未启动引擎或修改数据库。

## 可重复执行与验证边界

```bash
./gradlew -I scripts/boot4-upgrade-audit.init.gradle \
  boot4BackendInventory boot4BackendCompile --continue --console=plain

./gradlew -I scripts/boot4-upgrade-audit.init.gradle \
  boot4DependencyInventory --console=plain

# 仅补充诊断；依赖当前工作区已有薄 JAR，不能作为通过凭据。
./gradlew -I scripts/boot4-upgrade-audit.init.gradle \
  boot4BackendSourceProbe --continue --console=plain
```

本机实际执行时另加载 `/private/tmp/tenantfusion-boot4-audit/mirror.init.gradle` 绕过 Java TLS 下载故障。
正常网络环境无需该临时文件。`--continue` 仍会跳过依赖失败的下游；修复上游后必须重跑普通编译。
日志和 JSON 位于被 Git 忽略的 `build/reports/boot4-upgrade`，不会随分支提交；
本报告保留主要发现，审计任务可重新生成项目和依赖清单。

本次已完成 Operaton 候选依赖切换及全后端试编译。普通编译尚未通过，
JUnit、Docker、启动、AOT、Native 均未在 Boot 4 下验证。
当前只修改构建配置、审计脚本和报告，未修改业务 Java 源码，未提交升级分支。

## 附录：补充探测失败任务与文件

列出每个文件首次诊断位置，不按级联报错数估算工作量。
测试诊断可能受旧占位 JAR 影响，最终以恢复后的正式编译和测试为准。

### `:backend:admin:boot4ProbeMain`

- [admin/src/main/java/com/github/zeng/alt/admin/infrastructure/security/AdminAbacAuthorizationConstraint.java](../backend/admin/src/main/java/com/github/zeng/alt/admin/infrastructure/security/AdminAbacAuthorizationConstraint.java#L26)

### `:backend:admin:boot4ProbeTest`

- [admin/src/test/java/com/github/zeng/alt/admin/infrastructure/security/AdminSecurityDockerTest.java](../backend/admin/src/test/java/com/github/zeng/alt/admin/infrastructure/security/AdminSecurityDockerTest.java#L25)
- [admin/src/test/java/com/github/zeng/alt/admin/infrastructure/security/UserDataPermissionDockerTest.java](../backend/admin/src/test/java/com/github/zeng/alt/admin/infrastructure/security/UserDataPermissionDockerTest.java#L19)

### `:backend:components:camunda-component:api-camunda-component:boot4ProbeMain`

- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaGroupQuery.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaGroupQuery.java#L3)
- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaIdentityProvider.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaIdentityProvider.java#L3)
- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaIdentityProviderFactory.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaIdentityProviderFactory.java#L3)
- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaIdentityProviderPlugin.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaIdentityProviderPlugin.java#L3)
- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaTenantQuery.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaTenantQuery.java#L3)
- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaUserQuery.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/CamundaUserQuery.java#L3)
- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/config/CamundaIdentityAutoConfiguration.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/config/CamundaIdentityAutoConfiguration.java#L7)
- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/security/CamundaIdentityAuthenticationFilter.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/security/CamundaIdentityAuthenticationFilter.java#L8)
- [components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/security/CamundaIdentitySecurityAutoConfiguration.java](../backend/components/camunda-component/api-camunda-component/src/main/java/com/github/zeng/alt/camunda/identity/api/security/CamundaIdentitySecurityAutoConfiguration.java#L5)

### `:backend:components:camunda-component:api-camunda-component:boot4ProbeTest`

- [components/camunda-component/api-camunda-component/src/test/java/com/github/zeng/alt/camunda/identity/api/CamundaIdentityProviderTest.java](../backend/components/camunda-component/api-camunda-component/src/test/java/com/github/zeng/alt/camunda/identity/api/CamundaIdentityProviderTest.java#L56)

### `:backend:components:camunda-component:embedded-engine-component:boot4ProbeMain`

- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/correlation/EmbeddedCorrelationApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/correlation/EmbeddedCorrelationApi.java#L5)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/correlation/EmbeddedSignalApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/correlation/EmbeddedSignalApi.java#L5)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/decision/EmbeddedEvaluateDecisionApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/decision/EmbeddedEvaluateDecisionApi.java#L6)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/deploy/EmbeddedDeploymentApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/deploy/EmbeddedDeploymentApi.java#L7)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/history/EmbeddedHistoryApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/history/EmbeddedHistoryApi.java#L10)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/listener/TaskLoggingListener.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/listener/TaskLoggingListener.java#L5)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/process/EmbeddedProcessInstanceApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/process/EmbeddedProcessInstanceApi.java#L7)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/process/EmbeddedStartProcessApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/process/EmbeddedStartProcessApi.java#L9)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/repository/EmbeddedProcessDefinitionApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/repository/EmbeddedProcessDefinitionApi.java#L7)
- [components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/task/EmbeddedTaskApi.java](../backend/components/camunda-component/embedded-engine-component/src/main/java/com/github/zeng/alt/camunda/engine/embedded/task/EmbeddedTaskApi.java#L8)

### `:backend:components:core-component:boot4ProbeMain`

- [components/core-component/src/main/java/com/github/zeng/alt/core/advice/CustomErrorAttributes.java](../backend/components/core-component/src/main/java/com/github/zeng/alt/core/advice/CustomErrorAttributes.java#L4)
- [components/core-component/src/main/java/com/github/zeng/alt/core/advice/CustomErrorController.java](../backend/components/core-component/src/main/java/com/github/zeng/alt/core/advice/CustomErrorController.java#L6)

### `:backend:components:domain-component:boot4ProbeMain`

- [components/domain-component/src/main/java/com/github/zeng/alt/domain/config/DomainAutoConfiguration.java](../backend/components/domain-component/src/main/java/com/github/zeng/alt/domain/config/DomainAutoConfiguration.java#L22)

### `:backend:components:excel-component:boot4ProbeMain`

- [components/excel-component/src/main/java/com/github/zeng/alt/excel/config/ExcelAutoConfiguration.java](../backend/components/excel-component/src/main/java/com/github/zeng/alt/excel/config/ExcelAutoConfiguration.java#L18)
- [components/excel-component/src/main/java/com/github/zeng/alt/excel/config/ExcelWebFluxAutoConfiguration.java](../backend/components/excel-component/src/main/java/com/github/zeng/alt/excel/config/ExcelWebFluxAutoConfiguration.java#L13)
- [components/excel-component/src/main/java/com/github/zeng/alt/excel/config/ExcelWebMvcAutoConfiguration.java](../backend/components/excel-component/src/main/java/com/github/zeng/alt/excel/config/ExcelWebMvcAutoConfiguration.java#L15)

### `:backend:components:excel-component:boot4ProbeTest`

- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelAutoConfigurationTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelAutoConfigurationTest.java#L13)
- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelErrorPolicyTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelErrorPolicyTest.java#L12)
- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelI18nHeadTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelI18nHeadTest.java#L8)
- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelPropertiesTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelPropertiesTest.java#L11)
- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelValidationTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelValidationTest.java#L17)
- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelWebFluxIntegrationTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelWebFluxIntegrationTest.java#L13)
- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelWebIntegrationTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelWebIntegrationTest.java#L16)
- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelWebStackConditionTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelWebStackConditionTest.java#L15)
- [components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelWithoutRxJavaTest.java](../backend/components/excel-component/src/test/java/com/github/zeng/alt/excel/ExcelWithoutRxJavaTest.java#L13)

### `:backend:components:json-component:boot4ProbeMain`

- [components/json-component/src/main/java/com/github/zeng/alt/json/JsonConfiguration.java](../backend/components/json-component/src/main/java/com/github/zeng/alt/json/JsonConfiguration.java#L23)

### `:backend:components:log-component:jpa-log-component:boot4ProbeMain`

- [components/log-component/jpa-log-component/src/main/java/com/github/zeng/alt/log/jpa/config/JpaLogAutoConfiguration.java](../backend/components/log-component/jpa-log-component/src/main/java/com/github/zeng/alt/log/jpa/config/JpaLogAutoConfiguration.java#L15)

### `:backend:components:message-component:api-message-component:boot4ProbeMain`

- [components/message-component/api-message-component/src/main/java/com/github/zeng/alt/message/codec/JacksonMessagePacketCodec.java](../backend/components/event-component/api-event-component/src/main/java/com/github/zeng/alt/event/support/EventCodec.java)

### `:backend:components:oss-component:jpa-oss-component:boot4ProbeMain`

- [components/oss-component/jpa-oss-component/src/main/java/com/github/zeng/alt/oss/jpa/config/JpaOssAutoConfiguration.java](../backend/components/oss-component/jpa-oss-component/src/main/java/com/github/zeng/alt/oss/jpa/config/JpaOssAutoConfiguration.java#L17)

### `:backend:components:oss-component:jpa-oss-component:boot4ProbeTest`

- [components/oss-component/jpa-oss-component/src/test/java/com/github/zeng/alt/oss/jpa/OssFileControllerTest.java](../backend/components/oss-component/jpa-oss-component/src/test/java/com/github/zeng/alt/oss/jpa/OssFileControllerTest.java#L10)

### `:backend:components:security-component:core-security-component:boot4ProbeMain`

- [components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/reactive/CompositeReactiveAuthorizationManager.java](../backend/components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/reactive/CompositeReactiveAuthorizationManager.java#L18)
- [components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/reactive/ReactiveWhiteListAuthorizationManager.java](../backend/components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/reactive/ReactiveWhiteListAuthorizationManager.java#L27)
- [components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/reactive/config/ReactiveSecurityAutoConfiguration.java](../backend/components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/reactive/config/ReactiveSecurityAutoConfiguration.java#L89)
- [components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/web/CompositeAuthorizationManager.java](../backend/components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/web/CompositeAuthorizationManager.java#L20)
- [components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/web/WhiteListAuthorizationManager.java](../backend/components/security-component/core-security-component/src/main/java/com/github/zeng/alt/security/core/web/WhiteListAuthorizationManager.java#L22)

### `:backend:components:security-component:core-security-component:boot4ProbeTest`

- [components/security-component/core-security-component/src/test/java/com/github/zeng/alt/security/core/reactive/CompositeReactiveAuthorizationManagerTest.java](../backend/components/security-component/core-security-component/src/test/java/com/github/zeng/alt/security/core/reactive/CompositeReactiveAuthorizationManagerTest.java#L43)

### `:backend:components:security-component:data-permission-security-component:jpa-data-permission-security-component:boot4ProbeMain`

- [components/security-component/data-permission-security-component/jpa-data-permission-security-component/src/main/java/com/github/zeng/alt/security/data/jpa/DataPermissionJpaAutoConfiguration.java](../backend/components/security-component/data-permission-security-component/jpa-data-permission-security-component/src/main/java/com/github/zeng/alt/security/data/jpa/DataPermissionJpaAutoConfiguration.java#L9)

### `:backend:components:security-component:rbac-security-component:rbac-client-security-component:boot4ProbeMain`

- [components/security-component/rbac-security-component/rbac-client-security-component/src/main/java/com/github/zeng/alt/security/rbac/client/collector/ReactiveRouteTemplateCollector.java](../backend/components/security-component/rbac-security-component/rbac-client-security-component/src/main/java/com/github/zeng/alt/security/rbac/client/collector/ReactiveRouteTemplateCollector.java#L141)
- [components/security-component/rbac-security-component/rbac-client-security-component/src/main/java/com/github/zeng/alt/security/rbac/client/collector/ServletRouteTemplateCollector.java](../backend/components/security-component/rbac-security-component/rbac-client-security-component/src/main/java/com/github/zeng/alt/security/rbac/client/collector/ServletRouteTemplateCollector.java#L137)

### `:backend:components:security-component:rbac-security-component:rbac-serve-security-component:boot4ProbeMain`

- [components/security-component/rbac-security-component/rbac-serve-security-component/src/main/java/com/github/zeng/alt/security/rbac/serve/manager/AdminAuthorizationManager.java](../backend/components/security-component/rbac-security-component/rbac-serve-security-component/src/main/java/com/github/zeng/alt/security/rbac/serve/manager/AdminAuthorizationManager.java#L25)
- [components/security-component/rbac-security-component/rbac-serve-security-component/src/main/java/com/github/zeng/alt/security/rbac/serve/manager/RbacAccessAuthorizationManager.java](../backend/components/security-component/rbac-security-component/rbac-serve-security-component/src/main/java/com/github/zeng/alt/security/rbac/serve/manager/RbacAccessAuthorizationManager.java#L21)
- [components/security-component/rbac-security-component/rbac-serve-security-component/src/main/java/com/github/zeng/alt/security/rbac/serve/manager/ReactiveAdminAuthorizationManager.java](../backend/components/security-component/rbac-security-component/rbac-serve-security-component/src/main/java/com/github/zeng/alt/security/rbac/serve/manager/ReactiveAdminAuthorizationManager.java#L24)
- [components/security-component/rbac-security-component/rbac-serve-security-component/src/main/java/com/github/zeng/alt/security/rbac/serve/manager/ReactiveRbacAccessAuthorizationManager.java](../backend/components/security-component/rbac-security-component/rbac-serve-security-component/src/main/java/com/github/zeng/alt/security/rbac/serve/manager/ReactiveRbacAccessAuthorizationManager.java#L17)

### `:backend:components:storage-component:redisson-storage-component:boot4ProbeMain`

- [components/storage-component/redisson-storage-component/src/main/java/com/github/zeng/alt/storage/redisson/NullValueBuilderCustomizer.java](../backend/components/storage-component/redisson-storage-component/src/main/java/com/github/zeng/alt/storage/redisson/NullValueBuilderCustomizer.java#L7)
- [components/storage-component/redisson-storage-component/src/main/java/com/github/zeng/alt/storage/redisson/RedissonStorageAutoConfiguration.java](../backend/components/storage-component/redisson-storage-component/src/main/java/com/github/zeng/alt/storage/redisson/RedissonStorageAutoConfiguration.java#L21)

### `:backend:components:storage-component:spring-cache-storage-component:boot4ProbeMain`

- [components/storage-component/spring-cache-storage-component/src/main/java/com/github/zeng/alt/storage/springcache/SpringCacheStorageAutoConfiguration.java](../backend/components/storage-component/spring-cache-storage-component/src/main/java/com/github/zeng/alt/storage/springcache/SpringCacheStorageAutoConfiguration.java#L10)

### `:backend:components:tenant-component:core-tenant-component:boot4ProbeMain`

- [components/tenant-component/core-tenant-component/src/main/java/com/github/zeng/alt/tenant/core/TenantAutoConfiguration.java](../backend/components/tenant-component/core-tenant-component/src/main/java/com/github/zeng/alt/tenant/core/TenantAutoConfiguration.java#L21)

### `:backend:components:tenant-component:liquibase-tenant-component:boot4ProbeMain`

- [components/tenant-component/liquibase-tenant-component/src/main/java/com/github/zeng/alt/tenant/liquibase/TenantLiquibaseAutoConfiguration.java](../backend/components/tenant-component/liquibase-tenant-component/src/main/java/com/github/zeng/alt/tenant/liquibase/TenantLiquibaseAutoConfiguration.java#L16)

### `:backend:components:tenant-component:local-tenant-component:boot4ProbeMain`

- [components/tenant-component/local-tenant-component/src/main/java/com/github/zeng/alt/tenant/local/TenantServiceAuthorizationManager.java](../backend/components/tenant-component/local-tenant-component/src/main/java/com/github/zeng/alt/tenant/local/TenantServiceAuthorizationManager.java#L22)

### `:backend:module:workflow-module:boot4ProbeMain`

- [module/workflow-module/src/main/java/com/github/zeng/alt/workflow/service/GlobalFormResolver.java](../backend/module/workflow-module/src/main/java/com/github/zeng/alt/workflow/service/GlobalFormResolver.java#L15)
- [module/workflow-module/src/main/java/com/github/zeng/alt/workflow/service/TaskFormResolver.java](../backend/module/workflow-module/src/main/java/com/github/zeng/alt/workflow/service/TaskFormResolver.java#L26)

### `:backend:workflow:boot4ProbeMain`

- [workflow/src/main/java/com/github/zeng/alt/workflow/delegate/FormkService.java](../backend/workflow/src/main/java/com/github/zeng/alt/workflow/delegate/FormkService.java#L12)

### `:backend:workflow:boot4ProbeTest`

- [workflow/src/test/java/com/github/zeng/alt/workflow/delegate/FormkServiceTest.java](../backend/workflow/src/test/java/com/github/zeng/alt/workflow/delegate/FormkServiceTest.java#L9)


## 实施上下文（第二轮）

已复核上面的模块、依赖及测试映射。实施顺序为依赖 → 公共组件 → Security → Operaton → 全量验证。
保留已有 JSON 公共类型，使用 Boot 4 官方 Jackson 2 兼容模块；为 MVC/WebFlux 明确选择并添加 HTTP 回归。
JPA helper 选择保留现有查询 API 的 Boot 4 兼容 3.6.x，避免附带改写查询协议。
新增回归覆盖 mapper 配置、路由版本谓词、Security 7 授权结果、Operaton 引擎及旧 BPMN 扩展。
数据库升级只在隔离测试数据库中验证，不修改开发或生产库。
