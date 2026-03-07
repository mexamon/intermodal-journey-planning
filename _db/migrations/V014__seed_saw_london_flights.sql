-- ============================================================
-- V014: Seed — SAW ↔ London Flights (dummy data)
-- Uses IATA lookups for location IDs (works with any data source)
-- Idempotent: uses NOT EXISTS + ON CONFLICT (id)
-- ============================================================
SET search_path TO intermodal, public;

-- ══════════════════════════════════════
-- 1. Ensure locations exist (only INSERT if IATA not already present)
-- ══════════════════════════════════════

INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
SELECT gen_random_uuid(), v.iata, v.icao, v.name, 'AIRPORT', v.city, v.country, v.region, v.lat, v.lon, v.tz, 'INTERNAL', v.iata || '_SEED', TRUE, v.prio
FROM (VALUES
    ('SAW', 'LTFJ', 'Sabiha Gökçen International Airport', 'Istanbul', 'TR', 'TR-34', 40.898553, 29.309219, 'Europe/Istanbul', 100),
    ('LHR', 'EGLL', 'London Heathrow Airport',              'London',   'GB', 'GB-LND', 51.470600, -0.461941, 'Europe/London',   100),
    ('LGW', 'EGKK', 'London Gatwick Airport',               'London',   'GB', 'GB-LND', 51.148056, -0.190278, 'Europe/London',    90),
    ('STN', 'EGSS', 'London Stansted Airport',               'London',   'GB', 'GB-LND', 51.884998,  0.235000, 'Europe/London',    80),
    ('IST', 'LTFM', 'Istanbul Airport',                      'Istanbul', 'TR', 'TR-34', 41.275278, 28.751944, 'Europe/Istanbul', 100),
    ('LTN', 'EGGW', 'London Luton Airport',                  'London',   'GB', 'GB-LND', 51.874722, -0.368333, 'Europe/London',    70),
    ('ESB', 'LTAC', 'Esenboğa International Airport',        'Ankara',   'TR', 'TR-06', 40.128101, 32.995098, 'Europe/Istanbul',  90),
    ('ADB', 'LTBJ', 'Adnan Menderes Airport',                'Izmir',    'TR', 'TR-35', 38.292400, 27.157000, 'Europe/Istanbul',  85)
) AS v(iata, icao, name, city, country, region, lat, lon, tz, prio)
WHERE NOT EXISTS (SELECT 1 FROM location WHERE iata_code = v.iata);


-- ══════════════════════════════════════
-- 2. Create edges + trips via DO $$ block (IATA-based ID lookups)
-- ══════════════════════════════════════

DO $$
DECLARE
    v_fm UUID;  -- FLIGHT mode
    v_tk UUID;  -- Turkish Airlines
    v_pc UUID;  -- Pegasus
    v_saw UUID; v_lhr UUID; v_lgw UUID; v_stn UUID;
    v_ist UUID; v_ltn UUID; v_esb UUID; v_adb UUID;
