--liquibase formatted sql

--changeset zeng:seed-hybrid-demo-tenants context:dev
-- 混合模式演示租户（dev）：t_schema 走模式级隔离，t_db 走库级隔离。
-- 两者同时开行级判别（叠加组合），种子与校验逻辑与 t001 一致。
-- 注意：租户 schema/库的表结构与种子由 TenantLiquibaseRunner 在启动期自动迁移，
-- 无需手工建表；登录账号均为 admin/123456（各存储内独立的同名行）。
INSERT INTO main_tenant
(tenant_id, tenant_name, isolation_mode, datasource_key, schema_name, table_suffix,
 is_row_isolated, is_enabled, is_deleted, created_by, created_date)
VALUES ('t_schema', '演示模式租户', 'SCHEMA', NULL, 'sch_demo', NULL, TRUE,
 TRUE, FALSE, 'superAdmin', CURRENT_TIMESTAMP);

INSERT INTO main_tenant
(tenant_id, tenant_name, isolation_mode, datasource_key, schema_name, table_suffix,
 is_row_isolated, is_enabled, is_deleted, created_by, created_date)
VALUES ('t_db', '演示独立库租户', 'DATABASE', 'tdb', NULL, NULL, TRUE,
 TRUE, FALSE, 'superAdmin', CURRENT_TIMESTAMP);
