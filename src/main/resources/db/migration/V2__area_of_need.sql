create table if not exists area_of_need
(
    id            serial       PRIMARY KEY,
    uuid          uuid         NOT NULL UNIQUE,
    name          varchar(128) UNIQUE
);

insert into area_of_need(uuid, name) values(gen_random_uuid(), 'Accommodation');
insert into area_of_need(uuid, name) values(gen_random_uuid(), 'Employment and education');
insert into area_of_need(uuid, name) values(gen_random_uuid(), 'Finance');
insert into area_of_need(uuid, name) values(gen_random_uuid(), 'Drug use');
insert into area_of_need(uuid, name) values(gen_random_uuid(), 'Alcohol use');
insert into area_of_need(uuid, name) values(gen_random_uuid(), 'Health and wellbeing');
insert into area_of_need(uuid, name) values(gen_random_uuid(), 'Personal relationships and community');
insert into area_of_need(uuid, name) values(gen_random_uuid(), 'Thinking, behaviours and attitudes');