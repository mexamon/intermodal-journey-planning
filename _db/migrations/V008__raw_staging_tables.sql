-- ============================================================
-- V008: Raw Staging Tables (for CSV seed pipeline)
-- Data is loaded here first, then upserted into target tables
-- ============================================================
SET search_path TO intermodal, public;
-- ----- raw_country (countries.csv staging) -----
CREATE TABLE raw_country (
    id              INT,
    code            TEXT,
    name            TEXT,
    continent       TEXT,
    wikipedia_link  TEXT,
    keywords        TEXT,
    batch_id        TEXT,
    loaded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE raw_country IS 'Staging: raw countries.csv import → upsert to ref_country';

-- ----- raw_region (regions.csv staging) -----
CREATE TABLE raw_region (
    id              INT,
    code            TEXT,
    local_code      TEXT,
    name            TEXT,
    continent       TEXT,
    iso_country     TEXT,
    wikipedia_link  TEXT,
    keywords        TEXT,
    batch_id        TEXT,
    loaded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE raw_region IS 'Staging: raw regions.csv import → upsert to ref_region';

-- ----- raw_airport (airports.csv staging) -----
CREATE TABLE raw_airport (
    id              INT,
    ident           TEXT,
    type            TEXT,
    name            TEXT,
    latitude_deg    NUMERIC(12,8),
    longitude_deg   NUMERIC(12,8),
    elevation_ft    INT,
    continent       TEXT,
    iso_country     TEXT,
    iso_region      TEXT,
    municipality    TEXT,
    scheduled_service TEXT,
    icao_code       TEXT,
    iata_code       TEXT,
    gps_code        TEXT,
    local_code      TEXT,
    home_link       TEXT,
    wikipedia_link  TEXT,
    keywords        TEXT,
    batch_id        TEXT,
    loaded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE raw_airport IS 'Staging: raw airports.csv import → upsert to location + airport_profile';

-- ----- raw_runway (runways.csv staging, for future use) -----
CREATE TABLE raw_runway (
    id                          INT,
    airport_ref                 INT,
    airport_ident               TEXT,
    length_ft                   INT,
    width_ft                    INT,
    surface                     TEXT,
    lighted                     SMALLINT,
    closed                      SMALLINT,
    le_ident                    TEXT,
    le_latitude_deg             NUMERIC(12,8),
    le_longitude_deg            NUMERIC(12,8),
    le_elevation_ft             INT,
    le_heading_degT             NUMERIC(6,2),
    le_displaced_threshold_ft   INT,
    he_ident                    TEXT,
    he_latitude_deg             NUMERIC(12,8),
    he_longitude_deg            NUMERIC(12,8),
    he_elevation_ft             INT,
    he_heading_degT             NUMERIC(6,2),
    he_displaced_threshold_ft   INT,
    batch_id                    TEXT,
    loaded_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE raw_runway IS 'Staging: raw runways.csv import → upsert to runway_profile (future)';
