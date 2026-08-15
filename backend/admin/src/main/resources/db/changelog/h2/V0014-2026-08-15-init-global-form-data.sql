--liquibase formatted sql

--changeset zeng:init-global-form-data
CREATE TABLE wf_global_form_data (
    global_form_data_id BIGINT NOT NULL PRIMARY KEY,
    process_instance_id VARCHAR(64)  NOT NULL,
    workflow_code       VARCHAR(128),
    data                CLOB,
    definition          CLOB,
    submitted_date      TIMESTAMP,
    created_by          VARCHAR(255),
    created_date        TIMESTAMP,
    last_modified_by    VARCHAR(255),
    last_modified_date  TIMESTAMP
);

CREATE INDEX idx_wf_gfd_process_instance ON wf_global_form_data(process_instance_id);

COMMENT ON TABLE wf_global_form_data IS '流程全局表单数据';
COMMENT ON COLUMN wf_global_form_data.global_form_data_id IS '数据ID';
COMMENT ON COLUMN wf_global_form_data.process_instance_id IS '运行时流程实例ID';
COMMENT ON COLUMN wf_global_form_data.workflow_code IS '流程模板编码';
COMMENT ON COLUMN wf_global_form_data.data IS '全局表单字段值（JSON：字段名 → 值）';
COMMENT ON COLUMN wf_global_form_data.definition IS '发起流程时保存的全局表单定义快照（CAMUNDA 类型不保存 FormKit 定义）';
COMMENT ON COLUMN wf_global_form_data.submitted_date IS '提交时间';
COMMENT ON COLUMN wf_global_form_data.created_by IS '创建人';
COMMENT ON COLUMN wf_global_form_data.created_date IS '创建时间';
COMMENT ON COLUMN wf_global_form_data.last_modified_by IS '最后修改人';
COMMENT ON COLUMN wf_global_form_data.last_modified_date IS '最后修改时间';
