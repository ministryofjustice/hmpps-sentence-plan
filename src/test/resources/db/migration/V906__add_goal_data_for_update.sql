insert into plan(uuid, status, creation_date, updated_date) values ('d1e159a3-e5dc-4464-8b52-59f578100833', 'INCOMPLETE','2024-06-25 10:00:00', '2024-06-25 10:00:00');

INSERT INTO goal (uuid, title, area_of_need_id, target_date, creation_date, goal_order, plan_id)
SELECT '070442be-f855-4eb6-af7e-72f68aab54be', 'Goal For Updating', aon.id, null,
       '2024-06-27 16:10:57.299363', 3, plan.id
FROM
    area_of_need aon, plan where aon.name='Accommodation' and plan.uuid='d1e159a3-e5dc-4464-8b52-59f578100833';

INSERT INTO related_area_of_need (goal_id, area_of_need_id)
SELECT goal.id, aon.id
FROM
    goal, area_of_need aon where goal.uuid='070442be-f855-4eb6-af7e-72f68aab54be' and aon.name='Finance';

INSERT INTO related_area_of_need (goal_id, area_of_need_id)
SELECT goal.id, aon.id
FROM
    goal, area_of_need aon where goal.uuid='070442be-f855-4eb6-af7e-72f68aab54be' and aon.name='Health and wellbeing';