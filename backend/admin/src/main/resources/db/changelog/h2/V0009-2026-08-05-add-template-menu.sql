--liquibase formatted sql

--changeset zeng:add-template-menu
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (296, 'MENU', 'TemplateMgt', '模板管理', '表单模板与流程模板管理', TRUE, '/template', NULL, NULL, 'i-carbon:template', '', 't', NULL, NULL, 3, TRUE, NULL, NULL, '2026-08-05 00:00:00', NULL, '2026-08-05 00:00:00');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (297, 'MENU', 'FormTemplateMgt', '表单模板', '可视化表单模板管理', TRUE, '/template/form', '/src/views/template/form/index.vue', NULL, 'i-carbon:document', '', 't', NULL, NULL, 1, TRUE, 296, NULL, '2026-08-05 00:00:00', NULL, '2026-08-05 00:00:00');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (298, 'MENU', 'ProcessTemplateMgt', '流程模板', 'Camunda BPMN 流程模板管理', TRUE, '/template/process', '/src/views/template/process/index.vue', NULL, 'i-carbon:flow-modeler', '', 't', NULL, NULL, 2, TRUE, 296, NULL, '2026-08-05 00:00:00', NULL, '2026-08-05 00:00:00');
