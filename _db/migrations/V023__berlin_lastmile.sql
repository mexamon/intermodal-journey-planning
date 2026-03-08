-- ============================================================
-- V023: Berlin last-mile connectivity
-- 1. Berlin TAXI service area (40km, EUR)
-- 2. Fix UBER edge_resolution → COMPUTED
-- 3. TRAIN edge: BER ↔ Berlin Hbf (FEX airport express)
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- 1. Berlin TAXI Service Area
-- ═══════════════════════════════════════
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    center_lat, center_lon, radius_m,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000006',
    (SELECT id FROM transport_mode WHERE code = 'TAXI'),
    NULL,
    'Berlin Taxi Zone', 'RADIUS',
    52.520000, 13.405000, 40000,
    'DE', 'Berlin', TRUE,
    '{
        "max_distance_m": 40000,
        "pricing": {
            "base_fare_cents": 390,
            "per_km_cents": 220,
            "currency": "EUR",
            "min_fare_cents": 700,
            "pricing_type": "ESTIMATED"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════
-- 2. Fix UBER edge_resolution to COMPUTED
--    ComputedEdgeResolver only processes COMPUTED modes
-- ═══════════════════════════════════════
UPDATE transport_mode
SET edge_resolution = 'COMPUTED',
    coverage_type = 'POINT_TO_POINT'
WHERE code = 'UBER';

-- ═══════════════════════════════════════
-- 3. TRAIN: BER ↔ Berlin Hbf (FEX Airport Express)
--    ~30 min, 24km, every day, 05:00–23:00 every 30min
-- ═══════════════════════════════════════
DO $$
DECLARE
    v_train UUID;
    v_ber UUID;
    v_hbf UUID;
BEGIN
    SELECT id INTO v_train FROM transport_mode WHERE code = 'TRAIN';
    SELECT id INTO v_ber   FROM location WHERE iata_code = 'BER' LIMIT 1;
    SELECT id INTO v_hbf   FROM location WHERE source_pk = 'BER_HBF_SEED' LIMIT 1;

    IF v_hbf IS NULL THEN
        RAISE NOTICE 'Berlin Hbf not found, skipping train edges';
        RETURN;
    END IF;

    -- Edge: BER → Berlin Hbf
    INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id, provider_id, schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams)
    VALUES ('f0000000-0000-4000-8000-000000000029', v_ber, v_hbf, v_train, NULL, 'FIXED', 127, 'ACTIVE', 'MANUAL', 30, 24000, 2000)
    ON CONFLICT (id) DO NOTHING;

    -- Edge: Berlin Hbf → BER
    INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id, provider_id, schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams)
    VALUES ('f0000000-0000-4000-8000-000000000030', v_hbf, v_ber, v_train, NULL, 'FIXED', 127, 'ACTIVE', 'MANUAL', 30, 24000, 2000)
    ON CONFLICT (id) DO NOTHING;

    -- Trips: BER → Berlin Hbf (FEX, every 30min from 05:00–23:00 = ~18 trips, keep top 8)
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000102', 'f0000000-0000-4000-8000-000000000029', 'FEX01', '05:00', '05:30', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000103', 'f0000000-0000-4000-8000-000000000029', 'FEX03', '07:00', '07:30', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000104', 'f0000000-0000-4000-8000-000000000029', 'FEX05', '09:00', '09:30', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000105', 'f0000000-0000-4000-8000-000000000029', 'FEX07', '12:00', '12:30', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000106', 'f0000000-0000-4000-8000-000000000029', 'FEX09', '15:00', '15:30', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000107', 'f0000000-0000-4000-8000-000000000029', 'FEX11', '18:00', '18:30', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000108', 'f0000000-0000-4000-8000-000000000029', 'FEX13', '20:00', '20:30', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000109', 'f0000000-0000-4000-8000-000000000029', 'FEX15', '22:00', '22:30', 127, '2026-03-01', '2026-10-31', 360)
    ON CONFLICT (id) DO NOTHING;

    -- Trips: Berlin Hbf → BER (return, same frequency)
    INSERT INTO edge_trip (id, edge_id, service_code, departure_time, arrival_time, operating_days_mask, valid_from, valid_to, estimated_cost_cents) VALUES
        ('f1000000-0000-4000-8000-000000000110', 'f0000000-0000-4000-8000-000000000030', 'FEX02', '04:30', '05:00', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000111', 'f0000000-0000-4000-8000-000000000030', 'FEX04', '06:30', '07:00', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000112', 'f0000000-0000-4000-8000-000000000030', 'FEX06', '08:30', '09:00', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000113', 'f0000000-0000-4000-8000-000000000030', 'FEX08', '11:30', '12:00', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000114', 'f0000000-0000-4000-8000-000000000030', 'FEX10', '14:30', '15:00', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000115', 'f0000000-0000-4000-8000-000000000030', 'FEX12', '17:30', '18:00', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000116', 'f0000000-0000-4000-8000-000000000030', 'FEX14', '19:30', '20:00', 127, '2026-03-01', '2026-10-31', 360),
        ('f1000000-0000-4000-8000-000000000117', 'f0000000-0000-4000-8000-000000000030', 'FEX16', '21:30', '22:00', 127, '2026-03-01', '2026-10-31', 360)
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE '✅ V023: Berlin TAXI zone + UBER fix + BER↔Hbf train edges';
END $$;
