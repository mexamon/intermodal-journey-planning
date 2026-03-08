import React, { useState, useEffect, useMemo, useCallback } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from '../TransportModesPane/TransportModesPane.module.scss';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import * as Toast from '@radix-ui/react-toast';
import {
  FiSearch, FiPlus, FiEdit2, FiTrash2, FiMoreVertical, FiX,
  FiCheck, FiAlertCircle, FiMapPin, FiGlobe, FiCircle,
  FiChevronDown, FiFilter,
} from 'react-icons/fi';
import {
  MdLocalTaxi, MdDirectionsSubway, MdDirectionsBus, MdTrain,
  MdDirectionsWalk, MdFlight, MdDirectionsBoat, MdPedalBike,
} from 'react-icons/md';
import { apiGet, apiPost, apiPut, apiDelete } from '../../api/client';
import { VaulDrawer } from '../../components/shared';

/* ═══════════════════════════════════════════════
   Types & Helpers
   ═══════════════════════════════════════════════ */
type AreaType = 'RADIUS' | 'CITY' | 'COUNTRY' | 'GLOBAL';

const resolveEnum = (val: unknown): string => {
  if (val && typeof val === 'object' && 'value' in (val as Record<string, unknown>)) {
    return (val as Record<string, string>).value;
  }
  return (val as string) || '';
};

interface ServiceArea {
  id: string;
  transportMode: { id: string; code: string; name: string; colorHex: string } | null;
  provider: { id: string; code: string; name: string } | null;
  name: string;
  areaType: AreaType | { code: string; desc: string; value: string };
  centerLat: number | null;
  centerLon: number | null;
  radiusM: number | null;
  countryIsoCode: string | null;
  city: string | null;
  isActive: boolean;
  configJson: string | null;
  version: number;
  createdDate: string;
}

interface TransportMode { id: string; code: string; name: string; colorHex: string; }
interface Provider { id: string; code: string; name: string; }
interface RefCountry { id: string; isoCode: string; name: string; continent: string | null; }

interface FormState {
  name: string;
  areaType: AreaType;
  transportModeId: string;
  providerId: string;
  centerLat: string;
  centerLon: string;
  radiusM: string;
  countryIsoCode: string;
  city: string;
  isActive: boolean;
  configJson: string;
}

const emptyForm = (): FormState => ({
  name: '', areaType: 'RADIUS', transportModeId: '', providerId: '',
  centerLat: '', centerLon: '', radiusM: '50000',
  countryIsoCode: '', city: '', isActive: true,
  configJson: JSON.stringify({
    max_distance_m: 60000,
    pricing: {
      base_fare_cents: 5000, per_km_cents: 3500,
      currency: 'TRY', min_fare_cents: 10000, pricing_type: 'ESTIMATED',
    },
  }, null, 2),
});

const AREA_TYPE_COLORS: Record<AreaType, string> = {
  RADIUS: '#3b82f6', CITY: '#22c55e', COUNTRY: '#f97316', GLOBAL: '#6b7280',
};
const AREA_TYPE_ICONS: Record<AreaType, React.ReactNode> = {
  RADIUS: <FiMapPin size={14} />, CITY: <FiGlobe size={14} />,
  COUNTRY: <FiGlobe size={14} />, GLOBAL: <FiCircle size={14} />,
};
const MODE_ICONS: Record<string, React.ReactNode> = {
  TAXI: <MdLocalTaxi />, UBER: <MdLocalTaxi />, SUBWAY: <MdDirectionsSubway />,
  BUS: <MdDirectionsBus />, TRAIN: <MdTrain />, WALKING: <MdDirectionsWalk />,
  FLIGHT: <MdFlight />, FERRY: <MdDirectionsBoat />, BIKE: <MdPedalBike />,
};
const ALL_AREA_TYPES: AreaType[] = ['RADIUS', 'CITY', 'COUNTRY', 'GLOBAL'];

/* ═══════════════════════════════════════════════
   SearchableDropdown — Radix DropdownMenu, NO Portal
   Renders inline so it works inside Vaul drawer
   ═══════════════════════════════════════════════ */
