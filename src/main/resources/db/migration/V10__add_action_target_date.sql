alter table action add column target_date_month smallint;
alter table action add column target_date_year smallint;
update action set target_date_month = 7 where target_date_month is null;
update action set target_date_year = 2023 where target_date_year is null;
alter table action alter column target_date_month set not null;
alter table action alter column target_date_year set not null;
