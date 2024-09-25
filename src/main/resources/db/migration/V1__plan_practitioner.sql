create table if not exists practitioner
(
    id           serial           PRIMARY KEY,
    external_id  varchar(255)     NOT NULL UNIQUE,
    username     varchar(255)     NOT NULL
);
