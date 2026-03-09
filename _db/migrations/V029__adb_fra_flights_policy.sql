-- ============================================================
-- V029: ADB ↔ FRA flight seed + ADB policy
-- İzmir Adnan Menderes ↔ Frankfurt flights (TK + XQ)
-- All valid March 2026, operating_days_mask=127 (daily)
-- TR=UTC+3, DE=UTC+1 → net offset: dep + duration - 2h = arr (local)
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- 1. Ensure ADB + FRA locations exist
-- ═══════════════════════════════════════
INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
SELECT gen_random_uuid(), 'ADB', 'LTBJ', 'İzmir Adnan Menderes Airport', 'AIRPORT', 'İzmir', 'TR', 'TR-35',
       38.2924, 27.1567, 'Europe/Istanbul', 'INTERNAL', 'ADB_SEED', TRUE, 90
WHERE NOT EXISTS (SELECT 1 FROM location WHERE iata_code = 'ADB');

INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
SELECT gen_random_uuid(), 'FRA', 'EDDF', 'Frankfurt am Main Airport', 'AIRPORT', 'Frankfurt', 'DE', 'DE-HE',
       50.0379, 8.5622, 'Europe/Berlin', 'INTERNAL', 'FRA_SEED', TRUE, 95
WHERE NOT EXISTS (SELECT 1 FROM location WHERE iata_code = 'FRA');

-- ═══════════════════════════════════════
-- 2. Ensure TFL provider exists (for London GTFS)
-- ═══════════════════════════════════════
INSERT INTO provider (id, code, name, type, country_iso_code, is_active, version, created_date, last_modified_date, deleted)
SELECT gen_random_uuid(), 'TFL', 'Transport for London', 'METRO_OPERATOR', 'GB', TRUE, 0, NOW(), NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM provider WHERE code = 'TFL');


-- ═══════════════════════════════════════
-- 3. Edges + Trips
-- ═══════════════════════════════════════
DO $$
DECLARE
    v_fm UUID;   -- FLIGHT mode
    v_tk UUID;   -- Turkish Airlines
    v_xq UUID;   -- SunExpress
    v_lh UUID;   -- Lufthansa
    v_adb UUID;  v_fra UUID;
