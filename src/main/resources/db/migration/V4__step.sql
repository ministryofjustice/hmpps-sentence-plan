do $$ begin
    create type step_status_type as enum ('NOT_STARTED', 'IN_PROGRESS', 'CANNOT_BE_DONE_YET', 'COMPLETED');
exception
    when duplicate_object then null;
end $$;

create table if not exists step
(
    id                    serial           PRIMARY KEY,
    uuid                  uuid             NOT NULL UNIQUE,
    goal_id               integer          NOT NULL,
    description           varchar(256)     NOT NULL,
    status                step_status_type NOT NULL,
    creation_date         timestamp        NOT NULL,
    FOREIGN KEY (goal_id) REFERENCES goal (id) ON DELETE CASCADE
);