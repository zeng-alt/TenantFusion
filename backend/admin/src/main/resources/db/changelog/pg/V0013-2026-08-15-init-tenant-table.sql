--liquibase formatted sql

--changeset zeng:init-tenant-table
CREATE TABLE main_tenant (
    tenant_id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    tenant_name        VARCHAR(255),
    is_enabled         BOOLEAN      DEFAULT TRUE,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE UNIQUE INDEX uk_main_tenant_id ON main_tenant(tenant_id);

INSERT INTO main_tenant (tenant_id, tenant_name, is_enabled, is_deleted, created_by, created_date, last_modified_by, last_modified_date)
VALUES ('master', '主租户', TRUE, FALSE, 'superAdmin', '2026-08-15 00:00:00', NULL, NULL);

COMMENT ON TABLE main_tenant IS '租户';
COMMENT ON COLUMN main_tenant.tenant_id            IS '租户ID';
COMMENT ON COLUMN main_tenant.tenant_name          IS '租户名称';
COMMENT ON COLUMN main_tenant.is_enabled           IS '是否启用';
COMMENT ON COLUMN main_tenant.is_deleted           IS '是否删除';
COMMENT ON COLUMN main_tenant.created_by           IS '创建人';
COMMENT ON COLUMN main_tenant.created_date         IS '创建时间';
COMMENT ON COLUMN main_tenant.last_modified_by     IS '最后修改人';
COMMENT ON COLUMN main_tenant.last_modified_date   IS '最后修改时间';
