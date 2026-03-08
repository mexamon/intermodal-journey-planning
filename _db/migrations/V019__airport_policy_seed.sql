-- ============================================================
-- V019: Airport Journey Policy Seeds
-- Defines max first-mile / last-mile edge limits per airport
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- 1. SAW — Istanbul Sabiha Gökçen
--    Max 1 first-mile, 1 last-mile, 1 flight
-- ═══════════════════════════════════════
INSERT INTO journey_policy_set (id, code, scope_type, scope_key, segment, status, description, created_by)
VALUES ('a0000001-0001-4000-8000-000000000001',
        'SAW_DEFAULT', 'AIRPORT', 'SAW', 'DEFAULT', 'ACTIVE',
        'Sabiha Gökçen — max 1 first-mile transfer, 1 flight, 1 last-mile transfer',
        'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO journey_policy_constraints (id, policy_set_id, max_legs, min_flights, max_flights, min_transfers, max_transfers, max_total_duration_min, constraints_json)
VALUES ('b0000001-0001-4000-8000-000000000001',
        'a0000001-0001-4000-8000-000000000001',
        3, 1, 1, 0, 2, 720,
        '{"max_first_mile_edges": 1, "max_last_mile_edges": 1, "description": "SAW: 1 transfer before + 1 flight + 1 transfer after"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- Nodes: START → BEFORE → FLIGHT → AFTER → END
INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, ui_x, ui_y) VALUES
    ('c0000001-0001-4000-8000-000000000001', 'a0000001-0001-4000-8000-000000000001', 'START',  1, 1,   0, 150),
    ('c0000001-0001-4000-8000-000000000002', 'a0000001-0001-4000-8000-000000000001', 'BEFORE', 0, 1, 200, 150),
    ('c0000001-0001-4000-8000-000000000003', 'a0000001-0001-4000-8000-000000000001', 'FLIGHT', 1, 1, 400, 150),
    ('c0000001-0001-4000-8000-000000000004', 'a0000001-0001-4000-8000-000000000001', 'AFTER',  0, 1, 600, 150),
    ('c0000001-0001-4000-8000-000000000005', 'a0000001-0001-4000-8000-000000000001', 'END',    1, 1, 800, 150)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

-- Transitions: START→BEFORE→FLIGHT→AFTER→END  (+ shortcut START→FLIGHT, FLIGHT→END)
INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json) VALUES
    ('d0000001-0001-4000-8000-000000000001', 'a0000001-0001-4000-8000-000000000001',
     'c0000001-0001-4000-8000-000000000001', 'c0000001-0001-4000-8000-000000000002', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING"]}'::jsonb),
    ('d0000001-0001-4000-8000-000000000002', 'a0000001-0001-4000-8000-000000000001',
     'c0000001-0001-4000-8000-000000000002', 'c0000001-0001-4000-8000-000000000003', 20,
     '{"allowed_modes": ["FLIGHT"]}'::jsonb),
    ('d0000001-0001-4000-8000-000000000003', 'a0000001-0001-4000-8000-000000000001',
     'c0000001-0001-4000-8000-000000000003', 'c0000001-0001-4000-8000-000000000004', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING"]}'::jsonb),
    ('d0000001-0001-4000-8000-000000000004', 'a0000001-0001-4000-8000-000000000001',
     'c0000001-0001-4000-8000-000000000004', 'c0000001-0001-4000-8000-000000000005', 10, NULL),
    -- Shortcuts: direct to flight, direct to end
    ('d0000001-0001-4000-8000-000000000005', 'a0000001-0001-4000-8000-000000000001',
     'c0000001-0001-4000-8000-000000000001', 'c0000001-0001-4000-8000-000000000003', 5,
     '{"description": "Direct airport access — no first-mile needed"}'::jsonb),
    ('d0000001-0001-4000-8000-000000000006', 'a0000001-0001-4000-8000-000000000001',
     'c0000001-0001-4000-8000-000000000003', 'c0000001-0001-4000-8000-000000000005', 5,
     '{"description": "Direct airport arrival — no last-mile needed"}'::jsonb)
ON CONFLICT (id) DO NOTHING;


-- ═══════════════════════════════════════
-- 2. ESB — Ankara Esenboğa
--    Max 2 first-mile, 2 last-mile, 1 flight
-- ═══════════════════════════════════════
INSERT INTO journey_policy_set (id, code, scope_type, scope_key, segment, status, description, created_by)
VALUES ('a0000001-0002-4000-8000-000000000001',
        'ESB_DEFAULT', 'AIRPORT', 'ESB', 'DEFAULT', 'ACTIVE',
        'Esenboğa — max 2 first-mile transfers, 1 flight, 2 last-mile transfers (metro+taksi)',
        'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO journey_policy_constraints (id, policy_set_id, max_legs, min_flights, max_flights, min_transfers, max_transfers, max_total_duration_min, constraints_json)
VALUES ('b0000001-0002-4000-8000-000000000001',
        'a0000001-0002-4000-8000-000000000001',
        5, 1, 1, 0, 4, 900,
        '{"max_first_mile_edges": 2, "max_last_mile_edges": 2, "description": "ESB: up to 2 transfers before + 1 flight + up to 2 transfers after"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, ui_x, ui_y) VALUES
    ('c0000002-0001-4000-8000-000000000001', 'a0000001-0002-4000-8000-000000000001', 'START',  1, 1,   0, 150),
    ('c0000002-0001-4000-8000-000000000002', 'a0000001-0002-4000-8000-000000000001', 'BEFORE', 0, 2, 200, 150),
    ('c0000002-0001-4000-8000-000000000003', 'a0000001-0002-4000-8000-000000000001', 'FLIGHT', 1, 1, 400, 150),
    ('c0000002-0001-4000-8000-000000000004', 'a0000001-0002-4000-8000-000000000001', 'AFTER',  0, 2, 600, 150),
    ('c0000002-0001-4000-8000-000000000005', 'a0000001-0002-4000-8000-000000000001', 'END',    1, 1, 800, 150)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json) VALUES
    ('d0000002-0001-4000-8000-000000000001', 'a0000001-0002-4000-8000-000000000001',
     'c0000002-0001-4000-8000-000000000001', 'c0000002-0001-4000-8000-000000000002', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING"]}'::jsonb),
    ('d0000002-0001-4000-8000-000000000002', 'a0000001-0002-4000-8000-000000000001',
     'c0000002-0001-4000-8000-000000000002', 'c0000002-0001-4000-8000-000000000003', 20,
     '{"allowed_modes": ["FLIGHT"]}'::jsonb),
    ('d0000002-0001-4000-8000-000000000003', 'a0000001-0002-4000-8000-000000000001',
     'c0000002-0001-4000-8000-000000000003', 'c0000002-0001-4000-8000-000000000004', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING"]}'::jsonb),
    ('d0000002-0001-4000-8000-000000000004', 'a0000001-0002-4000-8000-000000000001',
     'c0000002-0001-4000-8000-000000000004', 'c0000002-0001-4000-8000-000000000005', 10, NULL),
    -- Shortcuts
    ('d0000002-0001-4000-8000-000000000007', 'a0000001-0002-4000-8000-000000000001',
     'c0000002-0001-4000-8000-000000000001', 'c0000002-0001-4000-8000-000000000003', 5, NULL),
    ('d0000002-0001-4000-8000-000000000008', 'a0000001-0002-4000-8000-000000000001',
     'c0000002-0001-4000-8000-000000000003', 'c0000002-0001-4000-8000-000000000005', 5, NULL)
ON CONFLICT (id) DO NOTHING;


-- ═══════════════════════════════════════
-- 3. LHR — London Heathrow
--    Max 1 first-mile, 1 last-mile, 1 flight
-- ═══════════════════════════════════════
INSERT INTO journey_policy_set (id, code, scope_type, scope_key, segment, status, description, created_by)
VALUES ('a0000001-0003-4000-8000-000000000001',
        'LHR_DEFAULT', 'AIRPORT', 'LHR', 'DEFAULT', 'ACTIVE',
        'London Heathrow — max 1 first-mile transfer, 1 flight, 1 last-mile transfer',
        'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO journey_policy_constraints (id, policy_set_id, max_legs, min_flights, max_flights, min_transfers, max_transfers, max_total_duration_min, constraints_json)
VALUES ('b0000001-0003-4000-8000-000000000001',
        'a0000001-0003-4000-8000-000000000001',
        3, 1, 1, 0, 2, 720,
        '{"max_first_mile_edges": 1, "max_last_mile_edges": 1, "description": "LHR: 1 transfer before + 1 flight + 1 transfer after"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, ui_x, ui_y) VALUES
    ('c0000003-0001-4000-8000-000000000001', 'a0000001-0003-4000-8000-000000000001', 'START',  1, 1,   0, 150),
    ('c0000003-0001-4000-8000-000000000002', 'a0000001-0003-4000-8000-000000000001', 'BEFORE', 0, 1, 200, 150),
    ('c0000003-0001-4000-8000-000000000003', 'a0000001-0003-4000-8000-000000000001', 'FLIGHT', 1, 1, 400, 150),
    ('c0000003-0001-4000-8000-000000000004', 'a0000001-0003-4000-8000-000000000001', 'AFTER',  0, 1, 600, 150),
    ('c0000003-0001-4000-8000-000000000005', 'a0000001-0003-4000-8000-000000000001', 'END',    1, 1, 800, 150)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json) VALUES
    ('d0000003-0001-4000-8000-000000000001', 'a0000001-0003-4000-8000-000000000001',
     'c0000003-0001-4000-8000-000000000001', 'c0000003-0001-4000-8000-000000000002', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING"]}'::jsonb),
    ('d0000003-0001-4000-8000-000000000002', 'a0000001-0003-4000-8000-000000000001',
     'c0000003-0001-4000-8000-000000000002', 'c0000003-0001-4000-8000-000000000003', 20,
     '{"allowed_modes": ["FLIGHT"]}'::jsonb),
    ('d0000003-0001-4000-8000-000000000003', 'a0000001-0003-4000-8000-000000000001',
     'c0000003-0001-4000-8000-000000000003', 'c0000003-0001-4000-8000-000000000004', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING"]}'::jsonb),
    ('d0000003-0001-4000-8000-000000000004', 'a0000001-0003-4000-8000-000000000001',
     'c0000003-0001-4000-8000-000000000004', 'c0000003-0001-4000-8000-000000000005', 10, NULL),
    ('d0000003-0001-4000-8000-000000000005', 'a0000001-0003-4000-8000-000000000001',
     'c0000003-0001-4000-8000-000000000001', 'c0000003-0001-4000-8000-000000000003', 5, NULL),
    ('d0000003-0001-4000-8000-000000000006', 'a0000001-0003-4000-8000-000000000001',
     'c0000003-0001-4000-8000-000000000003', 'c0000003-0001-4000-8000-000000000005', 5, NULL)
ON CONFLICT (id) DO NOTHING;


-- ═══════════════════════════════════════
-- 4. BER — Berlin Brandenburg
--    Max 2 first-mile, 2 last-mile, 1 flight
-- ═══════════════════════════════════════
INSERT INTO journey_policy_set (id, code, scope_type, scope_key, segment, status, description, created_by)
VALUES ('a0000001-0004-4000-8000-000000000001',
        'BER_DEFAULT', 'AIRPORT', 'BER', 'DEFAULT', 'ACTIVE',
        'Berlin Brandenburg — max 2 first-mile transfers, 1 flight, 2 last-mile transfers',
        'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO journey_policy_constraints (id, policy_set_id, max_legs, min_flights, max_flights, min_transfers, max_transfers, max_total_duration_min, constraints_json)
VALUES ('b0000001-0004-4000-8000-000000000001',
        'a0000001-0004-4000-8000-000000000001',
        5, 1, 1, 0, 4, 900,
        '{"max_first_mile_edges": 2, "max_last_mile_edges": 2, "description": "BER: up to 2 transfers before + 1 flight + up to 2 transfers after"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, ui_x, ui_y) VALUES
    ('c0000004-0001-4000-8000-000000000001', 'a0000001-0004-4000-8000-000000000001', 'START',  1, 1,   0, 150),
    ('c0000004-0001-4000-8000-000000000002', 'a0000001-0004-4000-8000-000000000001', 'BEFORE', 0, 2, 200, 150),
    ('c0000004-0001-4000-8000-000000000003', 'a0000001-0004-4000-8000-000000000001', 'FLIGHT', 1, 1, 400, 150),
    ('c0000004-0001-4000-8000-000000000004', 'a0000001-0004-4000-8000-000000000001', 'AFTER',  0, 2, 600, 150),
    ('c0000004-0001-4000-8000-000000000005', 'a0000001-0004-4000-8000-000000000001', 'END',    1, 1, 800, 150)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json) VALUES
    ('d0000004-0001-4000-8000-000000000001', 'a0000001-0004-4000-8000-000000000001',
     'c0000004-0001-4000-8000-000000000001', 'c0000004-0001-4000-8000-000000000002', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING","TRAIN"]}'::jsonb),
    ('d0000004-0001-4000-8000-000000000002', 'a0000001-0004-4000-8000-000000000001',
     'c0000004-0001-4000-8000-000000000002', 'c0000004-0001-4000-8000-000000000003', 20,
     '{"allowed_modes": ["FLIGHT"]}'::jsonb),
    ('d0000004-0001-4000-8000-000000000003', 'a0000001-0004-4000-8000-000000000001',
     'c0000004-0001-4000-8000-000000000003', 'c0000004-0001-4000-8000-000000000004', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING","TRAIN"]}'::jsonb),
    ('d0000004-0001-4000-8000-000000000004', 'a0000001-0004-4000-8000-000000000001',
     'c0000004-0001-4000-8000-000000000004', 'c0000004-0001-4000-8000-000000000005', 10, NULL),
    -- Shortcuts
    ('d0000004-0001-4000-8000-000000000007', 'a0000001-0004-4000-8000-000000000001',
     'c0000004-0001-4000-8000-000000000001', 'c0000004-0001-4000-8000-000000000003', 5, NULL),
    ('d0000004-0001-4000-8000-000000000008', 'a0000001-0004-4000-8000-000000000001',
     'c0000004-0001-4000-8000-000000000003', 'c0000004-0001-4000-8000-000000000005', 5, NULL)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════
-- 5. IST — Istanbul Airport
--    Max 2 first-mile, 2 last-mile, 1 flight
-- ═══════════════════════════════════════
INSERT INTO journey_policy_set (id, code, scope_type, scope_key, segment, status, description, created_by)
VALUES ('a0000001-0005-4000-8000-000000000001',
        'IST_DEFAULT', 'AIRPORT', 'IST', 'DEFAULT', 'ACTIVE',
        'Istanbul Airport — max 2 first-mile transfers, 1 flight, 2 last-mile transfers',
        'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO journey_policy_constraints (id, policy_set_id, max_legs, min_flights, max_flights, min_transfers, max_transfers, max_total_duration_min, constraints_json)
VALUES ('b0000001-0005-4000-8000-000000000001',
        'a0000001-0005-4000-8000-000000000001',
        5, 1, 1, 0, 4, 900,
        '{"max_first_mile_edges": 2, "max_last_mile_edges": 2, "description": "IST: up to 2 transfers before + 1 flight + up to 2 transfers after"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, ui_x, ui_y) VALUES
    ('c0000005-0001-4000-8000-000000000001', 'a0000001-0005-4000-8000-000000000001', 'START',  1, 1,   0, 150),
    ('c0000005-0001-4000-8000-000000000002', 'a0000001-0005-4000-8000-000000000001', 'BEFORE', 0, 2, 200, 150),
    ('c0000005-0001-4000-8000-000000000003', 'a0000001-0005-4000-8000-000000000001', 'FLIGHT', 1, 1, 400, 150),
    ('c0000005-0001-4000-8000-000000000004', 'a0000001-0005-4000-8000-000000000001', 'AFTER',  0, 2, 600, 150),
    ('c0000005-0001-4000-8000-000000000005', 'a0000001-0005-4000-8000-000000000001', 'END',    1, 1, 800, 150)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json) VALUES
    ('d0000005-0001-4000-8000-000000000001', 'a0000001-0005-4000-8000-000000000001',
     'c0000005-0001-4000-8000-000000000001', 'c0000005-0001-4000-8000-000000000002', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING","TRAIN"]}'::jsonb),
    ('d0000005-0001-4000-8000-000000000002', 'a0000001-0005-4000-8000-000000000001',
     'c0000005-0001-4000-8000-000000000002', 'c0000005-0001-4000-8000-000000000003', 20,
     '{"allowed_modes": ["FLIGHT"]}'::jsonb),
    ('d0000005-0001-4000-8000-000000000003', 'a0000001-0005-4000-8000-000000000001',
     'c0000005-0001-4000-8000-000000000003', 'c0000005-0001-4000-8000-000000000004', 10,
     '{"allowed_modes": ["TAXI","UBER","METRO","BUS","WALKING","TRAIN"]}'::jsonb),
    ('d0000005-0001-4000-8000-000000000004', 'a0000001-0005-4000-8000-000000000001',
     'c0000005-0001-4000-8000-000000000004', 'c0000005-0001-4000-8000-000000000005', 10, NULL),
    -- Shortcuts
    ('d0000005-0001-4000-8000-000000000007', 'a0000001-0005-4000-8000-000000000001',
     'c0000005-0001-4000-8000-000000000001', 'c0000005-0001-4000-8000-000000000003', 5, NULL),
    ('d0000005-0001-4000-8000-000000000008', 'a0000001-0005-4000-8000-000000000001',
     'c0000005-0001-4000-8000-000000000003', 'c0000005-0001-4000-8000-000000000005', 5, NULL)
ON CONFLICT (id) DO NOTHING;


-- ═══════════════════════════════════════
-- 6. GLOBAL fallback policy (catch-all)
-- ═══════════════════════════════════════
INSERT INTO journey_policy_set (id, code, scope_type, scope_key, segment, status, description, created_by)
VALUES ('a0000001-0000-4000-8000-000000000001',
        'GLOBAL_DEFAULT', 'GLOBAL', '*', 'DEFAULT', 'ACTIVE',
        'Global fallback — max 5 legs, 4 transfers',
        'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO journey_policy_constraints (id, policy_set_id, max_legs, min_flights, max_flights, min_transfers, max_transfers, max_total_duration_min, constraints_json)
VALUES ('b0000001-0000-4000-8000-000000000001',
        'a0000001-0000-4000-8000-000000000001',
        5, 1, 2, 0, 4, 1440,
        '{"max_first_mile_edges": 2, "max_last_mile_edges": 2, "description": "Global fallback policy"}'::jsonb)
ON CONFLICT (id) DO NOTHING;
