create table if not exists objective
(
    id                  uuid primary key,
    sentence_plan_id    uuid,
    description         varchar
);

alter table objective
    add constraint fk_objective_sentence_plan foreign key (sentence_plan_id) references sentence_plan(id)