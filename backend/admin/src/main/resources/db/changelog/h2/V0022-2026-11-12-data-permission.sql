-- liquibase formatted sql
-- changeset zeng-alt:20261112-data-permission-h2
CREATE TABLE main_data_permission (
    data_permission_id BIGINT PRIMARY KEY,
    tenant_by VARCHAR(64) NOT NULL,
    resource VARCHAR(64) NOT NULL,
    permission VARCHAR(256) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    scope_type VARCHAR(16) NOT NULL CHECK (scope_type IN ('SELF', 'DEPT', 'DEPT_TREE', 'DEPT_SET', 'ALL')),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64), created_date TIMESTAMP,
    last_modified_by VARCHAR(64), last_modified_date TIMESTAMP
);
CREATE INDEX idx_data_permission_lookup ON main_data_permission (tenant_by, resource, permission, role_code);
CREATE TABLE main_data_permission_department (
    data_permission_id BIGINT NOT NULL REFERENCES main_data_permission(data_permission_id) ON DELETE CASCADE,
    dept_id BIGINT NOT NULL,
    PRIMARY KEY (data_permission_id, dept_id)
);

-- 用户详情路由只需一个正则转义；修正旧初始化脚本存入的双反斜杠。
UPDATE main_permission SET path = '/v1/user/{id:\d+}', code = 'GET:/v1/user/{id:\d+}'
WHERE method = 'GET' AND path LIKE '/v1/user/{id:%';
INSERT INTO main_permission
    (permission_id, resource_type, code, name, method, path, tenant_by, is_enabled, is_deleted)
VALUES
    (20261112001, 'HTTP', 'GET:/v1/user/info', '查询用户卡片', 'GET', '/v1/user/info', '${tenantName}', TRUE, FALSE),
    (20261112002, 'HTTP', 'POST:/v1/user/search', '用户高级搜索', 'POST', '/v1/user/search', '${tenantName}', TRUE, FALSE);

-- rollback DELETE FROM main_role_permission WHERE permission_id IN (20261112001, 20261112002);
-- rollback DELETE FROM main_permission WHERE permission_id IN (20261112001, 20261112002);
-- rollback DROP TABLE main_data_permission_department;
-- rollback DROP TABLE main_data_permission;
