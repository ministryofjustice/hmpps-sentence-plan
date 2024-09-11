DROP TABLE IF EXISTS step_actors;

ALTER TABLE plan
ADD COLUMN updated_by_name varchar(255);

ALTER TABLE plan
ADD COLUMN updated_by_id varchar(255);