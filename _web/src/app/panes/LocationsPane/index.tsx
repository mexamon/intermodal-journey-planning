import React, { useState, useEffect, useMemo, useCallback } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from './LocationsPane.module.scss';
import * as Dialog from '@radix-ui/react-dialog';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import * as Toast from '@radix-ui/react-toast';
import {
  FiSearch, FiPlus, FiEdit2, FiTrash2, FiMoreVertical, FiX,
  FiCheck, FiAlertCircle, FiChevronDown, FiChevronLeft, FiChevronRight,
  FiChevronsLeft, FiChevronsRight, FiGlobe, FiMapPin, FiEye, FiEyeOff,
} from 'react-icons/fi';
import {
  MdFlight, MdTrain, MdDirectionsSubway, MdLocationCity,
} from 'react-icons/md';

/* ═══════════════════════════════════════════════
   Types — matching DB location (V004)
   ═══════════════════════════════════════════════ */
type LocationType = 'AIRPORT' | 'CITY' | 'STATION' | 'POI';
type LocationSource = 'INTERNAL' | 'OURAIRPORTS' | 'GOOGLE_PLACES' | 'GTFS' | 'API';

interface Location {
  id: string;
  type: LocationType;
  name: string;
  countryIsoCode: string;
  regionCode: string | null;
  city: string | null;
  timezone: string | null;
  lat: number | null;
  lon: number | null;
  iataCode: string | null;
  icaoCode: string | null;
  isSearchable: boolean;
  searchPriority: number;
  searchAliases: string[];
  source: LocationSource;
  sourcePk: string | null;
  version: number;
  createdDate: string;
  lastModifiedDate: string | null;
  deleted: boolean;
}

/* ═══════════ Icons / Colors ═══════════ */
const TYPE_META: Record<LocationType, { icon: React.ReactNode; color: string; label: string }> = {
  AIRPORT: { icon: <MdFlight size={14} />,          color: '#1E88E5', label: 'Airport' },
  STATION: { icon: <MdTrain size={14} />,           color: '#43A047', label: 'Station' },
  CITY:    { icon: <MdLocationCity size={14} />,     color: '#8E24AA', label: 'City' },
  POI:     { icon: <FiMapPin size={14} />,           color: '#f97316', label: 'POI' },
};

const SOURCE_META: Record<LocationSource, { color: string; label: string }> = {
  INTERNAL:       { color: '#3b82f6', label: 'Internal' },
  OURAIRPORTS:    { color: '#22c55e', label: 'OurAirports' },
  GOOGLE_PLACES:  { color: '#a855f7', label: 'Google Places' },
  GTFS:           { color: '#f97316', label: 'GTFS' },
  API:            { color: '#6b7280', label: 'API' },
};

const resolveEnum = (val: unknown): string => {
  if (val && typeof val === 'object' && 'value' in (val as Record<string, unknown>))
    return (val as Record<string, string>).value;
  return (val as string) || '';
};

import { apiGet, apiPost, apiPut, apiDelete } from '../../api/client';

/* ═══════════ Component ═══════════ */
const emptyForm = (): Omit<Location, 'id' | 'version' | 'createdDate' | 'lastModifiedDate' | 'deleted'> => ({
  type: 'AIRPORT', name: '', countryIsoCode: '', regionCode: null, city: null, timezone: null,
  lat: null, lon: null, iataCode: null, icaoCode: null,
  isSearchable: true, searchPriority: 100, searchAliases: [],
  source: 'INTERNAL', sourcePk: null,
});

