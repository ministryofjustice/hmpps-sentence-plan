alter table objective add column status varchar;
update objective set status = 'not-started' where status is null;

