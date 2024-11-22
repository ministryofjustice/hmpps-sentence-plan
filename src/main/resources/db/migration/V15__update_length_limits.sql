ALTER TABLE step ALTER COLUMN description TYPE varchar;

ALTER TABLE plan_progress_notes ALTER COLUMN note TYPE varchar;
ALTER TABLE plan_progress_notes ALTER COLUMN is_support_needed_note TYPE varchar;
ALTER TABLE plan_progress_notes ALTER COLUMN is_involved_note TYPE varchar;

ALTER TABLE plan_agreement_notes ALTER COLUMN optional_note TYPE varchar;
ALTER TABLE plan_agreement_notes ALTER COLUMN agreement_status_note TYPE varchar;
