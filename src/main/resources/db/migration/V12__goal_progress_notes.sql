do
$$
    begin
        create type goal_note_type as enum ('PROGRESS', 'REMOVED', 'ACHIEVED');
    exception
        when duplicate_object then null;
    end
$$;

CREATE TABLE IF NOT EXISTS goal_notes
(
    id            serial PRIMARY KEY,
    goal_id       integer        NOT NULL,
    note          TEXT,
    note_type     goal_note_type NOT NULL,
    created_date  TIMESTAMP      NOT NULL,
    created_by_id integer        NOT NULL
);

ALTER TABLE goal_notes
    ADD CONSTRAINT FK_GOAL_PROGRESS_NOTES_ON_CREATED_BY FOREIGN KEY (created_by_id) REFERENCES practitioner (id);

ALTER TABLE goal_notes
    ADD CONSTRAINT FK_GOAL_PROGRESS_NOTES_ON_GOAL FOREIGN KEY (goal_id) REFERENCES goal (id);