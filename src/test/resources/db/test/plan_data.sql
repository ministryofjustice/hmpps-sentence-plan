-- Insert into practitioner
INSERT INTO practitioner(external_id, username) VALUES ('test', 'test user');

-- Insert into plan
INSERT INTO plan(uuid, published_state, created_date, created_by_id, last_updated_date, last_updated_by_id, person_crn)
SELECT '556db5c8-a1eb-4064-986b-0740d6a83c33', 'UNPUBLISHED', now() - interval '1 day', practitioner.id, now() - interval '1 day', practitioner.id, 'X123456'
FROM practitioner
WHERE external_id = 'test';

-- Insert second row into plan - without person_crn
INSERT INTO plan(uuid, published_state, created_date, created_by_id, last_updated_date, last_updated_by_id)
SELECT '556db5c8-a1eb-4064-986b-0740d6a83c44', 'UNPUBLISHED', now() - interval '1 day', practitioner.id, now() - interval '1 day', practitioner.id
FROM practitioner
WHERE external_id = 'test';

-- Insert into plan_version
INSERT INTO plan_version(uuid, plan_id, plan_type, version, countersigning_status, agreement_status, created_date, created_by_id, last_updated_date, last_updated_by_id, read_only)
SELECT '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64', plan.id, 'INITIAL', 0, 'UNSIGNED', 'DRAFT', now() - interval '1 day', practitioner.id, now() - interval '1 day', practitioner.id, false
from plan, practitioner
where plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33' and practitioner.external_id = 'test';

-- Update current_plan_version_id in plan
UPDATE plan
SET current_plan_version_id = (
    SELECT id FROM plan_version
    WHERE uuid = '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64'
)
WHERE uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33';