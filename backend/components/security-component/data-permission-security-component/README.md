# 数据权限组件

通过实体字段注解和方法注解，把数据权限自动加入 Hibernate 生成的 SQL；普通 Repository 不需要改为专用查询模板。权限按租户、资源、查询权限和有效角色绑定，授权范围取并集，业务条件与租户条件始终取交集。已有 Querydsl Predicate / JPA Specification 门面保留兼容，admin 用户查询目前继续使用该兼容入口。

## 推荐接入：字段注解 + 方法开启

引入 `jpa-data-permission-security-component`，继续提供现有主体、规则和部门解析 SPI。实体只需声明一次字段含义，不再重复配置 `security.data-permission.resources`：

```java
@Entity
@DataResource("user")
public class User {
    @Id
    @DataColumn(DataColumnType.OWNER)
    private Long userId;

    @DataColumn(DataColumnType.DEPARTMENT)
    private Long deptId;

    @DataColumn(DataColumnType.TENANT)
    private String tenantBy;
}
```

也可直接识别已有 `@TenantId`，无需再添加 TENANT 注解。支持 `@MappedSuperclass` 继承和 JPA 属性访问，列名取 Hibernate 实际映射，包括 `@Column` 与命名策略。当前要求 tenant 为 String、owner 为 String/Long、department 为 Long，三种用途各声明一次；缺失、重复、错误类型、公式或跨表字段在启动时拒绝。

方法声明目标实体和权限码，保留普通 Repository 调用：

```java
@DataPermission(resource = User.class, permission = "GET:/v1/user")
public Page<User> page(Pageable pageable) {
    return userRepository.findAll(pageable);
}

@DataPermission(resource = User.class, permission = "GET:/v1/user")
public List<User> list() {
    return userRepository.findByEnabledTrue();
}

@DataPermission(resource = User.class, permission = "GET:/v1/user")
public long count() {
    return userRepository.count();
}
```

注解可以标在类、接口方法或实现方法上，通过 Spring Bean 代理调用生效；同类自调用沿用 Spring AOP 的限制。组件在调用前解析一次权限并建立事务作用域，分页内容与 count 使用同一个范围快照。支持派生查询、JPQL/HQL、Specification 和 Querydsl；在数据库中先过滤，再分页。没有范围授权返回空结果，`ALL` 始终受租户条件约束。

组件使用 Hibernate 参数化 Filter，不拼接用户输入或额外增加 JDBC 占位符。方法退出或抛错都恢复上下文。相同实体和相同范围的嵌套调用可复用作用域；同一实体嵌套切换到不同范围会明确拒绝，避免内层放宽外层权限。普通的后续调用重新读取当前规则。

### 查询边界

- `@DataResource` 是显式保护声明：直接读取该实体而没有有效方法作用域时拒绝，不会静默回到全量查询。授权 SPI 内部的主体/规则加载由组件建立受控读取边界，不提供面向业务的忽略权限开关。
- 主键查询启用 `applyToLoadByKey`，并在命中持久化上下文时复核缓存实体。若调用方已有事务中缓存了本次范围之外的受保护实体，会拒绝复用该上下文；不会通过 `clear()` 丢弃调用方状态。
- 包含 `@DataResource` 的持久化单元暂不支持原生 SELECT；执行时明确拒绝，包括缓存的原生执行计划。JdbcTemplate 不经过 Hibernate，不在注解覆盖范围内，禁止把它用作受保护数据的替代读取入口。
- 这是查询权限功能，不提供写授权。受保护方法中的即时批量写 SQL 会拒绝执行；不得用该注解替代修改、删除、审批等写操作的授权。流式结果必须在方法内消费，不能把未关闭的 Stream 返回到作用域外。
- 直接懒加载以受保护实体为元素的关联集合会拒绝，避免实体 Filter 未应用到集合加载 SQL。需要这些记录时，使用对应实体的已注解 Repository 查询方法；普通 Repository 返回 List 不受此限制。
- 受保护实体不允许二级实体缓存，包含受保护资源的持久化单元不允许 Hibernate 查询缓存，启用时在启动阶段拒绝。当前 Hibernate 的查询缓存键不包含 Supplier 延迟解析的 Filter 参数。
- 多表继承/secondary table 的字段映射暂不支持。保持原有租户 StatementInspector，组件的只读检查与其组合，不覆盖已有检查器。

本次不改 admin 实体的保护声明或数据库结构；已有认证和权限加载继续沿用原契约。迁移既有实体时，需要一起迁移其业务读取入口，并检查上述原生 SQL、缓存和关联集合边界。

## JOIN 查询

