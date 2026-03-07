import React, { useState, useMemo, useCallback } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from './ConnectionsPane.module.scss';
import * as Dialog from '@radix-ui/react-dialog';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import * as Toast from '@radix-ui/react-toast';
import {
  FiSearch, FiPlus, FiEdit2, FiTrash2, FiMoreVertical, FiX,
  FiCheck, FiAlertCircle, FiChevronDown, FiChevronLeft, FiChevronRight,
  FiChevronsLeft, FiChevronsRight, FiEdit, FiCloud, FiDatabase, FiCpu, FiGlobe,
} from 'react-icons/fi';
import {
  MdFlight, MdDirectionsBus, MdTrain, MdDirectionsSubway,
  MdLocalTaxi, MdDirectionsBoat, MdDirectionsWalk, MdPedalBike,
} from 'react-icons/md';

/* ═══════════════════════════════════════════════
   Types — matching DB V013 GTFS model
   ═══════════════════════════════════════════════ */
type EdgeStatus = 'ACTIVE' | 'INACTIVE';
type EdgeSource = 'MANUAL' | 'GOOGLE_API' | 'GTFS' | 'AMADEUS' | 'COMPUTED';
type ScheduleType = 'FIXED' | 'FREQUENCY' | 'ON_DEMAND';

interface EdgeTrip {
  id: string;
  serviceCode: string | null;
  departureTime: string;
  arrivalTime: string;
  operatingDaysMask: number;
  validFrom: string | null;
  validTo: string | null;
  estimatedCostCents: number | null;
}

interface TransportEdge {
  id: string;
  originLocationId: string;
  originLabel: string;
  destinationLocationId: string;
  destinationLabel: string;
  transportModeCode: string;
  providerCode: string | null;
  scheduleType: ScheduleType;
  // FREQUENCY fields
  operatingDaysMask: number;
  operatingStartTime: string | null;
  operatingEndTime: string | null;
  frequencyMinutes: number | null;
  // Route-level metrics
  status: EdgeStatus;
  source: EdgeSource;
  estimatedDurationMin: number | null;
  distanceM: number | null;
  co2Grams: number | null;
  // Trips (FIXED schedule only)
  trips: EdgeTrip[];
  // Audit
  version: number;
  createdDate: string;
  lastModifiedDate: string | null;
  deleted: boolean;
}

/* ═══════════ Icons / Colors ═══════════ */
const MODE_META: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  FLIGHT:  { icon: <MdFlight />,           color: '#1E88E5', label: 'Flight' },
  BUS:     { icon: <MdDirectionsBus />,    color: '#43A047', label: 'Bus' },
  TRAIN:   { icon: <MdTrain />,            color: '#E53935', label: 'Train' },
  SUBWAY:  { icon: <MdDirectionsSubway />, color: '#8E24AA', label: 'Subway' },
  UBER:    { icon: <MdLocalTaxi />,        color: '#212121', label: 'Ride-share' },
  FERRY:   { icon: <MdDirectionsBoat />,   color: '#00ACC1', label: 'Ferry' },
  WALKING: { icon: <MdDirectionsWalk />,   color: '#78909C', label: 'Walking' },
  BIKE:    { icon: <MdPedalBike />,        color: '#FF8F00', label: 'Bike' },
};

const SOURCE_META: Record<EdgeSource, { icon: React.ReactNode; color: string; label: string; desc: string }> = {
  MANUAL:     { icon: <FiEdit size={13} />,    color: '#3b82f6', label: 'Manual',     desc: 'Hand-entered data' },
  GOOGLE_API: { icon: <FiGlobe size={13} />,   color: '#a855f7', label: 'Google API', desc: 'Live from Google' },
  GTFS:       { icon: <FiDatabase size={13} />, color: '#22c55e', label: 'GTFS',       desc: 'Transit feed import' },
  AMADEUS:    { icon: <FiCloud size={13} />,    color: '#f97316', label: 'Amadeus',    desc: 'Amadeus GDS feed' },
  COMPUTED:   { icon: <FiCpu size={13} />,      color: '#6b7280', label: 'Computed',   desc: 'Auto-calculated' },
};
const SOURCE_COLORS: Record<EdgeSource, string> = Object.fromEntries(
  Object.entries(SOURCE_META).map(([k, v]) => [k, v.color])
) as Record<EdgeSource, string>;

const DAY_LABELS = ['M', 'T', 'W', 'T', 'F', 'S', 'S'] as const;
const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] as const;

/* ═══════════ Helpers ═══════════ */
const formatDuration = (min: number | null) => {
  if (min == null) return '—';
  const h = Math.floor(min / 60), m = min % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
};
const formatCost = (cents: number | null) => {
  if (cents == null) return '—';
  return cents === 0 ? 'Free' : `€${(cents / 100).toFixed(0)}`;
};
const formatDistance = (m: number | null) => {
  if (m == null) return '—';
  return m >= 1000 ? `${(m / 1000).toFixed(0)} km` : `${m} m`;
};

/* ── Custom TimePicker ── */
const HOURS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
const MINUTES = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'));

