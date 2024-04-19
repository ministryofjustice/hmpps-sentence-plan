create table if not exists person
(
    id      uuid primary key,
    crn     varchar null,
    noms_id varchar null,
    constraint person_crn unique (crn),
    constraint person_noms_id unique (noms_id)
);