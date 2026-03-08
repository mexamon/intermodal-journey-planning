import React, { useState, useEffect, useMemo, useCallback } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from './TransportModesPane.module.scss';
import * as Dialog from '@radix-ui/react-dialog';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import * as Toast from '@radix-ui/react-toast';
import {
  FiSearch, FiPlus, FiEdit2, FiTrash2, FiMoreVertical, FiX,
  FiCheck, FiAlertCircle, FiChevronDown,
} from 'react-icons/fi';
import {
  MdFlight, MdDirectionsBus, MdTrain, MdDirectionsSubway,
  MdLocalTaxi, MdDirectionsBoat, MdDirectionsWalk, MdPedalBike,
} from 'react-icons/md';
import { apiGet, apiPost, apiPut, apiDelete } from '../../api/client';

/* ═══════════════════════════════════════════════
   Types — matching DB transport_mode exactly (V005)
   ═══════════════════════════════════════════════ */
type ModeCategory = 'AIR' | 'GROUND_FIXED' | 'GROUND_FLEX' | 'PEDESTRIAN';
type CoverageType = 'POINT_TO_POINT' | 'FIXED_STOP' | 'NETWORK' | 'COMPUTED';
type EdgeResolution = 'STATIC' | 'API_DYNAMIC' | 'COMPUTED' | 'HYBRID';

interface TransportMode {
  id: string;
  code: string;
  name: string;
  category: ModeCategory;
  coverageType: CoverageType;
  edgeResolution: EdgeResolution;
  requiresStop: boolean;
  maxWalkingAccessM: number | null;
  defaultSpeedKmh: number;
  apiProvider: string | null;
  icon: string;
  colorHex: string;
  isActive: boolean;
  configJson: Record<string, unknown> | null;
  sortOrder: number;
  version: number;
  createdDate: string;
  lastModifiedDate: string | null;
  deleted: boolean;
}

/* ═══════════════════════════════════════════════
   Icon & color mapping
   ═══════════════════════════════════════════════ */
const MODE_ICONS: Record<string, React.ReactNode> = {
  FLIGHT:  <MdFlight />,
  BUS:     <MdDirectionsBus />,
  TRAIN:   <MdTrain />,
  SUBWAY:  <MdDirectionsSubway />,
  UBER:    <MdLocalTaxi />,
  FERRY:   <MdDirectionsBoat />,
  WALKING: <MdDirectionsWalk />,
  BIKE:    <MdPedalBike />,
};

const COVERAGE_COLORS: Record<CoverageType, string> = {
  POINT_TO_POINT: '#3b82f6',
  FIXED_STOP: '#22c55e',
  NETWORK: '#f97316',
  COMPUTED: '#6b7280',
};

const RESOLUTION_COLORS: Record<EdgeResolution, string> = {
  STATIC: '#22c55e',
  API_DYNAMIC: '#a855f7',
  COMPUTED: '#6b7280',
  HYBRID: '#f97316',
};

const CATEGORY_LABELS: Record<ModeCategory, string> = {
  AIR: 'Air', GROUND_FIXED: 'Ground (Fixed)', GROUND_FLEX: 'Ground (Flex)', PEDESTRIAN: 'Pedestrian',
};

/* ═══════════ Enum resolver (Java enums may come as objects) ═══════════ */
const resolveEnum = (val: unknown): string => {
  if (val && typeof val === 'object' && 'value' in (val as Record<string, unknown>))
    return (val as Record<string, string>).value;
  return (val as string) || '';
};
const mapMode = (m: any): TransportMode => ({
  id: m.id,
  code: m.code || '',
  name: m.name || '',
  category: (resolveEnum(m.category) || 'GROUND_FIXED') as ModeCategory,
  coverageType: (resolveEnum(m.coverageType) || 'FIXED_STOP') as CoverageType,
  edgeResolution: (resolveEnum(m.edgeResolution) || 'STATIC') as EdgeResolution,
  requiresStop: m.requiresStop ?? true,
  maxWalkingAccessM: m.maxWalkingAccessM ?? null,
  defaultSpeedKmh: m.defaultSpeedKmh ?? 40,
  apiProvider: m.apiProvider || null,
  icon: m.icon || 'bus',
  colorHex: m.colorHex || '#3b82f6',
  isActive: m.isActive ?? true,
  configJson: m.configJson ?? null,
  sortOrder: m.sortOrder ?? 0,
  version: m.version ?? 1,
  createdDate: m.createdDate || '',
  lastModifiedDate: m.lastModifiedDate || null,
  deleted: m.deleted ?? false,
});

