ALTER TABLE project DROP CONSTRAINT project_project_status_check;

ALTER TABLE project ADD CONSTRAINT project_project_status_check
    CHECK (((project_status)::text = ANY (ARRAY[('RECRUITING'::character varying)::text, ('IN_PROGRESS'::character varying)::text, ('COMPLETED'::character varying)::text, ('DELETED'::character varying)::text, ('HIDDEN'::character varying)::text])));
