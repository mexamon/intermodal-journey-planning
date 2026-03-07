import React, { useState, useMemo } from 'react';
import * as paneStyles from '../Panes.module.scss';
import { FiSearch, FiFilter, FiGlobe, FiMapPin } from 'react-icons/fi';
import { MdFlight } from 'react-icons/md';

// ── Types matching location + airport_profile entities ──
interface Location {
  id: string;
  type: 'AIRPORT' | 'CITY' | 'STATION' | 'POI';
  name: string;
  iataCode?: string;
  icaoCode?: string;
  city: string;
  country: string;
  lat: number;
  lon: number;
  isSearchable: boolean;
  searchPriority: number;
  searchAliases: string[];
  elevation?: number;
}

const MOCK_LOCATIONS: Location[] = [
  { id: '1', type: 'AIRPORT', name: 'Istanbul Airport', iataCode: 'IST', icaoCode: 'LTFM', city: 'Istanbul', country: 'TR', lat: 41.2611, lon: 28.7419, isSearchable: true, searchPriority: 200, searchAliases: ['Arnavutköy'], elevation: 99 },
  { id: '2', type: 'AIRPORT', name: 'Sabiha Gökçen International Airport', iataCode: 'SAW', icaoCode: 'LTFJ', city: 'Istanbul', country: 'TR', lat: 40.8986, lon: 29.3092, isSearchable: true, searchPriority: 200, searchAliases: ['Pendik'], elevation: 43 },
  { id: '3', type: 'AIRPORT', name: 'London Heathrow Airport', iataCode: 'LHR', icaoCode: 'EGLL', city: 'London', country: 'GB', lat: 51.4700, lon: -0.4543, isSearchable: true, searchPriority: 200, searchAliases: [], elevation: 25 },
  { id: '4', type: 'AIRPORT', name: 'Frankfurt am Main Airport', iataCode: 'FRA', icaoCode: 'EDDF', city: 'Frankfurt', country: 'DE', lat: 50.0333, lon: 8.5706, isSearchable: true, searchPriority: 200, searchAliases: [], elevation: 111 },
  { id: '5', type: 'AIRPORT', name: 'Charles de Gaulle Airport', iataCode: 'CDG', icaoCode: 'LFPG', city: 'Paris', country: 'FR', lat: 49.0097, lon: 2.5479, isSearchable: true, searchPriority: 200, searchAliases: ['Roissy'], elevation: 119 },
  { id: '6', type: 'AIRPORT', name: 'London Stansted Airport', iataCode: 'STN', icaoCode: 'EGSS', city: 'London', country: 'GB', lat: 51.8850, lon: 0.2350, isSearchable: true, searchPriority: 180, searchAliases: [], elevation: 106 },
  { id: '7', type: 'AIRPORT', name: 'Munich Airport', iataCode: 'MUC', icaoCode: 'EDDM', city: 'Munich', country: 'DE', lat: 48.3538, lon: 11.7861, isSearchable: true, searchPriority: 200, searchAliases: ['München'], elevation: 453 },
  { id: '8', type: 'AIRPORT', name: 'Antalya Airport', iataCode: 'AYT', icaoCode: 'LTAI', city: 'Antalya', country: 'TR', lat: 36.8987, lon: 30.8005, isSearchable: true, searchPriority: 180, searchAliases: [], elevation: 54 },
  { id: '9', type: 'STATION', name: 'Frankfurt Hauptbahnhof', iataCode: undefined, icaoCode: undefined, city: 'Frankfurt', country: 'DE', lat: 50.1071, lon: 8.6637, isSearchable: true, searchPriority: 160, searchAliases: ['FRA-C', 'Frankfurt Hbf'] },
  { id: '10', type: 'STATION', name: 'London Liverpool Street', iataCode: undefined, icaoCode: undefined, city: 'London', country: 'GB', lat: 51.5178, lon: -0.0823, isSearchable: true, searchPriority: 150, searchAliases: ['LST'] },
  { id: '11', type: 'STATION', name: 'Gare du Nord', iataCode: undefined, icaoCode: undefined, city: 'Paris', country: 'FR', lat: 48.8809, lon: 2.3553, isSearchable: true, searchPriority: 170, searchAliases: ['PAR-N'] },
  { id: '12', type: 'CITY', name: 'Taksim', iataCode: undefined, icaoCode: undefined, city: 'Istanbul', country: 'TR', lat: 41.0370, lon: 28.9850, isSearchable: true, searchPriority: 120, searchAliases: ['TAK', 'Taksim Meydanı'] },
  { id: '13', type: 'POI', name: 'Terminal 2E', iataCode: undefined, icaoCode: undefined, city: 'Paris', country: 'FR', lat: 49.0088, lon: 2.5656, isSearchable: false, searchPriority: 50, searchAliases: ['T2E'] },
  { id: '14', type: 'AIRPORT', name: 'Amsterdam Schiphol Airport', iataCode: 'AMS', icaoCode: 'EHAM', city: 'Amsterdam', country: 'NL', lat: 52.3086, lon: 4.7639, isSearchable: true, searchPriority: 200, searchAliases: [], elevation: -3 },
];

