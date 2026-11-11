--liquibase formatted sql

--changeset zeng:init-tenant-store
-- 租户库建表语句（由业务 changelog 的 V0013 + V0017 合并而来，建表即带路由列）。
-- 四个互相独立、可叠加的隔离旋钮：
--   datasource_key   非空 -> 库级隔离，取 alt.tenant.datasources.<key> 下的配置
--   schema_name      非空 -> 模式级隔离
--   table_suffix     非空 -> 表级隔离
--   is_row_isolated  为真 -> 行级判别列
-- isolation_mode 只是常用组合的预设入口，具体名字缺省时按 tenant_id 推导。
-- 注意：租户数据源的 url/账号/口令刻意不落库，只存 datasource_key，
-- 真实凭据放配置并由环境变量注入。
CREATE TABLE main_tenant (
    tenant_id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    tenant_name        VARCHAR(255),
    isolation_mode     VARCHAR(16),
    datasource_key     VARCHAR(64),
    schema_name        VARCHAR(64),
    table_suffix       VARCHAR(32),
    is_row_isolated    BOOLEAN,
    is_enabled         BOOLEAN      DEFAULT TRUE,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE UNIQUE INDEX uk_main_tenant_id ON main_tenant(tenant_id);

-- master 作为超管租户不参与隔离，其请求可跨租户查看
INSERT INTO main_tenant
(tenant_id, tenant_name, isolation_mode, datasource_key, schema_name, table_suffix,
 is_row_isolated, is_enabled, is_deleted, created_by, created_date, last_modified_by, last_modified_date)
VALUES ('master', '主租户', 'NONE', NULL, NULL, NULL, FALSE,
 TRUE, FALSE, 'superAdmin', '2026-08-15 00:00:00', NULL, NULL);

COMMENT ON TABLE main_tenant IS '租户';
COMMENT ON COLUMN main_tenant.tenant_id            IS '租户ID';
COMMENT ON COLUMN main_tenant.tenant_name          IS '租户名称';
COMMENT ON COLUMN main_tenant.isolation_mode       IS '隔离预设：NONE/ROW/TABLE/SCHEMA/DATABASE';
COMMENT ON COLUMN main_tenant.datasource_key       IS '库级隔离的数据源键，对应 alt.tenant.datasources';
COMMENT ON COLUMN main_tenant.schema_name          IS '模式级隔离的 schema 名';
COMMENT ON COLUMN main_tenant.table_suffix         IS '表级隔离的表名后缀';
COMMENT ON COLUMN main_tenant.is_row_isolated      IS '是否启用行级判别列';
COMMENT ON COLUMN main_tenant.is_enabled           IS '是否启用';
COMMENT ON COLUMN main_tenant.is_deleted           IS '是否删除';
COMMENT ON COLUMN main_tenant.created_by           IS '创建人';
COMMENT ON COLUMN main_tenant.created_date         IS '创建时间';
COMMENT ON COLUMN main_tenant.last_modified_by     IS '最后修改人';
COMMENT ON COLUMN main_tenant.last_modified_date   IS '最后修改时间';
