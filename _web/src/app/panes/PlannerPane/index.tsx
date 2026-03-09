import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import * as s from './PlannerPane.module.scss';
import { fetchTransportModes, TransportModeDto } from '../../api/transportApi';
import {
  FiNavigation, FiMapPin, FiCalendar, FiSearch, FiRepeat,
  FiDollarSign, FiWind, FiChevronDown, FiChevronLeft, FiChevronRight, FiX, FiUsers, FiFilter, FiClock, FiStar, FiZap,
  FiAlertCircle, FiLoader,
} from 'react-icons/fi';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import {
  MdFlight, MdDirectionsBus, MdTrain, MdDirectionsWalk, MdLocalTaxi,
  MdDirectionsSubway, MdSwapVert, MdPlace, MdFlightTakeoff, MdDirectionsCar,
  MdDirectionsBoat, MdPedalBike,
} from 'react-icons/md';
import {
  ReactFlow, Background, Controls, BackgroundVariant, MiniMap,
  Handle, Position,
  type Node, type Edge, type NodeProps,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useThemeContext } from '../../contexts/ThemeContext';
import { searchLocations, LocationResult } from '../../api/locationApi';
import { useJourneyStore } from '../../stores/journeyStore';
import { useSettingsStore } from '../../stores/settingsStore';

/* ═══════════════════════════════════════════════
   Types
   ═══════════════════════════════════════════════ */
interface PlaceSuggestion {
  id: string;
  name: string;
  description: string;
  iataCode: string | null;
  lat: number;
  lon: number;
  type: 'address' | 'city' | 'airport' | 'station' | 'poi';
}

type SegmentMode = 'flight' | 'train' | 'bus' | 'metro' | 'walk' | 'walking' | 'taxi' | 'uber' | string;

interface JourneySegment {
  mode: SegmentMode;
  originCode: string;
  originName: string;
  destinationCode: string;
  destinationName: string;
  departureTime: string;
  arrivalTime: string;
  durationMin: number;
  serviceCode?: string;
  provider?: string;
  costCents?: number;
  currency?: string;
}

interface JourneyResult {
  id: string;
  label: string;
  segments: JourneySegment[];
  totalDurationMin: number;
  totalCostCents: number;
  co2Grams: number;
  transfers: number;
  tags: string[];
}

/* ═══════════════════════════════════════════════
   Mode Metadata
   ═══════════════════════════════════════════════ */
const MODE_META: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  flight:  { icon: <MdFlight size={14} />,          color: '#1E88E5', label: 'Flight' },
  train:   { icon: <MdTrain size={14} />,           color: '#E53935', label: 'Train' },
  bus:     { icon: <MdDirectionsBus size={14} />,    color: '#43A047', label: 'Bus' },
  metro:   { icon: <MdDirectionsSubway size={14} />, color: '#8E24AA', label: 'Metro' },
  walk:    { icon: <MdDirectionsWalk size={14} />,   color: '#78909C', label: 'Walk' },
  taxi:    { icon: <MdLocalTaxi size={14} />,        color: '#FFC107', label: 'Taxi' },
  uber:    { icon: <MdDirectionsCar size={14} />,    color: '#212121', label: 'Uber' },
  ferry:   { icon: <MdDirectionsBoat size={14} />,   color: '#00ACC1', label: 'Ferry' },
  bike:    { icon: <MdPedalBike size={14} />,        color: '#FF8F00', label: 'Bike' },
};
const FALLBACK_MODE = { icon: <FiMapPin size={14} />, color: '#6b7280', label: '?' };
const MODE_ALIAS: Record<string, string> = { walking: 'walk', subway: 'metro' };
const modeMeta = (m: string) => { const low = m?.toLowerCase(); const key = MODE_ALIAS[low] || low; return MODE_META[key] || MODE_META[low] || FALLBACK_MODE; };
// Filter chip display list — default, overridden by API on mount
const DEFAULT_FILTER_MODES: SegmentMode[] = ['flight', 'train', 'bus', 'metro', 'walk', 'taxi', 'uber', 'ferry', 'bike'];
// Normalize backend mode names to filter keys (backend sends WALKING, SUBWAY etc.)
const normalizeMode = (m: string) => { const low = m?.toLowerCase(); return MODE_ALIAS[low] || low; };

/* ═══════════════════════════════════════════════
   Journey Graph — builds React Flow graph from results
   ═══════════════════════════════════════════════ */
type JGraphNodeData = {
  label: string;
  code: string;
  mode?: SegmentMode;
  provider?: string;
  duration?: string;
  cost?: string;
  isOrigin?: boolean;
  isDestination?: boolean;
  locationType?: string;  // Airport, Station, City, Place
};

const JGraphNode: React.FC<NodeProps<Node<JGraphNodeData>>> = ({ data }) => {
  const isTerminal = data.isOrigin || data.isDestination;
  const color = data.mode ? modeMeta(data.mode).color : '#6b7280';
  return (
    <div className={s.jgNode} style={{ borderColor: color }}>
      {!data.isOrigin && <Handle type="target" position={Position.Left} className={s.jgHandle} />}
      {!data.isDestination && <Handle type="source" position={Position.Right} className={s.jgHandle} />}
      {data.locationType && (
        <span className={`${s.jgTypeBadge} ${s['jgType' + data.locationType]}`}>
          {data.locationType === 'Airport' && <MdFlightTakeoff size={11} />}
          {data.locationType === 'Station' && <MdTrain size={11} />}
          {data.locationType === 'Place' && <MdPlace size={11} />}
        </span>
      )}
      {data.mode && (
        <span className={s.jgModeIcon} style={{ color }}>{modeMeta(data.mode).icon}</span>
      )}
      <div className={s.jgInfo}>
        <strong>{data.code}</strong>
        <span className={s.jgLabel}>{data.label}</span>
        {data.duration && <span className={s.jgDuration}>{data.duration}</span>}
        {data.provider && <span className={s.jgProvider}>{data.provider}</span>}
      </div>
    </div>
  );
};
const jgNodeTypes = { journeyNode: JGraphNode };

