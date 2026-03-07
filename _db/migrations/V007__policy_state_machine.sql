-- ============================================================
-- V007: Policy / State Machine Layer
-- journey_policy_set, constraints, nodes, transitions
-- ============================================================
SET search_path TO intermodal, public;
-- ----- journey_policy_set (versioned, scoped rule sets) -----
CREATE TABLE journey_policy_set (
    id                      UUID PRIMARY KEY,
    code                    TEXT NOT NULL,
    scope_type              TEXT NOT NULL,
    scope_key               TEXT NOT NULL,
    segment                 TEXT NOT NULL DEFAULT 'DEFAULT',
    status                  TEXT NOT NULL DEFAULT 'DRAFT',
    parent_policy_set_id    UUID,
    effective_from          TIMESTAMPTZ,
    effective_to            TIMESTAMPTZ,
    description             TEXT,
    created_by              TEXT,

    -- Audit (version also serves as the policy revision number)
    version                 BIGINT NOT NULL DEFAULT 1,
    created_date            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date      TIMESTAMPTZ,
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_policy_code UNIQUE (code),
    CONSTRAINT chk_policy_scope CHECK (scope_type IN (
        'GLOBAL', 'COUNTRY', 'REGION', 'AIRPORT', 'AIRPORT_PAIR'
    )),
    CONSTRAINT chk_policy_segment CHECK (segment IN (
        'DEFAULT', 'CORPORATE', 'ELITE', 'IRROPS'
    )),
    CONSTRAINT chk_policy_status CHECK (status IN (
        'DRAFT', 'ACTIVE', 'DEPRECATED'
    )),
    CONSTRAINT fk_policy_parent
        FOREIGN KEY (parent_policy_set_id) REFERENCES journey_policy_set (id)
);

CREATE INDEX idx_policy_scope
    ON journey_policy_set (scope_type, scope_key, segment, status);
CREATE INDEX idx_policy_parent
    ON journey_policy_set (parent_policy_set_id)
    WHERE parent_policy_set_id IS NOT NULL;
CREATE INDEX idx_policy_effective
    ON journey_policy_set (effective_from, effective_to);

COMMENT ON TABLE journey_policy_set IS 'Versioned journey policy sets — defines rules per scope (global, country, airport, airport-pair)';
COMMENT ON COLUMN journey_policy_set.scope_type IS 'Policy resolution order: AIRPORT_PAIR > AIRPORT > REGION > COUNTRY > GLOBAL';
COMMENT ON COLUMN journey_policy_set.scope_key IS 'Scope identifier: * (global), TR (country), TR-34 (region), SAW (airport), SAW-LHR (pair)';
COMMENT ON COLUMN journey_policy_set.segment IS 'Customer segment: DEFAULT, CORPORATE, ELITE, IRROPS (irregular operations)';

-- ----- journey_policy_constraints (route-level limits) -----
CREATE TABLE journey_policy_constraints (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_set_id           UUID NOT NULL,
    max_legs                INT NOT NULL DEFAULT 3,
    min_flights             INT NOT NULL DEFAULT 1,
    max_flights             INT NOT NULL DEFAULT 1,
    min_transfers           INT NOT NULL DEFAULT 0,
    max_transfers           INT NOT NULL DEFAULT 2,
    max_total_duration_min  INT,
    max_walking_total_m     INT,
    min_connection_minutes  INT,
    max_total_co2_grams     INT,
    preferred_modes_json    JSONB,
    constraints_json        JSONB,

    -- Audit
    version                 BIGINT NOT NULL DEFAULT 1,
    created_date            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date      TIMESTAMPTZ,
    deleted                 BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_constraints_policy
        FOREIGN KEY (policy_set_id) REFERENCES journey_policy_set (id) ON DELETE CASCADE,
    CONSTRAINT chk_flights_range CHECK (min_flights <= max_flights),
    CONSTRAINT chk_transfers_range CHECK (min_transfers <= max_transfers),
    CONSTRAINT chk_max_legs_positive CHECK (max_legs > 0)
);

