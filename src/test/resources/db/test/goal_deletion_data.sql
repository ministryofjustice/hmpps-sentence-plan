INSERT INTO goal (uuid, title, area_of_need_id, target_date, created_date, created_by_id, last_updated_date,
                  last_updated_by_id, goal_status, status_date,
                  plan_version_id, goal_order)
SELECT 'ede47f7f-8431-4ff9-80ec-2dd3a8db3841',
       'GoalControllerTest deleteGoal',
       aon.id,
       '2024-06-27',
       now(),
       practitioner.id,
       now(),
       practitioner.id,
       'ACTIVE',
       now(),
       plan_version.id,
       1
FROM area_of_need aon,
     plan_version,
     practitioner
where aon.name = 'Accommodation'
  and plan_version.uuid = '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64'
  and practitioner.external_id = 'test';

INSERT INTO step (uuid, goal_id, description, status, created_date, created_by_id, actor)
SELECT '3173908c-d04c-4b85-9490-ab01f20b71a0', goal.id, 'Test step 1', 'NOT_STARTED', now(), practitioner.id, 'actor'
FROM goal, practitioner
where goal.uuid = 'ede47f7f-8431-4ff9-80ec-2dd3a8db3841' and practitioner.external_id = 'test';
