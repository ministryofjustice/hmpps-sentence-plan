INSERT INTO plan_agreement_notes (plan_version_id, optional_note, agreement_status, agreement_status_note, practitioner_name, person_name, created_date, created_by_id)
SELECT plan_version.id, 'Optional note', 'AGREED', 'Agreement status note', 'Practitioner name', 'Person name', now(), practitioner.id
FROM plan_version, practitioner
where plan_version.uuid = '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64' and practitioner.external_id = 'test';

insert into plan_progress_notes (plan_version_id, note, is_support_needed, is_support_needed_note, is_involved,
                                 is_involved_note, person_name, practitioner_name, created_date, created_by_id)
SELECT plan_version.id, 'Note', 'YES', 'Support needed note', TRUE, 'Is involved note', 'Person name', 'Practitioner name', now(), practitioner.id
FROM plan_version, practitioner
where plan_version.uuid = '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64' and practitioner.external_id = 'test';
