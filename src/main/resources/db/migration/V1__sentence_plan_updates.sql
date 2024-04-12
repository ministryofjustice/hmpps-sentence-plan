alter table person
    drop column noms_id;

create table if not exists sentence_plan
(
    id           uuid primary key,
    person_id    uuid,
    created_date timestamp
)