DELETE FROM oasys_pk_to_plan o
WHERE o.plan_id = (
    select id from plan where plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33'
    );

DELETE FROM plan
WHERE plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33';

DELETE from practitioner
WHERE external_id = 'test';