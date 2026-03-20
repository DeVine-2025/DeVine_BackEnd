-- V1: Initial schema from production dump (2026-03-18)
-- Idempotent: safe to run on both fresh and existing databases

CREATE EXTENSION IF NOT EXISTS vector;

-- ===========================================
-- Tables
-- ===========================================

CREATE TABLE IF NOT EXISTS member (
    member_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    address character varying(255),
    body character varying(255),
    disclosure boolean NOT NULL,
    image character varying(512),
    main_type character varying(255) NOT NULL,
    name character varying(10),
    nickname character varying(20) NOT NULL,
    used character varying(20) NOT NULL,
    clerk_id character varying(255),
    github_username character varying(39),
    proposal_alarm boolean DEFAULT true NOT NULL,
    CONSTRAINT member_main_type_check CHECK (((main_type)::text = ANY ((ARRAY['DEVELOPER'::character varying, 'PM'::character varying])::text[]))),
    CONSTRAINT member_used_check CHECK (((used)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('DELETED'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS category (
    category_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    genre character varying(30) NOT NULL,
    CONSTRAINT category_genre_check CHECK (((genre)::text = ANY (ARRAY[('EDUCATION'::character varying)::text, ('ENTERTAINMENT'::character varying)::text, ('FINTECH'::character varying)::text, ('ETC'::character varying)::text, ('HEALTHCARE'::character varying)::text, ('ECOMMERCE'::character varying)::text, ('SOCIAL'::character varying)::text, ('AI_DATA'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS techstack (
    techstack_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    tech_genre character varying(255),
    techstack_name character varying(255) NOT NULL,
    parent_stack bigint,
    CONSTRAINT techstack_tech_genre_check CHECK (((tech_genre)::text = ANY ((ARRAY['LANGUAGE'::character varying, 'FRAMEWORK'::character varying, 'DATABASE'::character varying, 'MOBILE'::character varying, 'CLOUD'::character varying, 'CONTAINER'::character varying])::text[]))),
    CONSTRAINT techstack_techstack_name_check CHECK (((techstack_name)::text = ANY ((ARRAY['BACKEND'::character varying, 'FRONTEND'::character varying, 'INFRA'::character varying, 'JAVA'::character varying, 'PYTHON'::character varying, 'GO'::character varying, 'C'::character varying, 'KOTLIN'::character varying, 'PHP'::character varying, 'SPRINGBOOT'::character varying, 'NODEJS'::character varying, 'EXPRESS'::character varying, 'NESTJS'::character varying, 'DJANGO'::character varying, 'FLASK'::character varying, 'MONGODB'::character varying, 'MYSQL'::character varying, 'JAVASCRIPT'::character varying, 'TYPESCRIPT'::character varying, 'REACT'::character varying, 'VUEJS'::character varying, 'NEXTJS'::character varying, 'SVELTE'::character varying, 'REACT_NATIVE'::character varying, 'FLUTTER'::character varying, 'SWIFT'::character varying, 'AWS'::character varying, 'FIREBASE'::character varying, 'DOCKER'::character varying, 'KUBERNETES'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS terms (
    terms_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    title character varying(100) NOT NULL,
    content text NOT NULL,
    required boolean NOT NULL
);

CREATE TABLE IF NOT EXISTS image (
    image_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    image_type character varying(20) NOT NULL,
    image_url character varying(512) NOT NULL,
    s3_key character varying(512) NOT NULL,
    uploaded boolean NOT NULL,
    member_id bigint,
    clerk_id character varying(255)
);

CREATE TABLE IF NOT EXISTS contact (
    contact_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    type character varying(20) NOT NULL,
    value character varying(255) NOT NULL,
    link character varying(255),
    member_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS git_repo_url (
    git_repo_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    git_url character varying(500) NOT NULL,
    member_id bigint NOT NULL,
    git_description character varying(500)
);

CREATE TABLE IF NOT EXISTS member_agreement (
    member_agreement_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    member_id bigint NOT NULL,
    terms_id bigint NOT NULL,
    agreed boolean NOT NULL
);

CREATE TABLE IF NOT EXISTS member_category (
    member_category_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    category_id bigint NOT NULL,
    member_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS dev_techstack (
    member_id bigint NOT NULL,
    techstack_id bigint NOT NULL,
    source character varying(20) NOT NULL,
    id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS bookmark (
    bookmark_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    target_id bigint NOT NULL,
    target_type character varying(20) NOT NULL,
    member_id bigint NOT NULL,
    CONSTRAINT bookmark_target_type_check CHECK (((target_type)::text = ANY (ARRAY[('PROJECT'::character varying)::text, ('DEVELOPER'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS project (
    project_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    project_content text NOT NULL,
    project_name character varying(255) NOT NULL,
    project_status character varying(20) NOT NULL,
    domain_id bigint NOT NULL,
    member_id bigint NOT NULL,
    last_view_reset_date date,
    location character varying(100) NOT NULL,
    project_mode character varying(20) NOT NULL,
    project_field character varying(20) NOT NULL,
    recruitment_deadline date NOT NULL,
    total_view_count bigint NOT NULL,
    weekly_view_count bigint NOT NULL,
    previous_week_view_count bigint DEFAULT 0 NOT NULL,
    duration_range character varying(20) NOT NULL,
    CONSTRAINT project_project_field_check CHECK (((project_field)::text = ANY ((ARRAY['WEB'::character varying, 'MOBILE'::character varying, 'AI'::character varying, 'GAME'::character varying, 'DATA'::character varying, 'BACKEND'::character varying, 'FRONTEND'::character varying])::text[]))),
    CONSTRAINT project_project_mode_check CHECK (((project_mode)::text = ANY ((ARRAY['ONLINE'::character varying, 'OFFLINE'::character varying, 'HYBRID'::character varying])::text[]))),
    CONSTRAINT project_project_status_check CHECK (((project_status)::text = ANY (ARRAY[('RECRUITING'::character varying)::text, ('IN_PROGRESS'::character varying)::text, ('COMPLETED'::character varying)::text, ('DELETED'::character varying)::text])))
);

CREATE TABLE IF NOT EXISTS project_image (
    project_image_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    project_id bigint NOT NULL,
    image_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS project_requirement_member (
    project_requirement_member_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    project_id bigint NOT NULL,
    req_mem_part character varying(30) NOT NULL,
    req_mem_num integer NOT NULL,
    current_count integer
);

CREATE TABLE IF NOT EXISTS matching (
    matching_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    status character varying(20) NOT NULL,
    member_id bigint NOT NULL,
    project_id bigint NOT NULL,
    matching_type character varying(20) NOT NULL,
    decision character varying(20) NOT NULL,
    content text,
    part character varying(20),
    CONSTRAINT matching_matching_type_check CHECK (((matching_type)::text = ANY (ARRAY[('APPLY'::character varying)::text, ('PROPOSE'::character varying)::text]))),
    CONSTRAINT matching_status_check CHECK (((status)::text = ANY (ARRAY['PENDING'::text, 'PROCESSING'::text, 'COMPLETED'::text, 'CANCELLED'::text])))
);

CREATE TABLE IF NOT EXISTS project_techstack (
    project_techstack_id bigint NOT NULL,
    project_id bigint NOT NULL,
    techstack_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS project_requirement_techstack (
    project_requirement_techstack_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    project_requirement_member_id bigint NOT NULL,
    techstack_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS dev_report (
    dev_report_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    git_repo_id bigint NOT NULL,
    completed_at timestamp without time zone,
    error_message character varying(1000),
    report_type character varying(20) NOT NULL,
    visibility character varying(20) NOT NULL,
    report_content jsonb
);

CREATE TABLE IF NOT EXISTS report_embedding (
    report_embedding_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    dev_report_id bigint NOT NULL,
    embedding vector(1536),
    status character varying(20) NOT NULL,
    error_message character varying(1000),
    retry_count integer NOT NULL,
    updated_at timestamp without time zone
);

CREATE TABLE IF NOT EXISTS project_embedding (
    project_embedding_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    project_id bigint NOT NULL,
    embedding vector(1536),
    status character varying(20) NOT NULL,
    error_message character varying(1000),
    retry_count integer NOT NULL,
    updated_at timestamp without time zone
);

CREATE TABLE IF NOT EXISTS notification (
    notification_id bigint NOT NULL,
    created_at timestamp without time zone NOT NULL,
    receiver_id bigint NOT NULL,
    sender_id bigint,
    type character varying(255) NOT NULL,
    title character varying(100) NOT NULL,
    content character varying(500) NOT NULL,
    reference_id bigint,
    is_read boolean NOT NULL,
    read_at timestamp without time zone
);

-- ===========================================
-- Sequences (IDENTITY) - idempotent via DO blocks
-- ===========================================

DO $$ BEGIN ALTER TABLE bookmark ALTER COLUMN bookmark_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME bookmark_bookmark_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE category ALTER COLUMN category_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME category_category_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE contact ALTER COLUMN contact_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME contact_contact_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE dev_report ALTER COLUMN dev_report_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME dev_report_dev_report_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE dev_techstack ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME dev_techstack_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE git_repo_url ALTER COLUMN git_repo_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME git_repo_url_git_repo_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE image ALTER COLUMN image_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME image_image_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE matching ALTER COLUMN matching_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME matching_matching_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE member ALTER COLUMN member_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME member_member_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE member_agreement ALTER COLUMN member_agreement_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME member_agreement_member_agreement_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE member_category ALTER COLUMN member_category_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME member_category_member_category_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE notification ALTER COLUMN notification_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME notification_notification_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE project ALTER COLUMN project_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME project_project_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE project_embedding ALTER COLUMN project_embedding_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME project_embedding_project_embedding_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE project_image ALTER COLUMN project_image_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME project_image_project_image_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE project_requirement_member ALTER COLUMN project_requirement_member_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME project_requirement_member_project_requirement_member_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE project_requirement_techstack ALTER COLUMN project_requirement_techstack_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME project_requirement_techstack_project_requirement_techstack_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE report_embedding ALTER COLUMN report_embedding_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME report_embedding_report_embedding_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE techstack ALTER COLUMN techstack_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME techstack_techstack_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE terms ALTER COLUMN terms_id ADD GENERATED BY DEFAULT AS IDENTITY (SEQUENCE NAME terms_terms_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1); EXCEPTION WHEN OTHERS THEN NULL; END $$;

CREATE SEQUENCE IF NOT EXISTS project_techstack_project_techstack_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE project_techstack_project_techstack_id_seq OWNED BY project_techstack.project_techstack_id;
ALTER TABLE ONLY project_techstack ALTER COLUMN project_techstack_id SET DEFAULT nextval('project_techstack_project_techstack_id_seq'::regclass);

-- ===========================================
-- Primary Keys (idempotent via DO block)
-- ===========================================

DO $$
BEGIN
    ALTER TABLE ONLY bookmark ADD CONSTRAINT bookmark_pkey PRIMARY KEY (bookmark_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY category ADD CONSTRAINT category_pkey PRIMARY KEY (category_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY contact ADD CONSTRAINT pk_contact PRIMARY KEY (contact_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY dev_report ADD CONSTRAINT dev_report_pkey PRIMARY KEY (dev_report_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY dev_techstack ADD CONSTRAINT pk_dev_techstack PRIMARY KEY (id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY git_repo_url ADD CONSTRAINT git_repo_url_pkey PRIMARY KEY (git_repo_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY image ADD CONSTRAINT pk_image PRIMARY KEY (image_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY matching ADD CONSTRAINT matching_pkey PRIMARY KEY (matching_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY member ADD CONSTRAINT member_pkey PRIMARY KEY (member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY member_agreement ADD CONSTRAINT pk_member_agreement PRIMARY KEY (member_agreement_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY member_category ADD CONSTRAINT member_category_pkey PRIMARY KEY (member_category_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY notification ADD CONSTRAINT pk_notification PRIMARY KEY (notification_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project ADD CONSTRAINT project_pkey PRIMARY KEY (project_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_embedding ADD CONSTRAINT pk_project_embedding PRIMARY KEY (project_embedding_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_image ADD CONSTRAINT project_image_pkey PRIMARY KEY (project_image_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_requirement_member ADD CONSTRAINT pk_project_requirement_member PRIMARY KEY (project_requirement_member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_requirement_techstack ADD CONSTRAINT project_requirement_techstack_pkey PRIMARY KEY (project_requirement_techstack_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_techstack ADD CONSTRAINT project_techstack_pkey PRIMARY KEY (project_techstack_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY report_embedding ADD CONSTRAINT pk_report_embedding PRIMARY KEY (report_embedding_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY techstack ADD CONSTRAINT techstack_pkey PRIMARY KEY (techstack_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY terms ADD CONSTRAINT pk_terms PRIMARY KEY (terms_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- ===========================================
-- Unique Constraints (idempotent via DO block)
-- ===========================================

DO $$
BEGIN
    ALTER TABLE ONLY member ADD CONSTRAINT uk_member_clerk_id UNIQUE (clerk_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY dev_techstack ADD CONSTRAINT uc_601412d361f6df34b6a58769b UNIQUE (member_id, techstack_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY git_repo_url ADD CONSTRAINT uc_307fd4088578bb2806b451d6c UNIQUE (member_id, git_url);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY member_category ADD CONSTRAINT uk_member_category UNIQUE (member_id, category_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY dev_report ADD CONSTRAINT uk_dev_report_git_repo_type UNIQUE (git_repo_id, report_type);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_embedding ADD CONSTRAINT uc_project_embedding_project UNIQUE (project_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY report_embedding ADD CONSTRAINT uc_report_embedding_dev_report UNIQUE (dev_report_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- ===========================================
-- Indexes
-- ===========================================

CREATE INDEX IF NOT EXISTS idx_notification_receiver ON notification USING btree (receiver_id, is_read, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_matching_active ON matching USING btree (project_id, member_id, matching_type) WHERE ((status)::text <> 'CANCELLED'::text);

-- ===========================================
-- Foreign Keys (idempotent via DO block)
-- ===========================================

DO $$
BEGIN
    ALTER TABLE ONLY bookmark ADD CONSTRAINT fk5bm7rup91j277mc7gg63akie2 FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY contact ADD CONSTRAINT fk_contact_on_member FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY dev_report ADD CONSTRAINT fkahjga2hg4633ofklaqfdwvjoc FOREIGN KEY (git_repo_id) REFERENCES git_repo_url(git_repo_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY dev_techstack ADD CONSTRAINT fkhhleh9a1jgwgsiw12uvm1wbcn FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY dev_techstack ADD CONSTRAINT fkibniuyymwakfiy6fg7nstvw4v FOREIGN KEY (techstack_id) REFERENCES techstack(techstack_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY git_repo_url ADD CONSTRAINT fkk6ff3h2wbtecdvvcpx3t17x25 FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY image ADD CONSTRAINT fk_image_on_member FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY matching ADD CONSTRAINT fk9nonq8mof5tr6m4gial9dkwu1 FOREIGN KEY (project_id) REFERENCES project(project_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY matching ADD CONSTRAINT fkjnbradlm7sj3phjqo9kmt8h78 FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY member_agreement ADD CONSTRAINT fk_member_agreement_on_member FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY member_agreement ADD CONSTRAINT fk_member_agreement_on_terms FOREIGN KEY (terms_id) REFERENCES terms(terms_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY member_category ADD CONSTRAINT fk87r65a4xna6uray30n79f9ar4 FOREIGN KEY (category_id) REFERENCES category(category_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY member_category ADD CONSTRAINT fk8hm1bokubb1b6412fgb7jd77f FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY notification ADD CONSTRAINT fk_notification_on_receiver FOREIGN KEY (receiver_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY notification ADD CONSTRAINT fk_notification_on_sender FOREIGN KEY (sender_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project ADD CONSTRAINT fk931m4nx5huix951xcq8o0xwsw FOREIGN KEY (domain_id) REFERENCES category(category_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project ADD CONSTRAINT fkf02mrsqr7qo2g4pi5oetixtf1 FOREIGN KEY (member_id) REFERENCES member(member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_embedding ADD CONSTRAINT fk_project_embedding_on_project FOREIGN KEY (project_id) REFERENCES project(project_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_image ADD CONSTRAINT fk_project_image_on_image FOREIGN KEY (image_id) REFERENCES image(image_id) ON DELETE CASCADE;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_image ADD CONSTRAINT fksrkbi9ax581cp14a13mbk9qtm FOREIGN KEY (project_id) REFERENCES project(project_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_requirement_member ADD CONSTRAINT fk_project_requirement_member_on_project FOREIGN KEY (project_id) REFERENCES project(project_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_requirement_techstack ADD CONSTRAINT fk_project_requirement_techstack_on_project_requirement_member FOREIGN KEY (project_requirement_member_id) REFERENCES project_requirement_member(project_requirement_member_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_requirement_techstack ADD CONSTRAINT fkmmxkjftfc8upk06o5lk7easdg FOREIGN KEY (techstack_id) REFERENCES techstack(techstack_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_techstack ADD CONSTRAINT fk_project_techstack_project FOREIGN KEY (project_id) REFERENCES project(project_id) ON DELETE CASCADE;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY project_techstack ADD CONSTRAINT fk_project_techstack_techstack FOREIGN KEY (techstack_id) REFERENCES techstack(techstack_id) ON DELETE CASCADE;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY report_embedding ADD CONSTRAINT fk_report_embedding_on_dev_report FOREIGN KEY (dev_report_id) REFERENCES dev_report(dev_report_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
DO $$
BEGIN
    ALTER TABLE ONLY techstack ADD CONSTRAINT fkn3uongav3ktc40au3p740t34m FOREIGN KEY (parent_stack) REFERENCES techstack(techstack_id);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
