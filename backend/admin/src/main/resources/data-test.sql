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