-- ============================================================
-- V020: Transfer Times — stored in transport_mode.config_json
-- Previously hardcoded in JourneySearchServiceImpl
-- ============================================================
SET search_path TO intermodal, public;

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '60'::jsonb
) WHERE code = 'FLIGHT';

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '15'::jsonb
) WHERE code = 'TRAIN';

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '10'::jsonb
) WHERE code = 'BUS';

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '5'::jsonb
) WHERE code = 'SUBWAY';

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '15'::jsonb
) WHERE code = 'FERRY';

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '0'::jsonb
) WHERE code = 'WALKING';

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '5'::jsonb
) WHERE code = 'UBER';

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '5'::jsonb
) WHERE code = 'BIKE';

UPDATE transport_mode SET config_json = jsonb_set(
    COALESCE(config_json, '{}'::jsonb),
    '{transfer_time_min}',
    '5'::jsonb
) WHERE code = 'TAXI';
