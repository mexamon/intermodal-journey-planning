import { create } from 'zustand';

export type CurrencyCode = 'EUR' | 'TRY' | 'GBP' | 'USD';

export interface CurrencyOption {
  code: CurrencyCode;
  symbol: string;
  label: string;
}

export const CURRENCIES: CurrencyOption[] = [
  { code: 'EUR', symbol: '€', label: 'Euro' },
  { code: 'TRY', symbol: '₺', label: 'Türk Lirası' },
  { code: 'GBP', symbol: '£', label: 'British Pound' },
  { code: 'USD', symbol: '$', label: 'US Dollar' },
];

interface SettingsState {
  currency: CurrencyCode;
  setCurrency: (c: CurrencyCode) => void;
}

export const useSettingsStore = create<SettingsState>((set) => ({
  currency: 'EUR',
  setCurrency: (currency) => set({ currency }),
}));
