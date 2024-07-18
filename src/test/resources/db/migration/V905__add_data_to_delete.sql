INSERT INTO goal (uuid, title, area_of_need_uuid, target_date, creation_date, goal_order, plan_uuid)
SELECT 'ede47f7f-8431-4ff9-80ec-2dd3a8db3841', 'Goal Title', uuid, '2024-06-27 16:10:57.299363',
        '2024-06-27 16:10:57.299363', 1, '556db5c8-a1eb-4064-986b-0740d6a83c33' FROM area_of_need where name='Accommodation';

INSERT INTO step (uuid, related_goal_uuid, description, status, creation_date) VALUES ('79803555-fad5-4cb7-8f8e-10f6d436834c', 'ede47f7f-8431-4ff9-80ec-2dd3a8db3841', 'Test step 1', 'Status name', '2024-06-27 16:26:38.000000');

INSERT INTO step_actors (step_uuid, actor, actor_option_id) VALUES ('79803555-fad5-4cb7-8f8e-10f6d436834c', 'Actor name', 1);
