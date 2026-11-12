# Security 修复与验证记录（2026-11-12）

范围：security-component，以及 admin/workflow 的安全接入。验证使用隔离的 PostgreSQL 16、Redis 7；未连接现有开发或生产数据。未修改前端工作区文件。

## 已完成的行为修复

- 移除缺省固定 Servlet 账户与自动表单登录。JWT 签发端校验数据库账户状态、用户 ID 和租户，停用用户时撤销该租户用户的 Access/Refresh 凭证；仅验签端不执行本地刷新。
- RBAC 缺省开关与属性默认值一致，显式 `false` 仍关闭；Servlet/Reactive 管理员授权遵守 `security.context.admin.enabled`。
- 多角色权限缓存部分命中时只补查缺失角色，缓存的空权限集合视为命中；Reactive 资源定位从 principal 读取租户。WebFlux 拒绝请求返回 401/403 ProblemDetail。
- JWT/Cookie 模块可以共同装配，过滤器与 OpenAPI Bean 使用独立名称。
- Cookie 验证默认启用 CSRF，GET 请求下发 `XSRF-TOKEN`，写请求需提交匹配的 `X-XSRF-TOKEN`；表单属性仍使用 XOR 编码。允许通过 `security.cookie-auth.csrf-enabled=false` 显式关闭，或由宿主 customizer 配置自己的策略。
- 核心组件不再硬编码放行 `/login/**`、H2、租户 GraphQL/GraphiQL 和 actuator startup。宿主通过 `security.filter.ignore-url` 或有序授权 customizer 声明放行；实际登录过滤器仍处理其配置的登录端点。admin 的 H2 开发控制台由 dev 配置显式放行。
- `LoginHelper.reset` 不再默认返回 `null`，Servlet 重载委托基础重载；未实现重签或按用户撤销时明确抛出 `UnsupportedOperationException`。JWT 与 Cookie 现均支持按当前租户用户批量撤销，Cookie 的实现见下方追加修复。既有 `getCurrentUser` 可空契约保持兼容。
- 会话 Cookie 的签发与清除共享名称、路径、域、SameSite 和安全属性，HTTPS 请求自动携带 Secure。JWT 两条 Refresh Cookie 签发路径都遵守 SameSite/Path/TTL；自定义 Refresh Header 用于刷新和登出撤销，刷新仍必须携带 Bearer Access Token。缓存撤销故障不再被吞掉。

## 接入变化

Cookie SPA 客户端先发 GET，读取 `XSRF-TOKEN`，再在 POST/PUT/PATCH/DELETE 请求中提交 `X-XSRF-TOKEN`；登录与登出也受 CSRF 校验。认证会话 Cookie 仍为 HttpOnly，CSRF Cookie 必须允许前端读取。跨源部署还需由宿主配置 CORS 与 Cookie 域策略。

原先依赖组件内置白名单的应用需显式声明需要公开的路径。公开路径仍受到宿主贡献的附加授权约束。Cookie 的默认 SameSite 为 `Lax`，JWT Refresh Cookie 的既有默认值也为 `Lax`。

## 依赖安全

使用 OSV-Scanner 2.7.1 下载公开 Maven 漏洞库并离线匹配 Gradle 实际解析的传递运行时依赖，未向扫描服务上传源码、SBOM 或依赖清单。范围包括 admin、workflow 和独立安全模块。

- 升级前：289 个不同坐标，113 条不同公告（122 个坐标/公告匹配）。
- 升级后：289 个不同坐标，3 条不同公告（4 个坐标/公告匹配）。
- 告警是版本匹配结果，不等同于已经证明项目存在可利用路径。

基线升级为 Spring Boot 3.5.16 / Spring Security 6.5.11 / Spring Framework 6.2.19；Hibernate 增强插件与运行时对齐为 6.6.53.Final。统一覆盖 Boot 尚未包含的修复：Tomcat 10.1.59、Jackson 2.21.7、Netty 4.1.137.Final、PostgreSQL JDBC 42.7.12、Commons Lang 3.18.0、HttpClient 5.6.3 / HttpCore 5.4.3、Log4j API 2.25.5、Guava 33.7.2-jre、Bouncy Castle 1.85。均保持既有主版本。

### 保留的三条告警