BEGIN
    SELECT id INTO v_fm  FROM transport_mode WHERE code = 'FLIGHT';
    SELECT id INTO v_tk  FROM provider WHERE code = 'TK';
    SELECT id INTO v_xq  FROM provider WHERE code = 'XQ';
    SELECT id INTO v_lh  FROM provider WHERE code = 'LH';
    SELECT id INTO v_adb FROM location WHERE iata_code = 'ADB' LIMIT 1;
    SELECT id INTO v_fra FROM location WHERE iata_code = 'FRA' LIMIT 1;

    IF v_adb IS NULL THEN RAISE EXCEPTION 'ADB not found'; END IF;
    IF v_fra IS NULL THEN RAISE EXCEPTION 'FRA not found'; END IF;

    -- ═══════════════════════════════════════════════════════
    -- EDGES: ADB ↔ FRA (6 routes)
    -- ═══════════════════════════════════════════════════════
    INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id, provider_id, schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams) VALUES
        -- ADB → FRA
        ('f0000000-0000-4000-8000-000000000030', v_adb, v_fra, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 195, 2150000, 245000),
        ('f0000000-0000-4000-8000-000000000031', v_adb, v_fra, v_fm, v_xq, 'FIXED', 127, 'ACTIVE', 'MANUAL', 180, 2150000, 230000),
        ('f0000000-0000-4000-8000-000000000032', v_adb, v_fra, v_fm, v_lh, 'FIXED', 127, 'ACTIVE', 'MANUAL', 190, 2150000, 240000),
        -- FRA → ADB
        ('f0000000-0000-4000-8000-000000000033', v_fra, v_adb, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 190, 2150000, 245000),
        ('f0000000-0000-4000-8000-000000000034', v_fra, v_adb, v_fm, v_xq, 'FIXED', 127, 'ACTIVE', 'MANUAL', 175, 2150000, 230000),
        ('f0000000-0000-4000-8000-000000000035', v_fra, v_adb, v_fm, v_lh, 'FIXED', 127, 'ACTIVE', 'MANUAL', 185, 2150000, 240000)
    ON CONFLICT (id) DO NOTHING;

    -- ═══════════════════════════════════════════════════════
    -- TRIPS: ADB → FRA  (TR→DE: arr = dep + duration - 2h net)
    -- ═══════════════════════════════════════════════════════

    -- TK ADB→FRA: 195min, net arrival = dep + 1h15m
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000200', 'f0000000-0000-4000-8000-000000000030', 'TK1824', '06:30', '07:45', 127, '2026-03-01', '2026-03-31', 15000),
        ('f1000000-0000-4000-8000-000000000201', 'f0000000-0000-4000-8000-000000000030', 'TK1826', '08:30', '09:45', 127, '2026-03-01', '2026-03-31', 17000),
        ('f1000000-0000-4000-8000-000000000202', 'f0000000-0000-4000-8000-000000000030', 'TK1828', '13:00', '14:15', 127, '2026-03-01', '2026-03-31', 16000),
        ('f1000000-0000-4000-8000-000000000203', 'f0000000-0000-4000-8000-000000000030', 'TK1830', '17:30', '18:45', 127, '2026-03-01', '2026-03-31', 18000),
        ('f1000000-0000-4000-8000-000000000204', 'f0000000-0000-4000-8000-000000000030', 'TK1832', '21:00', '22:15', 127, '2026-03-01', '2026-03-31', 14000)
    ON CONFLICT (id) DO NOTHING;

    -- XQ ADB→FRA: 180min, net arrival = dep + 1h
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000205', 'f0000000-0000-4000-8000-000000000031', 'XQ517',  '07:00', '08:00', 127, '2026-03-01', '2026-03-31',  9500),
        ('f1000000-0000-4000-8000-000000000206', 'f0000000-0000-4000-8000-000000000031', 'XQ519',  '14:00', '15:00', 127, '2026-03-01', '2026-03-31',  8500),
        ('f1000000-0000-4000-8000-000000000207', 'f0000000-0000-4000-8000-000000000031', 'XQ521',  '19:30', '20:30', 127, '2026-03-01', '2026-03-31', 10000)
    ON CONFLICT (id) DO NOTHING;

    -- LH ADB→FRA: 190min, net arrival = dep + 1h10m
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000208', 'f0000000-0000-4000-8000-000000000032', 'LH1340', '09:00', '10:10', 127, '2026-03-01', '2026-03-31', 22000),
        ('f1000000-0000-4000-8000-000000000209', 'f0000000-0000-4000-8000-000000000032', 'LH1342', '15:00', '16:10', 127, '2026-03-01', '2026-03-31', 24000)
    ON CONFLICT (id) DO NOTHING;

    -- ═══════════════════════════════════════════════════════
    -- TRIPS: FRA → ADB  (DE→TR: arr = dep + duration + 2h net)
    -- ═══════════════════════════════════════════════════════

    -- TK FRA→ADB: 190min, net arrival = dep + 5h10m
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000210', 'f0000000-0000-4000-8000-000000000033', 'TK1825', '07:00', '12:10', 127, '2026-03-01', '2026-03-31', 16000),
        ('f1000000-0000-4000-8000-000000000211', 'f0000000-0000-4000-8000-000000000033', 'TK1827', '10:00', '15:10', 127, '2026-03-01', '2026-03-31', 17500),
        ('f1000000-0000-4000-8000-000000000212', 'f0000000-0000-4000-8000-000000000033', 'TK1829', '14:30', '19:40', 127, '2026-03-01', '2026-03-31', 16500),
        ('f1000000-0000-4000-8000-000000000213', 'f0000000-0000-4000-8000-000000000033', 'TK1831', '18:00', '23:10', 127, '2026-03-01', '2026-03-31', 18500)
    ON CONFLICT (id) DO NOTHING;

    -- XQ FRA→ADB: 175min, net arrival = dep + 4h55m
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000214', 'f0000000-0000-4000-8000-000000000034', 'XQ518',  '08:00', '12:55', 127, '2026-03-01', '2026-03-31', 10000),
        ('f1000000-0000-4000-8000-000000000215', 'f0000000-0000-4000-8000-000000000034', 'XQ520',  '15:00', '19:55', 127, '2026-03-01', '2026-03-31',  9000),
        ('f1000000-0000-4000-8000-000000000216', 'f0000000-0000-4000-8000-000000000034', 'XQ522',  '20:30', '01:25', 127, '2026-03-01', '2026-03-31', 10500)
    ON CONFLICT (id) DO NOTHING;

    -- LH FRA→ADB: 185min, net arrival = dep + 5h05m
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000217', 'f0000000-0000-4000-8000-000000000035', 'LH1341', '10:00', '15:05', 127, '2026-03-01', '2026-03-31', 23000),
        ('f1000000-0000-4000-8000-000000000218', 'f0000000-0000-4000-8000-000000000035', 'LH1343', '16:00', '21:05', 127, '2026-03-01', '2026-03-31', 25000)
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE '✅ V029: ADB ↔ FRA — 6 edges + 19 trips seeded';
END $$;


