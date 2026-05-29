-- liquibase formatted sql

-- changeset zengJiaJun:1
CREATE TABLE alt_i18n_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    locale VARCHAR(20) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    module VARCHAR(100),
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP
);

-- changeset zengJiaJun:2
CREATE UNIQUE INDEX uk_i18n_message_code_locale ON alt_i18n_message(code, locale);

-- changeset zengJiaJun:3
CREATE INDEX idx_i18n_message_code ON alt_i18n_message(code);

-- changeset zengJiaJun:4
CREATE INDEX idx_i18n_message_locale ON alt_i18n_message(locale);
