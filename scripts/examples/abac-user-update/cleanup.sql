-- 移除本示例创建的数据。清理后重启 Admin，移除演示 RBAC 缓存并重新登录。
BEGIN;
DELETE FROM main_policy_rule WHERE policy_rule_id IN (862030, 862031) AND tenant_by = 't001';
DELETE FROM main_role_permission WHERE role_permission_id IN (862050, 862051) AND tenant_by = 't001';
DELETE FROM main_role_permission WHERE role_id = 862011 AND tenant_by = 't001';
DELETE FROM main_user_role WHERE user_role_id IN (862041, 862042, 862043) AND tenant_by = 't001';
DELETE FROM main_permission WHERE permission_id = 862020 AND tenant_by = 't001';
DELETE FROM main_role WHERE role_id IN (862010, 862011) AND tenant_by = 't001';
DELETE FROM main_user WHERE user_id IN (862001, 862002, 862003, 862004, 862005) AND tenant_by = 't001';
DELETE FROM main_department WHERE dept_id IN (862101, 862102) AND tenant_by = 't001';
COMMIT;
