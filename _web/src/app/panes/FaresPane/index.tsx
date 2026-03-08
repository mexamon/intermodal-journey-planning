import React, { useState, useEffect, useMemo, useCallback } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from '../TransportModesPane/TransportModesPane.module.scss';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import {
  FiSearch, FiPlus, FiEdit2, FiTrash2, FiMoreVertical,
  FiFilter, FiChevronDown, FiCheck, FiPackage, FiRefreshCw,
} from 'react-icons/fi';
import {
  MdFlight, MdDirectionsBus, MdTrain, MdDirectionsSubway,
  MdLocalTaxi, MdDirectionsBoat, MdBusiness, MdDirectionsWalk,
} from 'react-icons/md';
import { apiGet, apiPost, apiPut, apiDelete, emitToast } from '../../api/client';
import { VaulDrawer } from '../../components/shared';

/* ═══════════════════════════════════════════════ */
const resolveEnum = (val: unknown): string => {
  if (val && typeof val === 'object' && 'value' in (val as Record<string, unknown>))
    return (val as Record<string, string>).value;
  return (val as string) || '';
};

/* Types */
interface FareEdge {
  id: string;
  originLocation?: { iataCode: string; name: string };
  destinationLocation?: { iataCode: string; name: string };
  transportMode?: { code: string; name: string };
  provider?: { code: string; name: string } | null;
  trips?: FareTrip[];
}
interface FareTrip { id: string; serviceCode: string | null; departureTime: string; arrivalTime: string; }
interface FareItem {
  id: string;
  edge: FareEdge;
  trip: FareTrip | null;
  fareClass: string | { code: string; desc: string; value: string };
  pricingType: string | { code: string; desc: string; value: string };
  priceCents: number | null;
  currency: string;
  refundable: boolean;
  changeable: boolean;
  luggageKg: number | null;
  cabinLuggageKg: number | null;
}
interface FormState {
  edgeId: string; tripId: string; fareClass: string; pricingType: string;
  priceCents: string; currency: string; refundable: boolean; changeable: boolean;
  luggageKg: string; cabinLuggageKg: string;
}

const emptyForm = (): FormState => ({
  edgeId: '', tripId: '', fareClass: 'STANDARD', pricingType: 'FIXED',
  priceCents: '', currency: 'TRY', refundable: false, changeable: false,
  luggageKg: '', cabinLuggageKg: '',
});

const FARE_CLASSES = ['ECONOMY', 'PREMIUM_ECONOMY', 'BUSINESS', 'FIRST', 'STANDARD', 'COMFORT', 'VIP'] as const;
const PRICING_TYPES = ['FIXED', 'ESTIMATED', 'DYNAMIC', 'FREE'] as const;

const CLASS_COLORS: Record<string, string> = {
  ECONOMY: '#22c55e', PREMIUM_ECONOMY: '#10b981', BUSINESS: '#3b82f6',
  FIRST: '#eab308', STANDARD: '#6b7280', COMFORT: '#8b5cf6', VIP: '#f97316',
};
const PRICING_COLORS: Record<string, string> = {
  FIXED: '#3b82f6', ESTIMATED: '#f59e0b', DYNAMIC: '#8b5cf6', FREE: '#22c55e',
};
const MODE_ICONS: Record<string, React.ReactNode> = {
  FLIGHT: <MdFlight size={14} />, BUS: <MdDirectionsBus size={14} />,
  TRAIN: <MdTrain size={14} />, SUBWAY: <MdDirectionsSubway size={14} />,
  UBER: <MdLocalTaxi size={14} />, FERRY: <MdDirectionsBoat size={14} />,
  WALKING: <MdDirectionsWalk size={14} />,
};
const MODE_COLORS: Record<string, string> = {
  FLIGHT: '#1E88E5', BUS: '#43A047', TRAIN: '#E53935', SUBWAY: '#8E24AA',
  UBER: '#212121', FERRY: '#00ACC1', WALKING: '#78909C',
};

const formatPrice = (cents: number | null, currency: string, pricingType: string) => {
  const pt = resolveEnum(pricingType);
  if (pt === 'FREE') return 'Free';
  if (pt === 'DYNAMIC') return 'Dynamic';
  if (cents == null) return '—';
  const syms: Record<string, string> = { TRY: '₺', EUR: '€', USD: '$', GBP: '£' };
  const sym = syms[currency] || currency + ' ';
  return `${sym}${(cents / 100).toFixed(2)}`;
};

