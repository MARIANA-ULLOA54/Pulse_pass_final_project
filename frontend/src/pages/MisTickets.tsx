import { useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { Heading, TicketCard, EstadoVacio } from "../componentes/UI";
import { Remote } from "./shared";
export default function MisTickets() {
  const { session } = useAuth();
  const [filter, setFilter] = useState("TODOS");
  if (session.status !== "authenticated") return null;
  return (
    <>
      <Heading
        title="Mis tickets"
        subtitle="Consulta y administra tus entradas"
      />
      <div className="flex flex-wrap gap-2 mb-6">
        {["TODOS", "PAID", "USED", "CANCELLED"].map((s) => (
          <button
            key={s}
            aria-pressed={filter === s}
            onClick={() => setFilter(s)}
            className={`rounded-full px-4 py-2 ${filter === s ? "bg-marca-600 text-white" : "bg-slate-200 dark:bg-slate-800"}`}
          >
            {s === "TODOS"
              ? "Todos"
              : s === "PAID"
                ? "Pagados"
                : s === "USED"
                  ? "Usados"
                  : "Cancelados"}
          </button>
        ))}
      </div>
      <Remote
        keyName={["tickets", session.user.email]}
        fetcher={() => api.tickets(session.user.email)}
      >
        {(data) => {
          const filtered = data.filter(
            (t) => filter === "TODOS" || t.status === filter,
          );
          return filtered.length ? (
            <div className="grid md:grid-cols-2 gap-4">
              {filtered.map((t) => (
                <TicketCard key={t.id} ticket={t} />
              ))}
            </div>
          ) : (
            <EstadoVacio mensaje="No tienes tickets en esta categoría" />
          );
        }}
      </Remote>
    </>
  );
}