-- ═══════════════════════════════════════
-- 4. ADB Policy — İzmir Adnan Menderes
--    Max 2 first-mile, 1 flight, 2 last-mile
-- ═══════════════════════════════════════
INSERT INTO journey_policy_set (id, code, scope_type, scope_key, segment, status, description, created_by)
VALUES ('a0000001-0006-4000-8000-000000000001',
        'ADB_DEFAULT', 'AIRPORT', 'ADB', 'DEFAULT', 'ACTIVE',
        'İzmir ADB — max 2 first-mile transfers (İzban/tram/taxi), 1 flight, 2 last-mile transfers',
        'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO journey_policy_constraints (id, policy_set_id, max_legs, min_flights, max_flights, min_transfers, max_transfers, max_total_duration_min, constraints_json)
VALUES ('b0000001-0006-4000-8000-000000000001',
        'a0000001-0006-4000-8000-000000000001',
        5, 1, 1, 0, 4, 600,
        '{"max_first_mile_edges": 2, "max_last_mile_edges": 2, "description": "ADB: up to 2 transfers before + 1 flight + up to 2 transfers after"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, ui_x, ui_y) VALUES
    ('c0000006-0001-4000-8000-000000000001', 'a0000001-0006-4000-8000-000000000001', 'START',  1, 1,   0, 150),
    ('c0000006-0001-4000-8000-000000000002', 'a0000001-0006-4000-8000-000000000001', 'BEFORE', 0, 2, 200, 150),
    ('c0000006-0001-4000-8000-000000000003', 'a0000001-0006-4000-8000-000000000001', 'FLIGHT', 1, 1, 400, 150),
    ('c0000006-0001-4000-8000-000000000004', 'a0000001-0006-4000-8000-000000000001', 'AFTER',  0, 2, 600, 150),
    ('c0000006-0001-4000-8000-000000000005', 'a0000001-0006-4000-8000-000000000001', 'END',    1, 1, 800, 150)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json) VALUES
    ('d0000006-0001-4000-8000-000000000001', 'a0000001-0006-4000-8000-000000000001',
     'c0000006-0001-4000-8000-000000000001', 'c0000006-0001-4000-8000-000000000002', 10,
     '{"allowed_modes": ["TAXI","UBER","SUBWAY","TRAIN","BUS","WALKING","BIKE"]}'::jsonb),
    ('d0000006-0001-4000-8000-000000000002', 'a0000001-0006-4000-8000-000000000001',
     'c0000006-0001-4000-8000-000000000002', 'c0000006-0001-4000-8000-000000000003', 20,
     '{"allowed_modes": ["FLIGHT"]}'::jsonb),
    ('d0000006-0001-4000-8000-000000000003', 'a0000001-0006-4000-8000-000000000001',
     'c0000006-0001-4000-8000-000000000003', 'c0000006-0001-4000-8000-000000000004', 10,
     '{"allowed_modes": ["TAXI","UBER","SUBWAY","TRAIN","BUS","WALKING","BIKE"]}'::jsonb),
    ('d0000006-0001-4000-8000-000000000004', 'a0000001-0006-4000-8000-000000000001',
     'c0000006-0001-4000-8000-000000000004', 'c0000006-0001-4000-8000-000000000005', 10, NULL),
    -- Shortcuts: direct to flight, direct to end
    ('d0000006-0001-4000-8000-000000000005', 'a0000001-0006-4000-8000-000000000001',
     'c0000006-0001-4000-8000-000000000001', 'c0000006-0001-4000-8000-000000000003', 5,
     '{"description": "Direct airport access — no first-mile needed"}'::jsonb),
    ('d0000006-0001-4000-8000-000000000006', 'a0000001-0006-4000-8000-000000000001',
     'c0000006-0001-4000-8000-000000000003', 'c0000006-0001-4000-8000-000000000005', 5,
     '{"description": "Direct airport arrival — no last-mile needed"}'::jsonb)
ON CONFLICT (id) DO NOTHING;


-- ═══════════════════════════════════════
-- 5. FRA Policy — Frankfurt Airport
--    Max 2 first-mile, 1 flight, 2 last-mile
-- ═══════════════════════════════════════
INSERT INTO journey_policy_set (id, code, scope_type, scope_key, segment, status, description, created_by)
VALUES ('a0000001-0007-4000-8000-000000000001',
        'FRA_DEFAULT', 'AIRPORT', 'FRA', 'DEFAULT', 'ACTIVE',
        'Frankfurt FRA — max 2 first-mile transfers, 1 flight, 2 last-mile transfers (S-Bahn/taxi)',
        'SYSTEM')
ON CONFLICT (code) DO NOTHING;

INSERT INTO journey_policy_constraints (id, policy_set_id, max_legs, min_flights, max_flights, min_transfers, max_transfers, max_total_duration_min, constraints_json)
VALUES ('b0000001-0007-4000-8000-000000000001',
        'a0000001-0007-4000-8000-000000000001',
        5, 1, 1, 0, 4, 600,
        '{"max_first_mile_edges": 2, "max_last_mile_edges": 2, "description": "FRA: up to 2 transfers before + 1 flight + up to 2 transfers after"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO journey_policy_node (id, policy_set_id, node_key, min_visits, max_visits, ui_x, ui_y) VALUES
    ('c0000007-0001-4000-8000-000000000001', 'a0000001-0007-4000-8000-000000000001', 'START',  1, 1,   0, 150),
    ('c0000007-0001-4000-8000-000000000002', 'a0000001-0007-4000-8000-000000000001', 'BEFORE', 0, 2, 200, 150),
    ('c0000007-0001-4000-8000-000000000003', 'a0000001-0007-4000-8000-000000000001', 'FLIGHT', 1, 1, 400, 150),
    ('c0000007-0001-4000-8000-000000000004', 'a0000001-0007-4000-8000-000000000001', 'AFTER',  0, 2, 600, 150),
    ('c0000007-0001-4000-8000-000000000005', 'a0000001-0007-4000-8000-000000000001', 'END',    1, 1, 800, 150)
ON CONFLICT (policy_set_id, node_key) DO NOTHING;

INSERT INTO journey_policy_transition (id, policy_set_id, from_node_id, to_node_id, priority, guard_json) VALUES
    ('d0000007-0001-4000-8000-000000000001', 'a0000001-0007-4000-8000-000000000001',
     'c0000007-0001-4000-8000-000000000001', 'c0000007-0001-4000-8000-000000000002', 10,
     '{"allowed_modes": ["TAXI","UBER","SUBWAY","TRAIN","BUS","WALKING"]}'::jsonb),
    ('d0000007-0001-4000-8000-000000000002', 'a0000001-0007-4000-8000-000000000001',
     'c0000007-0001-4000-8000-000000000002', 'c0000007-0001-4000-8000-000000000003', 20,
     '{"allowed_modes": ["FLIGHT"]}'::jsonb),
    ('d0000007-0001-4000-8000-000000000003', 'a0000001-0007-4000-8000-000000000001',
     'c0000007-0001-4000-8000-000000000003', 'c0000007-0001-4000-8000-000000000004', 10,
     '{"allowed_modes": ["TAXI","UBER","SUBWAY","TRAIN","BUS","WALKING"]}'::jsonb),
    ('d0000007-0001-4000-8000-000000000004', 'a0000001-0007-4000-8000-000000000001',
     'c0000007-0001-4000-8000-000000000004', 'c0000007-0001-4000-8000-000000000005', 10, NULL),
    -- Shortcuts
    ('d0000007-0001-4000-8000-000000000005', 'a0000001-0007-4000-8000-000000000001',
     'c0000007-0001-4000-8000-000000000001', 'c0000007-0001-4000-8000-000000000003', 5, NULL),
    ('d0000007-0001-4000-8000-000000000006', 'a0000001-0007-4000-8000-000000000001',
     'c0000007-0001-4000-8000-000000000003', 'c0000007-0001-4000-8000-000000000005', 5, NULL)
ON CONFLICT (id) DO NOTHING;


-- Verify
SELECT 'ADB↔FRA Edges: ' || count(*) FROM transportation_edge WHERE id::text LIKE 'f0000000-0000-4000-8000-00000000003%';
SELECT 'ADB↔FRA Trips: ' || count(*) FROM edge_trip WHERE id::text LIKE 'f1000000-0000-4000-8000-0000000002%';
SELECT 'ADB Policy: ' || count(*) FROM journey_policy_set WHERE code = 'ADB_DEFAULT';
SELECT 'FRA Policy: ' || count(*) FROM journey_policy_set WHERE code = 'FRA_DEFAULT';
