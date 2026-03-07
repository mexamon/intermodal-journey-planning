-- ============================================================
-- V002: Auth Layer — app_user
-- ============================================================
SET search_path TO intermodal, public;
CREATE TABLE app_user (
    id                  UUID PRIMARY KEY,
    email               TEXT NOT NULL,
    password_hash       TEXT NOT NULL,
    role                TEXT NOT NULL DEFAULT 'AGENCY',
    display_name        TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'AGENCY'))
);

CREATE UNIQUE INDEX uq_app_user_email ON app_user (LOWER(email));

COMMENT ON TABLE app_user IS 'Application users with role-based access control';
COMMENT ON COLUMN app_user.role IS 'ADMIN = full access, AGENCY = route listing only';
COMMENT ON COLUMN app_user.version IS 'Optimistic locking version';
COMMENT ON COLUMN app_user.created_date IS 'Record creation timestamp (UTC)';
COMMENT ON COLUMN app_user.last_modified_date IS 'Record last modification timestamp (UTC)';
COMMENT ON COLUMN app_user.deleted IS 'Soft delete flag';
