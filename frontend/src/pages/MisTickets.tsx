import { useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { Heading, TicketCard, EstadoVacio } from "../componentes/UI";
import { ticketStatus, type TicketStatus } from "../lib/schemas";
import { Remote } from "./shared";

const estados: Record<TicketStatus, string> = {
  RESERVED: "Reservados",
  PAID: "Pagados",
  CANCELLED: "Cancelados",
  USED: "Usados",
};
const filtros = ["TODOS", ...ticketStatus.options] as const;

export default function MisTickets() {
  const { session } = useAuth();
  const [filter, setFilter] = useState<TicketStatus | "TODOS">("TODOS");
  if (session.status !== "authenticated") return null;
  return (
    <>
      <Heading
        title="Mis tickets"
        subtitle="Consulta y administra tus entradas"
      />
      <div aria-label="Filtrar tickets por estado" className="flex flex-wrap gap-2 mb-6" role="group">
        {filtros.map((s) => (
          <button
            key={s}
            aria-pressed={filter === s}
            onClick={() => setFilter(s)}
            className={`rounded-full px-4 py-2 ${filter === s ? "bg-marca-600 text-white" : "bg-slate-200 dark:bg-slate-800"}`}
          >
            {s === "TODOS" ? "Todos" : estados[s]}
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
            <EstadoVacio
              mensaje={filter === "TODOS"
                ? "Aún no tienes tickets. Explora la cartelera para encontrar tu próximo evento."
                : `No tienes tickets ${estados[filter].toLocaleLowerCase("es-CO")}.`}
            />
          );
        }}
      </Remote>
    </>
  );
}
