-- liquibase formatted sql

-- changeset system:1
CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    completion_date TIMESTAMP(6) WITH TIME ZONE,
    event_type VARCHAR(500) NOT NULL,
    listener_id VARCHAR(255) NOT NULL,
    publication_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    serialized_event TEXT NOT NULL
);