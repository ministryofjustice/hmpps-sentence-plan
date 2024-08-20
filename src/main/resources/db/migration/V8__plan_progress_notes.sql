create table if not exists plan_progress_notes
(
    id                      serial        PRIMARY KEY,
    plan_uuid               uuid          NOT NULL,
    title                   varchar(128)  NOT NULL,
    text                    varchar(512)  NOT NULL,
    practitioner_name       varchar(128)  NOT NULL,
    person_name             varchar(128)  NOT NULL,
    creation_date           timestamp     NOT NULL,
    FOREIGN KEY (plan_uuid) REFERENCES plan (uuid) ON DELETE CASCADE
);