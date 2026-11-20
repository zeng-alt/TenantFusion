--liquibase formatted sql

--changeset zeng:abac-method-only
-- HTTP 属性策略退出运行；已有方法规则及其前后置阶段保持不变。
UPDATE main_policy_rule SET is_deleted = TRUE WHERE execution_scope = 'HTTP';
ALTER TABLE main_policy_rule DROP CONSTRAINT chk_main_policy_scope;
ALTER TABLE main_policy_rule DROP COLUMN execution_scope;
ALTER TABLE main_policy_rule DROP COLUMN resource_id;
