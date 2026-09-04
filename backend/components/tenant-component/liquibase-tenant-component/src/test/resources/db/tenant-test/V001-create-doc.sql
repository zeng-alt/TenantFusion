--liquibase formatted sql

--changeset test:create-doc
CREATE TABLE tenant_doc (
    id     BIGINT PRIMARY KEY,
    title  VARCHAR(100),
    owner  VARCHAR(64)
);

-- 用 ${tenantName} 验证参数确实按租户替换，而不是всегда取主库那个值
INSERT INTO tenant_doc (id, title, owner) VALUES (1, '归属校验', '${tenantName}');
