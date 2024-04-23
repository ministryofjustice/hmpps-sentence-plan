create table if not exists goal
(
    id                  serial          PRIMARY KEY,
    uuid                uuid            NOT NULL UNIQUE,
    title               varchar(128)    NOT NULL,
    area_of_need        varchar(128)    NOT NULL,
    target_date         timestamp       NOT NULL,
    is_agreed           boolean         NOT NULL,
    agreement_note      varchar(256)    NOT NULL,
    creation_date       timestamp       NOT NULL
);

create table if not exists step
(
    id                  serial          PRIMARY KEY,
    uuid                uuid            NOT NULL UNIQUE,
    related_goal_id     uuid            NOT NULL,
    description         varchar(256)    NOT NULL,
    actor               varchar(256)    NOT NULL,
    status              varchar(256)    NOT NULL,
    creation_date       timestamp       NOT NULL,
    FOREIGN KEY (related_goal_id) REFERENCES goal (uuid)
);