const TimePicker: React.FC<{ value: string; onChange: (v: string) => void; placeholder?: string }> = ({ value, onChange, placeholder = 'HH:MM' }) => {
  const [open, setOpen] = useState(false);
  const [selHour, setSelHour] = useState<string | null>(null);
  const ref = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (!open) return;
    const handle = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false); };
    document.addEventListener('mousedown', handle);
    return () => document.removeEventListener('mousedown', handle);
  }, [open]);

  const curHour = value?.split(':')[0] || null;
  const activeHour = selHour ?? curHour;

  const pickHour = (h: string) => { setSelHour(h); };
  const pickMinute = (m: string) => { onChange(`${activeHour ?? '00'}:${m}`); setOpen(false); setSelHour(null); };

  return (
    <div ref={ref} style={{ position: 'relative' }}>
      <input
        className={paneStyles.formInput}
        style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem', cursor: 'pointer' }}
        value={value} placeholder={placeholder} maxLength={5}
        onFocus={() => setOpen(true)}
        onClick={() => setOpen(true)}
        onChange={e => {
          const v = e.target.value.replace(/[^\d:]/g, '');
          if (v.length === 2 && !v.includes(':') && value.length < 2) { onChange(v + ':'); }
          else { onChange(v); }
        }}
      />
      {open && (
        <div style={{
          position: 'absolute', top: '100%', left: 0, zIndex: 500, marginTop: 4,
          borderRadius: 10, overflow: 'hidden', display: 'flex',
          width: 220,
        }} className={s.dropdownContent}>
          {/* Hours */}
          <div style={{ flex: 1, maxHeight: 200, overflowY: 'auto', borderRight: '1px solid rgba(128,128,128,0.1)', padding: '0.25rem', scrollbarWidth: 'thin', scrollbarColor: 'rgba(0,0,0,0.12) transparent' } as React.CSSProperties}>
            <div style={{ fontSize: '0.6rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', padding: '0.2rem 0.35rem', opacity: 0.4 }}>Hour</div>
            {HOURS.map(h => (
              <div key={h} onClick={() => pickHour(h)} style={{
                padding: '0.3rem 0.5rem', borderRadius: 6, cursor: 'pointer', fontSize: '0.8rem',
                fontFamily: 'monospace', fontWeight: activeHour === h ? 700 : 400,
                backgroundColor: activeHour === h ? 'rgba(30,136,229,0.15)' : 'transparent',
                color: activeHour === h ? '#1E88E5' : 'inherit',
                transition: 'background 0.1s',
              }}
                onMouseEnter={e => { if (activeHour !== h) (e.target as HTMLElement).style.backgroundColor = 'rgba(128,128,128,0.08)'; }}
                onMouseLeave={e => { if (activeHour !== h) (e.target as HTMLElement).style.backgroundColor = 'transparent'; }}
              >
                {h}
              </div>
            ))}
          </div>
          {/* Minutes */}
          <div style={{ flex: 1, maxHeight: 200, overflowY: 'auto', padding: '0.25rem', scrollbarWidth: 'thin', scrollbarColor: 'rgba(0,0,0,0.12) transparent' } as React.CSSProperties}>
            <div style={{ fontSize: '0.6rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', padding: '0.2rem 0.35rem', opacity: 0.4 }}>Min</div>
            {MINUTES.map(m => {
              const isActive = value === `${activeHour}:${m}`;
              return (
                <div key={m} onClick={() => pickMinute(m)} style={{
                  padding: '0.3rem 0.5rem', borderRadius: 6, cursor: 'pointer', fontSize: '0.8rem',
                  fontFamily: 'monospace', fontWeight: isActive ? 700 : 400,
                  backgroundColor: isActive ? 'rgba(30,136,229,0.15)' : 'transparent',
                  color: isActive ? '#1E88E5' : 'inherit',
                  transition: 'background 0.1s',
                }}
                  onMouseEnter={e => { if (!isActive) (e.target as HTMLElement).style.backgroundColor = 'rgba(128,128,128,0.08)'; }}
                  onMouseLeave={e => { if (!isActive) (e.target as HTMLElement).style.backgroundColor = 'transparent'; }}
                >
                  :{m}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

/* ── Operating Days mini component ── */
const OperatingDays: React.FC<{ mask: number }> = ({ mask }) => (
  <div style={{ display: 'flex', gap: '2px' }}>
    {DAY_LABELS.map((label, i) => {
      const active = (mask & (1 << i)) !== 0;
      return (
        <span key={i} title={DAY_NAMES[i]} style={{
          width: 16, height: 16, borderRadius: '50%', display: 'inline-flex',
          alignItems: 'center', justifyContent: 'center',
          fontSize: '0.55rem', fontWeight: 700,
          backgroundColor: active ? 'rgba(30,136,229,0.8)' : 'rgba(107,114,128,0.12)',
          color: active ? '#fff' : 'rgba(107,114,128,0.4)',
        }}>
          {label}
        </span>
      );
    })}
  </div>
);

/* ═══════════════════════════════════════════════
   Mock Data — realistic edges (TR + DE routes)
   ═══════════════════════════════════════════════ */
const INITIAL_EDGES: TransportEdge[] = [
  { id: 'e1', originLocationId: 'loc_ist', originLabel: 'IST — Istanbul Airport', destinationLocationId: 'loc_lhr', destinationLabel: 'LHR — London Heathrow', transportModeCode: 'FLIGHT', providerCode: 'TK', scheduleType: 'FIXED', operatingDaysMask: 127, operatingStartTime: null, operatingEndTime: null, frequencyMinutes: null, status: 'ACTIVE', source: 'MANUAL', estimatedDurationMin: 230, distanceM: 2500000, co2Grams: 285000, trips: [
    { id: 't1a', serviceCode: 'TK1987', departureTime: '07:35', arrivalTime: '10:25', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 28900 },
    { id: 't1b', serviceCode: 'TK1971', departureTime: '13:40', arrivalTime: '16:30', operatingDaysMask: 62, validFrom: null, validTo: null, estimatedCostCents: 31500 },
    { id: 't1c', serviceCode: 'TK1979', departureTime: '19:15', arrivalTime: '22:05', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 26900 },
  ], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e2', originLocationId: 'loc_ist', originLabel: 'IST — Istanbul Airport', destinationLocationId: 'loc_fra', destinationLabel: 'FRA — Frankfurt Airport', transportModeCode: 'FLIGHT', providerCode: 'TK', scheduleType: 'FIXED', operatingDaysMask: 127, operatingStartTime: null, operatingEndTime: null, frequencyMinutes: null, status: 'ACTIVE', source: 'MANUAL', estimatedDurationMin: 195, distanceM: 1870000, co2Grams: 210000, trips: [
    { id: 't2a', serviceCode: 'TK1591', departureTime: '08:20', arrivalTime: '10:50', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 22500 },
    { id: 't2b', serviceCode: 'TK1593', departureTime: '16:05', arrivalTime: '18:35', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 24000 },
  ], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e3', originLocationId: 'loc_saw', originLabel: 'SAW — Sabiha Gökçen', destinationLocationId: 'loc_ber', destinationLabel: 'BER — Berlin Brandenburg', transportModeCode: 'FLIGHT', providerCode: 'PC', scheduleType: 'FIXED', operatingDaysMask: 62, operatingStartTime: null, operatingEndTime: null, frequencyMinutes: null, status: 'ACTIVE', source: 'MANUAL', estimatedDurationMin: 195, distanceM: 1730000, co2Grams: 195000, trips: [
    { id: 't3a', serviceCode: 'PC1171', departureTime: '14:30', arrivalTime: '16:45', operatingDaysMask: 62, validFrom: null, validTo: null, estimatedCostCents: 14900 },
  ], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e4', originLocationId: 'loc_fra', originLabel: 'FRA — Frankfurt Flughafen', destinationLocationId: 'loc_frahbf', destinationLabel: 'Frankfurt Hbf', transportModeCode: 'TRAIN', providerCode: 'DB', scheduleType: 'FREQUENCY', operatingDaysMask: 127, operatingStartTime: '04:30', operatingEndTime: '00:30', frequencyMinutes: 15, status: 'ACTIVE', source: 'GTFS', estimatedDurationMin: 12, distanceM: 12000, co2Grams: 150, trips: [], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e5', originLocationId: 'loc_frahbf', originLabel: 'Frankfurt Hbf', destinationLocationId: 'loc_berhbf', destinationLabel: 'Berlin Hbf', transportModeCode: 'TRAIN', providerCode: 'DBFV', scheduleType: 'FIXED', operatingDaysMask: 127, operatingStartTime: null, operatingEndTime: null, frequencyMinutes: null, status: 'ACTIVE', source: 'GTFS', estimatedDurationMin: 236, distanceM: 545000, co2Grams: 2200, trips: [
    { id: 't5a', serviceCode: 'ICE 1537', departureTime: '06:52', arrivalTime: '10:48', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 5990 },
    { id: 't5b', serviceCode: 'ICE 1539', departureTime: '08:52', arrivalTime: '12:48', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 5990 },
    { id: 't5c', serviceCode: 'ICE 1545', departureTime: '14:52', arrivalTime: '18:48', operatingDaysMask: 62, validFrom: null, validTo: null, estimatedCostCents: 5990 },
  ], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e6', originLocationId: 'loc_ank', originLabel: 'Ankara Gar', destinationLocationId: 'loc_ist_gar', destinationLabel: 'İstanbul Pendik YHT', transportModeCode: 'TRAIN', providerCode: 'TCDD', scheduleType: 'FIXED', operatingDaysMask: 127, operatingStartTime: null, operatingEndTime: null, frequencyMinutes: null, status: 'ACTIVE', source: 'MANUAL', estimatedDurationMin: 265, distanceM: 533000, co2Grams: 4600, trips: [
    { id: 't6a', serviceCode: 'YHT 8001', departureTime: '06:30', arrivalTime: '10:55', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 32000 },
    { id: 't6b', serviceCode: 'YHT 8003', departureTime: '09:00', arrivalTime: '13:25', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 32000 },
    { id: 't6c', serviceCode: 'YHT 8007', departureTime: '15:30', arrivalTime: '19:55', operatingDaysMask: 62, validFrom: null, validTo: null, estimatedCostCents: 32000 },
    { id: 't6d', serviceCode: 'YHT 8009', departureTime: '18:00', arrivalTime: '22:25', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 32000 },
  ], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e7', originLocationId: 'loc_emn', originLabel: 'Eminönü', destinationLocationId: 'loc_kdky', destinationLabel: 'Kadıköy', transportModeCode: 'FERRY', providerCode: 'SHHL', scheduleType: 'FREQUENCY', operatingDaysMask: 127, operatingStartTime: '07:00', operatingEndTime: '22:30', frequencyMinutes: 20, status: 'ACTIVE', source: 'MANUAL', estimatedDurationMin: 25, distanceM: 3500, co2Grams: 800, trips: [], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e8', originLocationId: 'loc_ist_otag', originLabel: 'İstanbul Otogarı', destinationLocationId: 'loc_ank_asti', destinationLabel: 'Ankara AŞTİ', transportModeCode: 'BUS', providerCode: 'MET', scheduleType: 'FREQUENCY', operatingDaysMask: 127, operatingStartTime: '00:00', operatingEndTime: '23:59', frequencyMinutes: 30, status: 'ACTIVE', source: 'MANUAL', estimatedDurationMin: 360, distanceM: 450000, co2Grams: 22000, trips: [], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e9', originLocationId: 'loc_tax', originLabel: 'Taksim', destinationLocationId: 'loc_ist', destinationLabel: 'IST — Istanbul Airport', transportModeCode: 'UBER', providerCode: 'UBTR', scheduleType: 'ON_DEMAND', operatingDaysMask: 127, operatingStartTime: null, operatingEndTime: null, frequencyMinutes: null, status: 'ACTIVE', source: 'COMPUTED', estimatedDurationMin: 45, distanceM: 52000, co2Grams: 3800, trips: [], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e10', originLocationId: 'loc_bvg_alex', originLabel: 'Alexanderplatz', destinationLocationId: 'loc_bvg_zoo', destinationLabel: 'Zoologischer Garten', transportModeCode: 'SUBWAY', providerCode: 'BVG', scheduleType: 'FREQUENCY', operatingDaysMask: 127, operatingStartTime: '04:00', operatingEndTime: '01:00', frequencyMinutes: 5, status: 'ACTIVE', source: 'GTFS', estimatedDurationMin: 14, distanceM: 8200, co2Grams: 100, trips: [], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e11', originLocationId: 'loc_ist_t1', originLabel: 'IST Terminal 1', destinationLocationId: 'loc_ist_metro', destinationLabel: 'IST Metro İstasyonu', transportModeCode: 'WALKING', providerCode: null, scheduleType: 'ON_DEMAND', operatingDaysMask: 127, operatingStartTime: null, operatingEndTime: null, frequencyMinutes: null, status: 'ACTIVE', source: 'COMPUTED', estimatedDurationMin: 8, distanceM: 650, co2Grams: 0, trips: [], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'e12', originLocationId: 'loc_ido_yen', originLabel: 'Yenikapı İDO', destinationLocationId: 'loc_ido_brs', destinationLabel: 'Bursa Güzelyalı', transportModeCode: 'FERRY', providerCode: 'IDO', scheduleType: 'FIXED', operatingDaysMask: 127, operatingStartTime: null, operatingEndTime: null, frequencyMinutes: null, status: 'ACTIVE', source: 'MANUAL', estimatedDurationMin: 110, distanceM: 75000, co2Grams: 12000, trips: [
    { id: 't12a', serviceCode: null, departureTime: '07:30', arrivalTime: '09:20', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 28000 },
    { id: 't12b', serviceCode: null, departureTime: '10:00', arrivalTime: '11:50', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 28000 },
    { id: 't12c', serviceCode: null, departureTime: '14:30', arrivalTime: '16:20', operatingDaysMask: 62, validFrom: null, validTo: null, estimatedCostCents: 28000 },
    { id: 't12d', serviceCode: null, departureTime: '18:00', arrivalTime: '19:50', operatingDaysMask: 127, validFrom: null, validTo: null, estimatedCostCents: 28000 },
  ], version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
];

const PAGE_SIZES = [10, 20, 50];

type FormState = {
  originLabel: string; destinationLabel: string;
  transportModeCode: string; providerCode: string;
  scheduleType: ScheduleType;
  operatingDaysMask: number;
  operatingStartTime: string; operatingEndTime: string;
  frequencyMinutes: string;
  status: EdgeStatus; source: EdgeSource;
  estimatedDurationMin: string;
  distanceM: string; co2Grams: string;
};

const emptyForm = (): FormState => ({
  originLabel: '', destinationLabel: '',
  transportModeCode: 'FLIGHT', providerCode: '',
  scheduleType: 'FIXED',
  operatingDaysMask: 127,
  operatingStartTime: '', operatingEndTime: '',
  frequencyMinutes: '',
  status: 'ACTIVE', source: 'MANUAL',
  estimatedDurationMin: '',
  distanceM: '', co2Grams: '',
});

/* ═══════════════════════════════════════════════
   COMPONENT
   ═══════════════════════════════════════════════ */
export const ConnectionsPane: React.FC = () => {
  const [edges, setEdges] = useState<TransportEdge[]>(INITIAL_EDGES);
  const [search, setSearch] = useState('');
  const [modeFilter, setModeFilter] = useState<string>('ALL');
  const [sourceFilter, setSourceFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState('');
  const [toastVariant, setToastVariant] = useState<'success' | 'error'>('success');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingEdge, setDeletingEdge] = useState<TransportEdge | null>(null);
  const [expandedRoutes, setExpandedRoutes] = useState<Set<string>>(new Set());

  const showToast = useCallback((msg: string, variant: 'success' | 'error' = 'success') => {
    setToastMsg(msg); setToastVariant(variant); setToastOpen(true);
  }, []);

  /* ═══ Filtering + Pagination ═══ */
  const filtered = useMemo(() => {
    let result = edges.filter(e => !e.deleted);
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(e =>
        e.originLabel.toLowerCase().includes(q) || e.destinationLabel.toLowerCase().includes(q) ||
        e.trips.some(t => (t.serviceCode ?? '').toLowerCase().includes(q)) || (e.providerCode ?? '').toLowerCase().includes(q)
      );
    }
    if (modeFilter !== 'ALL') result = result.filter(e => e.transportModeCode === modeFilter);
    if (sourceFilter !== 'ALL') result = result.filter(e => e.source === sourceFilter);
    if (statusFilter !== 'ALL') result = result.filter(e => e.status === statusFilter);
    return result;
  }, [edges, search, modeFilter, sourceFilter, statusFilter]);

  const totalElements = filtered.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const paged = filtered.slice(safePage * pageSize, (safePage + 1) * pageSize);
  const goPage = (p: number) => setPage(Math.max(0, Math.min(p, totalPages - 1)));

  /* ═══ CRUD ═══ */
  const openAdd = () => { setEditingId(null); setForm(emptyForm()); setDialogOpen(true); };
  const openEdit = (e: TransportEdge) => {
    setEditingId(e.id);
    setForm({
      originLabel: e.originLabel, destinationLabel: e.destinationLabel,
      transportModeCode: e.transportModeCode, providerCode: e.providerCode ?? '',
      scheduleType: e.scheduleType, operatingDaysMask: e.operatingDaysMask,
      operatingStartTime: e.operatingStartTime ?? '', operatingEndTime: e.operatingEndTime ?? '',
      frequencyMinutes: e.frequencyMinutes != null ? String(e.frequencyMinutes) : '',
      status: e.status, source: e.source,
      estimatedDurationMin: e.estimatedDurationMin != null ? String(e.estimatedDurationMin) : '',
      distanceM: e.distanceM != null ? String(e.distanceM) : '',
      co2Grams: e.co2Grams != null ? String(e.co2Grams) : '',
    });
    setDialogOpen(true);
  };

  const handleSave = () => {
    if (!form.originLabel.trim() || !form.destinationLabel.trim()) {
      showToast('Origin and Destination are required.', 'error'); return;
    }
    const n = (v: string) => v ? parseInt(v) : null;
    const edgeData: Partial<TransportEdge> = {
      originLabel: form.originLabel.trim(), destinationLabel: form.destinationLabel.trim(),
      originLocationId: `loc_${Date.now()}`, destinationLocationId: `loc_${Date.now() + 1}`,
      transportModeCode: form.transportModeCode, providerCode: form.providerCode || null,
      scheduleType: form.scheduleType, operatingDaysMask: form.operatingDaysMask,
      operatingStartTime: form.operatingStartTime || null, operatingEndTime: form.operatingEndTime || null,
      frequencyMinutes: n(form.frequencyMinutes), status: form.status, source: form.source,
      estimatedDurationMin: n(form.estimatedDurationMin),
      distanceM: n(form.distanceM), co2Grams: n(form.co2Grams),
    };
    if (editingId) {
      setEdges(prev => prev.map(e => e.id === editingId ? { ...e, ...edgeData, version: e.version + 1, lastModifiedDate: new Date().toISOString() } as TransportEdge : e));
      showToast('Route updated.');
    } else {
      setEdges(prev => [...prev, { id: `e_${Date.now()}`, ...edgeData, trips: [], version: 1, createdDate: new Date().toISOString(), lastModifiedDate: null, deleted: false } as TransportEdge]);
      showToast('Route created.');
    }
    setDialogOpen(false);
  };

  const confirmDelete = (e: TransportEdge) => { setDeletingEdge(e); setDeleteDialogOpen(true); };
  const handleDelete = () => {
    if (!deletingEdge) return;
    setEdges(prev => prev.map(e => e.id === deletingEdge.id ? { ...e, deleted: true } : e));
    showToast('Connection deleted.');
    setDeleteDialogOpen(false); setDeletingEdge(null);
  };
  const toggleStatus = (e: TransportEdge) => {
    const newStatus: EdgeStatus = e.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setEdges(prev => prev.map(x => x.id === e.id ? { ...x, status: newStatus, lastModifiedDate: new Date().toISOString() } : x));
    showToast(`Connection ${newStatus === 'ACTIVE' ? 'activated' : 'deactivated'}.`);
  };

  const toggleDay = (bit: number) => {
    setForm(f => ({ ...f, operatingDaysMask: f.operatingDaysMask ^ (1 << bit) }));
  };

  /* ═══════════ RENDER ═══════════ */
  return (
    <Toast.Provider swipeDirection="right">
      {/* ── Search Panel ── */}
      <div className={s.searchPanel}>
        <div className={s.searchInputWrap}>
          <FiSearch size={14} className={s.searchIcon} />
          <input className={s.searchInput} placeholder="Search by origin, destination, service code..."
            value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} />
        </div>

        {/* Mode filter */}
        <DropdownMenu.Root>
          <DropdownMenu.Trigger asChild>
            <button className={s.filterBtn}>
              {modeFilter === 'ALL' ? 'All Modes' : MODE_META[modeFilter]?.label ?? modeFilter}
              <FiChevronDown size={12} />
            </button>
          </DropdownMenu.Trigger>
          <DropdownMenu.Portal>
            <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
              <DropdownMenu.Item className={s.dropdownItem} onSelect={() => { setModeFilter('ALL'); setPage(0); }}>
                All Modes {modeFilter === 'ALL' && <FiCheck size={14} />}
              </DropdownMenu.Item>
              {Object.entries(MODE_META).map(([code, meta]) => (
                <DropdownMenu.Item key={code} className={s.dropdownItem} onSelect={() => { setModeFilter(code); setPage(0); }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                    <span style={{ color: meta.color, display: 'flex' }}>{meta.icon}</span> {meta.label}
                  </span>
                  {modeFilter === code && <FiCheck size={14} />}
                </DropdownMenu.Item>
              ))}
            </DropdownMenu.Content>
          </DropdownMenu.Portal>
        </DropdownMenu.Root>

        {/* Source filter */}
        <DropdownMenu.Root>
          <DropdownMenu.Trigger asChild>
            <button className={s.filterBtn}>
              {sourceFilter === 'ALL' ? 'All Sources' : sourceFilter}
              <FiChevronDown size={12} />
            </button>
          </DropdownMenu.Trigger>
          <DropdownMenu.Portal>
            <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
              <DropdownMenu.Item className={s.dropdownItem} onSelect={() => { setSourceFilter('ALL'); setPage(0); }}>
                All Sources {sourceFilter === 'ALL' && <FiCheck size={14} />}
              </DropdownMenu.Item>
              {(Object.keys(SOURCE_COLORS) as EdgeSource[]).map(src => (
                <DropdownMenu.Item key={src} className={s.dropdownItem} onSelect={() => { setSourceFilter(src); setPage(0); }}>
                  <span style={{ color: SOURCE_COLORS[src] }}>{src}</span>
                  {sourceFilter === src && <FiCheck size={14} />}
                </DropdownMenu.Item>
              ))}
            </DropdownMenu.Content>
          </DropdownMenu.Portal>
        </DropdownMenu.Root>

        {/* Status filter */}
        <DropdownMenu.Root>
          <DropdownMenu.Trigger asChild>
            <button className={s.filterBtn}>
              {statusFilter === 'ALL' ? 'All Status' : statusFilter}
              <FiChevronDown size={12} />
            </button>
          </DropdownMenu.Trigger>
          <DropdownMenu.Portal>
            <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
              {['ALL', 'ACTIVE', 'INACTIVE'].map(val => (
                <DropdownMenu.Item key={val} className={s.dropdownItem} onSelect={() => { setStatusFilter(val); setPage(0); }}>
                  {val === 'ALL' ? 'All Status' : val} {statusFilter === val && <FiCheck size={14} />}
                </DropdownMenu.Item>
              ))}
            </DropdownMenu.Content>
          </DropdownMenu.Portal>
        </DropdownMenu.Root>

        <button className={s.addButton} onClick={openAdd}>
          <FiPlus size={15} /> Add Connection
        </button>

        {/* Badges */}
        <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', width: '100%', marginTop: '0.25rem' }}>
          {Object.entries(MODE_META).map(([code, meta]) => {
            const count = edges.filter(e => e.transportModeCode === code && !e.deleted).length;
            if (count === 0) return null;
            return (
              <button key={code} onClick={() => { setModeFilter(modeFilter === code ? 'ALL' : code); setPage(0); }} style={{
                display: 'inline-flex', alignItems: 'center', gap: '0.3rem',
                fontSize: '0.72rem', fontWeight: 600, padding: '0.2rem 0.55rem',
                borderRadius: 999, border: modeFilter === code ? `1.5px solid ${meta.color}` : '1.5px solid transparent',
                backgroundColor: `${meta.color}12`, color: meta.color, cursor: 'pointer', transition: 'border-color 0.2s',
              }}>
                {meta.icon} {count}
              </button>
            );
          })}
          <span style={{ fontSize: '0.72rem', opacity: 0.4, alignSelf: 'center', marginLeft: '0.25rem' }}>
            {totalElements} connection{totalElements !== 1 ? 's' : ''}
          </span>
        </div>
      </div>

      {/* ── Table ── */}
      <div className={paneStyles.tableWrapper} style={{ maxHeight: 'calc(100vh - 280px)', minHeight: 'calc(100vh - 280px)', overflow: 'auto' }}>
        <table className={paneStyles.dataTable}>
          <thead>
          <tr>
              <th>Mode</th>
              <th>Route</th>
              <th>Origin</th>
              <th style={{ width: 20 }}></th>
              <th>Destination</th>
              <th>Schedule</th>
              <th>Trips</th>
              <th>Duration</th>
              <th>Source</th>
              <th>Status</th>
              <th style={{ width: 32 }}></th>
            </tr>
          </thead>
          <tbody>
            {paged.length === 0 && (
              <tr><td colSpan={11} style={{ textAlign: 'center', padding: '2rem', opacity: 0.4 }}>No routes found.</td></tr>
            )}
            {paged.map(edge => {
              const meta = MODE_META[edge.transportModeCode] ?? { icon: null, color: '#6d7c8a', label: edge.transportModeCode };
              const isExpanded = expandedRoutes.has(edge.id);
              const toggleExpand = () => setExpandedRoutes(prev => {
                const next = new Set(prev);
                next.has(edge.id) ? next.delete(edge.id) : next.add(edge.id);
                return next;
              });
              const schedBadge = {
                FIXED: { color: '#3b82f6', label: 'Fixed' },
                FREQUENCY: { color: '#8b5cf6', label: `Every ${edge.frequencyMinutes}m` },
                ON_DEMAND: { color: '#6b7280', label: 'On Demand' },
              }[edge.scheduleType];
              return (
                <React.Fragment key={edge.id}>
                <tr style={{ opacity: edge.status === 'INACTIVE' ? 0.5 : 1, cursor: edge.trips.length > 0 ? 'pointer' : 'default' }}
                    onClick={() => { if (edge.trips.length > 0) toggleExpand(); }}>
                  <td>
                    <span style={{
                      width: 28, height: 28, borderRadius: 6, display: 'inline-flex',
                      alignItems: 'center', justifyContent: 'center',
                      fontSize: '1rem', backgroundColor: `${meta.color}12`, color: meta.color,
                    }}>
                      {meta.icon}
                    </span>
                  </td>
                  <td>
                    <span style={{ fontFamily: 'monospace', fontSize: '0.82rem', fontWeight: 600 }}>
                      {edge.providerCode || '—'}
                    </span>
                  </td>
                  <td style={{ fontSize: '0.85rem', maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {edge.originLabel}
                  </td>
                  <td style={{ fontSize: '0.75rem', opacity: 0.3, textAlign: 'center' }}>→</td>
                  <td style={{ fontSize: '0.85rem', maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {edge.destinationLabel}
                  </td>
                  <td>
                    <span style={{
                      fontSize: '0.68rem', fontWeight: 700, padding: '0.18rem 0.5rem', borderRadius: 999,
                      backgroundColor: `${schedBadge.color}12`, color: schedBadge.color,
                    }}>
                      {schedBadge.label}
                    </span>
                    {edge.scheduleType === 'FREQUENCY' && edge.operatingStartTime && (
                      <span style={{ fontSize: '0.68rem', opacity: 0.5, marginLeft: 4, fontFamily: 'monospace' }}>
                        {edge.operatingStartTime}–{edge.operatingEndTime}
                      </span>
                    )}
                  </td>
                  <td>
                    {edge.scheduleType === 'FIXED' ? (
                      <span style={{
                        display: 'inline-flex', alignItems: 'center', gap: '0.25rem',
                        fontSize: '0.72rem', fontWeight: 600, color: '#3b82f6', cursor: 'pointer',
                      }} onClick={e => { e.stopPropagation(); toggleExpand(); }}>
                        {isExpanded ? <FiChevronDown size={12} /> : <FiChevronRight size={12} />}
                        {edge.trips.length} trip{edge.trips.length !== 1 ? 's' : ''}
                      </span>
                    ) : (
                      <span style={{ fontSize: '0.72rem', opacity: 0.4 }}>—</span>
                    )}
                  </td>
                  <td style={{ fontSize: '0.85rem', fontWeight: 500 }}>{formatDuration(edge.estimatedDurationMin)}</td>
                  <td>
                    <span style={{
                      fontSize: '0.7rem', fontWeight: 700, padding: '0.2rem 0.55rem', borderRadius: 999,
                      backgroundColor: `${SOURCE_COLORS[edge.source] ?? '#6d7c8a'}15`,
                      color: SOURCE_COLORS[edge.source] ?? '#6d7c8a',
                    }}>
                      {edge.source}
                    </span>
                  </td>
                  <td>
                    <span style={{
                      display: 'inline-flex', alignItems: 'center', gap: '0.25rem',
                      fontSize: '0.7rem', fontWeight: 700, padding: '0.2rem 0.55rem', borderRadius: 999,
                      backgroundColor: edge.status === 'ACTIVE' ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)',
                      color: edge.status === 'ACTIVE' ? '#22c55e' : '#ef4444',
                    }}>
                      <span style={{ width: 5, height: 5, borderRadius: '50%', backgroundColor: edge.status === 'ACTIVE' ? '#22c55e' : '#ef4444' }} />
                      {edge.status === 'ACTIVE' ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td onClick={e => e.stopPropagation()}>
                    <DropdownMenu.Root>
                      <DropdownMenu.Trigger asChild>
                        <button className={paneStyles.rowActionBtn}>
                          <FiMoreVertical size={16} />
                        </button>
                      </DropdownMenu.Trigger>
                      <DropdownMenu.Portal>
                        <DropdownMenu.Content side="left" sideOffset={6} align="start" className={s.dropdownContent} style={{ minWidth: 150 }}>
                          <DropdownMenu.Item className={s.dropdownItem} onSelect={() => openEdit(edge)}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiEdit2 size={13} /> Edit</span>
                          </DropdownMenu.Item>
                          <DropdownMenu.Item className={s.dropdownItem} onSelect={() => toggleStatus(edge)}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                              {edge.status === 'ACTIVE' ? <><FiAlertCircle size={13} /> Deactivate</> : <><FiCheck size={13} /> Activate</>}
                            </span>
                          </DropdownMenu.Item>
                          <DropdownMenu.Separator className={s.dropdownSep} />
                          <DropdownMenu.Item className={s.dropdownItemDanger} onSelect={() => confirmDelete(edge)}>
                            <FiTrash2 size={13} /> Delete
                          </DropdownMenu.Item>
                        </DropdownMenu.Content>
                      </DropdownMenu.Portal>
                    </DropdownMenu.Root>
                  </td>
                </tr>
                {/* ── Expanded Trip Sub-Rows ── */}
                {isExpanded && edge.trips.map(trip => (
                  <tr key={trip.id} style={{ backgroundColor: 'rgba(59,130,246,0.03)' }}>
                    <td></td>
                    <td style={{ paddingLeft: '1.5rem' }}>
                      <span style={{ fontFamily: 'monospace', fontSize: '0.78rem', fontWeight: 600, color: '#3b82f6' }}>
                        {trip.serviceCode || '—'}
                      </span>
                    </td>
                    <td colSpan={2} style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                      {trip.departureTime} → {trip.arrivalTime}
                    </td>
                    <td><OperatingDays mask={trip.operatingDaysMask} /></td>
                    <td style={{ fontSize: '0.78rem', opacity: trip.estimatedCostCents != null ? 1 : 0.35 }}>
                      {formatCost(trip.estimatedCostCents)}
                    </td>
                    <td colSpan={2}>
                      {trip.validFrom && (
                        <span style={{ fontSize: '0.68rem', opacity: 0.5 }}>
                          {trip.validFrom} → {trip.validTo || '∞'}
                        </span>
                      )}
                    </td>
                    <td></td>
                    <td></td>
                    <td></td>
                  </tr>
                ))}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* ── Pagination Bar ── */}
      <div className={s.paginationBar}>
        <div className={s.paginationLeft}>
          <span style={{ fontSize: '0.75rem' }}>{safePage * pageSize + 1}–{Math.min((safePage + 1) * pageSize, totalElements)} of {totalElements}</span>
        </div>
        <div className={s.paginationCenter}>
          <button className={s.pageBtn} disabled={safePage <= 0} onClick={() => goPage(0)}><FiChevronsLeft size={14} /></button>
          <button className={s.pageBtn} disabled={safePage <= 0} onClick={() => goPage(safePage - 1)}><FiChevronLeft size={14} /></button>
          <span className={s.paginationInfo}>Page {safePage + 1} of {totalPages}</span>
          <button className={s.pageBtn} disabled={safePage >= totalPages - 1} onClick={() => goPage(safePage + 1)}><FiChevronRight size={14} /></button>
          <button className={s.pageBtn} disabled={safePage >= totalPages - 1} onClick={() => goPage(totalPages - 1)}><FiChevronsRight size={14} /></button>
        </div>
        <div className={s.paginationRight}>
          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <button className={s.pageSizeBtn}>{pageSize} / page <FiChevronDown size={12} /></button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content sideOffset={4} align="end" side="top" className={s.dropdownContent} style={{ minWidth: 100 }}>
                {PAGE_SIZES.map(sz => (
                  <DropdownMenu.Item key={sz} className={s.dropdownItem} onSelect={() => { setPageSize(sz); setPage(0); }}>
                    {sz} / page {pageSize === sz && <FiCheck size={14} />}
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>
        </div>
      </div>

      {/* ══════════ Add / Edit Dialog ══════════ */}
      <Dialog.Root open={dialogOpen} onOpenChange={setDialogOpen}>
        <Dialog.Portal>
          <Dialog.Overlay className={s.overlay} />
          <Dialog.Content className={s.dialogContent}>
            <div className={s.dialogHeader}>
              <Dialog.Title className={s.dialogTitle}>
                {editingId ? 'Edit Route' : 'New Route'}
              </Dialog.Title>
              <Dialog.Close asChild>
                <button className={s.dialogClose}><FiX size={18} /></button>
              </Dialog.Close>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.55rem' }}>
              {/* ── Route ── */}
              <div className={s.sectionTitle}>Route</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>Origin *</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} placeholder="e.g. IST — Istanbul Airport"
                    value={form.originLabel} onChange={e => setForm(f => ({ ...f, originLabel: e.target.value }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>Destination *</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} placeholder="e.g. LHR — London Heathrow"
                    value={form.destinationLabel} onChange={e => setForm(f => ({ ...f, destinationLabel: e.target.value }))} />
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>Transport Mode</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{ maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                          <span style={{ color: MODE_META[form.transportModeCode]?.color, display: 'flex' }}>{MODE_META[form.transportModeCode]?.icon}</span>
                          {MODE_META[form.transportModeCode]?.label ?? form.transportModeCode}
                        </span>
                        <FiChevronDown size={12} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {Object.entries(MODE_META).map(([code, meta]) => (
                          <DropdownMenu.Item key={code} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, transportModeCode: code }))}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                              <span style={{ color: meta.color, display: 'flex' }}>{meta.icon}</span> {meta.label}
                            </span>
                            {form.transportModeCode === code && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
                <div>
                  <label className={s.fieldLabel}>Provider</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} placeholder="e.g. TK, DB"
                    value={form.providerCode} onChange={e => setForm(f => ({ ...f, providerCode: e.target.value.toUpperCase() }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>Schedule Type</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{ maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}>
                        {form.scheduleType}
                        <FiChevronDown size={12} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {(['FIXED', 'FREQUENCY', 'ON_DEMAND'] as ScheduleType[]).map(st => (
                          <DropdownMenu.Item key={st} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, scheduleType: st }))}>
                            {st} {form.scheduleType === st && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
              </div>

              <div className={s.sectionTitle}>Schedule</div>
              <div>
                <label className={s.fieldLabel}>Operating Days</label>
                <div style={{ display: 'flex', gap: '0.35rem', marginTop: '0.15rem' }}>
                  {DAY_LABELS.map((label, i) => (
                    <button key={i} type="button"
                      className={`${s.dayToggle} ${(form.operatingDaysMask & (1 << i)) ? s.active : ''}`}
                      onClick={() => toggleDay(i)} title={DAY_NAMES[i]}>
                      {label}
                    </button>
                  ))}
                </div>
              </div>
              {form.scheduleType === 'FREQUENCY' && (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.6rem' }}>
                  <div>
                    <label className={s.fieldLabel}>Frequency (min) *</label>
                    <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} type="number" min={1} placeholder="e.g. 15"
                      value={form.frequencyMinutes} onChange={e => setForm(f => ({ ...f, frequencyMinutes: e.target.value }))} />
                  </div>
                  <div>
                    <label className={s.fieldLabel}>Operating Start</label>
                    <TimePicker value={form.operatingStartTime} onChange={v => setForm(f => ({ ...f, operatingStartTime: v }))} />
                  </div>
                  <div>
                    <label className={s.fieldLabel}>Operating End</label>
                    <TimePicker value={form.operatingEndTime} onChange={v => setForm(f => ({ ...f, operatingEndTime: v }))} />
                  </div>
                </div>
              )}
              {form.scheduleType === 'FIXED' && (
                <div style={{ fontSize: '0.78rem', opacity: 0.5, fontStyle: 'italic', padding: '0.25rem 0' }}>
                  ℹ️  Individual trip departures can be managed after saving the route.
                </div>
              )}

              {/* ── Estimates ── */}
              <div className={s.sectionTitle}>Estimates</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>Duration (min)</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} type="number" min={0}
                    value={form.estimatedDurationMin} onChange={e => setForm(f => ({ ...f, estimatedDurationMin: e.target.value }))} />
                </div>

                <div>
                  <label className={s.fieldLabel}>Distance (m)</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} type="number" min={0}
                    value={form.distanceM} onChange={e => setForm(f => ({ ...f, distanceM: e.target.value }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>CO₂ (g)</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} type="number" min={0}
                    value={form.co2Grams} onChange={e => setForm(f => ({ ...f, co2Grams: e.target.value }))} />
                </div>
              </div>

              {/* ── Meta ── */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>Source</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{ maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                          <span style={{ color: SOURCE_META[form.source].color, display: 'flex' }}>{SOURCE_META[form.source].icon}</span>
                          {SOURCE_META[form.source].label}
                        </span>
                        <FiChevronDown size={12} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {(Object.keys(SOURCE_META) as EdgeSource[]).map(src => (
                          <DropdownMenu.Item key={src} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, source: src }))}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                              <span style={{ color: SOURCE_META[src].color, display: 'flex' }}>{SOURCE_META[src].icon}</span>
                              <span>
                                <span style={{ fontWeight: 500 }}>{SOURCE_META[src].label}</span>
                                <span style={{ fontSize: '0.68rem', opacity: 0.45, marginLeft: 6 }}>{SOURCE_META[src].desc}</span>
                              </span>
                            </span>
                            {form.source === src && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
                <div>
                  <label className={s.fieldLabel}>Status</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{ maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}>
                        <span style={{ color: form.status === 'ACTIVE' ? '#22c55e' : '#ef4444' }}>{form.status}</span>
                        <FiChevronDown size={12} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {(['ACTIVE', 'INACTIVE'] as EdgeStatus[]).map(st => (
                          <DropdownMenu.Item key={st} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, status: st }))}>
                            <span style={{ color: st === 'ACTIVE' ? '#22c55e' : '#ef4444' }}>{st}</span>
                            {form.status === st && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
              </div>
            </div>

            <div className={s.dialogFooter}>
              <Dialog.Close asChild><button className={s.btnCancel}>Cancel</button></Dialog.Close>
              <button className={s.btnPrimary} onClick={handleSave}>
                {editingId ? 'Save Changes' : 'Create Connection'}
              </button>
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>

      {/* ══════════ Delete Confirm ══════════ */}
      <Dialog.Root open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <Dialog.Portal>
          <Dialog.Overlay className={s.overlay} />
          <Dialog.Content className={s.dialogContentSmall}>
            <div className={s.dialogHeader}>
              <Dialog.Title className={s.dialogTitle}>Delete Connection</Dialog.Title>
              <Dialog.Close asChild><button className={s.dialogClose}><FiX size={18} /></button></Dialog.Close>
            </div>
            <p style={{ fontSize: '0.85rem', margin: '0 0 0.5rem', lineHeight: 1.5 }}>
              Delete connection <strong>{deletingEdge?.originLabel}</strong> → <strong>{deletingEdge?.destinationLabel}</strong>?
            </p>
            <div className={s.dialogFooter}>
              <Dialog.Close asChild><button className={s.btnCancel}>Cancel</button></Dialog.Close>
              <button className={s.btnDanger} onClick={handleDelete}>Delete</button>
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>

      {/* ── Toast ── */}
      <Toast.Viewport style={{ position: 'fixed', bottom: 24, right: 24, zIndex: 999, display: 'flex', flexDirection: 'column', gap: 8 }} />
      <Toast.Root className={`${s.toast} ${s[toastVariant]}`} open={toastOpen} onOpenChange={setToastOpen} duration={3000}>
        <Toast.Description style={{ fontSize: '0.85rem', fontWeight: 500 }}>
          {toastVariant === 'success' ? <FiCheck size={16} /> : <FiAlertCircle size={16} />}
          {toastMsg}
        </Toast.Description>
      </Toast.Root>
    </Toast.Provider>
  );
};
