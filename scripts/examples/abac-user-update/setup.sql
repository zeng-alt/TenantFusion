-- PostgreSQL 开发示例。手动执行，不加入 Liquibase 自动迁移。
-- 租户 t001；演示账户密码均为 AbacDemo@123（下方保存 BCrypt）。
-- 已有业务权限不修改，固定示例规则按新契约更新；新增 user:update 对应真实 PATCH 路由。
-- SpEL 绑定权限，角色获取权限，用户加入角色；不是直接向角色/用户字段写表达式。
BEGIN;

INSERT INTO main_department
(dept_id, dept_name, dept_sort, is_enabled, is_deleted, tenant_by, created_by, created_date)
VALUES
(862101, 'ABAC 示例 A 部门', 100, TRUE, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP),
(862102, 'ABAC 示例 B 部门', 101, TRUE, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (dept_id) DO NOTHING;

INSERT INTO main_role
(role_id, code, name, role_sort, is_enabled, is_deleted, tenant_by, created_by, created_date)
VALUES (862010, 'ABAC_DEPT_EDITOR', 'ABAC 同部门用户编辑员', 100, TRUE, FALSE,
        't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (role_id) DO NOTHING;

INSERT INTO main_permission
(permission_id, resource_type, code, name, description, method, path,
 is_enabled, is_deleted, tenant_by, created_by, created_date)
VALUES (862020, 'HTTP', 'user:update', 'ABAC 示例：修改同部门用户',
        '与 UserServiceImpl.patchUser 的方法权限码一致', 'PATCH', '/v1/user/{id}',
        TRUE, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (permission_id) DO NOTHING;

INSERT INTO main_policy_rule
(policy_rule_id, name, description, permission_id, condition,
 is_pre_auth, is_deleted, tenant_by, created_by, created_date)
VALUES (862030, 'ABAC 示例：仅允许同租户同部门修改',
        '主体和目标属性均取自数据库；适用于 t001 的 user:update 权限',
        862020,
        'principal.deptId != null and principal.deptId == resource.deptId and (#dto.deptId == null or #dto.deptId == principal.deptId)',
        TRUE, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (policy_rule_id) DO UPDATE
SET condition = EXCLUDED.condition, is_pre_auth = TRUE;

-- 后置规则会在 patchUser 返回后、事务提交前执行；拒绝时撤销此次数据库修改。
INSERT INTO main_policy_rule
(policy_rule_id, name, description, permission_id, condition,
 is_pre_auth, is_deleted, tenant_by, created_by, created_date)
VALUES (862031, 'ABAC 示例：事务内后置校验', '禁止将用户名改为 abac_post_denied',
        862020,
        'returnObject == #id and (#dto.username == null or #dto.username != ''abac_post_denied'')',
        FALSE, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (policy_rule_id) DO UPDATE
SET condition = EXCLUDED.condition, is_pre_auth = FALSE;

INSERT INTO main_user
(user_id, username, password, nick_name, dept_id, is_enabled, is_deleted, tenant_by,
 created_by, created_date)
VALUES
(862001, 'abac_a', '{bcrypt}$2a$10$zAiLXmli44kvhEqd0rh0HO9MbpjxPv/zOBKcdF9QZEz3ql6lzmlMK', 'ABAC A 部门操作者', 862101, TRUE, FALSE,
 't001', 'abac-demo', CURRENT_TIMESTAMP),
(862002, 'abac_b', '{bcrypt}$2a$10$zAiLXmli44kvhEqd0rh0HO9MbpjxPv/zOBKcdF9QZEz3ql6lzmlMK', 'ABAC B 部门操作者', 862102, TRUE, FALSE,
 't001', 'abac-demo', CURRENT_TIMESTAMP),
(862003, 'abac_target_a', '{bcrypt}$2a$10$zAiLXmli44kvhEqd0rh0HO9MbpjxPv/zOBKcdF9QZEz3ql6lzmlMK', 'ABAC A 部门目标', 862101, TRUE, FALSE,
 't001', 'abac-demo', CURRENT_TIMESTAMP),
(862004, 'abac_no_permission', '{bcrypt}$2a$10$zAiLXmli44kvhEqd0rh0HO9MbpjxPv/zOBKcdF9QZEz3ql6lzmlMK', 'ABAC 无基础权限用户', 862101, TRUE, FALSE,
 't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO NOTHING;

-- 两个操作者拥有同一个角色，部门不同，所以同一权限会得到不同 ABAC 结论。
INSERT INTO main_user_role
(user_role_id, user_id, role_id, is_deleted, tenant_by, created_by, created_date)
VALUES
(862041, 862001, 862010, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP),
(862042, 862002, 862010, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (user_role_id) DO NOTHING;

INSERT INTO main_role_permission
(role_permission_id, role_id, permission_id, is_deleted, tenant_by, created_by, created_date)
VALUES (862050, 862010, 862020, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (role_permission_id) DO NOTHING;

-- 前端登录后的 /v1/auth/admin 资格检查使用独立基础权限。
-- 不授予管理员角色或其他业务接口权限。
INSERT INTO main_role_permission
(role_permission_id, role_id, permission_id, is_deleted, tenant_by, created_by, created_date)
SELECT 862051, 862010, permission_id, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP
FROM main_permission
WHERE tenant_by = 't001' AND resource_type = 'HTTP' AND method = 'GET'
  AND path = '/v1/auth/admin' AND is_deleted = FALSE
ON CONFLICT (role_permission_id) DO NOTHING;

-- 专用策略管理员仅管理 ABAC，不授予 user:update。
INSERT INTO main_role
(role_id, code, name, role_sort, is_enabled, is_deleted, tenant_by, created_by, created_date)
VALUES (862011, 'ABAC_POLICY_MANAGER', 'ABAC 示例策略管理员', 101, TRUE, FALSE,
        't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (role_id) DO NOTHING;

INSERT INTO main_user
(user_id, username, password, nick_name, dept_id, is_enabled, is_deleted, tenant_by, created_by, created_date)
VALUES (862005, 'abac_manager', '{bcrypt}$2a$10$zAiLXmli44kvhEqd0rh0HO9MbpjxPv/zOBKcdF9QZEz3ql6lzmlMK', 'ABAC 策略管理员', 862101, TRUE, FALSE,
        't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO main_user_role
(user_role_id, user_id, role_id, is_deleted, tenant_by, created_by, created_date)
VALUES (862043, 862005, 862011, FALSE, 't001', 'abac-demo', CURRENT_TIMESTAMP)
ON CONFLICT (user_role_id) DO NOTHING;

WITH RECURSIVE menu_ids AS (
    SELECT permission_id, parent_id FROM main_permission
    WHERE tenant_by = 't001' AND code = 'AbacMgt' AND resource_type = 'MENU'
    UNION ALL
    SELECT parent.permission_id, parent.parent_id FROM main_permission parent
    JOIN menu_ids child ON child.parent_id = parent.permission_id
    WHERE parent.tenant_by = 't001' AND parent.resource_type = 'MENU'
), grants AS (
    SELECT permission_id FROM menu_ids
    UNION
    SELECT permission_id FROM main_permission
    WHERE tenant_by = 't001' AND (
        code IN ('abac:permissions:read', 'abac:metadata:read', 'abac:policies:read', 'abac:simulate',
                 'abac:policies:create', 'abac:policies:update', 'abac:policies:delete', 'abac:policies:refresh')
        OR (resource_type = 'HTTP' AND method = 'GET' AND path = '/v1/auth/admin')
    ) AND is_deleted = FALSE
)
INSERT INTO main_role_permission
(role_permission_id, role_id, permission_id, is_deleted, tenant_by, created_by, created_date)
SELECT 862060 + ROW_NUMBER() OVER (ORDER BY permission_id), 862011, permission_id, FALSE,
       't001', 'abac-demo', CURRENT_TIMESTAMP FROM grants
ON CONFLICT DO NOTHING;

COMMIT;

-- 关系核对：策略 -> 权限 <- 角色权限 <- 角色 <- 用户角色 <- 用户。
SELECT u.username, u.user_id, u.dept_id, r.code AS role_code, p.code AS permission_code, pr.condition
FROM main_user u
JOIN main_user_role ur ON ur.user_id = u.user_id AND ur.tenant_by = u.tenant_by
JOIN main_role r ON r.role_id = ur.role_id AND r.tenant_by = u.tenant_by
JOIN main_role_permission rp ON rp.role_id = r.role_id AND rp.tenant_by = u.tenant_by
JOIN main_permission p ON p.permission_id = rp.permission_id AND p.tenant_by = u.tenant_by
JOIN main_policy_rule pr ON pr.permission_id = p.permission_id AND pr.tenant_by = u.tenant_by
WHERE u.tenant_by = 't001' AND u.user_id IN (862001, 862002)
ORDER BY u.user_id;
