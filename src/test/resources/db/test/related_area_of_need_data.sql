INSERT INTO related_area_of_need (goal_id, area_of_need_id)
SELECT goal.id, area_of_need.id
FROM goal, area_of_need
WHERE goal.uuid = '31d7e986-4078-4f5c-af1d-115f9ba3722d'
AND area_of_need.name = 'Health and wellbeing';