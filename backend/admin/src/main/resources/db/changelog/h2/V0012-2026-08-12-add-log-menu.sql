--liquibase formatted sql

--changeset zeng:add-log-menu
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (301, 'MENU', 'LogMgt', '日志管理', '登录日志与操作日志查看', TRUE, '/pms/log', NULL, NULL, 'i-carbon:document', '', 't', NULL, NULL, 2, TRUE, NULL, NULL, '2026-08-12 00:00:00', NULL, '2026-08-12 00:00:00');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (302, 'MENU', 'LoginLogMgt', '登录日志', '查看系统登录成功与失败日志', TRUE, '/pms/log/login', '/src/views/pms/log/login/index.vue', NULL, 'i-carbon:login', '', 't', NULL, NULL, 1, TRUE, 301, NULL, '2026-08-12 00:00:00', NULL, '2026-08-12 00:00:00');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (303, 'MENU', 'OperLogMgt', '操作日志', '查看系统用户操作日志', TRUE, '/pms/log/oper', '/src/views/pms/log/oper/index.vue', NULL, 'i-carbon:view', '', 't', NULL, NULL, 2, TRUE, 301, NULL, '2026-08-12 00:00:00', NULL, '2026-08-12 00:00:00');
