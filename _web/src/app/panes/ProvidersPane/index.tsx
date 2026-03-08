import React, { useState, useEffect, useMemo, useCallback } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from '../TransportModesPane/TransportModesPane.module.scss';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import {
  FiSearch, FiPlus, FiEdit2, FiTrash2, FiMoreVertical,
  FiFilter, FiChevronDown, FiCheck,
} from 'react-icons/fi';
import {
  MdFlight, MdDirectionsBus, MdTrain, MdDirectionsSubway,
  MdLocalTaxi, MdDirectionsBoat, MdBusiness,
} from 'react-icons/md';
import { apiGet, apiPost, apiPut, apiDelete, emitToast } from '../../api/client';
import { VaulDrawer } from '../../components/shared';

/* ═══════════════════════════════════════════════ */
const resolveEnum = (val: unknown): string => {
  if (val && typeof val === 'object' && 'value' in (val as Record<string, unknown>))
    return (val as Record<string, string>).value;
  return (val as string) || '';
};

interface Provider {
  id: string; code: string; name: string;
  type: string | { code: string; desc: string; value: string };
  countryIsoCode: string | null; isActive: boolean;
}
interface RefCountry { id: string; isoCode: string; name: string; }
interface FormState { code: string; name: string; type: string; countryIsoCode: string; isActive: boolean; }

const emptyForm = (): FormState => ({ code: '', name: '', type: 'OTHER', countryIsoCode: '', isActive: true });

const PROVIDER_TYPES = ['AIRLINE','BUS_COMPANY','TRAIN_OPERATOR','METRO_OPERATOR','RIDE_SHARE','FERRY_OPERATOR','OTHER'] as const;

const TYPE_ICONS: Record<string, React.ReactNode> = {
  AIRLINE: <MdFlight size={14} />, BUS_COMPANY: <MdDirectionsBus size={14} />,
  TRAIN_OPERATOR: <MdTrain size={14} />, METRO_OPERATOR: <MdDirectionsSubway size={14} />,
  RIDE_SHARE: <MdLocalTaxi size={14} />, FERRY_OPERATOR: <MdDirectionsBoat size={14} />,
  OTHER: <MdBusiness size={14} />,
};
const TYPE_COLORS: Record<string, string> = {
  AIRLINE: '#3b82f6', BUS_COMPANY: '#22c55e', TRAIN_OPERATOR: '#f97316',
  METRO_OPERATOR: '#8b5cf6', RIDE_SHARE: '#eab308', FERRY_OPERATOR: '#06b6d4', OTHER: '#6b7280',
};

