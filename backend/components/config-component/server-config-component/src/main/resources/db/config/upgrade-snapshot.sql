-- Existing installations: run once, then publish the complete active configuration set.
ALTER TABLE sys_config_release ADD COLUMN IF NOT EXISTS snapshot TEXT;
