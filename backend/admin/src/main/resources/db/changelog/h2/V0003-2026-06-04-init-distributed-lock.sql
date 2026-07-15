--liquibase formatted sql

--changeset zengJiaJun:1
CREATE TABLE sys_distributed_lock (
    lock_name VARCHAR(255) NOT NULL,
    instance_id VARCHAR(255) NOT NULL,
    locked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_at TIMESTAMP NULL,
    CONSTRAINT pk_sys_distributed_lock PRIMARY KEY (lock_name)
);
