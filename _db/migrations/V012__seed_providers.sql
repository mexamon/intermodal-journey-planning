-- ============================================================
-- V012: Seed Providers — Turkey & Germany
-- Real-world transport providers across all 7 types
-- Idempotent: uses ON CONFLICT (code) DO UPDATE
-- ============================================================
SET search_path TO intermodal, public;

INSERT INTO provider (id, code, name, type, country_iso_code, is_active, config_json)
VALUES
    -- ═══════════════════════════════════════
    -- AIRLINES
    -- ═══════════════════════════════════════
    -- Turkey
    (gen_random_uuid(), 'TK',   'Turkish Airlines',          'AIRLINE', 'TR', TRUE,  '{"iata":"TK","icao":"THY","alliance":"Star Alliance"}'),
    (gen_random_uuid(), 'PC',   'Pegasus Airlines',          'AIRLINE', 'TR', TRUE,  '{"iata":"PC","icao":"PGT","type":"LCC"}'),
    (gen_random_uuid(), 'XQ',   'SunExpress',                'AIRLINE', 'TR', TRUE,  '{"iata":"XQ","icao":"SXS","type":"leisure"}'),
    (gen_random_uuid(), 'AJ',   'AnadoluJet',                'AIRLINE', 'TR', TRUE,  '{"iata":"AJ","icao":"AJA","parent":"TK"}'),
    -- Germany
    (gen_random_uuid(), 'LH',   'Lufthansa',                 'AIRLINE', 'DE', TRUE,  '{"iata":"LH","icao":"DLH","alliance":"Star Alliance"}'),
    (gen_random_uuid(), 'EW',   'Eurowings',                 'AIRLINE', 'DE', TRUE,  '{"iata":"EW","icao":"EWG","type":"LCC","parent":"LH"}'),
    (gen_random_uuid(), 'DE',   'Condor',                    'AIRLINE', 'DE', TRUE,  '{"iata":"DE","icao":"CFG","type":"leisure"}'),

    -- ═══════════════════════════════════════
    -- TRAIN OPERATORS
    -- ═══════════════════════════════════════
    -- Turkey
    (gen_random_uuid(), 'TCDD', 'TCDD Taşımacılık',          'TRAIN_OPERATOR', 'TR', TRUE,  '{"services":["YHT","Mavi_Tren","Dogu_Ekspresi"]}'),
    -- Germany
    (gen_random_uuid(), 'DB',   'Deutsche Bahn',             'TRAIN_OPERATOR', 'DE', TRUE,  '{"services":["ICE","IC","RE","S-Bahn"]}'),
    (gen_random_uuid(), 'DBFV', 'DB Fernverkehr',            'TRAIN_OPERATOR', 'DE', TRUE,  '{"services":["ICE","IC"],"parent":"DB"}'),

    -- ═══════════════════════════════════════
    -- BUS COMPANIES
    -- ═══════════════════════════════════════
    -- Turkey
    (gen_random_uuid(), 'MET',  'Metro Turizm',              'BUS_COMPANY', 'TR', TRUE,  '{"website":"metrotirizm.com.tr"}'),
    (gen_random_uuid(), 'KAM',  'Kamil Koç',                 'BUS_COMPANY', 'TR', TRUE,  '{"website":"kamilkoc.com.tr","parent":"FlixMobility"}'),
    (gen_random_uuid(), 'PAM',  'Pamukkale Turizm',          'BUS_COMPANY', 'TR', TRUE,  '{"website":"pamukkale.com.tr"}'),
    (gen_random_uuid(), 'ULSK', 'Ulusoy',                    'BUS_COMPANY', 'TR', TRUE,  '{"website":"ulusoy.com.tr"}'),
    (gen_random_uuid(), 'SUHA', 'Süha Turizm',               'BUS_COMPANY', 'TR', TRUE,  NULL),
    -- Germany
    (gen_random_uuid(), 'FLX',  'FlixBus',                   'BUS_COMPANY', 'DE', TRUE,  '{"website":"flixbus.de","international":true}'),

    -- ═══════════════════════════════════════
    -- METRO / URBAN RAIL OPERATORS
    -- ═══════════════════════════════════════
    -- Turkey
    (gen_random_uuid(), 'IBBM', 'İstanbul Metro',            'METRO_OPERATOR', 'TR', TRUE,  '{"city":"Istanbul","lines":["M1","M2","M3","M4","M5","M6","M7","M8","M9","M11"]}'),
    (gen_random_uuid(), 'ESBM', 'EGO Ankara Metro',          'METRO_OPERATOR', 'TR', TRUE,  '{"city":"Ankara","lines":["M1","M2","M3","M4","Ankaray"]}'),
    (gen_random_uuid(), 'IZBN', 'İzmir Metro',               'METRO_OPERATOR', 'TR', TRUE,  '{"city":"Izmir","lines":["Metro","Tramvay"]}'),
    (gen_random_uuid(), 'BRSM', 'Bursa Metro (Bursaray)',    'METRO_OPERATOR', 'TR', TRUE,  '{"city":"Bursa","lines":["T1"]}'),
    -- Germany
    (gen_random_uuid(), 'BVG',  'BVG Berlin',                'METRO_OPERATOR', 'DE', TRUE,  '{"city":"Berlin","lines":["U1-U9","Tram"]}'),
    (gen_random_uuid(), 'MVG',  'MVG München',               'METRO_OPERATOR', 'DE', TRUE,  '{"city":"Munich","lines":["U1-U8"]}'),
    (gen_random_uuid(), 'HVV',  'HVV Hamburg',               'METRO_OPERATOR', 'DE', TRUE,  '{"city":"Hamburg","lines":["U1-U4","S-Bahn"]}'),

    -- ═══════════════════════════════════════
    -- RIDE-SHARE / TAXI
    -- ═══════════════════════════════════════
    -- Turkey
    (gen_random_uuid(), 'UBTR', 'Uber Turkey',               'RIDE_SHARE', 'TR', TRUE,  '{"service":"UberXL","cities":["Istanbul","Ankara","Izmir"]}'),
    (gen_random_uuid(), 'BOLT', 'Bolt Turkey',               'RIDE_SHARE', 'TR', TRUE,  '{"cities":["Istanbul","Ankara","Izmir","Antalya"]}'),
    (gen_random_uuid(), 'BTXI', 'BiTaksi',                   'RIDE_SHARE', 'TR', TRUE,  '{"type":"taxi_aggregator","cities":["Istanbul","Ankara"]}'),
    (gen_random_uuid(), 'ITXI', 'iTaksi',                    'RIDE_SHARE', 'TR', TRUE,  '{"type":"taxi_aggregator","operator":"IBB","city":"Istanbul"}'),
    -- Germany
    (gen_random_uuid(), 'UBDE', 'Uber Germany',              'RIDE_SHARE', 'DE', TRUE,  '{"cities":["Berlin","Munich","Frankfurt","Hamburg"]}'),
    (gen_random_uuid(), 'BLDE', 'Bolt Germany',              'RIDE_SHARE', 'DE', TRUE,  '{"cities":["Berlin","Munich","Cologne"]}'),
    (gen_random_uuid(), 'FREE', 'FREE NOW',                  'RIDE_SHARE', 'DE', TRUE,  '{"type":"taxi_aggregator","cities":["Berlin","Hamburg","Munich","Frankfurt"]}'),
    (gen_random_uuid(), 'SIXT', 'SIXT ride',                 'RIDE_SHARE', 'DE', TRUE,  '{"type":"ride_hailing","cities":["Munich","Berlin","Hamburg"]}'),

    -- ═══════════════════════════════════════
    -- FERRY OPERATORS
    -- ═══════════════════════════════════════
    -- Turkey
    (gen_random_uuid(), 'IDO',  'İDO',                       'FERRY_OPERATOR', 'TR', TRUE,  '{"routes":["Istanbul-Bursa","Istanbul-Yalova","Istanbul-Bandirma"]}'),
    (gen_random_uuid(), 'BUDO', 'BUDO',                      'FERRY_OPERATOR', 'TR', TRUE,  '{"routes":["Istanbul-Bursa (Mudanya)"],"operator":"Bursa Belediyesi"}'),
    (gen_random_uuid(), 'SHHL', 'Şehir Hatları',             'FERRY_OPERATOR', 'TR', TRUE,  '{"routes":["Bosphorus","Golden_Horn","Islands"],"operator":"IBB"}'),
    (gen_random_uuid(), 'IZDN', 'İzmir Deniz',               'FERRY_OPERATOR', 'TR', TRUE,  '{"routes":["Konak-Karsiyaka","Konak-Bostanli"]}'),

    -- ═══════════════════════════════════════
    -- BUS (URBAN / CITY)  — as OTHER type to distinguish from intercity bus
    -- ═══════════════════════════════════════
    -- Turkey
    (gen_random_uuid(), 'IETT', 'İETT',                      'OTHER', 'TR', TRUE,  '{"scope":"urban_bus","city":"Istanbul","operator":"IBB"}'),
    (gen_random_uuid(), 'EBUS', 'EGO Otobüs',               'OTHER', 'TR', TRUE,  '{"scope":"urban_bus","city":"Ankara"}'),
    -- Germany
    (gen_random_uuid(), 'BVGB', 'BVG Bus Berlin',            'OTHER', 'DE', TRUE,  '{"scope":"urban_bus","city":"Berlin"}')

ON CONFLICT (code) DO UPDATE SET
    name               = EXCLUDED.name,
    type               = EXCLUDED.type,
    country_iso_code   = EXCLUDED.country_iso_code,
    is_active          = EXCLUDED.is_active,
    config_json        = EXCLUDED.config_json,
    last_modified_date = NOW(),
    version            = provider.version + 1;

-- Summary report
DO $$
DECLARE
    v_total INT;
    v_types TEXT;
BEGIN
    SELECT count(*) INTO v_total FROM provider WHERE deleted = FALSE;
    SELECT string_agg(type || ': ' || cnt::TEXT, ', ' ORDER BY type)
    INTO v_types
    FROM (SELECT type, count(*) AS cnt FROM provider WHERE deleted = FALSE GROUP BY type) sub;

    RAISE NOTICE '✅ Provider seed complete — % active providers [%]', v_total, v_types;
END $$;
