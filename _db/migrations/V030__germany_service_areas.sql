-- ============================================================
-- V030: Rename FREE provider + Add Germany-wide service areas
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- Rename provider code: FREE → FREENOW
-- ═══════════════════════════════════════
UPDATE provider SET code = 'FREENOW' WHERE code = 'FREE';

-- V030: Add Germany-wide UBER + TAXI service areas
-- Fixes UBER costCents=0 and TAXI costCents=0 in Frankfurt/Germany
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- UBER Service Area: Germany-wide (EUR pricing)
-- ═══════════════════════════════════════
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    center_lat, center_lon, radius_m,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000030',
    (SELECT id FROM transport_mode WHERE code = 'UBER'),
    (SELECT id FROM provider WHERE code = 'UBDE'),
    'Germany Uber Zone', 'COUNTRY',
    NULL, NULL, NULL,
    'DE', NULL, TRUE,
    '{
        "max_distance_m": 60000,
        "pricing": {
            "base_fare_cents": 350,
            "per_km_cents": 180,
            "currency": "EUR",
            "min_fare_cents": 600,
            "surge_enabled": true,
            "pricing_type": "DYNAMIC"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════
-- TAXI Service Area: Germany-wide (FREE NOW - EUR pricing)
-- ═══════════════════════════════════════
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    center_lat, center_lon, radius_m,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000031',
    (SELECT id FROM transport_mode WHERE code = 'TAXI'),
    (SELECT id FROM provider WHERE code = 'FREENOW'),
    'Germany Taxi Zone', 'COUNTRY',
    NULL, NULL, NULL,
    'DE', NULL, TRUE,
    '{
        "max_distance_m": 80000,
        "pricing": {
            "base_fare_cents": 390,
            "per_km_cents": 220,
            "currency": "EUR",
            "min_fare_cents": 700,
            "pricing_type": "ESTIMATED"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;