function buildSingleJourneyGraph(journey: JourneyResult): { nodes: Node<JGraphNodeData>[]; edges: Edge[] } {
  const nodes: Node<JGraphNodeData>[] = [];
  const edges: Edge[] = [];
  const xSpacing = 480;

  // Helper: derive location type from surrounding segment modes
  const inferType = (segIdx: number, isOriginOfSeg: boolean): string => {
    const seg = journey.segments[segIdx];
    const prevSeg = segIdx > 0 ? journey.segments[segIdx - 1] : null;
    const mode = (isOriginOfSeg ? (prevSeg?.mode || seg.mode) : seg.mode)?.toLowerCase();
    const nextMode = (isOriginOfSeg ? seg.mode : (segIdx < journey.segments.length - 1 ? journey.segments[segIdx + 1]?.mode : null))?.toLowerCase();
    if (mode === 'flight' || nextMode === 'flight') return 'Airport';
    if (mode === 'train' || nextMode === 'train') return 'Station';
    if (mode === 'metro' || nextMode === 'metro') return 'Station';
    return 'Place';
  };

  journey.segments.forEach((seg, si) => {
    // Origin node
    const fromId = `jn_${si}_from`;
    if (!nodes.find(n => n.id === fromId)) {
      nodes.push({
        id: fromId, type: 'journeyNode',
        position: { x: si * xSpacing, y: 40 },
        data: {
          label: seg.originName, code: seg.originCode,
          isOrigin: si === 0,
          locationType: inferType(si, true),
        },
      });
    }
    // Destination node
    const toId = si < journey.segments.length - 1 ? `jn_${si + 1}_from` : `jn_${si}_to`;
    if (!nodes.find(n => n.id === toId)) {
      nodes.push({
        id: toId, type: 'journeyNode',
        position: { x: (si + 1) * xSpacing, y: 40 },
        data: {
          label: seg.destinationName, code: seg.destinationCode,
          isDestination: si === journey.segments.length - 1,
          locationType: inferType(si, false),
        },
      });
    }
    // Edge with mode info
    edges.push({
      id: `je_${si}`, source: fromId, target: toId,
      label: `${modeMeta(seg.mode).label} · ${formatDuration(seg.durationMin)}${seg.provider ? ` · ${seg.provider}` : ''}`,
      type: 'smoothstep',
      animated: true,
      style: { stroke: modeMeta(seg.mode).color, strokeWidth: 2.5 },
      labelStyle: { fontSize: 10, fontWeight: 600, fill: modeMeta(seg.mode).color },
      labelBgStyle: { fillOpacity: 0.95, rx: 4, ry: 4 },
      labelBgClassName: 'jg-edge-label-bg',
      labelBgPadding: [8, 5] as [number, number],
    });
  });

  return { nodes, edges };
}

const JourneyGraphView: React.FC<{ journey: JourneyResult }> = ({ journey }) => {
  const { theme } = useThemeContext();
  const { nodes, edges } = useMemo(() => buildSingleJourneyGraph(journey), [journey]);
  return (
    <div className={s.journeyGraphWrap}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={jgNodeTypes}
        fitView
        proOptions={{ hideAttribution: true }}
        colorMode={theme.variant}
        nodesDraggable={false}
        nodesConnectable={false}
        elementsSelectable={false}
      >
        <Background variant={BackgroundVariant.Dots} gap={20} size={1}
          color={theme.variant === 'dark' ? 'rgba(148,163,184,0.1)' : 'rgba(200,16,46,0.08)'} />
      </ReactFlow>
    </div>
  );
};

/* ═══════════════════════════════════════════════
   Mock Autocomplete Data (simulating Google Places API)
   ═══════════════════════════════════════════════ */
const MOCK_PLACES: PlaceSuggestion[] = [
  { id: 'p1', name: 'Beytepe, Ankara', description: 'Çankaya, Ankara, Türkiye', lat: 39.8686, lon: 32.7312, type: 'address' },
  { id: 'p2', name: 'Beytepe Parkı', description: 'Beytepe, Çankaya, Ankara', lat: 39.872, lon: 32.738, type: 'poi' },
  { id: 'p3', name: 'Hacettepe Üniversitesi Beytepe', description: 'Beytepe, Çankaya, Ankara', lat: 39.867, lon: 32.734, type: 'poi' },
  { id: 'p4', name: 'Hückelhoven', description: 'Nordrhein-Westfalen, Deutschland', lat: 51.0547, lon: 6.2264, type: 'city' },
  { id: 'p5', name: 'Hückelhoven Rathaus', description: 'Rathausplatz 1, Hückelhoven', lat: 51.055, lon: 6.227, type: 'poi' },
  { id: 'p6', name: 'Istanbul Airport (IST)', description: 'Arnavutköy, Istanbul, Türkiye', lat: 41.2611, lon: 28.7419, type: 'airport' },
  { id: 'p7', name: 'Sabiha Gökçen Airport (SAW)', description: 'Pendik, Istanbul, Türkiye', lat: 40.8986, lon: 29.3092, type: 'airport' },
  { id: 'p8', name: 'Kadıköy', description: 'Istanbul, Türkiye', lat: 40.9919, lon: 29.0234, type: 'city' },
  { id: 'p9', name: 'Frankfurt am Main', description: 'Hessen, Deutschland', lat: 50.1109, lon: 8.6821, type: 'city' },
  { id: 'p10', name: 'Taksim', description: 'Beyoğlu, Istanbul, Türkiye', lat: 41.0370, lon: 28.9850, type: 'city' },
  { id: 'p11', name: 'München Hauptbahnhof', description: 'München, Bayern, Deutschland', lat: 48.1403, lon: 11.56, type: 'station' },
  { id: 'p12', name: 'Ankara Esenboğa Airport (ESB)', description: 'Akyurt, Ankara, Türkiye', lat: 40.1281, lon: 32.9951, type: 'airport' },
  { id: 'p13', name: 'Düsseldorf Airport (DUS)', description: 'Düsseldorf, NRW, Deutschland', lat: 51.2895, lon: 6.7668, type: 'airport' },
  { id: 'p14', name: 'Erkelenz Bahnhof', description: 'Erkelenz, NRW, Deutschland', lat: 51.0799, lon: 6.3134, type: 'station' },
  { id: 'p15', name: 'London Heathrow (LHR)', description: 'London, United Kingdom', lat: 51.47, lon: -0.4543, type: 'airport' },
];

