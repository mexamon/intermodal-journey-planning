-- ============================================================
-- V015: Seed — Providers + Additional Airport Locations
-- Required for Amadeus Mock sync (TK, PC airlines + CDG, MUC, AMS, FCO, IST airports)
-- Idempotent: uses ON CONFLICT
-- ============================================================
SET search_path TO intermodal, public;

-- ══════════════════════════════════════
-- 1. Airline Providers
-- ══════════════════════════════════════

INSERT INTO provider (id, code, name, type, country_iso_code, is_active)
VALUES
    (gen_random_uuid(), 'TK', 'Turkish Airlines',   'AIRLINE', 'TR', TRUE),
    (gen_random_uuid(), 'PC', 'Pegasus Airlines',   'AIRLINE', 'TR', TRUE)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    is_active = EXCLUDED.is_active;

-- ══════════════════════════════════════
-- 2. Additional Airport Locations
--    (SAW, LHR, LGW, STN, LTN, ESB, ADB already in V014)
--    Adding: IST, CDG, MUC, AMS, FCO
-- ══════════════════════════════════════

INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
SELECT gen_random_uuid(), v.iata, v.icao, v.name, 'AIRPORT', v.city, v.country, v.region, v.lat, v.lon, v.tz, 'INTERNAL', v.iata || '_SEED', TRUE, v.prio
FROM (VALUES
    ('IST', 'LTFM', 'Istanbul Airport',                     'Istanbul',  'TR', 'TR-34', 41.275278, 28.751944, 'Europe/Istanbul', 100),
    ('CDG', 'LFPG', 'Paris Charles de Gaulle Airport',       'Paris',     'FR', 'FR-IDF', 49.009722, 2.547778, 'Europe/Paris',    100),
    ('MUC', 'EDDM', 'Munich Airport',                        'Munich',    'DE', 'DE-BY',  48.353783, 11.786086, 'Europe/Berlin',   100),
    ('AMS', 'EHAM', 'Amsterdam Schiphol Airport',             'Amsterdam', 'NL', 'NL-NH', 52.308056, 4.764167, 'Europe/Amsterdam', 100),
    ('FCO', 'LIRF', 'Rome Fiumicino Leonardo da Vinci Airport', 'Rome',   'IT', 'IT-RM',  41.800278, 12.238889, 'Europe/Rome',     100)
) AS v(iata, icao, name, city, country, region, lat, lon, tz, prio)
WHERE NOT EXISTS (
    SELECT 1 FROM location l WHERE l.iata_code = v.iata
);

-- ══════════════════════════════════════
-- 3. Reference countries (if missing)
-- ══════════════════════════════════════

INSERT INTO ref_country (iso_code, name, continent)
VALUES
    ('FR', 'France',      'EU'),
    ('DE', 'Germany',     'EU'),
    ('NL', 'Netherlands', 'EU'),
    ('IT', 'Italy',       'EU')
ON CONFLICT (iso_code) DO NOTHING;

-- ══════════════════════════════════════
-- Verification
-- ══════════════════════════════════════

SELECT 'Providers: ' || count(*) FROM provider WHERE code IN ('TK', 'PC');
SELECT 'New airports: ' || count(*) FROM location WHERE iata_code IN ('IST', 'CDG', 'MUC', 'AMS', 'FCO');
