import React, { useState } from 'react';
import * as paneStyles from '../Panes.module.scss';
import * as s from './RoutesPane.module.scss';
import {
  FiStar, FiClock, FiTrash2, FiSearch, FiFilter, FiNavigation,
  FiDollarSign, FiCalendar, FiArrowRight,
} from 'react-icons/fi';
import {
  MdFlight, MdDirectionsBus, MdTrain, MdDirectionsWalk, MdLocalTaxi, MdSubway,
} from 'react-icons/md';

interface SavedRoute {
  id: string;
  origin: string;
  originCode: string;
  destination: string;
  destCode: string;
  segments: { mode: string; from: string; to: string }[];
  totalDuration: string;
  totalCost: string;
  savedAt: string;
  isFavorite: boolean;
}

const MOCK_ROUTES: SavedRoute[] = [
  {
    id: '1', origin: 'Istanbul Airport', originCode: 'IST',
    destination: 'London Heathrow', destCode: 'LHR',
    segments: [{ mode: 'flight', from: 'IST', to: 'LHR' }],
    totalDuration: '3h 50m', totalCost: '€380', savedAt: '2026-03-05', isFavorite: true,
  },
  {
    id: '2', origin: 'Istanbul Airport', originCode: 'IST',
    destination: 'Frankfurt City', destCode: 'FRA',
    segments: [
      { mode: 'flight', from: 'IST', to: 'FRA' },
      { mode: 'bus', from: 'FRA Airport', to: 'FRA City' },
    ],
    totalDuration: '3h 10m', totalCost: '€245', savedAt: '2026-03-04', isFavorite: false,
  },
  {
    id: '3', origin: 'Taksim Square', originCode: 'TAK',
    destination: 'London Liverpool St', destCode: 'LST',
    segments: [
      { mode: 'metro', from: 'TAK', to: 'SAW' },
      { mode: 'flight', from: 'SAW', to: 'STN' },
      { mode: 'train', from: 'STN', to: 'LST' },
    ],
    totalDuration: '4h 47m', totalCost: '€189', savedAt: '2026-03-03', isFavorite: true,
  },
  {
    id: '4', origin: 'Istanbul Airport', originCode: 'IST',
    destination: 'Paris Gare du Nord', destCode: 'CDG',
    segments: [
      { mode: 'flight', from: 'IST', to: 'CDG' },
      { mode: 'walk', from: 'T1', to: 'T2E' },
      { mode: 'train', from: 'CDG', to: 'PAR-N' },
    ],
    totalDuration: '4h 35m', totalCost: '€325', savedAt: '2026-03-02', isFavorite: false,
  },
  {
    id: '5', origin: 'Kadikoy', originCode: 'KDK',
    destination: 'Munich Hbf', destCode: 'MUC',
    segments: [
      { mode: 'taxi', from: 'KDK', to: 'SAW' },
      { mode: 'flight', from: 'SAW', to: 'MUC' },
      { mode: 'train', from: 'MUC Airport', to: 'MUC Hbf' },
    ],
    totalDuration: '5h 15m', totalCost: '€278', savedAt: '2026-03-01', isFavorite: false,
  },
];

const getModeIcon = (mode: string) => {
  switch (mode) {
    case 'flight': return <MdFlight size={14} />;
    case 'bus': return <MdDirectionsBus size={14} />;
    case 'train': return <MdTrain size={14} />;
    case 'walk': return <MdDirectionsWalk size={14} />;
    case 'taxi': return <MdLocalTaxi size={14} />;
    case 'metro': return <MdSubway size={14} />;
    default: return null;
  }
};

const getModeColor = (mode: string) => {
  switch (mode) {
    case 'flight': return '#3b82f6';
    case 'bus': return '#22c55e';
    case 'train': return '#f97316';
    case 'walk': return '#94a3b8';
    case 'taxi': return '#a855f7';
    case 'metro': return '#8b5cf6';
    default: return '#6d7c8a';
  }
};

const getModeLabel = (mode: string) => {
  switch (mode) {
    case 'flight': return 'Flight';
    case 'bus': return 'Bus';
    case 'train': return 'Train';
    case 'walk': return 'Walk';
    case 'taxi': return 'Taxi';
    case 'metro': return 'Metro';
    default: return mode;
  }
};