Repository 继续编写普通 JPQL、Specification 或 Querydsl JOIN。方法注解声明本次受保护的实体，Hibernate 根据实际表别名应用该实体的权限范围。例如 User 是受保护资源、Department 是辅助查询表时：

```java
@DataPermission(resource = User.class, permission = "GET:/v1/user")
public Page<User> page(Pageable pageable) {
    return userRepository.pageWithDepartment(pageable);
}
```

```java
@Query(value = """
        select u from User u left join Department d
        on d.deptId = u.deptId and d.tenantBy = u.tenantBy
        """, countQuery = """
        select count(u) from User u left join Department d
        on d.deptId = u.deptId and d.tenantBy = u.tenantBy
        """)
Page<User> pageWithDepartment(Pageable pageable);
```

当前 JOIN 回归验证显式实体 JOIN、一对多关联 JOIN、关联标量投影、LEFT JOIN、非分页集合 FETCH JOIN、多对多目标表以及关联分页/count。组件在映射第二阶段给关联集合补上相同 Filter；只给实体注册 Filter 无法完整覆盖 `join folder.documents`。

LEFT JOIN 的右侧权限通过 JOIN 条件应用，右侧没有可见记录时保留左侧记录。过滤后的空关联不等于左侧资源无权访问；若业务要求必须存在可见右侧记录，使用 INNER JOIN 或 EXISTS。

一对多/多对多查询若以主实体为单位分页，需要匹配的 `select distinct root` 与 `count(distinct root.id)`，避免关联行导致重复和总数膨胀。不要用集合 FETCH JOIN 直接分页；先按权限分页主键，再批量查询关联数据。普通 JOIN 与受保护关联集合的直接懒加载是不同路径，后者仍明确拒绝。

当前每个 `@DataPermission` 仅声明一个实体。上面的例子没有为 Department 开启独立数据范围；如果 JOIN 两边都声明了 `@DataResource`，需要同时存在各自的权限作用域，不能用 User 的权限冒充 Department 的权限。目前尚未提供一个方法声明多资源的注解入口，也不支持直接重复 `@DataPermission`。

## 行为

- `SELF`：资源映射的 owner 字段等于可信当前用户 ID。
- `DEPT`：当前用户数据库记录中的部门。
- `DEPT_TREE`：当前租户有效部门树中的本部门和后代。
- `DEPT_SET`：规则关联的指定部门集合。
- `ALL`：当前租户全部数据；仍应用业务条件和软删除约束。
- 当前角色非空时只采用该角色；未选角色时合并有效角色，只采纳仍属于用户并拥有本次权限的角色。
- 基础权限不足返回 403；有基础权限但没有范围规则时列表为空、详情按不存在处理。加载或 SQL 执行故障不会降级成全量查询。
- 当前阶段不编译已有 SpEL 属性约束。查询权限如果绑定 ABAC 策略或被配置为必需 ABAC 权限，会明确拒绝查询，包括 `ALL` 和管理员。
- 不缓存授权范围，规则和部门变更在下一次查询生效。基础角色权限继续遵循已有 RBAC 缓存和失效机制。

## 用户接入

| 接口 | 规则中的 permission |
| --- | --- |
| `GET /v1/user` | `GET:/v1/user` |
| `GET /v1/user/{id}` | `GET:/v1/user/{id:\d+}`（一个实际反斜杠） |
| `GET /v1/user/info` | `GET:/v1/user/info` |
| `POST /v1/user/search` | `POST:/v1/user/search` |

列表保留 `username`、`gender`、`deptId`、`enabled`、`pageNo`、`pageSize`、`sort`、`order`。每页 1–100 条；高级搜索只允许 `userId`、`username`、`gender`、`deptId`、`enabled`，不开放 password、租户或关联实体字段。详情保留原返回包装，范围外或不存在返回 404。用户卡片查询沿用原有不存在错误契约。

高级搜索请求示例（数字条件值采用 jpa-search-helper 的字符串格式）：

```json
{
  "filter": {
    "operator": "or",
    "filters": [
      {"operator": "eq", "key": "deptId", "value": "10"},
      {"operator": "contains", "key": "username", "value": "alice"}
    ]
  },
  "options": {"pageSize": 10, "pageOffset": 0}
}
```

`UserQueryService` 是业务读取入口；受保护的读路径不调用认证用 `UserRepository`。用户原 APT 列表、详情生成开关已关闭，删除路由及其他资源生成代码保持不变。新增读取、统计、批量或导出功能时，应通过同一权限门面执行，不能直接调用内部仓储或原生 SQL。

## 数据库升级与授权

