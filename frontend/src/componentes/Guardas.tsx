import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth, hasRole } from "../context/AuthContext";
import { Cargando } from "./UI";
import { RUTAS } from "../lib/rutas";
export function RutaProtegida() {
  const { session } = useAuth();
  const location = useLocation();
  if (session.status === "loading") return <Cargando />;
  if (session.status === "anonymous")
    return (
      <Navigate to={RUTAS.login} state={{ desde: location.pathname }} replace />
    );
  return <Outlet />;
}
export function RutaPorRol({ roles }: { roles: string[] }) {
  const { session } = useAuth();
  if (session.status === "loading") return <Cargando />;
  if (session.status === "anonymous")
    return <Navigate to={RUTAS.login} replace />;
  return hasRole(session.user, roles) ? (
    <Outlet />
  ) : (
    <Navigate to={RUTAS.sinPermisos} replace />
  );
}
export function SoloAnonimo() {
  const { session } = useAuth();
  if (session.status === "loading") return <Cargando />;
  return session.status === "authenticated" ? (
    <Navigate to={RUTAS.cuenta} replace />
  ) : (
    <Outlet />
  );
}
