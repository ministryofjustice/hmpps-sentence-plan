create table if not exists step
(
    id                    serial          PRIMARY KEY,
    uuid                  uuid            NOT NULL UNIQUE,
    related_goal_uuid     uuid            NOT NULL,
    description           varchar(256)    NOT NULL,
    status                varchar(256)    NOT NULL,
    creation_date         timestamp       NOT NULL,
    FOREIGN KEY (related_goal_uuid) REFERENCES goal (uuid) ON DELETE CASCADE
);