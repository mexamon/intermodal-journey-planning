import React, { useState, useMemo, useCallback } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from './ProvidersPane.module.scss';
import * as Dialog from '@radix-ui/react-dialog';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import * as Toast from '@radix-ui/react-toast';
import * as Tooltip from '@radix-ui/react-tooltip';
import {
  FiSearch, FiPlus, FiEdit2, FiTrash2, FiX,
  FiCheck, FiAlertCircle, FiFilter, FiChevronDown,
} from 'react-icons/fi';
import {
  MdFlight, MdDirectionsBus, MdTrain, MdDirectionsSubway,
  MdLocalTaxi, MdDirectionsBoat, MdBusiness,
} from 'react-icons/md';
import { StatusBadge, Pagination, ActionMenu, ConfirmDialog } from '../../components/shared';

/* ═══════════════════════════════════════════════
   Types — matching DB provider entity exactly
   ═══════════════════════════════════════════════ */
type ProviderType = 'AIRLINE' | 'BUS_COMPANY' | 'TRAIN_OPERATOR' | 'METRO_OPERATOR' | 'RIDE_SHARE' | 'FERRY_OPERATOR' | 'OTHER';

interface Provider {
  id: string;
  code: string;
  name: string;
  type: ProviderType;
  countryIsoCode: string | null;
  isActive: boolean;
  configJson: Record<string, unknown> | null;
  version: number;
  createdDate: string;
  lastModifiedDate: string | null;
  deleted: boolean;
}

/* ═══════════════════════════════════════════════
   Metadata
   ═══════════════════════════════════════════════ */
const PROVIDER_TYPES: Record<ProviderType, { label: string; icon: React.ReactNode; color: string }> = {
  AIRLINE:        { label: 'Airline',        icon: <MdFlight />,           color: '#1E88E5' },
  BUS_COMPANY:    { label: 'Bus Company',    icon: <MdDirectionsBus />,    color: '#3b82f6' },
  TRAIN_OPERATOR: { label: 'Train Operator', icon: <MdTrain />,            color: '#22c55e' },
  METRO_OPERATOR: { label: 'Metro Operator', icon: <MdDirectionsSubway />, color: '#f97316' },
  RIDE_SHARE:     { label: 'Ride-share',     icon: <MdLocalTaxi />,        color: '#a855f7' },
  FERRY_OPERATOR: { label: 'Ferry Operator', icon: <MdDirectionsBoat />,   color: '#0ea5e9' },
  OTHER:          { label: 'Other',          icon: <MdBusiness />,         color: '#6b7280' },
};

/* ═══════════════════════════════════════════════
   Mock Data
   ═══════════════════════════════════════════════ */
