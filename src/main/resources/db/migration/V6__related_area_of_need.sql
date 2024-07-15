create table if not exists related_area_of_need
(
    id                    serial          PRIMARY KEY,
    goal_uuid             uuid            NOT NULL,
    area_of_need_uuid     uuid            NOT NULL,
    FOREIGN KEY (goal_uuid) references goal(uuid),
    FOREIGN KEY (area_of_need_uuid) references area_of_need(uuid)
)