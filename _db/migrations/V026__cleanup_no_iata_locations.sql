-- ============================================================
-- V026: Remove locations without IATA codes
-- Only airports with valid IATA codes are useful for routing.
-- Small airstrips, heliports, seaplane bases without IATA → delete.
-- ============================================================
SET search_path TO intermodal, public;

-- Step 1: Delete airport_profile records for locations without IATA
DELETE FROM airport_profile
WHERE location_id IN (
    SELECT id FROM location
    WHERE iata_code IS NULL OR TRIM(iata_code) = ''
);

-- Step 2: Delete locations without IATA code that are NOT referenced by edges
DELETE FROM location
WHERE (iata_code IS NULL OR TRIM(iata_code) = '')
  AND id NOT IN (
    SELECT origin_location_id FROM transportation_edge
    UNION
    SELECT destination_location_id FROM transportation_edge
  );
