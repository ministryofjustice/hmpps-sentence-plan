CREATE TABLE goals (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR NOT NULL,
                       area_id INT NOT NULL,
                       active boolean NOT NULL,
                       CONSTRAINT fk_area
                           FOREIGN KEY(area_id)
                               REFERENCES areas(id)
);

insert into goals(name, area_id, active) values ('Improve relationship with neighbours', 1, '1');
insert into goals(name, area_id, active) values ('Find suitable accommodation', 1, '1');
insert into goals(name, area_id, active) values ('Reduce risk of eviction', 1, '1');
insert into goals(name, area_id, active) values ('Follow rules of their accommodation provider and stay in current accommodation for the length of their sentence', 1, '1');
insert into goals(name, area_id, active) values ('Have housing needs assessed by the housing advice service', 1, '1');
insert into goals(name, area_id, active) values ('Reduce or pay off any unpaid rent payments, so they can be considered for other accommodation', 1, '1');
insert into goals(name, area_id, active) values ('Actively try to find suitable housing by submitting applications and attending assessments', 1, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 1', 2, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 2', 2, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 3', 2, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 4', 2, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 5', 2, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 6', 2, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 7', 2, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 8', 2, '1');
insert into goals(name, area_id, active) values ('Drugs Goal 9', 2, '1');
insert into goals(name, area_id, active) values ('Health and Wellbeing Goal 1', 3, '1');
insert into goals(name, area_id, active) values ('Health and Wellbeing Goal 2', 3, '1');
insert into goals(name, area_id, active) values ('Health and Wellbeing Goal 3', 3, '1');
insert into goals(name, area_id, active) values ('Health and Wellbeing Goal 4', 3, '1');
insert into goals(name, area_id, active) values ('Health and Wellbeing Goal 5', 3, '1');
insert into goals(name, area_id, active) values ('Health and Wellbeing Goal 6', 3, '1');
insert into goals(name, area_id, active) values ('Health and Wellbeing Goal 7', 3, '1');