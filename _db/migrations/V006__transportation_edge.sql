-- ============================================================
-- V006: Transportation Edge (graph edges between locations)
-- The core table for the route engine
-- ============================================================
SET search_path TO intermodal, public;
CREATE TABLE transportation_edge (
    id                          UUID PRIMARY KEY,
    origin_location_id          UUID NOT NULL,
    destination_location_id     UUID NOT NULL,
    transport_mode_id           UUID NOT NULL,
    provider_id                 UUID,
    service_area_id             UUID,
    service_code                TEXT,

    -- Schedule
    operating_days_mask         SMALLINT NOT NULL DEFAULT 127,
    operating_start_time        TIME,
    operating_end_time          TIME,
    valid_from                  DATE,
    valid_to                    DATE,

    -- Timetable (fixed-schedule modes: flight, train, bus)
    departure_time              TIME,
    arrival_time                TIME,

    -- Frequency (high-frequency modes: subway, bus — used when departure_time is NULL)
    frequency_minutes           INT,

    -- Status
    status                      TEXT NOT NULL DEFAULT 'ACTIVE',

    -- Data source
    source                      TEXT NOT NULL DEFAULT 'MANUAL',

    -- Estimates
    estimated_duration_min      INT,
    estimated_cost_cents        INT,
    distance_m                  INT,

    -- Environmental impact
    co2_grams                   INT,

    -- Walking access distances (from nearest walkable point)
    walking_access_origin_m     INT,
    walking_access_dest_m       INT,

    -- Flexible attributes
    attrs_json                  JSONB,

    -- Audit
    version                     BIGINT NOT NULL DEFAULT 1,
    created_date                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date          TIMESTAMPTZ,
    deleted                     BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT chk_edge_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_edge_source CHECK (source IN (
        'MANUAL', 'GOOGLE_API', 'GTFS', 'AMADEUS', 'COMPUTED'
    )),
    CONSTRAINT chk_edge_days_mask CHECK (
        operating_days_mask >= 0 AND operating_days_mask <= 127
    ),
    CONSTRAINT chk_edge_no_self_loop CHECK (
        origin_location_id != destination_location_id
    ),

    -- Foreign keys
    CONSTRAINT fk_edge_origin
        FOREIGN KEY (origin_location_id) REFERENCES location (id),
    CONSTRAINT fk_edge_destination
        FOREIGN KEY (destination_location_id) REFERENCES location (id),
    CONSTRAINT fk_edge_transport_mode
        FOREIGN KEY (transport_mode_id) REFERENCES transport_mode (id),
    CONSTRAINT fk_edge_provider
        FOREIGN KEY (provider_id) REFERENCES provider (id),
    CONSTRAINT fk_edge_service_area
        FOREIGN KEY (service_area_id) REFERENCES transport_service_area (id)
);

-- Critical query indexes for route engine
CREATE INDEX idx_edge_origin_mode
    ON transportation_edge (origin_location_id, transport_mode_id);
CREATE INDEX idx_edge_dest_mode
    ON transportation_edge (destination_location_id, transport_mode_id);
CREATE INDEX idx_edge_origin_dest_mode
    ON transportation_edge (origin_location_id, destination_location_id, transport_mode_id);
CREATE INDEX idx_edge_mode
    ON transportation_edge (transport_mode_id);
CREATE INDEX idx_edge_status
    ON transportation_edge (status) WHERE status = 'ACTIVE';
CREATE INDEX idx_edge_provider
    ON transportation_edge (provider_id) WHERE provider_id IS NOT NULL;
CREATE INDEX idx_edge_service_area
    ON transportation_edge (service_area_id) WHERE service_area_id IS NOT NULL;
CREATE INDEX idx_edge_validity
    ON transportation_edge (valid_from, valid_to)
    WHERE valid_from IS NOT NULL OR valid_to IS NOT NULL;

-- GIN index for flexible attributes
CREATE INDEX idx_edge_attrs ON transportation_edge USING GIN (attrs_json)
    WHERE attrs_json IS NOT NULL;

COMMENT ON TABLE transportation_edge IS 'Graph edges: connections between locations (flights, bus routes, uber rides, etc.)';
COMMENT ON COLUMN transportation_edge.transport_mode_id IS 'FK to transport_mode — FLIGHT, BUS, TRAIN, SUBWAY, UBER, FERRY, etc.';
COMMENT ON COLUMN transportation_edge.operating_days_mask IS '7-bit bitmask: bit0=Mon … bit6=Sun. 127=every day, 0=never. Example: Mon+Wed+Fri = 0b0010101 = 21';
COMMENT ON COLUMN transportation_edge.source IS 'How this edge was created: MANUAL=admin, GOOGLE_API=directions API, GTFS=transit feed, AMADEUS=flight API';
COMMENT ON COLUMN transportation_edge.walking_access_origin_m IS 'Walking distance from user to origin stop (filled by engine for fixed-stop modes)';
COMMENT ON COLUMN transportation_edge.walking_access_dest_m IS 'Walking distance from destination stop to final point';
