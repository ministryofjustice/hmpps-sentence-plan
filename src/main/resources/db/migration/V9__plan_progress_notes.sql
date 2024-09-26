do
$$
    begin
        create type note_is_support_needed as enum ('YES', 'NO', 'DONT_KNOW');
    exception
        when duplicate_object then null;
    end
$$;

create table if not exists plan_progress_notes
(
    id                     serial PRIMARY KEY,
    plan_version_id        integer                NOT NULL,
    note                   varchar(512)           NOT NULL,
    is_support_needed      note_is_support_needed NOT NULL,
    is_support_needed_note varchar(512),
    is_involved            boolean                NOT NULL,
    is_involved_note       varchar(512),
    person_name            varchar(128)           NOT NULL,
    practitioner_name      varchar(128)           NOT NULL,
    creation_date          timestamp              NOT NULL,
    FOREIGN KEY (plan_version_id) REFERENCES plan_version (id) ON DELETE CASCADE
);

