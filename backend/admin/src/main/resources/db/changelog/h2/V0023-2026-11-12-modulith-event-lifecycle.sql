-- liquibase formatted sql

-- changeset zeng-alt:23-modulith-event-lifecycle
ALTER TABLE event_publication ADD COLUMN completion_attempts INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE event_publication ADD COLUMN last_resubmission_date TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE event_publication ADD COLUMN status VARCHAR(20);
UPDATE event_publication
SET status = CASE WHEN completion_date IS NULL THEN 'PUBLISHED' ELSE 'COMPLETED' END;

-- rollback ALTER TABLE event_publication DROP COLUMN status;
-- rollback ALTER TABLE event_publication DROP COLUMN last_resubmission_date;
-- rollback ALTER TABLE event_publication DROP COLUMN completion_attempts;