interface SearchableDropdownProps {
  label: string;
  items: { key: string; label: string; sublabel?: string; icon?: React.ReactNode; color?: string }[];
  value: string;
  onChange: (key: string) => void;
  placeholder?: string;
  allowClear?: boolean;
}
const SearchableDropdown: React.FC<SearchableDropdownProps> = ({
  label, items, value, onChange, placeholder = 'Select...', allowClear,
}) => {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const selected = items.find(i => i.key === value);
  const filtered = useMemo(() => {
    if (!q) return items;
    const lq = q.toLowerCase();
    return items.filter(i =>
      i.label.toLowerCase().includes(lq) || (i.sublabel || '').toLowerCase().includes(lq)
    );
  }, [items, q]);

  return (
    <div>
      <label className={s.fieldLabel}>{label}</label>
      <DropdownMenu.Root open={open} onOpenChange={o => { setOpen(o); if (!o) setQ(''); }}>
        <DropdownMenu.Trigger asChild>
          <button className={paneStyles.formInput} type="button" style={{
            maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center',
            justifyContent: 'space-between', fontSize: '0.85rem', gap: '0.4rem', width: '100%',
          }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {selected ? (
                <>{selected.icon} <span style={{ color: selected.color }}>{selected.label}</span>{selected.sublabel ? ` — ${selected.sublabel}` : ''}</>
              ) : (
                <span style={{ opacity: 0.45 }}>{placeholder}</span>
              )}
            </span>
            <FiChevronDown size={13} style={{ opacity: 0.4, flexShrink: 0, transform: open ? 'rotate(180deg)' : 'none', transition: 'transform 0.15s' }} />
          </button>
        </DropdownMenu.Trigger>
        {/* NO Portal — renders inline inside drawer */}
        <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}
          style={{ minWidth: 240, maxHeight: 260, display: 'flex', flexDirection: 'column' }}
          onCloseAutoFocus={e => e.preventDefault()}>
          <div style={{ padding: '0.35rem 0.5rem', flexShrink: 0 }}>
            <input autoFocus placeholder="Search..." value={q}
              onChange={e => setQ(e.target.value)}
              onClick={e => e.stopPropagation()}
              onKeyDown={e => e.stopPropagation()}
              style={{ width: '100%', border: 'none', background: 'transparent', outline: 'none', fontSize: '0.8rem', color: 'inherit', padding: '0.2rem 0' }}
            />
          </div>
          <div style={{ overflowY: 'auto', flex: 1 }}>
            {allowClear && (
              <DropdownMenu.Item className={s.dropdownItem} onSelect={() => onChange('')}>
                <span style={{ opacity: 0.5 }}>None</span>
                {!value && <FiCheck size={14} />}
              </DropdownMenu.Item>
            )}
            {filtered.length === 0 && (
              <div style={{ padding: '0.6rem', textAlign: 'center', opacity: 0.4, fontSize: '0.78rem' }}>No results</div>
            )}
            {filtered.map(item => (
              <DropdownMenu.Item key={item.key} className={s.dropdownItem} onSelect={() => onChange(item.key)}>
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flex: 1, overflow: 'hidden' }}>
                  {item.icon && <span style={{ color: item.color, display: 'flex', flexShrink: 0 }}>{item.icon}</span>}
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {item.label}{item.sublabel ? ` — ${item.sublabel}` : ''}
                  </span>
                </span>
                {value === item.key && <FiCheck size={14} style={{ flexShrink: 0 }} />}
              </DropdownMenu.Item>
            ))}
          </div>
        </DropdownMenu.Content>
      </DropdownMenu.Root>
    </div>
  );
};

/* ═══════════════════════════════════════════════
   MAIN COMPONENT
   ═══════════════════════════════════════════════ */
