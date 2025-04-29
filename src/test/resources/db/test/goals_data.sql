INSERT INTO goal (uuid, title, area_of_need_id, target_date, reminder_date, created_date, created_by_id, last_updated_date, last_updated_by_id, goal_status, status_date,
                  plan_version_id, goal_order)
SELECT '31d7e986-4078-4f5c-af1d-115f9ba3722d',
       'Goal For Now Title',
       aon.id,
       '2024-06-27',
       null,
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

INSERT INTO goal (uuid, title, area_of_need_id, target_date, reminder_date, created_date, created_by_id, last_updated_date, last_updated_by_id, goal_status, status_date,
                  plan_version_id, goal_order)
SELECT '778b8e52-5927-42d4-9c05-7029ef3c6f6d',
       'Goal For Future Title',
       aon.id,
       null,
       '2024-06-27',
       now(),
       practitioner.id,
       now(),
       practitioner.id,
       'FUTURE',
       now(),
       plan_version.id,
       2
FROM area_of_need aon,
     plan_version,
     practitioner
where aon.name = 'Accommodation'
  and plan_version.uuid = '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64'
  and practitioner.external_id = 'test';
