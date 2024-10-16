-- Update agreement_status and date in plan_version
UPDATE plan_version SET agreement_status = 'AGREED', agreement_date = now() - interval '1 day' WHERE uuid = '9f2aaa46-e544-4bcd-8db6-fbe7842ddb64'
