-- liquibase formatted sql

-- changeset zeng-alt:25-login-log-location
ALTER TABLE sys_logininfor ADD COLUMN login_location VARCHAR(255);

-- rollback ALTER TABLE sys_logininfor DROP COLUMN login_location;
