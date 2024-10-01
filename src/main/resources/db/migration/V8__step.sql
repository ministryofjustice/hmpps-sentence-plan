do
$$
    begin
        create type step_status_type as enum ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'CANNOT_BE_DONE_YET', 'NO_LONGER_NEEDED');
    exception
        when duplicate_object then null;
    end
$$;

create table if not exists step
(
    id            serial PRIMARY KEY,
    uuid          uuid             NOT NULL UNIQUE,
    goal_id       integer          NOT NULL,
    description   varchar(256)     NOT NULL,
    status        step_status_type NOT NULL,
    created_date  timestamp        NOT NULL,
    created_by_id integer          NOT NULL,
    actor         varchar          NOT NULL,
    FOREIGN KEY (created_by_id) references practitioner (id),
    FOREIGN KEY (goal_id) REFERENCES goal (id) ON DELETE CASCADE
);