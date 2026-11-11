--liquibase formatted sql

--changeset zeng:seed-demo-tenant context:dev
-- 仅 dev 用的第二租户，用于验证行级隔离确实生效。
-- t001 走行级隔离（与 master 共享同一张业务表），master 为超管租户、绕过判别条件。
INSERT INTO main_tenant
(tenant_id, tenant_name, isolation_mode, datasource_key, schema_name, table_suffix,
 is_row_isolated, is_enabled, is_deleted, created_by, created_date)
VALUES ('t001', '演示租户一', 'ROW', NULL, NULL, NULL, TRUE,
 TRUE, FALSE, 'superAdmin', CURRENT_TIMESTAMP);
