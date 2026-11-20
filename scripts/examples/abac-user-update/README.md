# ABAC 用户修改与工作台示例

先让 Admin 执行 `V0027-2026-11-20-abac-method-only.sql` 迁移，再手动执行本目录的 `setup.sql`。示例位于行级隔离租户 `t001`，业务接口为 `PATCH /v1/user/{id}`。

| 账户 | 用户 ID | 部门 | 用途 |
| --- | --- | --- | --- |
| `abac_a` | 862001 | 862101 | A 部门编辑员 |
| `abac_b` | 862002 | 862102 | B 部门编辑员 |
| `abac_target_a` | 862003 | 862101 | 修改目标 |
| `abac_no_permission` | 862004 | 862101 | 无基础权限的对照账户 |
| `abac_manager` | 862005 | 862101 | 仅管理 ABAC 策略，可进入工作台 |

演示密码均为 `AbacDemo@123`，仅用于开发库。登录选择租户 `t001`；`requests.http` 可在 IDEA 直接执行。编辑员拥有 `ABAC_DEPT_EDITOR`，策略管理员拥有 `ABAC_POLICY_MANAGER`，不授予管理员角色。

## 两条规则

`UserServiceImpl.patchUser` 声明 `@AbacAuthorize(permission = "user:update", resources = UserResourceResolver.class)`，并由 Spring 事务包围。HTTP 层先做认证、租户及基础权限检查；ABAC 规则统一在方法代理执行。

前置规则 `862030`：

```spel
principal.deptId != null and principal.deptId == resource.deptId and (#dto.deptId == null or #dto.deptId == principal.deptId)
```

`principal` 来自数据库中的操作者，`resource` 来自目标用户当前数据库记录，`#dto` 来自本次方法参数。跨部门修改以及将目标转到其他部门均拒绝。租户边界由服务端单独校验，表达式不能解除。

后置规则 `862031`：

```spel
returnObject == #id and (#dto.username == null or #dto.username != 'abac_post_denied')
```

`patchUser` 的 `Either.right(Long)` 解包后作为 `returnObject`。方法执行完成后、事务提交之前求值；修改为 `abac_post_denied` 时返回 HTTP 403，数据库中的用户名保持原值。没有活动事务时直接报错，并在业务方法执行前拒绝。`void` 方法也执行后置规则，此时 `returnObject` 为 null；业务返回 `Either.left` / `Option.none` 时保留原业务失败。

## 变量契约与关系

公开根为 `authentication`、`principal`、`resource`、`env`；方法参数使用 `#id`、`#dto`、`#p0` / `#a0`，后置阶段额外开放 `returnObject`。不提供 `subject` / `environment` 或旧协议兼容构造器。

```text
main_policy_rule(862030 前置 / 862031 后置)
  -> main_permission(862020, code=user:update)
  <- main_role_permission(862050)
  <- main_role(862010, code=ABAC_DEPT_EDITOR)
  <- main_user_role(862041 / 862042)
  <- main_user(abac_a / abac_b)
```

当前数据模型是策略绑定权限，角色和用户通过授权关系受策略约束，没有策略直接绑定用户的关联表。规则作用于 `t001` 下所有拥有此权限的操作者。

## 可视化管理

使用 `abac_manager` 登录前端，进入「权限管理 → ABAC」，选择 `user:update`：

1. 选择前置或后置策略，使用代码编辑或 `vue-spel-query-builder` 可视化规则树。
2. 字段与参数由服务端元数据生成，后置阶段才显示返回结果。未能完整转换的代码会保留，并提示继续代码编辑。
3. 点击「重建样本」，填写 `principal.deptId`、`resource.deptId` 和 `variables.dto.deptId` 等，点击「校验并运行」。试算使用同一个后端求值器，不调用业务方法，也不改变登录身份。
4. 保存、删除通过策略服务发出提交后刷新通知。修改 SQL 则绕过通知，需点「刷新缓存」或重启；远程实例需收到广播或等待缓存 TTL。

前端使用包 `@zeng-alt/vue-spel-query-builder@1.1.1`；项目 pnpm patch 为它补充 `globals` 字段根，保留 `resource/env/returnObject` 的统一语法，并把包内样式限制在组件作用域，避免影响后台现有样式。

## 预期结果

| 账户 | 目标 | 请求体 | 状态 |
| --- | --- | --- | --- |
| abac_a | 862003 | `{"enabled":true}` | 200，同部门 |
| abac_a | 862002 | `{"enabled":false}` | 403，前置拒绝 |
| abac_a | 862002 | `{"deptId":862101}` | 403，不能替换数据库资源属性 |
| abac_a | 862003 | `{"deptId":862102}` | 403，不能转到其他部门 |
| abac_a | 862003 | `{"username":"abac_post_denied"}` | 403，后置拒绝并回滚 |
| abac_b | 862001 | `{"enabled":false}` | 403，前置拒绝 |
| abac_b | 862002 | `{"enabled":true}` | 200，同部门 |
| abac_no_permission | 862003 | `{"enabled":false}` | 403，缺少 RBAC 权限 |

核对数据库：

```sql
SELECT user_id, username, dept_id, is_enabled
FROM main_user
WHERE tenant_by = 't001' AND user_id BETWEEN 862001 AND 862005
ORDER BY user_id;
```

## 安装和清理

```sh
psql -X -v ON_ERROR_STOP=1 -f scripts/examples/abac-user-update/setup.sql
psql -X -v ON_ERROR_STOP=1 -f scripts/examples/abac-user-update/cleanup.sql
```

安装只添加固定示例数据，重复执行会将本示例两条规则更新到上述表达式。清理仅删除本示例 ID；SQL 操作后刷新 ABAC 缓存，角色权限变化后重新登录并清理相应 RBAC 缓存或等待 TTL。
