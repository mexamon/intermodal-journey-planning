-- ============================================================
-- V013: Edge Trip Schedule — GTFS-compatible model
-- Separates route templates from individual trip departures
-- ADDITIVE ONLY — no DROP, no ALTER existing constraints
-- ============================================================
SET search_path TO intermodal, public;

-- ╔══════════════════════════════════════════════════════════════╗
-- ║  STEP 1: Add schedule_type to transportation_edge           ║
-- ║  Classifies how the edge resolves its schedule              ║
-- ╚══════════════════════════════════════════════════════════════╝

ALTER TABLE transportation_edge
    ADD COLUMN schedule_type TEXT NOT NULL DEFAULT 'FIXED';

ALTER TABLE transportation_edge
    ADD CONSTRAINT chk_schedule_type
    CHECK (schedule_type IN ('FIXED', 'FREQUENCY', 'ON_DEMAND'));

COMMENT ON COLUMN transportation_edge.schedule_type IS
    'FIXED=named departures (flights, ICE trains) → see edge_trip. FREQUENCY=interval-based (metro every 5min). ON_DEMAND=no schedule (Uber, walking).';

-- ╔══════════════════════════════════════════════════════════════╗
-- ║  STEP 2: Create edge_trip table                             ║
-- ║  Individual departures for FIXED-schedule routes            ║
-- ╚══════════════════════════════════════════════════════════════╝

CREATE TABLE edge_trip (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    edge_id                 UUID NOT NULL,

    -- Trip identity
    service_code            TEXT,              -- TK1987, ICE 578, YHT 8001

    -- Timetable
    departure_time          TIME NOT NULL,
    arrival_time            TIME NOT NULL,

    -- Operating schedule
    operating_days_mask     SMALLINT NOT NULL DEFAULT 127,

    -- Validity window (seasonal schedules)
    valid_from              DATE,
    valid_to                DATE,

    -- Cost (per-trip pricing, complements fare table)
    estimated_cost_cents    INT,

    -- Flexible attributes (seat class, meal, codeshare info, etc.)
    attrs_json              JSONB,

    -- Audit
    version                 BIGINT NOT NULL DEFAULT 1,
    created_date            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date      TIMESTAMPTZ,
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT chk_trip_days_mask CHECK (
        operating_days_mask >= 0 AND operating_days_mask <= 127
    ),
    CONSTRAINT fk_trip_edge
        FOREIGN KEY (edge_id) REFERENCES transportation_edge (id) ON DELETE CASCADE
);

-- Query indexes
CREATE INDEX idx_trip_edge
    ON edge_trip (edge_id);
CREATE INDEX idx_trip_edge_departure
    ON edge_trip (edge_id, departure_time);
CREATE INDEX idx_trip_validity
    ON edge_trip (valid_from, valid_to)
    WHERE valid_from IS NOT NULL OR valid_to IS NOT NULL;
CREATE INDEX idx_trip_service_code
    ON edge_trip (service_code)
    WHERE service_code IS NOT NULL;

COMMENT ON TABLE edge_trip IS 'Individual trip departures for FIXED-schedule routes (e.g. TK1987 07:35, TK1971 13:40). One transportation_edge can have many edge_trips.';
COMMENT ON COLUMN edge_trip.service_code IS 'Carrier-assigned trip identifier: flight number (TK1987), train number (ICE 578), bus route code.';
COMMENT ON COLUMN edge_trip.operating_days_mask IS '7-bit bitmask: bit0=Mon … bit6=Sun. 127=every day. Per-trip override of the route default.';
COMMENT ON COLUMN edge_trip.valid_from IS 'First date this trip operates (seasonal schedule). NULL=always valid.';

-- ╔══════════════════════════════════════════════════════════════╗
-- ║  STEP 3: Migrate existing data from transportation_edge     ║
-- ║  Copy departure/arrival rows into edge_trip                 ║
-- ╚══════════════════════════════════════════════════════════════╝

INSERT INTO edge_trip (
    edge_id, service_code, departure_time, arrival_time,
    operating_days_mask, valid_from, valid_to, estimated_cost_cents
)
SELECT
    id, service_code, departure_time, arrival_time,
    operating_days_mask, valid_from, valid_to, estimated_cost_cents
FROM transportation_edge
WHERE departure_time IS NOT NULL
  AND deleted = FALSE;

-- ╔══════════════════════════════════════════════════════════════╗
-- ║  STEP 4: Classify existing edges by schedule_type           ║
-- ╚══════════════════════════════════════════════════════════════╝

