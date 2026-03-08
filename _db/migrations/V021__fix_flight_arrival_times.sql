-- ============================================================
-- V021: Fix flight arrival times — correct for timezone offsets
-- V014 seed had arrival_time calculated without timezone conversion
-- Istanbul (UTC+3) → London (UTC+0): arrival = dep + duration - 3h
-- London (UTC+0) → Istanbul (UTC+3): arrival = dep + duration + 3h
-- Domestic TR flights were correct (same timezone)
-- ============================================================
SET search_path TO intermodal, public;

-- ═══════════════════════════════════════
-- SAW → LHR (TK) — 230min, net +0h50m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '07:20' WHERE id = 'f1000000-0000-4000-8000-000000000001'; -- TK1987 06:30→07:20
UPDATE edge_trip SET arrival_time = '11:05' WHERE id = 'f1000000-0000-4000-8000-000000000002'; -- TK1971 10:15→11:05
UPDATE edge_trip SET arrival_time = '15:30' WHERE id = 'f1000000-0000-4000-8000-000000000003'; -- TK1975 14:40→15:30
UPDATE edge_trip SET arrival_time = '19:50' WHERE id = 'f1000000-0000-4000-8000-000000000004'; -- TK1979 19:00→19:50

-- ═══════════════════════════════════════
-- SAW → LGW (PC) — 240min, net +1h00m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '06:45' WHERE id = 'f1000000-0000-4000-8000-000000000005'; -- PC1171 05:45→06:45
UPDATE edge_trip SET arrival_time = '13:30' WHERE id = 'f1000000-0000-4000-8000-000000000006'; -- PC1173 12:30→13:30
UPDATE edge_trip SET arrival_time = '19:00' WHERE id = 'f1000000-0000-4000-8000-000000000007'; -- PC1175 18:00→19:00

-- ═══════════════════════════════════════
-- SAW → STN (PC) — 245min, net +1h05m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '07:05' WHERE id = 'f1000000-0000-4000-8000-000000000008'; -- PC1191 06:00→07:05
UPDATE edge_trip SET arrival_time = '14:50' WHERE id = 'f1000000-0000-4000-8000-000000000009'; -- PC1193 13:45→14:50
UPDATE edge_trip SET arrival_time = '21:20' WHERE id = 'f1000000-0000-4000-8000-000000000010'; -- PC1195 20:15→21:20

-- ═══════════════════════════════════════
-- SAW → LHR (PC) — 235min, net +0h55m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '07:55' WHERE id = 'f1000000-0000-4000-8000-000000000011'; -- PC1181 07:00→07:55
UPDATE edge_trip SET arrival_time = '17:25' WHERE id = 'f1000000-0000-4000-8000-000000000012'; -- PC1183 16:30→17:25

-- ═══════════════════════════════════════
-- SAW → LTN (TK) — 225min, net +0h45m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '08:45' WHERE id = 'f1000000-0000-4000-8000-000000000013'; -- TK1991 08:00→08:45
UPDATE edge_trip SET arrival_time = '18:00' WHERE id = 'f1000000-0000-4000-8000-000000000014'; -- TK1993 17:15→18:00

-- ═══════════════════════════════════════
-- LHR → SAW (TK return) — 220min, net +6h40m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '13:40' WHERE id = 'f1000000-0000-4000-8000-000000000015'; -- TK1988 07:00→13:40
UPDATE edge_trip SET arrival_time = '18:10' WHERE id = 'f1000000-0000-4000-8000-000000000016'; -- TK1972 11:30→18:10
UPDATE edge_trip SET arrival_time = '22:40' WHERE id = 'f1000000-0000-4000-8000-000000000017'; -- TK1976 16:00→22:40

-- ═══════════════════════════════════════
-- LGW → SAW (PC return) — 230min, net +6h50m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '16:20' WHERE id = 'f1000000-0000-4000-8000-000000000018'; -- PC1172 09:30→16:20
UPDATE edge_trip SET arrival_time = '03:50' WHERE id = 'f1000000-0000-4000-8000-000000000019'; -- PC1174 21:00→03:50 (+1 day)

-- ═══════════════════════════════════════
-- STN → SAW (PC return) — 235min, net +6h55m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '16:55' WHERE id = 'f1000000-0000-4000-8000-000000000020'; -- PC1192 10:00→16:55
UPDATE edge_trip SET arrival_time = '04:55' WHERE id = 'f1000000-0000-4000-8000-000000000021'; -- PC1194 22:00→04:55 (+1 day)

-- ═══════════════════════════════════════
-- IST → LHR (TK) — 225min, net +0h45m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '05:45' WHERE id = 'f1000000-0000-4000-8000-000000000022'; -- TK1981 05:00→05:45
UPDATE edge_trip SET arrival_time = '09:15' WHERE id = 'f1000000-0000-4000-8000-000000000023'; -- TK1983 08:30→09:15
UPDATE edge_trip SET arrival_time = '12:45' WHERE id = 'f1000000-0000-4000-8000-000000000024'; -- TK1985 12:00→12:45
UPDATE edge_trip SET arrival_time = '17:15' WHERE id = 'f1000000-0000-4000-8000-000000000025'; -- TK1989 16:30→17:15
UPDATE edge_trip SET arrival_time = '21:45' WHERE id = 'f1000000-0000-4000-8000-000000000026'; -- TK1995 21:00→21:45

-- ═══════════════════════════════════════
-- IST → LGW (TK) — 235min, net +0h55m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '09:55' WHERE id = 'f1000000-0000-4000-8000-000000000027'; -- TK1997 09:00→09:55
UPDATE edge_trip SET arrival_time = '18:55' WHERE id = 'f1000000-0000-4000-8000-000000000028'; -- TK1999 18:00→18:55

-- ═══════════════════════════════════════
-- ESB → LHR (TK) — 240min, net +1h00m
-- ═══════════════════════════════════════
UPDATE edge_trip SET arrival_time = '09:00' WHERE id = 'f1000000-0000-4000-8000-000000000038'; -- TK1960 08:00→09:00
UPDATE edge_trip SET arrival_time = '16:00' WHERE id = 'f1000000-0000-4000-8000-000000000039'; -- TK1962 15:00→16:00

-- Domestic flights (SAW→ESB, SAW→ADB) are already correct — same timezone