export const LocationsPane: React.FC = () => {
  /* ── State ── */
  const [data, setData] = useState<Location[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<'ALL' | LocationType>('ALL');
  const [sourceFilter, setSourceFilter] = useState<'ALL' | LocationSource>('ALL');

  /* Pagination */
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const PAGE_OPTS = [10, 20, 50, 100];

  /* CRUD */
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyForm());
  const [aliasText, setAliasText] = useState('');

  /* Delete */
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingLoc, setDeletingLoc] = useState<Location | null>(null);

  /* Toast */
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState('');
  const [toastType, setToastType] = useState<'success' | 'error'>('success');
  const flash = useCallback((msg: string, type: 'success' | 'error' = 'success') => {
    setToastMsg(msg); setToastType(type); setToastOpen(true);
  }, []);

  /* ── Load data from API ── */
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const result = await apiGet<{ content: Location[] }>('/inventory/locations/search', { page: 0, size: 5000 });
      setData(result?.content || []);
    } catch (err) {
      console.error('Failed to load locations:', err);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  /* ── Filtering ── */
  const filtered = useMemo(() => {
    let result = data;
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(l =>
        l.name.toLowerCase().includes(q) ||
        (l.iataCode ?? '').toLowerCase().includes(q) ||
        (l.icaoCode ?? '').toLowerCase().includes(q) ||
        (l.city ?? '').toLowerCase().includes(q) ||
        l.countryIsoCode.toLowerCase().includes(q) ||
        l.searchAliases.some(a => a.toLowerCase().includes(q))
      );
    }
    if (typeFilter !== 'ALL') result = result.filter(l => l.type === typeFilter);
    if (sourceFilter !== 'ALL') result = result.filter(l => l.source === sourceFilter);
    return result;
  }, [data, search, typeFilter, sourceFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const pageData = filtered.slice(safePage * pageSize, safePage * pageSize + pageSize);

  /* ── CRUD Handlers ── */
  const openAdd = () => { setEditingId(null); setForm(emptyForm()); setAliasText(''); setDialogOpen(true); };

  const openEdit = (loc: Location) => {
    setEditingId(loc.id);
    setForm({
      type: loc.type, name: loc.name, countryIsoCode: loc.countryIsoCode,
      regionCode: loc.regionCode, city: loc.city, timezone: loc.timezone,
      lat: loc.lat, lon: loc.lon, iataCode: loc.iataCode, icaoCode: loc.icaoCode,
      isSearchable: loc.isSearchable, searchPriority: loc.searchPriority,
      searchAliases: loc.searchAliases, source: loc.source, sourcePk: loc.sourcePk,
    });
    setAliasText(loc.searchAliases.join(', '));
    setDialogOpen(true);
  };

  const handleSave = async () => {
    if (!form.name.trim() || !form.countryIsoCode.trim()) { flash('Name and Country are required', 'error'); return; }
    const aliases = aliasText.trim() ? aliasText.split(',').map(a => a.trim()).filter(Boolean) : [];
    const body = { ...form, searchAliases: aliases };
    try {
      if (editingId) {
        await apiPut(`/inventory/locations/${editingId}`, body);
        flash(`Updated "${form.name}"`);
      } else {
        await apiPost('/inventory/locations', body);
        flash(`Added "${form.name}"`);
      }
      setDialogOpen(false);
      loadData();
    } catch { /* interceptor handles toast */ }
  };

  const confirmDelete = (l: Location) => { setDeletingLoc(l); setDeleteDialogOpen(true); };
  const handleDelete = async () => {
    if (!deletingLoc) return;
    try {
      await apiDelete(`/inventory/locations/${deletingLoc.id}`);
      flash(`Deleted "${deletingLoc.name}"`);
      loadData();
    } catch { /* interceptor handles toast */ }
    setDeleteDialogOpen(false); setDeletingLoc(null);
  };

  const toggleSearchable = async (loc: Location) => {
    try {
      await apiPut(`/inventory/locations/${loc.id}`, { isSearchable: !loc.isSearchable });
      flash(`${loc.name} is now ${loc.isSearchable ? 'hidden' : 'searchable'}`);
      loadData();
    } catch { /* interceptor handles toast */ }
  };

  /* ── Stats ── */
  const stats = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const l of data) counts[l.type] = (counts[l.type] || 0) + 1;
    return counts;
  }, [data]);

  /* ═══════════ Render ═══════════ */
  return (
    <Toast.Provider swipeDirection="right">
      {/* ── Search Panel ── */}
      <div className={s.searchPanel}>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap', width: '100%' }}>
          <div className={s.searchInputWrap}>
            <FiSearch className={s.searchIcon} />
            <input className={s.searchInput} placeholder="Search by name, IATA, city, or alias..."
              value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} />
          </div>
          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <button className={s.filterBtn}>
                {typeFilter === 'ALL' ? 'All Types' : TYPE_META[typeFilter].label}
                <FiChevronDown size={12} />
              </button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content sideOffset={4} className={s.dropdownContent}>
                <DropdownMenu.Item className={s.dropdownItem} onSelect={() => { setTypeFilter('ALL'); setPage(0); }}>
                  All Types {typeFilter === 'ALL' && <FiCheck size={14} />}
                </DropdownMenu.Item>
                {(Object.keys(TYPE_META) as LocationType[]).map(t => (
                  <DropdownMenu.Item key={t} className={s.dropdownItem} onSelect={() => { setTypeFilter(t); setPage(0); }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <span style={{ color: TYPE_META[t].color, display: 'flex' }}>{TYPE_META[t].icon}</span>
                      {TYPE_META[t].label} <span style={{ fontSize: '0.7rem', opacity: 0.4 }}>({stats[t] || 0})</span>
                    </span>
                    {typeFilter === t && <FiCheck size={14} />}
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>

          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <button className={s.filterBtn}>
                {sourceFilter === 'ALL' ? 'All Sources' : SOURCE_META[sourceFilter].label}
                <FiChevronDown size={12} />
              </button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content sideOffset={4} className={s.dropdownContent}>
                <DropdownMenu.Item className={s.dropdownItem} onSelect={() => { setSourceFilter('ALL'); setPage(0); }}>
                  All Sources {sourceFilter === 'ALL' && <FiCheck size={14} />}
                </DropdownMenu.Item>
                {(Object.keys(SOURCE_META) as LocationSource[]).map(src => (
                  <DropdownMenu.Item key={src} className={s.dropdownItem} onSelect={() => { setSourceFilter(src); setPage(0); }}>
                    <span style={{ color: SOURCE_META[src].color }}>{SOURCE_META[src].label}</span>
                    {sourceFilter === src && <FiCheck size={14} />}
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>

          <button className={s.addButton} onClick={openAdd}><FiPlus size={14} /> Add Location</button>
        </div>

        {/* ── Summary Badges (toggle filters) ── */}
        <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', width: '100%', marginTop: '0.25rem' }}>
          {(Object.keys(TYPE_META) as LocationType[]).map(type => {
            const count = stats[type] || 0;
            if (count === 0) return null;
            const isActive = typeFilter === type;
            return (
              <button key={type} onClick={() => { setTypeFilter(isActive ? 'ALL' : type); setPage(0); }} style={{
                display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
                fontSize: '0.72rem', fontWeight: 600, padding: '0.2rem 0.55rem',
                borderRadius: 999, border: isActive ? `1.5px solid ${TYPE_META[type].color}` : '1.5px solid transparent',
                backgroundColor: `${TYPE_META[type].color}12`, color: TYPE_META[type].color, cursor: 'pointer',
                transition: 'border-color 0.2s',
              }}>
                {TYPE_META[type].icon} {count}
              </button>
            );
          })}
          <span style={{ fontSize: '0.72rem', opacity: 0.4, alignSelf: 'center', marginLeft: '0.25rem' }}>
            {filtered.length} result{filtered.length !== 1 ? 's' : ''}
          </span>
        </div>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: '3rem', opacity: 0.5 }}>Loading...</div>}

      {/* ── Table ── */}
      {!loading && <div className={paneStyles.tableWrapper} style={{ minHeight: 'calc(100vh - 330px)' }}>
        <table className={paneStyles.dataTable}>
          <thead>
            <tr>
              <th>Type</th>
              <th>Code</th>
              <th>Name</th>
              <th>City</th>
              <th>Country</th>
              <th>Source</th>
              <th>Coordinates</th>
              <th>Priority</th>
              <th>Visible</th>
              <th style={{ width: 36 }}></th>
            </tr>
          </thead>
          <tbody>
            {pageData.map(loc => (
              <tr key={loc.id} style={{ opacity: loc.isSearchable ? 1 : 0.5 }}>
                <td>
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', gap: '0.25rem',
                    padding: '0.15rem 0.45rem', borderRadius: 999, fontSize: '0.7rem', fontWeight: 700,
                    backgroundColor: `${TYPE_META[loc.type].color}12`, color: TYPE_META[loc.type].color,
                  }}>
                    <span style={{ display: 'flex', width: 22, height: 22, alignItems: 'center', justifyContent: 'center',
                      borderRadius: 6, backgroundColor: `${TYPE_META[loc.type].color}18` }}>
                      {TYPE_META[loc.type].icon}
                    </span>
                    {loc.type}
                  </span>
                </td>
                <td>
                  {loc.iataCode ? (
                    <div>
                      <span style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '0.85rem' }}>{loc.iataCode}</span>
                      {loc.icaoCode && <span style={{ fontSize: '0.72rem', opacity: 0.4, marginLeft: 6, fontFamily: 'monospace' }}>{loc.icaoCode}</span>}
                    </div>
                  ) : (
                    <span style={{ opacity: 0.2, fontSize: '0.82rem' }}>—</span>
                  )}
                </td>
                <td><span style={{ fontWeight: 500, fontSize: '0.85rem' }}>{loc.name}</span></td>
                <td style={{ fontSize: '0.85rem' }}>{loc.city ?? '—'}</td>
                <td><span style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: '0.82rem' }}>{loc.countryIsoCode}</span></td>
                <td>
                  <span style={{
                    fontSize: '0.7rem', fontWeight: 600, padding: '0.12rem 0.4rem', borderRadius: 999,
                    backgroundColor: `${SOURCE_META[loc.source].color}12`, color: SOURCE_META[loc.source].color,
                  }}>
                    {SOURCE_META[loc.source].label}
                  </span>
                </td>
                <td style={{ fontSize: '0.78rem', fontFamily: 'monospace', opacity: 0.6 }}>
                  {loc.lat != null && loc.lon != null ? `${loc.lat.toFixed(4)}, ${loc.lon.toFixed(4)}` : '—'}
                </td>
                <td style={{ fontSize: '0.82rem', fontFamily: 'monospace', textAlign: 'center' }}>{loc.searchPriority}</td>
                <td style={{ textAlign: 'center' }}>
                  <button onClick={() => toggleSearchable(loc)}
                    style={{ all: 'unset', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    {loc.isSearchable ?
                      <FiEye size={14} style={{ color: '#22c55e' }} /> :
                      <FiEyeOff size={14} style={{ color: '#ef4444', opacity: 0.6 }} />}
                  </button>
                </td>
                <td>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.rowActionBtn}>
                        <FiMoreVertical size={16} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} align="end" className={s.dropdownContent}>
                        <DropdownMenu.Item className={s.dropdownItem} onSelect={() => openEdit(loc)}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiEdit2 size={13} /> Edit</span>
                        </DropdownMenu.Item>
                        <DropdownMenu.Item className={s.dropdownItem} onSelect={() => toggleSearchable(loc)}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                            {loc.isSearchable ? <><FiEyeOff size={13} /> Hide</> : <><FiEye size={13} /> Show</>}
                          </span>
                        </DropdownMenu.Item>
                        <DropdownMenu.Separator className={s.dropdownSep} />
                        <DropdownMenu.Item className={s.dropdownItemDanger} onSelect={() => confirmDelete(loc)}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><FiTrash2 size={13} /> Delete</span>
                        </DropdownMenu.Item>
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>}

      {/* ── Pagination ── */}
      <div className={s.paginationBar}>
        <div className={s.paginationLeft}>
          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <button className={s.pageSizeBtn}>{pageSize} / page <FiChevronDown size={10} /></button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content sideOffset={4} className={s.dropdownContent}>
                {PAGE_OPTS.map(n => (
                  <DropdownMenu.Item key={n} className={s.dropdownItem} onSelect={() => { setPageSize(n); setPage(0); }}>
                    {n} / page {pageSize === n && <FiCheck size={12} />}
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>
        </div>
        <div className={s.paginationCenter}>
          <button className={s.pageBtn} disabled={safePage === 0} onClick={() => setPage(0)}><FiChevronsLeft size={14} /></button>
          <button className={s.pageBtn} disabled={safePage === 0} onClick={() => setPage(p => p - 1)}><FiChevronLeft size={14} /></button>
          <span className={s.paginationInfo}>{safePage + 1} / {totalPages}</span>
          <button className={s.pageBtn} disabled={safePage >= totalPages - 1} onClick={() => setPage(p => p + 1)}><FiChevronRight size={14} /></button>
          <button className={s.pageBtn} disabled={safePage >= totalPages - 1} onClick={() => setPage(totalPages - 1)}><FiChevronsRight size={14} /></button>
        </div>
        <div className={s.paginationRight}>
          <span style={{ fontSize: '0.72rem', opacity: 0.5 }}>{filtered.length} total</span>
        </div>
      </div>

      {/* ═══ Add/Edit Dialog ═══ */}
      <Dialog.Root open={dialogOpen} onOpenChange={setDialogOpen}>
        <Dialog.Portal>
          <Dialog.Overlay className={s.overlay} />
          <Dialog.Content className={s.dialogContent}>
            <div className={s.dialogHeader}>
              <Dialog.Title className={s.dialogTitle}>{editingId ? 'Edit Location' : 'Add Location'}</Dialog.Title>
              <Dialog.Close className={s.dialogClose}><FiX size={16} /></Dialog.Close>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
              {/* Name & Country */}
              <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>Name *</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    placeholder="e.g. Istanbul Airport" value={form.name}
                    onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>Country *</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    placeholder="e.g. TR" maxLength={2} value={form.countryIsoCode}
                    onChange={e => setForm(f => ({ ...f, countryIsoCode: e.target.value.toUpperCase() }))} />
                </div>
              </div>

              {/* Type & Source */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>Type *</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{ maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                          <span style={{ color: TYPE_META[form.type].color, display: 'flex' }}>{TYPE_META[form.type].icon}</span>
                          {TYPE_META[form.type].label}
                        </span>
                        <FiChevronDown size={12} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} className={s.dropdownContent}>
                        {(Object.keys(TYPE_META) as LocationType[]).map(t => (
                          <DropdownMenu.Item key={t} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, type: t }))}>
                            <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                              <span style={{ color: TYPE_META[t].color, display: 'flex' }}>{TYPE_META[t].icon}</span>
                              {TYPE_META[t].label}
                            </span>
                            {form.type === t && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
                <div>
                  <label className={s.fieldLabel}>Source</label>
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger asChild>
                      <button className={paneStyles.formInput} style={{ maxWidth: '100%', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}>
                        <span style={{ color: SOURCE_META[form.source].color }}>{SOURCE_META[form.source].label}</span>
                        <FiChevronDown size={12} style={{ opacity: 0.4 }} />
                      </button>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Portal>
                      <DropdownMenu.Content sideOffset={4} className={s.dropdownContent}>
                        {(Object.keys(SOURCE_META) as LocationSource[]).map(src => (
                          <DropdownMenu.Item key={src} className={s.dropdownItem} onSelect={() => setForm(f => ({ ...f, source: src }))}>
                            <span style={{ color: SOURCE_META[src].color }}>{SOURCE_META[src].label}</span>
                            {form.source === src && <FiCheck size={14} />}
                          </DropdownMenu.Item>
                        ))}
                      </DropdownMenu.Content>
                    </DropdownMenu.Portal>
                  </DropdownMenu.Root>
                </div>
              </div>

              {/* City, Region, Timezone */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>City</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    placeholder="Istanbul" value={form.city ?? ''}
                    onChange={e => setForm(f => ({ ...f, city: e.target.value || null }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>Region</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    placeholder="TR-34" value={form.regionCode ?? ''}
                    onChange={e => setForm(f => ({ ...f, regionCode: e.target.value || null }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>Timezone</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    placeholder="Europe/Istanbul" value={form.timezone ?? ''}
                    onChange={e => setForm(f => ({ ...f, timezone: e.target.value || null }))} />
                </div>
              </div>

              {/* Codes */}
              <div className={s.sectionTitle}>Codes</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>IATA Code</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    placeholder="IST" maxLength={3} value={form.iataCode ?? ''}
                    onChange={e => setForm(f => ({ ...f, iataCode: e.target.value.toUpperCase() || null }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>ICAO Code</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    placeholder="LTFM" maxLength={4} value={form.icaoCode ?? ''}
                    onChange={e => setForm(f => ({ ...f, icaoCode: e.target.value.toUpperCase() || null }))} />
                </div>
              </div>

              {/* Coordinates */}
              <div className={s.sectionTitle}>Coordinates</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>Latitude</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    type="number" step="0.0001" placeholder="41.2611" value={form.lat ?? ''}
                    onChange={e => setForm(f => ({ ...f, lat: e.target.value ? parseFloat(e.target.value) : null }))} />
                </div>
                <div>
                  <label className={s.fieldLabel}>Longitude</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    type="number" step="0.0001" placeholder="28.7419" value={form.lon ?? ''}
                    onChange={e => setForm(f => ({ ...f, lon: e.target.value ? parseFloat(e.target.value) : null }))} />
                </div>
              </div>

              {/* Search config */}
              <div className={s.sectionTitle}>Search Configuration</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                <div>
                  <label className={s.fieldLabel}>Search Priority</label>
                  <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                    type="number" min={0} max={999} value={form.searchPriority}
                    onChange={e => setForm(f => ({ ...f, searchPriority: parseInt(e.target.value) || 0 }))} />
                </div>
                <div style={{ display: 'flex', alignItems: 'flex-end', paddingBottom: '0.2rem' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.82rem' }}>
                    <input type="checkbox" checked={form.isSearchable}
                      onChange={e => setForm(f => ({ ...f, isSearchable: e.target.checked }))}
                      style={{ accentColor: '#1E88E5', width: 16, height: 16, cursor: 'pointer' }} />
                    Searchable
                  </label>
                </div>
              </div>
              <div>
                <label className={s.fieldLabel}>Search Aliases <span style={{ opacity: 0.4, fontWeight: 400, textTransform: 'none' }}>(comma-separated)</span></label>
                <input className={paneStyles.formInput} style={{ maxWidth: '100%', fontSize: '0.82rem', padding: '0.5rem 0.6rem' }}
                  placeholder="e.g. Arnavutköy, New Istanbul" value={aliasText}
                  onChange={e => setAliasText(e.target.value)} />
              </div>
            </div>

            <div className={s.dialogFooter}>
              <button className={s.btnCancel} onClick={() => setDialogOpen(false)}>Cancel</button>
              <button className={s.btnPrimary} onClick={handleSave}>{editingId ? 'Update' : 'Create'}</button>
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>

      {/* ═══ Delete Dialog ═══ */}
      <Dialog.Root open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <Dialog.Portal>
          <Dialog.Overlay className={s.overlay} />
          <Dialog.Content className={s.dialogContentSmall}>
            <div className={s.dialogHeader}>
              <Dialog.Title className={s.dialogTitle}>Delete Location</Dialog.Title>
              <Dialog.Close className={s.dialogClose}><FiX size={16} /></Dialog.Close>
            </div>
            <p style={{ fontSize: '0.85rem', margin: '0 0 0.5rem' }}>
              Are you sure you want to delete <strong>{deletingLoc?.name}</strong>?
              {deletingLoc?.iataCode && <> (<code>{deletingLoc.iataCode}</code>)</>}
            </p>
            <p style={{ fontSize: '0.75rem', opacity: 0.5, margin: '0 0 0.75rem' }}>
              This may break existing connections that reference this location.
            </p>
            <div className={s.dialogFooter}>
              <button className={s.btnCancel} onClick={() => setDeleteDialogOpen(false)}>Cancel</button>
              <button className={s.btnDanger} onClick={handleDelete}>Delete</button>
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>

      {/* ═══ Toast ═══ */}
      <Toast.Viewport style={{ position: 'fixed', bottom: 24, right: 24, zIndex: 9999, display: 'flex', flexDirection: 'column', gap: 8 }} />
      <Toast.Root className={`${s.toast} ${s[toastType]}`} open={toastOpen} onOpenChange={setToastOpen} duration={2500}>
        {toastType === 'success' ? <FiCheck size={16} /> : <FiAlertCircle size={16} />}
        <Toast.Description>{toastMsg}</Toast.Description>
      </Toast.Root>
    </Toast.Provider>
  );
};
