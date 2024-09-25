do
$$
    begin
        create type countersigning_status_type as enum (
            'AWAITING-COUNTERSIGN',
            'AWAITING-DOUBLE-COUNTERSIGN',
            'COUNTERSIGNED',
            'DOUBLE-COUNTERSIGNED',
            'LOCKED-INCOMPLETE',
            'REJECTED',
            'ROLLED-BACK',
            'SELF-SIGNED',
            'UNSIGNED'
            );
        create type agreement_status_type as enum ('DRAFT', 'AGREED', 'DO_NOT_AGREE', 'COULD_NOT_ANSWER');
    exception
        when duplicate_object then null;
    end
$$;

create table if not exists plan_version
(
    id                    serial PRIMARY KEY,
    uuid                  uuid                       NOT NULL UNIQUE,
    version               integer                    NOT NULL,
    countersigning_status countersigning_status_type NOT NULL,
    agreement_status      agreement_status_type      NOT NULL,
    created_date          timestamp                  NOT NULL,
    created_by_id         integer                    NOT NULL,
    last_updated_date     timestamp                  NOT NULL,
    last_updated_by_id    integer                    NULL,
    agreement_date        timestamp                  NULL,
    read_only             boolean                    NOT NULL,
    checksum              varchar                    NULL,
    FOREIGN KEY (created_by_id) references practitioner (id),
    FOREIGN KEY (last_updated_by_id) references practitioner (id)
);