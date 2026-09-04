--liquibase formatted sql

--changeset zeng:add-business-menu
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (300, 'MENU', 'BusinessMgt', '业务管理', '业务树与关联配置表单管理', TRUE, '/template/business', '/src/views/template/business/index.vue', NULL, 'i-carbon:tree-view-alt', '', 'f', NULL, NULL, 4, TRUE, 296, NULL, '2026-08-09 00:00:00', NULL, '2026-08-09 00:00:00');
