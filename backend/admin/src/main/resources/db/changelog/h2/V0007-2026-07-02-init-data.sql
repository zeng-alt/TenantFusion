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
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (15,0,'http资源','HTTP',NULL,'',TRUE,NULL,TRUE,'resource_type','${tenantName}','superAdmin','2025-06-09 20:22:31.935867','superAdmin','2025-06-09 20:29:57.772071');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (18,0,'主服务','main',NULL,'primary',TRUE,NULL,FALSE,'sys_serve','${tenantName}','superAdmin','2025-06-11 17:42:51.871342','superAdmin','2025-06-11 17:42:51.871342');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (19,0,'开启','true',NULL,'success',TRUE,NULL,FALSE,'enable_status','${tenantName}','superAdmin','2025-06-12 10:45:16.236487','superAdmin','2025-06-12 10:45:16.236487');
INSERT INTO main_dict_data (dict_data_id,dict_sort,dict_label,dict_value,css_class,list_class,is_default,remark,is_enabled,dict_code,tenant_by,created_by,created_date,last_modified_by,last_modified_date) VALUES (20,0,'关闭','false',NULL,'warning',TRUE,NULL,FALSE,'enable_status','${tenantName}','superAdmin','2025-06-12 10:45:36.932463','superAdmin','2025-06-12 10:45:36.932463');


INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (277, 'MENU', 'AssginRoleMgt', '分配用户', NULL, TRUE, '/pms/role/user/:roleId', '/src/views/pms/role/role-user.vue', NULL, 'i-fe:user-plus', '', 't', NULL, NULL, 2, FALSE, 283, NULL, '2025-05-22 20:57:26.822246', NULL, '2025-05-22 20:57:26.822246');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (278, 'MENU', 'HttpMgt', 'HTTP资源管理', NULL, TRUE, '/pms/resource/http', '/src/views/pms/resource/http/index.vue', NULL, 'i-carbon:Http', '', 't', NULL, NULL, 2, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', NULL, '2025-05-22 20:57:26.822246');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (279, 'MENU', 'SysMgt', '系统管理', NULL, TRUE, '/pms', NULL, NULL, 'i-fe:grid', '', 't', NULL, NULL, 1, TRUE, NULL, NULL, '2025-05-22 20:57:26.822246', NULL, '2025-05-22 20:57:26.822246');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (280, 'MENU', 'MenuResourceMgt', '菜单资源管理', NULL, TRUE, '/pms/resource/menu', '/src/views/pms/resource/menu/index.vue', NULL, 'i-fe:menu', '', 't', NULL, '', 1, TRUE, 279, 'superAdmin', '2025-05-22 20:57:26.822246', 'superAdmin', '2025-06-12 15:45:49.601032');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (282, 'MENU', 'UserMgt', '用户管理', NULL, TRUE, '/pms/user', '/src/views/pms/user/index.vue', NULL, 'i-fe:user', '', 't', NULL, NULL, 4, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-27 22:36:35.047659');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (283, 'MENU', 'RoleMgt', '角色管理', '用于管理角色', TRUE, '/pms/role', '/src/views/pms/role/index.vue', NULL, 'i-ali:role', '', 't', NULL, NULL, 5, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-27 22:36:41.126443');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (284, 'MENU', 'DictMgt', '字典管理', '用于管理系统字典', TRUE, '/pms/dict', '/src/views/pms/dict/index.vue', NULL, 'i-ali:dictionary', '', 't', NULL, NULL, 6, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-27 22:36:51.993415');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (285, 'MENU', 'SystemPerMgt', '系统参数管理', '用于管理系统参数', TRUE, '/pms/parameter', '/src/views/pms/parameter/index.vue', NULL, 'i-ali:SystemParameter', '', 't', NULL, NULL, 7, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-27 22:41:56.283981');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (286, 'MENU', 'FormMgt', '动态表单', NULL, TRUE, '/base/form', '/src/views/base/form.vue', NULL, NULL, '', 't', NULL, '', 3, TRUE, 291, 'superAdmin', '2025-06-03 10:31:44.951313', 'superAdmin', '2025-06-03 10:31:44.951313');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (287, 'MENU', 'TenantMgt', '租户管理', '用于管理系统租户', TRUE, '/pms/tenant', '/src/views/pms/tenant/index.vue', NULL, 'i-fa:building-regular', '', 't', NULL, NULL, 0, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', 'superAdmin', '2025-05-29 11:04:44.439517');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (288, 'MENU', 'AbacMgt', 'ABAC权限管理', NULL, TRUE, '/pms/abac', '/src/views/pms/abac/index.vue', NULL, 'i-ma:RuleFilled', '', 't', NULL, '', 10, TRUE, 279, 'superAdmin', '2025-06-02 21:29:01.53098', 'superAdmin', '2025-06-02 22:11:45.164261');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (289, 'MENU', 'IconMgt', '图标管理', NULL, TRUE, '/base/uncon', '/src/views/base/unocss-icon.vue', NULL, 'i-fe:feather', '', 't', NULL, '', 12, TRUE, 291, 'superAdmin', '2025-06-02 21:39:31.020834', 'superAdmin', '2025-06-02 21:46:40.490375');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (290, 'MENU', 'BaseComponents', '基础组件', NULL, TRUE, '/base/components', '/src/views/base/index.vue', NULL, 'i-me:awesome', '', 't', NULL, '', 1, TRUE, 291, 'superAdmin', '2025-06-02 21:51:36.006087', 'superAdmin', '2025-06-02 21:51:50.051997');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (291, 'MENU', 'BaseMgt', '基础管理', NULL, TRUE, '/base', NULL, NULL, 'i-fe:grid', '', 't', NULL, '', 0, TRUE, NULL, 'superAdmin', '2025-06-02 21:44:13.358837', 'superAdmin', '2025-06-02 21:52:52.979418');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (292, 'MENU', 'UserProfile', '个人资料', NULL, TRUE, '/profile', '/src/views/profile/index.vue', NULL, 'i-fe:user', '', 't', NULL, '', 99, FALSE, NULL, 'superAdmin', '2025-06-06 13:41:01.652181', 'superAdmin', '2025-06-06 14:06:03.455539');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (293, 'MENU', 'SpelMgt', 'spel管理', NULL, TRUE, '/base/spel', '/src/views/base/spel.vue', NULL, NULL, '', 't', NULL, '', 10, TRUE, 291, 'superAdmin', '2025-06-06 22:26:44.468541', 'superAdmin', '2025-06-06 22:26:44.468541');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (294, 'MENU', 'SpelInfo', 'Spel文档', NULL, TRUE, '/pms/abac/info', '/src/views/pms/abac/spel-info.vue', NULL, NULL, '', 't', NULL, '', 2, TRUE, 288, 'superAdmin', '2025-06-08 22:29:47.252435', 'superAdmin', '2025-06-08 22:29:47.252435');
INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (295, 'MENU', 'EventMgt', '事件管理', NULL, TRUE, '/pms/event', '/src/views/pms/event/index.vue', NULL, 'i-ma:EventRepeatOutlined', '', 't', NULL, '', 14, TRUE, 279, 'superAdmin', '2025-06-03 14:17:34.806673', 'superAdmin', '2025-06-11 15:23:11.691221');


INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10001,'HTTP','GET:/v1/resource/http/associate','批量关联HTTP资源到菜单','批量关联HTTP资源到菜单',TRUE,FALSE,'POST','/v1/resource/http/associate',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10002,'HTTP','PATCH:/v1/resource/http/{id}/disconnect','取消HTTP资源菜单关联','取消HTTP资源的菜单关联',TRUE,FALSE,'PATCH','/v1/resource/http/{id}/disconnect',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10003,'HTTP','GET:/v1/resource/http/page','分页查询HTTP资源','分页查询HTTP资源列表',TRUE,FALSE,'GET','/v1/resource/http/page',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10004,'HTTP','GET:/v1/menu/resource/validate','验证菜单路径','验证菜单路径',TRUE,FALSE,'GET','/v1/menu/resource/validate',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10005,'HTTP','GET:/v1/menu/resource/tree','获取当前用户资源树','获取当前用户资源树',TRUE,FALSE,'GET','/v1/menu/resource/tree',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10006,'HTTP','GET:/v1/menu/resource/tree/menu','获取所有菜单资源','获取所有菜单资源',TRUE,FALSE,'GET','/v1/menu/resource/tree/menu',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10007,'HTTP','GET:/v1/menu/resource/tree/enable/all','获取所有开启资源树','获取所有开启的资源树',TRUE,FALSE,'GET','/v1/menu/resource/tree/enable/all',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10008,'HTTP','GET:/v1/menu/resource/tree/all','获取所有资源树','获取所有资源树',TRUE,FALSE,'GET','/v1/menu/resource/tree/all',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10009,'HTTP','GET:/v1/menu/resource/button/{id}','获取菜单按钮','获取菜单下按钮',TRUE,FALSE,'GET','/v1/menu/resource/button/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10010,'HTTP','GET:/v1/user/detail','获取当前登录用户信息','获取当前登录用户信息',TRUE,FALSE,'GET','/v1/user/detail',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10011,'HTTP','GET:/hello','Hello接口','测试接口',TRUE,FALSE,'GET','/hello',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10012,'HTTP','GET:/v1/auth/captcha','获取验证码','认证验证码接口',TRUE,FALSE,'GET','/v1/auth/captcha',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10013,'HTTP','POST:/v1/login/jwt','JWT登录','用户登录接口',TRUE,FALSE,'POST','/v1/login/jwt',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10014,'HTTP','POST:/v1/logout/jwt','JWT登出','用户退出登录',TRUE,FALSE,'POST','/v1/logout/jwt',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10015,'HTTP','GET:/v1/dict/data','分页查询DictData','分页查询DictData列表',TRUE,FALSE,'GET','/v1/dict/data',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10016,'HTTP','POST:/v1/dict/data','新增DictData','创建新的DictData记录',TRUE,FALSE,'POST','/v1/dict/data',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10017,'HTTP','GET:/v1/dict/data/all','查询所有DictData','条件查询所有DictData不分页',TRUE,FALSE,'GET','/v1/dict/data/all',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10018,'HTTP','GET:/v1/dict/data/{id}','获取DictData详情','根据ID获取DictData详情',TRUE,FALSE,'GET','/v1/dict/data/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10019,'HTTP','PUT:/v1/dict/data/{id}','更新DictData','全量更新DictData',TRUE,FALSE,'PUT','/v1/dict/data/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10020,'HTTP','PATCH:/v1/dict/data/{id}','部分更新DictData','部分更新DictData',TRUE,FALSE,'PATCH','/v1/dict/data/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10021,'HTTP','DELETE:/v1/dict/data/{ids}','批量删除DictData','批量删除DictData',TRUE,FALSE,'DELETE','/v1/dict/data/{ids}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10022,'HTTP','PUT:/v1/dict/data/sort','DictData排序','批量重排序DictData',TRUE,FALSE,'PUT','/v1/dict/data/sort',CURRENT_TIMESTAMP);

INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10023,'HTTP','GET:/v1/dict/type','分页查询DictType','分页查询DictType列表',TRUE,FALSE,'GET','/v1/dict/type',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10024,'HTTP','POST:/v1/dict/type','新增DictType','创建新的DictType记录',TRUE,FALSE,'POST','/v1/dict/type',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10025,'HTTP','GET:/v1/dict/type/{id}','获取DictType详情','根据ID获取DictType详情',TRUE,FALSE,'GET','/v1/dict/type/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10026,'HTTP','PUT:/v1/dict/type/{id}','更新DictType','全量更新DictType',TRUE,FALSE,'PUT','/v1/dict/type/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10027,'HTTP','PATCH:/v1/dict/type/{id}','部分更新DictType','部分更新DictType',TRUE,FALSE,'PATCH','/v1/dict/type/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10028,'HTTP','DELETE:/v1/dict/type/{ids}','批量删除DictType','批量删除DictType',TRUE,FALSE,'DELETE','/v1/dict/type/{ids}',CURRENT_TIMESTAMP);

INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10029,'HTTP','GET:/v1/http','分页查询HttpResource','分页查询HttpResource列表',TRUE,FALSE,'GET','/v1/http',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10030,'HTTP','POST:/v1/http','新增HttpResource','创建新的HttpResource记录',TRUE,FALSE,'POST','/v1/http',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10031,'HTTP','GET:/v1/http/{id}','获取HttpResource详情','根据ID获取HttpResource详情',TRUE,FALSE,'GET','/v1/http/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10032,'HTTP','PUT:/v1/http/{id}','更新HttpResource','全量更新HttpResource',TRUE,FALSE,'PUT','/v1/http/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10033,'HTTP','PATCH:/v1/http/{id}','部分更新HttpResource','部分更新HttpResource',TRUE,FALSE,'PATCH','/v1/http/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10034,'HTTP','DELETE:/v1/http/{ids}','批量删除HttpResource','批量删除HttpResource',TRUE,FALSE,'DELETE','/v1/http/{ids}',CURRENT_TIMESTAMP);

INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10035,'HTTP','GET:/v1/menu','分页查询MenuResource','分页查询MenuResource列表',TRUE,FALSE,'GET','/v1/menu',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10036,'HTTP','POST:/v1/menu','新增MenuResource','创建新的MenuResource记录',TRUE,FALSE,'POST','/v1/menu',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10037,'HTTP','GET:/v1/menu/{id}','获取MenuResource详情','根据ID获取MenuResource详情',TRUE,FALSE,'GET','/v1/menu/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10038,'HTTP','PUT:/v1/menu/{id}','更新MenuResource','全量更新MenuResource',TRUE,FALSE,'PUT','/v1/menu/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10039,'HTTP','PATCH:/v1/menu/{id}','部分更新MenuResource','部分更新MenuResource',TRUE,FALSE,'PATCH','/v1/menu/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10040,'HTTP','DELETE:/v1/menu/{ids}','批量删除MenuResource','批量删除MenuResource',TRUE,FALSE,'DELETE','/v1/menu/{ids}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10041,'HTTP','PUT:/v1/menu/sort','MenuResource排序','批量重排序MenuResource',TRUE,FALSE,'PUT','/v1/menu/sort',CURRENT_TIMESTAMP);

INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10042,'HTTP','GET:/v1/role','分页查询Role','分页查询Role列表',TRUE,FALSE,'GET','/v1/role',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10043,'HTTP','GET:/v1/role/all','查询所有Role','条件查询所有Role不分页',TRUE,FALSE,'GET','/v1/role/all',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10044,'HTTP','GET:/v1/role/{id}','获取Role详情','根据ID获取Role详情',TRUE,FALSE,'GET','/v1/role/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10045,'HTTP','PATCH:/v1/role/{id}','更新Role','部分更新Role',TRUE,FALSE,'PATCH','/v1/role/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10046,'HTTP','DELETE:/v1/role/{ids}','删除Role','批量删除Role',TRUE,FALSE,'DELETE','/v1/role/{ids}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10047,'HTTP','PATCH:/v1/role/users/add/{roleId}','添加角色用户','给角色添加用户',TRUE,FALSE,'PATCH','/v1/role/users/add/{roleId}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10048,'HTTP','PATCH:/v1/role/users/remove/{roleId}','移除角色用户','从角色移除用户',TRUE,FALSE,'PATCH','/v1/role/users/remove/{roleId}',CURRENT_TIMESTAMP);

INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10049,'HTTP','GET:/v1/user','分页查询User','分页查询User列表',TRUE,FALSE,'GET','/v1/user',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10050,'HTTP','GET:/v1/user/{id}','获取User详情','根据ID获取User详情',TRUE,FALSE,'GET','/v1/user/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10051,'HTTP','PATCH:/v1/user/{id}','更新User','部分更新User',TRUE,FALSE,'PATCH','/v1/user/{id}',CURRENT_TIMESTAMP);
INSERT INTO main_permission(permission_id,resource_type,code,name,description,is_enabled,is_deleted,method,path,created_date) VALUES(10052,'HTTP','DELETE:/v1/user/{ids}','删除User','批量删除User',TRUE,FALSE,'DELETE','/v1/user/{ids}',CURRENT_TIMESTAMP);

INSERT INTO MAIN_USER
(user_id, username, password, nick_name, avatar, email, phone_number, gender, is_enabled, is_deleted)
VALUES
    (1, 'admin', '{noop}123456', '管理员', NULL, 'admin@test.com', '13800000001', 'M', true, false);

INSERT INTO MAIN_USER
(user_id, username, password, nick_name, avatar, email, phone_number, gender, is_enabled, is_deleted)
VALUES
    (1002, 'alice', '{noop}123456', 'Alice', NULL, 'alice@test.com', '13800000002', 'F', true, false);

INSERT INTO MAIN_USER
(user_id, username, password, nick_name, avatar, email, phone_number, gender, is_enabled, is_deleted)
VALUES
    (1003, 'bob', '{noop}123456', 'Bob', NULL, 'bob@test.com', '13800000003', 'M', true, false);

INSERT INTO MAIN_USER
(user_id, username, password, nick_name, avatar, email, phone_number, gender, is_enabled, is_deleted)
VALUES
    (1004, 'locked', '{noop}123456', 'LockedUser', NULL, 'locked@test.com', '13800000004', 'M', false, false);


INSERT INTO main_role (role_id, code, name, role_sort, is_enabled, is_deleted, created_by, created_date, last_modified_by, last_modified_date) VALUES
(1, 'ADMIN', '超级管理员', 1, TRUE, FALSE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(2, 'USER', '普通用户', 2, TRUE, FALSE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(3, 'MANAGER', '管理员', 3, TRUE, FALSE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(4, 'AUDITOR', '审计员', 4, TRUE, FALSE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(5, 'GUEST', '访客', 5, TRUE, FALSE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);


INSERT INTO main_user_role VALUES
(1, 1, 3, FALSE, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
(2, 1, 1, FALSE, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP);

INSERT INTO main_role_permission VALUES
(1, 3, 10010, FALSE, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP),
(2, 3, 10005, FALSE, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP);