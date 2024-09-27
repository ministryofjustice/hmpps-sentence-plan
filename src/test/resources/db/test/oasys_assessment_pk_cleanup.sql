DELETE FROM oasys_pk_to_plan o
WHERE o.plan_id = (
    select id from plan where plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33'
    );

UPDATE plan
SET current_plan_version_id = NULL
WHERE uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33';

DELETE from plan_version
where uuid = '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64';

DELETE FROM plan
WHERE plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33';

DELETE from practitioner
WHERE external_id = 'test';