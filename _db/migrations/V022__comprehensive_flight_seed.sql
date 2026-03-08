-- ============================================================
-- V022: Comprehensive flight seed — IST, SAW, ESB, BER, LHR
-- Adds Berlin Brandenburg (BER) + 15 new edges + 62 new trips
-- All operating_days_mask=127 (daily), valid March 2026 – Oct 2026
-- Timezone math: TR=UTC+3, DE=UTC+1(CET), UK=UTC+0(GMT)
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- 1. Ensure BER location exists
-- ═══════════════════════════════════════
INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
SELECT gen_random_uuid(), 'BER', 'EDDB', 'Berlin Brandenburg Airport', 'AIRPORT', 'Berlin', 'DE', 'DE-BE',
       52.366667, 13.503333, 'Europe/Berlin', 'INTERNAL', 'BER_SEED', TRUE, 95
WHERE NOT EXISTS (SELECT 1 FROM location WHERE iata_code = 'BER');

-- Also ensure London city node for last-mile
INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
SELECT gen_random_uuid(), NULL, NULL, 'Berlin Hauptbahnhof', 'STATION', 'Berlin', 'DE', 'DE-BE',
       52.525000, 13.369444, 'Europe/Berlin', 'INTERNAL', 'BER_HBF_SEED', TRUE, 70
WHERE NOT EXISTS (SELECT 1 FROM location WHERE source_pk = 'BER_HBF_SEED');


-- ═══════════════════════════════════════
-- 2. Edges + Trips via DO $$ block
-- ═══════════════════════════════════════
DO $$
DECLARE
    v_fm UUID;   -- FLIGHT mode
    v_tk UUID;   -- Turkish Airlines
    v_pc UUID;   -- Pegasus
    v_ist UUID;  v_saw UUID;  v_esb UUID;
    v_lhr UUID;  v_lgw UUID;  v_ltn UUID;
    v_ber UUID;
