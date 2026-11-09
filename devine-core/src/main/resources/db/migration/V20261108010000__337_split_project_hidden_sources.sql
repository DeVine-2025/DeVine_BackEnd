ALTER TABLE project ADD COLUMN hidden_by_owner BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE project ADD COLUMN hidden_by_admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE project ADD COLUMN report_blinded BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE project
SET hidden_by_owner = TRUE
WHERE is_hidden = TRUE AND visibility_changed_by IS NOT NULL AND visibility_changed_by = member_id;

UPDATE project
SET hidden_by_admin = TRUE
WHERE is_hidden = TRUE AND (visibility_changed_by IS NULL OR visibility_changed_by <> member_id);
