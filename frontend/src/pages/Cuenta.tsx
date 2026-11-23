import { useState } from "react";
import { Link } from "react-router";
import { api } from "../lib/api";
import { hasRole, useAuth } from "../context/AuthContext";
import { Boton, Heading, Panel } from "../componentes/UI";
import { RUTAS } from "../lib/rutas";
import { Remote } from "./shared";

export default function Cuenta() {
  const { session, logout } = useAuth();
  const [lookup, setLookup] = useState<"email" | "username">("email");
  if (session.status !== "authenticated") return null;
  const { email, roles, username } = session.user;

  return (
    <section className="max-w-2xl mx-auto">
      <Heading title="Mi cuenta" />
      <Panel>
        <div className="flex flex-wrap justify-between gap-3 mb-5">
          <p className="break-all">@{username} · {email}</p>
          <ul aria-label="Roles" className="flex flex-wrap gap-2">
            {roles.map((role) => (
              <li key={role} className="rounded-full bg-slate-100 px-3 py-1 text-sm font-medium dark:bg-slate-800">
                {role.replace(/^ROLE_/, "")}
              </li>
            ))}
          </ul>
        </div>
        <Remote
          keyName={["profile", lookup, lookup === "email" ? email : username]}
          fetcher={() => lookup === "email" ? api.userByEmail(email) : api.userByUsername(username)}
        >
          {(profile) => (
            <dl className="grid gap-3 sm:grid-cols-2">
              <div><dt className="font-semibold">Nombre</dt><dd>{profile.firstName} {profile.lastName}</dd></div>
              <div><dt className="font-semibold">Fecha de nacimiento</dt><dd>{profile.birthDate.split("-").reverse().join("/")}</dd></div>
              <div><dt className="font-semibold">Estado</dt><dd>{profile.active ? "Activa" : "Inactiva"}</dd></div>
              <div><dt className="font-semibold">Ciudad</dt><dd>{profile.city || "Sin registrar"}</dd></div>
            </dl>
          )}
        </Remote>
        <div className="mt-6 flex flex-wrap items-center gap-4">
          <button type="button" className="text-sm underline" onClick={() => setLookup(lookup === "email" ? "username" : "email")}>
            {lookup === "email" ? "Consultar por usuario" : "Consultar por correo"}
          </button>
          {hasRole(session.user, ["USER"]) && (
            <Link className="text-marca-600 dark:text-indigo-300 font-semibold" to={RUTAS.tickets}>Ver mis tickets</Link>
          )}
          <Boton onClick={logout}>Cerrar sesión</Boton>
        </div>
      </Panel>
    </section>
  );
}
