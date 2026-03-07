#!/usr/bin/env bash
# ============================================================
# OurAirports CSV → PostgreSQL Seed Pipeline
# Loads CSVs into raw staging tables, then upserts to targets
#
# Kullanım:
#   cd _db/seed
#   ./load_csv.sh
#
# Özelleştirme:
#   ./load_csv.sh [DB_NAME] [DB_USER] [DB_HOST] [DB_PORT]
# ============================================================
set -euo pipefail

DB_NAME="${1:-airway}"
DB_USER="${2:-postgres}"
DB_HOST="${3:-localhost}"
DB_PORT="${4:-5432}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATION_DIR="${SCRIPT_DIR}/../migrations"

PSQL="psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -v ON_ERROR_STOP=1"

echo "╔══════════════════════════════════════════════════╗"
echo "║  OurAirports CSV Seed Pipeline                   ║"
echo "║  ${DB_NAME}@${DB_HOST}:${DB_PORT}               ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

# ── STEP 1: Truncate staging tables ──
echo "1/5  Truncating raw staging tables..."
${PSQL} -c "SET search_path TO intermodal, public; TRUNCATE raw_country, raw_region, raw_airport;"
echo "     ✅ Done"

# ── STEP 2: Load countries.csv ──
echo "2/5  Loading countries.csv..."
${PSQL} <<EOF
SET search_path TO intermodal, public;
\COPY raw_country (id, code, name, continent, wikipedia_link, keywords) FROM '${SCRIPT_DIR}/countries.csv' WITH (FORMAT csv, HEADER true, QUOTE '"');
EOF
COUNT=$(${PSQL} -t -c "SELECT COUNT(*) FROM intermodal.raw_country;" | tr -d ' ')
echo "     ✅ ${COUNT} countries loaded"

# ── STEP 3: Load regions.csv ──
echo "3/5  Loading regions.csv..."
${PSQL} <<EOF
SET search_path TO intermodal, public;
\COPY raw_region (id, code, local_code, name, continent, iso_country, wikipedia_link, keywords) FROM '${SCRIPT_DIR}/regions.csv' WITH (FORMAT csv, HEADER true, QUOTE '"');
EOF
COUNT=$(${PSQL} -t -c "SELECT COUNT(*) FROM intermodal.raw_region;" | tr -d ' ')
echo "     ✅ ${COUNT} regions loaded"

# ── STEP 4: Load airports.csv ──
echo "4/5  Loading airports.csv..."
${PSQL} <<EOF
SET search_path TO intermodal, public;
\COPY raw_airport (id, ident, type, name, latitude_deg, longitude_deg, elevation_ft, continent, iso_country, iso_region, municipality, scheduled_service, icao_code, iata_code, gps_code, local_code, home_link, wikipedia_link, keywords) FROM '${SCRIPT_DIR}/airports.csv' WITH (FORMAT csv, HEADER true, QUOTE '"');
EOF
COUNT=$(${PSQL} -t -c "SELECT COUNT(*) FROM intermodal.raw_airport;" | tr -d ' ')
echo "     ✅ ${COUNT} airports loaded"

# ── STEP 5: Upsert raw → target tables ──
echo "5/5  Upserting to target tables..."
${PSQL} -f "${MIGRATION_DIR}/V011__seed_reference_data.sql"

# Verify counts
echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║  Seed Complete — Summary                         ║"
echo "╚══════════════════════════════════════════════════╝"
C_COUNTRY=$(${PSQL} -t -c "SELECT COUNT(*) FROM intermodal.ref_country;" | tr -d ' ')
C_REGION=$(${PSQL} -t -c "SELECT COUNT(*) FROM intermodal.ref_region;" | tr -d ' ')
C_LOCATION=$(${PSQL} -t -c "SELECT COUNT(*) FROM intermodal.location;" | tr -d ' ')
C_AIRPORT=$(${PSQL} -t -c "SELECT COUNT(*) FROM intermodal.airport_profile;" | tr -d ' ')
C_SEARCHABLE=$(${PSQL} -t -c "SELECT COUNT(*) FROM intermodal.location WHERE is_searchable = TRUE;" | tr -d ' ')

echo "  ref_country    : ${C_COUNTRY}"
echo "  ref_region     : ${C_REGION}"
echo "  location       : ${C_LOCATION}"
echo "  airport_profile: ${C_AIRPORT}"
echo "  searchable     : ${C_SEARCHABLE} (large+medium with IATA)"
echo ""
echo "Done! 🎉"
