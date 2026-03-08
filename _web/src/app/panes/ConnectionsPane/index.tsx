import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from './ConnectionsPane.module.scss';
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
import { apiGet, apiPost, apiPut, apiDelete } from '../../api/client';
import { VaulDrawer } from '../../components/shared';

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

/* ═══════════ resolveEnum + mapEdge ═══════════ */
const resolveEnum = (val: unknown): string => {
  if (val && typeof val === 'object' && 'value' in (val as Record<string, unknown>))
    return (val as Record<string, string>).value;
  return (val as string) || '';
};

const mapEdge = (e: any): TransportEdge => ({
  id: e.id,
  originLocationId: e.originLocation?.id || '',
  originLabel: `${e.originLocation?.iataCode ? e.originLocation.iataCode + ' — ' : ''}${e.originLocation?.name || ''}`,
  destinationLocationId: e.destinationLocation?.id || '',
  destinationLabel: `${e.destinationLocation?.iataCode ? e.destinationLocation.iataCode + ' — ' : ''}${e.destinationLocation?.name || ''}`,
  transportModeCode: resolveEnum(e.transportMode?.code) || e.transportMode?.code || '',
  providerCode: e.provider?.code || null,
  scheduleType: (resolveEnum(e.scheduleType) || e.scheduleType || 'FIXED') as ScheduleType,
  operatingDaysMask: e.operatingDaysMask ?? 127,
  operatingStartTime: e.operatingStartTime || null,
  operatingEndTime: e.operatingEndTime || null,
  frequencyMinutes: e.frequencyMinutes ?? null,
  status: (resolveEnum(e.status) || e.status || 'ACTIVE') as EdgeStatus,
  source: (resolveEnum(e.source) || e.source || 'MANUAL') as EdgeSource,
  estimatedDurationMin: e.estimatedDurationMin ?? null,
  distanceM: e.distanceM ?? null,
  co2Grams: e.co2Grams ?? null,
  trips: (e.trips || []).filter((t: any) => !t.deleted).map((t: any) => ({
    id: t.id,
    serviceCode: t.serviceCode || null,
    departureTime: t.departureTime || '',
    arrivalTime: t.arrivalTime || '',
    operatingDaysMask: t.operatingDaysMask ?? 127,
    validFrom: t.validFrom || null,
    validTo: t.validTo || null,
    estimatedCostCents: t.estimatedCostCents ?? null,
  })),
  version: e.version ?? 1,
  createdDate: e.createdDate || '',
  lastModifiedDate: e.lastModifiedDate || null,
  deleted: e.deleted ?? false,
});


const PAGE_SIZES = [10, 20, 50];

type FormState = {
  originLabel: string; originLocationId: string;
  destinationLabel: string; destinationLocationId: string;
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
  originLabel: '', originLocationId: '',
  destinationLabel: '', destinationLocationId: '',
  transportModeCode: 'FLIGHT', providerCode: '',
  scheduleType: 'FIXED',
  operatingDaysMask: 127,
  operatingStartTime: '', operatingEndTime: '',
  frequencyMinutes: '',
  status: 'ACTIVE', source: 'MANUAL',
  estimatedDurationMin: '',
  distanceM: '', co2Grams: '',
});

interface LocOption { id: string; label: string; iata: string | null; type: string; }
interface ModeOption { id: string; code: string; name: string; }
interface ProviderOption { id: string; code: string; name: string; }

/* ═══════════════════════════════════════════════
   COMPONENT
   ═══════════════════════════════════════════════ */