BEGIN
    SELECT id INTO v_fm  FROM transport_mode WHERE code = 'FLIGHT';
    SELECT id INTO v_tk  FROM provider WHERE code = 'TK';
    SELECT id INTO v_pc  FROM provider WHERE code = 'PC';
    SELECT id INTO v_saw FROM location WHERE iata_code = 'SAW' LIMIT 1;
    SELECT id INTO v_lhr FROM location WHERE iata_code = 'LHR' LIMIT 1;
    SELECT id INTO v_lgw FROM location WHERE iata_code = 'LGW' LIMIT 1;
    SELECT id INTO v_stn FROM location WHERE iata_code = 'STN' LIMIT 1;
    SELECT id INTO v_ist FROM location WHERE iata_code = 'IST' LIMIT 1;
    SELECT id INTO v_ltn FROM location WHERE iata_code = 'LTN' LIMIT 1;
    SELECT id INTO v_esb FROM location WHERE iata_code = 'ESB' LIMIT 1;
    SELECT id INTO v_adb FROM location WHERE iata_code = 'ADB' LIMIT 1;

    IF v_saw IS NULL THEN RAISE EXCEPTION 'SAW not found'; END IF;
    IF v_lhr IS NULL THEN RAISE EXCEPTION 'LHR not found'; END IF;

    RAISE NOTICE 'SAW=%, LHR=%, LGW=%, STN=%', v_saw, v_lhr, v_lgw, v_stn;

    -- ═══════════════════════════════════
    -- EDGES (13 routes)
    -- ═══════════════════════════════════
    INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id, provider_id, schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams) VALUES
        ('f0000000-0000-4000-8000-000000000001', v_saw, v_lhr, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 230, 2994000, 285000),
        ('f0000000-0000-4000-8000-000000000002', v_saw, v_lgw, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL', 240, 2976000, 290000),
        ('f0000000-0000-4000-8000-000000000003', v_saw, v_stn, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL', 245, 3010000, 295000),
        ('f0000000-0000-4000-8000-000000000004', v_saw, v_lhr, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL', 235, 2994000, 292000),
        ('f0000000-0000-4000-8000-000000000005', v_saw, v_ltn, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 225, 2960000, 280000),
        ('f0000000-0000-4000-8000-000000000006', v_lhr, v_saw, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 220, 2994000, 285000),
        ('f0000000-0000-4000-8000-000000000007', v_lgw, v_saw, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL', 230, 2976000, 290000),
        ('f0000000-0000-4000-8000-000000000008', v_stn, v_saw, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL', 235, 3010000, 295000),
        ('f0000000-0000-4000-8000-000000000009', v_ist, v_lhr, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 225, 2980000, 282000),
        ('f0000000-0000-4000-8000-000000000010', v_ist, v_lgw, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 235, 2960000, 288000),
        ('f0000000-0000-4000-8000-000000000011', v_saw, v_esb, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL',  75,  360000,  65000),
        ('f0000000-0000-4000-8000-000000000012', v_saw, v_adb, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL',  70,  330000,  60000),
        ('f0000000-0000-4000-8000-000000000013', v_esb, v_lhr, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 240, 2860000, 278000)
    ON CONFLICT (id) DO NOTHING;

    -- ═══════════════════════════════════
    -- TRIPS (39 departures, all mask=127 = every day)
    -- valid_from 2026-03-01 → valid_to 2026-10-31
    -- ═══════════════════════════════════

    -- SAW → LHR (TK) — 4 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents, attrs_json) VALUES
        ('f1000000-0000-4000-8000-000000000001', 'f0000000-0000-4000-8000-000000000001', 'TK1987', '06:30', '09:20', 127, '2026-03-01', '2026-10-31', 18500, '{"aircraft":"B737-800"}'),
        ('f1000000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000001', 'TK1971', '10:15', '13:05', 127, '2026-03-01', '2026-10-31', 21000, '{"aircraft":"A321neo"}'),
        ('f1000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000001', 'TK1975', '14:40', '17:30', 127, '2026-03-01', '2026-10-31', 19500, '{"aircraft":"B737-800"}'),
        ('f1000000-0000-4000-8000-000000000004', 'f0000000-0000-4000-8000-000000000001', 'TK1979', '19:00', '21:50', 127, '2026-03-01', '2026-10-31', 17000, '{"aircraft":"A320"}')
    ON CONFLICT (id) DO NOTHING;

    -- SAW → LGW (PC) — 3 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000005', 'f0000000-0000-4000-8000-000000000002', 'PC1171', '05:45', '08:45', 127, '2026-03-01', '2026-10-31',  8900),
        ('f1000000-0000-4000-8000-000000000006', 'f0000000-0000-4000-8000-000000000002', 'PC1173', '12:30', '15:30', 127, '2026-03-01', '2026-10-31', 10500),
        ('f1000000-0000-4000-8000-000000000007', 'f0000000-0000-4000-8000-000000000002', 'PC1175', '18:00', '21:00', 127, '2026-03-01', '2026-10-31',  9200)
    ON CONFLICT (id) DO NOTHING;

    -- SAW → STN (PC) — 3 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000008', 'f0000000-0000-4000-8000-000000000003', 'PC1191', '06:00', '09:05', 127, '2026-03-01', '2026-10-31', 7500),
        ('f1000000-0000-4000-8000-000000000009', 'f0000000-0000-4000-8000-000000000003', 'PC1193', '13:45', '16:50', 127, '2026-03-01', '2026-10-31', 8800),
        ('f1000000-0000-4000-8000-000000000010', 'f0000000-0000-4000-8000-000000000003', 'PC1195', '20:15', '23:20', 127, '2026-03-01', '2026-10-31', 7200)
    ON CONFLICT (id) DO NOTHING;

    -- SAW → LHR (PC) — 2 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000011', 'f0000000-0000-4000-8000-000000000004', 'PC1181', '07:00', '09:55', 127, '2026-03-01', '2026-10-31',  9500),
        ('f1000000-0000-4000-8000-000000000012', 'f0000000-0000-4000-8000-000000000004', 'PC1183', '16:30', '19:25', 127, '2026-03-01', '2026-10-31', 11000)
    ON CONFLICT (id) DO NOTHING;

    -- SAW → LTN (TK) — 2 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000013', 'f0000000-0000-4000-8000-000000000005', 'TK1991', '08:00', '10:45', 127, '2026-03-01', '2026-10-31', 16000),
        ('f1000000-0000-4000-8000-000000000014', 'f0000000-0000-4000-8000-000000000005', 'TK1993', '17:15', '20:00', 127, '2026-03-01', '2026-10-31', 14500)
    ON CONFLICT (id) DO NOTHING;

    -- LHR → SAW (TK return) — 3 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000015', 'f0000000-0000-4000-8000-000000000006', 'TK1988', '07:00', '13:20', 127, '2026-03-01', '2026-10-31', 19000),
        ('f1000000-0000-4000-8000-000000000016', 'f0000000-0000-4000-8000-000000000006', 'TK1972', '11:30', '17:50', 127, '2026-03-01', '2026-10-31', 22000),
        ('f1000000-0000-4000-8000-000000000017', 'f0000000-0000-4000-8000-000000000006', 'TK1976', '16:00', '22:20', 127, '2026-03-01', '2026-10-31', 18500)
    ON CONFLICT (id) DO NOTHING;

    -- LGW → SAW (PC return) — 2 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000018', 'f0000000-0000-4000-8000-000000000007', 'PC1172', '09:30', '15:30', 127, '2026-03-01', '2026-10-31', 9500),
        ('f1000000-0000-4000-8000-000000000019', 'f0000000-0000-4000-8000-000000000007', 'PC1174', '21:00', '03:00', 127, '2026-03-01', '2026-10-31', 8200)
    ON CONFLICT (id) DO NOTHING;

    -- STN → SAW (PC return) — 2 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000020', 'f0000000-0000-4000-8000-000000000008', 'PC1192', '10:00', '16:15', 127, '2026-03-01', '2026-10-31', 8000),
        ('f1000000-0000-4000-8000-000000000021', 'f0000000-0000-4000-8000-000000000008', 'PC1194', '22:00', '04:15', 127, '2026-03-01', '2026-10-31', 7000)
    ON CONFLICT (id) DO NOTHING;

    -- IST → LHR (TK) — 5 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000022', 'f0000000-0000-4000-8000-000000000009', 'TK1981', '05:00', '07:45', 127, '2026-03-01', '2026-10-31', 22000),
        ('f1000000-0000-4000-8000-000000000023', 'f0000000-0000-4000-8000-000000000009', 'TK1983', '08:30', '11:15', 127, '2026-03-01', '2026-10-31', 24500),
        ('f1000000-0000-4000-8000-000000000024', 'f0000000-0000-4000-8000-000000000009', 'TK1985', '12:00', '14:45', 127, '2026-03-01', '2026-10-31', 23000),
        ('f1000000-0000-4000-8000-000000000025', 'f0000000-0000-4000-8000-000000000009', 'TK1989', '16:30', '19:15', 127, '2026-03-01', '2026-10-31', 21500),
        ('f1000000-0000-4000-8000-000000000026', 'f0000000-0000-4000-8000-000000000009', 'TK1995', '21:00', '23:45', 127, '2026-03-01', '2026-10-31', 19000)
    ON CONFLICT (id) DO NOTHING;

    -- IST → LGW (TK) — 2 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000027', 'f0000000-0000-4000-8000-000000000010', 'TK1997', '09:00', '11:55', 127, '2026-03-01', '2026-10-31', 20000),
        ('f1000000-0000-4000-8000-000000000028', 'f0000000-0000-4000-8000-000000000010', 'TK1999', '18:00', '20:55', 127, '2026-03-01', '2026-10-31', 18000)
    ON CONFLICT (id) DO NOTHING;

    -- SAW → ESB (TK domestic) — 5 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000029', 'f0000000-0000-4000-8000-000000000011', 'TK2120', '06:00', '07:15', 127, '2026-03-01', '2026-10-31', 4500),
        ('f1000000-0000-4000-8000-000000000030', 'f0000000-0000-4000-8000-000000000011', 'TK2122', '09:30', '10:45', 127, '2026-03-01', '2026-10-31', 5000),
        ('f1000000-0000-4000-8000-000000000031', 'f0000000-0000-4000-8000-000000000011', 'TK2124', '13:00', '14:15', 127, '2026-03-01', '2026-10-31', 4800),
        ('f1000000-0000-4000-8000-000000000032', 'f0000000-0000-4000-8000-000000000011', 'TK2126', '16:30', '17:45', 127, '2026-03-01', '2026-10-31', 5200),
        ('f1000000-0000-4000-8000-000000000033', 'f0000000-0000-4000-8000-000000000011', 'TK2128', '20:00', '21:15', 127, '2026-03-01', '2026-10-31', 4200)
    ON CONFLICT (id) DO NOTHING;

    -- SAW → ADB (PC domestic) — 4 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000034', 'f0000000-0000-4000-8000-000000000012', 'PC2230', '07:00', '08:10', 127, '2026-03-01', '2026-10-31', 3500),
        ('f1000000-0000-4000-8000-000000000035', 'f0000000-0000-4000-8000-000000000012', 'PC2232', '11:00', '12:10', 127, '2026-03-01', '2026-10-31', 4000),
        ('f1000000-0000-4000-8000-000000000036', 'f0000000-0000-4000-8000-000000000012', 'PC2234', '15:00', '16:10', 127, '2026-03-01', '2026-10-31', 3800),
        ('f1000000-0000-4000-8000-000000000037', 'f0000000-0000-4000-8000-000000000012', 'PC2236', '19:30', '20:40', 127, '2026-03-01', '2026-10-31', 3200)
    ON CONFLICT (id) DO NOTHING;

    -- ESB → LHR (TK connecting) — 2 daily
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000038', 'f0000000-0000-4000-8000-000000000013', 'TK1960', '08:00', '11:40', 127, '2026-03-01', '2026-10-31', 20000),
        ('f1000000-0000-4000-8000-000000000039', 'f0000000-0000-4000-8000-000000000013', 'TK1962', '15:00', '18:40', 127, '2026-03-01', '2026-10-31', 18500)
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE '✅ 13 edges + 39 trips seeded';
END $$;

-- Verify
SELECT 'Edges: ' || count(*) FROM transportation_edge WHERE id::text LIKE 'f0000000%';
SELECT 'Trips: ' || count(*) FROM edge_trip WHERE id::text LIKE 'f1000000%';
