DROP TABLE IF EXISTS step_actors;

ALTER TABLE step
ADD COLUMN actor varchar(255);