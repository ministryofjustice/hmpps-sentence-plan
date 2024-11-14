INSERT INTO goal_notes (goal_id, note, note_type, created_date, created_by_id)
SELECT goal.id,
       'First goal note',
       'PROGRESS',
       now() + interval '1 hour',
       practitioner.id
FROM goal,
     practitioner
where goal.uuid = '31d7e986-4078-4f5c-af1d-115f9ba3722d'
  and practitioner.external_id = 'test';

INSERT INTO goal_notes (goal_id, note, note_type, created_date, created_by_id)
SELECT goal.id,
       'Second goal note',
       'ACHIEVED',
       now() + interval '2 hours',
       practitioner.id
FROM goal,
     practitioner
where goal.uuid = '31d7e986-4078-4f5c-af1d-115f9ba3722d'
  and practitioner.external_id = 'test';
