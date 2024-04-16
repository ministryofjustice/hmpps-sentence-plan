alter table sentence_plan
    add constraint fk_sentence_plan_person foreign key (person_id) references person(id)