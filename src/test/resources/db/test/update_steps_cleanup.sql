DELETE FROM step where goal_id = (SELECT id FROM goal where goal.uuid = 'b9c66782-1dd0-4be5-910a-001e01313420');

DELETE FROM goal where plan_id = (select id from plan where plan.uuid = '5012fc38-2f13-4111-8c7e-abc3ee7e4822');

DELETE FROM plan WHERE uuid = '5012fc38-2f13-4111-8c7e-abc3ee7e4822'