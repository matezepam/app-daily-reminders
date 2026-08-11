import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  ApiClientError,
  apiRequest,
  authenticate,
  getMyProfile,
  refreshTokens,
} from "@/src/api/client";
import {
  clearSession,
  loadSession,
  saveSession,
} from "@/src/services/authStorage";
import type { AuthSession, UserProfile } from "@/src/types/auth";

type AuthContextValue = {
  busy: boolean;
  error: string;
  initializing: boolean;
  session: AuthSession | null;
  clearError: () => void;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  getSession: () => Promise<AuthSession>;
  updateProfile: (fullName: string) => Promise<UserProfile>;
};
const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const renew = useCallback(async (stored: AuthSession) => {
    if (stored.expiresAt > Date.now() + 30_000) return stored;
    if (!stored.refreshToken)
      throw new ApiClientError(
        "Tu sesión venció. Inicia sesión nuevamente.",
        401,
      );
    const tokens = await refreshTokens(stored.refreshToken);
    const next = {
      ...stored,
      accessToken: tokens.AccessToken,
      idToken: tokens.IdToken,
      expiresAt: Date.now() + tokens.ExpiresIn * 1000,
    };
    await saveSession(next);
    setSession(next);
    return next;
  }, []);

  useEffect(() => {
    loadSession()
      .then(async (stored) => {
        if (!stored) return;
        try {
          const active = await renew(stored);
          const profile = await getMyProfile(active.accessToken);
          const next = { ...active, profile };
          await saveSession(next);
          setSession(next);
        } catch {
          await clearSession();
        }
      })
      .finally(() => setInitializing(false));
  }, [renew]);

  async function signIn(email: string, password: string) {
    setBusy(true);
    setError("");
    try {
      const tokens = await authenticate(email.trim(), password);
      const profile = await getMyProfile(tokens.AccessToken);
      const next: AuthSession = {
        accessToken: tokens.AccessToken,
        idToken: tokens.IdToken,
        refreshToken: tokens.RefreshToken ?? null,
        expiresAt: Date.now() + tokens.ExpiresIn * 1000,
        profile,
      };
      await saveSession(next);
      setSession(next);
    } catch (cause) {
      const message =
        cause instanceof ApiClientError
          ? cause.message
          : "No se pudo iniciar sesión.";
      setError(message);
      throw cause;
    } finally {
      setBusy(false);
    }
  }
  async function signOut() {
    setBusy(true);
    try {
      await clearSession();
      setSession(null);
      setError("");
    } finally {
      setBusy(false);
    }
  }
  const getSession = useCallback(async () => {
    if (!session) throw new ApiClientError("Debes iniciar sesión.", 401);
    try {
      return await renew(session);
    } catch (cause) {
      await clearSession();
      setSession(null);
      throw cause;
    }
  }, [renew, session]);
  const updateProfile = useCallback(
    async (fullName: string) => {
      const active = await getSession();
      const profile = await apiRequest<UserProfile>(active, "/users/me", {
        method: "PUT",
        body: { fullName: fullName.trim() },
      });
      const next = { ...active, profile };
      await saveSession(next);
      setSession(next);
      return profile;
    },
    [getSession],
  );
  const value = useMemo(
    () => ({
      busy,
      error,
      initializing,
      session,
      clearError: () => setError(""),
      signIn,
      signOut,
      getSession,
      updateProfile,
    }),
    [busy, error, initializing, session, getSession, updateProfile],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