export const ServiceAreasPane: React.FC = () => {
  const [areas, setAreas] = useState<ServiceArea[]>([]);
  const [modes, setModes] = useState<TransportMode[]>([]);
  const [providers, setProviders] = useState<Provider[]>([]);
  const [countries, setCountries] = useState<RefCountry[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState<AreaType | 'ALL'>('ALL');
  const [filterMode, setFilterMode] = useState<string>('ALL');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState('');
  const [toastVariant, setToastVariant] = useState<'success' | 'error'>('success');
  const [deleteDrawerOpen, setDeleteDrawerOpen] = useState(false);
  const [deletingArea, setDeletingArea] = useState<ServiceArea | null>(null);

  const showToast = useCallback((msg: string, variant: 'success' | 'error' = 'success') => {
    setToastMsg(msg); setToastVariant(variant); setToastOpen(true);
  }, []);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [areasData, modesData, providersData, countriesData] = await Promise.all([
        apiGet<ServiceArea[]>('/transport/service-areas'),
        apiGet<TransportMode[]>('/transport/modes', { all: true }),
        apiGet<Provider[]>('/inventory/providers'),
        apiGet<RefCountry[]>('/admin/ref/countries'),
      ]);
      setAreas(areasData || []);
      setModes(modesData || []);
      setProviders(providersData || []);
      setCountries(countriesData || []);
    } catch (err) {
      console.error('Failed to load data:', err);
      showToast('Failed to load data.', 'error');
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => { loadData(); }, [loadData]);

  const filtered = useMemo(() => {
    let result = areas;
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(a =>
        a.name.toLowerCase().includes(q) ||
        (a.transportMode?.code || '').toLowerCase().includes(q) ||
        (a.transportMode?.name || '').toLowerCase().includes(q) ||
        (a.provider?.name || '').toLowerCase().includes(q) ||
        (a.provider?.code || '').toLowerCase().includes(q) ||
        (a.city || '').toLowerCase().includes(q) ||
        (a.countryIsoCode || '').toLowerCase().includes(q) ||
        resolveEnum(a.areaType).toLowerCase().includes(q)
      );
    }
    if (filterType !== 'ALL') result = result.filter(a => resolveEnum(a.areaType) === filterType);
    if (filterMode !== 'ALL') result = result.filter(a => a.transportMode?.code === filterMode);
    return result;
  }, [areas, search, filterType, filterMode]);

  const activeModeCodesInAreas = useMemo(() => {
    const set = new Set<string>();
    areas.forEach(a => { if (a.transportMode?.code) set.add(a.transportMode.code); });
    return Array.from(set).sort();
  }, [areas]);

  const modeItems = useMemo(() => modes.map(m => ({
    key: m.id, label: m.code, sublabel: m.name, icon: MODE_ICONS[m.code] || null, color: m.colorHex,
  })), [modes]);
  const providerItems = useMemo(() => providers.map(p => ({
    key: p.id, label: p.code, sublabel: p.name,
  })), [providers]);
  const countryItems = useMemo(() => countries.map(c => ({
    key: c.isoCode, label: c.isoCode, sublabel: c.name,
  })), [countries]);

  const openAdd = () => { setEditingId(null); setForm(emptyForm()); setDrawerOpen(true); };
  const openEdit = (a: ServiceArea) => {
    setEditingId(a.id);
    setForm({
      name: a.name,
      areaType: resolveEnum(a.areaType) as AreaType,
      transportModeId: a.transportMode?.id || '',
      providerId: a.provider?.id || '',
      centerLat: a.centerLat?.toString() || '',
      centerLon: a.centerLon?.toString() || '',
      radiusM: a.radiusM?.toString() || '',
      countryIsoCode: a.countryIsoCode || '',
      city: a.city || '',
      isActive: a.isActive,
      configJson: a.configJson || '{}',
    });
    setDrawerOpen(true);
  };

  const handleSave = async () => {
    if (!form.name.trim()) { showToast('Name is required.', 'error'); return; }
    if (!form.transportModeId) { showToast('Transport Mode is required.', 'error'); return; }
    const body: Record<string, unknown> = {
      name: form.name.trim(),
      areaType: form.areaType,
      transportMode: { id: form.transportModeId },
      provider: form.providerId ? { id: form.providerId } : null,
      centerLat: form.centerLat ? parseFloat(form.centerLat) : null,
      centerLon: form.centerLon ? parseFloat(form.centerLon) : null,
      radiusM: form.radiusM ? parseInt(form.radiusM) : null,
      countryIsoCode: form.countryIsoCode || null,
      city: form.city || null,
      isActive: form.isActive,
      configJson: form.configJson || null,
    };
    try {
      if (editingId) {
        await apiPut(`/transport/service-areas/${editingId}`, body);
        showToast(`"${form.name}" updated.`);
      } else {
        await apiPost('/transport/service-areas', body);
        showToast(`"${form.name}" created.`);
      }
      setDrawerOpen(false);
      loadData();
    } catch {
      showToast('Failed to save.', 'error');
    }
  };

  const confirmDelete = (a: ServiceArea) => { setDeletingArea(a); setDeleteDrawerOpen(true); };
  const handleDelete = async () => {
    if (!deletingArea) return;
    try {
      await apiDelete(`/transport/service-areas/${deletingArea.id}`);
      showToast(`"${deletingArea.name}" deleted.`);
      loadData();
    } catch {
      showToast('Failed to delete.', 'error');
    }
    setDeleteDrawerOpen(false); setDeletingArea(null);
  };

  /* ═══════════ RENDER ═══════════ */
  return (
    <Toast.Provider swipeDirection="right">
      {/* ── Search ── */}
      <div className={s.searchPanel}>
        <div className={s.searchInputWrap}>
          <FiSearch size={14} className={s.searchIcon} />
          <input className={s.searchInput} placeholder="Search by name, mode, provider, city..."
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <button className={s.addButton} onClick={openAdd}>
          <FiPlus size={15} /> Add Service Area
        </button>
      </div>

      {/* ── Filter Chips ── */}
      <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', padding: '0 0 0.75rem', alignItems: 'center' }}>
        <FiFilter size={13} style={{ opacity: 0.4, marginRight: '0.2rem' }} />
        <FilterChip label="All Types" active={filterType === 'ALL'} onClick={() => setFilterType('ALL')} />
        {ALL_AREA_TYPES.map(at => (
          <FilterChip key={at} label={at} active={filterType === at}
            color={AREA_TYPE_COLORS[at]} icon={AREA_TYPE_ICONS[at]}
            onClick={() => setFilterType(filterType === at ? 'ALL' : at)} />
        ))}
        <div style={{ width: 1, height: 18, backgroundColor: 'rgba(128,128,128,0.2)', margin: '0 0.3rem' }} />
        <FilterChip label="All Modes" active={filterMode === 'ALL'} onClick={() => setFilterMode('ALL')} />
        {activeModeCodesInAreas.map(code => {
          const mode = modes.find(m => m.code === code);
          return (
            <FilterChip key={code} label={code} active={filterMode === code}
              color={mode?.colorHex} icon={MODE_ICONS[code]}
              onClick={() => setFilterMode(filterMode === code ? 'ALL' : code)} />
          );
        })}
        <span style={{ fontSize: '0.7rem', opacity: 0.4, marginLeft: 'auto' }}>
          {filtered.length} / {areas.length}
        </span>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: '3rem', opacity: 0.5 }}>Loading...</div>}

      {/* ── Cards ── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '0.75rem' }}>
        {!loading && filtered.length === 0 && (
          <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '3rem', opacity: 0.4 }}>
            <FiMapPin size={24} style={{ display: 'block', margin: '0 auto 8px' }} />No service areas found.
          </div>
        )}
        {filtered.map(area => {
          const areaType = resolveEnum(area.areaType) as AreaType;
          const modeCode = area.transportMode?.code || 'TAXI';
          const modeColor = area.transportMode?.colorHex || '#6b7280';
          const modeIcon = MODE_ICONS[modeCode] || <MdLocalTaxi />;
          let pricingInfo = '';
          try {
            const cfg = area.configJson ? JSON.parse(area.configJson) : null;
            if (cfg?.pricing) {
              const p = cfg.pricing;
              pricingInfo = `${(p.base_fare_cents / 100).toFixed(0)} ${p.currency} base + ${(p.per_km_cents / 100).toFixed(1)}/km`;
            }
          } catch { /* */ }

          return (
            <div key={area.id} className={s.modeCard} style={{ opacity: area.isActive ? 1 : 0.5 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.7rem' }}>
                  <div style={{
                    width: 40, height: 40, borderRadius: 10, display: 'flex',
                    alignItems: 'center', justifyContent: 'center',
                    fontSize: '1.2rem', backgroundColor: `${modeColor}18`, color: modeColor,
                  }}>{modeIcon}</div>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{area.name}</div>
                    <div style={{ fontSize: '0.7rem', opacity: 0.5 }}>{area.provider?.name || 'No provider'}</div>
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <span style={{
                    fontSize: '0.62rem', fontWeight: 700, padding: '0.12rem 0.45rem', borderRadius: 999,
                    backgroundColor: area.isActive ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)',
                    color: area.isActive ? '#22c55e' : '#ef4444',
                  }}>{area.isActive ? 'ACTIVE' : 'INACTIVE'}</span>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.rowActionBtn}><FiMoreVertical size={16} /></button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content side="left" sideOffset={6} align="start" className={s.dropdownContent} style={{ minWidth: 150 }}>
                        <DropdownMenu.Item className={s.dropdownItem} onSelect={() => openEdit(area)}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiEdit2 size={13} /> Edit</span>
                        </DropdownMenu.Item>
                        <DropdownMenu.Separator className={s.dropdownSep} />
                        <DropdownMenu.Item className={s.dropdownItemDanger} onSelect={() => confirmDelete(area)}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiTrash2 size={13} /> Delete</span>
                        </DropdownMenu.Item>
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                <span style={{
                  fontSize: '0.62rem', fontWeight: 700, padding: '0.15rem 0.5rem', borderRadius: 999,
                  backgroundColor: `${AREA_TYPE_COLORS[areaType]}15`, color: AREA_TYPE_COLORS[areaType],
                  display: 'flex', alignItems: 'center', gap: '0.25rem',
                }}>{AREA_TYPE_ICONS[areaType]} {areaType}</span>
                <span style={{
                  fontSize: '0.62rem', fontWeight: 700, padding: '0.15rem 0.5rem', borderRadius: 999,
                  backgroundColor: `${modeColor}15`, color: modeColor,
                }}>{modeCode}</span>
                {area.countryIsoCode && (
                  <span style={{
                    fontSize: '0.62rem', fontWeight: 600, padding: '0.15rem 0.5rem', borderRadius: 999,
                    backgroundColor: 'rgba(107,114,128,0.1)', color: 'inherit', opacity: 0.6,
                  }}>{area.countryIsoCode}</span>
                )}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.4rem', fontSize: '0.78rem' }}>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', fontWeight: 600, opacity: 0.45, letterSpacing: '0.04em' }}>
                    {areaType === 'RADIUS' ? 'Radius' : 'City'}
                  </span>
                  <span style={{ fontWeight: 600 }}>
                    {areaType === 'RADIUS' && area.radiusM ? `${(area.radiusM / 1000).toFixed(0)} km` : area.city || '—'}
                  </span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', fontWeight: 600, opacity: 0.45, letterSpacing: '0.04em' }}>Center</span>
                  <span style={{ fontWeight: 600, fontSize: '0.72rem' }}>
                    {area.centerLat && area.centerLon ? `${area.centerLat.toFixed(2)}, ${area.centerLon.toFixed(2)}` : '—'}
                  </span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', fontWeight: 600, opacity: 0.45, letterSpacing: '0.04em' }}>Pricing</span>
                  <span style={{ fontWeight: 600, fontSize: '0.72rem' }}>{pricingInfo || '—'}</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* ══════════ Add / Edit Drawer ══════════ */}
      <VaulDrawer open={drawerOpen} onOpenChange={setDrawerOpen}
        title={editingId ? 'Edit Service Area' : 'New Service Area'} width={520}
        footer={<>
          <button className={s.btnCancel} onClick={() => setDrawerOpen(false)}>Cancel</button>
          <button className={s.btnPrimary} onClick={handleSave}>
            {editingId ? 'Save Changes' : 'Create Service Area'}
          </button>
        </>}
      >
        <div><label className={s.fieldLabel}>Name *</label>
          <input className={paneStyles.formInput} style={{ maxWidth: '100%' }}
            placeholder="e.g. Esenboğa Taxi Zone"
            value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
          <SearchableDropdown label="Transport Mode *" items={modeItems} value={form.transportModeId}
            onChange={v => setForm(f => ({ ...f, transportModeId: v }))} placeholder="Select mode..." />
          <SearchableDropdown label="Provider" items={providerItems} value={form.providerId}
            onChange={v => setForm(f => ({ ...f, providerId: v }))} placeholder="Select provider..." allowClear />
        </div>

        <div><label className={s.fieldLabel}>Area Type *</label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            {ALL_AREA_TYPES.map(at => (
              <button key={at} onClick={() => setForm(f => ({ ...f, areaType: at }))}
                style={{
                  padding: '0.3rem 0.7rem', borderRadius: 6, border: '1px solid',
                  borderColor: form.areaType === at ? AREA_TYPE_COLORS[at] : 'rgba(128,128,128,0.2)',
                  backgroundColor: form.areaType === at ? `${AREA_TYPE_COLORS[at]}15` : 'transparent',
                  color: form.areaType === at ? AREA_TYPE_COLORS[at] : 'inherit',
                  fontSize: '0.78rem', fontWeight: 600, cursor: 'pointer',
                  display: 'flex', alignItems: 'center', gap: '0.3rem',
                }}>{AREA_TYPE_ICONS[at]} {at}</button>
            ))}
          </div>
        </div>

        {form.areaType === 'RADIUS' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
            <div><label className={s.fieldLabel}>Center Lat</label>
              <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} type="number" step="0.000001"
                placeholder="40.128100" value={form.centerLat}
                onChange={e => setForm(f => ({ ...f, centerLat: e.target.value }))} /></div>
            <div><label className={s.fieldLabel}>Center Lon</label>
              <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} type="number" step="0.000001"
                placeholder="32.995100" value={form.centerLon}
                onChange={e => setForm(f => ({ ...f, centerLon: e.target.value }))} /></div>
            <div><label className={s.fieldLabel}>Radius (m)</label>
              <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} type="number" min={1000}
                placeholder="60000" value={form.radiusM}
                onChange={e => setForm(f => ({ ...f, radiusM: e.target.value }))} /></div>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
          <SearchableDropdown label="Country" items={countryItems} value={form.countryIsoCode}
            onChange={v => setForm(f => ({ ...f, countryIsoCode: v }))} placeholder="Select country..." allowClear />
          <div><label className={s.fieldLabel}>City</label>
            <input className={paneStyles.formInput} style={{ maxWidth: '100%' }}
              placeholder="e.g. Ankara" value={form.city}
              onChange={e => setForm(f => ({ ...f, city: e.target.value }))} /></div>
        </div>

        <div><label className={s.fieldLabel}>Config JSON (pricing, max_distance, etc.)</label>
          <textarea className={paneStyles.formInput}
            style={{ maxWidth: '100%', minHeight: 120, fontFamily: 'monospace', fontSize: '0.78rem', resize: 'vertical' }}
            value={form.configJson} onChange={e => setForm(f => ({ ...f, configJson: e.target.value }))} /></div>

        <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.85rem' }}>
          <input type="checkbox" checked={form.isActive}
            onChange={e => setForm(f => ({ ...f, isActive: e.target.checked }))}
            style={{ accentColor: '#C8102E', width: 16, height: 16, cursor: 'pointer' }} />
          Active
        </label>
      </VaulDrawer>

      {/* ══════════ Delete Confirm Drawer ══════════ */}
      <VaulDrawer open={deleteDrawerOpen} onOpenChange={setDeleteDrawerOpen}
        title="Delete Service Area" width={380}
        footer={<>
          <button className={s.btnCancel} onClick={() => setDeleteDrawerOpen(false)}>Cancel</button>
          <button className={s.btnDanger} onClick={handleDelete}>Delete</button>
        </>}
      >
        <p style={{ fontSize: '0.9rem', margin: 0, lineHeight: 1.5 }}>
          Are you sure you want to delete <strong>{deletingArea?.name}</strong>? This action cannot be undone.
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

/* ═══════════════════════════════════════════════
   FilterChip
   ═══════════════════════════════════════════════ */
const FilterChip: React.FC<{
  label: string; active: boolean; onClick: () => void;
  color?: string; icon?: React.ReactNode;
}> = ({ label, active, onClick, color, icon }) => (
  <button onClick={onClick}
    style={{
      padding: '0.2rem 0.55rem', borderRadius: 999, border: '1px solid',
      borderColor: active ? (color || 'rgba(200,16,46,0.4)') : 'rgba(128,128,128,0.2)',
      backgroundColor: active ? `${color || 'rgba(200,16,46)'}${color ? '12' : ',0.08)'}` : 'transparent',
      color: active ? (color || '#C8102E') : 'inherit',
      fontSize: '0.72rem', fontWeight: 600, cursor: 'pointer',
      display: 'flex', alignItems: 'center', gap: '0.25rem',
      opacity: active ? 1 : 0.65,
    }}>
    {icon} {label}
  </button>
);
