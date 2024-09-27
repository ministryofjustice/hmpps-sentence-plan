INSERT INTO practitioner(external_id, username) VALUES ('test', 'test user');

INSERT INTO plan(uuid, published_state, created_date, created_by_id, last_updated_date, last_updated_by_id)
SELECT '556db5c8-a1eb-4064-986b-0740d6a83c33', 'UNPUBLISHED', now() , practitioner.id, now(), practitioner.id
FROM practitioner
WHERE external_id = 'test';

INSERT INTO oasys_pk_to_plan (oasys_assessment_pk, plan_id)
SELECT '1', plan.id
from plan
where plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33';
