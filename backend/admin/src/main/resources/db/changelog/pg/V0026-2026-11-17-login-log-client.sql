-- liquibase formatted sql

-- changeset zeng-alt:26-login-log-client
ALTER TABLE sys_logininfor ADD COLUMN browser VARCHAR(255);
ALTER TABLE sys_logininfor ADD COLUMN os VARCHAR(255);

-- rollback ALTER TABLE sys_logininfor DROP COLUMN os;
-- rollback ALTER TABLE sys_logininfor DROP COLUMN browser;
