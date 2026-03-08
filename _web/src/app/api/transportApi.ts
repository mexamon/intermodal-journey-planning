import { apiGet } from './client';

/* ═══════════════════════════════════════════════
   Transport Mode — matches backend TransportMode entity
   ═══════════════════════════════════════════════ */
export interface TransportModeDto {
  id: string;
  code: string;
  name: string;
  category: string;
  coverageType: string;
  edgeResolution: string;
  requiresStop: boolean;
  maxWalkingAccessM: number | null;
  defaultSpeedKmh: number | null;
  apiProvider: string | null;
  icon: string;
  colorHex: string;
  sortOrder: number;
  isActive: boolean;
}

/* ═══════════════════════════════════════════════
   API Functions
   ═══════════════════════════════════════════════ */

/** Fetch active transport modes sorted by sortOrder */
export async function fetchTransportModes(all = false): Promise<TransportModeDto[]> {
  return apiGet<TransportModeDto[]>(`/transport/modes`, { all });
}
