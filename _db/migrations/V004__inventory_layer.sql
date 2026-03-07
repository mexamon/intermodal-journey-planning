-- ============================================================
-- V004: Inventory Layer — provider, location, airport_profile, runway_profile
-- Core entities for the intermodal graph
-- ============================================================
SET search_path TO intermodal, public;
-- ----- provider (transport operators) -----
CREATE TABLE provider (
    id                  UUID PRIMARY KEY,
    code                TEXT NOT NULL,
    name                TEXT NOT NULL,
    type                TEXT NOT NULL,
    country_iso_code    CHAR(2),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    config_json         JSONB,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_provider_code UNIQUE (code),
    CONSTRAINT chk_provider_type CHECK (type IN (
        'AIRLINE', 'BUS_COMPANY', 'TRAIN_OPERATOR',
        'METRO_OPERATOR', 'RIDE_SHARE', 'FERRY_OPERATOR', 'OTHER'
    )),
    CONSTRAINT fk_provider_country
        FOREIGN KEY (country_iso_code) REFERENCES ref_country (iso_code)
);

COMMENT ON TABLE provider IS 'Transport service providers (THY, IETT, Uber, etc.)';

-- ----- location (unified location table) -----
CREATE TABLE location (
    id                  UUID PRIMARY KEY,
    type                TEXT NOT NULL,
    name                TEXT NOT NULL,
    country_iso_code    CHAR(2) NOT NULL,
    region_code         TEXT,
    city                TEXT,
    timezone            TEXT,
    lat                 NUMERIC(9,6),
    lon                 NUMERIC(9,6),

    -- Aviation codes (nullable, airport-specific)
    iata_code           CHAR(3),
    icao_code           CHAR(4),
    gps_code            TEXT,
    local_code          TEXT,

    -- Search & dropdown
    is_searchable       BOOLEAN NOT NULL DEFAULT TRUE,
    search_priority     INT NOT NULL DEFAULT 0,
    search_aliases      TEXT[],

    -- PostGIS spatial column (OPTIONAL — populated via trigger when lat/lon are set)
    geom                GEOMETRY(Point, 4326),

    -- Seed provenance
    source              TEXT NOT NULL DEFAULT 'INTERNAL',
    source_pk           TEXT,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_location_type CHECK (type IN (
        'AIRPORT', 'CITY', 'STATION', 'POI'
    )),
    CONSTRAINT chk_location_source CHECK (source IN (
        'INTERNAL', 'OURAIRPORTS', 'GOOGLE_PLACES', 'GTFS', 'API'
    )),
    CONSTRAINT fk_location_country
        FOREIGN KEY (country_iso_code) REFERENCES ref_country (iso_code),
    CONSTRAINT fk_location_region
        FOREIGN KEY (region_code) REFERENCES ref_region (code)
);

-- IATA code: partial unique index (many locations have no IATA)
CREATE UNIQUE INDEX uq_location_iata
    ON location (iata_code) WHERE iata_code IS NOT NULL;

-- ICAO code: partial unique index
CREATE UNIQUE INDEX uq_location_icao
    ON location (icao_code) WHERE icao_code IS NOT NULL;

-- Seed dedup
CREATE UNIQUE INDEX uq_location_source_pk
    ON location (source, source_pk) WHERE source_pk IS NOT NULL;

-- Search & filtering indexes
CREATE INDEX idx_location_type ON location (type);
CREATE INDEX idx_location_country ON location (country_iso_code);
CREATE INDEX idx_location_region ON location (region_code);
CREATE INDEX idx_location_searchable ON location (is_searchable, search_priority DESC)
    WHERE is_searchable = TRUE;

-- Trigram index for fuzzy name search
CREATE INDEX idx_location_name_trgm ON location USING GIN (name gin_trgm_ops);

-- Spatial index (PostGIS GIST)
CREATE INDEX idx_location_geom ON location USING GIST (geom);

COMMENT ON TABLE location IS 'Unified location: airports, cities, stations, POIs';
COMMENT ON COLUMN location.type IS 'AIRPORT | CITY | STATION | POI';
COMMENT ON COLUMN location.iata_code IS '3-char IATA code (airports only), e.g. IST, LHR';
COMMENT ON COLUMN location.is_searchable IS 'Whether this location appears in user-facing search/dropdown';
COMMENT ON COLUMN location.search_priority IS 'Higher = appears first in search results (IST=200, Taksim=100)';
COMMENT ON COLUMN location.search_aliases IS 'Alternative names for search: {"Taksim Meydanı", "Taksim Sq"}';
COMMENT ON COLUMN location.geom IS 'PostGIS point for spatial proximity queries';
COMMENT ON COLUMN location.source IS 'INTERNAL=admin entered, OURAIRPORTS=CSV seed, etc.';

-- Trigger: auto-populate geom from lat/lon on insert/update
CREATE OR REPLACE FUNCTION fn_location_update_geom()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.lat IS NOT NULL AND NEW.lon IS NOT NULL THEN
        NEW.geom := ST_SetSRID(ST_MakePoint(NEW.lon, NEW.lat), 4326);
    ELSE
        NEW.geom := NULL;
    END IF;
    NEW.last_modified_date := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_location_geom
    BEFORE INSERT OR UPDATE OF lat, lon ON location
    FOR EACH ROW
    EXECUTE FUNCTION fn_location_update_geom();

-- ----- airport_profile (airport-specific extension) -----
CREATE TABLE airport_profile (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id         UUID NOT NULL,
    airport_kind        TEXT NOT NULL,
    elevation_ft        INT,
    scheduled_service   BOOLEAN,
    home_link           TEXT,
    wikipedia_link      TEXT,
    keywords            TEXT,

    -- Capacity & operations
    terminal_count      INT,
    avg_transfer_minutes INT,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_airport_kind CHECK (airport_kind IN (
        'large_airport', 'medium_airport', 'small_airport',
        'heliport', 'seaplane_base', 'balloonport', 'closed'
    )),
    CONSTRAINT fk_airport_location
        FOREIGN KEY (location_id) REFERENCES location (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_airport_location ON airport_profile (location_id);
CREATE INDEX idx_airport_kind ON airport_profile (airport_kind);
CREATE INDEX idx_airport_scheduled ON airport_profile (scheduled_service)
    WHERE scheduled_service = TRUE;

COMMENT ON TABLE airport_profile IS 'Airport-specific attributes (1:1 extension of location)';
COMMENT ON COLUMN airport_profile.airport_kind IS 'OurAirports type: large_airport, small_airport, heliport…';

-- ----- runway_profile (optional, for future policy rules) -----
CREATE TABLE runway_profile (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id         UUID NOT NULL,
    ident               TEXT,
    length_ft           INT,
    width_ft            INT,
    surface             TEXT,
    is_lighted          BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed           BOOLEAN NOT NULL DEFAULT FALSE,
    source_pk           TEXT,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_runway_location
        FOREIGN KEY (location_id) REFERENCES location (id) ON DELETE CASCADE
);

CREATE INDEX idx_runway_location ON runway_profile (location_id);

COMMENT ON TABLE runway_profile IS 'Runway data for policy rules (e.g. min runway length for aircraft type)';