type FormState = Omit<TransportMode, 'id' | 'version' | 'createdDate' | 'lastModifiedDate' | 'deleted'>;

const emptyForm = (): FormState => ({
  code: '', name: '', category: 'GROUND_FIXED', coverageType: 'FIXED_STOP',
  edgeResolution: 'STATIC', requiresStop: true, maxWalkingAccessM: null,
  defaultSpeedKmh: 40, apiProvider: null, icon: 'bus', colorHex: '#3b82f6',
  isActive: true, configJson: null, sortOrder: 0,
});

/* ═══════════════════════════════════════════════
   COMPONENT
   ═══════════════════════════════════════════════ */
export const TransportModesPane: React.FC = () => {
  const [modes, setModes] = useState<TransportMode[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState('');
  const [toastVariant, setToastVariant] = useState<'success' | 'error'>('success');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingMode, setDeletingMode] = useState<TransportMode | null>(null);

  const showToast = useCallback((msg: string, variant: 'success' | 'error' = 'success') => {
    setToastMsg(msg); setToastVariant(variant); setToastOpen(true);
  }, []);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const raw = await apiGet<any[]>('/transport/modes', { all: true });
      setModes((raw || []).map(mapMode));
    } catch (err) { console.error('Failed to load modes:', err); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const filtered = useMemo(() => {
    let result = modes.filter(m => !m.deleted);
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(m =>
        m.code.toLowerCase().includes(q) || m.name.toLowerCase().includes(q) ||
        m.category.toLowerCase().includes(q)
      );
    }
    return result.sort((a, b) => a.sortOrder - b.sortOrder);
  }, [modes, search]);

  const openAdd = () => { setEditingId(null); setForm(emptyForm()); setDialogOpen(true); };
  const openEdit = (m: TransportMode) => {
    setEditingId(m.id);
    setForm({
      code: m.code, name: m.name, category: m.category, coverageType: m.coverageType,
      edgeResolution: m.edgeResolution, requiresStop: m.requiresStop,
      maxWalkingAccessM: m.maxWalkingAccessM, defaultSpeedKmh: m.defaultSpeedKmh,
      apiProvider: m.apiProvider, icon: m.icon, colorHex: m.colorHex,
      isActive: m.isActive, configJson: m.configJson, sortOrder: m.sortOrder,
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    if (!form.code.trim() || !form.name.trim()) { showToast('Code and Name are required.', 'error'); return; }
    const body = {
      code: form.code.trim().toUpperCase(), name: form.name.trim(),
      category: form.category, coverageType: form.coverageType,
      edgeResolution: form.edgeResolution, requiresStop: form.requiresStop,
      maxWalkingAccessM: form.maxWalkingAccessM, defaultSpeedKmh: form.defaultSpeedKmh,
      apiProvider: form.apiProvider, icon: form.icon, colorHex: form.colorHex,
      isActive: form.isActive, configJson: form.configJson, sortOrder: form.sortOrder,
    };
    try {
      if (editingId) {
        await apiPut(`/transport/modes/${editingId}`, body);
        showToast(`Mode "${form.name}" updated.`);
      } else {
        await apiPost('/transport/modes', body);
        showToast(`Mode "${form.name}" created.`);
      }
      setDialogOpen(false);
      loadData();
    } catch { /* interceptor handles toast */ }
  };

  const confirmDelete = (m: TransportMode) => { setDeletingMode(m); setDeleteDialogOpen(true); };
  const handleDelete = async () => {
    if (!deletingMode) return;
    try {
      await apiDelete(`/transport/modes/${deletingMode.id}`);
      showToast(`Mode "${deletingMode.name}" deleted.`);
      loadData();
    } catch { /* interceptor handles toast */ }
    setDeleteDialogOpen(false); setDeletingMode(null);
  };

  const toggleActive = async (m: TransportMode) => {
    try {
      await apiPut(`/transport/modes/${m.id}`, { isActive: !m.isActive });
      showToast(`${m.name} ${m.isActive ? 'deactivated' : 'activated'}.`);
      loadData();
    } catch { /* interceptor handles toast */ }
  };

  /* ═══════════ RENDER ═══════════ */
  return (
    <Toast.Provider swipeDirection="right">
      {/* ── Search Panel ── */}
      <div className={s.searchPanel}>
        <div className={s.searchInputWrap}>
          <FiSearch size={14} className={s.searchIcon} />
          <input className={s.searchInput} placeholder="Search transport modes..."
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <button className={s.addButton} onClick={openAdd}>
          <FiPlus size={15} /> Add Mode
        </button>
      </div>

      {/* ── Mode Cards Grid ── */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
        gap: '0.75rem',
      }}>
        {filtered.length === 0 && (
          <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '3rem', opacity: 0.4 }}>
            <FiSearch size={24} style={{ display: 'block', margin: '0 auto 8px' }} />
            No transport modes found.
          </div>
        )}
        {filtered.map(mode => {
          const modeIcon = MODE_ICONS[mode.code] || <MdDirectionsBus />;
          return (
            <div key={mode.id} className={s.modeCard} style={{ opacity: mode.isActive ? 1 : 0.5 }}>
              {/* Header */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.7rem' }}>
                  <div style={{
                    width: 40, height: 40, borderRadius: 10, display: 'flex',
                    alignItems: 'center', justifyContent: 'center',
                    fontSize: '1.2rem', backgroundColor: `${mode.colorHex}18`, color: mode.colorHex,
                  }}>
                    {modeIcon}
                  </div>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{mode.name}</div>
                    <div style={{ fontSize: '0.7rem', fontFamily: 'monospace', opacity: 0.5 }}>{mode.code}</div>
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <span style={{
                    fontSize: '0.62rem', fontWeight: 700, padding: '0.12rem 0.45rem', borderRadius: 999,
                    backgroundColor: mode.isActive ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)',
                    color: mode.isActive ? '#22c55e' : '#ef4444',
                  }}>
                    {mode.isActive ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                  {/* Three-dot menu */}
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.rowActionBtn}><FiMoreVertical size={16} /></button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content side="left" sideOffset={6} align="start" className={s.dropdownContent} style={{ minWidth: 150 }}>
                        <DropdownMenu.Item className={s.dropdownItem} onSelect={() => openEdit(mode)}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiEdit2 size={13} /> Edit</span>
                        </DropdownMenu.Item>
                        <DropdownMenu.Item className={s.dropdownItem} onSelect={() => toggleActive(mode)}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                            {mode.isActive ? <><FiAlertCircle size={13} /> Deactivate</> : <><FiCheck size={13} /> Activate</>}
                          </span>
                        </DropdownMenu.Item>
                        <DropdownMenu.Separator className={s.dropdownSep} />
                        <DropdownMenu.Item className={s.dropdownItemDanger} onSelect={() => confirmDelete(mode)}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiTrash2 size={13} /> Delete</span>
                        </DropdownMenu.Item>
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
              </div>

              {/* Badges */}
              <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                <span style={{
                  fontSize: '0.62rem', fontWeight: 700, padding: '0.15rem 0.5rem', borderRadius: 999,
                  backgroundColor: `${COVERAGE_COLORS[mode.coverageType]}15`,
                  color: COVERAGE_COLORS[mode.coverageType],
                }}>
                  {mode.coverageType.replace(/_/g, ' ')}
                </span>
                <span style={{
                  fontSize: '0.62rem', fontWeight: 700, padding: '0.15rem 0.5rem', borderRadius: 999,
                  backgroundColor: `${RESOLUTION_COLORS[mode.edgeResolution]}15`,
                  color: RESOLUTION_COLORS[mode.edgeResolution],
                }}>
                  {mode.edgeResolution.replace(/_/g, ' ')}
                </span>
                <span style={{
                  fontSize: '0.62rem', fontWeight: 600, padding: '0.15rem 0.5rem', borderRadius: 999,
                  backgroundColor: 'rgba(107,114,128,0.1)', color: 'inherit', opacity: 0.6,
                }}>
                  {CATEGORY_LABELS[mode.category]}
                </span>
              </div>

              {/* Stats */}
              <div style={{
                display: 'grid', gridTemplateColumns: '1fr 1fr 1fr',
                gap: '0.4rem', fontSize: '0.78rem',
              }}>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', fontWeight: 600, opacity: 0.45, letterSpacing: '0.04em' }}>Speed</span>
                  <span style={{ fontWeight: 600 }}>{mode.defaultSpeedKmh} km/h</span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', fontWeight: 600, opacity: 0.45, letterSpacing: '0.04em' }}>Walking</span>
                  <span style={{ fontWeight: 600 }}>{mode.maxWalkingAccessM != null && mode.maxWalkingAccessM > 0 ? `${mode.maxWalkingAccessM}m` : '—'}</span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <span style={{ fontSize: '0.6rem', textTransform: 'uppercase', fontWeight: 600, opacity: 0.45, letterSpacing: '0.04em' }}>Stops</span>
                  <span style={{ fontWeight: 600 }}>{mode.requiresStop ? 'Required' : 'None'}</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* ══════════ Add / Edit Dialog ══════════ */}
      <Dialog.Root open={dialogOpen} onOpenChange={setDialogOpen}>
        <Dialog.Portal>
          <Dialog.Overlay className={s.overlay} />
          <Dialog.Content className={s.dialogContent}>
            <div className={s.dialogHeader}>
              <Dialog.Title className={s.dialogTitle}>
                {editingId ? 'Edit Transport Mode' : 'New Transport Mode'}
              </Dialog.Title>
              <Dialog.Close asChild>
                <button className={s.dialogClose}><FiX size={18} /></button>
              </Dialog.Close>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label className={s.fieldLabel}>Code *</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} placeholder="e.g. FLIGHT"
                    value={form.code} onChange={e => setForm(f => ({ ...f, code: e.target.value }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>Name *</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} placeholder="e.g. Flight"
                    value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                {/* Category */}
                <div>
                  <label className={s.fieldLabel}>Category</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{
                        maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center',
                        justifyContent: 'space-between', fontSize: '0.85rem',
                      }}>
                        <span>{CATEGORY_LABELS[form.category]}</span>
                        <FiChevronDown size={13} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {(Object.entries(CATEGORY_LABELS) as [ModeCategory, string][]).map(([key, label]) => (
                          <DropdownMenu.Item key={key} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, category: key }))}>
                            {label} {form.category === key && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>

                {/* Coverage type */}
                <div>
                  <label className={s.fieldLabel}>Coverage Type</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{
                        maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center',
                        justifyContent: 'space-between', fontSize: '0.85rem',
                      }}>
                        <span style={{ color: COVERAGE_COLORS[form.coverageType] }}>{form.coverageType.replace(/_/g, ' ')}</span>
                        <FiChevronDown size={13} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {(Object.keys(COVERAGE_COLORS) as CoverageType[]).map(key => (
                          <DropdownMenu.Item key={key} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, coverageType: key }))}>
                            <span style={{ color: COVERAGE_COLORS[key] }}>{key.replace(/_/g, ' ')}</span>
                            {form.coverageType === key && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                {/* Edge Resolution */}
                <div>
                  <label className={s.fieldLabel}>Edge Resolution</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{
                        maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center',
                        justifyContent: 'space-between', fontSize: '0.85rem',
                      }}>
                        <span style={{ color: RESOLUTION_COLORS[form.edgeResolution] }}>{form.edgeResolution.replace(/_/g, ' ')}</span>
                        <FiChevronDown size={13} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} align="start" className={s.dropdownContent}>
                        {(Object.keys(RESOLUTION_COLORS) as EdgeResolution[]).map(key => (
                          <DropdownMenu.Item key={key} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, edgeResolution: key }))}>
                            <span style={{ color: RESOLUTION_COLORS[key] }}>{key.replace(/_/g, ' ')}</span>
                            {form.edgeResolution === key && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>

                {/* Speed */}
                <div>
                  <label className={s.fieldLabel}>Default Speed (km/h)</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} type="number" min={1}
                    value={form.defaultSpeedKmh} onChange={e => setForm(f => ({ ...f, defaultSpeedKmh: parseInt(e.target.value) || 0 }))} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label className={s.fieldLabel}>Max Walking Access (m)</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} type="number" min={0}
                    placeholder="e.g. 800"
                    value={form.maxWalkingAccessM ?? ''} onChange={e => setForm(f => ({ ...f, maxWalkingAccessM: e.target.value ? parseInt(e.target.value) : null }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>Sort Order</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} type="number" min={0}
                    value={form.sortOrder} onChange={e => setForm(f => ({ ...f, sortOrder: parseInt(e.target.value) || 0 }))} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label className={s.fieldLabel}>Color Hex</label>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <input className={paneStyles.formInput} style={{ maxWidth: '100%', flex: 1 }} placeholder="#1E88E5"
                      value={form.colorHex} onChange={e => setForm(f => ({ ...f, colorHex: e.target.value }))} />
                    <div style={{ width: 32, height: 32, borderRadius: 8, backgroundColor: form.colorHex, border: '1px solid rgba(128,128,128,0.3)', flexShrink: 0 }} />
                  </div>
                </div>
                <div>
                  <label className={s.fieldLabel}>API Provider</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%' }} placeholder="e.g. GOOGLE (optional)"
                    value={form.apiProvider ?? ''} onChange={e => setForm(f => ({ ...f, apiProvider: e.target.value || null }))} />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '1.5rem' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.85rem' }}>
                  <input type="checkbox" checked={form.requiresStop} onChange={e => setForm(f => ({ ...f, requiresStop: e.target.checked }))}
                    style={{ accentColor: '#C8102E', width: 16, height: 16, cursor: 'pointer' }} />
                  Requires Stop
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.85rem' }}>
                  <input type="checkbox" checked={form.isActive} onChange={e => setForm(f => ({ ...f, isActive: e.target.checked }))}
                    style={{ accentColor: '#C8102E', width: 16, height: 16, cursor: 'pointer' }} />
                  Active
                </label>
              </div>
            </div>

            <div className={s.dialogFooter}>
              <Dialog.Close asChild>
                <button className={s.btnCancel}>Cancel</button>
              </Dialog.Close>
              <button className={s.btnPrimary} onClick={handleSave}>
                {editingId ? 'Save Changes' : 'Create Mode'}
              </button>
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>

      {/* ══════════ Delete Confirm Dialog ══════════ */}
      <Dialog.Root open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <Dialog.Portal>
          <Dialog.Overlay className={s.overlay} />
          <Dialog.Content className={s.dialogContentSmall}>
            <div className={s.dialogHeader}>
              <Dialog.Title className={s.dialogTitle}>Delete Mode</Dialog.Title>
              <Dialog.Close asChild>
                <button className={s.dialogClose}><FiX size={18} /></button>
              </Dialog.Close>
            </div>
            <p style={{ fontSize: '0.9rem', margin: '0 0 0.5rem', lineHeight: 1.5 }}>
              Are you sure you want to delete <strong>{deletingMode?.name}</strong>?
              This cannot be undone.
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
