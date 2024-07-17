create table if not exists goal
(
    id                  serial          PRIMARY KEY,
    uuid                uuid            NOT NULL UNIQUE,
    title               varchar(128)    NOT NULL,
    area_of_need_uuid   uuid            NOT NULL,
    target_date         timestamp       NULL,
    creation_date       timestamp       NOT NULL,
    plan_uuid           uuid            NOT NULL,
    goal_order          integer         NULL,
    FOREIGN KEY (plan_uuid) references  plan(uuid),
    FOREIGN KEY (area_of_need_uuid) references  area_of_need(uuid)
);
