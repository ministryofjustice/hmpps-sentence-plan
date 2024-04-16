
CREATE SCHEMA IF NOT EXISTS public

CREATE TABLE areas (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL,
    active boolean NOT NULL
);

insert into areas(name, active) values ('Accommodation', '1');
insert into areas(name, active) values ('Drugs', '1');
insert into areas(name, active) values ('Health and Wellbeing', '1');