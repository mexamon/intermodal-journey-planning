import { create } from 'zustand';
import { searchJourneys, JourneySearchRequest, JourneyResult } from '../api';
import { ApiError } from '../api/client';

interface JourneyState {
  /* ── Data ── */
  results: JourneyResult[];
  selectedJourney: JourneyResult | null;

  /* ── UI State ── */
  loading: boolean;
  error: string | null;

  /* ── Search Params ── */
  lastRequest: JourneySearchRequest | null;

  /* ── Actions ── */
  search: (request: JourneySearchRequest) => Promise<void>;
  selectJourney: (journey: JourneyResult | null) => void;
  clearResults: () => void;
  clearError: () => void;
}

export const useJourneyStore = create<JourneyState>((set, get) => ({
  results: [],
  selectedJourney: null,
  loading: false,
  error: null,
  lastRequest: null,

  search: async (request: JourneySearchRequest) => {
    set({ loading: true, error: null, lastRequest: request });
    try {
      const results = await searchJourneys(request);
      set({
        results,
        loading: false,
        selectedJourney: results.length > 0 ? results[0] : null,
      });
    } catch (err) {
      const message = err instanceof ApiError
        ? err.message
        : 'Yolculuk araması sırasında bir hata oluştu.';
      set({ loading: false, error: message, results: [] });
    }
  },

  selectJourney: (journey) => set({ selectedJourney: journey }),

  clearResults: () => set({ results: [], selectedJourney: null, error: null }),

  clearError: () => set({ error: null }),
}));
