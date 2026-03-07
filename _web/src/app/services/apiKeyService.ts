// File: src/app/services/apiKeyService.ts
import { mockApiKeys } from '../data/mockData';
import { ApiKey } from '../types';

// Bu fonksiyonlar ileride gerçek API çağrıları yapacak.
export const apiKeyService = {
  getApiKeys: async (workspaceId: string): Promise<ApiKey[]> => {
    console.log(`Fetching API keys for workspace ${workspaceId}...`);
    // Simülasyon: 1 saniye bekle
    await new Promise(resolve => setTimeout(resolve, 1000));
    return mockApiKeys;
  },
  createApiKey: async (workspaceId: string, name: string): Promise<ApiKey> => {
    console.log(`Creating API key "${name}" for workspace ${workspaceId}...`);
    await new Promise(resolve => setTimeout(resolve, 500));
    const newKey: ApiKey = {
      id: `key_${Date.now()}`,
      name,
      token: `bhk_${[...Array(32)].map(() => Math.random().toString(36)[2]).join('')}`,
      createdAt: new Date().toISOString().split('T')[0], // YYYY-MM-DD formatı
      lastUsed: null,
    };
    // mockApiKeys.push(newKey); // Gerçek uygulamada bu state'i güncellemek yerine API'den gelen listeyi yeniden çekeriz.
    return newKey;
  },
  deleteApiKey: async (keyId: string): Promise<void> => {
    console.log(`Deleting API key ${keyId}...`);
    await new Promise(resolve => setTimeout(resolve, 500));
    console.log('Key deleted.');
  },
};