const INITIAL_PROVIDERS: Provider[] = [
  { id: 'p1', code: 'TK',   name: 'Turkish Airlines',    type: 'AIRLINE',        countryIsoCode: 'TR', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p2', code: 'PC',   name: 'Pegasus Airlines',    type: 'AIRLINE',        countryIsoCode: 'TR', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p3', code: 'LH',   name: 'Lufthansa',           type: 'AIRLINE',        countryIsoCode: 'DE', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p4', code: 'BA',   name: 'British Airways',     type: 'AIRLINE',        countryIsoCode: 'GB', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p5', code: 'AF',   name: 'Air France',          type: 'AIRLINE',        countryIsoCode: 'FR', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p6', code: 'DB',   name: 'Deutsche Bahn',       type: 'TRAIN_OPERATOR', countryIsoCode: 'DE', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p7', code: 'SNCF', name: 'SNCF',                type: 'TRAIN_OPERATOR', countryIsoCode: 'FR', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p8', code: 'TCDD', name: 'TCDD Taşımacılık',    type: 'TRAIN_OPERATOR', countryIsoCode: 'TR', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p9', code: 'FLX',  name: 'FlixBus',             type: 'BUS_COMPANY',    countryIsoCode: 'DE', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p10', code: 'IETT', name: 'İETT',               type: 'BUS_COMPANY',    countryIsoCode: 'TR', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p11', code: 'UBER', name: 'Uber',               type: 'RIDE_SHARE',     countryIsoCode: 'US', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p12', code: 'BOLT', name: 'Bolt',               type: 'RIDE_SHARE',     countryIsoCode: 'EE', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p13', code: 'IBB',  name: 'İstanbul Metro',     type: 'METRO_OPERATOR', countryIsoCode: 'TR', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p14', code: 'IDO',  name: 'İDO Ferries',        type: 'FERRY_OPERATOR', countryIsoCode: 'TR', isActive: true,  configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
  { id: 'p15', code: 'SJ',   name: 'SJ (Sweden)',        type: 'TRAIN_OPERATOR', countryIsoCode: 'SE', isActive: false, configJson: null, version: 1, createdDate: '2026-03-01T10:00:00Z', lastModifiedDate: null, deleted: false },
];

const emptyForm = (): Omit<Provider, 'id' | 'version' | 'createdDate' | 'lastModifiedDate' | 'deleted'> => ({
  code: '', name: '', type: 'AIRLINE', countryIsoCode: '', isActive: true, configJson: null,
});

const PAGE_SIZES = [10, 20, 50] as const;

/* ═══════════════════════════════════════════════
   COMPONENT
   ═══════════════════════════════════════════════ */
export const ProvidersPane: React.FC = () => {
  const [providers, setProviders] = useState<Provider[]>(INITIAL_PROVIDERS);
  const [search, setSearch] = useState('');
  const [typeFilters, setTypeFilters] = useState<Set<string>>(new Set());
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<number>(20);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyForm());
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState('');
  const [toastVariant, setToastVariant] = useState<'success' | 'error'>('success');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingProvider, setDeletingProvider] = useState<Provider | null>(null);
  const [configJsonStr, setConfigJsonStr] = useState('');
  const [jsonError, setJsonError] = useState(false);

  const showToast = useCallback((msg: string, variant: 'success' | 'error' = 'success') => {
    setToastMsg(msg); setToastVariant(variant); setToastOpen(true);
  }, []);

  const toggleType = useCallback((type: string) => {
    setTypeFilters(prev => {
      const next = new Set(prev);
      if (next.has(type)) next.delete(type); else next.add(type);
      return next;
    });
    setPage(0);
  }, []);

  const filtered = useMemo(() => {
    let result = providers.filter(p => !p.deleted);
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(q) || p.code.toLowerCase().includes(q) ||
        (p.countryIsoCode ?? '').toLowerCase().includes(q)
      );
    }
    if (typeFilters.size > 0) result = result.filter(p => typeFilters.has(p.type));
    if (statusFilter === 'ACTIVE') result = result.filter(p => p.isActive);
    if (statusFilter === 'INACTIVE') result = result.filter(p => !p.isActive);
    return result;
  }, [providers, search, typeFilters, statusFilter]);

  /* ── Spring Page model ── */
  const totalElements = filtered.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const paged = useMemo(() =>
    filtered.slice(safePage * pageSize, (safePage + 1) * pageSize)
  , [filtered, safePage, pageSize]);

  const goPage = (p: number) => setPage(Math.max(0, Math.min(p, totalPages - 1)));

  const openAdd = () => { setEditingId(null); setForm(emptyForm()); setConfigJsonStr(''); setJsonError(false); setDialogOpen(true); };
  const openEdit = (p: Provider) => {
    setEditingId(p.id);
    setForm({ code: p.code, name: p.name, type: p.type, countryIsoCode: p.countryIsoCode ?? '', isActive: p.isActive, configJson: p.configJson });
    setConfigJsonStr(p.configJson ? JSON.stringify(p.configJson, null, 2) : '');
    setJsonError(false);
    setDialogOpen(true);
  };

  const handleSave = () => {
    if (!form.code.trim() || !form.name.trim()) { showToast('Code and Name are required.', 'error'); return; }
    const dup = providers.find(p => p.code === form.code.trim().toUpperCase() && p.id !== editingId && !p.deleted);
    if (dup) { showToast(`Provider code "${form.code}" already exists.`, 'error'); return; }
    // Parse config JSON
    let parsedConfig: Record<string, unknown> | null = null;
    if (configJsonStr.trim()) {
      try { parsedConfig = JSON.parse(configJsonStr); } catch { showToast('Invalid JSON in Config field.', 'error'); return; }
    }
    if (editingId) {
      setProviders(prev => prev.map(p => p.id === editingId ? { ...p, ...form, code: form.code.trim().toUpperCase(), countryIsoCode: form.countryIsoCode || null, configJson: parsedConfig, version: p.version + 1, lastModifiedDate: new Date().toISOString() } : p));
      showToast(`Provider "${form.name}" updated.`);
    } else {
      setProviders(prev => [...prev, { id: `p_${Date.now()}`, code: form.code.trim().toUpperCase(), name: form.name.trim(), type: form.type, countryIsoCode: form.countryIsoCode || null, isActive: form.isActive, configJson: parsedConfig, version: 1, createdDate: new Date().toISOString(), lastModifiedDate: null, deleted: false }]);
      showToast(`Provider "${form.name}" created.`);
    }
    setDialogOpen(false);
  };

  const confirmDelete = (p: Provider) => { setDeletingProvider(p); setDeleteDialogOpen(true); };
  const handleDelete = () => {
    if (!deletingProvider) return;
    const hasEdges = Math.random() > 0.7;
    if (hasEdges) { showToast(`Cannot delete "${deletingProvider.name}" — linked to transportation edges.`, 'error'); }
    else { setProviders(prev => prev.map(p => p.id === deletingProvider.id ? { ...p, deleted: true } : p)); showToast(`Provider "${deletingProvider.name}" deleted.`); }
    setDeleteDialogOpen(false); setDeletingProvider(null);
  };

  const toggleActive = (p: Provider) => {
    setProviders(prev => prev.map(pr => pr.id === p.id ? { ...pr, isActive: !pr.isActive, lastModifiedDate: new Date().toISOString() } : pr));
    showToast(`${p.name} ${p.isActive ? 'deactivated' : 'activated'}.`);
  };

  const activeCount = providers.filter(p => !p.deleted && p.isActive).length;
  const inactiveCount = providers.filter(p => !p.deleted && !p.isActive).length;

  /* ═══════════ RENDER ═══════════ */
  return (
    <Toast.Provider swipeDirection="right">
      {/* ── Search Panel (Journey Planner style) ── */}
      <div className={s.searchPanel}>
        <div className={s.searchInputWrap}>
          <FiSearch size={14} className={s.searchIcon} />
          <input className={s.searchInput} placeholder="Search by name, code, or country..."
            value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} />
        </div>

        <div className={s.filterGroup}>
          {/* Type filter */}
          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <button className={s.searchInput} style={{
                width: 'auto', minWidth: 160, paddingLeft: '0.75rem', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: '0.4rem', justifyContent: 'space-between',
                whiteSpace: 'nowrap',
              }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  <FiFilter size={13} style={{ opacity: 0.5, flexShrink: 0 }} />
                  {typeFilters.size === 0 ? 'All Types' : typeFilters.size === 1 ? PROVIDER_TYPES[Array.from(typeFilters)[0] as ProviderType]?.label : `${typeFilters.size} Types`}
                </span>
                <FiChevronDown size={13} style={{ opacity: 0.4, flexShrink: 0 }} />
              </button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content sideOffset={4} align="end" className={s.dropdownContent} onCloseAutoFocus={e => e.preventDefault()}>
                <DropdownMenu.Item className={s.dropdownItem} onSelect={(e) => { e.preventDefault(); setTypeFilters(new Set()); }}>
                  All Types {typeFilters.size === 0 && <FiCheck size={14} />}
                </DropdownMenu.Item>
                <DropdownMenu.Separator className={s.dropdownSep} />
                {Object.entries(PROVIDER_TYPES).map(([key, meta]) => (
                  <DropdownMenu.Item key={key} className={s.dropdownItem} onSelect={(e) => { e.preventDefault(); toggleType(key); }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span style={{ color: meta.color, display: 'flex' }}>{meta.icon}</span>
                      {meta.label}
                    </span>
                    {typeFilters.has(key) && <FiCheck size={14} />}
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>

          {/* Status filter */}
          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <button className={s.searchInput} style={{
                width: 'auto', minWidth: 130, paddingLeft: '0.75rem', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: '0.4rem', justifyContent: 'space-between',
                whiteSpace: 'nowrap',
              }}>
                <span>{statusFilter === 'ALL' ? 'All Status' : statusFilter === 'ACTIVE' ? 'Active' : 'Inactive'}</span>
                <FiChevronDown size={13} style={{ opacity: 0.4 }} />
              </button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content sideOffset={4} align="end" className={s.dropdownContent} style={{ minWidth: 160 }}>
                {(['ALL', 'ACTIVE', 'INACTIVE'] as const).map(sv => (
                  <DropdownMenu.Item key={sv} className={s.dropdownItem} onSelect={() => { setStatusFilter(sv); setPage(0); }}>
                    {sv === 'ALL' ? 'All Status' : sv === 'ACTIVE' ? `Active (${activeCount})` : `Inactive (${inactiveCount})`}
                    {statusFilter === sv && <FiCheck size={14} />}
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>

          {/* Add button */}
          <button className={s.addButton} onClick={openAdd}>
            <FiPlus size={15} /> Add Provider
          </button>
        </div>

        {/* ── Summary Badges ── */}
        <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', width: '100%', marginTop: '0.25rem' }}>
          {Object.entries(PROVIDER_TYPES).map(([code, meta]) => {
            const count = providers.filter(p => p.type === code && !p.deleted).length;
            if (count === 0) return null;
            return (
              <button key={code} onClick={() => toggleType(code)} style={{
                display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
                fontSize: '0.72rem', fontWeight: 600, padding: '0.2rem 0.55rem',
                borderRadius: 999, border: typeFilters.has(code) ? `1.5px solid ${meta.color}` : '1.5px solid transparent',
                backgroundColor: `${meta.color}12`, color: meta.color, cursor: 'pointer',
                transition: 'border-color 0.2s',
              }}>
                {meta.icon} {count}
              </button>
            );
          })}
          <span style={{ fontSize: '0.72rem', opacity: 0.4, alignSelf: 'center', marginLeft: '0.25rem' }}>
            {totalElements} result{totalElements !== 1 ? 's' : ''}
          </span>
        </div>
      </div>

      {/* ── Table ── */}
      <div className={paneStyles.tableWrapper} style={{ maxHeight: 'calc(100vh - 280px)', minHeight: 'calc(100vh - 280px)', overflow: 'auto' }}>
        <table className={paneStyles.dataTable}>
          <thead>
            <tr>
              <th style={{ width: 50 }}>Type</th>
              <th style={{ width: 80 }}>Code</th>
              <th>Provider Name</th>
              <th style={{ width: 80 }}>Country</th>
              <th style={{ width: 80 }}>Status</th>
              <th style={{ width: 50 }}></th>
            </tr>
          </thead>
          <tbody>
            {paged.length === 0 && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '2rem', opacity: 0.4, fontSize: '0.85rem' }}>
                <FiSearch size={20} style={{ marginBottom: 6, display: 'block', margin: '0 auto 6px' }} />
                No providers found.
              </td></tr>
            )}
            {paged.map(p => {
              const meta = PROVIDER_TYPES[p.type];
              return (
                <tr key={p.id} style={{ opacity: p.isActive ? 1 : 0.5 }}>
                  <td style={{ }}>
                    <Tooltip.Provider delayDuration={200}>
                      <Tooltip.Root>
                        <Tooltip.Trigger asChild>
                          <span style={{
                            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                            width: 28, height: 28, borderRadius: 6,
                            backgroundColor: `${meta.color}12`, color: meta.color, fontSize: '1rem',
                          }}>
                            {meta.icon}
                          </span>
                        </Tooltip.Trigger>
                        <Tooltip.Portal>
                          <Tooltip.Content sideOffset={6} className={s.tooltipContent}>
                            {meta.label}
                            <Tooltip.Arrow className={s.tooltipArrow} />
                          </Tooltip.Content>
                        </Tooltip.Portal>
                      </Tooltip.Root>
                    </Tooltip.Provider>
                  </td>
                  <td style={{ }}><strong style={{ fontFamily: 'monospace', letterSpacing: '0.05em', fontSize: '0.82rem' }}>{p.code}</strong></td>
                  <td style={{ }}><span style={{ fontWeight: 500, fontSize: '0.85rem' }}>{p.name}</span></td>
                  <td style={{ fontSize: '0.82rem' }}>{p.countryIsoCode ?? '—'}</td>
                  <td>
                    <StatusBadge status={p.isActive} />
                  </td>
                  <td>
                    <ActionMenu
                      items={[
                        { label: 'Edit', icon: <FiEdit2 size={13} />, onClick: () => openEdit(p) },
                        { label: p.isActive ? 'Deactivate' : 'Activate', icon: <FiCheck size={13} />, onClick: () => toggleActive(p) },
                        { label: 'Delete', icon: <FiTrash2 size={13} />, onClick: () => confirmDelete(p), variant: 'danger' },
                      ]}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <Pagination
        currentPage={safePage}
        totalItems={totalElements}
        pageSize={pageSize}
        pageSizes={PAGE_SIZES}
        onPageChange={goPage}
        onPageSizeChange={(sz) => { setPageSize(sz); setPage(0); }}
      />

      {/* ══════════ Add / Edit Dialog ══════════ */}
      <Dialog.Root open={dialogOpen} onOpenChange={setDialogOpen}>
        <Dialog.Portal>
          <Dialog.Overlay className={s.overlay} />
          <Dialog.Content className={s.dialogContent}>
            <div className={s.dialogHeader}>
              <Dialog.Title className={s.dialogTitle}>
                {editingId ? 'Edit Provider' : 'New Provider'}
              </Dialog.Title>
              <Dialog.Close asChild>
                <button className={s.dialogClose}><FiX size={18} /></button>
              </Dialog.Close>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
              <div>
                <label className={s.fieldLabel}>Code *</label>
                <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} placeholder="e.g. TK, DB, FLX"
                  value={form.code} onChange={e => setForm(f => ({ ...f, code: e.target.value }))} />
              </div>
              <div>
                <label className={s.fieldLabel}>Name *</label>
                <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} placeholder="e.g. Turkish Airlines"
                  value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
              </div>
              <div>
                <label className={s.fieldLabel}>Type *</label>
                <DropdownMenu.Root>
                  <DropdownMenu.Trigger asChild>
                    <button className={paneStyles.formInput} style={{
                      maxWidth: '100%', cursor: 'pointer', textAlign: 'left', fontSize: '0.82rem', padding: '0.5rem 0.6rem',
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{ color: PROVIDER_TYPES[form.type].color, display: 'flex' }}>{PROVIDER_TYPES[form.type].icon}</span>
                        {PROVIDER_TYPES[form.type].label}
                      </span>
                      <FiChevronDown size={14} style={{ opacity: 0.4 }} />
                    </button>
                  </DropdownMenu.Trigger>
                  <DropdownMenu.Portal>
                    <DropdownMenu.Content sideOffset={4} className={s.dropdownContentWide}>
                      {Object.entries(PROVIDER_TYPES).map(([key, meta]) => (
                        <DropdownMenu.Item key={key} className={s.dropdownItem}
                          onSelect={() => setForm(f => ({ ...f, type: key as ProviderType }))}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <span style={{ color: meta.color, display: 'flex' }}>{meta.icon}</span>
                            {meta.label}
                          </span>
                          {form.type === key && <FiCheck size={14} />}
                        </DropdownMenu.Item>
                      ))}
                    </DropdownMenu.Content>
                  </DropdownMenu.Portal>
                </DropdownMenu.Root>
              </div>
              <div>
                <label className={s.fieldLabel}>Country (ISO Code)</label>
                <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }} placeholder="e.g. TR, DE, GB"
                  maxLength={2} value={form.countryIsoCode ?? ''} onChange={e => setForm(f => ({ ...f, countryIsoCode: e.target.value.toUpperCase() }))} />
              </div>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', cursor: 'pointer', fontSize: '0.85rem' }}>
                <input type="checkbox" checked={form.isActive} onChange={e => setForm(f => ({ ...f, isActive: e.target.checked }))}
                  style={{ accentColor: '#C8102E', width: 18, height: 18, cursor: 'pointer' }} />
                Active
              </label>
              {/* Config JSON */}
              <div>
                <label className={s.fieldLabel}>Config JSON <span style={{ opacity: 0.5, fontWeight: 400, textTransform: 'none' }}>(optional)</span></label>
                <textarea
                  className={s.searchInput}
                  style={{
                    width: '100%', minHeight: 80, resize: 'vertical',
                    fontFamily: 'monospace', fontSize: '0.72rem', lineHeight: 1.4,
                    paddingLeft: '0.75rem', paddingTop: '0.45rem',
                    borderColor: jsonError ? '#ef4444' : undefined,
                  }}
                  placeholder='{"iata":"TK","alliance":"Star Alliance"}'
                  value={configJsonStr}
                  onChange={e => {
                    setConfigJsonStr(e.target.value);
                    if (e.target.value.trim()) {
                      try { JSON.parse(e.target.value); setJsonError(false); } catch { setJsonError(true); }
                    } else { setJsonError(false); }
                  }}
                />
                {jsonError && <span style={{ fontSize: '0.7rem', color: '#ef4444', marginTop: '0.2rem', display: 'block' }}>Invalid JSON</span>}
              </div>
            </div>

            <div className={s.dialogFooter}>
              <Dialog.Close asChild>
                <button className={s.btnCancel}>Cancel</button>
              </Dialog.Close>
              <button className={s.btnPrimary} onClick={handleSave}>
                {editingId ? 'Save Changes' : 'Create Provider'}
              </button>
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>

      <ConfirmDialog
        open={deleteDialogOpen}
        onOpenChange={setDeleteDialogOpen}
        title="Delete Provider"
        description={`Are you sure you want to delete ${deletingProvider?.name} (${deletingProvider?.code})? This action may fail if the provider has linked connections or service areas.`}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={handleDelete}
      />

      {/* ══════════ Toast ══════════ */}
      <Toast.Viewport style={{ position: 'fixed', bottom: 24, right: 24, zIndex: 500, display: 'flex', flexDirection: 'column', gap: '0.5rem', maxWidth: 380 }} />
      <Toast.Root open={toastOpen} onOpenChange={setToastOpen} duration={3500}
        className={`${s.toast} ${toastVariant === 'success' ? s.success : s.error}`}>
        {toastVariant === 'success' ? <FiCheck size={16} /> : <FiAlertCircle size={16} />}
        <Toast.Description style={{ fontSize: '0.85rem', fontWeight: 500 }}>{toastMsg}</Toast.Description>
      </Toast.Root>
    </Toast.Provider>
  );
};