H2、PostgreSQL 均新增 `V0022-2026-11-12-data-permission.sql`：创建 `main_data_permission` 和 `main_data_permission_department`，补充用户卡片、高级搜索权限；修正既有用户详情权限的正则转义，保留权限 ID 和角色关联。已有 RBAC 角色权限缓存需要通过现有失效接口刷新，或等待 TTL 到期。

新只读权限不会自动分配给普通角色；已有基础权限也不会自动得到全量数据范围。配置数据范围前，普通角色的用户查询返回空。宿主配置 `security.data-permission.administrator-all: true` 显式保留内置管理员的租户内全量查询，仍遵守 `security.context.admin.enabled`。

例如给 master 租户 MANAGER 角色的分页查询授予本部门及下级部门范围（需先拥有对应基础权限）：

```sql
INSERT INTO main_data_permission
    (data_permission_id, tenant_by, resource, permission, role_code, scope_type, is_enabled)
VALUES (900000001, 'master', 'user', 'GET:/v1/user', 'MANAGER', 'DEPT_TREE', TRUE);
```

`DEPT_SET` 的部门 ID 写入 `main_data_permission_department`。范围规则按查询权限分别配置，不自动从列表权限复制到详情、卡片或高级搜索。当前没有新增规则管理 HTTP 接口或前端页面。

此前本机原生运行配置关闭了自动迁移；使用该旧配置启动新版本前，需要先执行迁移。本轮验证使用临时数据库，未修改本机保留的数据卷。

## 扩展和验证

API 模块提供注解、范围模型和宿主 SPI；Core 解析主体与合并范围；JPA 模块注册 Hibernate 映射、管理方法作用域并执行过滤。下列配置及链式写法为保留的兼容入口。宿主通过 `DataPermissionCustomizer` 有序贡献配置，也可以使用 `security.data-permission.resources` 声明实体、tenant、owner 和 department 字段。配置错误在启动时被拒绝。

```java
return properties -> properties.resource("user", User.class,
        fields -> fields.setTenant("tenantBy").setOwner("userId").setDepartment("deptId"));
```

单次构建器使用一种查询后端，终结操作重新授权；分页内容与 count 共用一次授权范围快照：

```java
return permissions.query(User.class)
        .permission("GET:/v1/user")
        .where(businessPredicate)
        .page(pageable);
```

组件测试覆盖并集、空范围、配置绑定、AOT 元数据、分页/count/exists/详情、Specification 与 Querydsl。admin 测试覆盖 H2 迁移/回滚、可信主体角色交集和隔离 PostgreSQL/Redis 的真实 HTTP 查询。

```bash
./gradlew :backend:components:security-component:data-permission-security-component:core-data-permission-security-component:test \
  :backend:components:security-component:data-permission-security-component:jpa-data-permission-security-component:test
scripts/verify-security-docker.sh --tests '*Test'
```

既有用户查询适配验证记录：88 项测试通过（admin 73、Core 8、JPA 7），无失败或跳过；新增重点逻辑 JaCoCo 行覆盖率 87.5%–100%，workflow 编译通过。macOS ARM64 / GraalVM 21 quick-build 镜像构建成功，15 项用户查询原生检查通过；临时进程与测试容器已关闭，本机保留服务仍暂停。

用户认证和单条主体读取显式预取角色；分页在数据库中先完成，再批量加载结果用户的角色，避免集合 fetch join 退化为内存分页。用户卡片的部门名称通过标量查询加载。永久回归还在 `hibernate.bytecode.provider=none` 下执行用户查询，并检查分页包装与嵌套角色 DTO 的 AOT 绑定元数据。

额外探测到原生 `GET /v1/role` 在关联数据读取时仍会触发 Hibernate 运行时代理错误；该业务未接入本组件，本轮按范围保持代码不变。JVM 的角色列表回归用于确认本次未将用户数据范围应用到其他资源。

注解集成测试使用真实 H2、Hibernate 和普通 Spring Data Repository，断言实际集合结果、分页内容、count SQL、参数绑定、部门列表长度变化、跨租户隔离、无规则、异常清理、主键缓存、嵌套调用、受控策略读取及原生 SQL 拒绝。JPA 模块 `check` 已增加 80% 行覆盖率门槛：

```bash
./gradlew :backend:components:security-component:data-permission-security-component:core-data-permission-security-component:test \
  :backend:components:security-component:data-permission-security-component:jpa-data-permission-security-component:check
```

Hibernate 服务加载器的构造器及服务描述符已注册 RuntimeHints。本次新增能力执行 JVM/H2 测试及宿主兼容性检查，未重新构建 native-image。
