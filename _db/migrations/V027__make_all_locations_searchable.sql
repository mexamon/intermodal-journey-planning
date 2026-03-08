-- ============================================================
-- V027: Make all remaining locations searchable
-- After V026 cleanup, only IATA-coded airports remain.
-- All of them should be searchable in journey search.
-- ============================================================
SET search_path TO intermodal, public;

UPDATE location SET is_searchable = TRUE WHERE is_searchable = FALSE;
