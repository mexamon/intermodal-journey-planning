import { apiPost } from './client';

/* ═══════════════════════════════════════════════
   Journey API Types — matches backend DTOs
   ═══════════════════════════════════════════════ */
export interface JourneySearchRequest {
  originLocationId?: string;
  originIataCode?: string;
  originQuery?: string;           // free-text: "Kadıköy", "Taksim"
  originLat?: number;             // from selected autocomplete
  originLon?: number;
  originType?: 'airport' | 'station' | 'place';
  destinationLocationId?: string;
  destinationIataCode?: string;
  destinationQuery?: string;      // free-text: "London Heathrow"
  destinationLat?: number;
  destinationLon?: number;
  destinationType?: 'airport' | 'station' | 'place';
  departureDate?: string;       // ISO date: 2026-03-08
  earliestDeparture?: string;   // HH:mm
  maxTransfers?: number;
  maxDurationMinutes?: number;
  preferredModes?: string[];
  sortBy?: 'FASTEST' | 'CHEAPEST' | 'GREENEST' | 'FEWEST_TRANSFERS';
}

export interface JourneySegment {
  mode: string;
  originCode: string;
  originName: string;
  destinationCode: string;
  destinationName: string;
  departureTime: string;
  arrivalTime: string;
  durationMin: number;
  serviceCode: string | null;
  provider: string | null;
  costCents: number;
  edgeId: string;
  tripId: string | null;
}

export interface JourneyResult {
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
   API Functions
   ═══════════════════════════════════════════════ */
export async function searchJourneys(request: JourneySearchRequest): Promise<JourneyResult[]> {
  return apiPost<JourneyResult[]>('/journey/search', request);
}
