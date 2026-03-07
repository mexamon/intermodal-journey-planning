import React from 'react';
import type { Edge, Node } from '@xyflow/react';
import { MdFlight, MdDirectionsBus, MdTrain, MdDirectionsWalk, MdLocalTaxi, MdDirectionsSubway } from 'react-icons/md';
import { FiPlay, FiFlag, FiTruck, FiAnchor, FiRefreshCw } from 'react-icons/fi';
import type { Airport, JourneyPhase, ModeConfig, MockProvider, PhaseLibraryItem, PolicySet, RouteEdgeData, RouteNodeData, TransportMode } from './flowTypes';

// ── Default mode configs (all modes) ──
const allGroundModes: ModeConfig[] = [
  { mode: 'BUS',     enabled: true },
  { mode: 'TRAIN',   enabled: true },
  { mode: 'SUBWAY',  enabled: true },
  { mode: 'TAXI',    enabled: true },
  { mode: 'WALKING', enabled: true },
  { mode: 'SHUTTLE', enabled: false },
  { mode: 'FERRY',   enabled: false },
];

const flightModes: ModeConfig[] = [
  { mode: 'FLIGHT', enabled: true },
];

const transferModes: ModeConfig[] = [
  { mode: 'TRAIN',   enabled: true },
  { mode: 'BUS',     enabled: true },
  { mode: 'TAXI',    enabled: true },
  { mode: 'SHUTTLE', enabled: false },
];

// ── Defaults ──
export const defaultEdgeData: RouteEdgeData = {
  sameAirport: false,
  maxDurationMin: undefined,
  maxWalkingM: undefined,
  requireSameTerminal: false,
  minConnectionMin: undefined,
  priority: 1,
  guardJson: '',
};

export const defaultPolicySet: PolicySet = {
  id: 'ps_default',
  name: 'Default Route Policy',
  code: 'GLOBAL_DEFAULT',
  scope: 'GLOBAL',
  scopeKey: '*',
  segment: 'DEFAULT',
  status: 'DRAFT',
  description: 'Global default routing policy for all routes',
  constraints: {
    maxLegs: 5,
    minFlights: 1,
    maxFlights: 2,
    maxTransfers: 2,
    maxWalkingTotalM: 2000,
    maxTotalDurationMin: 720,
    minConnectionMin: 60,
    maxTotalCo2Grams: undefined,
  },
  lastSaved: 'just now',
};

// ── Phase Colors ──
export const phaseColors: Record<JourneyPhase, { bg: string; border: string; text: string }> = {
  start:       { bg: '#374151', border: '#6b7280', text: '#f9fafb' },
  first_mile:  { bg: '#1e3a5f', border: '#3b82f6', text: '#93c5fd' },
  main_haul:   { bg: '#4a0d1a', border: '#C8102E', text: '#fca5a5' },
  interchange: { bg: '#2d1b4e', border: '#a855f7', text: '#d8b4fe' },
  last_mile:   { bg: '#14532d', border: '#22c55e', text: '#86efac' },
  end:         { bg: '#374151', border: '#6b7280', text: '#f9fafb' },
};

// ── Transport Mode Metadata ──
export const transportModesMeta: Record<TransportMode, { icon: React.ReactNode; color: string; label: string }> = {
  FLIGHT:  { icon: <MdFlight />,           color: '#C8102E', label: 'Flight' },
  BUS:     { icon: <MdDirectionsBus />,    color: '#3b82f6', label: 'Bus' },
  TRAIN:   { icon: <MdTrain />,            color: '#22c55e', label: 'Train' },
  SUBWAY:  { icon: <MdDirectionsSubway />, color: '#f97316', label: 'Metro' },
  TAXI:    { icon: <MdLocalTaxi />,        color: '#a855f7', label: 'Taxi' },
  WALKING: { icon: <MdDirectionsWalk />,   color: '#94a3b8', label: 'Walking' },
  FERRY:   { icon: <FiAnchor />,           color: '#0ea5e9', label: 'Ferry' },
  SHUTTLE: { icon: <FiTruck />,            color: '#ec4899', label: 'Shuttle' },
};

// ── Phase Library ──
export const phaseLibrary: PhaseLibraryItem[] = [
  { id: 'pl_start', phase: 'start',       label: 'Start',       description: 'Journey origin',                icon: <FiPlay />,      defaultModes: [] },
  { id: 'pl_fm',    phase: 'first_mile',   label: 'First Mile',  description: 'Origin → Airport transfer',     icon: <MdDirectionsBus />, defaultModes: [...allGroundModes] },
  { id: 'pl_mh',    phase: 'main_haul',    label: 'Main Haul',   description: 'Primary air transport',          icon: <MdFlight />,    defaultModes: [...flightModes] },
  { id: 'pl_ic',    phase: 'interchange',  label: 'Interchange', description: 'Inter-airport transfer',         icon: <FiRefreshCw />, defaultModes: [...transferModes] },
  { id: 'pl_lm',    phase: 'last_mile',    label: 'Last Mile',   description: 'Airport → Destination transfer', icon: <MdTrain />,     defaultModes: [...allGroundModes] },
  { id: 'pl_end',   phase: 'end',          label: 'End',         description: 'Journey destination',             icon: <FiFlag />,      defaultModes: [] },
];

