import { createContext, type PropsWithChildren, useContext, useEffect, useMemo, useState } from 'react';

import { ApiClientError, getSessionIdentity, postJson } from '@/src/api/client';
import { clearSession, loadSession, saveSession } from '@/src/services/authStorage';
import type { AuthenticationResponse, AuthSession } from '@/src/types/auth';

type AuthContextValue = {
  busy: boolean;
  clearError: () => void;
  error: string;
  initializing: boolean;
  session: AuthSession | null;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadSession()
      .then(async (storedSession) => {
        if (!storedSession) return;
        if (storedSession.roles?.length && storedSession.userId && storedSession.username) {
          setSession(storedSession);
          return;
        }

        try {
          const identity = await getSessionIdentity(storedSession.accessToken);
          const hydratedSession = { ...storedSession, ...identity };
          await saveSession(hydratedSession);
          setSession(hydratedSession);
        } catch {
          await clearSession();
        }
      })
      .finally(() => setInitializing(false));
  }, []);

  async function signIn(email: string, password: string) {
    setBusy(true);
    setError('');

    try {
      const response = await postJson<AuthenticationResponse, { email: string; password: string }>(
        '/auth/login',
        { email, password },
      );
      const nextSession: AuthSession = {
        ...response,
        email,
        expiresAt: Date.now() + response.expiresIn * 1000,
        ...(await getSessionIdentity(response.accessToken)),
      };
      await saveSession(nextSession);
      setSession(nextSession);
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : 'Unable to sign in. Please try again.',
      );
      throw requestError;
    } finally {
      setBusy(false);
    }
  }

  async function signOut() {
    setBusy(true);
    try {
      await clearSession();
      setSession(null);
      setError('');
    } finally {
      setBusy(false);
    }
  }

  const value = useMemo(
    () => ({
      busy,
      clearError: () => setError(''),
      error,
      initializing,
      session,
      signIn,
      signOut,
    }),
    [busy, error, initializing, session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
