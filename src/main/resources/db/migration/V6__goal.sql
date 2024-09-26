do
$$
    begin
        create type goal_status_type as enum ('ACTIVE', 'FUTURE', 'ACHIEVED', 'REMOVED');
    exception
        when duplicate_object then null;
    end
$$;

create table if not exists goal
(
    id                 serial PRIMARY KEY,
    uuid               uuid             NOT NULL UNIQUE,
    title              varchar          NOT NULL,
    area_of_need_id    integer          NOT NULL,
    target_date        timestamp        NULL,
    created_date       timestamp        NOT NULL,
    created_by_id      integer          NOT NULL,
    last_updated_date  timestamp        NOT NULL,
    last_updated_by_id integer          NOT NULL,
    goal_status        goal_status_type NOT NULL,
    status_date        timestamp        NULL,
    plan_version_id    integer          NOT NULL,
    goal_order         integer          NOT NULL,
    FOREIGN KEY (plan_version_id) references plan_version (id),
    FOREIGN KEY (area_of_need_id) references area_of_need (id),
    FOREIGN KEY (created_by_id) references practitioner (id)
);
