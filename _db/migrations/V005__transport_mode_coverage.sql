-- ============================================================
-- V005: Transport Mode & Coverage Layer
-- transport_mode, transport_service_area, transport_stop
-- ============================================================
SET search_path TO intermodal, public;
-- ----- transport_mode (registry of transport types) -----
CREATE TABLE transport_mode (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                TEXT NOT NULL,
    name                TEXT NOT NULL,
    category            TEXT NOT NULL,
    coverage_type       TEXT NOT NULL,
    edge_resolution     TEXT NOT NULL,
    requires_stop       BOOLEAN NOT NULL DEFAULT FALSE,
    max_walking_access_m INT,
    default_speed_kmh   INT,
    api_provider        TEXT,
    icon                TEXT,
    color_hex           TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    config_json         JSONB,
    sort_order          INT NOT NULL DEFAULT 0,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_transport_mode_code UNIQUE (code),
    CONSTRAINT chk_mode_category CHECK (category IN (
        'AIR', 'GROUND_FIXED', 'GROUND_FLEX', 'PEDESTRIAN'
    )),
    CONSTRAINT chk_mode_coverage CHECK (coverage_type IN (
        'POINT_TO_POINT', 'FIXED_STOP', 'NETWORK', 'COMPUTED'
    )),
    CONSTRAINT chk_mode_resolution CHECK (edge_resolution IN (
        'STATIC', 'API_DYNAMIC', 'COMPUTED', 'HYBRID'
    ))
);

COMMENT ON TABLE transport_mode IS 'Registry defining how each transport type works (coverage, edge resolution strategy)';
COMMENT ON COLUMN transport_mode.coverage_type IS 'POINT_TO_POINT=anywhere (Uber), FIXED_STOP=stations (Bus/Train), NETWORK=graph (Subway), COMPUTED=distance-based (Walking)';
COMMENT ON COLUMN transport_mode.edge_resolution IS 'STATIC=pre-defined edges, API_DYNAMIC=resolved at query time, COMPUTED=distance calc, HYBRID=static+API enrichment';
COMMENT ON COLUMN transport_mode.max_walking_access_m IS 'Max walking distance to reach this mode (e.g. 800m for bus stop, 1200m for train station)';

-- ----- transport_service_area (where does each mode operate?) -----
CREATE TABLE transport_service_area (
    id                  UUID PRIMARY KEY,
    transport_mode_id   UUID NOT NULL,
    provider_id         UUID,
    name                TEXT NOT NULL,
    area_type           TEXT NOT NULL,

    -- Radius-based coverage
    center_lat          NUMERIC(9,6),
    center_lon          NUMERIC(9,6),
    radius_m            INT,

    -- Polygon-based coverage (PostGIS future)
    boundary_geojson    JSONB,

    -- General scope
    country_iso_code    CHAR(2),
    city                TEXT,

    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from          DATE,
    valid_to            DATE,
    config_json         JSONB,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_area_type CHECK (area_type IN (
        'RADIUS', 'POLYGON', 'CITY', 'COUNTRY', 'GLOBAL'
    )),
    CONSTRAINT fk_sa_transport_mode
        FOREIGN KEY (transport_mode_id) REFERENCES transport_mode (id),
    CONSTRAINT fk_sa_provider
        FOREIGN KEY (provider_id) REFERENCES provider (id),
    CONSTRAINT fk_sa_country
        FOREIGN KEY (country_iso_code) REFERENCES ref_country (iso_code)
);

CREATE INDEX idx_sa_mode ON transport_service_area (transport_mode_id);
CREATE INDEX idx_sa_provider ON transport_service_area (provider_id)
    WHERE provider_id IS NOT NULL;
CREATE INDEX idx_sa_country ON transport_service_area (country_iso_code)
    WHERE country_iso_code IS NOT NULL;
CREATE INDEX idx_sa_active ON transport_service_area (is_active)
    WHERE is_active = TRUE;

COMMENT ON TABLE transport_service_area IS 'Defines where a transport mode operates (e.g. Istanbul Uber Zone, IETT Bus Network)';
COMMENT ON COLUMN transport_service_area.area_type IS 'RADIUS=circle, POLYGON=geojson boundary, CITY/COUNTRY/GLOBAL=name-based scope';

-- ----- transport_stop (boarding points for fixed-route modes) -----
CREATE TABLE transport_stop (
    id                  UUID PRIMARY KEY,
    location_id         UUID NOT NULL,
    service_area_id     UUID NOT NULL,
    stop_code           TEXT,
    stop_name           TEXT NOT NULL,
    stop_sequence       INT,
    is_terminal         BOOLEAN NOT NULL DEFAULT FALSE,
    platform_info       TEXT,
    attrs_json          JSONB,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_stop_location
        FOREIGN KEY (location_id) REFERENCES location (id) ON DELETE CASCADE,
    CONSTRAINT fk_stop_service_area
        FOREIGN KEY (service_area_id) REFERENCES transport_service_area (id)
);

CREATE INDEX idx_stop_location ON transport_stop (location_id);
CREATE INDEX idx_stop_service_area ON transport_stop (service_area_id);
CREATE INDEX idx_stop_code ON transport_stop (stop_code)
    WHERE stop_code IS NOT NULL;

COMMENT ON TABLE transport_stop IS 'Boarding points for fixed-route modes (bus stops, train stations, metro stations)';
COMMENT ON COLUMN transport_stop.location_id IS 'FK to location (type=STATION) — every stop is also a searchable location';
COMMENT ON COLUMN transport_stop.stop_sequence IS 'Order on the route line (1=first stop, N=last stop)';
