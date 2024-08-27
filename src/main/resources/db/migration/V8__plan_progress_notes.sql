do $$ begin
    create type note_is_support_needed as enum ('YES', 'NO', 'DONT_KNOW');
exception
    when duplicate_object then null;
end $$;

create table if not exists plan_agreement_notes
(
    id                      serial                      PRIMARY KEY,
    plan_uuid               uuid                        NOT NULL,
    optional_note           varchar(512),
    agreement_status        agreement_status_type       NOT NULL,
    agreement_status_note   varchar(512),
    practitioner_name       varchar(128)                NOT NULL,
    person_name             varchar(128)                NOT NULL,
    creation_date           timestamp                   NOT NULL,
    FOREIGN KEY (plan_uuid) REFERENCES plan (uuid) ON DELETE CASCADE
);

create table if not exists plan_progress_notes
(
    id                              serial                  PRIMARY KEY,
    plan_uuid                       uuid                    NOT NULL,
    note                            varchar(512)            NOT NULL,
    is_support_needed               note_is_support_needed  NOT NULL,
    is_support_needed_note          varchar(512),
    is_involved                     boolean                 NOT NULL,
    is_involved_note                varchar(512),
    person_name                     varchar(128)            NOT NULL,
    practitioner_name               varchar(128)            NOT NULL,
    creation_date                   timestamp               NOT NULL,
    FOREIGN KEY (plan_uuid)         REFERENCES plan (uuid) ON DELETE CASCADE
);