-- Frequency-based (metro, frequent bus): has frequency but no fixed departure
UPDATE transportation_edge
SET schedule_type = 'FREQUENCY'
WHERE frequency_minutes IS NOT NULL
  AND departure_time IS NULL;

-- On-demand (Uber, Walking): computed at query time
UPDATE transportation_edge
SET schedule_type = 'ON_DEMAND'
WHERE frequency_minutes IS NULL
  AND departure_time IS NULL
  AND source IN ('COMPUTED', 'GOOGLE_API');

-- Everything else stays as 'FIXED' (default)

-- ╔══════════════════════════════════════════════════════════════╗
-- ║  STEP 5: Add optional trip_id FK to fare table              ║
-- ║  Allows per-trip pricing (TK1987 Economy ≠ TK1971 Economy)  ║
-- ╚══════════════════════════════════════════════════════════════╝

ALTER TABLE fare
    ADD COLUMN trip_id UUID;

ALTER TABLE fare
    ADD CONSTRAINT fk_fare_trip
    FOREIGN KEY (trip_id) REFERENCES edge_trip (id) ON DELETE SET NULL;

CREATE INDEX idx_fare_trip ON fare (trip_id) WHERE trip_id IS NOT NULL;

COMMENT ON COLUMN fare.trip_id IS 'Optional link to specific trip. NULL=fare applies to all trips on this edge.';

-- ╔══════════════════════════════════════════════════════════════╗
-- ║  STEP 6: Add optional trip_id FK to schedule_exception      ║
-- ║  Allows per-trip cancellations (TK1987 cancelled, not 1971) ║
-- ╚══════════════════════════════════════════════════════════════╝

ALTER TABLE schedule_exception
    ADD COLUMN trip_id UUID;

ALTER TABLE schedule_exception
    ADD CONSTRAINT fk_exception_trip
    FOREIGN KEY (trip_id) REFERENCES edge_trip (id) ON DELETE SET NULL;

CREATE INDEX idx_exception_trip ON schedule_exception (trip_id) WHERE trip_id IS NOT NULL;

COMMENT ON COLUMN schedule_exception.trip_id IS 'Optional link to specific trip. NULL=exception applies to all trips on this edge.';

-- ╔══════════════════════════════════════════════════════════════╗
-- ║  STEP 7: Mark deprecated columns (NO DROP — backward compat)║
-- ╚══════════════════════════════════════════════════════════════╝

COMMENT ON COLUMN transportation_edge.service_code IS
    'DEPRECATED in V013: Use edge_trip.service_code for FIXED-schedule trips. Kept for backward compatibility.';
COMMENT ON COLUMN transportation_edge.departure_time IS
    'DEPRECATED in V013: Use edge_trip.departure_time for FIXED-schedule trips. Kept for backward compatibility.';
COMMENT ON COLUMN transportation_edge.arrival_time IS
    'DEPRECATED in V013: Use edge_trip.arrival_time for FIXED-schedule trips. Kept for backward compatibility.';
COMMENT ON COLUMN transportation_edge.valid_from IS
    'DEPRECATED in V013: Use edge_trip.valid_from for per-trip validity. Kept for backward compatibility.';
COMMENT ON COLUMN transportation_edge.valid_to IS
    'DEPRECATED in V013: Use edge_trip.valid_to for per-trip validity. Kept for backward compatibility.';
COMMENT ON COLUMN transportation_edge.estimated_cost_cents IS
    'DEPRECATED in V013: Use edge_trip.estimated_cost_cents or fare table. Kept for backward compatibility.';

-- Summary
DO $$
DECLARE
    v_trips INT;
    v_fixed INT;
    v_freq INT;
    v_demand INT;
BEGIN
    SELECT count(*) INTO v_trips FROM edge_trip WHERE deleted = FALSE;
    SELECT count(*) INTO v_fixed FROM transportation_edge WHERE schedule_type = 'FIXED' AND deleted = FALSE;
    SELECT count(*) INTO v_freq FROM transportation_edge WHERE schedule_type = 'FREQUENCY' AND deleted = FALSE;
    SELECT count(*) INTO v_demand FROM transportation_edge WHERE schedule_type = 'ON_DEMAND' AND deleted = FALSE;

    RAISE NOTICE '✅ V013 migration complete:';
    RAISE NOTICE '   edge_trip rows created: %', v_trips;
    RAISE NOTICE '   FIXED routes: %, FREQUENCY routes: %, ON_DEMAND routes: %', v_fixed, v_freq, v_demand;
END $$;
