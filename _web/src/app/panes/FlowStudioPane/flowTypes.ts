import type { ReactNode } from 'react';

// ── Journey Phase (state machine node types) ──
export type JourneyPhase = 'start' | 'first_mile' | 'main_haul' | 'interchange' | 'last_mile' | 'end';

// ── Transport Modes (from DB transport_mode.code) ──
export type TransportMode = 'FLIGHT' | 'BUS' | 'TRAIN' | 'SUBWAY' | 'TAXI' | 'WALKING' | 'FERRY' | 'SHUTTLE';

// ── Per-Mode Config (within a phase) ──
export type ModeConfig = {
  mode: TransportMode;
  enabled: boolean;
  providerId?: string;
  providerName?: string;
  maxWalkingAccessM?: number;
  maxDurationMin?: number;
  notes?: string;
};

// ── Route Node Data (attached to React Flow node) ──
export type RouteNodeData = {
  label: string;
  phase: JourneyPhase;
  modeConfigs: ModeConfig[];       // per-mode rules for this phase
  minVisits: number;
  maxVisits: number;
  maxLegsInPhase?: number;          // max segments within this phase
  notes?: string;
};

// ── Route Edge Data (attached to React Flow edge) ──
export type RouteEdgeData = {
  sameAirport: boolean;
  maxDurationMin?: number;
  maxWalkingM?: number;
  requireSameTerminal: boolean;
  minConnectionMin?: number;        // min connection time
  priority: number;
  guardJson?: string;
};

// ── Policy Constraints (global limits for the policy set) ──
export type PolicyConstraints = {
  maxLegs: number;
  minFlights: number;
  maxFlights: number;
  maxTransfers: number;
  maxWalkingTotalM: number;
  maxTotalDurationMin: number;
  minConnectionMin: number;
  maxTotalCo2Grams?: number;
};

// ── Policy Set ──
export type PolicyScope = 'GLOBAL' | 'COUNTRY' | 'REGION' | 'AIRPORT' | 'AIRPORT_PAIR';

export type PolicySet = {
  id: string;
  name: string;
  code: string;
  scope: PolicyScope;
  scopeKey: string;                  // * (global), TR (country), IST (airport), IST-LHR (pair)
  segment: 'DEFAULT' | 'CORPORATE' | 'ELITE' | 'IRROPS';
  status: 'DRAFT' | 'ACTIVE' | 'DEPRECATED';
  description?: string;
  constraints: PolicyConstraints;
  lastSaved: string;
};

// ── Airport ──
export type Airport = {
  iata: string;
  name: string;
  city: string;
  country: string;
};

// ── Phase Library Item (for "+" add node popover) ──
export type PhaseLibraryItem = {
  id: string;
  phase: JourneyPhase;
  label: string;
  description: string;
  icon: ReactNode;
  defaultModes: ModeConfig[];
};

// ── Mock Provider ──
export type MockProvider = {
  id: string;
  name: string;
  type: string;
  applicableModes: TransportMode[];
};
