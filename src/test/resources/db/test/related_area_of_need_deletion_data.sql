INSERT INTO related_area_of_need (goal_id, area_of_need_id)
SELECT goal.id, aon.id
FROM
    goal, area_of_need aon where goal.uuid='31d7e986-4078-4f5c-af1d-115f9ba3722d' and aon.name='Finance';

INSERT INTO related_area_of_need (goal_id, area_of_need_id)
SELECT goal.id, aon.id
FROM
    goal, area_of_need aon where goal.uuid='31d7e986-4078-4f5c-af1d-115f9ba3722d' and aon.name='Health and wellbeing';