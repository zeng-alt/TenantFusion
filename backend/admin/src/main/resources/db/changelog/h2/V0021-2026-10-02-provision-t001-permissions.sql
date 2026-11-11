--liquibase formatted sql

--changeset zeng:provision-t001-permissions context:dev
-- t001 资源开通：permission 行级隔离后，新租户看不到任何资源定义，
-- 其用户会被 RBAC 全部拒绝，因此开通时复制 master 的全部资源。
-- 复制品 ID 整体 +100000（种子 ID 均小于 20000，无冲突；运行时均为雪花 ID，更无冲突），
-- 表内自引用 parent_id / menu_id 同步 remap，code 保持不变（唯一约束已是租户内）。
-- 随后把 t001 的 ADMIN 角色（2001）绑定到所有复制品（rp ID 用 200000 + 行号）。
-- 生产开新租户时照此逻辑另行执行（未来做成租户开通接口）。
INSERT INTO main_permission
(permission_id, resource_type, code, name, description, is_enabled, is_deleted,
 path, component, redirect, icon, layout, keep_alive, menu_name, menu_style,
 resource_order, is_show, parent_id, method, button_name, menu_id,
 created_by, created_date, last_modified_by, last_modified_date, tenant_by)
SELECT permission_id + 100000, resource_type, code, name, description, is_enabled, is_deleted,
 path, component, redirect, icon, layout, keep_alive, menu_name, menu_style,
 resource_order, is_show,
 CASE WHEN parent_id IS NULL THEN NULL ELSE parent_id + 100000 END,
 method, button_name,
 CASE WHEN menu_id IS NULL THEN NULL ELSE menu_id + 100000 END,
 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 't001'
FROM main_permission WHERE tenant_by = 'master';

INSERT INTO main_role_permission
(role_permission_id, role_id, permission_id, is_deleted,
 created_by, created_date, last_modified_by, last_modified_date, tenant_by)
SELECT 200000 + ROW_NUMBER() OVER (ORDER BY permission_id), 2001, permission_id, FALSE,
 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 't001'
FROM main_permission WHERE tenant_by = 't001';
