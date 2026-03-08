-- ============================================================
-- V024: Add UBER service area for Turkey (Ankara/İstanbul)
-- Without this, ComputedEdgeResolver returns cost=0 for UBER
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- UBER Service Area: Turkey-wide (TRY pricing)
-- ═══════════════════════════════════════
INSERT INTO transport_service_area (
    id, transport_mode_id, provider_id, name, area_type,
    country_iso_code, city, is_active, config_json
) VALUES (
    'e0000001-0000-4000-8000-000000000007',
    (SELECT id FROM transport_mode WHERE code = 'UBER'),
    NULL,
    'Turkey Uber Zone', 'COUNTRY',
    'TR', NULL, TRUE,
    '{
        "max_distance_m": 80000,
        "pricing": {
            "base_fare_cents": 4000,
            "per_km_cents": 2500,
            "currency": "TRY",
            "min_fare_cents": 8000,
            "surge_enabled": true,
            "pricing_type": "DYNAMIC"
        }
    }'::jsonb
) ON CONFLICT (id) DO NOTHING;
