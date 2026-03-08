-- ============================================================
-- V017: Seed transport service areas for scenario testing
-- Binds TAXI/UBER modes to specific geographic zones
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- TAXI Service Areas (Türkiye)
-- ═══════════════════════════════════════

-- ESB Ankara: BiTaksi, 60km radius
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    center_lat, center_lon, radius_m,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000001',
    (SELECT id FROM transport_mode WHERE code = 'TAXI'),
    (SELECT id FROM provider WHERE code = 'BTXI'),
    'Esenboğa Taxi Zone', 'RADIUS',
    40.128100, 32.995100, 60000,
    'TR', 'Ankara', TRUE,
    '{
        "max_distance_m": 60000,
        "pricing": {
            "base_fare_cents": 5000,
            "per_km_cents": 3500,
            "currency": "TRY",
            "min_fare_cents": 10000,
            "night_multiplier": 1.5,
            "pricing_type": "ESTIMATED"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;

-- IST/SAW İstanbul: iTaksi, 80km radius
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    center_lat, center_lon, radius_m,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000002',
    (SELECT id FROM transport_mode WHERE code = 'TAXI'),
    (SELECT id FROM provider WHERE code = 'ITXI'),
    'İstanbul Taxi Zone', 'RADIUS',
    41.010000, 28.980000, 80000,
    'TR', 'İstanbul', TRUE,
    '{
        "max_distance_m": 80000,
        "pricing": {
            "base_fare_cents": 3500,
            "per_km_cents": 4000,
            "currency": "TRY",
            "min_fare_cents": 12000,
            "night_multiplier": 1.5,
            "pricing_type": "ESTIMATED"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════
-- UBER Service Areas (Almanya)
-- ═══════════════════════════════════════

-- BER Berlin: Uber Germany, 40km radius
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    center_lat, center_lon, radius_m,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000003',
    (SELECT id FROM transport_mode WHERE code = 'UBER'),
    (SELECT id FROM provider WHERE code = 'UBDE'),
    'Berlin Uber Zone', 'RADIUS',
    52.520000, 13.405000, 40000,
    'DE', 'Berlin', TRUE,
    '{
        "max_distance_m": 40000,
        "pricing": {
            "base_fare_cents": 390,
            "per_km_cents": 200,
            "currency": "EUR",
            "min_fare_cents": 700,
            "surge_enabled": true,
            "pricing_type": "DYNAMIC"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;