| 公告 | 必要触发条件 | 本项目核对结果 |
| --- | --- | --- |
| [CVE-2026-47884](https://spring.io/security/cve-2026-47884/) | XsltView，且存在未指定视图名的通配映射 | 未发现 XSLT 配置；实际 admin 无 XsltView / XsltViewResolver Bean |
| [CVE-2026-47890](https://spring.io/security/cve-2026-47890/) | 通过 SSE 向其他用户渲染视图片段，攻击者可控片段数据 | 未发现 SSE/片段渲染代码；实际 admin 路由返回类型未启用这些功能 |
| [CVE-2026-47892](https://spring.io/security/cve-2026-47892/) | WebFlux 函数式端点部署在 DispatcherServlet 下 | 两个宿主是 Servlet 应用；未发现相关函数式路由，实际 admin 无 Reactive RouterFunction Bean |

官方提供的同主版本修复为 Spring Framework 6.2.20，仅商业支持仓库提供；公开修复为 7.0.9。当前 Maven Central 的 6.2 最新版是 6.2.19。本轮不跨越已授权的主版本边界，不伪造“扫描清零”，也不通过忽略规则隐藏上述告警。将来启用这些功能、引入相关扩展或升级依赖时应重新评估。要消除版本告警，需要接入有授权的商业仓库，或单独执行主版本迁移。

## 验证

- 266 个测试通过，无失败、错误或跳过：admin 53（含 5 项 Docker 验证）、核心安全 67、Cookie 8、JWT 29、RBAC 服务端 31、ABAC API 6 / 核心 18 / 方法 18 / SpEL 36。
- Docker 验证通过：真实 HTTP 登录及凭证入 Redis；Access/Refresh 同时撤销；数据库账户停用后拒绝资料写入；旧固定白名单路径匿名被拒；真实权限加载器与 Redis 的部分缓存命中、租户命名空间隔离；保留告警所需功能的运行时检查。
- `workflow:compileJava`、admin AOT 测试生成与编译通过；新增 UniqueCheckAotTest 验证真实工厂元数据在 AOT 流程中可预测出事务代理，实际 AOT 输出也包含所需 CGLIB 字节码。SQL 导出工具 `HttpResourceSqlExporter` 仍按原有说明禁用 AOT 测试处理；本轮常规测试范围为 `*Test`。
- JaCoCo 行覆盖率：CookieHelper、CookieCsrfTokenRequestHandler、CookieAuthenticationSuccessHandler、JwtCookieHelper 为 100%；CookieLogoutHandler 90.9%；JwtLoginHelper 95.5%；JwtAuthenticationFilter 98.7%。此前 P2 重点逻辑覆盖率为 93.3%–100%。覆盖率只针对列出的类/逻辑，不是仓库总体覆盖率。
- `git diff --check`、新增 Java 文件头/行宽检查和 Docker 脚本语法检查通过。
- Native：修复日志/Netty 构建期初始化冲突，优化模式镜像构建成功；启动发现领域唯一性检查 Bean 的事务代理缺失，已将工厂返回类型收窄为实现类型，并通过真实 AOT 代理生成回归。修复后的 quick-build 镜像构建成功，连接隔离 PostgreSQL/Redis 的五项原生 HTTP 烟测全部通过：匿名 401、合法缓存 JWT 200、篡改 JWT 401、数据库账户停用后 401、撤销凭证后 401。最终回归使用 GraalVM 21.0.12+7.1 / macOS ARM64 / quick-build；项目默认仍为优化模式，修复代理后未重复运行完整优化模式构建。

## 本机原生验证码修复追加验证

本机 macOS ARM64 / GraalVM 21 原生进程的验证码曾因 AWT 库不可用返回 500。渲染器改用内置点阵字形和 JDK 压缩、CRC 功能生成 PNG，不再调用 AWT、ImageIO 或系统字体；保持随机字符、算术验证码、图片接口、Cookie 和 Redis 校验契约。Servlet API 仅补为组件测试的运行时依赖，未新增生产依赖。

真实登录还暴露出 JWT 的外部嵌套配置未生成绑定元数据，配置的 `/v1/login/jwt` 未生效。为登录、登出属性补充 `@NestedConfigurationProperty`，新增真实 AOT 生成回归；修复前该测试失败，修复后通过。最终镜像中的配置路径生效，默认 `/login/jwt` 不再提供登录。

- 本轮重跑 97 项测试全部通过：验证码 14、JWT 30、admin 53（含隔离 Docker 集成验证）。渲染器 JaCoCo 行覆盖率为 95.1%；仅加载 `java.base` 的子进程也能生成 PNG。
- 最终 quick-build 原生文件重建并重启成功；连接本次启动任务创建的专用 PostgreSQL/Redis，12 项原生检查通过：匿名拒绝、默认登录路径停用、缺失验证码拒绝、错误验证码拒绝、错误尝试后的验证码不可再用、错误密码拒绝、正确登录、受保护接口访问、验证码重放拒绝、登出、登出后 Token 失效、PNG 图片返回。
- 运行配置、随机管理员密码和验证脚本位于 Git 忽略的 `backend/admin/build/local-native`，未提交凭证。验证码校验保持开启，数据库、Redis 与最终原生进程继续运行。

## 重复执行

### 六项复查问题追加修复

- Servlet 和 WebFlux 把必需 AND 约束放在 URL 授权之后执行，覆盖 customizer 的 `authenticated`、`permitAll` 等规则。基础授权拒绝时不执行约束，约束按顺序短路；保留原有 Composite 的公开构造契约。admin 原先仅要求登录的自定义路径现在也会执行宿主 ABAC/RBAC 约束，调用者需要满足已有权限与策略。
- Cookie 认证从缓存取得可信租户后，清除请求头产生的数据库、schema、路由信息，再在该租户加载权威账户。每次请求复核 ID、租户和四项账户状态，并使用当前角色集合；查无账户或账户失效会删除会话，基础设施异常不会被吞掉，请求结束清除安全及租户上下文。宿主需要提供可查询当前账户的 `UserDetailsService`；旧三参数过滤器构造器保留但安全拒绝恢复会话，应改用四参数构造器。
- Cookie 新会话 ID 使用 Base64 URL 编码的租户、用户范围加随机 UUID，仍通过原有会话 Cookie 和 `session:<sessionId>` 缓存访问；`logout(userId)` 只撤销当前租户该用户的所有会话。旧格式 Cookie 会话无法按用户定位，升级后失效，需要重新登录。无需数据库迁移；JWT 凭证格式不变。
- Reactive 缺省用户服务改为空服务，移除 `root / 123456`，宿主提供的用户服务仍优先。
- Servlet 白名单不再依赖非 Bean 上的调度注解；每次授权读取服务快照，只有路径集合变化才重新编译匹配器，新增和移除都在下一请求生效。
- JWT 配置按 Base64 解码后的字节数校验。Access 至少 32 字节，单独 Refresh 至少 64 字节；签发端复用 Access 密钥签 HS512 Refresh 时，Access 也须达到 64 字节。非法编码和不足长度在绑定阶段拒绝，合法的 44/88 字符最小密钥可以正常使用。

验证：先运行新增测试，确认旧实现失败，再修复并回归。最终相关测试共 335 项，失败、错误、跳过均为 0；其中 admin 77 项包含 9 项隔离 Docker 回归。覆盖核心安全、Cookie、JWT、RBAC、ABAC 和数据权限模块；`workflow:compileJava`、admin AOT 测试生成及编译通过。实际 PostgreSQL/Redis 验证包括自定义认证路径的权限和 ABAC 拒绝、Cookie 可信租户下的数据库读取、数据库停用后的 Cookie 拒绝、按租户用户撤销 Redis 会话。

JaCoCo 0.8.13 对本轮重点类的行覆盖率：SessionManager、CookieLoginHelper、SecurityAutoConfiguration、JwtProperties 为 100%，CookieAuthenticationFilter 85.1%，WhiteListAuthorizationManager 94.4%，WebSecurityAutoConfiguration 80.0%，ReactiveSecurityAutoConfiguration 96.9%。报告位于 `build/reports/security-fixes`；这些数字不代表仓库整体覆盖率。本轮未重建 Native 可执行文件，JWT 配置 AOT 回归额外检查新校验 getter 的反射提示。

### 回归命令

Docker 脚本使用仅绑定本机的随机端口、临时 PostgreSQL 文件系统和独立 Redis，退出时自动删除本次容器：

```bash
scripts/verify-security-docker.sh --tests '*Test' :backend:workflow:compileJava
```

安全组件测试可用各模块的 `:test` 任务执行。默认不开启 Docker 场景；设置 `SECURITY_DOCKER_TESTS=true` 并提供隔离服务端口后才运行 `AdminSecurityDockerTest`。

导出实际运行时依赖，再离线扫描（生成物保留在 build 目录，不提交）：

```bash
./gradlew -I scripts/security-runtime-inventory.init.gradle securityRuntimeInventory
osv-scanner scan source --offline --download-offline-databases --no-resolve \
  -L build/reports/security/bom.cdx.json --format json \
  --output-file build/reports/security/osv-results.json
```

首次命令只下载公开漏洞库；已经下载后可以去掉 `--download-offline-databases` 完全断网运行。扫描器命中告警时返回退出码 1，本轮保留三条告警属于已记录结果，不应当作“扫描没有执行”。

本机 Java 的 Maven TLS 下载曾失败。验证通过校验公开依赖并使用临时本地缓存仓库继续，未关闭 TLS 证书检查，未修改项目仓库地址。