export const RoutesPane: React.FC = () => {
  const [routes, setRoutes] = useState(MOCK_ROUTES);
  const [filter, setFilter] = useState<'all' | 'favorites'>('all');
  const [search, setSearch] = useState('');

  const filtered = routes
    .filter(r => filter === 'favorites' ? r.isFavorite : true)
    .filter(r => {
      if (!search) return true;
      const q = search.toLowerCase();
      return r.origin.toLowerCase().includes(q) || r.destination.toLowerCase().includes(q)
        || r.originCode.toLowerCase().includes(q) || r.destCode.toLowerCase().includes(q);
    });

  const toggleFav = (id: string) => {
    setRoutes(prev => prev.map(r => r.id === id ? { ...r, isFavorite: !r.isFavorite } : r));
  };

  const deleteRoute = (id: string) => {
    setRoutes(prev => prev.filter(r => r.id !== id));
  };

  return (
    <>
      {/* ── Search & Filter Bar ── */}
      <div className={s.topBar}>
        <div className={s.searchWrap}>
          <FiSearch size={14} className={s.searchIcon} />
          <input
            className={s.searchInput}
            placeholder="Search saved routes..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div className={s.filterGroup}>
          <button className={`${s.filterBtn} ${filter === 'all' ? s.active : ''}`} onClick={() => setFilter('all')}>
            All ({routes.length})
          </button>
          <button className={`${s.filterBtn} ${filter === 'favorites' ? s.active : ''}`} onClick={() => setFilter('favorites')}>
            <FiStar size={12} /> Favorites ({routes.filter(r => r.isFavorite).length})
          </button>
        </div>
      </div>

      {/* ── Route Cards ── */}
      <div className={s.routeList}>
        {filtered.map(route => (
          <div key={route.id} className={s.routeCard}>
            <div className={s.cardTop}>
              <div className={s.routeEndpoints}>
                <div className={s.endpoint}>
                  <span className={s.endpointCode}>{route.originCode}</span>
                  <span className={s.endpointName}>{route.origin}</span>
                </div>
                <div className={s.routeArrow}>
                  <div className={s.arrowLine} />
                  <FiArrowRight size={14} />
                  <div className={s.arrowLine} />
                </div>
                <div className={s.endpoint}>
                  <span className={s.endpointCode}>{route.destCode}</span>
                  <span className={s.endpointName}>{route.destination}</span>
                </div>
              </div>
              <div className={s.cardActions}>
                <button className={s.favBtn} onClick={() => toggleFav(route.id)} title={route.isFavorite ? 'Unfavorite' : 'Favorite'}>
                  <FiStar size={15} style={{ fill: route.isFavorite ? '#f59e0b' : 'none', color: route.isFavorite ? '#f59e0b' : undefined }} />
                </button>
                <button className={s.deleteBtn} onClick={() => deleteRoute(route.id)} title="Delete">
                  <FiTrash2 size={14} />
                </button>
              </div>
            </div>

            {/* Segment Chain */}
            <div className={s.segmentChain}>
              {route.segments.map((seg, i) => (
                <React.Fragment key={i}>
                  <div className={s.segmentPill} style={{ borderColor: `${getModeColor(seg.mode)}30`, color: getModeColor(seg.mode) }}>
                    {getModeIcon(seg.mode)}
                    <span>{getModeLabel(seg.mode)}</span>
                  </div>
                  {i < route.segments.length - 1 && (
                    <div className={s.segmentDot} />
                  )}
                </React.Fragment>
              ))}
            </div>

            {/* Stats */}
            <div className={s.cardStats}>
              <span><FiClock size={12} /> {route.totalDuration}</span>
              <span><FiDollarSign size={12} /> {route.totalCost}</span>
              <span><FiCalendar size={12} /> {route.savedAt}</span>
            </div>
          </div>
        ))}

        {filtered.length === 0 && (
          <div className={s.emptyState}>
            <FiNavigation size={28} />
            <p>{search ? 'No routes match your search' : 'No saved routes yet'}</p>
          </div>
        )}
      </div>
    </>
  );
};
