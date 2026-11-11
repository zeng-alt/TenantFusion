--liquibase formatted sql

--changeset zeng:add-tenant-by-columns-2
-- 第二批行级隔离：关联表（user_role/role_permission/user_resource）、ABAC 规则与资源定义。
-- 对应实体已改为继承 TenantBaseEntity，由 Hibernate 的 @TenantId 自动追加
-- where tenant_by = ? 并在插入时回填。
-- 关联表的 (user,role) 之类组合因 Snowflake ID 全局唯一而天然租户一致，
-- tenant_by 仅作判别列与索引，不动原有唯一约束；
-- main_permission(code) 的全局唯一改为租户内唯一，各租户可有相同 code。
ALTER TABLE main_user_role       ADD COLUMN tenant_by VARCHAR(64);
ALTER TABLE main_role_permission ADD COLUMN tenant_by VARCHAR(64);
ALTER TABLE main_user_resource   ADD COLUMN tenant_by VARCHAR(64);
ALTER TABLE main_policy_rule     ADD COLUMN tenant_by VARCHAR(64);
ALTER TABLE main_permission      ADD COLUMN tenant_by VARCHAR(64);

COMMENT ON COLUMN main_user_role.tenant_by       IS '租户标识';
COMMENT ON COLUMN main_role_permission.tenant_by IS '租户标识';
COMMENT ON COLUMN main_user_resource.tenant_by   IS '租户标识';
COMMENT ON COLUMN main_policy_rule.tenant_by    IS '租户标识';
COMMENT ON COLUMN main_permission.tenant_by     IS '租户标识';

-- 判别列会出现在几乎每条查询的 where 里，必须建索引
CREATE INDEX idx_main_user_role_tenant       ON main_user_role(tenant_by);
CREATE INDEX idx_main_role_permission_tenant ON main_role_permission(tenant_by);
CREATE INDEX idx_main_user_resource_tenant   ON main_user_resource(tenant_by);
CREATE INDEX idx_main_policy_rule_tenant    ON main_policy_rule(tenant_by);
CREATE INDEX idx_main_permission_tenant     ON main_permission(tenant_by);

-- main_permission(code) 原本是全局 UNIQUE，先删掉
DROP INDEX idx_main_perm_code;
CREATE UNIQUE INDEX uk_main_perm_code_tenant ON main_permission(code, tenant_by);

-- 回填既有数据为 master，否则判别条件会让它们对任何租户都不可见
UPDATE main_user_role       SET tenant_by = '${tenantName}' WHERE tenant_by IS NULL;
UPDATE main_role_permission SET tenant_by = '${tenantName}' WHERE tenant_by IS NULL;
UPDATE main_user_resource   SET tenant_by = '${tenantName}' WHERE tenant_by IS NULL;
UPDATE main_policy_rule     SET tenant_by = '${tenantName}' WHERE tenant_by IS NULL;
UPDATE main_permission      SET tenant_by = '${tenantName}' WHERE tenant_by IS NULL;
