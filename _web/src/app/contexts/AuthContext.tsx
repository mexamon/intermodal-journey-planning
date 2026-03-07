import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';

interface AuthUser {
  id: string;
  name: string;
  email: string;
  avatar?: string;
}

interface AuthContextValue {
  isAuthenticated: boolean;
  user: AuthUser | null;
  token: string | null;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const TOKEN_KEY = 'intermodal_auth_token';
const USER_KEY = 'intermodal_auth_user';

/* ── Mock users for frontend-only auth ── */
const MOCK_USERS: { email: string; password: string; user: AuthUser }[] = [
  { email: 'admin@intermodal.io', password: 'admin', user: { id: 'u1', name: 'Engin Mahmut', email: 'admin@intermodal.io', avatar: 'https://github.com/mexamon.png' } },
  { email: 'demo@intermodal.io', password: 'demo', user: { id: 'u2', name: 'Demo User', email: 'demo@intermodal.io' } },
];

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<AuthUser | null>(() => {
    try { const raw = localStorage.getItem(USER_KEY); return raw ? JSON.parse(raw) : null; }
    catch { return null; }
  });

  const isAuthenticated = !!token && !!user;

  const login = useCallback(async (email: string, password: string): Promise<boolean> => {
    // Simulate network delay
    await new Promise(r => setTimeout(r, 600));
    const match = MOCK_USERS.find(m => m.email === email && m.password === password);
    if (!match) return false;
    const fakeToken = `tok_${Date.now()}_${Math.random().toString(36).slice(2)}`;
    localStorage.setItem(TOKEN_KEY, fakeToken);
    localStorage.setItem(USER_KEY, JSON.stringify(match.user));
    setToken(fakeToken);
    setUser(match.user);
    return true;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