/* Demo Route Results: Beytepe→Hückelhoven */
const DEMO_RESULTS: JourneyResult[] = [
  {
    id: 'r1', label: 'En Hızlı', transfers: 2,
    tags: ['fastest', 'recommended'],
    segments: [
      { mode: 'taxi', originCode: 'BYT', originName: 'Beytepe, Ankara', destinationCode: 'ESB', destinationName: 'Esenboğa Havalimanı', departureTime: '04:30', arrivalTime: '05:05', durationMin: 35, provider: 'Taksi', costCents: 4500 },
      { mode: 'flight', originCode: 'ESB', originName: 'Esenboğa Havalimanı', destinationCode: 'DUS', destinationName: 'Düsseldorf Airport', departureTime: '06:30', arrivalTime: '09:00', durationMin: 210, serviceCode: 'XQ 501', provider: 'SunExpress', costCents: 14900 },
      { mode: 'train', originCode: 'DUS', originName: 'Düsseldorf Flughafen', destinationCode: 'ERK', destinationName: 'Erkelenz Bahnhof', departureTime: '09:45', arrivalTime: '10:25', durationMin: 40, serviceCode: 'RE 4', provider: 'Deutsche Bahn', costCents: 1250 },
      { mode: 'taxi', originCode: 'ERK', originName: 'Erkelenz Bahnhof', destinationCode: 'HCK', destinationName: 'Hückelhoven', departureTime: '10:30', arrivalTime: '10:40', durationMin: 10, provider: 'Taksi', costCents: 1500 },
    ],
    totalDurationMin: 370, totalCostCents: 22150, co2Grams: 86000,
  },
  {
    id: 'r2', label: 'En Ucuz', transfers: 3,
    tags: ['cheapest'],
    segments: [
      { mode: 'bus', originCode: 'BYT', originName: 'Beytepe', destinationCode: 'AŞTİ', destinationName: 'Ankara AŞTİ', departureTime: '03:00', arrivalTime: '03:40', durationMin: 40, serviceCode: 'EGO 1', costCents: 150 },
      { mode: 'taxi', originCode: 'AŞTİ', originName: 'Ankara AŞTİ', destinationCode: 'ESB', destinationName: 'Esenboğa Havalimanı', departureTime: '03:50', arrivalTime: '04:25', durationMin: 35, provider: 'Taksi', costCents: 4000 },
      { mode: 'flight', originCode: 'ESB', originName: 'Esenboğa', destinationCode: 'CGN', destinationName: 'Köln/Bonn Airport', departureTime: '06:00', arrivalTime: '08:25', durationMin: 205, serviceCode: 'PC 1011', provider: 'Pegasus', costCents: 7900 },
      { mode: 'train', originCode: 'CGN', originName: 'Köln/Bonn Flughafen', destinationCode: 'ERK', destinationName: 'Erkelenz', departureTime: '09:15', arrivalTime: '10:10', durationMin: 55, serviceCode: 'RE 9 + RE 4', provider: 'Deutsche Bahn', costCents: 1680 },
      { mode: 'walk', originCode: 'ERK', originName: 'Erkelenz Bf', destinationCode: 'HCK', destinationName: 'Hückelhoven Zentrum', departureTime: '10:15', arrivalTime: '10:55', durationMin: 40, costCents: 0 },
    ],
    totalDurationMin: 475, totalCostCents: 13730, co2Grams: 72000,
  },
  {
    id: 'r3', label: 'Istanbul Aktarmalı', transfers: 4,
    tags: ['most-options'],
    segments: [
      { mode: 'taxi', originCode: 'BYT', originName: 'Beytepe, Ankara', destinationCode: 'ESB', destinationName: 'Esenboğa Havalimanı', departureTime: '05:00', arrivalTime: '05:35', durationMin: 35, provider: 'Taksi', costCents: 4500 },
      { mode: 'flight', originCode: 'ESB', originName: 'Esenboğa', destinationCode: 'IST', destinationName: 'Istanbul Airport', departureTime: '07:00', arrivalTime: '08:15', durationMin: 75, serviceCode: 'TK 2124', provider: 'Turkish Airlines', costCents: 5500 },
      { mode: 'flight', originCode: 'IST', originName: 'Istanbul Airport', destinationCode: 'DUS', destinationName: 'Düsseldorf', departureTime: '10:00', arrivalTime: '12:20', durationMin: 200, serviceCode: 'TK 1523', provider: 'Turkish Airlines', costCents: 18500 },
      { mode: 'train', originCode: 'DUS', originName: 'Düsseldorf Flughafen', destinationCode: 'ERK', destinationName: 'Erkelenz', departureTime: '13:00', arrivalTime: '13:40', durationMin: 40, serviceCode: 'RE 4', provider: 'DB', costCents: 1250 },
      { mode: 'taxi', originCode: 'ERK', originName: 'Erkelenz', destinationCode: 'HCK', destinationName: 'Hückelhoven', departureTime: '13:45', arrivalTime: '13:55', durationMin: 10, provider: 'Taksi', costCents: 1500 },
    ],
    totalDurationMin: 535, totalCostCents: 31250, co2Grams: 124000,
  },
  {
    id: 'r4', label: 'YHT + Uçuş', transfers: 4,
    tags: ['greenest'],
    segments: [
      { mode: 'metro', originCode: 'BYT', originName: 'Beytepe', destinationCode: 'KZL', destinationName: 'Kızılay', departureTime: '04:00', arrivalTime: '04:35', durationMin: 35, serviceCode: 'M3', costCents: 150 },
      { mode: 'walk', originCode: 'KZL', originName: 'Kızılay Metro', destinationCode: 'ANK', destinationName: 'Ankara YHT Garı', departureTime: '04:35', arrivalTime: '04:45', durationMin: 10, costCents: 0 },
      { mode: 'train', originCode: 'ANK', originName: 'Ankara YHT', destinationCode: 'IST-H', destinationName: 'Istanbul Halkalı', departureTime: '05:30', arrivalTime: '10:00', durationMin: 270, serviceCode: 'YHT 1001', provider: 'TCDD', costCents: 7500 },
      { mode: 'flight', originCode: 'IST', originName: 'Istanbul Airport', destinationCode: 'DUS', destinationName: 'Düsseldorf', departureTime: '13:00', arrivalTime: '15:20', durationMin: 200, serviceCode: 'TK 1525', provider: 'Turkish Airlines', costCents: 15000 },
      { mode: 'train', originCode: 'DUS', originName: 'Düsseldorf', destinationCode: 'ERK', destinationName: 'Erkelenz', departureTime: '16:00', arrivalTime: '16:40', durationMin: 40, serviceCode: 'RE 4', provider: 'DB', costCents: 1250 },
      { mode: 'taxi', originCode: 'ERK', originName: 'Erkelenz', destinationCode: 'HCK', destinationName: 'Hückelhoven', departureTime: '16:45', arrivalTime: '16:55', durationMin: 10, provider: 'Taksi', costCents: 1500 },
    ],
    totalDurationMin: 775, totalCostCents: 25400, co2Grams: 58000,
  },
];

