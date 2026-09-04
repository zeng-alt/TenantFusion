--liquibase formatted sql

--changeset zeng:add-tenant-by-columns
-- 让 main_user / main_role / main_department 真正参与行级隔离。
-- 对应实体已改为继承 TenantBaseEntity，由 Hibernate 的 @TenantId 自动追加
-- where tenant_by = ? 并在插入时回填。
-- 其余实体暂不纳入，仍为全租户共享。
ALTER TABLE main_user       ADD COLUMN tenant_by VARCHAR(64);
ALTER TABLE main_role       ADD COLUMN tenant_by VARCHAR(64);
ALTER TABLE main_department ADD COLUMN tenant_by VARCHAR(64);

COMMENT ON COLUMN main_user.tenant_by       IS '租户标识';
COMMENT ON COLUMN main_role.tenant_by       IS '租户标识';
COMMENT ON COLUMN main_department.tenant_by IS '租户标识';

-- 判别列会出现在几乎每条查询的 where 里，必须建索引
CREATE INDEX idx_main_user_tenant       ON main_user(tenant_by);
CREATE INDEX idx_main_role_tenant       ON main_role(tenant_by);
CREATE INDEX idx_main_department_tenant ON main_department(tenant_by);

-- 全局唯一约束必须改为租户内唯一，否则两个租户无法拥有同名角色，行级隔离形同虚设。
-- main_role(code) 原本是全局 UNIQUE，先删掉。
DROP INDEX idx_main_role_code;
CREATE UNIQUE INDEX uk_main_role_code_tenant ON main_role(code, tenant_by);

-- main_user(username) 原本只是普通索引，唯一性由实体上的 @UniqueCheck 在应用层校验。
-- 那个校验走 Hibernate 查询，会自动带上租户判别条件，因此天然变成租户内唯一；
-- 这里再补一条数据库级约束兜底。
CREATE UNIQUE INDEX uk_main_user_username_tenant ON main_user(username, tenant_by);

-- 回填既有数据，否则判别条件会让它们对任何租户都不可见
UPDATE main_user       SET tenant_by = '${tenantName}' WHERE tenant_by IS NULL;
UPDATE main_role       SET tenant_by = '${tenantName}' WHERE tenant_by IS NULL;
UPDATE main_department SET tenant_by = '${tenantName}' WHERE tenant_by IS NULL;
