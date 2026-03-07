// File: src/app/hooks/useApiKeys.ts
import { useState, useEffect, useCallback } from 'react';
import { apiKeyService } from '../services/apiKeyService';
import { ApiKey } from '../types';

export const useApiKeys = (workspaceId: string) => {
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchKeys = useCallback(async () => {
    try {
      setIsLoading(true);
      const fetchedKeys = await apiKeyService.getApiKeys(workspaceId);
      setKeys(fetchedKeys);
    } catch (err) {
      setError('Failed to load API keys.');
    } finally {
      setIsLoading(false);
    }
  }, [workspaceId]);

  useEffect(() => {
    fetchKeys();
  }, [fetchKeys]);

  const createKey = useCallback(async (name: string) => {
    try {
        const newKey = await apiKeyService.createApiKey(workspaceId, name);
        // Yeni anahtar oluşturulduktan sonra listeyi tazelemek en iyi pratiktir.
        await fetchKeys(); 
        return newKey; // Oluşturulan anahtarı modal'da göstermek için geri döndür
    } catch (err) {
        // Hata yönetimi eklenebilir.
        console.error("Failed to create key", err);
    }
  }, [workspaceId, fetchKeys]);

  const deleteKey = useCallback(async (keyId: string) => {
    try {
        await apiKeyService.deleteApiKey(keyId);
        setKeys(prevKeys => prevKeys.filter(key => key.id !== keyId));
    } catch (err) {
        // Hata yönetimi eklenebilir.
        console.error("Failed to delete key", err);
    }
  }, []);

  return { keys, isLoading, error, createKey, deleteKey };
};
