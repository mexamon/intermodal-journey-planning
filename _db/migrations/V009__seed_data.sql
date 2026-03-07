-- ============================================================
-- V009: Seed Data — transport modes + default global policy
-- Idempotent: uses ON CONFLICT for safe re-runs
-- ============================================================
SET search_path TO intermodal, public;
-- ══════════════════════════════════════
-- 1. Transport Mode Registry
-- ══════════════════════════════════════

INSERT INTO transport_mode (code, name, category, coverage_type, edge_resolution, requires_stop, max_walking_access_m, default_speed_kmh, api_provider, icon, color_hex, sort_order) VALUES
    ('FLIGHT',  'Flight',   'AIR',          'FIXED_STOP',     'STATIC',      TRUE,  0,    800, NULL,     'plane',         '#1E88E5', 1),
    ('BUS',     'Bus',      'GROUND_FIXED', 'FIXED_STOP',     'STATIC',      TRUE,  800,  40,  NULL,     'bus',           '#43A047', 2),
    ('TRAIN',   'Train',    'GROUND_FIXED', 'FIXED_STOP',     'STATIC',      TRUE,  1200, 120, NULL,     'train',         '#E53935', 3),
    ('SUBWAY',  'Subway',   'GROUND_FIXED', 'NETWORK',        'STATIC',      TRUE,  600,  35,  NULL,     'subway',        '#8E24AA', 4),
    ('UBER',    'Uber',     'GROUND_FLEX',  'POINT_TO_POINT', 'API_DYNAMIC', FALSE, 0,    30,  'GOOGLE', 'car',           '#212121', 5),
    ('FERRY',   'Ferry',    'GROUND_FIXED', 'FIXED_STOP',     'STATIC',      TRUE,  500,  25,  NULL,     'ship',          '#00ACC1', 6),
    ('WALKING', 'Walking',  'PEDESTRIAN',   'COMPUTED',        'COMPUTED',    FALSE, NULL, 5,   NULL,     'walking',       '#78909C', 7),
    ('BIKE',    'Bike',     'PEDESTRIAN',   'COMPUTED',        'COMPUTED',    FALSE, NULL, 15,  NULL,     'bicycle',       '#FF8F00', 8)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    coverage_type = EXCLUDED.coverage_type,
    edge_resolution = EXCLUDED.edge_resolution,
    requires_stop = EXCLUDED.requires_stop,
    max_walking_access_m = EXCLUDED.max_walking_access_m,
    default_speed_kmh = EXCLUDED.default_speed_kmh,
    api_provider = EXCLUDED.api_provider,
    icon = EXCLUDED.icon,
    color_hex = EXCLUDED.color_hex,
    sort_order = EXCLUDED.sort_order;


-- ══════════════════════════════════════
-- 2. Default Global Policy
-- ══════════════════════════════════════

-- Policy Set
INSERT INTO journey_policy_set (
    id, code, scope_type, scope_key, segment, status, version,
    description, created_by
) VALUES (
    'a0000000-0000-4000-8000-000000000001',
    'DEFAULT_GLOBAL', 'GLOBAL', '*', 'DEFAULT', 'ACTIVE', 1,
    'Default global journey policy: 1 flight, 0-2 ground transfers, max 3 legs',
    'SYSTEM'
) ON CONFLICT (code) DO NOTHING;

-- Constraints
INSERT INTO journey_policy_constraints (
    id, policy_set_id, max_legs, min_flights, max_flights,
    min_transfers, max_transfers, max_total_duration_min, max_walking_total_m
) VALUES (
    'a1000000-0000-4000-8000-000000000001',
    'a0000000-0000-4000-8000-000000000001',
    3,    -- max legs (before + flight + after)
    1,    -- min flights
    1,    -- max flights
    0,    -- min transfers (flight-only route is valid)
    2,    -- max transfers (1 before + 1 after)
    NULL, -- no duration limit
    2000  -- max 2km total walking
) ON CONFLICT (id) DO NOTHING;

-- ── State Machine Nodes ──

-- START node
INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, props_json, ui_x, ui_y) VALUES
    ('b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001',
     'START', 1, 1, NULL, 50, 200)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

-- BEFORE node (optional pre-flight transfer)
INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, props_json, ui_x, ui_y) VALUES
    ('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001',
     'BEFORE', 0, 1,
     '{"allowed_types": ["BUS", "TRAIN", "SUBWAY", "UBER", "FERRY"], "same_city_only": true, "max_walking_m": 800}'::jsonb,
     250, 200)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

-- FLIGHT node (required)
INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, props_json, ui_x, ui_y) VALUES
    ('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001',
     'FLIGHT', 1, 1,
     '{"allowed_types": ["FLIGHT"]}'::jsonb,
     500, 200)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

-- AFTER node (optional post-flight transfer)
INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, props_json, ui_x, ui_y) VALUES
    ('b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001',
     'AFTER', 0, 1,
     '{"allowed_types": ["BUS", "TRAIN", "SUBWAY", "UBER", "FERRY"], "same_city_only": true, "max_walking_m": 800}'::jsonb,
     750, 200)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

-- END node
INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, props_json, ui_x, ui_y) VALUES
    ('b0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000001',
     'END', 1, 1, NULL, 1000, 200)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;


-- ── State Machine Transitions ──

-- START → BEFORE (take pre-flight transfer)
INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json, ui_json) VALUES
    ('c0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001',
     'b0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
     10, '{"typeNotIn": ["FLIGHT"]}'::jsonb,
     '{"label": "ground transfer"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- START → FLIGHT (direct flight, no pre-transfer)
INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json, ui_json) VALUES
    ('c0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001',
     'b0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000003',
     5, '{"typeIn": ["FLIGHT"]}'::jsonb,
     '{"label": "skip before", "style": {"strokeDasharray": "5,5"}}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- BEFORE → FLIGHT
INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json, ui_json) VALUES
    ('c0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001',
     'b0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000003',
     10, '{"typeIn": ["FLIGHT"]}'::jsonb,
     '{"label": "board flight"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- FLIGHT → AFTER (take post-flight transfer)
INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json, ui_json) VALUES
    ('c0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001',
     'b0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000004',
     10, '{"typeNotIn": ["FLIGHT"]}'::jsonb,
     '{"label": "ground transfer"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- FLIGHT → END (direct arrival, no post-transfer)
INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json, ui_json) VALUES
    ('c0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000001',
     'b0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000005',
     5, NULL,
     '{"label": "skip after", "style": {"strokeDasharray": "5,5"}}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- AFTER → END
INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json, ui_json) VALUES
    ('c0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000001',
     'b0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000005',
     10, NULL,
     '{"label": "arrive"}'::jsonb)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════
-- 3. Default Admin User (password: admin123 — CHANGE IN PRODUCTION!)
-- Hash: bcrypt placeholder — actual hash to be set by application
-- ══════════════════════════════════════

INSERT INTO app_user (id, email, password_hash, role, display_name) VALUES
    ('d0000000-0000-4000-8000-000000000001',
     'admin@intermodal.dev',
     '$2a$10$PLACEHOLDER_HASH_CHANGE_IN_PRODUCTION',
     'ADMIN',
     'System Admin')
ON CONFLICT DO NOTHING;

INSERT INTO app_user (id, email, password_hash, role, display_name) VALUES
    ('d0000000-0000-4000-8000-000000000002',
     'agency@intermodal.dev',
     '$2a$10$PLACEHOLDER_HASH_CHANGE_IN_PRODUCTION',
     'AGENCY',
     'Demo Agency')
ON CONFLICT DO NOTHING;
