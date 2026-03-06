--
-- PostgreSQL database dump
--

\restrict tyN75uQj6UarHW8e9LSBUoqW4itmOSuIRX8pfUOMDfmifY1TmkOBc3zhha7dIwz

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
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ai_usage_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_usage_logs (
    id uuid NOT NULL,
    org_id uuid NOT NULL,
    user_id uuid NOT NULL,
    category character varying(30) NOT NULL,
    feature character varying(50) NOT NULL,
    model character varying(50) NOT NULL,
    input_tokens integer NOT NULL,
    output_tokens integer NOT NULL,
    credits_used numeric(10,4) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: alembic_version; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alembic_version (
    version_num character varying(32) NOT NULL
);


--
-- Name: email_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verifications (
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    code_hash character varying(64) NOT NULL,
    verification_token_hash character varying(64),
    status character varying(20) NOT NULL,
    attempt_count integer NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: invitations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invitations (
    id uuid NOT NULL,
    org_id uuid NOT NULL,
    email character varying(255) NOT NULL,
    role character varying(20) NOT NULL,
    token_hash character varying(64) NOT NULL,
    status character varying(20) NOT NULL,
    invited_by uuid NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    accepted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: memberships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.memberships (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    org_id uuid NOT NULL,
    role character varying(20) NOT NULL,
    job_role character varying(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: organizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organizations (
    id uuid NOT NULL,
    slug character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    owner_id uuid NOT NULL,
    industry character varying(50),
    team_size character varying(20),
    plan_type character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    profile_image_file_key character varying(1000),
    max_members integer NOT NULL,
    used_members integer NOT NULL,
    plan_credits_remaining integer NOT NULL,
    bonus_credits_remaining integer NOT NULL,
    storage_bytes_limit bigint NOT NULL,
    storage_bytes_used bigint NOT NULL,
    allow_storage_overage boolean NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_jti character varying(36) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: subscriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subscriptions (
    org_id uuid NOT NULL,
    plan_type character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    billing_cycle character varying(20) NOT NULL,
    current_period_start timestamp with time zone NOT NULL,
    current_period_end timestamp with time zone NOT NULL,
    max_members integer NOT NULL,
    ai_credits_granted integer NOT NULL,
    storage_bytes_limit bigint NOT NULL,
    cancel_at_period_end boolean NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    hashed_password character varying(255) NOT NULL,
    full_name character varying(100) NOT NULL,
    phone character varying(20),
    profile_image_file_key character varying(1000),
    is_active boolean NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: ai_usage_logs ai_usage_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_usage_logs
    ADD CONSTRAINT ai_usage_logs_pkey PRIMARY KEY (id);


--
-- Name: alembic_version alembic_version_pkc; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alembic_version
    ADD CONSTRAINT alembic_version_pkc PRIMARY KEY (version_num);


--
-- Name: email_verifications email_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications
    ADD CONSTRAINT email_verifications_pkey PRIMARY KEY (id);


--
-- Name: invitations invitations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT invitations_pkey PRIMARY KEY (id);


--
-- Name: memberships memberships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.memberships
    ADD CONSTRAINT memberships_pkey PRIMARY KEY (id);


--
-- Name: organizations organizations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: subscriptions subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);


--
-- Name: invitations uq_invitations_org_id_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT uq_invitations_org_id_email UNIQUE (org_id, email);


--
-- Name: invitations uq_invitations_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT uq_invitations_token_hash UNIQUE (token_hash);


--
-- Name: memberships uq_memberships_user_id_org_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.memberships
    ADD CONSTRAINT uq_memberships_user_id_org_id UNIQUE (user_id, org_id);


--
-- Name: organizations uq_organizations_slug; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT uq_organizations_slug UNIQUE (slug);


--
-- Name: refresh_tokens uq_refresh_tokens_token_jti; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT uq_refresh_tokens_token_jti UNIQUE (token_jti);


--
-- Name: users uq_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: ix_ai_usage_logs_org_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ai_usage_logs_org_id ON public.ai_usage_logs USING btree (org_id);


--
-- Name: ix_ai_usage_logs_org_id_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ai_usage_logs_org_id_created_at ON public.ai_usage_logs USING btree (org_id, created_at);


--
-- Name: ix_ai_usage_logs_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_ai_usage_logs_user_id ON public.ai_usage_logs USING btree (user_id);


--
-- Name: ix_email_verifications_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_email_verifications_email ON public.email_verifications USING btree (email);


--
-- Name: ix_invitations_invited_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_invitations_invited_by ON public.invitations USING btree (invited_by);


--
-- Name: ix_invitations_org_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_invitations_org_id ON public.invitations USING btree (org_id);


--
-- Name: ix_memberships_org_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_memberships_org_id ON public.memberships USING btree (org_id);


--
-- Name: ix_organizations_owner_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_organizations_owner_id ON public.organizations USING btree (owner_id);


--
-- Name: ix_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: ix_subscriptions_org_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_subscriptions_org_id ON public.subscriptions USING btree (org_id);


--
-- Name: uq_subscriptions_org_id_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_subscriptions_org_id_active ON public.subscriptions USING btree (org_id) WHERE ((status)::text = 'ACTIVE'::text);


--
-- Name: ai_usage_logs ai_usage_logs_org_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_usage_logs
    ADD CONSTRAINT ai_usage_logs_org_id_fkey FOREIGN KEY (org_id) REFERENCES public.organizations(id) ON DELETE CASCADE;


--
-- Name: ai_usage_logs ai_usage_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_usage_logs
    ADD CONSTRAINT ai_usage_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: invitations invitations_invited_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT invitations_invited_by_fkey FOREIGN KEY (invited_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: invitations invitations_org_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT invitations_org_id_fkey FOREIGN KEY (org_id) REFERENCES public.organizations(id) ON DELETE CASCADE;


--
-- Name: memberships memberships_org_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.memberships
    ADD CONSTRAINT memberships_org_id_fkey FOREIGN KEY (org_id) REFERENCES public.organizations(id) ON DELETE CASCADE;


--
-- Name: memberships memberships_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.memberships
    ADD CONSTRAINT memberships_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: organizations organizations_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT organizations_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: subscriptions subscriptions_org_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_org_id_fkey FOREIGN KEY (org_id) REFERENCES public.organizations(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict tyN75uQj6UarHW8e9LSBUoqW4itmOSuIRX8pfUOMDfmifY1TmkOBc3zhha7dIwz