/* ═══════════════════════════════════════════════
   Helpers
   ═══════════════════════════════════════════════ */
const formatDuration = (min: number) => {
  const h = Math.floor(min / 60);
  const m = min % 60;
  if (h > 0 && m > 0) return `${h}h ${m} min`;
  if (h > 0) return `${h}h`;
  return `${m} min`;
};
const CURRENCY_MAP: Record<string, string> = {
  EUR: '€', TRY: '₺', GBP: '£', USD: '$',
};
const formatPrice = (cents: number, currency?: string | null) => {
  const sym = CURRENCY_MAP[currency || 'EUR'] || currency || '€';
  return cents >= 10000 ? `${sym}${(cents / 100).toFixed(0)}` : `${sym}${(cents / 100).toFixed(2)}`;
};
const formatCo2 = (g: number) => `${(g / 1000).toFixed(0)} kg`;

const PLACE_TYPE_ICON: Record<string, React.ReactNode> = {
  airport: <MdFlight size={14} />,
  station: <MdTrain size={14} />,
  city: <FiMapPin size={14} />,
  address: <FiNavigation size={14} />,
  poi: <FiMapPin size={14} />,
};
const PLACE_TYPE_COLOR: Record<string, string> = {
  airport: '#3b82f6', station: '#f97316', city: '#8b5cf6', address: '#22c55e', poi: '#f97316',
};

/* ═══════════════════════════════════════════════
   Autocomplete Hook
   ═══════════════════════════════════════════════ */
function useAutocomplete() {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<PlaceSuggestion[]>([]);
  const [selected, setSelected] = useState<PlaceSuggestion | null>(null);
  const [open, setOpen] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    if (!query || query.length < 2 || selected) { setSuggestions([]); setOpen(false); return; }
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      try {
        const results = await searchLocations({ name: query }, 0, 8);
        const mapped: PlaceSuggestion[] = results.map(loc => {
          const typeMap: Record<string, PlaceSuggestion['type']> = {
            AIRPORT: 'airport', TRAIN_STATION: 'station', BUS_TERMINAL: 'station',
            FERRY_PORT: 'station', CITY_CENTER: 'city', CITY: 'city',
            STATION: 'station', POI: 'city',
          };
          // loc.type can be string "AIRPORT" or object {value:"AIRPORT", desc:"..."}
          const rawType = typeof loc.type === 'object' && loc.type !== null ? (loc.type as any).value : loc.type;
          return {
            id: loc.id,
            name: loc.iataCode ? `${loc.name} (${loc.iataCode})` : loc.name,
            description: [loc.city, loc.countryIsoCode].filter(Boolean).join(', '),
            iataCode: loc.iataCode,
            lat: loc.lat, lon: loc.lon,
            type: typeMap[rawType] || 'airport',
          };
        });
        setSuggestions(mapped);
        setOpen(mapped.length > 0);
      } catch {
        // Fallback to mock data on API error
        const q = query.toLowerCase();
        const matched = MOCK_PLACES.filter(p =>
          p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q)
        ).slice(0, 6).map(p => ({ ...p, iataCode: null }));
        setSuggestions(matched);
        setOpen(matched.length > 0);
      }
    }, 250);
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [query, selected]);

  const select = (place: PlaceSuggestion) => {
    setSelected(place);
    setQuery(place.name);
    setOpen(false);
  };

  const clear = () => { setQuery(''); setSelected(null); setSuggestions([]); setOpen(false); };

  return { query, setQuery: (v: string) => { setQuery(v); setSelected(null); }, suggestions, selected, open, setOpen, select, clear };
}

/* ═══════════════════════════════════════════════
   Component
   ═══════════════════════════════════════════════ */
