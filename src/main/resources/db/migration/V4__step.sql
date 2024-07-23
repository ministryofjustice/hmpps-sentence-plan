create table if not exists step
(
    id                    serial          PRIMARY KEY,
    uuid                  uuid            NOT NULL UNIQUE,
    goal_id               integer         NOT NULL,
    description           varchar(256)    NOT NULL,
    status                varchar(256)    NOT NULL,
    creation_date         timestamp       NOT NULL,
    FOREIGN KEY (goal_id) REFERENCES goal (id) ON DELETE CASCADE
);