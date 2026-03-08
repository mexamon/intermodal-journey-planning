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
  targetCurrency?: string;  // display currency: EUR, TRY, GBP, USD
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
  currency: string | null;  // ISO: EUR, TRY, GBP, USD
  edgeId: string;
  tripId: string | null;
  originTimezone: string | null;       // IANA: Europe/Istanbul
  destinationTimezone: string | null;  // IANA: Europe/London
}

export interface JourneyResult {
  id: string;
  label: string;
  segments: JourneySegment[];
  totalDurationMin: number;
  totalCostCents: number;
  currency: string | null;   // normalized display currency: EUR, TRY, etc.
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
