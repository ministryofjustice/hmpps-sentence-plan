-- For test data intended to remain static throughout the tests (i.e. where PlanEntity properties and child objects are used)
insert into plan(uuid, countersigning_status, agreement_status, agreement_date, creation_date, updated_date)
values ('556db5c8-a1eb-4064-986b-0740d6a83c33', 'INCOMPLETE', 'DRAFT', null, '2024-06-25 10:00:00',
        '2024-06-25 10:00:00');

-- For tests data intended to vary through the life of the tests (i.e. where PlanEntity properties and child objects are freely mutable)
insert into plan(uuid, countersigning_status, agreement_status, agreement_date, creation_date, updated_date)
values ('4fe411e3-820d-4198-8400-ab4268208641', 'INCOMPLETE', 'DRAFT', null, '2024-06-25 10:00:00',
        '2024-06-25 10:00:00');