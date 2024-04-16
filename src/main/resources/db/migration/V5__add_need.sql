create table if not exists need
(
    id           uuid primary key,
    code         varchar not null,
    objective_id uuid    not null
);

alter table need
    add constraint fk_need_objective foreign key (objective_id) references objective (id)