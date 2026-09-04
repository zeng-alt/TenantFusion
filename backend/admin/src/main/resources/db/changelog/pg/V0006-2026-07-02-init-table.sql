--liquibase formatted sql

--changeset zeng:init-admin-tables
CREATE TABLE main_user (
    user_id            BIGINT       NOT NULL PRIMARY KEY,
    username           VARCHAR(64),
    password           VARCHAR(255),
    nick_name          VARCHAR(255),
    avatar             VARCHAR(255),
    email              VARCHAR(255),
    phone_number       VARCHAR(255),
    gender             VARCHAR(255),
    is_enabled         BOOLEAN      DEFAULT TRUE,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE TABLE main_role (
    role_id            BIGINT       NOT NULL PRIMARY KEY,
    code               VARCHAR(64),
    name               VARCHAR(255),
    role_sort          INTEGER      DEFAULT 0,
    is_enabled         BOOLEAN      DEFAULT TRUE,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE TABLE main_permission (
    permission_id      BIGINT       NOT NULL PRIMARY KEY,
    resource_type      VARCHAR(31),
    code               VARCHAR(64),
    name               VARCHAR(255),
    description        VARCHAR(500),
    is_enabled         BOOLEAN      DEFAULT TRUE,
    is_deleted         BOOLEAN      DEFAULT FALSE,

    -- MenuResource fields
    path               VARCHAR(255),
    component          VARCHAR(255),
    redirect           VARCHAR(255),
    icon               VARCHAR(255),
    layout             VARCHAR(255),
    keep_alive         VARCHAR(255),
    menu_name          VARCHAR(255),
    menu_style         VARCHAR(255),
    resource_order     INTEGER,
    is_show            BOOLEAN      DEFAULT TRUE,
    parent_id          BIGINT,

    -- HttpResource fields
    method             VARCHAR(10),
    button_name        VARCHAR(255),
    menu_id            BIGINT,

    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE TABLE main_policy_rule (
    policy_rule_id     BIGINT       NOT NULL PRIMARY KEY,
    name               VARCHAR(255),
    description        VARCHAR(255),
    permission_id      BIGINT,
    resource_id        BIGINT,
    condition          TEXT,
    is_pre_auth        BOOLEAN      DEFAULT TRUE,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE TABLE main_user_role (
    user_role_id       BIGINT       NOT NULL PRIMARY KEY,
    user_id            BIGINT,
    role_id            BIGINT,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE TABLE main_role_permission (
    role_permission_id BIGINT       NOT NULL PRIMARY KEY,
    role_id            BIGINT,
    permission_id      BIGINT,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE TABLE main_user_resource (
    user_resource_id   BIGINT       NOT NULL PRIMARY KEY,
    user_id            BIGINT,
    resource_id        BIGINT,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

-- Foreign keys
-- ALTER TABLE main_permission ADD CONSTRAINT fk_perm_parent FOREIGN KEY (parent_id) REFERENCES main_permission(permission_id);
-- ALTER TABLE main_permission ADD CONSTRAINT fk_perm_menu    FOREIGN KEY (menu_id)    REFERENCES main_permission(permission_id);
-- ALTER TABLE main_policy_rule  ADD CONSTRAINT fk_policy_perm   FOREIGN KEY (permission_id) REFERENCES main_permission(permission_id);
-- ALTER TABLE main_user_role    ADD CONSTRAINT fk_ur_user       FOREIGN KEY (user_id)       REFERENCES main_user(user_id);
-- ALTER TABLE main_user_role    ADD CONSTRAINT fk_ur_role       FOREIGN KEY (role_id)       REFERENCES main_role(role_id);
-- ALTER TABLE main_role_permission ADD CONSTRAINT fk_rp_role    FOREIGN KEY (role_id)       REFERENCES main_role(role_id);
-- ALTER TABLE main_role_permission ADD CONSTRAINT fk_rp_perm    FOREIGN KEY (permission_id) REFERENCES main_permission(permission_id);
-- ALTER TABLE main_user_resource   ADD CONSTRAINT fk_ures_user  FOREIGN KEY (user_id)       REFERENCES main_user(user_id);
-- ALTER TABLE main_user_resource   ADD CONSTRAINT fk_ures_res   FOREIGN KEY (resource_id)   REFERENCES main_permission(permission_id);

-- Indexes for main_user
CREATE INDEX idx_main_user_username      ON main_user(username);
CREATE INDEX idx_main_user_email         ON main_user(email);
CREATE INDEX idx_main_user_phone_number  ON main_user(phone_number);
CREATE INDEX idx_main_user_created_date  ON main_user(created_date);

-- Indexes for main_role
CREATE UNIQUE INDEX idx_main_role_code   ON main_role(code);

-- Indexes for main_permission
CREATE INDEX idx_main_perm_resource_type  ON main_permission(resource_type);
CREATE UNIQUE INDEX idx_main_perm_code    ON main_permission(code);
CREATE INDEX idx_main_perm_parent_id      ON main_permission(parent_id);
CREATE INDEX idx_main_perm_menu_id        ON main_permission(menu_id);
CREATE INDEX idx_main_perm_resource_order ON main_permission(resource_order);

-- Indexes for main_policy_rule
CREATE INDEX idx_main_policy_perm_id      ON main_policy_rule(permission_id);
CREATE INDEX idx_main_policy_resource_id  ON main_policy_rule(resource_id);
CREATE INDEX idx_main_policy_pre_auth     ON main_policy_rule(is_pre_auth);

-- Indexes for main_user_role
CREATE INDEX idx_main_ur_user_id          ON main_user_role(user_id);
CREATE INDEX idx_main_ur_role_id          ON main_user_role(role_id);
CREATE UNIQUE INDEX uniq_main_ur_user_role ON main_user_role(user_id, role_id);

-- Indexes for main_role_permission
CREATE INDEX idx_main_rp_role_id          ON main_role_permission(role_id);
CREATE INDEX idx_main_rp_perm_id          ON main_role_permission(permission_id);
CREATE UNIQUE INDEX uniq_main_rp_role_perm ON main_role_permission(role_id, permission_id);

-- Indexes for main_user_resource
CREATE INDEX idx_main_ures_user_id        ON main_user_resource(user_id);
CREATE INDEX idx_main_ures_resource_id    ON main_user_resource(resource_id);
CREATE UNIQUE INDEX uniq_main_ures_user_res ON main_user_resource(user_id, resource_id);

-- Table & column comments
COMMENT ON TABLE main_user              IS '用户';
COMMENT ON COLUMN main_user.user_id            IS '用户ID';
COMMENT ON COLUMN main_user.username           IS '用户名';
COMMENT ON COLUMN main_user.password           IS '密码';
COMMENT ON COLUMN main_user.nick_name          IS '昵称';
COMMENT ON COLUMN main_user.avatar             IS '头像';
COMMENT ON COLUMN main_user.email              IS '邮箱';
COMMENT ON COLUMN main_user.phone_number        IS '手机号';
COMMENT ON COLUMN main_user.gender             IS '性别';
COMMENT ON COLUMN main_user.is_enabled          IS '是否启用';
COMMENT ON COLUMN main_user.is_deleted          IS '是否删除';
COMMENT ON COLUMN main_user.created_by          IS '创建人';
COMMENT ON COLUMN main_user.created_date        IS '创建时间';
COMMENT ON COLUMN main_user.last_modified_by    IS '最后修改人';
COMMENT ON COLUMN main_user.last_modified_date  IS '最后修改时间';

COMMENT ON TABLE main_role              IS '角色';
COMMENT ON COLUMN main_role.role_id            IS '角色ID';
COMMENT ON COLUMN main_role.code               IS '角色编码';
COMMENT ON COLUMN main_role.name               IS '角色名称';
COMMENT ON COLUMN main_role.role_sort          IS '排序';
COMMENT ON COLUMN main_role.is_enabled          IS '是否启用';
COMMENT ON COLUMN main_role.is_deleted          IS '是否删除';
COMMENT ON COLUMN main_role.created_by          IS '创建人';
COMMENT ON COLUMN main_role.created_date        IS '创建时间';
COMMENT ON COLUMN main_role.last_modified_by    IS '最后修改人';
COMMENT ON COLUMN main_role.last_modified_date  IS '最后修改时间';

COMMENT ON TABLE main_permission        IS '权限（菜单/接口单表继承）';
COMMENT ON COLUMN main_permission.permission_id      IS '权限ID';
COMMENT ON COLUMN main_permission.resource_type      IS '资源类型：MENU-菜单 HTTP-接口';
COMMENT ON COLUMN main_permission.code               IS '权限编码';
COMMENT ON COLUMN main_permission.name               IS '权限名称';
COMMENT ON COLUMN main_permission.description         IS '描述';
COMMENT ON COLUMN main_permission.is_enabled          IS '是否启用';
COMMENT ON COLUMN main_permission.path                IS '路由路径（菜单）';
COMMENT ON COLUMN main_permission.component           IS '组件路径（菜单）';
COMMENT ON COLUMN main_permission.redirect            IS '重定向（菜单）';
COMMENT ON COLUMN main_permission.icon                IS '图标（菜单）';
COMMENT ON COLUMN main_permission.layout              IS '布局（菜单）';
COMMENT ON COLUMN main_permission.keep_alive          IS '缓存（菜单）';
COMMENT ON COLUMN main_permission.menu_name           IS '菜单名称';
COMMENT ON COLUMN main_permission.menu_style          IS '菜单样式';
COMMENT ON COLUMN main_permission.resource_order      IS '排序';
COMMENT ON COLUMN main_permission.is_show             IS '是否显示';
COMMENT ON COLUMN main_permission.parent_id           IS '父菜单ID';
COMMENT ON COLUMN main_permission.method              IS '请求方法（接口）：GET/POST/PUT/DELETE';
COMMENT ON COLUMN main_permission.button_name         IS '按钮名称（接口）';
COMMENT ON COLUMN main_permission.menu_id             IS '所属菜单ID（接口）';
COMMENT ON COLUMN main_permission.created_by          IS '创建人';
COMMENT ON COLUMN main_permission.created_date        IS '创建时间';
COMMENT ON COLUMN main_permission.last_modified_by    IS '最后修改人';
COMMENT ON COLUMN main_permission.last_modified_date  IS '最后修改时间';

COMMENT ON TABLE main_policy_rule       IS '策略规则';
COMMENT ON COLUMN main_policy_rule.policy_rule_id     IS '规则ID';
COMMENT ON COLUMN main_policy_rule.name               IS '规则名称';
COMMENT ON COLUMN main_policy_rule.description         IS '规则描述';
COMMENT ON COLUMN main_policy_rule.permission_id       IS '所属权限ID';
COMMENT ON COLUMN main_policy_rule.resource_id         IS '资源ID';
COMMENT ON COLUMN main_policy_rule.condition           IS '条件表达式';
COMMENT ON COLUMN main_policy_rule.is_pre_auth         IS '是否预授权';
COMMENT ON COLUMN main_policy_rule.created_by          IS '创建人';
COMMENT ON COLUMN main_policy_rule.created_date        IS '创建时间';
COMMENT ON COLUMN main_policy_rule.last_modified_by    IS '最后修改人';
COMMENT ON COLUMN main_policy_rule.last_modified_date  IS '最后修改时间';

COMMENT ON TABLE main_user_role         IS '用户角色关联';
COMMENT ON COLUMN main_user_role.user_role_id         IS '关联ID';
COMMENT ON COLUMN main_user_role.user_id              IS '用户ID';
COMMENT ON COLUMN main_user_role.role_id              IS '角色ID';
COMMENT ON COLUMN main_user_role.created_by           IS '创建人';
COMMENT ON COLUMN main_user_role.created_date         IS '创建时间';
COMMENT ON COLUMN main_user_role.last_modified_by     IS '最后修改人';
COMMENT ON COLUMN main_user_role.last_modified_date   IS '最后修改时间';

COMMENT ON TABLE main_role_permission   IS '角色权限关联';
COMMENT ON COLUMN main_role_permission.role_permission_id IS '关联ID';
COMMENT ON COLUMN main_role_permission.role_id         IS '角色ID';
COMMENT ON COLUMN main_role_permission.permission_id   IS '权限ID';
COMMENT ON COLUMN main_role_permission.created_by      IS '创建人';
COMMENT ON COLUMN main_role_permission.created_date    IS '创建时间';
COMMENT ON COLUMN main_role_permission.last_modified_by IS '最后修改人';
COMMENT ON COLUMN main_role_permission.last_modified_date IS '最后修改时间';

COMMENT ON TABLE main_user_resource     IS '用户资源关联';
COMMENT ON COLUMN main_user_resource.user_resource_id   IS '关联ID';
COMMENT ON COLUMN main_user_resource.user_id            IS '用户ID';
COMMENT ON COLUMN main_user_resource.resource_id        IS '资源ID';
COMMENT ON COLUMN main_user_resource.created_by         IS '创建人';
COMMENT ON COLUMN main_user_resource.created_date       IS '创建时间';
COMMENT ON COLUMN main_user_resource.last_modified_by   IS '最后修改人';
COMMENT ON COLUMN main_user_resource.last_modified_date IS '最后修改时间';

-- =============================================
-- 字典类型表
-- =============================================

CREATE TABLE main_dict_type
(
    dict_type_id BIGINT NOT NULL,

    dict_name VARCHAR(100) NOT NULL,

    dict_code VARCHAR(100) NOT NULL,

    is_default BOOLEAN DEFAULT FALSE,

    remark VARCHAR(500),

    tenant_by VARCHAR(64),

    create_time TIMESTAMP,

    update_time TIMESTAMP,

    create_by VARCHAR(64),

    update_by VARCHAR(64),

    deleted BOOLEAN DEFAULT FALSE,

    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP,

    PRIMARY KEY (dict_type_id)
);

COMMENT ON COLUMN main_dict_type.dict_type_id IS '字典类型ID';
COMMENT ON COLUMN main_dict_type.dict_name IS '字典名称';
COMMENT ON COLUMN main_dict_type.dict_code IS '字典类型编码';
COMMENT ON COLUMN main_dict_type.is_default IS '是否默认';
COMMENT ON COLUMN main_dict_type.remark IS '备注';
COMMENT ON COLUMN main_dict_type.tenant_by IS '租户标识';
COMMENT ON COLUMN main_dict_type.create_time IS '创建时间';
COMMENT ON COLUMN main_dict_type.update_time IS '更新时间';
COMMENT ON COLUMN main_dict_type.create_by IS '创建人';
COMMENT ON COLUMN main_dict_type.update_by IS '更新人';
COMMENT ON COLUMN main_dict_type.deleted IS '逻辑删除标识';


-- 字典编码唯一
CREATE UNIQUE INDEX uk_dict_type_code_tenant
    ON main_dict_type(dict_code, tenant_by);


-- 租户查询优化
CREATE INDEX idx_dict_type_tenant
    ON main_dict_type(tenant_by);



-- =============================================
-- 字典数据表
-- =============================================

CREATE TABLE main_dict_data
(
    dict_data_id BIGINT NOT NULL,

    dict_sort INT DEFAULT 0,

    dict_label VARCHAR(100) NOT NULL,

    dict_value VARCHAR(100) NOT NULL,

    css_class VARCHAR(100),

    list_class VARCHAR(100),

    is_default BOOLEAN DEFAULT FALSE,

    remark VARCHAR(500),

    is_enabled BOOLEAN DEFAULT TRUE,

    tenant_by VARCHAR(64),

    dict_code VARCHAR(100) NOT NULL,

    create_time TIMESTAMP,

    update_time TIMESTAMP,

    create_by VARCHAR(64),

    update_by VARCHAR(64),

    deleted BOOLEAN DEFAULT FALSE,

    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP,

    PRIMARY KEY (dict_data_id)
);

COMMENT ON COLUMN main_dict_data.dict_data_id IS '字典数据ID';
COMMENT ON COLUMN main_dict_data.dict_sort IS '字典排序';
COMMENT ON COLUMN main_dict_data.dict_label IS '字典标签';
COMMENT ON COLUMN main_dict_data.dict_value IS '字典键值';
COMMENT ON COLUMN main_dict_data.css_class IS '样式属性';
COMMENT ON COLUMN main_dict_data.list_class IS '表格字典样式';
COMMENT ON COLUMN main_dict_data.is_default IS '是否默认';
COMMENT ON COLUMN main_dict_data.remark IS '备注';
COMMENT ON COLUMN main_dict_data.is_enabled IS '状态(true正常 false停用)';
COMMENT ON COLUMN main_dict_data.tenant_by IS '租户标识';
COMMENT ON COLUMN main_dict_data.dict_code IS '字典类型编码';
COMMENT ON COLUMN main_dict_data.create_time IS '创建时间';
COMMENT ON COLUMN main_dict_data.update_time IS '更新时间';
COMMENT ON COLUMN main_dict_data.create_by IS '创建人';
COMMENT ON COLUMN main_dict_data.update_by IS '更新人';
COMMENT ON COLUMN main_dict_data.deleted IS '逻辑删除标识';



-- =============================================
-- 索引设计
-- =============================================


-- 核心查询:
CREATE INDEX idx_dict_data_query
    ON main_dict_data(
                      dict_code,
                      tenant_by,
                      is_enabled,
                      deleted,
                      dict_sort DESC
        );

CREATE UNIQUE INDEX uk_dict_data_code_value_tenant
    ON main_dict_data(dict_code, dict_value, tenant_by);



-- 根据字典编码 + 值查询
-- 常用于:
-- dictCode + dictValue -> label
CREATE INDEX idx_dict_data_code_value_tenant
    ON main_dict_data(dict_code, dict_value, tenant_by);



-- 查询启用状态字典
CREATE INDEX idx_dict_data_code_enabled_tenant
    ON main_dict_data(dict_code, is_enabled, tenant_by);



-- 租户隔离查询
CREATE INDEX idx_dict_data_tenant
    ON main_dict_data(tenant_by);



-- 逻辑删除过滤
CREATE INDEX idx_dict_data_deleted
    ON main_dict_data(deleted);

