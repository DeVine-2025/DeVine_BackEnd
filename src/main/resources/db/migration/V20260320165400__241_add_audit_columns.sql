-- V4: Add audit columns (updated_at, created_by, updated_by) to all BaseEntity tables
-- updated_at: already exists on report_embedding, project_embedding
-- created_by, updated_by: new on all tables

-- =============================================
-- Add updated_at where missing
-- =============================================

ALTER TABLE member ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE category ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE techstack ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE terms ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE image ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE contact ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE git_repo_url ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE member_agreement ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE member_category ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE bookmark ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE project ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE project_image ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE project_requirement_member ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE matching ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE project_techstack ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE project_requirement_techstack ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE dev_report ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone;

-- =============================================
-- Add created_by to all BaseEntity tables
-- =============================================

ALTER TABLE member ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE category ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE techstack ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE terms ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE image ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE contact ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE git_repo_url ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE member_agreement ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE member_category ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE bookmark ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE project ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE project_image ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE project_requirement_member ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE matching ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE project_techstack ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE project_requirement_techstack ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE dev_report ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE report_embedding ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE project_embedding ADD COLUMN IF NOT EXISTS created_by character varying(255);
ALTER TABLE notification ADD COLUMN IF NOT EXISTS created_by character varying(255);

-- =============================================
-- Add updated_by to all BaseEntity tables
-- =============================================

ALTER TABLE member ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE category ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE techstack ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE terms ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE image ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE contact ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE git_repo_url ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE member_agreement ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE member_category ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE bookmark ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE project ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE project_image ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE project_requirement_member ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE matching ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE project_techstack ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE project_requirement_techstack ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE dev_report ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE report_embedding ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE project_embedding ADD COLUMN IF NOT EXISTS updated_by character varying(255);
ALTER TABLE notification ADD COLUMN IF NOT EXISTS updated_by character varying(255);