/* SearchableDropdown — inline */
interface SDProps {
  label: string; items: { key: string; label: string; sublabel?: string }[];
  value: string; onChange: (k: string) => void; placeholder?: string; allowClear?: boolean;
}
const SearchableDropdown: React.FC<SDProps> = ({ label, items, value, onChange, placeholder = 'Select...', allowClear }) => {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const selected = items.find(i => i.key === value);
  const filtered = useMemo(() => {
    if (!q) return items;
    const lq = q.toLowerCase();
    return items.filter(i => i.label.toLowerCase().includes(lq) || (i.sublabel || '').toLowerCase().includes(lq));
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
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {selected ? `${selected.label}${selected.sublabel ? ` — ${selected.sublabel}` : ''}` : <span style={{ opacity: 0.45 }}>{placeholder}</span>}
            </span>
            <FiChevronDown size={13} style={{ opacity: 0.4, flexShrink: 0, transform: open ? 'rotate(180deg)' : 'none', transition: 'transform 0.15s' }} />
          </button>
        </DropdownMenu.Trigger>
        <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}
          style={{ minWidth: 260, maxHeight: 260, display: 'flex', flexDirection: 'column' }}
          onCloseAutoFocus={e => e.preventDefault()}>
          <div style={{ padding: '0.35rem 0.5rem', flexShrink: 0 }}>
            <input autoFocus placeholder="Search..." value={q} onChange={e => setQ(e.target.value)}
              onClick={e => e.stopPropagation()} onKeyDown={e => e.stopPropagation()}
              style={{ width: '100%', border: 'none', background: 'transparent', outline: 'none', fontSize: '0.8rem', color: 'inherit', padding: '0.2rem 0' }} />
          </div>
          <div style={{ overflowY: 'auto', flex: 1 }}>
            {allowClear && (
              <DropdownMenu.Item className={s.dropdownItem} onSelect={() => onChange('')}>
                <span style={{ opacity: 0.5 }}>None</span>
                {!value && <FiCheck size={14} />}
              </DropdownMenu.Item>
            )}
            {filtered.length === 0 && <div style={{ padding: '0.6rem', textAlign: 'center', opacity: 0.4, fontSize: '0.78rem' }}>No results</div>}
            {filtered.map(item => (
              <DropdownMenu.Item key={item.key} className={s.dropdownItem} onSelect={() => onChange(item.key)}>
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {item.label}{item.sublabel ? ` — ${item.sublabel}` : ''}
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
   MAIN
   ═══════════════════════════════════════════════ */
export const FaresPane: React.FC = () => {
  const [fares, setFares] = useState<FareItem[]>([]);
  const [edges, setEdges] = useState<FareEdge[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterClass, setFilterClass] = useState<string>('ALL');
  const [filterPricing, setFilterPricing] = useState<string>('ALL');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [deleteDrawerOpen, setDeleteDrawerOpen] = useState(false);
  const [deletingFare, setDeletingFare] = useState<FareItem | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [fareData, edgeData] = await Promise.all([
        apiGet<FareItem[]>('/transport/fares'),
        apiGet<FareEdge[]>('/transport/edges'),
      ]);
      setFares(fareData || []);
      setEdges(edgeData || []);
    } catch (err) {
      console.error('Failed to load fares:', err);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  /* Derived data */
  const edgeItems = useMemo(() => edges.map(e => ({
    key: e.id,
    label: `${e.originLocation?.iataCode || '?'} → ${e.destinationLocation?.iataCode || '?'}`,
    sublabel: `${resolveEnum(e.transportMode?.code || '')} · ${e.provider?.code || '—'}`,
  })), [edges]);

  const selectedEdge = useMemo(() => edges.find(e => e.id === form.edgeId), [edges, form.edgeId]);
  const tripItems = useMemo(() => {
    if (!selectedEdge?.trips?.length) return [];
    return selectedEdge.trips.map(t => ({
      key: t.id,
      label: `${t.serviceCode || '—'} ${t.departureTime}→${t.arrivalTime}`,
    }));
  }, [selectedEdge]);

  const filtered = useMemo(() => {
    let result = fares;
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(f => {
        const fc = resolveEnum(f.fareClass).toLowerCase();
        const pt = resolveEnum(f.pricingType).toLowerCase();
        const route = `${f.edge?.originLocation?.iataCode || ''} ${f.edge?.destinationLocation?.iataCode || ''}`.toLowerCase();
        const provider = (f.edge?.provider?.code || '').toLowerCase();
        const trip = (f.trip?.serviceCode || '').toLowerCase();
        return fc.includes(q) || pt.includes(q) || route.includes(q) || provider.includes(q) || trip.includes(q);
      });
    }
    if (filterClass !== 'ALL') result = result.filter(f => resolveEnum(f.fareClass) === filterClass);
    if (filterPricing !== 'ALL') result = result.filter(f => resolveEnum(f.pricingType) === filterPricing);
    return result;
  }, [fares, search, filterClass, filterPricing]);

  const activeClassesInData = useMemo(() => {
    const set = new Set<string>(); fares.forEach(f => set.add(resolveEnum(f.fareClass)));
    return Array.from(set).sort();
  }, [fares]);

  /* Handlers */
  const openAdd = () => { setEditingId(null); setForm(emptyForm()); setDrawerOpen(true); };
  const openEdit = (f: FareItem) => {
    setEditingId(f.id);
    setForm({
      edgeId: f.edge?.id || '', tripId: f.trip?.id || '',
      fareClass: resolveEnum(f.fareClass), pricingType: resolveEnum(f.pricingType),
      priceCents: f.priceCents != null ? String(f.priceCents) : '',
      currency: f.currency || 'TRY',
      refundable: f.refundable, changeable: f.changeable,
      luggageKg: f.luggageKg != null ? String(f.luggageKg) : '',
      cabinLuggageKg: f.cabinLuggageKg != null ? String(f.cabinLuggageKg) : '',
    });
    setDrawerOpen(true);
  };

  const handleSave = async () => {
    if (!form.edgeId) { emitToast('warning', 'Edge is required.'); return; }
    if (form.pricingType === 'FIXED' && !form.priceCents) { emitToast('warning', 'Price is required for FIXED pricing.'); return; }

    const body = {
      edgeId: form.edgeId,
      tripId: form.tripId || null,
      fareClass: form.fareClass,
      pricingType: form.pricingType,
      priceCents: form.pricingType === 'FREE' ? 0 : (form.pricingType === 'DYNAMIC' ? null : (form.priceCents ? parseInt(form.priceCents) : null)),
      currency: form.currency,
      refundable: form.refundable,
      changeable: form.changeable,
      luggageKg: form.luggageKg ? parseInt(form.luggageKg) : null,
      cabinLuggageKg: form.cabinLuggageKg ? parseInt(form.cabinLuggageKg) : null,
    };
    try {
      if (editingId) {
        await apiPut(`/transport/fares/${editingId}`, body);
        emitToast('success', 'Fare updated.');
      } else {
        await apiPost('/transport/fares', body);
        emitToast('success', 'Fare created.');
      }
      setDrawerOpen(false); loadData();
    } catch { /* apiPost/apiPut already emits error toast */ }
  };

  const confirmDelete = (f: FareItem) => { setDeletingFare(f); setDeleteDrawerOpen(true); };
  const handleDelete = async () => {
    if (!deletingFare) return;
    try {
      await apiDelete(`/transport/fares/${deletingFare.id}`);
      emitToast('success', 'Fare deleted.');
      loadData();
    } catch { /* apiDelete already handles */ }
    setDeleteDrawerOpen(false); setDeletingFare(null);
  };

  const isPriceDisabled = form.pricingType === 'DYNAMIC' || form.pricingType === 'FREE';

  return (
    <>
      {/* Search */}
      <div className={s.searchPanel}>
        <div className={s.searchInputWrap}>
          <FiSearch size={14} className={s.searchIcon} />
          <input className={s.searchInput} placeholder="Search by route, provider, class, trip..."
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <button className={s.addButton} onClick={openAdd}><FiPlus size={15} /> Add Fare</button>
      </div>

      {/* Filters */}
      <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', padding: '0 0 0.75rem', alignItems: 'center' }}>
        <FiFilter size={13} style={{ opacity: 0.4, marginRight: '0.2rem' }} />
        <Chip label="All" active={filterClass === 'ALL'} onClick={() => setFilterClass('ALL')} />
        {activeClassesInData.map(c => (
          <Chip key={c} label={c.replace(/_/g, ' ')} active={filterClass === c} color={CLASS_COLORS[c]}
            onClick={() => setFilterClass(filterClass === c ? 'ALL' : c)} />
        ))}
        <span style={{ width: 1, height: 16, background: 'rgba(128,128,128,0.15)', margin: '0 0.3rem' }} />
        {PRICING_TYPES.map(pt => (
          <Chip key={pt} label={pt} active={filterPricing === pt} color={PRICING_COLORS[pt]}
            onClick={() => setFilterPricing(filterPricing === pt ? 'ALL' : pt)} />
        ))}
        <span style={{ fontSize: '0.7rem', opacity: 0.4, marginLeft: 'auto' }}>{filtered.length} / {fares.length}</span>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: '3rem', opacity: 0.5 }}>Loading...</div>}

      {/* Table */}
      {!loading && (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: 0, fontSize: '0.84rem' }}>
            <thead>
              <tr>
                {['Route', 'Mode', 'Provider', 'Trip', 'Class', 'Pricing', 'Price', 'Refund', 'Change', 'Luggage', ''].map((h, i) => (
                  <th key={i} style={{
                    textAlign: 'left', padding: '0.6rem 0.75rem', fontWeight: 600, fontSize: '0.72rem',
                    textTransform: 'uppercase', letterSpacing: '0.04em', opacity: 0.5,
                    borderBottom: '1px solid rgba(128,128,128,0.12)', whiteSpace: 'nowrap',
                  }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 && (
                <tr><td colSpan={11} style={{ textAlign: 'center', padding: '2.5rem', opacity: 0.4 }}>
                  <FiPackage size={20} style={{ display: 'block', margin: '0 auto 6px' }} /> No fares found.
                </td></tr>
              )}
              {filtered.map(fare => {
                const fc = resolveEnum(fare.fareClass);
                const pt = resolveEnum(fare.pricingType);
                const modeCode = resolveEnum(fare.edge?.transportMode?.code || '');
                const mIcon = MODE_ICONS[modeCode] || <MdBusiness size={14} />;
                const mColor = MODE_COLORS[modeCode] || '#6b7280';
                return (
                  <tr key={fare.id}
                    onMouseEnter={e => (e.currentTarget.style.background = 'rgba(128,128,128,0.04)')}
                    onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
                    <td style={{ padding: '0.55rem 0.75rem', fontWeight: 600, fontSize: '0.82rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>
                      {fare.edge?.originLocation?.iataCode || '?'} → {fare.edge?.destinationLocation?.iataCode || '?'}
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>
                      <span style={{ fontSize: '0.68rem', fontWeight: 700, padding: '0.12rem 0.45rem', borderRadius: 999, backgroundColor: `${mColor}15`, color: mColor, display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>{mIcon} {modeCode}</span>
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', fontFamily: 'monospace', fontSize: '0.82rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>{fare.edge?.provider?.code || '—'}</td>
                    <td style={{ padding: '0.55rem 0.75rem', fontSize: '0.78rem', borderBottom: '1px solid rgba(128,128,128,0.06)', opacity: fare.trip ? 1 : 0.3 }}>
                      {fare.trip?.serviceCode || '—'}
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>
                      <span style={{ fontSize: '0.68rem', fontWeight: 700, padding: '0.12rem 0.45rem', borderRadius: 999, backgroundColor: `${CLASS_COLORS[fc] || '#6b7280'}15`, color: CLASS_COLORS[fc] || '#6b7280' }}>{fc.replace(/_/g, ' ')}</span>
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>
                      <span style={{ fontSize: '0.68rem', fontWeight: 700, padding: '0.12rem 0.45rem', borderRadius: 999, backgroundColor: `${PRICING_COLORS[pt] || '#6b7280'}15`, color: PRICING_COLORS[pt] || '#6b7280' }}>{pt}</span>
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', fontWeight: 600, fontSize: '0.85rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>
                      {formatPrice(fare.priceCents, fare.currency, fare.pricingType)}
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', fontSize: '0.82rem', borderBottom: '1px solid rgba(128,128,128,0.06)', textAlign: 'center' }}>
                      <span style={{ color: fare.refundable ? '#22c55e' : '#ef4444', fontWeight: 600 }}>{fare.refundable ? '✓' : '✗'}</span>
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', fontSize: '0.82rem', borderBottom: '1px solid rgba(128,128,128,0.06)', textAlign: 'center' }}>
                      <span style={{ color: fare.changeable ? '#22c55e' : '#ef4444', fontWeight: 600 }}>{fare.changeable ? '✓' : '✗'}</span>
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', fontSize: '0.78rem', borderBottom: '1px solid rgba(128,128,128,0.06)', opacity: fare.luggageKg ? 1 : 0.3 }}>
                      {fare.luggageKg != null ? `${fare.luggageKg}kg` : '—'}{fare.cabinLuggageKg != null ? ` + ${fare.cabinLuggageKg}kg` : ''}
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', borderBottom: '1px solid rgba(128,128,128,0.06)', textAlign: 'right' }}>
                      <DropdownMenu.Root>
                        <DropdownMenu.Trigger asChild>
                          <button className={paneStyles.rowActionBtn}><FiMoreVertical size={15} /></button>
                        </DropdownMenu.Trigger>
                        <DropdownMenu.Portal>
                          <DropdownMenu.Content side="left" sideOffset={6} align="start" className={s.dropdownContent} style={{ minWidth: 140 }}>
                            <DropdownMenu.Item className={s.dropdownItem} onSelect={() => openEdit(fare)}>
                              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiEdit2 size={13} /> Edit</span>
                            </DropdownMenu.Item>
                            <DropdownMenu.Separator className={s.dropdownSep} />
                            <DropdownMenu.Item className={s.dropdownItemDanger} onSelect={() => confirmDelete(fare)}>
                              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiTrash2 size={13} /> Delete</span>
                            </DropdownMenu.Item>
                          </DropdownMenu.Content>
                        </DropdownMenu.Portal>
                      </DropdownMenu.Root>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Add/Edit Drawer */}
      <VaulDrawer open={drawerOpen} onOpenChange={setDrawerOpen}
        title={editingId ? 'Edit Fare' : 'New Fare'} width={440}
        footer={<>
          <button className={s.btnCancel} onClick={() => setDrawerOpen(false)}>Cancel</button>
          <button className={s.btnPrimary} onClick={handleSave}>{editingId ? 'Save Changes' : 'Create Fare'}</button>
        </>}>

        {/* Edge selector */}
        <SearchableDropdown label="Route (Edge) *" items={edgeItems} value={form.edgeId}
          onChange={v => setForm(f => ({ ...f, edgeId: v, tripId: '' }))} placeholder="Select route..." />

        {/* Trip selector (optional, only if selected edge has trips) */}
        {tripItems.length > 0 && (
          <SearchableDropdown label="Trip (optional)" items={tripItems} value={form.tripId}
            onChange={v => setForm(f => ({ ...f, tripId: v }))} placeholder="All trips..." allowClear />
        )}

        {/* Fare class */}
        <div><label className={s.fieldLabel}>Fare Class *</label>
          <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
            {FARE_CLASSES.map(c => (
              <button key={c} type="button" onClick={() => setForm(f => ({ ...f, fareClass: c }))}
                style={{
                  padding: '0.25rem 0.5rem', borderRadius: 6, border: '1px solid',
                  borderColor: form.fareClass === c ? (CLASS_COLORS[c] || '#6b7280') : 'rgba(128,128,128,0.2)',
                  backgroundColor: form.fareClass === c ? `${CLASS_COLORS[c] || '#6b7280'}15` : 'transparent',
                  color: form.fareClass === c ? (CLASS_COLORS[c] || '#6b7280') : 'inherit',
                  fontSize: '0.7rem', fontWeight: 600, cursor: 'pointer',
                }}>{c.replace(/_/g, ' ')}</button>
            ))}
          </div>
        </div>

        {/* Pricing type */}
        <div><label className={s.fieldLabel}>Pricing Type *</label>
          <div style={{ display: 'flex', gap: '0.35rem' }}>
            {PRICING_TYPES.map(pt => (
              <button key={pt} type="button" onClick={() => setForm(f => ({ ...f, pricingType: pt, priceCents: pt === 'FREE' ? '0' : (pt === 'DYNAMIC' ? '' : f.priceCents) }))}
                style={{
                  padding: '0.25rem 0.55rem', borderRadius: 6, border: '1px solid',
                  borderColor: form.pricingType === pt ? (PRICING_COLORS[pt]) : 'rgba(128,128,128,0.2)',
                  backgroundColor: form.pricingType === pt ? `${PRICING_COLORS[pt]}15` : 'transparent',
                  color: form.pricingType === pt ? PRICING_COLORS[pt] : 'inherit',
                  fontSize: '0.7rem', fontWeight: 600, cursor: 'pointer',
                }}>{pt}</button>
            ))}
          </div>
        </div>

        {/* Price + Currency */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 100px', gap: '0.6rem' }}>
          <div>
            <label className={s.fieldLabel}>Price (cents) {form.pricingType === 'FIXED' && '*'}</label>
            <input className={paneStyles.formInput}
              style={{ maxWidth: '100%', fontFamily: 'monospace', opacity: isPriceDisabled ? 0.4 : 1 }}
              type="number" min={0} placeholder={isPriceDisabled ? (form.pricingType === 'FREE' ? '0' : 'N/A') : 'e.g. 28900'}
              value={isPriceDisabled ? '' : form.priceCents}
              onChange={e => setForm(f => ({ ...f, priceCents: e.target.value }))}
              disabled={isPriceDisabled} />
          </div>
          <div>
            <label className={s.fieldLabel}>Currency *</label>
            <DropdownMenu.Root>
              <DropdownMenu.Trigger asChild>
                <button className={paneStyles.formInput} type="button" style={{
                  maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center',
                  justifyContent: 'space-between', fontSize: '0.85rem',
                }}>
                  {form.currency} <FiChevronDown size={12} style={{ opacity: 0.4 }} />
                </button>
              </DropdownMenu.Trigger>
              <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent} style={{ minWidth: 100 }}
                onCloseAutoFocus={e => e.preventDefault()}>
                {['TRY', 'EUR', 'USD', 'GBP'].map(c => (
                  <DropdownMenu.Item key={c} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, currency: c }))}>
                    {c} {form.currency === c && <FiCheck size={14} />}
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Root>
          </div>
        </div>

        {/* Toggles */}
        <div style={{ display: 'flex', gap: '1.5rem' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.85rem' }}>
            <input type="checkbox" checked={form.refundable}
              onChange={e => setForm(f => ({ ...f, refundable: e.target.checked }))}
              style={{ accentColor: '#22c55e', width: 16, height: 16, cursor: 'pointer' }} />
            Refundable
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.85rem' }}>
            <input type="checkbox" checked={form.changeable}
              onChange={e => setForm(f => ({ ...f, changeable: e.target.checked }))}
              style={{ accentColor: '#3b82f6', width: 16, height: 16, cursor: 'pointer' }} />
            Changeable
          </label>
        </div>

        {/* Luggage */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
          <div><label className={s.fieldLabel}>Luggage (kg)</label>
            <input className={paneStyles.formInput} style={{ maxWidth: '100%' }}
              type="number" min={0} placeholder="e.g. 20"
              value={form.luggageKg} onChange={e => setForm(f => ({ ...f, luggageKg: e.target.value }))} /></div>
          <div><label className={s.fieldLabel}>Cabin Luggage (kg)</label>
            <input className={paneStyles.formInput} style={{ maxWidth: '100%' }}
              type="number" min={0} placeholder="e.g. 8"
              value={form.cabinLuggageKg} onChange={e => setForm(f => ({ ...f, cabinLuggageKg: e.target.value }))} /></div>
        </div>
      </VaulDrawer>

      {/* Delete Drawer */}
      <VaulDrawer open={deleteDrawerOpen} onOpenChange={setDeleteDrawerOpen}
        title="Delete Fare" width={380}
        footer={<>
          <button className={s.btnCancel} onClick={() => setDeleteDrawerOpen(false)}>Cancel</button>
          <button className={s.btnDanger} onClick={handleDelete}>Delete</button>
        </>}>
        <p style={{ fontSize: '0.9rem', margin: 0, lineHeight: 1.5 }}>
          Are you sure you want to delete this <strong>{resolveEnum(deletingFare?.fareClass || '')}</strong> fare
          for <strong>{deletingFare?.edge?.originLocation?.iataCode} → {deletingFare?.edge?.destinationLocation?.iataCode}</strong>?
        </p>
      </VaulDrawer>
    </>
  );
};

/* Chip */
const Chip: React.FC<{ label: string; active: boolean; onClick: () => void; color?: string }> = ({
  label, active, onClick, color,
}) => (
  <button onClick={onClick} style={{
    padding: '0.2rem 0.55rem', borderRadius: 999, border: '1px solid',
    borderColor: active ? (color || 'rgba(200,16,46,0.4)') : 'rgba(128,128,128,0.2)',
    backgroundColor: active ? `${color || '#C8102E'}15` : 'transparent',
    color: active ? (color || '#C8102E') : 'inherit',
    fontSize: '0.72rem', fontWeight: 600, cursor: 'pointer',
    opacity: active ? 1 : 0.65,
  }}>{label}</button>
);