// ── Default Graph ──
export const initialNodes: Node<RouteNodeData>[] = [
  {
    id: 'node_start', type: 'routePhase', position: { x: 60, y: 200 },
    data: { label: 'Start', phase: 'start', modeConfigs: [], minVisits: 1, maxVisits: 1 },
  },
  {
    id: 'node_fm', type: 'routePhase', position: { x: 300, y: 200 },
    data: { label: 'First Mile', phase: 'first_mile', modeConfigs: [...allGroundModes], minVisits: 0, maxVisits: 1, maxLegsInPhase: 2 },
  },
  {
    id: 'node_mh', type: 'routePhase', position: { x: 560, y: 200 },
    data: { label: 'Main Haul', phase: 'main_haul', modeConfigs: [...flightModes], minVisits: 1, maxVisits: 2 },
  },
  {
    id: 'node_ic', type: 'routePhase', position: { x: 560, y: 380 },
    data: {
      label: 'Interchange', phase: 'interchange',
      modeConfigs: [...transferModes],
      minVisits: 0, maxVisits: 1, maxLegsInPhase: 1,
      notes: 'Ground transfer between airports (e.g. FRA→MUC by train)',
    },
  },
  {
    id: 'node_lm', type: 'routePhase', position: { x: 820, y: 200 },
    data: { label: 'Last Mile', phase: 'last_mile', modeConfigs: [...allGroundModes], minVisits: 0, maxVisits: 1, maxLegsInPhase: 2 },
  },
  {
    id: 'node_end', type: 'routePhase', position: { x: 1060, y: 200 },
    data: { label: 'End', phase: 'end', modeConfigs: [], minVisits: 1, maxVisits: 1 },
  },
];

export const initialEdges: Edge<RouteEdgeData>[] = [
  // Normal flow
  { id: 'e1', source: 'node_start', target: 'node_fm',  label: 'ground',   data: { ...defaultEdgeData } },
  { id: 'e2', source: 'node_start', target: 'node_mh',  label: 'direct',   data: { ...defaultEdgeData } },
  { id: 'e3', source: 'node_fm',    target: 'node_mh',  label: 'to airport', data: { ...defaultEdgeData } },
  // After flight
  { id: 'e4', source: 'node_mh',    target: 'node_lm',  label: 'land + ground', data: { ...defaultEdgeData } },
  { id: 'e5', source: 'node_mh',    target: 'node_end', label: 'direct arrive', data: { ...defaultEdgeData } },
  { id: 'e6', source: 'node_lm',    target: 'node_end', label: 'arrive',   data: { ...defaultEdgeData } },
  // Interchange loop: Flight → ground transfer → Flight (again)
  { id: 'e7', source: 'node_mh',    target: 'node_ic',  label: 'to interchange', data: { ...defaultEdgeData, maxDurationMin: 240 } },
  { id: 'e8', source: 'node_ic',    target: 'node_mh',  label: 'to 2nd flight',  data: { ...defaultEdgeData, minConnectionMin: 90 } },
];

// ── Airports ──
export const mockAirports: Airport[] = [
  { iata: 'IST', name: 'Istanbul Airport', city: 'Istanbul', country: 'TR' },
  { iata: 'SAW', name: 'Sabiha Gökçen', city: 'Istanbul', country: 'TR' },
  { iata: 'ESB', name: 'Esenboğa Airport', city: 'Ankara', country: 'TR' },
  { iata: 'ADB', name: 'Adnan Menderes', city: 'Izmir', country: 'TR' },
  { iata: 'AYT', name: 'Antalya Airport', city: 'Antalya', country: 'TR' },
  { iata: 'LHR', name: 'Heathrow Airport', city: 'London', country: 'GB' },
  { iata: 'CDG', name: 'Charles de Gaulle', city: 'Paris', country: 'FR' },
  { iata: 'FRA', name: 'Frankfurt Airport', city: 'Frankfurt', country: 'DE' },
  { iata: 'DUS', name: 'Düsseldorf Airport', city: 'Düsseldorf', country: 'DE' },
  { iata: 'JFK', name: 'John F. Kennedy', city: 'New York', country: 'US' },
  { iata: 'DXB', name: 'Dubai International', city: 'Dubai', country: 'AE' },
];

// ── Providers ──
export const mockProviders: MockProvider[] = [
  { id: 'prov_thy',      name: 'Turkish Airlines',  type: 'AIRLINE',         applicableModes: ['FLIGHT'] },
  { id: 'prov_pegasus',  name: 'Pegasus Airlines',  type: 'AIRLINE',         applicableModes: ['FLIGHT'] },
  { id: 'prov_sun',      name: 'SunExpress',        type: 'AIRLINE',         applicableModes: ['FLIGHT'] },
  { id: 'prov_havaist',  name: 'HAVAIST',           type: 'BUS_COMPANY',     applicableModes: ['BUS'] },
  { id: 'prov_iett',     name: 'IETT',              type: 'BUS_COMPANY',     applicableModes: ['BUS'] },
  { id: 'prov_db',       name: 'Deutsche Bahn',     type: 'TRAIN_OPERATOR',  applicableModes: ['TRAIN'] },
  { id: 'prov_tcdd',     name: 'TCDD',              type: 'TRAIN_OPERATOR',  applicableModes: ['TRAIN'] },
  { id: 'prov_metro_ist',name: 'İstanbul Metro',    type: 'METRO_OPERATOR',  applicableModes: ['SUBWAY'] },
  { id: 'prov_bitaksi',  name: 'BiTaksi',           type: 'RIDE_SHARE',      applicableModes: ['TAXI'] },
  { id: 'prov_uber',     name: 'Uber',              type: 'RIDE_SHARE',      applicableModes: ['TAXI'] },
  { id: 'prov_shuttle',  name: 'XYZ Fleet',         type: 'OTHER',           applicableModes: ['SHUTTLE'] },
];
