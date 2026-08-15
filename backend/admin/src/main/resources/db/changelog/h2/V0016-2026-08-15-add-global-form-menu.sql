--liquibase formatted sql

--changeset zeng:add-global-form-menu
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (304, 'MENU', 'GlobalFormMgt', '全局表单', '全局表单查看与预览（只读）', TRUE, '/template/global-form', '/src/views/template/global-form/index.vue', NULL, 'i-carbon:view', '', 'f', NULL, NULL, 5, TRUE, 296, NULL, '2026-08-15 00:00:00', NULL, '2026-08-15 00:00:00');
