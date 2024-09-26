create table if not exists plan_agreement_notes
(
    id                    serial PRIMARY KEY,
    plan_version_id       integer               NOT NULL,
    optional_note         varchar(512),
    agreement_status      agreement_status_type NOT NULL,
    agreement_status_note varchar(512),
    practitioner_name     varchar(128)          NOT NULL,
    person_name           varchar(128)          NOT NULL,
    creation_date         timestamp             NOT NULL,
    FOREIGN KEY (plan_version_id) REFERENCES plan_version (id) ON DELETE CASCADE
);