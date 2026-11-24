import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Navigate, useLocation } from "react-router";
import { z } from "zod";
import { api } from "../lib/api";
import { setHttpAuth } from "../lib/http";
import { RUTAS } from "../lib/rutas";
import type { Identity } from "../lib/schemas";

const credentialsSchema = z.object({
  token: z.string().min(1),
  expiresAt: z.number(),
});
type Credentials = z.infer<typeof credentialsSchema>;
type Session =
  | { status: "loading" }
  | { status: "anonymous" }
  | ({ status: "authenticated"; user: Identity } & Credentials);
type AuthValue = {
  session: Session;
  login: (identifier: string, password: string, destination?: string) => Promise<void>;
  logout: () => void;
};

type Redirect = {
  to: string;
  fromKey: string;
  state?: { motivo: "expirada"; desde: string };
};

const Context = createContext<AuthValue | null>(null);
const key = "pulsepass_session";

function readCredentials() {
  try {
    return credentialsSchema.safeParse(JSON.parse(sessionStorage.getItem(key) ?? "null")).data ?? null;
  } catch {
    return null;
  }
}

function saveCredentials(credentials: Credentials | null) {
  try {
    if (credentials) sessionStorage.setItem(key, JSON.stringify(credentials));
    else sessionStorage.removeItem(key);
  } catch {
    // La sesión sigue disponible en memoria si el navegador bloquea el almacenamiento.
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient();
  const location = useLocation();
  const currentLocation = useRef(location);
  const credentials = useRef<Credentials | null>(null);
  const [session, setSession] = useState<Session>({ status: "loading" });
  const [redirect, setRedirect] = useState<Redirect | null>(null);

  useLayoutEffect(() => {
    currentLocation.current = location;
  }, [location]);

  useEffect(() => {
    if (redirect && location.key !== redirect.fromKey) setRedirect(null);
  }, [location.key, redirect]);

  const clearSession = useCallback(() => {
    credentials.current = null;
    saveCredentials(null);
    setSession({ status: "anonymous" });
    client.clear();
  }, [client]);

  const logout = useCallback(() => {
    clearSession();
    setRedirect({ to: RUTAS.inicio, fromKey: currentLocation.current.key });
  }, [clearSession]);

  const expire = useCallback(() => {
    const { key: fromKey, pathname, search, hash } = currentLocation.current;
    clearSession();
    setRedirect({
      to: RUTAS.login,
      fromKey,
      state: { motivo: "expirada", desde: pathname + search + hash },
    });
  }, [clearSession]);

  useLayoutEffect(
    () => setHttpAuth(() => credentials.current?.token ?? null, expire),
    [expire],
  );

  useLayoutEffect(() => {
    const saved = readCredentials();
    if (!saved || saved.expiresAt <= Date.now()) {
      saveCredentials(null);
      setSession({ status: "anonymous" });
      return;
    }
    credentials.current = saved;
    let restoring = true;
    client.fetchQuery({
      queryKey: ["auth", "me"],
      queryFn: ({ signal }) => api.me(signal),
      retry: false,
      staleTime: 0,
    }).then((user) => {
      if (restoring && credentials.current === saved) {
        if (saved.expiresAt <= Date.now()) expire();
        else setSession({ status: "authenticated", ...saved, user });
      }
    }).catch(() => {
      if (restoring && credentials.current === saved) clearSession();
    });
    return () => {
      restoring = false;
      void client.cancelQueries({ queryKey: ["auth", "me"], exact: true });
    };
  }, [clearSession, client, expire]);

  useEffect(() => {
    if (session.status !== "authenticated") return;
    const timer = window.setTimeout(expire, Math.max(0, session.expiresAt - Date.now()));
    return () => window.clearTimeout(timer);
  }, [session, expire]);

  const login = useCallback(async (identifier: string, password: string, destination: string = RUTAS.inicio) => {
    const result = await api.login({ identifier, password });
    const next = {
      token: result.accessToken,
      expiresAt: Date.now() + result.expiresIn * 1000,
    };
    credentials.current = next;
    try {
      const user = await client.fetchQuery({
        queryKey: ["auth", "me"],
        queryFn: ({ signal }) => api.me(signal, false),
        retry: false,
        staleTime: 0,
      });
      saveCredentials(next);
      setSession({ status: "authenticated", ...next, user });
      setRedirect({ to: destination, fromKey: currentLocation.current.key });
    } catch (error) {
      clearSession();
      throw error;
    }
  }, [clearSession, client]);

  const value = useMemo(() => ({ session, login, logout }), [session, login, logout]);
  return (
    <Context.Provider value={value}>
      {redirect && location.key === redirect.fromKey ? (
        <Navigate to={redirect.to} state={redirect.state} replace />
      ) : children}
    </Context.Provider>
  );
}

export function useAuth() {
  const value = useContext(Context);
  if (!value) throw new Error("AuthProvider requerido");
  return value;
}

export function hasRole(user: Identity, roles: string[]) {
  return user.roles.some((role) => roles.includes(role.replace(/^ROLE_/, "")));
}
