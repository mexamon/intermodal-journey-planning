import { apiPost, apiGet } from './client';

/* ═══════════════════════════════════════════════
   Location Types — matches backend Location entity
   ═══════════════════════════════════════════════ */
export interface LocationResult {
  id: string;
  name: string;
  iataCode: string | null;
  icaoCode: string | null;
  city: string | null;
  countryIsoCode: string | null;
  type: string;         // AIRPORT, TRAIN_STATION, BUS_TERMINAL, FERRY_PORT, CITY_CENTER
  lat: number;
  lon: number;
  searchPriority: number;
}

export interface LocationSearchRequest {
  name?: string;
  iataCode?: string;
  icaoCode?: string;
  countryIsoCode?: string;
  city?: string;
  type?: string;
}

/* ═══════════════════════════════════════════════
   API Functions
   ═══════════════════════════════════════════════ */
export async function searchLocations(request: LocationSearchRequest, page = 0, size = 10): Promise<LocationResult[]> {
  const result = await apiPost<{ content: LocationResult[] }>(
    `/inventory/locations/search?page=${page}&size=${size}&sort=searchPriority,desc`,
    request
  );
  return result.content;
}

export async function getLocationByIata(iataCode: string): Promise<LocationResult> {
  return apiGet<LocationResult>(`/inventory/locations/iata/${iataCode}`);
}

/* ═══════════════════════════════════════════════
   Lightweight Airport Lookup (no N+1)
   ═══════════════════════════════════════════════ */
export interface AirportLookup {
  iata: string;
  name: string;
  city: string;
  country: string;
}

export async function lookupAirports(query?: string): Promise<AirportLookup[]> {
  const params = query ? `?q=${encodeURIComponent(query)}` : '';
  const result = await apiGet<AirportLookup[]>(`/inventory/airports/lookup${params}`);
  return result;
}
