INSERT INTO step (uuid, goal_id, description, status, created_date, created_by_id, actor)
SELECT '71793b64-545e-4ae7-9936-610639093857', goal.id, 'Test step 1', 'NOT_STARTED', now(), practitioner.id, 'actor'
FROM goal, practitioner
where goal.uuid = '31d7e986-4078-4f5c-af1d-115f9ba3722d' and practitioner.external_id = 'test';
