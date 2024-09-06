DROP TABLE IF EXISTS step_actors;

ALTER TABLE plan
ADD COLUMN updated_by varchar(255);