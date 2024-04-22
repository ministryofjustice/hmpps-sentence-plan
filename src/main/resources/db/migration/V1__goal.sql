create table if not exists goal
(
    id                  uuid            PRIMARY KEY,
    title               varchar(128)    not null,
    area_of_need        varchar(128)    not null,
    target_date         timestamp       not null,
    is_agreed           boolean         not null,
    agreement_note      varchar(256)    not null
);

create table if not exists step
(
    id                  uuid            PRIMARY KEY,
    related_goal_id     int             not null,
    description         varchar(256)    not null,
    actor               varchar(256)    not null,
    status              varchar(256)    not null
);