BEGIN
    SELECT id INTO v_fm  FROM transport_mode WHERE code = 'FLIGHT';
    SELECT id INTO v_tk  FROM provider WHERE code = 'TK';
    SELECT id INTO v_pc  FROM provider WHERE code = 'PC';
    SELECT id INTO v_ist FROM location WHERE iata_code = 'IST' LIMIT 1;
    SELECT id INTO v_saw FROM location WHERE iata_code = 'SAW' LIMIT 1;
    SELECT id INTO v_esb FROM location WHERE iata_code = 'ESB' LIMIT 1;
    SELECT id INTO v_lhr FROM location WHERE iata_code = 'LHR' LIMIT 1;
    SELECT id INTO v_lgw FROM location WHERE iata_code = 'LGW' LIMIT 1;
    SELECT id INTO v_ltn FROM location WHERE iata_code = 'LTN' LIMIT 1;
    SELECT id INTO v_ber FROM location WHERE iata_code = 'BER' LIMIT 1;

    IF v_ber IS NULL THEN RAISE EXCEPTION 'BER not found'; END IF;

    -- ═══════════════════════════════════════════════════════
    -- EDGES (15 new routes)
    -- ═══════════════════════════════════════════════════════
    INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id, provider_id, schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams) VALUES
        -- Turkey → Berlin
        ('f0000000-0000-4000-8000-000000000014', v_ist, v_ber, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 180, 1750000, 170000),
        ('f0000000-0000-4000-8000-000000000015', v_saw, v_ber, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL', 185, 1780000, 175000),
        ('f0000000-0000-4000-8000-000000000016', v_esb, v_ber, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 195, 1900000, 185000),
        -- Berlin → Turkey
        ('f0000000-0000-4000-8000-000000000017', v_ber, v_ist, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 175, 1750000, 170000),
        ('f0000000-0000-4000-8000-000000000018', v_ber, v_saw, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL', 180, 1780000, 175000),
        ('f0000000-0000-4000-8000-000000000019', v_ber, v_esb, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 190, 1900000, 185000),
        -- Berlin ↔ London
        ('f0000000-0000-4000-8000-000000000020', v_ber, v_lhr, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 110,  930000,  95000),
        ('f0000000-0000-4000-8000-000000000021', v_lhr, v_ber, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 105,  930000,  95000),
        ('f0000000-0000-4000-8000-000000000022', v_ber, v_lgw, v_fm, v_pc, 'FIXED', 127, 'ACTIVE', 'MANUAL', 115,  940000,  98000),
        -- London → Turkey (returns)
        ('f0000000-0000-4000-8000-000000000023', v_lhr, v_ist, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 240, 2980000, 282000),
        ('f0000000-0000-4000-8000-000000000024', v_lhr, v_esb, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 245, 2860000, 278000),
        -- Turkey domestic (new routes)
        ('f0000000-0000-4000-8000-000000000025', v_ist, v_esb, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL',  75,  360000,  65000),
        ('f0000000-0000-4000-8000-000000000026', v_esb, v_ist, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL',  75,  360000,  65000),
        ('f0000000-0000-4000-8000-000000000027', v_esb, v_saw, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL',  75,  390000,  67000),
        -- IST → LTN
        ('f0000000-0000-4000-8000-000000000028', v_ist, v_ltn, v_fm, v_tk, 'FIXED', 127, 'ACTIVE', 'MANUAL', 220, 2960000, 280000)
    ON CONFLICT (id) DO NOTHING;


    -- ═══════════════════════════════════════════════════════
    -- TRIPS — 62 new departures
    -- Timezone net offset applied to departure for local arrival:
    --   TR→DE: duration - 2h    DE→TR: duration + 2h
    --   TR→UK: duration - 3h    UK→TR: duration + 3h
    --   DE→UK: duration - 1h    UK→DE: duration + 1h
    --   TR domestic: duration (same tz)
    -- ═══════════════════════════════════════════════════════

    -- ── IST → BER (TK) — 180min, net +1h ──────────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000040', 'f0000000-0000-4000-8000-000000000014', 'TK1721', '06:00', '07:00', 127, '2026-03-01', '2026-10-31', 15500),
        ('f1000000-0000-4000-8000-000000000041', 'f0000000-0000-4000-8000-000000000014', 'TK1723', '09:00', '10:00', 127, '2026-03-01', '2026-10-31', 17000),
        ('f1000000-0000-4000-8000-000000000042', 'f0000000-0000-4000-8000-000000000014', 'TK1725', '13:00', '14:00', 127, '2026-03-01', '2026-10-31', 16500),
        ('f1000000-0000-4000-8000-000000000043', 'f0000000-0000-4000-8000-000000000014', 'TK1727', '17:00', '18:00', 127, '2026-03-01', '2026-10-31', 18000),
        ('f1000000-0000-4000-8000-000000000044', 'f0000000-0000-4000-8000-000000000014', 'TK1729', '21:30', '22:30', 127, '2026-03-01', '2026-10-31', 14000)
    ON CONFLICT (id) DO NOTHING;

    -- ── SAW → BER (PC) — 185min, net +1h05m ──────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000045', 'f0000000-0000-4000-8000-000000000015', 'PC1261', '05:30', '06:35', 127, '2026-03-01', '2026-10-31',  7500),
        ('f1000000-0000-4000-8000-000000000046', 'f0000000-0000-4000-8000-000000000015', 'PC1263', '10:00', '11:05', 127, '2026-03-01', '2026-10-31',  8900),
        ('f1000000-0000-4000-8000-000000000047', 'f0000000-0000-4000-8000-000000000015', 'PC1265', '15:30', '16:35', 127, '2026-03-01', '2026-10-31',  8200),
        ('f1000000-0000-4000-8000-000000000048', 'f0000000-0000-4000-8000-000000000015', 'PC1267', '20:00', '21:05', 127, '2026-03-01', '2026-10-31',  7000)
    ON CONFLICT (id) DO NOTHING;

    -- ── ESB → BER (TK) — 195min, net +1h15m ──────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000049', 'f0000000-0000-4000-8000-000000000016', 'TK1741', '07:30', '08:45', 127, '2026-03-01', '2026-10-31', 16000),
        ('f1000000-0000-4000-8000-000000000050', 'f0000000-0000-4000-8000-000000000016', 'TK1743', '13:30', '14:45', 127, '2026-03-01', '2026-10-31', 17500),
        ('f1000000-0000-4000-8000-000000000051', 'f0000000-0000-4000-8000-000000000016', 'TK1745', '19:00', '20:15', 127, '2026-03-01', '2026-10-31', 15000)
    ON CONFLICT (id) DO NOTHING;

    -- ── BER → IST (TK) — 175min, net +4h55m ──────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000052', 'f0000000-0000-4000-8000-000000000017', 'TK1722', '06:00', '10:55', 127, '2026-03-01', '2026-10-31', 16000),
        ('f1000000-0000-4000-8000-000000000053', 'f0000000-0000-4000-8000-000000000017', 'TK1724', '08:30', '13:25', 127, '2026-03-01', '2026-10-31', 17500),
        ('f1000000-0000-4000-8000-000000000054', 'f0000000-0000-4000-8000-000000000017', 'TK1726', '12:00', '16:55', 127, '2026-03-01', '2026-10-31', 16500),
        ('f1000000-0000-4000-8000-000000000055', 'f0000000-0000-4000-8000-000000000017', 'TK1728', '16:00', '20:55', 127, '2026-03-01', '2026-10-31', 18500),
        ('f1000000-0000-4000-8000-000000000056', 'f0000000-0000-4000-8000-000000000017', 'TK1730', '20:30', '01:25', 127, '2026-03-01', '2026-10-31', 14500)
    ON CONFLICT (id) DO NOTHING;

    -- ── BER → SAW (PC) — 180min, net +5h ─────────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000057', 'f0000000-0000-4000-8000-000000000018', 'PC1262', '07:00', '12:00', 127, '2026-03-01', '2026-10-31',  8000),
        ('f1000000-0000-4000-8000-000000000058', 'f0000000-0000-4000-8000-000000000018', 'PC1264', '11:00', '16:00', 127, '2026-03-01', '2026-10-31',  9500),
        ('f1000000-0000-4000-8000-000000000059', 'f0000000-0000-4000-8000-000000000018', 'PC1266', '16:30', '21:30', 127, '2026-03-01', '2026-10-31',  8800),
        ('f1000000-0000-4000-8000-000000000060', 'f0000000-0000-4000-8000-000000000018', 'PC1268', '21:00', '02:00', 127, '2026-03-01', '2026-10-31',  7200)
    ON CONFLICT (id) DO NOTHING;

    -- ── BER → ESB (TK) — 190min, net +5h10m ──────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000061', 'f0000000-0000-4000-8000-000000000019', 'TK1742', '08:00', '13:10', 127, '2026-03-01', '2026-10-31', 16500),
        ('f1000000-0000-4000-8000-000000000062', 'f0000000-0000-4000-8000-000000000019', 'TK1744', '14:00', '19:10', 127, '2026-03-01', '2026-10-31', 18000),
        ('f1000000-0000-4000-8000-000000000063', 'f0000000-0000-4000-8000-000000000019', 'TK1746', '20:00', '01:10', 127, '2026-03-01', '2026-10-31', 15500)
    ON CONFLICT (id) DO NOTHING;

    -- ── BER → LHR (TK) — 110min, net +0h50m ─────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000064', 'f0000000-0000-4000-8000-000000000020', 'TK8801', '06:30', '07:20', 127, '2026-03-01', '2026-10-31', 12000),
        ('f1000000-0000-4000-8000-000000000065', 'f0000000-0000-4000-8000-000000000020', 'TK8803', '10:00', '10:50', 127, '2026-03-01', '2026-10-31', 13500),
        ('f1000000-0000-4000-8000-000000000066', 'f0000000-0000-4000-8000-000000000020', 'TK8805', '14:30', '15:20', 127, '2026-03-01', '2026-10-31', 14000),
        ('f1000000-0000-4000-8000-000000000067', 'f0000000-0000-4000-8000-000000000020', 'TK8807', '19:00', '19:50', 127, '2026-03-01', '2026-10-31', 11500)
    ON CONFLICT (id) DO NOTHING;

    -- ── LHR → BER (TK) — 105min, net +2h45m ─────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000068', 'f0000000-0000-4000-8000-000000000021', 'TK8802', '07:00', '09:45', 127, '2026-03-01', '2026-10-31', 12500),
        ('f1000000-0000-4000-8000-000000000069', 'f0000000-0000-4000-8000-000000000021', 'TK8804', '11:30', '14:15', 127, '2026-03-01', '2026-10-31', 14000),
        ('f1000000-0000-4000-8000-000000000070', 'f0000000-0000-4000-8000-000000000021', 'TK8806', '16:00', '18:45', 127, '2026-03-01', '2026-10-31', 13500),
        ('f1000000-0000-4000-8000-000000000071', 'f0000000-0000-4000-8000-000000000021', 'TK8808', '20:30', '23:15', 127, '2026-03-01', '2026-10-31', 11000)
    ON CONFLICT (id) DO NOTHING;

    -- ── BER → LGW (PC) — 115min, net +0h55m ─────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000072', 'f0000000-0000-4000-8000-000000000022', 'PC5501', '08:00', '08:55', 127, '2026-03-01', '2026-10-31',  6500),
        ('f1000000-0000-4000-8000-000000000073', 'f0000000-0000-4000-8000-000000000022', 'PC5503', '17:00', '17:55', 127, '2026-03-01', '2026-10-31',  7000)
    ON CONFLICT (id) DO NOTHING;

    -- ── LHR → IST (TK) — 240min, net +7h ────────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000074', 'f0000000-0000-4000-8000-000000000023', 'TK1982', '06:00', '13:00', 127, '2026-03-01', '2026-10-31', 23000),
        ('f1000000-0000-4000-8000-000000000075', 'f0000000-0000-4000-8000-000000000023', 'TK1984', '09:30', '16:30', 127, '2026-03-01', '2026-10-31', 25000),
        ('f1000000-0000-4000-8000-000000000076', 'f0000000-0000-4000-8000-000000000023', 'TK1986', '13:00', '20:00', 127, '2026-03-01', '2026-10-31', 24000),
        ('f1000000-0000-4000-8000-000000000077', 'f0000000-0000-4000-8000-000000000023', 'TK1990', '17:00', '00:00', 127, '2026-03-01', '2026-10-31', 22000),
        ('f1000000-0000-4000-8000-000000000078', 'f0000000-0000-4000-8000-000000000023', 'TK1992', '21:00', '04:00', 127, '2026-03-01', '2026-10-31', 20000)
    ON CONFLICT (id) DO NOTHING;

    -- ── LHR → ESB (TK) — 245min, net +7h05m ─────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000079', 'f0000000-0000-4000-8000-000000000024', 'TK1961', '08:00', '15:05', 127, '2026-03-01', '2026-10-31', 21000),
        ('f1000000-0000-4000-8000-000000000080', 'f0000000-0000-4000-8000-000000000024', 'TK1963', '14:00', '21:05', 127, '2026-03-01', '2026-10-31', 19500),
        ('f1000000-0000-4000-8000-000000000081', 'f0000000-0000-4000-8000-000000000024', 'TK1965', '20:00', '03:05', 127, '2026-03-01', '2026-10-31', 17500)
    ON CONFLICT (id) DO NOTHING;

    -- ── IST → ESB (TK domestic) — 75min ──────────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000082', 'f0000000-0000-4000-8000-000000000025', 'TK2200', '06:30', '07:45', 127, '2026-03-01', '2026-10-31', 4200),
        ('f1000000-0000-4000-8000-000000000083', 'f0000000-0000-4000-8000-000000000025', 'TK2202', '09:00', '10:15', 127, '2026-03-01', '2026-10-31', 4800),
        ('f1000000-0000-4000-8000-000000000084', 'f0000000-0000-4000-8000-000000000025', 'TK2204', '12:00', '13:15', 127, '2026-03-01', '2026-10-31', 4500),
        ('f1000000-0000-4000-8000-000000000085', 'f0000000-0000-4000-8000-000000000025', 'TK2206', '16:00', '17:15', 127, '2026-03-01', '2026-10-31', 5000),
        ('f1000000-0000-4000-8000-000000000086', 'f0000000-0000-4000-8000-000000000025', 'TK2208', '20:00', '21:15', 127, '2026-03-01', '2026-10-31', 3800)
    ON CONFLICT (id) DO NOTHING;

    -- ── ESB → IST (TK domestic) — 75min ──────────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000087', 'f0000000-0000-4000-8000-000000000026', 'TK2201', '07:00', '08:15', 127, '2026-03-01', '2026-10-31', 4300),
        ('f1000000-0000-4000-8000-000000000088', 'f0000000-0000-4000-8000-000000000026', 'TK2203', '10:00', '11:15', 127, '2026-03-01', '2026-10-31', 4900),
        ('f1000000-0000-4000-8000-000000000089', 'f0000000-0000-4000-8000-000000000026', 'TK2205', '14:00', '15:15', 127, '2026-03-01', '2026-10-31', 4600),
        ('f1000000-0000-4000-8000-000000000090', 'f0000000-0000-4000-8000-000000000026', 'TK2207', '17:30', '18:45', 127, '2026-03-01', '2026-10-31', 5100),
        ('f1000000-0000-4000-8000-000000000091', 'f0000000-0000-4000-8000-000000000026', 'TK2209', '21:00', '22:15', 127, '2026-03-01', '2026-10-31', 3900)
    ON CONFLICT (id) DO NOTHING;

    -- ── ESB → SAW (TK domestic) — 75min ──────────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000092', 'f0000000-0000-4000-8000-000000000027', 'TK2130', '06:00', '07:15', 127, '2026-03-01', '2026-10-31', 4400),
        ('f1000000-0000-4000-8000-000000000093', 'f0000000-0000-4000-8000-000000000027', 'TK2132', '12:00', '13:15', 127, '2026-03-01', '2026-10-31', 4800),
        ('f1000000-0000-4000-8000-000000000094', 'f0000000-0000-4000-8000-000000000027', 'TK2134', '18:00', '19:15', 127, '2026-03-01', '2026-10-31', 4600)
    ON CONFLICT (id) DO NOTHING;

    -- ── IST → LTN (TK) — 220min, net +0h40m ─────────
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000095', 'f0000000-0000-4000-8000-000000000028', 'TK1951', '07:00', '07:40', 127, '2026-03-01', '2026-10-31', 17000),
        ('f1000000-0000-4000-8000-000000000096', 'f0000000-0000-4000-8000-000000000028', 'TK1953', '16:00', '16:40', 127, '2026-03-01', '2026-10-31', 15500)
    ON CONFLICT (id) DO NOTHING;

    -- ── Extra: IST → LGW (TK, existing edge 010) — 235min, net +0h55m ──
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000097', 'f0000000-0000-4000-8000-000000000010', 'TK1998', '14:00', '14:55', 127, '2026-03-01', '2026-10-31', 19000),
        ('f1000000-0000-4000-8000-000000000098', 'f0000000-0000-4000-8000-000000000010', 'TK1996', '22:00', '22:55', 127, '2026-03-01', '2026-10-31', 17000)
    ON CONFLICT (id) DO NOTHING;

    -- ── Extra: LHR → BER (TK, edge 021) — 3 more departures ──
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000099', 'f0000000-0000-4000-8000-000000000021', 'TK8810', '08:30', '11:15', 127, '2026-03-01', '2026-10-31', 13000),
        ('f1000000-0000-4000-8000-000000000100', 'f0000000-0000-4000-8000-000000000021', 'TK8812', '14:00', '16:45', 127, '2026-03-01', '2026-10-31', 14500),
        ('f1000000-0000-4000-8000-000000000101', 'f0000000-0000-4000-8000-000000000021', 'TK8814', '18:00', '20:45', 127, '2026-03-01', '2026-10-31', 12000)
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE '✅ V022: 15 edges + 62 trips seeded (total 101+ trips)';
END $$;

-- Verify totals
SELECT 'Total Edges: ' || count(*) FROM transportation_edge WHERE source = 'MANUAL';
SELECT 'Total Trips: ' || count(*) FROM edge_trip WHERE id::text LIKE 'f1000000%';
