-- ============================================================
-- V010: Fare & Schedule Exception tables
-- Production-scale pricing and schedule override support
-- ============================================================
SET search_path TO intermodal, public;

-- ----- fare (pricing per edge, multiple fare classes) -----
CREATE TABLE fare (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    edge_id             UUID NOT NULL,
    fare_class          TEXT NOT NULL DEFAULT 'STANDARD',
    pricing_type        TEXT NOT NULL DEFAULT 'FIXED',
    price_cents         INT,
    currency            TEXT NOT NULL DEFAULT 'TRY',
    refundable          BOOLEAN NOT NULL DEFAULT FALSE,
    changeable          BOOLEAN NOT NULL DEFAULT FALSE,
    luggage_kg          INT,
    cabin_luggage_kg    INT,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_fare_class CHECK (fare_class IN (
        'ECONOMY', 'PREMIUM_ECONOMY', 'BUSINESS', 'FIRST',
        'STANDARD', 'COMFORT', 'VIP'
    )),
    CONSTRAINT chk_pricing_type CHECK (pricing_type IN (
        'FIXED', 'ESTIMATED', 'DYNAMIC', 'FREE'
    )),
    CONSTRAINT fk_fare_edge
        FOREIGN KEY (edge_id) REFERENCES transportation_edge (id) ON DELETE CASCADE
);

CREATE INDEX idx_fare_edge ON fare (edge_id);
CREATE INDEX idx_fare_class ON fare (edge_id, fare_class);

COMMENT ON TABLE fare IS 'Fare options per transportation edge (flight tickets, bus fares, etc.)';
COMMENT ON COLUMN fare.pricing_type IS 'FIXED=known price, ESTIMATED=approximate, DYNAMIC=API-resolved at query time, FREE=no cost';
COMMENT ON COLUMN fare.price_cents IS 'Price in minor units (cents/kuruş). NULL for DYNAMIC pricing (resolved via API)';

-- ----- schedule_exception (per-day overrides for edge schedules) -----
CREATE TABLE schedule_exception (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    edge_id             UUID NOT NULL,
    exception_date      DATE NOT NULL,
    exception_type      TEXT NOT NULL,
    reason              TEXT,
    override_start_time TIME,
    override_end_time   TIME,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_exception_type CHECK (exception_type IN (
        'CANCELLED', 'ADDED', 'MODIFIED'
    )),
    CONSTRAINT fk_exception_edge
        FOREIGN KEY (edge_id) REFERENCES transportation_edge (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_exception_edge_date
    ON schedule_exception (edge_id, exception_date);
CREATE INDEX idx_exception_date
    ON schedule_exception (exception_date);

COMMENT ON TABLE schedule_exception IS 'Per-day schedule overrides: cancellations, additions, and modifications';
COMMENT ON COLUMN schedule_exception.exception_type IS 'CANCELLED=service not running, ADDED=extra service, MODIFIED=changed hours';
