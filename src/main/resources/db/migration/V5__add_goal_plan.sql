alter table goal add column plan_uuid uuid not null;
alter table goal add foreign key (plan_uuid) references plan(uuid);
