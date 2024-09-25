create table if not exists related_area_of_need
(
    id                    serial          PRIMARY KEY,
    goal_id               integer         NOT NULL,
    area_of_need_id       integer         NOT NULL,
    FOREIGN KEY (goal_id) references goal(id),
    FOREIGN KEY (area_of_need_id) references area_of_need(id)
)