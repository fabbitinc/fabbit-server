--
-- PostgreSQL database dump
--

\restrict vpgKnwjZK6B4nIAaM2N0tw3VaIsVYHDgJbsvmtto3nhjgeFjmuwpbUwRYwjBTJS

-- Dumped from database version 18.1 (Debian 18.1-1.pgdg13+2)
-- Dumped by pg_dump version 18.1 (Debian 18.1-1.pgdg13+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA tenant_019cb9ee26fc7111aa3fb3f85c96893c;


--
-- Name: activity_target_type; Type: TYPE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TYPE tenant_019cb9ee26fc7111aa3fb3f85c96893c.activity_target_type AS ENUM (
    'PROJECT',
    'ISSUE',
    'ORGANIZATION'
);


--
-- Name: cr_state; Type: TYPE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TYPE tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_state AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'MERGED',
    'CLOSED'
);


--
-- Name: issue_state; Type: TYPE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TYPE tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_state AS ENUM (
    'OPEN',
    'CLOSED'
);


--
-- Name: issue_type; Type: TYPE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TYPE tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_type AS ENUM (
    'ISSUE',
    'CHANGE_REQUEST'
);


--
-- Name: notification_type; Type: TYPE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TYPE tenant_019cb9ee26fc7111aa3fb3f85c96893c.notification_type AS ENUM (
    'MENTION'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: _ag_label_edge; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge (
    id ag_catalog.graphid NOT NULL,
    start_id ag_catalog.graphid NOT NULL,
    end_id ag_catalog.graphid NOT NULL,
    properties ag_catalog.agtype DEFAULT ag_catalog.agtype_build_map() NOT NULL
);


--
-- Name: CONSISTS_OF; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF" (
)
INHERITS (tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge);


--
-- Name: CONSISTS_OF_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 281474976710655
    CACHE 1;


--
-- Name: CONSISTS_OF_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF_id_seq" OWNED BY tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF".id;


--
-- Name: _ag_label_vertex; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex (
    id ag_catalog.graphid NOT NULL,
    properties ag_catalog.agtype DEFAULT ag_catalog.agtype_build_map() NOT NULL
);


--
-- Name: Drawing; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing" (
)
INHERITS (tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex);


--
-- Name: Drawing_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 281474976710655
    CACHE 1;


--
-- Name: Drawing_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing_id_seq" OWNED BY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing".id;


--
-- Name: Part; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part" (
)
INHERITS (tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex);


--
-- Name: Part_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 281474976710655
    CACHE 1;


--
-- Name: Part_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part_id_seq" OWNED BY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part".id;


--
-- Name: Project; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project" (
)
INHERITS (tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex);


--
-- Name: Project_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 281474976710655
    CACHE 1;


--
-- Name: Project_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project_id_seq" OWNED BY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project".id;


--
-- Name: SUPPLIED_BY; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY" (
)
INHERITS (tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge);


--
-- Name: SUPPLIED_BY_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 281474976710655
    CACHE 1;


--
-- Name: SUPPLIED_BY_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY_id_seq" OWNED BY tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY".id;


--
-- Name: Supplier; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier" (
)
INHERITS (tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex);


--
-- Name: Supplier_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 281474976710655
    CACHE 1;


--
-- Name: Supplier_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier_id_seq" OWNED BY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier".id;


--
-- Name: _ag_label_edge_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 281474976710655
    CACHE 1;


--
-- Name: _ag_label_edge_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge_id_seq OWNED BY tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge.id;


--
-- Name: _ag_label_vertex_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 281474976710655
    CACHE 1;


--
-- Name: _ag_label_vertex_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex_id_seq OWNED BY tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex.id;


--
-- Name: _label_id_seq; Type: SEQUENCE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE SEQUENCE tenant_019cb9ee26fc7111aa3fb3f85c96893c._label_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 65535
    CACHE 1
    CYCLE;


