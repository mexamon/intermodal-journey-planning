-- ============================================================
-- V003: Reference Data — ref_country, ref_region
-- Seeded from countries.csv and regions.csv (OurAirports)
-- ============================================================
SET search_path TO intermodal, public;
-- ----- ref_country -----
CREATE TABLE ref_country (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    iso_code            CHAR(2) NOT NULL,
    name                TEXT NOT NULL,
    continent           CHAR(2),
    wikipedia_link      TEXT,
    keywords            TEXT,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE ref_country
    ADD CONSTRAINT uq_ref_country_iso_code UNIQUE (iso_code);

CREATE INDEX idx_ref_country_continent ON ref_country (continent);

COMMENT ON TABLE ref_country IS 'ISO 3166-1 countries — seed from countries.csv';
COMMENT ON COLUMN ref_country.iso_code IS 'ISO 3166-1 alpha-2 code, e.g. TR, US, GB';
COMMENT ON COLUMN ref_country.continent IS 'Two-letter continent: AF, AN, AS, EU, NA, OC, SA';

-- ----- ref_region -----
CREATE TABLE ref_region (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                TEXT NOT NULL,
    local_code          TEXT,
    name                TEXT NOT NULL,
    continent           CHAR(2),
    country_iso_code    CHAR(2) NOT NULL,
    wikipedia_link      TEXT,
    keywords            TEXT,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_region_country
        FOREIGN KEY (country_iso_code) REFERENCES ref_country (iso_code)
);

ALTER TABLE ref_region
    ADD CONSTRAINT uq_ref_region_code UNIQUE (code);

CREATE INDEX idx_ref_region_country ON ref_region (country_iso_code);

COMMENT ON TABLE ref_region IS 'ISO 3166-2 regions/provinces — seed from regions.csv';
COMMENT ON COLUMN ref_region.code IS 'ISO 3166-2 region code, e.g. TR-34, US-CA';
