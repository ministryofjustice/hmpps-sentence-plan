create table if not exists goal
(
    id                  serial          PRIMARY KEY,
    uuid                uuid            NOT NULL UNIQUE,
    title               varchar(128)    NOT NULL,
    area_of_need_id     integer         NOT NULL,
    target_date         timestamp       NULL,
    creation_date       timestamp       NOT NULL,
    plan_id             integer         NOT NULL,
    goal_order          integer         NULL,
    FOREIGN KEY (plan_id) references  plan(id),
    FOREIGN KEY (area_of_need_id) references  area_of_need(id)
);
