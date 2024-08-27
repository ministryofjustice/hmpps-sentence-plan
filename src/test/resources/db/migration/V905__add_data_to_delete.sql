INSERT INTO goal (uuid, title, area_of_need_id, target_date, creation_date, goal_order, plan_id)
SELECT 'ede47f7f-8431-4ff9-80ec-2dd3a8db3841', 'Goal Title', aon.id, '2024-06-27 16:10:57.299363',
       '2024-06-27 16:10:57.299363', 1, plan.id
FROM
    area_of_need aon, plan where aon.name='Accommodation' and plan.uuid='4fe411e3-820d-4198-8400-ab4268208641';

INSERT INTO step (uuid, goal_id, description, status, creation_date)
SELECT '79803555-fad5-4cb7-8f8e-10f6d436834c', goal.id, 'Test step 1', 'Status name', '2024-06-27 16:26:38.000000'
FROM goal where goal.uuid = 'ede47f7f-8431-4ff9-80ec-2dd3a8db3841';

INSERT INTO step_actors (step_id, actor)
SELECT step.id, 'Delete name' FROM step;
