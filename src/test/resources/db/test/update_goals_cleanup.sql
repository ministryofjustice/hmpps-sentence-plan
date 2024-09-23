DELETE FROM related_area_of_need WHERE goal_id in (select id from goal where plan_id in (select id from plan where plan.uuid='d1e159a3-e5dc-4464-8b52-59f578100833'));

DELETE FROM goal where plan_id = (select id from plan where plan.uuid = 'd1e159a3-e5dc-4464-8b52-59f578100833');

DELETE FROM plan WHERE uuid = 'd1e159a3-e5dc-4464-8b52-59f578100833'