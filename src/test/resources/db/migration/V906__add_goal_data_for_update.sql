INSERT INTO goal (uuid, title, area_of_need_id, target_date, creation_date, goal_order, plan_id)
SELECT '070442be-f855-4eb6-af7e-72f68aab54be', 'Goal For Updating', aon.id, null,
       '2024-06-27 16:10:57.299363', 3, plan.id
FROM
    area_of_need aon, plan where aon.name='Accommodation' and plan.uuid='556db5c8-a1eb-4064-986b-0740d6a83c33';


INSERT INTO related_area_of_need (goal_id, area_of_need_id)
SELECT goal.id, aon.id
FROM
    goal, area_of_need aon where goal.uuid='070442be-f855-4eb6-af7e-72f68aab54be' and aon.name='Finance';

INSERT INTO related_area_of_need (goal_id, area_of_need_id)
SELECT goal.id, aon.id
FROM
    goal, area_of_need aon where goal.uuid='070442be-f855-4eb6-af7e-72f68aab54be' and aon.name='Health and wellbeing';