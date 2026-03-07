# Database Seed Pipeline

## Connection

| Key | Value |
|-----|-------|
| Host | `92.205.129.27` |
| Port | `5434` |
| Database | `intermodal` |
| User | `intermodal` |
| Password | `Cloud5231!` |

## Seed (Completed ✅)

```bash
export PATH="/opt/homebrew/opt/libpq/bin:$PATH"
export PGPASSWORD='Cloud5231!'
cd /Volumes/Nevermind/code/intermodal-journey-planning/_db/seed
./load_csv.sh intermodal intermodal 92.205.129.27 5434
```

### Results

| Table | Count | Source |
|-------|-------|--------|
| `ref_country` | 249 | countries.csv |
| `ref_region` | 3,942 | regions.csv |
| `location` | 71,673 | airports.csv → V011 upsert |
| `airport_profile` | 71,673 | airports.csv → V011 upsert |
| `transport_mode` | 8 | V009 (SQL direct) |
| `journey_policy_set` | 1 | V009 (SQL direct) |
| `journey_policy_node` | 5 | V009 (SQL direct) |
| `app_user` | 2 | V009 (SQL direct) |

> Raw staging tables (`raw_country`, `raw_region`, `raw_airport`) are intermediate — no longer needed after upsert.

---

## Next Steps — What to fill next?

The following tables need data to make the routing engine work:

| Priority | Table | What | How |
|----------|-------|------|-----|
| 1️⃣ | `provider` | Airlines, railways, bus operators | Admin UI or SQL |
| 2️⃣ | `transportation_edge` | Flight/bus/train connections between locations | Admin UI or GTFS import |
| 3️⃣ | `transport_service_area` | Uber/taxi coverage zones (geofences) | Admin UI |
| 4️⃣ | `transport_stop` | Bus/train boarding points | GTFS import |
| 5️⃣ | `fare` | Pricing rules per edge | Admin UI |

### First screen to build with real data → **Providers**

Why: `transportation_edge.provider_id` references `provider` — you can't create connections without providers. Flow:

```
Provider → transportation_edge → fare → Journey Planner can search
```
