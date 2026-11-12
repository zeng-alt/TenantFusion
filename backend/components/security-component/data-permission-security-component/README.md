# 数据权限组件

在数据库查询前把数据授权转换成 Querydsl Predicate 或 JPA Specification。权限按租户、资源、查询权限和有效角色绑定；授权范围取并集，业务条件与租户条件始终取交集。只有用户业务查询接入，其他资源、用户写操作、登录账号加载和当前用户个人资料保持原有行为。

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

API 模块只提供范围模型和宿主 SPI；Core 解析主体与合并范围；JPA 模块验证资源字段并执行查询。宿主通过 `DataPermissionCustomizer` 有序贡献配置，也可以使用 `security.data-permission.resources` 声明实体、tenant、owner 和 department 字段。配置错误在启动时被拒绝。

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

本轮验证：88 项测试通过（admin 73、Core 8、JPA 7），无失败或跳过；新增重点逻辑 JaCoCo 行覆盖率 87.5%–100%，workflow 编译通过。macOS ARM64 / GraalVM 21 quick-build 镜像构建成功，15 项用户查询原生检查通过；临时进程与测试容器已关闭，本机保留服务仍暂停。

用户认证和单条主体读取显式预取角色；分页在数据库中先完成，再批量加载结果用户的角色，避免集合 fetch join 退化为内存分页。用户卡片的部门名称通过标量查询加载。永久回归还在 `hibernate.bytecode.provider=none` 下执行用户查询，并检查分页包装与嵌套角色 DTO 的 AOT 绑定元数据。

额外探测到原生 `GET /v1/role` 在关联数据读取时仍会触发 Hibernate 运行时代理错误；该业务未接入本组件，本轮按范围保持代码不变。JVM 的角色列表回归用于确认本次未将用户数据范围应用到其他资源。
