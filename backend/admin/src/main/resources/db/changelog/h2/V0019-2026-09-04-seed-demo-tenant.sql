--liquibase formatted sql

--changeset zeng:seed-demo-tenant context:dev
-- 仅 dev 用的第二租户，用于验证行级隔离确实生效。
-- t001 走行级隔离（与 master 共享同一张表），master 为超管租户、绕过判别条件。
INSERT INTO main_tenant
(tenant_id, tenant_name, is_enabled, is_deleted, isolation_mode, is_row_isolated, created_by, created_date)
VALUES ('t001', '演示租户一', TRUE, FALSE, 'ROW', TRUE, 'superAdmin', CURRENT_TIMESTAMP);

-- t001 自己的用户；用户名在租户内唯一，故可与 master 的用户重名，这里另起名字便于区分
INSERT INTO main_user
(user_id, username, password, nick_name, email, phone_number, gender, is_enabled, is_deleted, tenant_by)
VALUES (2001, 't001admin', '{noop}123456', 'T001 管理员', 't001@test.com', '13900000001', 'M', TRUE, FALSE, 't001');

-- t001 自己的角色与绑定
INSERT INTO main_role
(role_id, code, name, role_sort, is_enabled, is_deleted, tenant_by, created_by, created_date)
VALUES (2001, 'ADMIN', 'T001 超级管理员', 1, TRUE, FALSE, 't001', 'system', CURRENT_TIMESTAMP);

INSERT INTO main_user_role VALUES
(2001, 2001, 2001, FALSE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

-- t001 私有的字典数据，master 之外的租户不应看到彼此的
INSERT INTO main_dict_type
(dict_type_id, dict_name, dict_code, remark, is_default, tenant_by, created_by, created_date)
VALUES (9001, 'T001 专属字典', 't001_only', NULL, FALSE, 't001', 'system', CURRENT_TIMESTAMP);
