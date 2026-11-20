--liquibase formatted sql

--changeset zeng:abac-execution-scope
ALTER TABLE main_policy_rule ADD COLUMN execution_scope VARCHAR(16) DEFAULT 'HTTP' NOT NULL;
ALTER TABLE main_policy_rule ADD CONSTRAINT chk_main_policy_scope CHECK (execution_scope IN ('HTTP', 'METHOD'));

-- 与当前 Controller 路径和方法注解统一，保留已有角色权限关联 ID。
UPDATE main_permission p SET code = 'user:update', path = '/v1/user/{id}'
WHERE p.resource_type = 'HTTP' AND p.method = 'PATCH' AND p.path LIKE '/v1/user/{id:%}'
  AND NOT EXISTS (SELECT 1 FROM main_permission current_permission
                  WHERE current_permission.tenant_by = p.tenant_by AND current_permission.code = 'user:update');

UPDATE main_policy_rule SET execution_scope = 'METHOD'
WHERE permission_id IN (SELECT permission_id FROM main_permission
                         WHERE code IN ('user:update', 'GET:/v1/user/info'));

-- 升级已保存的数据；运行时不保留旧变量别名。
UPDATE main_policy_rule SET condition = REPLACE(REPLACE(condition, 'subject[', 'principal['), 'environment[', 'env[');

-- 每个现有业务租户各自拥有工作台 HTTP 权限。管理员沿用既有豁免；普通管理角色需显式授予。
INSERT INTO main_permission
(permission_id, resource_type, code, name, method, path, menu_id,
 is_enabled, is_deleted, tenant_by, created_by, created_date)
SELECT 202611200000 + ROW_NUMBER() OVER (ORDER BY tenants.tenant_by, operations.operation_order),
       'HTTP', operations.code, operations.name, operations.method, operations.path,
       (SELECT MIN(menu.permission_id) FROM main_permission menu
         WHERE menu.tenant_by = tenants.tenant_by AND menu.code = 'AbacMgt' AND menu.resource_type = 'MENU'),
       TRUE, FALSE, tenants.tenant_by, 'system', CURRENT_TIMESTAMP
FROM (SELECT DISTINCT tenant_by FROM main_permission WHERE tenant_by IS NOT NULL) tenants
CROSS JOIN (VALUES
 (1, 'GET', '/v1/abac/permissions', 'abac:permissions:read', 'ABAC 权限目录'),
 (2, 'GET', '/v1/abac/metadata', 'abac:metadata:read', 'ABAC 字段元数据'),
 (3, 'GET', '/v1/abac/policies', 'abac:policies:read', 'ABAC 策略读取'),
 (4, 'POST', '/v1/abac/simulations', 'abac:simulate', 'ABAC 模拟求值'),
 (5, 'POST', '/v1/abac/policies', 'abac:policies:create', 'ABAC 策略创建'),
 (6, 'PUT', '/v1/abac/policies/{id}', 'abac:policies:update', 'ABAC 策略更新'),
 (7, 'DELETE', '/v1/abac/policies/{id}', 'abac:policies:delete', 'ABAC 策略删除'),
 (8, 'POST', '/v1/abac/policies/refresh', 'abac:policies:refresh', 'ABAC 缓存刷新')
) operations(operation_order, method, path, code, name)
WHERE NOT EXISTS (SELECT 1 FROM main_permission existing
                   WHERE existing.tenant_by = tenants.tenant_by AND existing.code = operations.code);
