--liquibase formatted sql

--changeset zeng:add-tenant-routing-columns
-- 多租户路由元数据。四列对应四个互相独立、可叠加的隔离旋钮：
--   datasource_key   非空 -> 库级隔离，取 alt.tenant.datasources.<key> 下的配置
--   schema_name      非空 -> 模式级隔离
--   table_suffix     非空 -> 表级隔离
--   is_row_isolated  为真 -> 行级判别列
-- isolation_mode 只是常用组合的预设入口，具体名字缺省时按 tenant_id 推导。
-- 注意：租户数据源的 url/账号/口令刻意不落库，只存 datasource_key，
-- 真实凭据放配置并由环境变量注入。
ALTER TABLE main_tenant ADD COLUMN isolation_mode   VARCHAR(16);
ALTER TABLE main_tenant ADD COLUMN datasource_key   VARCHAR(64);
ALTER TABLE main_tenant ADD COLUMN schema_name      VARCHAR(64);
ALTER TABLE main_tenant ADD COLUMN table_suffix     VARCHAR(32);
ALTER TABLE main_tenant ADD COLUMN is_row_isolated  BOOLEAN;

COMMENT ON COLUMN main_tenant.isolation_mode  IS '隔离预设：NONE/ROW/TABLE/SCHEMA/DATABASE';
COMMENT ON COLUMN main_tenant.datasource_key  IS '库级隔离的数据源键，对应 alt.tenant.datasources';
COMMENT ON COLUMN main_tenant.schema_name     IS '模式级隔离的 schema 名';
COMMENT ON COLUMN main_tenant.table_suffix    IS '表级隔离的表名后缀';
COMMENT ON COLUMN main_tenant.is_row_isolated IS '是否启用行级判别列';

-- master 作为超管租户不参与隔离，其请求可跨租户查看
UPDATE main_tenant SET isolation_mode = 'NONE', is_row_isolated = FALSE WHERE tenant_id = 'master';
