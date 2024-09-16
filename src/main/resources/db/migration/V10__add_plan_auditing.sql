create table if not exists practitioner
(
    id           serial           PRIMARY KEY,
    external_id  varchar(255)     NOT NULL UNIQUE,
    username     varchar(255)     NOT NULL
);

ALTER TABLE plan
ADD COLUMN updated_by_id integer;

ALTER TABLE plan
ADD FOREIGN KEY (updated_by_id) references practitioner(id);