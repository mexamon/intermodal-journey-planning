-- ============================================================
-- V011: Seed reference data from OurAirports CSVs
-- Pipeline: CSV → raw staging tables → target tables
-- ============================================================
SET search_path TO intermodal, public;

-- ╔══════════════════════════════════════════════════════════╗
-- ║  STEP 1: Load CSVs into raw staging tables              ║
-- ║  Run these COPY commands manually via psql:              ║
-- ║  \copy raw_country FROM '_db/seed/countries.csv' ...     ║
-- ╚══════════════════════════════════════════════════════════╝
-- Note: Flyway cannot execute COPY with local files.
-- Use the companion script _db/seed/load_csv.sh instead.
-- This migration handles STEP 2: raw → target upserts.

-- ╔══════════════════════════════════════════════════════════╗
-- ║  STEP 2a: raw_country → ref_country                     ║
-- ╚══════════════════════════════════════════════════════════╝
INSERT INTO ref_country (iso_code, name, continent, wikipedia_link, keywords)
SELECT
    rc.code,
    rc.name,
    rc.continent,
    rc.wikipedia_link,
    rc.keywords
FROM raw_country rc
WHERE rc.code IS NOT NULL
  AND LENGTH(rc.code) = 2
ON CONFLICT (iso_code) DO UPDATE SET
    name            = EXCLUDED.name,
    continent       = EXCLUDED.continent,
    wikipedia_link  = EXCLUDED.wikipedia_link,
    keywords        = EXCLUDED.keywords,
    last_modified_date = NOW();

-- ╔══════════════════════════════════════════════════════════╗
-- ║  STEP 2b: raw_region → ref_region                       ║
-- ╚══════════════════════════════════════════════════════════╝
INSERT INTO ref_region (code, local_code, name, continent, country_iso_code, wikipedia_link, keywords)
SELECT
    rr.code,
    rr.local_code,
    rr.name,
    rr.continent,
    rr.iso_country,
    rr.wikipedia_link,
    rr.keywords
FROM raw_region rr
WHERE rr.code IS NOT NULL
  AND rr.iso_country IS NOT NULL
  AND EXISTS (SELECT 1 FROM ref_country WHERE iso_code = rr.iso_country)
ON CONFLICT (code) DO UPDATE SET
    local_code      = EXCLUDED.local_code,
    name            = EXCLUDED.name,
    continent       = EXCLUDED.continent,
    wikipedia_link  = EXCLUDED.wikipedia_link,
    keywords        = EXCLUDED.keywords,
    last_modified_date = NOW();

-- ╔══════════════════════════════════════════════════════════╗
-- ║  STEP 2c: raw_airport → location                        ║
-- ║  Uses DO NOTHING because partial unique index on         ║
-- ║  (source, source_pk) doesn't support ON CONFLICT         ║
-- ╚══════════════════════════════════════════════════════════╝
INSERT INTO location (
    id, type, name, country_iso_code, region_code, city, timezone,
    lat, lon, iata_code, icao_code, gps_code, local_code,
    is_searchable, search_priority, source, source_pk
)
SELECT
    gen_random_uuid(),
    'AIRPORT',
    ra.name,
    ra.iso_country,
    ra.iso_region,
    ra.municipality,
    NULL,  -- timezone will be enriched later
    ra.latitude_deg,
    ra.longitude_deg,
    NULLIF(TRIM(ra.iata_code), ''),
    NULLIF(TRIM(ra.icao_code), ''),
    NULLIF(TRIM(ra.gps_code), ''),
    NULLIF(TRIM(ra.local_code), ''),
    -- Searchability: large/medium airports with IATA codes
    CASE WHEN ra.type IN ('large_airport', 'medium_airport')
          AND ra.iata_code IS NOT NULL
          AND TRIM(ra.iata_code) <> ''
         THEN TRUE ELSE FALSE END,
    -- Priority: large=200, medium=100, small=50, rest=10
    CASE ra.type
        WHEN 'large_airport'  THEN 200
        WHEN 'medium_airport' THEN 100
        WHEN 'small_airport'  THEN 50
        ELSE 10
    END,
    'OURAIRPORTS',
    ra.id::TEXT
FROM raw_airport ra
WHERE ra.iso_country IS NOT NULL
  AND EXISTS (SELECT 1 FROM ref_country WHERE iso_code = ra.iso_country)
  AND (ra.iso_region IS NULL OR EXISTS (SELECT 1 FROM ref_region WHERE code = ra.iso_region))
  AND ra.type NOT IN ('closed')
  AND ra.id IS NOT NULL
ON CONFLICT DO NOTHING;

-- ╔══════════════════════════════════════════════════════════╗
-- ║  STEP 2d: raw_airport → airport_profile                 ║
-- ╚══════════════════════════════════════════════════════════╝
INSERT INTO airport_profile (location_id, airport_kind, elevation_ft, scheduled_service, home_link, wikipedia_link, keywords)
SELECT
    loc.id,
    ra.type,
    ra.elevation_ft,
    CASE WHEN LOWER(ra.scheduled_service) = 'yes' THEN TRUE ELSE FALSE END,
    ra.home_link,
    ra.wikipedia_link,
    ra.keywords
FROM raw_airport ra
JOIN location loc ON loc.source = 'OURAIRPORTS' AND loc.source_pk = ra.id::TEXT
WHERE ra.type NOT IN ('closed')
ON CONFLICT DO NOTHING;