/* SearchableDropdown */
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
          style={{ minWidth: 220, maxHeight: 260, display: 'flex', flexDirection: 'column' }}
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
export const ProvidersPane: React.FC = () => {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [countries, setCountries] = useState<RefCountry[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterActive, setFilterActive] = useState<string>('ALL');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [deleteDrawerOpen, setDeleteDrawerOpen] = useState(false);
  const [deletingProvider, setDeletingProvider] = useState<Provider | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [provData, countryData] = await Promise.all([
        apiGet<Provider[]>('/inventory/providers'),
        apiGet<RefCountry[]>('/admin/ref/countries'),
      ]);
      setProviders(provData || []);
      setCountries(countryData || []);
    } catch (err) {
      console.error('Failed to load providers:', err);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const filtered = useMemo(() => {
    let result = providers;
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(p =>
        p.code.toLowerCase().includes(q) || p.name.toLowerCase().includes(q) ||
        resolveEnum(p.type).toLowerCase().includes(q) || (p.countryIsoCode || '').toLowerCase().includes(q));
    }
    if (filterType !== 'ALL') result = result.filter(p => resolveEnum(p.type) === filterType);
    if (filterActive === 'ACTIVE') result = result.filter(p => p.isActive);
    if (filterActive === 'INACTIVE') result = result.filter(p => !p.isActive);
    return result;
  }, [providers, search, filterType, filterActive]);

  const activeTypesInData = useMemo(() => {
    const set = new Set<string>(); providers.forEach(p => set.add(resolveEnum(p.type)));
    return Array.from(set).sort();
  }, [providers]);

  const countryItems = useMemo(() => countries.map(c => ({ key: c.isoCode, label: c.isoCode, sublabel: c.name })), [countries]);

  const openAdd = () => { setEditingId(null); setForm(emptyForm()); setDrawerOpen(true); };
  const openEdit = (p: Provider) => {
    setEditingId(p.id);
    setForm({ code: p.code, name: p.name, type: resolveEnum(p.type), countryIsoCode: p.countryIsoCode || '', isActive: p.isActive });
    setDrawerOpen(true);
  };

  const handleSave = async () => {
    if (!form.code.trim()) { emitToast('warning', 'Code is required.'); return; }
    if (!form.name.trim()) { emitToast('warning', 'Name is required.'); return; }
    const body = { code: form.code.trim().toUpperCase(), name: form.name.trim(), type: form.type, countryIsoCode: form.countryIsoCode || null, isActive: form.isActive };
    try {
      if (editingId) { await apiPut(`/inventory/providers/${editingId}`, body); emitToast('success', `"${form.name}" updated.`); }
      else { await apiPost('/inventory/providers', body); emitToast('success', `"${form.name}" created.`); }
      setDrawerOpen(false); loadData();
    } catch {
      // apiPost/apiPut already emits error toast via interceptor
    }
  };

  const confirmDelete = (p: Provider) => { setDeletingProvider(p); setDeleteDrawerOpen(true); };
  const handleDelete = async () => {
    if (!deletingProvider) return;
    try {
      await apiDelete(`/inventory/providers/${deletingProvider.id}`);
      emitToast('success', `"${deletingProvider.name}" deleted.`);
      loadData();
    } catch {
      // apiDelete already emits error toast via interceptor
    }
    setDeleteDrawerOpen(false); setDeletingProvider(null);
  };

  return (
    <>
      {/* Search bar */}
      <div className={s.searchPanel}>
        <div className={s.searchInputWrap}>
          <FiSearch size={14} className={s.searchIcon} />
          <input className={s.searchInput} placeholder="Search by code, name, type, country..."
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <button className={s.addButton} onClick={openAdd}><FiPlus size={15} /> Add Provider</button>
      </div>

      {/* Filters */}
      <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', padding: '0 0 0.75rem', alignItems: 'center' }}>
        <FiFilter size={13} style={{ opacity: 0.4, marginRight: '0.2rem' }} />
        <Chip label="All" active={filterType === 'ALL'} onClick={() => setFilterType('ALL')} />
        {activeTypesInData.map(t => (
          <Chip key={t} label={t.replace(/_/g, ' ')} active={filterType === t} color={TYPE_COLORS[t]}
            icon={TYPE_ICONS[t]} onClick={() => setFilterType(filterType === t ? 'ALL' : t)} />
        ))}
        <span style={{ width: 1, height: 16, background: 'rgba(128,128,128,0.15)', margin: '0 0.3rem' }} />
        <Chip label="Active" active={filterActive === 'ACTIVE'} color="#22c55e" onClick={() => setFilterActive(filterActive === 'ACTIVE' ? 'ALL' : 'ACTIVE')} />
        <Chip label="Inactive" active={filterActive === 'INACTIVE'} color="#ef4444" onClick={() => setFilterActive(filterActive === 'INACTIVE' ? 'ALL' : 'INACTIVE')} />
        <span style={{ fontSize: '0.7rem', opacity: 0.4, marginLeft: 'auto' }}>{filtered.length} / {providers.length}</span>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: '3rem', opacity: 0.5 }}>Loading...</div>}

      {/* Table grid */}
      {!loading && (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: 0, fontSize: '0.84rem' }}>
            <thead>
              <tr>
                {['Code', 'Name', 'Type', 'Country', 'Status', ''].map((h, i) => (
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
                <tr><td colSpan={6} style={{ textAlign: 'center', padding: '2.5rem', opacity: 0.4 }}>
                  <MdBusiness size={20} style={{ display: 'block', margin: '0 auto 6px' }} /> No providers found.
                </td></tr>
              )}
              {filtered.map(prov => {
                const provType = resolveEnum(prov.type);
                const color = TYPE_COLORS[provType] || '#6b7280';
                const icon = TYPE_ICONS[provType] || <MdBusiness size={14} />;
                return (
                  <tr key={prov.id} style={{ opacity: prov.isActive ? 1 : 0.5, transition: 'background 0.15s' }}
                    onMouseEnter={e => (e.currentTarget.style.background = 'rgba(128,128,128,0.04)')}
                    onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
                    <td style={{ padding: '0.55rem 0.75rem', fontFamily: 'monospace', fontWeight: 600, fontSize: '0.82rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>{prov.code}</td>
                    <td style={{ padding: '0.55rem 0.75rem', fontWeight: 500, borderBottom: '1px solid rgba(128,128,128,0.06)' }}>{prov.name}</td>
                    <td style={{ padding: '0.55rem 0.75rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>
                      <span style={{ fontSize: '0.68rem', fontWeight: 700, padding: '0.12rem 0.45rem', borderRadius: 999, backgroundColor: `${color}15`, color, display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>{icon} {provType.replace(/_/g, ' ')}</span>
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', fontSize: '0.82rem', opacity: prov.countryIsoCode ? 1 : 0.3, borderBottom: '1px solid rgba(128,128,128,0.06)' }}>{prov.countryIsoCode || '—'}</td>
                    <td style={{ padding: '0.55rem 0.75rem', borderBottom: '1px solid rgba(128,128,128,0.06)' }}>
                      <span style={{ fontSize: '0.62rem', fontWeight: 700, padding: '0.12rem 0.45rem', borderRadius: 999, backgroundColor: prov.isActive ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)', color: prov.isActive ? '#22c55e' : '#ef4444' }}>{prov.isActive ? 'ACTIVE' : 'INACTIVE'}</span>
                    </td>
                    <td style={{ padding: '0.55rem 0.75rem', borderBottom: '1px solid rgba(128,128,128,0.06)', textAlign: 'right' }}>
                      <DropdownMenu.Root>
                        <DropdownMenu.Trigger asChild>
                          <button className={paneStyles.rowActionBtn}><FiMoreVertical size={15} /></button>
                        </DropdownMenu.Trigger>
                        <DropdownMenu.Portal>
                          <DropdownMenu.Content side="left" sideOffset={6} align="start" className={s.dropdownContent} style={{ minWidth: 140 }}>
                            <DropdownMenu.Item className={s.dropdownItem} onSelect={() => openEdit(prov)}>
                              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiEdit2 size={13} /> Edit</span>
                            </DropdownMenu.Item>
                            <DropdownMenu.Separator className={s.dropdownSep} />
                            <DropdownMenu.Item className={s.dropdownItemDanger} onSelect={() => confirmDelete(prov)}>
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
        title={editingId ? 'Edit Provider' : 'New Provider'} width={420}
        footer={<>
          <button className={s.btnCancel} onClick={() => setDrawerOpen(false)}>Cancel</button>
          <button className={s.btnPrimary} onClick={handleSave}>{editingId ? 'Save Changes' : 'Create Provider'}</button>
        </>}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
          <div><label className={s.fieldLabel}>Code *</label>
            <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontFamily: 'monospace', textTransform: 'uppercase' }}
              placeholder="e.g. BTXI" value={form.code} maxLength={10}
              onChange={e => setForm(f => ({ ...f, code: e.target.value }))} disabled={!!editingId} /></div>
          <div><label className={s.fieldLabel}>Name *</label>
            <input className={paneStyles.formInput} style={{ maxWidth: '100%' }}
              placeholder="e.g. BiTaksi" value={form.name}
              onChange={e => setForm(f => ({ ...f, name: e.target.value }))} /></div>
        </div>

        <div><label className={s.fieldLabel}>Type *</label>
          <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
            {PROVIDER_TYPES.map(t => (
              <button key={t} type="button" onClick={() => setForm(f => ({ ...f, type: t }))}
                style={{
                  padding: '0.3rem 0.6rem', borderRadius: 6, border: '1px solid',
                  borderColor: form.type === t ? (TYPE_COLORS[t] || '#6b7280') : 'rgba(128,128,128,0.2)',
                  backgroundColor: form.type === t ? `${TYPE_COLORS[t] || '#6b7280'}15` : 'transparent',
                  color: form.type === t ? (TYPE_COLORS[t] || '#6b7280') : 'inherit',
                  fontSize: '0.72rem', fontWeight: 600, cursor: 'pointer',
                  display: 'flex', alignItems: 'center', gap: '0.25rem',
                }}>{TYPE_ICONS[t]} {t.replace(/_/g, ' ')}</button>
            ))}
          </div>
        </div>

        <SearchableDropdown label="Country" items={countryItems} value={form.countryIsoCode}
          onChange={v => setForm(f => ({ ...f, countryIsoCode: v }))} placeholder="Select country..." allowClear />

        <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.85rem' }}>
          <input type="checkbox" checked={form.isActive}
            onChange={e => setForm(f => ({ ...f, isActive: e.target.checked }))}
            style={{ accentColor: '#C8102E', width: 16, height: 16, cursor: 'pointer' }} />
          Active
        </label>
      </VaulDrawer>

      {/* Delete Drawer */}
      <VaulDrawer open={deleteDrawerOpen} onOpenChange={setDeleteDrawerOpen}
        title="Delete Provider" width={380}
        footer={<>
          <button className={s.btnCancel} onClick={() => setDeleteDrawerOpen(false)}>Cancel</button>
          <button className={s.btnDanger} onClick={handleDelete}>Delete</button>
        </>}>
        <p style={{ fontSize: '0.9rem', margin: 0, lineHeight: 1.5 }}>
          Are you sure you want to delete <strong>{deletingProvider?.name}</strong> ({deletingProvider?.code})?
          <br /><br />
          <span style={{ fontSize: '0.78rem', opacity: 0.6 }}>
            If this provider is used in service areas or transportation edges, deletion will be blocked.
          </span>
        </p>
      </VaulDrawer>
    </>
  );
};

/* Chip */
const Chip: React.FC<{ label: string; active: boolean; onClick: () => void; color?: string; icon?: React.ReactNode }> = ({
  label, active, onClick, color, icon,
}) => (
  <button onClick={onClick} style={{
    padding: '0.2rem 0.55rem', borderRadius: 999, border: '1px solid',
    borderColor: active ? (color || 'rgba(200,16,46,0.4)') : 'rgba(128,128,128,0.2)',
    backgroundColor: active ? `${color || '#C8102E'}15` : 'transparent',
    color: active ? (color || '#C8102E') : 'inherit',
    fontSize: '0.72rem', fontWeight: 600, cursor: 'pointer',
    display: 'flex', alignItems: 'center', gap: '0.25rem', opacity: active ? 1 : 0.65,
  }}>{icon} {label}</button>
);
