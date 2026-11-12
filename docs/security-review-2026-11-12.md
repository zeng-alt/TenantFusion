# Security 修复与验证记录（2026-11-12）

范围：security-component，以及 admin/workflow 的安全接入。验证使用隔离的 PostgreSQL 16、Redis 7；未连接现有开发或生产数据。未修改前端工作区文件。

## 已完成的行为修复

- 移除缺省固定 Servlet 账户与自动表单登录。JWT 签发端校验数据库账户状态、用户 ID 和租户，停用用户时撤销该租户用户的 Access/Refresh 凭证；仅验签端不执行本地刷新。
- RBAC 缺省开关与属性默认值一致，显式 `false` 仍关闭；Servlet/Reactive 管理员授权遵守 `security.context.admin.enabled`。
- 多角色权限缓存部分命中时只补查缺失角色，缓存的空权限集合视为命中；Reactive 资源定位从 principal 读取租户。WebFlux 拒绝请求返回 401/403 ProblemDetail。
- JWT/Cookie 模块可以共同装配，过滤器与 OpenAPI Bean 使用独立名称。
- Cookie 验证默认启用 CSRF，GET 请求下发 `XSRF-TOKEN`，写请求需提交匹配的 `X-XSRF-TOKEN`；表单属性仍使用 XOR 编码。允许通过 `security.cookie-auth.csrf-enabled=false` 显式关闭，或由宿主 customizer 配置自己的策略。
- 核心组件不再硬编码放行 `/login/**`、H2、租户 GraphQL/GraphiQL 和 actuator startup。宿主通过 `security.filter.ignore-url` 或有序授权 customizer 声明放行；实际登录过滤器仍处理其配置的登录端点。admin 的 H2 开发控制台由 dev 配置显式放行。
- `LoginHelper.reset` 不再默认返回 `null`，Servlet 重载委托基础重载；未实现重签或按用户撤销时明确抛出 `UnsupportedOperationException`。Cookie 尚不支持按用户批量撤销，不再静默报告成功；JWT 支持该能力。既有 `getCurrentUser` 可空契约保持兼容。
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

## 重复执行

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
