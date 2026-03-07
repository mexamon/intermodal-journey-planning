-- ============================================================
-- V016: Add TAXI computed mode + set distance limits
-- TAXI = point-to-point computed edge (haversine distance, ~40km/h)
-- Also sets sensible distance limits for WALKING and BIKE
-- ============================================================
SET search_path TO intermodal, public;

-- Add TAXI as COMPUTED mode (works without API, uses haversine)
INSERT INTO transport_mode (code, name, category, coverage_type, edge_resolution, requires_stop, max_walking_access_m, default_speed_kmh, api_provider, icon, color_hex, sort_order)
VALUES ('TAXI', 'Taxi', 'GROUND_FLEX', 'POINT_TO_POINT', 'COMPUTED', FALSE, 80000, 40, NULL, 'car', '#FFC107', 9)
ON CONFLICT (code) DO UPDATE SET
    edge_resolution = 'COMPUTED',
    max_walking_access_m = 80000,
    default_speed_kmh = 40;

-- Set distance limits: WALKING max 3km, BIKE max 15km
UPDATE transport_mode SET max_walking_access_m = 3000  WHERE code = 'WALKING';
UPDATE transport_mode SET max_walking_access_m = 15000 WHERE code = 'BIKE';
