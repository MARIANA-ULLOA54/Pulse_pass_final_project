import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { setHttpAuth } from "../lib/http";
import type { Identity } from "../lib/schemas";
type Session =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "authenticated"; token: string; user: Identity };
type AuthValue = {
  session: Session;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => void;
};
const Context = createContext<AuthValue | null>(null);
const key = "pulsepass_session";
function stored() {
  try {
    const value = sessionStorage.getItem(key);
    if (!value) return null;
    const data: unknown = JSON.parse(value);
    if (
      typeof data === "object" &&
      data !== null &&
      "token" in data &&
      "expiresAt" in data &&
      typeof data.token === "string" &&
      typeof data.expiresAt === "number" &&
      Date.now() < data.expiresAt
    )
      return data.token;
  } catch {}
  sessionStorage.removeItem(key);
  return null;
}
export function AuthProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient();
  const [session, setSession] = useState<Session>({ status: "loading" });
  const logout = useCallback(() => {
    sessionStorage.removeItem(key);
    setSession({ status: "anonymous" });
    client.clear();
  }, [client]);
  useEffect(() => {
    setHttpAuth(() => {
      try {
        return stored();
      } catch {
        return null;
      }
    }, logout);
    const token = stored();
    if (!token) {
      setSession({ status: "anonymous" });
      return;
    }
    api
      .me()
      .then((user) => setSession({ status: "authenticated", token, user }))
      .catch(() => logout());
  }, [logout]);
  const login = useCallback(
    async (identifier: string, password: string) => {
      const result = await api.login({ identifier, password });
      sessionStorage.setItem(
        key,
        JSON.stringify({
          token: result.accessToken,
          expiresAt: Date.now() + result.expiresIn * 1000,
        }),
      );
      try {
        const user = await api.me();
        setSession({
          status: "authenticated",
          token: result.accessToken,
          user,
        });
      } catch (e) {
        logout();
        throw e;
      }
    },
    [logout],
  );
  const value = useMemo(
    () => ({ session, login, logout }),
    [session, login, logout],
  );
  return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function useAuth() {
  const value = useContext(Context);
  if (!value) throw new Error("AuthProvider requerido");
  return value;
}
export function hasRole(user: Identity, roles: string[]) {
  return user.roles.some((r) => roles.includes(r.replace(/^ROLE_/, "")));
}
