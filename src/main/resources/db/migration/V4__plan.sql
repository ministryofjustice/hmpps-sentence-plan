do $$ begin
    create type status_type as enum ('incomplete', 'complete', 'locked', 'signed');
exception
    when duplicate_object then null;
end $$;

create table if not exists plan
(
    id            serial PRIMARY KEY,
    uuid          uuid        NOT NULL UNIQUE,
    status        status_type NOT NULL,
    creation_date timestamp   NOT NULL,
    updated_date  timestamp   NOT NULL
);

create table if not exists oasys_pk_to_plan
(
    id                  serial PRIMARY KEY,
    oasys_assessment_pk text NOT NULL,
    plan_uuid           uuid NOT NULL,
    FOREIGN KEY (plan_uuid) REFERENCES plan (uuid)
)