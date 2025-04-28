ALTER TABLE goal
    ADD COLUMN IF NOT EXISTS reminder_date timestamp NULL;