--
-- Name: activities; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.activities (
    target_type tenant_019cb9ee26fc7111aa3fb3f85c96893c.activity_target_type NOT NULL,
    target_id uuid NOT NULL,
    action character varying(50) NOT NULL,
    actor_id uuid NOT NULL,
    detail jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: alembic_version; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.alembic_version (
    version_num character varying(32) NOT NULL
);


--
-- Name: bom_links; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.bom_links (
    id uuid NOT NULL,
    parent_part_id uuid NOT NULL,
    child_part_id uuid NOT NULL,
    quantity integer NOT NULL,
    extended_properties jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: change_request_issues; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_issues (
    change_request_id uuid NOT NULL,
    issue_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: change_request_reviewers; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_reviewers (
    change_request_id uuid NOT NULL,
    user_id uuid NOT NULL,
    review_status character varying(20) NOT NULL,
    reviewed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: change_requests; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_requests (
    id uuid NOT NULL,
    cr_state tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_state NOT NULL,
    merged_at timestamp with time zone,
    merged_by uuid
);


--
-- Name: cr_team_reviewers; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_team_reviewers (
    change_request_id uuid NOT NULL,
    team_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: drawing_analysis_records; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_analysis_records (
    id uuid NOT NULL,
    file_id uuid NOT NULL,
    name character varying(500) NOT NULL,
    analysis jsonb NOT NULL,
    page_count integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: drawing_synthesis_jobs; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_synthesis_jobs (
    id uuid NOT NULL,
    analysis_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    nodes_created integer NOT NULL,
    relationships_created integer NOT NULL,
    errors jsonb NOT NULL,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: drawings; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings (
    id uuid NOT NULL,
    folder_id uuid,
    project_id uuid,
    original_file_id uuid,
    pdf_file_id uuid,
    thumbnail_file_id uuid,
    name character varying(200) NOT NULL,
    original_file_key character varying(500),
    pdf_key character varying(500),
    thumbnail_key character varying(500),
    conversion_status character varying(20),
    drawing_number character varying(100),
    version character varying(50),
    status character varying(50),
    extended_properties jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone
);


--
-- Name: extended_property_definitions; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.extended_property_definitions (
    id uuid NOT NULL,
    key character varying(200) NOT NULL,
    display_name character varying(200) NOT NULL,
    data_type character varying(20) NOT NULL,
    target_entity character varying(50) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: files; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.files (
    id uuid NOT NULL,
    original_name character varying(500) NOT NULL,
    file_key character varying(1000) NOT NULL,
    content_type character varying(100) NOT NULL,
    file_size bigint NOT NULL,
    status character varying(20) NOT NULL,
    owner_type character varying(50),
    owner_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone
);


--
-- Name: issue_assignees; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_assignees (
    issue_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: issue_comments; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_comments (
    issue_id uuid NOT NULL,
    body text NOT NULL,
    created_by uuid,
    updated_by uuid,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: issue_labels; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_labels (
    issue_id uuid NOT NULL,
    label_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: issue_parts; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_parts (
    issue_id uuid NOT NULL,
    part_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: issue_team_assignees; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_team_assignees (
    issue_id uuid NOT NULL,
    team_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: issues; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues (
    number integer NOT NULL,
    type tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_type NOT NULL,
    title character varying(500) NOT NULL,
    body text,
    state tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_state NOT NULL,
    closed_at timestamp with time zone,
    created_by uuid,
    updated_by uuid,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: labels; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.labels (
    name character varying(50) NOT NULL,
    description character varying(200),
    color character varying(7) NOT NULL,
    created_by uuid,
    updated_by uuid,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: mapping_records; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_records (
    id uuid NOT NULL,
    name character varying(200) NOT NULL,
    scope character varying(20) NOT NULL,
    is_active boolean NOT NULL,
    usage_count integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone
);


--
-- Name: mapping_revisions; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_revisions (
    id uuid NOT NULL,
    record_id uuid NOT NULL,
    file_id uuid NOT NULL,
    version integer NOT NULL,
    sheet_name character varying(200),
    original_headers jsonb NOT NULL,
    mapping jsonb NOT NULL,
    usage_count integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: notifications; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.notifications (
    user_id uuid NOT NULL,
    type tenant_019cb9ee26fc7111aa3fb3f85c96893c.notification_type NOT NULL,
    actor_id uuid NOT NULL,
    payload jsonb NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: part_default_owners; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_default_owners (
    category character varying(100),
    default_owner_id uuid,
    default_owner_team_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: part_revisions; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_revisions (
    id uuid NOT NULL,
    part_id uuid NOT NULL,
    synthesis_job_id uuid,
    drawing_id uuid,
    part_number character varying(100) NOT NULL,
    name character varying(500),
    revision character varying(50) NOT NULL,
    material character varying(200),
    unit character varying(20),
    description text,
    category character varying(100),
    is_phantom boolean,
    lifecycle_state character varying(50),
    lead_time_days integer,
    extended_properties jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: part_suppliers; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_suppliers (
    id uuid NOT NULL,
    part_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    unit_cost double precision,
    extended_properties jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: parts; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts (
    id uuid NOT NULL,
    drawing_id uuid,
    owner_id uuid,
    owner_team_id uuid,
    part_number character varying(100) NOT NULL,
    name character varying(500),
    revision character varying(50) DEFAULT '1'::character varying NOT NULL,
    material character varying(200),
    unit character varying(20),
    description text,
    category character varying(100),
    is_phantom boolean,
    lifecycle_state character varying(50),
    lead_time_days integer,
    extended_properties jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: project_members; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_members (
    project_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: project_parts; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_parts (
    project_id uuid NOT NULL,
    part_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: projects; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.projects (
    name character varying(200) NOT NULL,
    description text,
    is_archived boolean NOT NULL,
    deleted_at timestamp with time zone,
    created_by uuid,
    updated_by uuid,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: storage_usage_snapshots; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.storage_usage_snapshots (
    snapshot_date date NOT NULL,
    drawing_bytes bigint NOT NULL,
    attachment_bytes bigint NOT NULL,
    other_bytes bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: suppliers; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.suppliers (
    id uuid NOT NULL,
    company_name character varying(200) NOT NULL,
    code character varying(100),
    country character varying(100),
    contact_info text,
    extended_properties jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: synthesis_batches; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_batches (
    id uuid NOT NULL,
    project_id uuid,
    mapping_id uuid NOT NULL,
    requested_count integer NOT NULL,
    accepted_count integer NOT NULL,
    failed_uploads jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: synthesis_jobs; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs (
    id uuid NOT NULL,
    batch_id uuid,
    mapping_id uuid NOT NULL,
    file_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    total_rows integer NOT NULL,
    processed_rows integer NOT NULL,
    nodes_created integer NOT NULL,
    relationships_created integer NOT NULL,
    errors jsonb NOT NULL,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: team_members; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.team_members (
    team_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: teams; Type: TABLE; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE TABLE tenant_019cb9ee26fc7111aa3fb3f85c96893c.teams (
    name character varying(100) NOT NULL,
    description text,
    created_by uuid NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: CONSISTS_OF id; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF" ALTER COLUMN id SET DEFAULT ag_catalog._graphid((ag_catalog._label_id('tenant_019cb9ee26fc7111aa3fb3f85c96893c'::name, 'CONSISTS_OF'::name))::integer, nextval('tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF_id_seq"'::regclass));


--
-- Name: CONSISTS_OF properties; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF" ALTER COLUMN properties SET DEFAULT ag_catalog.agtype_build_map();


--
-- Name: Drawing id; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing" ALTER COLUMN id SET DEFAULT ag_catalog._graphid((ag_catalog._label_id('tenant_019cb9ee26fc7111aa3fb3f85c96893c'::name, 'Drawing'::name))::integer, nextval('tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing_id_seq"'::regclass));


--
-- Name: Drawing properties; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing" ALTER COLUMN properties SET DEFAULT ag_catalog.agtype_build_map();


--
-- Name: Part id; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part" ALTER COLUMN id SET DEFAULT ag_catalog._graphid((ag_catalog._label_id('tenant_019cb9ee26fc7111aa3fb3f85c96893c'::name, 'Part'::name))::integer, nextval('tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part_id_seq"'::regclass));


--
-- Name: Part properties; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part" ALTER COLUMN properties SET DEFAULT ag_catalog.agtype_build_map();


--
-- Name: Project id; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project" ALTER COLUMN id SET DEFAULT ag_catalog._graphid((ag_catalog._label_id('tenant_019cb9ee26fc7111aa3fb3f85c96893c'::name, 'Project'::name))::integer, nextval('tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project_id_seq"'::regclass));


--
-- Name: Project properties; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project" ALTER COLUMN properties SET DEFAULT ag_catalog.agtype_build_map();


--
-- Name: SUPPLIED_BY id; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY" ALTER COLUMN id SET DEFAULT ag_catalog._graphid((ag_catalog._label_id('tenant_019cb9ee26fc7111aa3fb3f85c96893c'::name, 'SUPPLIED_BY'::name))::integer, nextval('tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY_id_seq"'::regclass));


--
-- Name: SUPPLIED_BY properties; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY" ALTER COLUMN properties SET DEFAULT ag_catalog.agtype_build_map();


--
-- Name: Supplier id; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier" ALTER COLUMN id SET DEFAULT ag_catalog._graphid((ag_catalog._label_id('tenant_019cb9ee26fc7111aa3fb3f85c96893c'::name, 'Supplier'::name))::integer, nextval('tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier_id_seq"'::regclass));


--
-- Name: Supplier properties; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier" ALTER COLUMN properties SET DEFAULT ag_catalog.agtype_build_map();


--
-- Name: _ag_label_edge id; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge ALTER COLUMN id SET DEFAULT ag_catalog._graphid((ag_catalog._label_id('tenant_019cb9ee26fc7111aa3fb3f85c96893c'::name, '_ag_label_edge'::name))::integer, nextval('tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge_id_seq'::regclass));


--
-- Name: _ag_label_vertex id; Type: DEFAULT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex ALTER COLUMN id SET DEFAULT ag_catalog._graphid((ag_catalog._label_id('tenant_019cb9ee26fc7111aa3fb3f85c96893c'::name, '_ag_label_vertex'::name))::integer, nextval('tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex_id_seq'::regclass));


--
-- Name: Drawing Drawing_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing"
    ADD CONSTRAINT "Drawing_pkey" PRIMARY KEY (id);


--
-- Name: Part Part_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part"
    ADD CONSTRAINT "Part_pkey" PRIMARY KEY (id);


--
-- Name: Project Project_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project"
    ADD CONSTRAINT "Project_pkey" PRIMARY KEY (id);


--
-- Name: Supplier Supplier_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier"
    ADD CONSTRAINT "Supplier_pkey" PRIMARY KEY (id);


--
-- Name: _ag_label_edge _ag_label_edge_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge
    ADD CONSTRAINT _ag_label_edge_pkey PRIMARY KEY (id);


--
-- Name: _ag_label_vertex _ag_label_vertex_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_vertex
    ADD CONSTRAINT _ag_label_vertex_pkey PRIMARY KEY (id);


--
-- Name: activities activities_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.activities
    ADD CONSTRAINT activities_pkey PRIMARY KEY (id);


--
-- Name: alembic_version alembic_version_pkc; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.alembic_version
    ADD CONSTRAINT alembic_version_pkc PRIMARY KEY (version_num);


--
-- Name: bom_links bom_links_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.bom_links
    ADD CONSTRAINT bom_links_pkey PRIMARY KEY (id);


--
-- Name: change_request_issues change_request_issues_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_issues
    ADD CONSTRAINT change_request_issues_pkey PRIMARY KEY (id);


--
-- Name: change_request_reviewers change_request_reviewers_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_reviewers
    ADD CONSTRAINT change_request_reviewers_pkey PRIMARY KEY (id);


--
-- Name: change_requests change_requests_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_requests
    ADD CONSTRAINT change_requests_pkey PRIMARY KEY (id);


--
-- Name: cr_team_reviewers cr_team_reviewers_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_team_reviewers
    ADD CONSTRAINT cr_team_reviewers_pkey PRIMARY KEY (id);


--
-- Name: drawing_analysis_records drawing_analysis_records_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_analysis_records
    ADD CONSTRAINT drawing_analysis_records_pkey PRIMARY KEY (id);


--
-- Name: drawing_synthesis_jobs drawing_synthesis_jobs_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_synthesis_jobs
    ADD CONSTRAINT drawing_synthesis_jobs_pkey PRIMARY KEY (id);


--
-- Name: drawings drawings_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings
    ADD CONSTRAINT drawings_pkey PRIMARY KEY (id);


--
-- Name: extended_property_definitions extended_property_definitions_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.extended_property_definitions
    ADD CONSTRAINT extended_property_definitions_pkey PRIMARY KEY (id);


--
-- Name: files files_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.files
    ADD CONSTRAINT files_pkey PRIMARY KEY (id);


--
-- Name: issue_assignees issue_assignees_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_assignees
    ADD CONSTRAINT issue_assignees_pkey PRIMARY KEY (id);


--
-- Name: issue_comments issue_comments_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_comments
    ADD CONSTRAINT issue_comments_pkey PRIMARY KEY (id);


--
-- Name: issue_labels issue_labels_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_labels
    ADD CONSTRAINT issue_labels_pkey PRIMARY KEY (id);


--
-- Name: issue_parts issue_parts_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_parts
    ADD CONSTRAINT issue_parts_pkey PRIMARY KEY (id);


--
-- Name: issue_team_assignees issue_team_assignees_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_team_assignees
    ADD CONSTRAINT issue_team_assignees_pkey PRIMARY KEY (id);


--
-- Name: issues issues_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues
    ADD CONSTRAINT issues_pkey PRIMARY KEY (id);


--
-- Name: labels labels_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.labels
    ADD CONSTRAINT labels_pkey PRIMARY KEY (id);


--
-- Name: mapping_records mapping_records_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_records
    ADD CONSTRAINT mapping_records_pkey PRIMARY KEY (id);


--
-- Name: mapping_revisions mapping_revisions_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_revisions
    ADD CONSTRAINT mapping_revisions_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: part_default_owners part_default_owners_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_default_owners
    ADD CONSTRAINT part_default_owners_pkey PRIMARY KEY (id);


--
-- Name: part_revisions part_revisions_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_revisions
    ADD CONSTRAINT part_revisions_pkey PRIMARY KEY (id);


--
-- Name: part_suppliers part_suppliers_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_suppliers
    ADD CONSTRAINT part_suppliers_pkey PRIMARY KEY (id);


--
-- Name: parts parts_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts
    ADD CONSTRAINT parts_pkey PRIMARY KEY (id);


--
-- Name: project_members project_members_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_members
    ADD CONSTRAINT project_members_pkey PRIMARY KEY (id);


--
-- Name: project_parts project_parts_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_parts
    ADD CONSTRAINT project_parts_pkey PRIMARY KEY (id);


--
-- Name: projects projects_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (id);


--
-- Name: storage_usage_snapshots storage_usage_snapshots_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.storage_usage_snapshots
    ADD CONSTRAINT storage_usage_snapshots_pkey PRIMARY KEY (id);


--
-- Name: suppliers suppliers_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);


--
-- Name: synthesis_batches synthesis_batches_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_batches
    ADD CONSTRAINT synthesis_batches_pkey PRIMARY KEY (id);


--
-- Name: synthesis_jobs synthesis_jobs_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs
    ADD CONSTRAINT synthesis_jobs_pkey PRIMARY KEY (id);


--
-- Name: team_members team_members_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.team_members
    ADD CONSTRAINT team_members_pkey PRIMARY KEY (id);


--
-- Name: teams teams_pkey; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);


--
-- Name: bom_links uq_bom_links_parent_part_id_child_part_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.bom_links
    ADD CONSTRAINT uq_bom_links_parent_part_id_child_part_id UNIQUE (parent_part_id, child_part_id);


--
-- Name: change_request_issues uq_change_request_issues_cr_id_issue_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_issues
    ADD CONSTRAINT uq_change_request_issues_cr_id_issue_id UNIQUE (change_request_id, issue_id);


--
-- Name: change_request_reviewers uq_cr_reviewers_cr_id_user_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_reviewers
    ADD CONSTRAINT uq_cr_reviewers_cr_id_user_id UNIQUE (change_request_id, user_id);


--
-- Name: cr_team_reviewers uq_cr_team_reviewers_cr_id_team_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_team_reviewers
    ADD CONSTRAINT uq_cr_team_reviewers_cr_id_team_id UNIQUE (change_request_id, team_id);


--
-- Name: drawings uq_drawings_drawing_number; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings
    ADD CONSTRAINT uq_drawings_drawing_number UNIQUE (drawing_number);


--
-- Name: extended_property_definitions uq_extended_property_definitions_key_target_entity; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.extended_property_definitions
    ADD CONSTRAINT uq_extended_property_definitions_key_target_entity UNIQUE (key, target_entity);


--
-- Name: files uq_files_file_key; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.files
    ADD CONSTRAINT uq_files_file_key UNIQUE (file_key);


--
-- Name: issue_assignees uq_issue_assignees_issue_id_user_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_assignees
    ADD CONSTRAINT uq_issue_assignees_issue_id_user_id UNIQUE (issue_id, user_id);


--
-- Name: issue_labels uq_issue_labels_issue_id_label_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_labels
    ADD CONSTRAINT uq_issue_labels_issue_id_label_id UNIQUE (issue_id, label_id);


--
-- Name: issue_parts uq_issue_parts_issue_id_part_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_parts
    ADD CONSTRAINT uq_issue_parts_issue_id_part_id UNIQUE (issue_id, part_id);


--
-- Name: issue_team_assignees uq_issue_team_assignees_issue_id_team_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_team_assignees
    ADD CONSTRAINT uq_issue_team_assignees_issue_id_team_id UNIQUE (issue_id, team_id);


--
-- Name: issues uq_issues_number; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues
    ADD CONSTRAINT uq_issues_number UNIQUE (number);


--
-- Name: labels uq_labels_name; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.labels
    ADD CONSTRAINT uq_labels_name UNIQUE (name);


--
-- Name: part_revisions uq_part_revisions_part_id_revision; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_revisions
    ADD CONSTRAINT uq_part_revisions_part_id_revision UNIQUE (part_id, revision);


--
-- Name: part_suppliers uq_part_suppliers_part_id_supplier_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_suppliers
    ADD CONSTRAINT uq_part_suppliers_part_id_supplier_id UNIQUE (part_id, supplier_id);


--
-- Name: parts uq_parts_part_number; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts
    ADD CONSTRAINT uq_parts_part_number UNIQUE (part_number);


--
-- Name: project_members uq_project_members_project_id_user_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_members
    ADD CONSTRAINT uq_project_members_project_id_user_id UNIQUE (project_id, user_id);


--
-- Name: project_parts uq_project_parts_project_id_part_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_parts
    ADD CONSTRAINT uq_project_parts_project_id_part_id UNIQUE (project_id, part_id);


--
-- Name: storage_usage_snapshots uq_storage_usage_snapshots_snapshot_date; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.storage_usage_snapshots
    ADD CONSTRAINT uq_storage_usage_snapshots_snapshot_date UNIQUE (snapshot_date);


--
-- Name: suppliers uq_suppliers_company_name; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.suppliers
    ADD CONSTRAINT uq_suppliers_company_name UNIQUE (company_name);


--
-- Name: team_members uq_team_members_team_id_user_id; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.team_members
    ADD CONSTRAINT uq_team_members_team_id_user_id UNIQUE (team_id, user_id);


--
-- Name: teams uq_teams_name; Type: CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.teams
    ADD CONSTRAINT uq_teams_name UNIQUE (name);


--
-- Name: CONSISTS_OF_end_id_idx; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX "CONSISTS_OF_end_id_idx" ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF" USING btree (end_id);


--
-- Name: CONSISTS_OF_start_id_idx; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX "CONSISTS_OF_start_id_idx" ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."CONSISTS_OF" USING btree (start_id);


--
-- Name: SUPPLIED_BY_end_id_idx; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX "SUPPLIED_BY_end_id_idx" ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY" USING btree (end_id);


--
-- Name: SUPPLIED_BY_start_id_idx; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX "SUPPLIED_BY_start_id_idx" ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."SUPPLIED_BY" USING btree (start_id);


--
-- Name: _ag_label_edge_end_id_idx; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX _ag_label_edge_end_id_idx ON tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge USING btree (end_id);


--
-- Name: _ag_label_edge_start_id_idx; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX _ag_label_edge_start_id_idx ON tenant_019cb9ee26fc7111aa3fb3f85c96893c._ag_label_edge USING btree (start_id);


--
-- Name: ix_activities_target; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_activities_target ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.activities USING btree (target_type, target_id);


--
-- Name: ix_bom_links_child_part_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_bom_links_child_part_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.bom_links USING btree (child_part_id);


--
-- Name: ix_bom_links_extended_properties; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_bom_links_extended_properties ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.bom_links USING gin (extended_properties);


--
-- Name: ix_bom_links_parent_part_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_bom_links_parent_part_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.bom_links USING btree (parent_part_id);


--
-- Name: ix_change_request_issues_change_request_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_change_request_issues_change_request_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_issues USING btree (change_request_id);


--
-- Name: ix_change_request_issues_issue_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_change_request_issues_issue_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_issues USING btree (issue_id);


--
-- Name: ix_cr_reviewers_change_request_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_cr_reviewers_change_request_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_reviewers USING btree (change_request_id);


--
-- Name: ix_cr_reviewers_user_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_cr_reviewers_user_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_reviewers USING btree (user_id);


--
-- Name: ix_cr_team_reviewers_change_request_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_cr_team_reviewers_change_request_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_team_reviewers USING btree (change_request_id);


--
-- Name: ix_cr_team_reviewers_team_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_cr_team_reviewers_team_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_team_reviewers USING btree (team_id);


--
-- Name: ix_drawing_analysis_records_file_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawing_analysis_records_file_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_analysis_records USING btree (file_id);


--
-- Name: ix_drawing_drawing_number; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawing_drawing_number ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."Drawing" USING btree (ag_catalog.agtype_access_operator(VARIADIC ARRAY[properties, '"drawing_number"'::ag_catalog.agtype]));


--
-- Name: ix_drawing_synthesis_jobs_analysis_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawing_synthesis_jobs_analysis_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_synthesis_jobs USING btree (analysis_id);


--
-- Name: ix_drawings_drawing_number; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawings_drawing_number ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings USING btree (drawing_number);


--
-- Name: ix_drawings_extended_properties; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawings_extended_properties ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings USING gin (extended_properties);


--
-- Name: ix_drawings_folder_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawings_folder_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings USING btree (folder_id);


--
-- Name: ix_drawings_name; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawings_name ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings USING btree (name);


--
-- Name: ix_drawings_original_file_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawings_original_file_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings USING btree (original_file_id);


--
-- Name: ix_drawings_pdf_file_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawings_pdf_file_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings USING btree (pdf_file_id);


--
-- Name: ix_drawings_project_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawings_project_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings USING btree (project_id);


--
-- Name: ix_drawings_thumbnail_file_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_drawings_thumbnail_file_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings USING btree (thumbnail_file_id);


--
-- Name: ix_files_owner_type_owner_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_files_owner_type_owner_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.files USING btree (owner_type, owner_id);


--
-- Name: ix_issue_assignees_issue_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_assignees_issue_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_assignees USING btree (issue_id);


--
-- Name: ix_issue_assignees_user_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_assignees_user_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_assignees USING btree (user_id);


--
-- Name: ix_issue_comments_issue_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_comments_issue_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_comments USING btree (issue_id);


--
-- Name: ix_issue_labels_issue_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_labels_issue_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_labels USING btree (issue_id);


--
-- Name: ix_issue_labels_label_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_labels_label_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_labels USING btree (label_id);


--
-- Name: ix_issue_parts_issue_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_parts_issue_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_parts USING btree (issue_id);


--
-- Name: ix_issue_parts_part_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_parts_part_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_parts USING btree (part_id);


--
-- Name: ix_issue_team_assignees_issue_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_team_assignees_issue_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_team_assignees USING btree (issue_id);


--
-- Name: ix_issue_team_assignees_team_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_issue_team_assignees_team_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_team_assignees USING btree (team_id);


--
-- Name: ix_mapping_records_scope_is_active; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_mapping_records_scope_is_active ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_records USING btree (scope, is_active);


--
-- Name: ix_mapping_revisions_file_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_mapping_revisions_file_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_revisions USING btree (file_id);


--
-- Name: ix_mapping_revisions_record_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_mapping_revisions_record_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_revisions USING btree (record_id);


--
-- Name: ix_notifications_user_unread; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_notifications_user_unread ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.notifications USING btree (user_id, read_at);


--
-- Name: ix_part_part_number; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_part_part_number ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."Part" USING btree (ag_catalog.agtype_access_operator(VARIADIC ARRAY[properties, '"part_number"'::ag_catalog.agtype]));


--
-- Name: ix_part_revisions_part_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_part_revisions_part_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_revisions USING btree (part_id);


--
-- Name: ix_part_revisions_synthesis_job_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_part_revisions_synthesis_job_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_revisions USING btree (synthesis_job_id);


--
-- Name: ix_part_suppliers_extended_properties; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_part_suppliers_extended_properties ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_suppliers USING gin (extended_properties);


--
-- Name: ix_part_suppliers_part_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_part_suppliers_part_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_suppliers USING btree (part_id);


--
-- Name: ix_part_suppliers_supplier_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_part_suppliers_supplier_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_suppliers USING btree (supplier_id);


--
-- Name: ix_parts_category; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_parts_category ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts USING btree (category);


--
-- Name: ix_parts_drawing_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_parts_drawing_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts USING btree (drawing_id);


--
-- Name: ix_parts_extended_properties; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_parts_extended_properties ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts USING gin (extended_properties);


--
-- Name: ix_parts_name; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_parts_name ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts USING btree (name);


--
-- Name: ix_parts_owner_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_parts_owner_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts USING btree (owner_id);


--
-- Name: ix_parts_owner_team_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_parts_owner_team_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts USING btree (owner_team_id);


--
-- Name: ix_project_members_project_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_project_members_project_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_members USING btree (project_id);


--
-- Name: ix_project_members_user_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_project_members_user_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_members USING btree (user_id);


--
-- Name: ix_project_name; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_project_name ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project" USING btree (ag_catalog.agtype_access_operator(VARIADIC ARRAY[properties, '"name"'::ag_catalog.agtype]));


--
-- Name: ix_project_parts_part_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_project_parts_part_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_parts USING btree (part_id);


--
-- Name: ix_project_parts_project_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_project_parts_project_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_parts USING btree (project_id);


--
-- Name: ix_project_project_code; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_project_project_code ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."Project" USING btree (ag_catalog.agtype_access_operator(VARIADIC ARRAY[properties, '"project_code"'::ag_catalog.agtype]));


--
-- Name: ix_storage_usage_snapshots_snapshot_date; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_storage_usage_snapshots_snapshot_date ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.storage_usage_snapshots USING btree (snapshot_date);


--
-- Name: ix_supplier_code; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_supplier_code ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier" USING btree (ag_catalog.agtype_access_operator(VARIADIC ARRAY[properties, '"code"'::ag_catalog.agtype]));


--
-- Name: ix_supplier_company_name; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_supplier_company_name ON tenant_019cb9ee26fc7111aa3fb3f85c96893c."Supplier" USING btree (ag_catalog.agtype_access_operator(VARIADIC ARRAY[properties, '"company_name"'::ag_catalog.agtype]));


--
-- Name: ix_suppliers_code; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_suppliers_code ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.suppliers USING btree (code);


--
-- Name: ix_suppliers_extended_properties; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_suppliers_extended_properties ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.suppliers USING gin (extended_properties);


--
-- Name: ix_synthesis_batches_mapping_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_synthesis_batches_mapping_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_batches USING btree (mapping_id);


--
-- Name: ix_synthesis_batches_project_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_synthesis_batches_project_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_batches USING btree (project_id);


--
-- Name: ix_synthesis_jobs_batch_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_synthesis_jobs_batch_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs USING btree (batch_id);


--
-- Name: ix_synthesis_jobs_file_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_synthesis_jobs_file_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs USING btree (file_id);


--
-- Name: ix_synthesis_jobs_mapping_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_synthesis_jobs_mapping_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs USING btree (mapping_id);


--
-- Name: ix_team_members_team_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_team_members_team_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.team_members USING btree (team_id);


--
-- Name: ix_team_members_user_id; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE INDEX ix_team_members_user_id ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.team_members USING btree (user_id);


--
-- Name: uq_mapping_records_name; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE UNIQUE INDEX uq_mapping_records_name ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_records USING btree (name);


--
-- Name: uq_mapping_revisions_record_version; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE UNIQUE INDEX uq_mapping_revisions_record_version ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_revisions USING btree (record_id, version);


--
-- Name: uq_part_default_owners_category; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE UNIQUE INDEX uq_part_default_owners_category ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_default_owners USING btree (category) WHERE (category IS NOT NULL);


--
-- Name: uq_part_default_owners_fallback; Type: INDEX; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

CREATE UNIQUE INDEX uq_part_default_owners_fallback ON tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_default_owners USING btree ((true)) WHERE (category IS NULL);


--
-- Name: bom_links bom_links_child_part_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.bom_links
    ADD CONSTRAINT bom_links_child_part_id_fkey FOREIGN KEY (child_part_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts(id) ON DELETE CASCADE;


--
-- Name: bom_links bom_links_parent_part_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.bom_links
    ADD CONSTRAINT bom_links_parent_part_id_fkey FOREIGN KEY (parent_part_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts(id) ON DELETE CASCADE;


--
-- Name: change_request_issues change_request_issues_change_request_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_issues
    ADD CONSTRAINT change_request_issues_change_request_id_fkey FOREIGN KEY (change_request_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_requests(id) ON DELETE CASCADE;


--
-- Name: change_request_issues change_request_issues_issue_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_issues
    ADD CONSTRAINT change_request_issues_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues(id) ON DELETE CASCADE;


--
-- Name: change_request_reviewers change_request_reviewers_change_request_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_request_reviewers
    ADD CONSTRAINT change_request_reviewers_change_request_id_fkey FOREIGN KEY (change_request_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_requests(id) ON DELETE CASCADE;


--
-- Name: change_requests change_requests_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_requests
    ADD CONSTRAINT change_requests_id_fkey FOREIGN KEY (id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues(id) ON DELETE CASCADE;


--
-- Name: cr_team_reviewers cr_team_reviewers_change_request_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_team_reviewers
    ADD CONSTRAINT cr_team_reviewers_change_request_id_fkey FOREIGN KEY (change_request_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.change_requests(id) ON DELETE CASCADE;


--
-- Name: cr_team_reviewers cr_team_reviewers_team_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.cr_team_reviewers
    ADD CONSTRAINT cr_team_reviewers_team_id_fkey FOREIGN KEY (team_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.teams(id) ON DELETE CASCADE;


--
-- Name: drawing_analysis_records drawing_analysis_records_file_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_analysis_records
    ADD CONSTRAINT drawing_analysis_records_file_id_fkey FOREIGN KEY (file_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.files(id) ON DELETE CASCADE;


--
-- Name: drawing_synthesis_jobs drawing_synthesis_jobs_analysis_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_synthesis_jobs
    ADD CONSTRAINT drawing_synthesis_jobs_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawing_analysis_records(id) ON DELETE CASCADE;


--
-- Name: drawings drawings_original_file_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings
    ADD CONSTRAINT drawings_original_file_id_fkey FOREIGN KEY (original_file_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.files(id) ON DELETE SET NULL;


--
-- Name: drawings drawings_pdf_file_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings
    ADD CONSTRAINT drawings_pdf_file_id_fkey FOREIGN KEY (pdf_file_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.files(id) ON DELETE SET NULL;


--
-- Name: drawings drawings_thumbnail_file_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings
    ADD CONSTRAINT drawings_thumbnail_file_id_fkey FOREIGN KEY (thumbnail_file_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.files(id) ON DELETE SET NULL;


--
-- Name: issue_assignees issue_assignees_issue_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_assignees
    ADD CONSTRAINT issue_assignees_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues(id) ON DELETE CASCADE;


--
-- Name: issue_comments issue_comments_issue_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_comments
    ADD CONSTRAINT issue_comments_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues(id) ON DELETE CASCADE;


--
-- Name: issue_labels issue_labels_issue_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_labels
    ADD CONSTRAINT issue_labels_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues(id) ON DELETE CASCADE;


--
-- Name: issue_labels issue_labels_label_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_labels
    ADD CONSTRAINT issue_labels_label_id_fkey FOREIGN KEY (label_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.labels(id) ON DELETE CASCADE;


--
-- Name: issue_parts issue_parts_issue_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_parts
    ADD CONSTRAINT issue_parts_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues(id) ON DELETE CASCADE;


--
-- Name: issue_parts issue_parts_part_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_parts
    ADD CONSTRAINT issue_parts_part_id_fkey FOREIGN KEY (part_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts(id) ON DELETE CASCADE;


--
-- Name: issue_team_assignees issue_team_assignees_issue_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_team_assignees
    ADD CONSTRAINT issue_team_assignees_issue_id_fkey FOREIGN KEY (issue_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.issues(id) ON DELETE CASCADE;


--
-- Name: issue_team_assignees issue_team_assignees_team_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.issue_team_assignees
    ADD CONSTRAINT issue_team_assignees_team_id_fkey FOREIGN KEY (team_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.teams(id) ON DELETE CASCADE;


--
-- Name: mapping_revisions mapping_revisions_file_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_revisions
    ADD CONSTRAINT mapping_revisions_file_id_fkey FOREIGN KEY (file_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.files(id) ON DELETE CASCADE;


--
-- Name: mapping_revisions mapping_revisions_record_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_revisions
    ADD CONSTRAINT mapping_revisions_record_id_fkey FOREIGN KEY (record_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_records(id) ON DELETE CASCADE;


--
-- Name: part_default_owners part_default_owners_default_owner_team_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_default_owners
    ADD CONSTRAINT part_default_owners_default_owner_team_id_fkey FOREIGN KEY (default_owner_team_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.teams(id) ON DELETE SET NULL;


--
-- Name: part_revisions part_revisions_part_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_revisions
    ADD CONSTRAINT part_revisions_part_id_fkey FOREIGN KEY (part_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts(id) ON DELETE CASCADE;


--
-- Name: part_revisions part_revisions_synthesis_job_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_revisions
    ADD CONSTRAINT part_revisions_synthesis_job_id_fkey FOREIGN KEY (synthesis_job_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs(id) ON DELETE SET NULL;


--
-- Name: part_suppliers part_suppliers_part_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_suppliers
    ADD CONSTRAINT part_suppliers_part_id_fkey FOREIGN KEY (part_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts(id) ON DELETE CASCADE;


--
-- Name: part_suppliers part_suppliers_supplier_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.part_suppliers
    ADD CONSTRAINT part_suppliers_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.suppliers(id) ON DELETE CASCADE;


--
-- Name: parts parts_drawing_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts
    ADD CONSTRAINT parts_drawing_id_fkey FOREIGN KEY (drawing_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.drawings(id) ON DELETE SET NULL;


--
-- Name: parts parts_owner_team_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts
    ADD CONSTRAINT parts_owner_team_id_fkey FOREIGN KEY (owner_team_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.teams(id) ON DELETE SET NULL;


--
-- Name: project_members project_members_project_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_members
    ADD CONSTRAINT project_members_project_id_fkey FOREIGN KEY (project_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.projects(id) ON DELETE CASCADE;


--
-- Name: project_parts project_parts_part_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_parts
    ADD CONSTRAINT project_parts_part_id_fkey FOREIGN KEY (part_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.parts(id) ON DELETE CASCADE;


--
-- Name: project_parts project_parts_project_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.project_parts
    ADD CONSTRAINT project_parts_project_id_fkey FOREIGN KEY (project_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.projects(id) ON DELETE CASCADE;


--
-- Name: synthesis_batches synthesis_batches_mapping_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_batches
    ADD CONSTRAINT synthesis_batches_mapping_id_fkey FOREIGN KEY (mapping_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_records(id) ON DELETE CASCADE;


--
-- Name: synthesis_jobs synthesis_jobs_batch_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs
    ADD CONSTRAINT synthesis_jobs_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_batches(id) ON DELETE SET NULL;


--
-- Name: synthesis_jobs synthesis_jobs_file_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs
    ADD CONSTRAINT synthesis_jobs_file_id_fkey FOREIGN KEY (file_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.files(id) ON DELETE CASCADE;


--
-- Name: synthesis_jobs synthesis_jobs_mapping_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.synthesis_jobs
    ADD CONSTRAINT synthesis_jobs_mapping_id_fkey FOREIGN KEY (mapping_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.mapping_records(id) ON DELETE CASCADE;


--
-- Name: team_members team_members_team_id_fkey; Type: FK CONSTRAINT; Schema: tenant_019cb9ee26fc7111aa3fb3f85c96893c; Owner: -
--

ALTER TABLE ONLY tenant_019cb9ee26fc7111aa3fb3f85c96893c.team_members
    ADD CONSTRAINT team_members_team_id_fkey FOREIGN KEY (team_id) REFERENCES tenant_019cb9ee26fc7111aa3fb3f85c96893c.teams(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict vpgKnwjZK6B4nIAaM2N0tw3VaIsVYHDgJbsvmtto3nhjgeFjmuwpbUwRYwjBTJS

