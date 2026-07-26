--liquibase formatted sql

--changeset zeng:add-department
CREATE TABLE main_department (
    dept_id            BIGINT       NOT NULL PRIMARY KEY,
    dept_name          VARCHAR(128),
    dept_sort          INTEGER      DEFAULT 0,
    is_enabled         BOOLEAN      DEFAULT TRUE,
    is_deleted         BOOLEAN      DEFAULT FALSE,
    remark             VARCHAR(500),
    parent_id          BIGINT,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP
);

CREATE INDEX idx_main_dept_parent_id   ON main_department(parent_id);
CREATE INDEX idx_main_dept_dept_sort   ON main_department(dept_sort);
CREATE INDEX idx_main_dept_is_enabled  ON main_department(is_enabled);

ALTER TABLE main_user ADD COLUMN dept_id BIGINT;
CREATE INDEX idx_main_user_dept_id     ON main_user(dept_id);

COMMENT ON TABLE main_department IS '部门';
COMMENT ON COLUMN main_department.dept_id IS '部门ID';
COMMENT ON COLUMN main_department.dept_name IS '部门名称';
COMMENT ON COLUMN main_department.dept_sort IS '排序';
COMMENT ON COLUMN main_department.is_enabled IS '是否启用';
COMMENT ON COLUMN main_department.remark IS '备注';
COMMENT ON COLUMN main_department.parent_id IS '上级部门ID';

INSERT INTO MAIN_PERMISSION (permission_id, resource_type, code, name, description, is_enabled, path, component, redirect, icon, layout, keep_alive, menu_name, menu_style, resource_order, is_show, parent_id, created_by, created_date, last_modified_by, last_modified_date) VALUES
    (2000, 'MENU', 'DeptMgt', '部门管理', NULL, TRUE, '/pms/dept', '/src/views/pms/dept/index.vue', NULL, '', '', 't', NULL, NULL, 2, TRUE, 279, NULL, '2025-05-22 20:57:26.822246', NULL, '2025-05-22 20:57:26.822246');

