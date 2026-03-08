-- ============================================================
-- V025: Metro (SUBWAY) edges for Ankara and Berlin last-mile
-- Enables multi-modal routes: TAXI+METRO, TRAIN+METRO etc.
-- ============================================================
SET search_path TO intermodal, public;

DO $$
DECLARE
    v_subway UUID;
    -- Ankara
    v_esb   UUID;
    v_kizilay UUID;
    v_ankara_station UUID;
    -- Berlin
    v_ber   UUID;
    v_hbf   UUID;
    v_alex  UUID;
    v_zoo   UUID;
BEGIN
    SELECT id INTO v_subway FROM transport_mode WHERE code = 'SUBWAY';
    IF v_subway IS NULL THEN RAISE EXCEPTION 'SUBWAY mode not found'; END IF;

    -- ═══════════════════════════════════════
    -- 1. Ankara Metro Stations + Edges
    -- ═══════════════════════════════════════

    -- Kızılay station (city center, metro hub)
    INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
    SELECT gen_random_uuid(), NULL, NULL, 'Kızılay Metro', 'STATION', 'Ankara', 'TR', 'TR-06',
           39.921000, 32.854000, 'Europe/Istanbul', 'INTERNAL', 'ANK_KIZILAY_SEED', TRUE, 60
    WHERE NOT EXISTS (SELECT 1 FROM location WHERE source_pk = 'ANK_KIZILAY_SEED');

    -- AŞTİ (bus terminal / metro station - connects to city and ESB direction)
    INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
    SELECT gen_random_uuid(), NULL, NULL, 'Ankara Garı', 'STATION', 'Ankara', 'TR', 'TR-06',
           39.937000, 32.841000, 'Europe/Istanbul', 'INTERNAL', 'ANK_GAR_SEED', TRUE, 55
    WHERE NOT EXISTS (SELECT 1 FROM location WHERE source_pk = 'ANK_GAR_SEED');

    SELECT id INTO v_esb     FROM location WHERE iata_code = 'ESB' LIMIT 1;
    SELECT id INTO v_kizilay FROM location WHERE source_pk = 'ANK_KIZILAY_SEED' LIMIT 1;
    SELECT id INTO v_ankara_station FROM location WHERE source_pk = 'ANK_GAR_SEED' LIMIT 1;

    IF v_esb IS NULL THEN RAISE NOTICE 'ESB not found, skipping Ankara metro'; END IF;
    IF v_kizilay IS NULL THEN RAISE NOTICE 'Kızılay not found'; END IF;

    -- Edge: Kızılay → Ankara Garı (metro, ~8 min)
    IF v_kizilay IS NOT NULL AND v_ankara_station IS NOT NULL THEN
        INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id,
            schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams)
        VALUES
            ('f5000000-0000-4000-8000-000000000001', v_kizilay, v_ankara_station, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 8, 3200, 200),
            ('f5000000-0000-4000-8000-000000000002', v_ankara_station, v_kizilay, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 8, 3200, 200)
        ON CONFLICT (id) DO NOTHING;

        -- Frequency-based: every 5 min, operating 06:00-23:00
        -- No edge_trip needed for FREQUENCY — computed at query time with duration
        RAISE NOTICE '✅ Ankara metro edges: Kızılay ↔ Ankara Garı';
    END IF;

    -- ═══════════════════════════════════════
    -- 2. Berlin U-Bahn/S-Bahn Stations + Edges
    -- ═══════════════════════════════════════

    -- Berlin Alexanderplatz (major transit hub)
    INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
    SELECT gen_random_uuid(), NULL, NULL, 'Berlin Alexanderplatz', 'STATION', 'Berlin', 'DE', 'DE-BE',
           52.521992, 13.413244, 'Europe/Berlin', 'INTERNAL', 'BER_ALEX_SEED', TRUE, 65
    WHERE NOT EXISTS (SELECT 1 FROM location WHERE source_pk = 'BER_ALEX_SEED');

    -- Berlin Zoologischer Garten (west Berlin hub)
    INSERT INTO location (id, iata_code, icao_code, name, type, city, country_iso_code, region_code, lat, lon, timezone, source, source_pk, is_searchable, search_priority)
    SELECT gen_random_uuid(), NULL, NULL, 'Berlin Zoo', 'STATION', 'Berlin', 'DE', 'DE-BE',
           52.506944, 13.332500, 'Europe/Berlin', 'INTERNAL', 'BER_ZOO_SEED', TRUE, 60
    WHERE NOT EXISTS (SELECT 1 FROM location WHERE source_pk = 'BER_ZOO_SEED');

    SELECT id INTO v_ber  FROM location WHERE iata_code = 'BER' LIMIT 1;
    SELECT id INTO v_hbf  FROM location WHERE source_pk = 'BER_HBF_SEED' LIMIT 1;
    SELECT id INTO v_alex FROM location WHERE source_pk = 'BER_ALEX_SEED' LIMIT 1;
    SELECT id INTO v_zoo  FROM location WHERE source_pk = 'BER_ZOO_SEED' LIMIT 1;

    IF v_ber IS NULL THEN RAISE NOTICE 'BER airport not found, skipping Berlin metro'; END IF;
    IF v_hbf IS NULL THEN RAISE NOTICE 'Berlin Hbf not found'; END IF;

    -- Edge: BER → Alexanderplatz (S-Bahn S9/S45, ~35 min)
    IF v_ber IS NOT NULL AND v_alex IS NOT NULL THEN
        INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id,
            schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams)
        VALUES
            ('f5000000-0000-4000-8000-000000000010', v_ber, v_alex, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 35, 22000, 500),
            ('f5000000-0000-4000-8000-000000000011', v_alex, v_ber, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 35, 22000, 500)
        ON CONFLICT (id) DO NOTHING;
        RAISE NOTICE '✅ Berlin metro: BER ↔ Alexanderplatz';
    END IF;

    -- Edge: Hbf → Alexanderplatz (U/S-Bahn, ~8 min)
    IF v_hbf IS NOT NULL AND v_alex IS NOT NULL THEN
        INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id,
            schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams)
        VALUES
            ('f5000000-0000-4000-8000-000000000012', v_hbf, v_alex, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 8, 3500, 100),
            ('f5000000-0000-4000-8000-000000000013', v_alex, v_hbf, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 8, 3500, 100)
        ON CONFLICT (id) DO NOTHING;
        RAISE NOTICE '✅ Berlin metro: Hbf ↔ Alexanderplatz';
    END IF;

    -- Edge: Hbf → Zoo (S-Bahn, ~10 min)
    IF v_hbf IS NOT NULL AND v_zoo IS NOT NULL THEN
        INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id,
            schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams)
        VALUES
            ('f5000000-0000-4000-8000-000000000014', v_hbf, v_zoo, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 10, 4500, 120),
            ('f5000000-0000-4000-8000-000000000015', v_zoo, v_hbf, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 10, 4500, 120)
        ON CONFLICT (id) DO NOTHING;
        RAISE NOTICE '✅ Berlin metro: Hbf ↔ Zoo';
    END IF;

    -- Edge: Alexanderplatz → Zoo (U2, ~15 min)
    IF v_alex IS NOT NULL AND v_zoo IS NOT NULL THEN
        INSERT INTO transportation_edge (id, origin_location_id, destination_location_id, transport_mode_id,
            schedule_type, operating_days_mask, status, source, estimated_duration_min, distance_m, co2_grams)
        VALUES
            ('f5000000-0000-4000-8000-000000000016', v_alex, v_zoo, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 15, 6000, 150),
            ('f5000000-0000-4000-8000-000000000017', v_zoo, v_alex, v_subway,
             'FREQUENCY', 127, 'ACTIVE', 'MANUAL', 15, 6000, 150)
        ON CONFLICT (id) DO NOTHING;
        RAISE NOTICE '✅ Berlin metro: Alexanderplatz ↔ Zoo';
    END IF;

    RAISE NOTICE '✅ V025: Metro edges seeded for Ankara + Berlin';
END $$;
