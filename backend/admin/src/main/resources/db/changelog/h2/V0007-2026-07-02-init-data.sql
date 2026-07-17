--liquibase formatted sql

--changeset zeng:init-admin-data
INSERT INTO main_dict_type (dict_type_id,dict_name,dict_code,remark,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (24,'系统服务','sys_serve',NULL,'${tenantName}','superAdmin','2025-06-11 17:41:47.625825','superAdmin','2025-06-11 17:41:47.625825');
INSERT INTO main_dict_type (dict_type_id,dict_name,dict_code,remark,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (25,'启动状态','enable_status',NULL,'${tenantName}','superAdmin','2025-06-12 10:44:36.9081','superAdmin','2025-06-12 10:44:36.9081');
INSERT INTO main_dict_type (dict_type_id,dict_name,dict_code,remark,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (1,'性别','sys_gender','11111111111111111111111111111111111','${tenantName}',NULL,NULL,'superAdmin','2025-05-19 16:00:02.2916');
INSERT INTO main_dict_type (dict_type_id,dict_name,dict_code,remark,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (23,'资源类型','resource_type',NULL,'${tenantName}','superAdmin','2025-06-09 20:19:11.536751','superAdmin','2025-06-09 20:19:11.536751');

INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (7,3,'其他','100',NULL,'success',TRUE,NULL,TRUE,'sys_gender','${tenantName}','superAdmin','2025-05-17 11:06:56.742978','superAdmin','2025-05-19 21:17:01.591062');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (6,2,'女','1','1111112222','tertiary',TRUE,NULL,TRUE,'sys_gender','${tenantName}','superAdmin','2025-05-17 11:06:43.513282','superAdmin','2025-05-23 14:54:48.873621');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (10,4,'男2','4',NULL,'tertiary',FALSE,NULL,TRUE,'sys_gender','${tenantName}','superAdmin','2025-05-19 20:53:53.192724','superAdmin','2025-06-09 20:15:42.08913');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (16,0,'菜单资源','MENU',NULL,'tertiary',TRUE,NULL,FALSE,'resource_type','${tenantName}','superAdmin','2025-06-09 20:24:38.855148','superAdmin','2025-06-09 20:24:38.855148');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (17,0,'graphql资源','GRAPHQL',NULL,'tertiary',TRUE,NULL,FALSE,'resource_type','${tenantName}','superAdmin','2025-06-09 20:24:55.671454','superAdmin','2025-06-09 20:24:55.671454');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (15,0,'http资源','HTTP',NULL,'default',TRUE,NULL,TRUE,'resource_type','${tenantName}','superAdmin','2025-06-09 20:22:31.935867','superAdmin','2025-06-09 20:29:57.772071');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (18,0,'主服务','main',NULL,'primary',TRUE,NULL,FALSE,'sys_serve','${tenantName}','superAdmin','2025-06-11 17:42:51.871342','superAdmin','2025-06-11 17:42:51.871342');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (19,0,'开启','true',NULL,'success',TRUE,NULL,FALSE,'enable_status','${tenantName}','superAdmin','2025-06-12 10:45:16.236487','superAdmin','2025-06-12 10:45:16.236487');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (20,0,'关闭','false',NULL,'warning',TRUE,NULL,FALSE,'enable_status','${tenantName}','superAdmin','2025-06-12 10:45:36.932463','superAdmin','2025-06-12 10:45:36.932463');


INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (277, 'MENU', 'AssginRoleMgt', '分配用户', NULL, TRUE, '/pms/role/user/:roleId', '/src/views/pms/role/role-user.vue', NULL, 'i-fe:user-plus', 'default', 't', NULL, NULL, 2, TRUE, 283, NULL, '2025-05-22 20:57:26.822246', NULL, '2025-05-22 20:57:26.822246');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (278, 'MENU', 'HttpMgt', 'HTTP资源管理', NULL, TRUE, '/pms/resource/http', '/src/views/pms/resource/http/index.vue', NULL, 'i-carbon:Http', 'default', 't', NULL, NULL, 2, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', NULL, '2025-05-22 20:57:26.822246');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (279, 'MENU', 'SysMgt', '系统管理', NULL, TRUE, '/pms', NULL, NULL, 'i-fe:grid', 'list', 't', NULL, NULL, 1, TRUE, NULL, NULL, '2025-05-22 20:57:26.822246', NULL, '2025-05-22 20:57:26.822246');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (280, 'MENU', 'MenuResourceMgt', '菜单资源管理', NULL, TRUE, '/pms/resource/menu', '/src/views/pms/resource/menu/index.vue', NULL, 'i-fe:menu', 'default', 't', NULL, '', 1, TRUE, 279, 'superAdmin', '2025-05-22 20:57:26.822246', 'superAdmin', '2025-06-12 15:45:49.601032');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (282, 'MENU', 'UserMgt', '用户管理', NULL, TRUE, '/pms/user', '/src/views/pms/user/index.vue', NULL, 'i-fe:user', 'default', 't', NULL, NULL, 4, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-27 22:36:35.047659');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (283, 'MENU', 'RoleMgt', '角色管理', '用于管理角色', TRUE, '/pms/role', '/src/views/pms/role/index.vue', NULL, 'i-ali:role', 'default', 't', NULL, NULL, 5, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-27 22:36:41.126443');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (284, 'MENU', 'DictMgt', '字典管理', '用于管理系统字典', TRUE, '/pms/dict', '/src/views/pms/dict/index.vue', NULL, 'i-ali:dictionary', 'default', 't', NULL, NULL, 6, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-27 22:36:51.993415');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (285, 'MENU', 'SystemPerMgt', '系统参数管理', '用于管理系统参数', TRUE, '/pms/parameter', '/src/views/pms/parameter/index.vue', NULL, 'i-ali:SystemParameter', 'default', 't', NULL, NULL, 7, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-27 22:41:56.283981');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (286, 'MENU', 'FormMgt', '动态表单', NULL, TRUE, '/base/form', '/src/views/base/form.vue', NULL, NULL, 'default', 't', NULL, '', 3, TRUE, 291, 'superAdmin', '2025-06-03 10:31:44.951313', 'superAdmin', '2025-06-03 10:31:44.951313');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (287, 'MENU', 'TenantMgt', '租户管理', '用于管理系统租户', TRUE, '/pms/tenant', '/src/views/pms/tenant/index.vue', NULL, 'i-fa:building-regular', 'default', 't', NULL, NULL, 0, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-29 11:04:44.439517');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (288, 'MENU', 'AbacMgt', 'ABAC权限管理', NULL, TRUE, '/pms/abac', '/src/views/pms/abac/index.vue', NULL, 'i-ma:RuleFilled', 'default', 't', NULL, '', 10, TRUE, 279, 'superAdmin', '2025-06-02 21:29:01.53098', 'superAdmin', '2025-06-02 22:11:45.164261');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (289, 'MENU', 'IconMgt', '图标管理', NULL, TRUE, '/base/uncon', '/src/views/base/unocss-icon.vue', NULL, 'i-fe:feather', 'default', 't', NULL, '', 12, TRUE, 291, 'superAdmin', '2025-06-02 21:39:31.020834', 'superAdmin', '2025-06-02 21:46:40.490375');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (290, 'MENU', 'BaseComponents', '基础组件', NULL, TRUE, '/base/components', '/src/views/base/index.vue', NULL, 'i-me:awesome', 'default', 't', NULL, '', 1, TRUE, 291, 'superAdmin', '2025-06-02 21:51:36.006087', 'superAdmin', '2025-06-02 21:51:50.051997');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (291, 'MENU', 'BaseMgt', '基础管理', NULL, TRUE, '/base', NULL, NULL, 'i-fe:grid', 'default', 't', NULL, '', 0, TRUE, NULL, 'superAdmin', '2025-06-02 21:44:13.358837', 'superAdmin', '2025-06-02 21:52:52.979418');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (292, 'MENU', 'UserProfile', '个人资料', NULL, TRUE, '/profile', '/src/views/profile/index.vue', NULL, 'i-fe:user', 'default', 't', NULL, '', 99, FALSE, NULL, 'superAdmin', '2025-06-06 13:41:01.652181', 'superAdmin', '2025-06-06 14:06:03.455539');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (293, 'MENU', 'SpelMgt', 'spel管理', NULL, TRUE, '/base/spel', '/src/views/base/spel.vue', NULL, NULL, 'default', 't', NULL, '', 10, TRUE, 291, 'superAdmin', '2025-06-06 22:26:44.468541', 'superAdmin', '2025-06-06 22:26:44.468541');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (294, 'MENU', 'SpelInfo', 'Spel文档', NULL, TRUE, '/pms/abac/info', '/src/views/pms/abac/spel-info.vue', NULL, NULL, 'default', 't', NULL, '', 2, TRUE, 288, 'superAdmin', '2025-06-08 22:29:47.252435', 'superAdmin', '2025-06-08 22:29:47.252435');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (295, 'MENU', 'EventMgt', '事件管理', NULL, TRUE, '/pms/event', '/src/views/pms/event/index.vue', NULL, 'i-ma:EventRepeatOutlined', 'default', 't', NULL, '', 14, TRUE, 279, 'superAdmin', '2025-06-03 14:17:34.806673', 'superAdmin', '2025-06-11 15:23:11.691221');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (253, 'HTTP', 'functionCancelAuthorize', '取消所选角色graphql功能权限', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/role/function/cancel/authorize', NULL, '2025-06-23 11:09:18.127688', NULL, '2025-06-23 11:09:18.127688');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (254, 'HTTP', 'button', '获取菜单下的按钮', NULL, TRUE, 'GET', NULL, NULL, '/main/v1/menu/resource/button/{id}', NULL, '2025-06-23 11:09:18.133842', NULL, '2025-06-23 11:09:18.133842');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (255, 'HTTP', 'validateMenuPath', '验证菜单路径', NULL, TRUE, 'GET', NULL, NULL, '/main/v1/menu/resource/validate', NULL, '2025-06-23 11:09:18.136491', NULL, '2025-06-23 11:09:18.136491');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (256, 'HTTP', 'initPassword', '更新密码', NULL, TRUE, 'PUT', NULL, NULL, '/main/v1/user/init/password', NULL, '2025-06-23 11:09:18.139807', NULL, '2025-06-23 11:09:18.139807');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (257, 'HTTP', 'deleteHttpResource', '根据id删除http资源', NULL, TRUE, 'DELETE', NULL, NULL, '/main/v1/http/resource/{id}', NULL, '2025-06-23 11:09:18.14557', NULL, '2025-06-23 11:09:18.14557');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (258, 'HTTP', 'detail', '获取当前用户信息', NULL, TRUE, 'GET', NULL, NULL, '/main/v1/user/detail', NULL, '2025-06-23 11:09:18.149432', NULL, '2025-06-23 11:09:18.149432');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (259, 'HTTP', 'addUserRole', '批量授权用户角色', NULL, TRUE, 'PATCH', NULL, NULL, '/main/v1/user/add/role/{roleId}', NULL, '2025-06-23 11:09:18.153139', NULL, '2025-06-23 11:09:18.153139');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (260, 'HTTP', 'saveUser', '保存或更新用户', NULL, TRUE, 'PUT', NULL, NULL, '/main/v1/user', NULL, '2025-06-23 11:09:18.155804', NULL, '2025-06-23 11:09:18.155804');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (261, 'HTTP', 'tree', '获取当前用户资源树', NULL, TRUE, 'GET', NULL, NULL, '/main/v1/menu/resource/tree', NULL, '2025-06-23 11:09:18.157498', NULL, '2025-06-23 11:09:18.157498');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (262, 'HTTP', 'treeAll', '获取所有资源树', NULL, TRUE, 'GET', NULL, NULL, '/main/v1/menu/resource/tree/all', NULL, '2025-06-23 11:09:18.158834', NULL, '2025-06-23 11:09:18.158834');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (263, 'HTTP', 'treeMenu', '获取所有菜单资源', NULL, TRUE, 'GET', NULL, NULL, '/main/v1/menu/resource/tree/menu', NULL, '2025-06-23 11:09:18.160845', NULL, '2025-06-23 11:09:18.160845');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (264, 'HTTP', 'functionAuthorize', '授权所选角色graphql功能权限', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/role/function/authorize', NULL, '2025-06-23 11:09:18.162839', NULL, '2025-06-23 11:09:18.162839');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (265, 'HTTP', 'detailById', '根据id获取用户信息', NULL, TRUE, 'GET', NULL, NULL, '/main/v1/user/detail/{id}', NULL, '2025-06-23 11:09:18.165566', NULL, '2025-06-23 11:09:18.165566');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (266, 'HTTP', 'authorizePermission', '授权所选角色权限', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/role/authorize/permission', NULL, '2025-06-23 11:09:18.166595', NULL, '2025-06-23 11:09:18.166595');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (267, 'HTTP', 'findRuleByCode', '根据编码查询安全规则', NULL, TRUE, 'GET', NULL, NULL, '/main/v1/policy/rule/findRuleByCode/{code}/{isPreAuth}', NULL, '2025-06-23 11:09:18.169485', NULL, '2025-06-23 11:09:18.169485');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (268, 'HTTP', 'initUserPassword', '初始化用户密码', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/parameter/init/password', NULL, '2025-06-23 11:09:18.171288', NULL, '2025-06-23 11:09:18.171288');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (269, 'HTTP', 'assignRole', '授权用户角色', NULL, TRUE, 'PATCH', NULL, NULL, '/main/v1/user/assign/{userId}', NULL, '2025-06-23 11:09:18.17368', NULL, '2025-06-23 11:09:18.17368');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (270, 'HTTP', 'removeUserRole', '批量取消授权用户角色', NULL, TRUE, 'PATCH', NULL, NULL, '/main/v1/user/remove/role/{roleId}', NULL, '2025-06-23 11:09:18.175727', NULL, '2025-06-23 11:09:18.175727');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (271, 'HTTP', 'saveRule', '保存或更新安全规则', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/policy/rule', NULL, '2025-06-23 11:09:18.177758', NULL, '2025-06-23 11:09:18.177758');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (272, 'HTTP', 'saveRole', '保存或更新角色', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/role', NULL, '2025-06-23 11:09:18.178982', NULL, '2025-06-23 11:09:18.178982');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (273, 'HTTP', 'serviceCancelAuthorize', '取消所选角色graphql服务权限', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/role/service/cancel/authorize', NULL, '2025-06-23 11:09:18.181068', NULL, '2025-06-23 11:09:18.181068');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (274, 'HTTP', 'deleteRole', '根据id删除角色', NULL, TRUE, 'DELETE', NULL, NULL, '/main/v1/role/{id}', NULL, '2025-06-23 11:09:18.183082', NULL, '2025-06-23 11:09:18.183082');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (275, 'HTTP', 'saveHttpResource', '保存或更新http资源', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/http/resource', NULL, '2025-06-23 11:09:18.1846', NULL, '2025-06-23 11:09:18.1846');

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, method, button_name, menu_id, path, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (276, 'HTTP', 'serviceAuthorize', '授权所选角色graphql服务权限', NULL, TRUE, 'POST', NULL, NULL, '/main/v1/role/service/authorize', NULL, '2025-06-23 11:09:18.186232', NULL, '2025-06-23 11:09:18.186232');


INSERT INTO MAIN_USER
(user_id, username, password, nick_name, avatar, email, phone_number, gender, status, is_enabled, is_deleted)
VALUES
    (1001, 'admin', '{noop}123456', '管理员', NULL, 'admin@test.com', '13800000001', 'M', 'ACTIVE', true, false);

INSERT INTO MAIN_USER
(user_id, username, password, nick_name, avatar, email, phone_number, gender, status, is_enabled, is_deleted)
VALUES
    (1002, 'alice', '{noop}123456', 'Alice', NULL, 'alice@test.com', '13800000002', 'F', 'ACTIVE', true, false);

INSERT INTO MAIN_USER
(user_id, username, password, nick_name, avatar, email, phone_number, gender, status, is_enabled, is_deleted)
VALUES
    (1003, 'bob', '{noop}123456', 'Bob', NULL, 'bob@test.com', '13800000003', 'M', 'ACTIVE', true, false);

INSERT INTO MAIN_USER
(user_id, username, password, nick_name, avatar, email, phone_number, gender, status, is_enabled, is_deleted)
VALUES
    (1004, 'locked', '{noop}123456', 'LockedUser', NULL, 'locked@test.com', '13800000004', 'M', 'LOCKED', false, false);