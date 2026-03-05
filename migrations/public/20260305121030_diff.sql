-- liquibase formatted sql

-- changeset seongha.moon:1772680232584-1 splitStatements:false
ALTER TABLE "users" ADD "email2" VARCHAR(255) NOT NULL;