const TYPE_ICONS: Record<string, React.ReactNode> = {
  AIRPORT: <MdFlight style={{ color: '#C8102E' }} />,
  STATION: <FiMapPin style={{ color: '#22c55e' }} />,
  CITY: <FiGlobe style={{ color: '#3b82f6' }} />,
  POI: <FiMapPin style={{ color: '#f97316' }} />,
};

const TYPE_COLORS: Record<string, string> = {
  AIRPORT: '#C8102E', STATION: '#22c55e', CITY: '#3b82f6', POI: '#f97316',
};

export const LocationsPane: React.FC = () => {
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [searchableFilter, setSearchableFilter] = useState('ALL');

  const filtered = useMemo(() => {
    let result = MOCK_LOCATIONS;
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(l =>
        l.name.toLowerCase().includes(q) ||
        (l.iataCode ?? '').toLowerCase().includes(q) ||
        l.city.toLowerCase().includes(q) ||
        l.searchAliases.some(a => a.toLowerCase().includes(q))
      );
    }
    if (typeFilter !== 'ALL') result = result.filter(l => l.type === typeFilter);
    if (searchableFilter !== 'ALL') result = result.filter(l => searchableFilter === 'YES' ? l.isSearchable : !l.isSearchable);
    return result;
  }, [search, typeFilter, searchableFilter]);

  return (
    <>
      <div className={paneStyles.usersActions}>
        <div className={paneStyles.userSearchWrapper}>
          <FiSearch size={14} />
          <input placeholder="Search by name, IATA, city, or alias..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <FiFilter size={14} style={{ opacity: 0.5 }} />
          <select className={paneStyles.formInput} style={{ maxWidth: 140, padding: '0.4rem 0.6rem', fontSize: '0.85rem' }}
            value={typeFilter} onChange={e => setTypeFilter(e.target.value)}>
            <option value="ALL">All Types</option>
            <option value="AIRPORT">Airports</option>
            <option value="STATION">Stations</option>
            <option value="CITY">Cities</option>
            <option value="POI">POI</option>
          </select>
          <select className={paneStyles.formInput} style={{ maxWidth: 150, padding: '0.4rem 0.6rem', fontSize: '0.85rem' }}
            value={searchableFilter} onChange={e => setSearchableFilter(e.target.value)}>
            <option value="ALL">All Visibility</option>
            <option value="YES">Searchable</option>
            <option value="NO">Hidden</option>
          </select>
        </div>
      </div>

      {/* Stats */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '0.5rem', flexWrap: 'wrap' }}>
        {(['AIRPORT', 'STATION', 'CITY', 'POI'] as const).map(type => {
          const count = MOCK_LOCATIONS.filter(l => l.type === type).length;
          return (
            <span key={type} style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.35rem',
              fontSize: '0.75rem', fontWeight: 600, padding: '0.2rem 0.6rem',
              borderRadius: 999, backgroundColor: `${TYPE_COLORS[type]}15`, color: TYPE_COLORS[type],
            }}>
              {TYPE_ICONS[type]} {count} {type.charAt(0) + type.slice(1).toLowerCase()}{count > 1 ? 's' : ''}
            </span>
          );
        })}
      </div>

      <div className={paneStyles.tableWrapper}>
        <table className={paneStyles.dataTable}>
          <thead>
            <tr>
              <th>Type</th>
              <th>Code</th>
              <th>Name</th>
              <th>City</th>
              <th>Country</th>
              <th>Coordinates</th>
              <th>Priority</th>
              <th>Searchable</th>
              <th>Aliases</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(loc => (
              <tr key={loc.id} style={{ opacity: loc.isSearchable ? 1 : 0.5 }}>
                <td>
                  <span style={{
                    fontSize: '0.65rem', fontWeight: 700, padding: '0.15rem 0.5rem', borderRadius: 999,
                    backgroundColor: `${TYPE_COLORS[loc.type]}15`, color: TYPE_COLORS[loc.type],
                    display: 'inline-flex', alignItems: 'center', gap: '0.3rem',
                  }}>
                    {TYPE_ICONS[loc.type]} {loc.type}
                  </span>
                </td>
                <td>
                  {loc.iataCode ? (
                    <div>
                      <strong>{loc.iataCode}</strong>
                      {loc.icaoCode && <span style={{ fontSize: '0.75rem', opacity: 0.5, marginLeft: 6, fontFamily: 'monospace' }}>{loc.icaoCode}</span>}
                    </div>
                  ) : (
                    <span style={{ opacity: 0.3 }}>—</span>
                  )}
                </td>
                <td><span style={{ fontWeight: 500 }}>{loc.name}</span></td>
                <td>{loc.city}</td>
                <td>{loc.country}</td>
                <td style={{ fontSize: '0.78rem', fontFamily: 'monospace', opacity: 0.7 }}>
                  {loc.lat.toFixed(4)}, {loc.lon.toFixed(4)}
                </td>
                <td>{loc.searchPriority}</td>
                <td>{loc.isSearchable ? '✅' : '❌'}</td>
                <td style={{ fontSize: '0.75rem', opacity: 0.6 }}>
                  {loc.searchAliases.length > 0 ? loc.searchAliases.join(', ') : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
};
