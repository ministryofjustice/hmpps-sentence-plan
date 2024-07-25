INSERT INTO goal (uuid, title, area_of_need_id, target_date, creation_date, goal_order, plan_id)
SELECT '31d7e986-4078-4f5c-af1d-115f9ba3722d', 'Goal Title', aon.id, '2024-06-27 16:10:57.299363',
       '2024-06-27 16:10:57.299363', 1, plan.id
FROM
    area_of_need aon, plan where aon.name='Accommodation';
