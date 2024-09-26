create table if not exists plan
(
    id                      serial PRIMARY KEY,
    published_state         publish_type NOT NULL,
    uuid                    uuid         NOT NULL UNIQUE,
    created_date            timestamp    NOT NULL,
    created_by_id           integer      NOT NULL,
    last_updated_date       timestamp    NOT NULL,
    last_updated_by_id      integer      NOT NULL,
    current_plan_version_id integer      NULL,
    FOREIGN KEY (current_plan_version_id) REFERENCES plan_version (id),
    FOREIGN KEY (created_by_id) references practitioner (id),
    FOREIGN KEY (last_updated_by_id) references practitioner (id)
);

ALTER TABLE plan_version ADD FOREIGN KEY (plan_id) REFERENCES plan(id);

create table if not exists oasys_pk_to_plan
(
    id                  serial PRIMARY KEY,
    oasys_assessment_pk varchar(15) NOT NULL UNIQUE,
    plan_id             integer     NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES plan (id)
);
