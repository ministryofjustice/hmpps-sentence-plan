do
$$
    begin
        create type countersigning_status_type as enum (
            'AWAITING_COUNTERSIGN',
            'AWAITING_DOUBLE_COUNTERSIGN',
            'COUNTERSIGNED',
            'DOUBLE_COUNTERSIGNED',
            'LOCKED_INCOMPLETE',
            'REJECTED',
            'ROLLED_BACK',
            'SELF_SIGNED',
            'UNSIGNED'
            );
        create type agreement_status_type as enum ('DRAFT', 'AGREED', 'DO_NOT_AGREE', 'COULD_NOT_ANSWER');
        create type plan_type_type as enum('INITIAL', 'REVIEW', 'TERMINATE', 'TRANSFER', 'OTHER');
    exception
        when duplicate_object then null;
    end
$$;

create table if not exists plan_version
(
    id                    serial PRIMARY KEY,
    uuid                  uuid                       NOT NULL UNIQUE,
    version               integer                    NOT NULL,
    plan_id               integer                    NOT NULL,
    plan_type             plan_type_type             NOT NULL,
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
