import { useState } from "react";
import { Link, NavLink, Outlet } from "react-router";
import { Menu, X, Ticket, Sun, Moon } from "lucide-react";
import { useAuth, hasRole } from "../context/AuthContext";
import { RUTAS } from "../lib/rutas";
export default function Layout() {
  const { session, logout } = useAuth();
  const [menu, setMenu] = useState(false);
  const [dark, setDark] = useState(() =>
    document.documentElement.classList.contains("dark"),
  );
  const links = [
    ["Eventos", RUTAS.eventos],
    ["Artistas", RUTAS.artistas],
    ["Sedes", RUTAS.sedes],
  ];
  if (session.status === "authenticated") {
    links.push([`Mi cuenta (${session.user.username})`, RUTAS.cuenta]);
    if (hasRole(session.user, ["USER"]))
      links.push(["Mis tickets", RUTAS.tickets]);
    if (hasRole(session.user, ["STAFF", "ADMIN"]))
      links.push(["Validar ingreso", RUTAS.validacion]);
    if (hasRole(session.user, ["ADMIN"]))
      links.push(["Administrar", RUTAS.admin]);
  }
  function theme() {
    document.documentElement.classList.toggle("dark", !dark);
    setDark(!dark);
  }
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <a href="#contenido" className="sr-only focus:not-sr-only">
        Saltar al contenido
      </a>
      <header className="sticky top-0 z-30 bg-white/95 dark:bg-slate-900/95 border-b border-slate-200 dark:border-slate-800">
        <nav
          aria-label="Principal"
          className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between gap-5"
        >
          <Link
            to={RUTAS.inicio}
            className="flex items-center gap-2 font-titulo font-black text-xl"
          >
            <span className="bg-marca-600 text-white rounded-lg p-2">
              <Ticket size={19} />
            </span>
            PulsePass
          </Link>
          <div className="hidden lg:flex items-center gap-5">
            {links.map(([label, path]) => (
              <NavLink
                key={path}
                to={path}
                className={({ isActive }) =>
                  isActive
                    ? "text-marca-600 font-semibold"
                    : "hover:text-marca-600"
                }
              >
                {label}
              </NavLink>
            ))}
          </div>
          <div className="hidden lg:flex items-center gap-2">
            <button aria-label="Cambiar tema" onClick={theme} className="p-2">
              {dark ? <Sun /> : <Moon />}
            </button>
            {session.status === "authenticated" ? (
              <button onClick={logout} className="text-sm font-semibold">
                Salir
              </button>
            ) : (
              <>
                <Link to={RUTAS.login} className="px-3">
                  Ingresar
                </Link>
                <Link
                  to={RUTAS.registro}
                  className="bg-marca-600 text-white px-4 py-2 rounded-lg"
                >
                  Registrarse
                </Link>
              </>
            )}
          </div>
          <button
            className="lg:hidden p-2"
            aria-label={menu ? "Cerrar menú" : "Abrir menú"}
            aria-expanded={menu}
            onClick={() => setMenu(!menu)}
          >
            {menu ? <X /> : <Menu />}
          </button>
        </nav>
        {menu && (
          <div className="lg:hidden p-4 border-t border-slate-200 dark:border-slate-800 space-y-3">
            {links.map(([label, path]) => (
              <Link
                onClick={() => setMenu(false)}
                className="block py-2"
                to={path}
                key={path}
              >
                {label}
              </Link>
            ))}
            {session.status === "authenticated" ? (
              <button
                onClick={() => {
                  logout();
                  setMenu(false);
                }}
              >
                Salir
              </button>
            ) : (
              <>
                <Link
                  className="block"
                  to={RUTAS.login}
                  onClick={() => setMenu(false)}
                >
                  Ingresar
                </Link>
                <Link
                  className="block"
                  to={RUTAS.registro}
                  onClick={() => setMenu(false)}
                >
                  Registrarse
                </Link>
              </>
            )}
            <button className="block" onClick={theme}>
              Cambiar tema
            </button>
          </div>
        )}
      </header>
      <main
        id="contenido"
        className="w-full max-w-6xl mx-auto px-4 py-8 flex-1"
      >
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 dark:border-slate-800 text-slate-500 text-sm py-6">
        <div className="max-w-6xl mx-auto px-4 flex flex-wrap justify-between gap-3">
          <span>© 2026 PulsePass · Caso de estudio académico</span>
          <span>Eventos · Artistas · Sedes</span>
        </div>
      </footer>
    </div>
  );
}