export const ConnectionsPane: React.FC = () => {
  const [edges, setEdges] = useState<TransportEdge[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [modeFilter, setModeFilter] = useState<string>('ALL');
  const [sourceFilter, setSourceFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState('');
  const [toastVariant, setToastVariant] = useState<'success' | 'error'>('success');
  const [deleteDrawerOpen, setDeleteDrawerOpen] = useState(false);
  const [deletingEdge, setDeletingEdge] = useState<TransportEdge | null>(null);
  const [expandedRoutes, setExpandedRoutes] = useState<Set<string>>(new Set());

  /* ═══ Reference data for dropdowns ═══ */
  const [modes, setModes] = useState<ModeOption[]>([]);
  const [providers, setProviders] = useState<ProviderOption[]>([]);
  const [originSugg, setOriginSugg] = useState<LocOption[]>([]);
  const [destSugg, setDestSugg] = useState<LocOption[]>([]);
  const [showOriginSugg, setShowOriginSugg] = useState(false);
  const [showDestSugg, setShowDestSugg] = useState(false);
  const originTimer = useRef<ReturnType<typeof setTimeout>>();
  const destTimer = useRef<ReturnType<typeof setTimeout>>();

  const searchLocations = useCallback(async (q: string, target: 'origin' | 'dest') => {
    if (q.length < 2) { target === 'origin' ? setOriginSugg([]) : setDestSugg([]); return; }
    try {
      const res = await apiPost<{ content: any[] }>('/inventory/locations/search?page=0&size=15', { name: q });
      const opts: LocOption[] = (res?.content || []).map((l: any) => ({
        id: l.id, label: `${l.iataCode ? l.iataCode + ' \u2014 ' : ''}${l.name}`, iata: l.iataCode, type: l.type,
      }));
      if (target === 'origin') { setOriginSugg(opts); setShowOriginSugg(true); }
      else { setDestSugg(opts); setShowDestSugg(true); }
    } catch { /* ignore */ }
  }, []);
  const showToast = useCallback((msg: string, variant: 'success' | 'error' = 'success') => {
    setToastMsg(msg); setToastVariant(variant); setToastOpen(true);
  }, []);

  /* ═══ Load reference data once ═══ */
  useEffect(() => {
    (async () => {
      try {
        const [rawModes, rawProviders] = await Promise.all([
          apiGet<any[]>('/transport/modes', { all: true }),
          apiGet<any[]>('/inventory/providers'),
        ]);
        setModes((rawModes || []).map((m: any) => ({ id: m.id, code: m.code || resolveEnum(m.code), name: m.name })));
        setProviders((rawProviders || []).map((p: any) => ({ id: p.id, code: p.code, name: p.name })));
      } catch (err) { console.error('Failed to load reference data:', err); }
    })();
  }, []);

  /* ═══ Load edges from API (server-side paging) ═══ */
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const body: Record<string, unknown> = {};
      if (statusFilter !== 'ALL') body.status = statusFilter;
      const result = await apiPost<any>(`/transport/edges/search?page=${page}&size=${pageSize}&sort=id,desc`, body);
      const items = (result?.content || []).map(mapEdge);
      setEdges(items);
      setTotalElements(result?.totalElements ?? items.length);
      setTotalPages(result?.totalPages ?? 1);
    } catch (err) { console.error('Failed to load edges:', err); }
    finally { setLoading(false); }
  }, [page, pageSize, statusFilter]);

  useEffect(() => { loadData(); }, [loadData]);

  /* Debounce search (300ms) */
  useEffect(() => {
    const t = setTimeout(() => { setDebouncedSearch(search); setPage(0); }, 300);
    return () => clearTimeout(t);
  }, [search]);

  const onOriginInput = (val: string) => {
    setForm(f => ({ ...f, originLabel: val, originLocationId: '' }));
    clearTimeout(originTimer.current);
    originTimer.current = setTimeout(() => searchLocations(val, 'origin'), 300);
  };
  const onDestInput = (val: string) => {
    setForm(f => ({ ...f, destinationLabel: val, destinationLocationId: '' }));
    clearTimeout(destTimer.current);
    destTimer.current = setTimeout(() => searchLocations(val, 'dest'), 300);
  };
  const pickOrigin = (o: LocOption) => { setForm(f => ({ ...f, originLabel: o.label, originLocationId: o.id })); setShowOriginSugg(false); };
  const pickDest = (o: LocOption) => { setForm(f => ({ ...f, destinationLabel: o.label, destinationLocationId: o.id })); setShowDestSugg(false); };

  /* ═══ Client-side filtering (text search, mode, source) ═══ */
  const filtered = useMemo(() => {
    let result = edges.filter(e => !e.deleted);
    if (debouncedSearch) {
      const q = debouncedSearch.toLowerCase();
      result = result.filter(e =>
        e.originLabel.toLowerCase().includes(q) || e.destinationLabel.toLowerCase().includes(q) ||
        e.trips.some(t => (t.serviceCode ?? '').toLowerCase().includes(q)) || (e.providerCode ?? '').toLowerCase().includes(q)
      );
    }
    if (modeFilter !== 'ALL') result = result.filter(e => e.transportModeCode === modeFilter);
    if (sourceFilter !== 'ALL') result = result.filter(e => e.source === sourceFilter);
    return result;
  }, [edges, debouncedSearch, modeFilter, sourceFilter]);

  const goPage = (p: number) => setPage(Math.max(0, Math.min(p, totalPages - 1)));

  /* ═══ CRUD ═══ */
  const openAdd = () => { setEditingId(null); setForm(emptyForm()); setDrawerOpen(true); };
  const openEdit = (e: TransportEdge) => {
    setEditingId(e.id);
    setForm({
      originLabel: e.originLabel, originLocationId: e.originLocationId,
      destinationLabel: e.destinationLabel, destinationLocationId: e.destinationLocationId,
      transportModeCode: e.transportModeCode, providerCode: e.providerCode ?? '',
      scheduleType: e.scheduleType, operatingDaysMask: e.operatingDaysMask,
      operatingStartTime: e.operatingStartTime ?? '', operatingEndTime: e.operatingEndTime ?? '',
      frequencyMinutes: e.frequencyMinutes != null ? String(e.frequencyMinutes) : '',
      status: e.status, source: e.source,
      estimatedDurationMin: e.estimatedDurationMin != null ? String(e.estimatedDurationMin) : '',
      distanceM: e.distanceM != null ? String(e.distanceM) : '',
      co2Grams: e.co2Grams != null ? String(e.co2Grams) : '',
    });
    setDrawerOpen(true);
  };

  const handleSave = async () => {
    const n = (v: string) => v ? parseInt(v) : null;
    const sharedBody: Record<string, unknown> = {
      scheduleType: form.scheduleType,
      operatingDaysMask: form.operatingDaysMask,
      operatingStartTime: form.operatingStartTime || null,
      operatingEndTime: form.operatingEndTime || null,
      frequencyMinutes: n(form.frequencyMinutes),
      status: form.status,
      source: form.source,
      estimatedDurationMin: n(form.estimatedDurationMin),
      distanceM: n(form.distanceM),
      co2Grams: n(form.co2Grams),
    };
    try {
      if (editingId) {
        await apiPut(`/transport/edges/${editingId}`, sharedBody);
        showToast('Route updated.');
      } else {
        /* ── Create new edge ── */
        if (!form.originLocationId || !form.destinationLocationId) {
          showToast('Please select Origin and Destination from the suggestions.', 'error'); return;
        }
        const modeObj = modes.find(m => m.code === form.transportModeCode);
        if (!modeObj) { showToast('Please select a Transport Mode.', 'error'); return; }
        const provObj = form.providerCode ? providers.find(p => p.code === form.providerCode) : null;
        const createBody = {
          ...sharedBody,
          originLocation: { id: form.originLocationId },
          destinationLocation: { id: form.destinationLocationId },
          transportMode: { id: modeObj.id },
          ...(provObj ? { provider: { id: provObj.id } } : {}),
        };
        await apiPost('/transport/edges', createBody);
        showToast('Route created.');
      }
      setDrawerOpen(false);
      loadData();
    } catch { /* interceptor handles toast */ }
  };

  const confirmDelete = (e: TransportEdge) => { setDeletingEdge(e); setDeleteDrawerOpen(true); };
  const handleDelete = async () => {
    if (!deletingEdge) return;
    try {
      await apiDelete(`/transport/edges/${deletingEdge.id}`);
      showToast('Connection deleted.');
      loadData();
    } catch { /* interceptor handles toast */ }
    setDeleteDrawerOpen(false); setDeletingEdge(null);
  };
  const toggleStatus = async (e: TransportEdge) => {
    const newStatus: EdgeStatus = e.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await apiPut(`/transport/edges/${e.id}`, { status: newStatus });
      showToast(`Connection ${newStatus === 'ACTIVE' ? 'activated' : 'deactivated'}.`);
      loadData();
    } catch { /* interceptor handles toast */ }
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

      {loading && <div style={{ textAlign: 'center', padding: '3rem', opacity: 0.5 }}>Loading...</div>}

      {/* ── Table ── */}
      {!loading && <div className={paneStyles.tableWrapper} style={{ maxHeight: 'calc(100vh - 280px)', minHeight: 'calc(100vh - 280px)', overflow: 'auto' }}>
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
            {filtered.length === 0 && (
              <tr><td colSpan={11} style={{ textAlign: 'center', padding: '2rem', opacity: 0.4 }}>No routes found.</td></tr>
            )}
            {filtered.map(edge => {
              const meta = MODE_META[edge.transportModeCode] ?? { icon: null, color: '#6d7c8a', label: edge.transportModeCode };
              const isExpanded = expandedRoutes.has(edge.id);
              const toggleExpand = () => setExpandedRoutes(prev => {
                const next = new Set(prev);
                next.has(edge.id) ? next.delete(edge.id) : next.add(edge.id);
                return next;
              });
              const schedBadge = {
                FIXED: { color: '#3b82f6', label: 'Fixed' },
                FREQUENCY: { color: '#8b5cf6', label: `Every ${edge.frequencyMinutes ?? '?'}m` },
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
                    <td>
                      <button style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#ef4444', opacity: 0.6, padding: '0.2rem' }}
                        title="Delete trip"
                        onClick={async () => {
                          try { await apiDelete(`/transport/trips/${trip.id}`); showToast('Trip deleted.'); loadData(); }
                          catch { /* interceptor */ }
                        }}>
                        <FiTrash2 size={13} />
                      </button>
                    </td>
                  </tr>
                ))}
                {/* ── Add Trip Row ── */}
                {isExpanded && (
                  <tr style={{ backgroundColor: 'rgba(34,197,94,0.04)' }}>
                    <td></td>
                    <td style={{ paddingLeft: '1.5rem' }}>
                      <input placeholder="Code" style={{ width: 70, fontSize: '0.75rem', padding: '0.25rem 0.35rem', border: '1px solid rgba(128,128,128,0.2)', borderRadius: 4, background: 'transparent', color: 'inherit', fontFamily: 'monospace' }}
                        id={`trip-code-${edge.id}`} />
                    </td>
                    <td>
                      <input type="time" style={{ fontSize: '0.75rem', padding: '0.25rem', border: '1px solid rgba(128,128,128,0.2)', borderRadius: 4, background: 'transparent', color: 'inherit' }}
                        id={`trip-dep-${edge.id}`} />
                    </td>
                    <td>
                      <input type="time" style={{ fontSize: '0.75rem', padding: '0.25rem', border: '1px solid rgba(128,128,128,0.2)', borderRadius: 4, background: 'transparent', color: 'inherit' }}
                        id={`trip-arr-${edge.id}`} />
                    </td>
                    <td>
                      <input type="number" placeholder="127" style={{ width: 40, fontSize: '0.75rem', padding: '0.25rem', border: '1px solid rgba(128,128,128,0.2)', borderRadius: 4, background: 'transparent', color: 'inherit', textAlign: 'center' }}
                        id={`trip-days-${edge.id}`} defaultValue="127" />
                    </td>
                    <td>
                      <input type="number" placeholder="cents" style={{ width: 60, fontSize: '0.75rem', padding: '0.25rem', border: '1px solid rgba(128,128,128,0.2)', borderRadius: 4, background: 'transparent', color: 'inherit' }}
                        id={`trip-cost-${edge.id}`} />
                    </td>
                    <td colSpan={4}></td>
                    <td>
                      <button style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#22c55e', padding: '0.2rem' }}
                        title="Add trip"
                        onClick={async () => {
                          const code = (document.getElementById(`trip-code-${edge.id}`) as HTMLInputElement)?.value || '';
                          const dep = (document.getElementById(`trip-dep-${edge.id}`) as HTMLInputElement)?.value || '';
                          const arr = (document.getElementById(`trip-arr-${edge.id}`) as HTMLInputElement)?.value || '';
                          const days = parseInt((document.getElementById(`trip-days-${edge.id}`) as HTMLInputElement)?.value || '127');
                          const cost = (document.getElementById(`trip-cost-${edge.id}`) as HTMLInputElement)?.value;
                          if (!dep || !arr) { showToast('Departure and arrival times are required.', 'error'); return; }
                          try {
                            await apiPost(`/transport/edges/${edge.id}/trips`, {
                              serviceCode: code || null,
                              departureTime: dep,
                              arrivalTime: arr,
                              operatingDaysMask: days,
                              estimatedCostCents: cost ? parseInt(cost) : null,
                            });
                            showToast('Trip added.');
                            loadData();
                          } catch { /* interceptor */ }
                        }}>
                        <FiPlus size={14} />
                      </button>
                    </td>
                  </tr>
                )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>}

      {/* ── Pagination Bar ── */}
      <div className={s.paginationBar}>
        <div className={s.paginationLeft}>
          <span style={{ fontSize: '0.75rem' }}>{page * pageSize + 1}–{Math.min((page + 1) * pageSize, totalElements)} of {totalElements}</span>
        </div>
        <div className={s.paginationCenter}>
          <button className={s.pageBtn} disabled={page <= 0} onClick={() => goPage(0)}><FiChevronsLeft size={14} /></button>
          <button className={s.pageBtn} disabled={page <= 0} onClick={() => goPage(page - 1)}><FiChevronLeft size={14} /></button>
          <span className={s.paginationInfo}>Page {page + 1} of {totalPages}</span>
          <button className={s.pageBtn} disabled={page >= totalPages - 1} onClick={() => goPage(page + 1)}><FiChevronRight size={14} /></button>
          <button className={s.pageBtn} disabled={page >= totalPages - 1} onClick={() => goPage(totalPages - 1)}><FiChevronsRight size={14} /></button>
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

      {/* ══════════ Add / Edit Drawer ══════════ */}
      <VaulDrawer open={drawerOpen} onOpenChange={setDrawerOpen}
        title={editingId ? 'Edit Route' : 'New Route'} width={560}
        footer={<>
          <button className={s.btnCancel} onClick={() => setDrawerOpen(false)}>Cancel</button>
          <button className={s.btnPrimary} onClick={handleSave}>
            {editingId ? 'Save Changes' : 'Create Connection'}
          </button>
        </>}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.55rem' }}>
              {/* ── Route ── */}
              <div className={s.sectionTitle}>Route</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                <div style={{ position: 'relative' }}>
                  <label className={s.fieldLabel}>Origin *{form.originLocationId && <FiCheck size={12} style={{ color: '#22c55e', marginLeft: 4 }} />}</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} placeholder="Type to search... (e.g. IST, Barcelona)"
                    value={form.originLabel} onChange={e => onOriginInput(e.target.value)}
                    onFocus={() => originSugg.length > 0 && setShowOriginSugg(true)}
                    onBlur={() => setTimeout(() => setShowOriginSugg(false), 200)}
                    readOnly={!!editingId} />
                  {showOriginSugg && originSugg.length > 0 && (
                    <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 50, background: 'var(--surface, #1a1a2e)', border: '1px solid rgba(128,128,128,0.2)', borderRadius: 8, maxHeight: 200, overflowY: 'auto', boxShadow: '0 8px 24px rgba(0,0,0,0.3)' }}>
                      {originSugg.map(o => (
                        <div key={o.id} style={{ padding: '0.45rem 0.6rem', cursor: 'pointer', fontSize: '0.78rem', borderBottom: '1px solid rgba(128,128,128,0.08)' }}
                          onMouseDown={() => pickOrigin(o)}>
                          <span style={{ opacity: 0.4, fontSize: '0.65rem', marginRight: 6 }}>{o.type}</span>{o.label}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
                <div style={{ position: 'relative' }}>
                  <label className={s.fieldLabel}>Destination *{form.destinationLocationId && <FiCheck size={12} style={{ color: '#22c55e', marginLeft: 4 }} />}</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} placeholder="Type to search... (e.g. LHR, Brussels)"
                    value={form.destinationLabel} onChange={e => onDestInput(e.target.value)}
                    onFocus={() => destSugg.length > 0 && setShowDestSugg(true)}
                    onBlur={() => setTimeout(() => setShowDestSugg(false), 200)}
                    readOnly={!!editingId} />
                  {showDestSugg && destSugg.length > 0 && (
                    <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 50, background: 'var(--surface, #1a1a2e)', border: '1px solid rgba(128,128,128,0.2)', borderRadius: 8, maxHeight: 200, overflowY: 'auto', boxShadow: '0 8px 24px rgba(0,0,0,0.3)' }}>
                      {destSugg.map(o => (
                        <div key={o.id} style={{ padding: '0.45rem 0.6rem', cursor: 'pointer', fontSize: '0.78rem', borderBottom: '1px solid rgba(128,128,128,0.08)' }}
                          onMouseDown={() => pickDest(o)}>
                          <span style={{ opacity: 0.4, fontSize: '0.65rem', marginRight: 6 }}>{o.type}</span>{o.label}
                        </div>
                      ))}
                    </div>
                  )}
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
                  </DropdownMenu.Root>
                </div>
                <div>
                  <label className={s.fieldLabel}>Provider</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{ maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}>
                        {form.providerCode ? `${form.providerCode}` : <span style={{ opacity: 0.4 }}>Select provider...</span>}
                        <FiChevronDown size={12} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent} style={{ maxHeight: 250, overflowY: 'auto' }}>
                        <DropdownMenu.Item className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, providerCode: '' }))}>
                          <span style={{ opacity: 0.5 }}>None</span>
                        </DropdownMenu.Item>
                        {providers.map(p => (
                          <DropdownMenu.Item key={p.id} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, providerCode: p.code }))}>
                            {p.code} — {p.name}
                            {form.providerCode === p.code && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                  </DropdownMenu.Root>
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
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {(['FIXED', 'FREQUENCY', 'ON_DEMAND'] as ScheduleType[]).map(st => (
                          <DropdownMenu.Item key={st} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, scheduleType: st }))}>
                            {st} {form.scheduleType === st && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
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
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {(['ACTIVE', 'INACTIVE'] as EdgeStatus[]).map(st => (
                          <DropdownMenu.Item key={st} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, status: st }))}>
                            <span style={{ color: st === 'ACTIVE' ? '#22c55e' : '#ef4444' }}>{st}</span>
                            {form.status === st && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                  </DropdownMenu.Root>
                </div>
              </div>
            </div>
      </VaulDrawer>

      {/* ══════════ Delete Confirm Drawer ══════════ */}
      <VaulDrawer open={deleteDrawerOpen} onOpenChange={setDeleteDrawerOpen}
        title="Delete Connection" width={400}
        footer={<>
          <button className={s.btnCancel} onClick={() => setDeleteDrawerOpen(false)}>Cancel</button>
          <button className={s.btnDanger} onClick={handleDelete}>Delete</button>
        </>}>
            <p style={{ fontSize: '0.85rem', margin: '0 0 0.5rem', lineHeight: 1.5 }}>
              Delete connection <strong>{deletingEdge?.originLabel}</strong> → <strong>{deletingEdge?.destinationLabel}</strong>?
            </p>
      </VaulDrawer>

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
