insert into plan(uuid, countersigning_status, agreement_status, agreement_date, creation_date, updated_date)
values ('5012fc38-2f13-4111-8c7e-abc3ee7e4822',
        'INCOMPLETE',
        'DRAFT',
        null,
        '2024-06-25 10:00:00',
        '2024-06-25 10:00:00');

INSERT INTO goal (uuid, title, area_of_need_id, target_date, creation_date, goal_status, status_date, goal_order, plan_id)
SELECT 'b9c66782-1dd0-4be5-910a-001e01313420',
       'Goal with no steps',
       aon.id,
       '2024-06-27 16:10:57.299363',
       '2024-06-27 16:10:57.299363',
       'ACTIVE',
       null,
       1,
       plan.id
FROM area_of_need aon,
     plan
where aon.name = 'Accommodation'
  and plan.uuid = '5012fc38-2f13-4111-8c7e-abc3ee7e4822';

INSERT INTO goal (uuid, title, area_of_need_id, target_date, creation_date, goal_status, status_date, goal_order, plan_id)
SELECT '8b889730-ade8-4c3c-8e06-91a78b3ff3b2',
       'Goal with one step',
       aon.id,
       '2024-06-27 16:10:57.299363',
       '2024-06-27 16:10:57.299363',
       'ACTIVE',
       null,
       1,
       plan.id
FROM area_of_need aon,
     plan
where aon.name = 'Accommodation'
  and plan.uuid = '5012fc38-2f13-4111-8c7e-abc3ee7e4822';

INSERT INTO step (uuid, goal_id, description, status, creation_date, actor)
SELECT 'fcf019dc-e9aa-44dd-ad9b-1f2f8ba06c99',
       goal.id,
       'Step for update steps tests',
       'NOT_STARTED',
       '2024-06-27 16:26:38.000000',
       'actor'
FROM goal
where goal.uuid = '8b889730-ade8-4c3c-8e06-91a78b3ff3b2';