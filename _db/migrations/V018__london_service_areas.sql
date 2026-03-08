-- ============================================================
-- V018: Add London and global TAXI/UBER service areas
-- Ensures last-mile coverage for London and fallback global coverage
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- TAXI Service Area: London (GBP, Black Cab rates)
-- ═══════════════════════════════════════
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    center_lat, center_lon, radius_m,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000004',
    (SELECT id FROM transport_mode WHERE code = 'TAXI'),
    NULL,
    'London Taxi Zone', 'RADIUS',
    51.507400, -0.127800, 60000,
    'GB', 'London', TRUE,
    '{
        "max_distance_m": 60000,
        "pricing": {
            "base_fare_cents": 320,
            "per_km_cents": 180,
            "currency": "GBP",
            "min_fare_cents": 500,
            "pricing_type": "ESTIMATED"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════
-- UBER Service Area: London (GBP, Uber rates)
-- ═══════════════════════════════════════
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    center_lat, center_lon, radius_m,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000005',
    (SELECT id FROM transport_mode WHERE code = 'UBER'),
    NULL,
    'London Uber Zone', 'RADIUS',
    51.507400, -0.127800, 50000,
    'GB', 'London', TRUE,
    '{
        "max_distance_m": 50000,
        "pricing": {
            "base_fare_cents": 250,
            "per_km_cents": 125,
            "currency": "GBP",
            "min_fare_cents": 500,
            "surge_enabled": true,
            "pricing_type": "DYNAMIC"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;
