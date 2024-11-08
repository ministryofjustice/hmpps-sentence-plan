DELETE FROM oasys_pk_to_plan;

UPDATE plan SET current_plan_version_id = NULL;

DELETE from plan_version;

DELETE FROM plan;

DELETE from practitioner;