export const PlannerPane: React.FC = () => {
  const origin = useAutocomplete();
  const dest = useAutocomplete();
  const [date, setDate] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  });
  const formattedDate = new Date(date + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'short', day: 'numeric', month: 'long', year: 'numeric' });
  const [adults, setAdults] = useState(1);
  const [children, setChildren] = useState(0);
  const [infants, setInfants] = useState(0);
  const [cabinClass, setCabinClass] = useState<'economy' | 'business' | 'first'>('economy');
  const totalPax = adults + children + infants;
  const cabinLabels = { economy: 'Economy', business: 'Business', first: 'First' };
  const paxSummary = `${totalPax} Pax · ${cabinLabels[cabinClass]}`;
  const { results: storeResults, loading, error, search: storeSearch, selectedJourney, clearError } = useJourneyStore();
  const currency = useSettingsStore(s => s.currency);
  const [searched, setSearched] = useState(false);
  const [sortBy, setSortBy] = useState<'fastest' | 'cheapest' | 'greenest'>('fastest');
  const [activeFilters, setActiveFilters] = useState<Set<string>>(new Set(['all']));
  const [expandedCard, setExpandedCard] = useState<string | null>(null);
  const [filterModes, setFilterModes] = useState<SegmentMode[]>(DEFAULT_FILTER_MODES);

  const originRef = useRef<HTMLDivElement>(null);
  const destRef = useRef<HTMLDivElement>(null);

  /* Load transport modes from API — drives filter chips + colors */
  useEffect(() => {
    fetchTransportModes().then((modes: TransportModeDto[]) => {
      // Build filter list from DB modes (normalized, deduplicated)
      const seen = new Set<string>();
      const dbFilters: SegmentMode[] = [];
      for (const m of modes) {
        const key = normalizeMode(m.code);
        if (!seen.has(key)) {
          seen.add(key);
          dbFilters.push(key as SegmentMode);
          // Merge DB color into MODE_META if entry exists
          if (MODE_META[key] && m.colorHex) {
            MODE_META[key] = { ...MODE_META[key], color: m.colorHex };
          }
        }
      }
      if (dbFilters.length > 0) setFilterModes(dbFilters);
    }).catch(() => { /* keep defaults */ });
  }, []);

  /* Close dropdowns on outside click */
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (originRef.current && !originRef.current.contains(e.target as Node)) origin.setOpen(false);
      if (destRef.current && !destRef.current.contains(e.target as Node)) dest.setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSearch = async () => {
    setSearched(true);
    clearError();

    // UUID format check — mock place IDs like "p9" are not valid UUIDs
    const isUuid = (s?: string) => !!s && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(s);

    // Determine origin/destination identifiers
    const originId = origin.selected?.id;
    const originIata = origin.selected?.iataCode;
    const destId = dest.selected?.id;
    const destIata = dest.selected?.iataCode;

    const sortMap: Record<string, string> = {
      fastest: 'FASTEST', cheapest: 'CHEAPEST', greenest: 'GREENEST',
    };

    await storeSearch({
      originLocationId: (!originIata && isUuid(originId)) ? originId : undefined,
      originIataCode: originIata || undefined,
      originQuery: origin.query || undefined,
      originLat: origin.selected?.lat,
      originLon: origin.selected?.lon,
      originType: origin.selected?.type === 'airport' ? 'airport'
        : origin.selected?.type === 'station' ? 'station' : 'place',
      destinationLocationId: (!destIata && isUuid(destId)) ? destId : undefined,
      destinationIataCode: destIata || undefined,
      destinationQuery: dest.query || undefined,
      destinationLat: dest.selected?.lat,
      destinationLon: dest.selected?.lon,
      destinationType: dest.selected?.type === 'airport' ? 'airport'
        : dest.selected?.type === 'station' ? 'station' : 'place',
      departureDate: date,
      sortBy: (sortMap[sortBy] || 'FASTEST') as any,
      targetCurrency: currency,
    });
  };

  const handleSwap = () => {
    const oq = origin.query, os = origin.selected;
    const dq = dest.query, ds = dest.selected;
    origin.clear(); dest.clear();
    setTimeout(() => {
      if (ds) { origin.setQuery(ds.name); origin.select(ds); }
      else origin.setQuery(dq);
      if (os) { dest.setQuery(os.name); dest.select(os); }
      else dest.setQuery(oq);
    }, 0);
  };

  const toggleFilter = (filter: string) => {
    const next = new Set(activeFilters);
    if (filter === 'all') { setActiveFilters(new Set(['all'])); return; }
    next.delete('all');
    if (next.has(filter)) next.delete(filter); else next.add(filter);
    if (next.size === 0) next.add('all');
    setActiveFilters(next);
  };

  // Use store results, apply local sort + filter
  const sortedResults = useMemo(() => {
    const arr = [...storeResults];
    if (sortBy === 'cheapest') arr.sort((a, b) => a.totalCostCents - b.totalCostCents);
    else if (sortBy === 'greenest') arr.sort((a, b) => a.co2Grams - b.co2Grams);
    else arr.sort((a, b) => a.totalDurationMin - b.totalDurationMin);
    return arr;
  }, [storeResults, sortBy]);

  const filteredResults = sortedResults.filter(r => {
    if (activeFilters.has('all')) return true;
    return r.segments.some(seg => activeFilters.has(normalizeMode(seg.mode)));
  });

  /* ═══════════ Render ═══════════ */
  return (
    <>
      {/* ── Search Panel ── */}
      <div className={s.searchPanel}>
        <div className={s.searchRow}>
          {/* Origin */}
          <div className={s.inputGroup} ref={originRef} style={origin.open ? { zIndex: 100 } : undefined}>
            <FiNavigation className={s.inputIcon} />
            <input className={s.searchInput} placeholder="Origin (e.g. Istanbul, ESB, Frankfurt)"
              value={origin.query} onChange={e => origin.setQuery(e.target.value)}
              onFocus={() => origin.suggestions.length > 0 && origin.setOpen(true)} />
            {origin.selected && (
              <button className={s.clearBtn} onClick={origin.clear}><FiX size={12} /></button>
            )}
            {origin.open && (
              <div className={s.autocompleteDropdown}>
                {origin.suggestions.map(p => (
                  <button key={p.id} className={s.autocompleteItem} onClick={() => origin.select(p)}>
                    <span className={s.placeIcon} style={{ color: PLACE_TYPE_COLOR[p.type] }}>
                      {PLACE_TYPE_ICON[p.type]}
                    </span>
                    <div className={s.placeText}>
                      <span className={s.placeName}>{p.name}</span>
                      <span className={s.placeDesc}>{p.description}</span>
                    </div>
                    <span className={s.placeType}>{p.type}</span>
                  </button>
                ))}
                <div className={s.poweredBy}>Location database</div>
              </div>
            )}
          </div>

          {/* Swap */}
          <button className={s.swapButton} onClick={handleSwap} title="Swap">
            <MdSwapVert size={16} />
          </button>

          {/* Destination */}
          <div className={s.inputGroup} ref={destRef} style={dest.open ? { zIndex: 100 } : undefined}>
            <FiMapPin className={s.inputIcon} />
            <input className={s.searchInput} placeholder="Destination (e.g. Berlin, LHR, Ankara)"
              value={dest.query} onChange={e => dest.setQuery(e.target.value)}
              onFocus={() => dest.suggestions.length > 0 && dest.setOpen(true)} />
            {dest.selected && (
              <button className={s.clearBtn} onClick={dest.clear}><FiX size={12} /></button>
            )}
            {dest.open && (
              <div className={s.autocompleteDropdown}>
                {dest.suggestions.map(p => (
                  <button key={p.id} className={s.autocompleteItem} onClick={() => dest.select(p)}>
                    <span className={s.placeIcon} style={{ color: PLACE_TYPE_COLOR[p.type] }}>
                      {PLACE_TYPE_ICON[p.type]}
                    </span>
                    <div className={s.placeText}>
                      <span className={s.placeName}>{p.name}</span>
                      <span className={s.placeDesc}>{p.description}</span>
                    </div>
                    <span className={s.placeType}>{p.type}</span>
                  </button>
                ))}
                <div className={s.poweredBy}>Location database</div>
              </div>
            )}
          </div>
        </div>

        {/* Second row: date, passengers, search */}
        <div className={s.optionsRow}>
          {/* Custom Date Picker */}
          {(() => {
            const [calOpen, setCalOpen] = React.useState(false);
            const calRef = React.useRef<HTMLDivElement>(null);
            const sel = new Date(date + 'T00:00:00');
            const [viewYear, setViewYear] = React.useState(sel.getFullYear());
            const [viewMonth, setViewMonth] = React.useState(sel.getMonth());

            React.useEffect(() => {
              const handler = (e: MouseEvent) => {
                if (calRef.current && !calRef.current.contains(e.target as Node)) setCalOpen(false);
              };
              document.addEventListener('mousedown', handler);
              return () => document.removeEventListener('mousedown', handler);
            }, []);

            const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
            const firstDay = new Date(viewYear, viewMonth, 1).getDay();
            const startOffset = firstDay === 0 ? 6 : firstDay - 1; // Monday start
            const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];
            const DAYS = ['Mo','Tu','We','Th','Fr','Sa','Su'];
            const today = new Date(); today.setHours(0,0,0,0);

            const prevMonth = () => { if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); } else setViewMonth(m => m - 1); };
            const nextMonth = () => { if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); } else setViewMonth(m => m + 1); };

            const pickDay = (day: number) => {
              const m = String(viewMonth + 1).padStart(2, '0');
              const d = String(day).padStart(2, '0');
              setDate(`${viewYear}-${m}-${d}`);
              setCalOpen(false);
            };

            return (
              <div className={s.datePickerWrap} ref={calRef} style={calOpen ? { zIndex: 100 } : undefined}>
                <button className={s.dateButton} onClick={() => setCalOpen(!calOpen)}>
                  <FiCalendar size={14} />
                  <span>{formattedDate}</span>
                  <FiChevronDown size={11} style={{ opacity: 0.35 }} />
                </button>
                {calOpen && (
                  <div className={s.calendarPopup}>
                    <div className={s.calendarHeader}>
                      <button className={s.calNavBtn} onClick={prevMonth}>‹</button>
                      <span className={s.calMonthYear}>{MONTHS[viewMonth]} {viewYear}</span>
                      <button className={s.calNavBtn} onClick={nextMonth}>›</button>
                    </div>
                    <div className={s.calendarGrid}>
                      {DAYS.map(d => <div key={d} className={s.calDayLabel}>{d}</div>)}
                      {Array.from({ length: startOffset }).map((_, i) => <div key={`e${i}`} />)}
                      {Array.from({ length: daysInMonth }).map((_, i) => {
                        const day = i + 1;
                        const thisDate = new Date(viewYear, viewMonth, day);
                        const isSelected = thisDate.getTime() === sel.getTime();
                        const isToday = thisDate.getTime() === today.getTime();
                        const isPast = thisDate < today;
                        return (
                          <button key={day}
                            className={`${s.calDay} ${isSelected ? s.calSelected : ''} ${isToday ? s.calToday : ''} ${isPast ? s.calPast : ''}`}
                            onClick={() => pickDay(day)}>
                            {day}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            );
          })()}
          {/* Custom Passenger/Class Picker */}
          {(() => {
            const [paxOpen, setPaxOpen] = React.useState(false);
            const paxRef = React.useRef<HTMLDivElement>(null);

            React.useEffect(() => {
              const handler = (e: MouseEvent) => {
                if (paxRef.current && !paxRef.current.contains(e.target as Node)) setPaxOpen(false);
              };
              document.addEventListener('mousedown', handler);
              return () => document.removeEventListener('mousedown', handler);
            }, []);

            const Counter = ({ label, desc, value, setValue, min = 0, max = 9 }: { label: string; desc: string; value: number; setValue: (v: number) => void; min?: number; max?: number }) => (
              <div className={s.paxRow}>
                <div className={s.paxRowInfo}>
                  <span className={s.paxLabel}>{label}</span>
                  <span className={s.paxDesc}>{desc}</span>
                </div>
                <div className={s.paxControls}>
                  <button className={s.paxBtn} disabled={value <= min} onClick={() => setValue(Math.max(min, value - 1))}>−</button>
                  <span className={s.paxCount}>{value}</span>
                  <button className={s.paxBtn} disabled={value >= max} onClick={() => setValue(Math.min(max, value + 1))}>+</button>
                </div>
              </div>
            );

            return (
              <div className={s.datePickerWrap} ref={paxRef} style={paxOpen ? { zIndex: 100 } : undefined}>
                <button className={s.dateButton} onClick={() => setPaxOpen(!paxOpen)}>
                  <FiUsers size={14} />
                  <span>{paxSummary}</span>
                  <FiChevronDown size={11} style={{ opacity: 0.35 }} />
                </button>
                {paxOpen && (
                  <div className={s.paxPopup}>
                    <Counter label="Yetişkin" desc="12+ yaş" value={adults} setValue={setAdults} min={1} />
                    <Counter label="Çocuk" desc="2–11 yaş" value={children} setValue={setChildren} />
                    <Counter label="Bebek" desc="0–2 yaş" value={infants} setValue={setInfants} />
                    <div className={s.paxSep} />
                    <div className={s.cabinSection}>
                      <span className={s.cabinLabel}>Class</span>
                      <div className={s.cabinOptions}>
                        {(['economy', 'business', 'first'] as const).map(cls => (
                          <button key={cls}
                            className={`${s.cabinBtn} ${cabinClass === cls ? s.cabinActive : ''}`}
                            onClick={() => setCabinClass(cls)}>
                            {cabinLabels[cls]}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })()}
          <button className={s.searchButton} onClick={handleSearch} disabled={loading}>
            {loading ? <><FiLoader size={15} className={s.spinning} /> Searching...</> : <><FiSearch size={15} /> Search</>}
          </button>
        </div>
      </div>

      {/* ── Filter Box ── */}
      {searched && (
        <div className={s.filterBox}>
          <div className={s.filterHeader}>
            <FiFilter size={13} />
            <span>Filters</span>
            <span className={s.resultCount}>{filteredResults.length} routes found</span>
          </div>
          <div className={s.filterContent}>
            <div className={s.filterChips}>
              <button className={`${s.filterChip} ${activeFilters.has('all') ? s.active : ''}`}
                onClick={() => toggleFilter('all')}>All</button>
              {filterModes.map(mode => (
                <button key={mode}
                  className={`${s.filterChip} ${activeFilters.has(mode) ? s.active : ''}`}
                  onClick={() => toggleFilter(mode)}>
                  <span style={{ color: modeMeta(mode).color, display: 'flex' }}>{modeMeta(mode).icon}</span>
                  {modeMeta(mode).label}
                </button>
              ))}
            </div>
            <div className={s.sortGroup}>
              <span className={s.sortLabel}>Sort:</span>
              {([
                { key: 'fastest', icon: <FiZap size={12} />, label: 'Fastest' },
                { key: 'cheapest', icon: <FiDollarSign size={12} />, label: 'Cheapest' },
                { key: 'greenest', icon: <FiWind size={12} />, label: 'Greenest' },
              ] as const).map(opt => (
                <button key={opt.key}
                  className={`${s.sortChip} ${sortBy === opt.key ? s.active : ''}`}
                  onClick={() => { setSortBy(opt.key); handleSearch(); }}>
                  {opt.icon} {opt.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ── Date Tab Bar ── */}
      {searched && !loading && filteredResults.length > 0 && (() => {
        const DAY_MS = 86400000;
        const sel = new Date(date + 'T00:00:00');
        const offsets = [-2, -1, 0, 1, 2];
        const tabs = offsets.map(o => {
          const d = new Date(sel.getTime() + o * DAY_MS);
          return {
            iso: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
            dayName: d.toLocaleDateString('en-US', { weekday: 'short' }),
            dayNum: d.getDate(),
            month: d.toLocaleDateString('en-US', { month: 'short' }),
            isSelected: o === 0,
          };
        });
        const shift = (dir: number) => {
          const next = new Date(sel.getTime() + dir * DAY_MS);
          const iso = `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}-${String(next.getDate()).padStart(2, '0')}`;
          setDate(iso);
          setTimeout(() => handleSearch(), 50);
        };
        const pick = (iso: string) => {
          setDate(iso);
          setTimeout(() => handleSearch(), 50);
        };
        return (
          <div className={s.dateTabBar}>
            <button className={s.dateTabArrow} onClick={() => shift(-1)}>
              <FiChevronLeft size={18} />
            </button>
            {tabs.map(t => (
              <button
                key={t.iso}
                className={`${s.dateTab} ${t.isSelected ? s.dateTabActive : ''}`}
                onClick={() => !t.isSelected && pick(t.iso)}
              >
                <span className={s.dateTabDay}>{t.dayName}</span>
                <span className={s.dateTabNum}>{t.dayNum} {t.month}</span>
              </button>
            ))}
            <button className={s.dateTabArrow} onClick={() => shift(1)}>
              <FiChevronRight size={18} />
            </button>
          </div>
        );
      })()}

      {/* ── Loading ── */}
      {searched && loading && (
        <div style={{ textAlign: 'center', padding: '3rem', opacity: 0.5 }}>Loading...</div>
      )}

      {/* ── Results ── */}
      {searched && !loading && filteredResults.length > 0 && (
        <div className={s.resultsGrid}>
          {filteredResults.map((journey, idx) => (
            <div key={journey.id} className={`${s.resultCard} ${journey.tags.includes('recommended') ? s.recommended : ''}`}>

              {/* Clickable header area — toggles expand/collapse */}
              <div className={s.cardClickable} onClick={() => setExpandedCard(expandedCard === journey.id ? null : journey.id)}>
                {/* Tags */}
                {journey.tags.length > 0 && (
                  <div className={s.cardTags}>
                    {journey.tags.includes('recommended') && <span className={s.tagRecommended}><FiStar size={11} /> Recommended</span>}
                    {journey.tags.includes('fastest') && <span className={s.tagFastest}><FiZap size={11} /> Fastest</span>}
                    {journey.tags.includes('cheapest') && <span className={s.tagCheapest}><FiDollarSign size={11} /> Cheapest</span>}
                    {journey.tags.includes('greenest') && <span className={s.tagGreenest}><FiWind size={11} /> Greenest</span>}
                  </div>
                )}

                {/* Card Header */}
                <div className={s.cardTop}>
                  <div className={s.cardSummary}>
                    <h3>{journey.segments[0].originName} → {journey.segments[journey.segments.length - 1].destinationName}</h3>
                    <div className={s.cardMeta}>
                      <span><FiClock size={12} /> {formatDuration(journey.totalDurationMin)}</span>
                      <span>{journey.transfers === 0 ? 'Direct' : `${journey.transfers} transfers`}</span>
                      <span>
                        {journey.departureCode && <><MdFlight size={12} style={{marginRight: 2}} />{journey.departureCode} </>}
                        {journey.departureTime || journey.segments[0]?.departureTime || '–'}
                        {' → '}
                        {journey.arrivalCode && <>{journey.arrivalCode} </>}
                        {journey.arrivalTime || journey.segments[journey.segments.length - 1]?.arrivalTime || '–'}
                      </span>
                    </div>
                  </div>
                  <div className={s.cardPricing}>
                    <div className={s.price}>{formatPrice(journey.totalCostCents, journey.currency)}</div>
                    <div className={s.co2Badge}><FiWind size={10} /> {formatCo2(journey.co2Grams)} CO₂</div>
                  </div>
                </div>

                {/* Timeline */}
                <div className={s.segmentTimeline}>
                  {journey.segments.map((seg, i) => (
                    <React.Fragment key={i}>
                      <div className={s.segmentDot} style={{ borderColor: modeMeta(seg.mode).color, backgroundColor: modeMeta(seg.mode).color }} />
                      <div className={s.segmentLine} style={{ backgroundColor: modeMeta(seg.mode).color, flex: seg.durationMin || 1 }} />
                    </React.Fragment>
                  ))}
                  <div className={s.segmentDot} style={{ borderColor: '#6d7c8a', backgroundColor: '#6d7c8a' }} />
                </div>

                {/* Segment Labels */}
                <div className={s.segmentDetails}>
                  {journey.segments.map((seg, i) => (
                    <div key={i} className={s.segmentInfo}>
                      <span className={s.segmentModeIcon} style={{ color: modeMeta(seg.mode).color }}>
                        {modeMeta(seg.mode).icon}
                      </span>
                      <span className={s.segmentLabel}>{seg.originCode}→{seg.destinationCode}</span>
                      <span className={s.segmentDuration}>{formatDuration(seg.durationMin)}</span>
                      {seg.provider && <span className={s.providerTag}>{seg.provider}</span>}
                    </div>
                  ))}
                </div>
              </div>

              {/* Expanded Details — NOT clickable for toggle */}
              {expandedCard === journey.id && (
                <div className={s.expandedDetails}>
                  {/* Inline Route Graph */}
                  <JourneyGraphView journey={journey} />
                  <div className={s.expandedTitle}>Route Details</div>
                  {journey.segments.map((seg, i) => (
                    <div key={i} className={s.expandedRow}>
                      <div className={s.expandedIcon} style={{ backgroundColor: `${modeMeta(seg.mode).color}15`, color: modeMeta(seg.mode).color }}>
                        {modeMeta(seg.mode).icon}
                      </div>
                      <div className={s.expandedInfo}>
                        <div className={s.expandedRoute}>
                          <strong>{seg.originName}</strong> → <strong>{seg.destinationName}</strong>
                        </div>
                        <div className={s.expandedMeta}>
                          <span>{seg.departureTime} – {seg.arrivalTime}</span>
                          <span>{formatDuration(seg.durationMin)}</span>
                          {seg.serviceCode && <span className={s.serviceCode}>{seg.serviceCode}</span>}
                          {seg.provider && <span>{seg.provider}</span>}
                          {seg.costCents != null && <span style={{ fontWeight: 600 }}>{formatPrice(seg.costCents, seg.currency)}</span>}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* ── No Results ── */}
      {searched && !loading && filteredResults.length === 0 && (
        <div style={{
          textAlign: 'center',
          padding: '3rem 1rem',
          opacity: 0.6,
          fontSize: '0.9rem',
        }}>
          <FiAlertCircle size={28} style={{ marginBottom: 8, opacity: 0.4 }} />
          <p style={{ fontWeight: 600 }}>No results found</p>
          <p style={{ fontSize: '0.8rem', marginTop: 4 }}>No routes found for the selected date and destination. Try a different date or city.</p>
        </div>
      )}

      {/* ── Empty State ── */}
      {!searched && (
        <div className={s.emptyState}>
          <div className={s.emptyIcon}>
            <FiNavigation size={36} />
          </div>
          <h3>Intermodal Journey Planner</h3>
          <p>
            Enter your origin and destination to discover multi-modal travel routes.
            Compare flights, trains, metro, buses and taxis in a single search.
          </p>
          <div className={s.emptyHints}>
            <span><MdFlight size={14} style={{ color: '#3b82f6' }} /> Flight</span>
            <span><MdTrain size={14} style={{ color: '#f97316' }} /> Train</span>
            <span><MdDirectionsSubway size={14} style={{ color: '#8b5cf6' }} /> Metro</span>
            <span><MdDirectionsBus size={14} style={{ color: '#22c55e' }} /> Bus</span>
            <span><MdLocalTaxi size={14} style={{ color: '#a855f7' }} /> Taxi</span>
            <span><MdDirectionsWalk size={14} style={{ color: '#94a3b8' }} /> Walk</span>
          </div>
        </div>
      )}
    </>
  );
};
