-- ============================================================
-- V001: Schema & Required PostgreSQL Extensions
-- Intermodal Journey Planning
-- ============================================================

-- Create dedicated schema (all tables live here, not in 'public')
CREATE SCHEMA IF NOT EXISTS intermodal;
SET search_path TO intermodal, public;

-- Note: uuid-ossp NOT needed — Java (JPA) handles UUID generation

-- Spatial queries: ST_DWithin, GEOMETRY type, GIST index on location.geom
CREATE EXTENSION IF NOT EXISTS "postgis" SCHEMA public;

-- Fuzzy text search: GIN trigram index on location.name for dropdown search
CREATE EXTENSION IF NOT EXISTS "pg_trgm" SCHEMA public;
