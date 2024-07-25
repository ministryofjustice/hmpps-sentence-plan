INSERT INTO step (uuid, goal_id, description, status, creation_date)
SELECT '71793b64-545e-4ae7-9936-610639093857', goal.id, 'Test step 1', 'Status name', '2024-06-27 16:26:38.000000'
FROM goal where goal.uuid = '31d7e986-4078-4f5c-af1d-115f9ba3722d';
