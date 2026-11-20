--liquibase formatted sql

--changeset zeng:isolate-master-tenant
-- master 与行级租户共用业务表，应使用行级条件而不是跨租户视图。
UPDATE main_tenant
SET isolation_mode = 'ROW', is_row_isolated = TRUE
WHERE tenant_id = 'master' AND isolation_mode = 'NONE' AND is_row_isolated = FALSE
  AND datasource_key IS NULL AND schema_name IS NULL AND table_suffix IS NULL;
