-- Insert into practitioner
INSERT INTO practitioner(external_id, username) VALUES ('test', 'test user');

-- Insert into plan
INSERT INTO plan(uuid, published_state, created_date, created_by_id, last_updated_date, last_updated_by_id)
SELECT '556db5c8-a1eb-4064-986b-0740d6a83c33', 'UNPUBLISHED', now() - interval '1 day', practitioner.id, now() - interval '1 day', practitioner.id
FROM practitioner
WHERE external_id = 'test';

-- Insert into plan_version
INSERT INTO plan_version(uuid, plan_id, plan_type, version, countersigning_status, agreement_status, created_date, created_by_id, last_updated_date, last_updated_by_id, read_only)
SELECT '353fae07-4a6d-4614-afd6-bb3953f8fadb', plan.id, 'INITIAL', 0, 'UNSIGNED', 'DRAFT', now() - interval '1 day', practitioner.id, now() - interval '1 day', practitioner.id, false
from plan, practitioner
where plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33' and practitioner.external_id = 'test';

INSERT INTO plan_version(uuid, plan_id, plan_type, version, countersigning_status, agreement_status, created_date, created_by_id, last_updated_date, last_updated_by_id, read_only, soft_deleted)
SELECT '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64', plan.id, 'INITIAL', 1, 'UNSIGNED', 'DRAFT', now() - interval '1 day', practitioner.id, now() - interval '1 day', practitioner.id, false, true
from plan, practitioner
where plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33' and practitioner.external_id = 'test';

INSERT INTO plan_version(uuid, plan_id, plan_type, version, countersigning_status, agreement_status, created_date, created_by_id, last_updated_date, last_updated_by_id, read_only)
SELECT '59f8d10b-dda6-48d0-8f65-bb5eb1216b3b', plan.id, 'INITIAL', 2, 'SELF_SIGNED', 'DRAFT', now() - interval '1 day', practitioner.id, now() - interval '1 day', practitioner.id, false
from plan, practitioner
where plan.uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33' and practitioner.external_id = 'test';

-- Update current_plan_version_id in plan
UPDATE plan
SET current_plan_version_id = (
    SELECT id FROM plan_version
    WHERE uuid = '59f8d10b-dda6-48d0-8f65-bb5eb1216b3b'
)
WHERE uuid = '556db5c8-a1eb-4064-986b-0740d6a83c33';
