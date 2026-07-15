--liquibase formatted sql

--changeset zengJiaJun:1
-- 缩略图相关字段：file_type, thumbnail_name, thumbnail_url, thumbnail_width, thumbnail_height
ALTER TABLE sys_oss_file ADD COLUMN file_type VARCHAR(20);
ALTER TABLE sys_oss_file ADD COLUMN thumbnail_name VARCHAR(500);
ALTER TABLE sys_oss_file ADD COLUMN thumbnail_url VARCHAR(2048);
ALTER TABLE sys_oss_file ADD COLUMN thumbnail_width INT;
ALTER TABLE sys_oss_file ADD COLUMN thumbnail_height INT;

--changeset zengJiaJun:2
-- 分片上传相关字段：upload_id, upload_status
ALTER TABLE sys_oss_file ADD COLUMN upload_id VARCHAR(255);
ALTER TABLE sys_oss_file ADD COLUMN upload_status VARCHAR(20);

--changeset zengJiaJun:3
-- 分片上传索引
CREATE INDEX idx_sys_oss_file_upload_id ON sys_oss_file(upload_id);
CREATE INDEX idx_sys_oss_file_file_type ON sys_oss_file(file_type);
CREATE INDEX idx_sys_oss_file_upload_status ON sys_oss_file(upload_status);
