INSERT INTO goal (uuid, title, area_of_need_uuid, target_date, creation_date, goal_order, plan_uuid) SELECT '31d7e986-4078-4f5c-af1d-115f9ba3722d', 'Goal Title', uuid, '2024-06-27 16:10:57.299363',
        '2024-06-27 16:10:57.299363', 1, '556db5c8-a1eb-4064-986b-0740d6a83c33' FROM area_of_need where name='Accommodation';