COMMENT ON TABLE journey_policy_constraints IS 'Route-level constraints for a policy set (max legs, flight count, walking limits)';
COMMENT ON COLUMN journey_policy_constraints.max_legs IS 'Maximum total transportation segments in a route (default 3: before + flight + after)';
COMMENT ON COLUMN journey_policy_constraints.max_walking_total_m IS 'Maximum total walking distance allowed across the route';

-- ----- journey_policy_node (state machine nodes, React Flow nodes) -----
CREATE TABLE journey_policy_node (
    id                  UUID PRIMARY KEY,
    policy_set_id       UUID NOT NULL,
    node_key            TEXT NOT NULL,
    min_visits          INT NOT NULL DEFAULT 0,
    max_visits          INT NOT NULL DEFAULT 1,
    props_json          JSONB,
    ui_x                INT,
    ui_y                INT,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_policy_node_key UNIQUE (policy_set_id, node_key),
    CONSTRAINT chk_visits_range CHECK (min_visits <= max_visits),
    CONSTRAINT chk_visits_positive CHECK (max_visits >= 0),
    CONSTRAINT fk_node_policy
        FOREIGN KEY (policy_set_id) REFERENCES journey_policy_set (id) ON DELETE CASCADE
);

CREATE INDEX idx_node_policy ON journey_policy_node (policy_set_id);

COMMENT ON TABLE journey_policy_node IS 'State machine nodes for journey policy (maps to React Flow nodes)';
COMMENT ON COLUMN journey_policy_node.node_key IS 'Free-text node type. Recommended: START, BEFORE, FLIGHT, TRANSFER, AFTER, END, WALK_ACCESS. START and END are required by the engine.';
COMMENT ON COLUMN journey_policy_node.min_visits IS '0=optional segment, 1=required segment';
COMMENT ON COLUMN journey_policy_node.max_visits IS '1=single, 2+=allows connecting flights or multiple transfers';
COMMENT ON COLUMN journey_policy_node.props_json IS 'Node properties: {"allowed_types":["BUS","TRAIN"], "same_city_only":true, "max_walking_m":800}';
COMMENT ON COLUMN journey_policy_node.ui_x IS 'React Flow X position (persisted for drag-drop editor)';

-- ----- journey_policy_transition (state machine edges, React Flow edges) -----
CREATE TABLE journey_policy_transition (
    id                  UUID PRIMARY KEY,
    policy_set_id       UUID NOT NULL,
    from_node_id        UUID NOT NULL,
    to_node_id          UUID NOT NULL,
    priority            INT NOT NULL DEFAULT 0,
    guard_json          JSONB,
    ui_json             JSONB,

    -- Audit
    version             BIGINT NOT NULL DEFAULT 1,
    created_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_date  TIMESTAMPTZ,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_transition_policy
        FOREIGN KEY (policy_set_id) REFERENCES journey_policy_set (id) ON DELETE CASCADE,
    CONSTRAINT fk_transition_from
        FOREIGN KEY (from_node_id) REFERENCES journey_policy_node (id) ON DELETE CASCADE,
    CONSTRAINT fk_transition_to
        FOREIGN KEY (to_node_id) REFERENCES journey_policy_node (id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_transition CHECK (from_node_id != to_node_id)
);

CREATE INDEX idx_transition_policy ON journey_policy_transition (policy_set_id);
CREATE INDEX idx_transition_from ON journey_policy_transition (policy_set_id, from_node_id);
CREATE INDEX idx_transition_to ON journey_policy_transition (policy_set_id, to_node_id);

COMMENT ON TABLE journey_policy_transition IS 'State machine transitions (maps to React Flow edges)';
COMMENT ON COLUMN journey_policy_transition.guard_json IS 'Transition guard conditions: {"typeNotIn":["FLIGHT"], "maxWalkingM":1000, "same_airport":true}';
COMMENT ON COLUMN journey_policy_transition.ui_json IS 'React Flow edge styling: {"label":"skip", "animated":true, "style":{"stroke":"#ff0"}}';
