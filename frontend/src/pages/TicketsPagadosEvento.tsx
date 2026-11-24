import { useParams } from "react-router";
import { api } from "../lib/api";
import { Heading, Panel, EstadoVacio, Badge } from "../componentes/UI";
import { Remote } from "./shared";
export default function TicketsPagadosEvento() {
  const { eventCode = "" } = useParams();
  return (
    <>
      <Heading title="Tickets pagados" subtitle={`Evento: ${eventCode}`} />
      <Remote keyName={["paid", eventCode]} fetcher={() => api.paid(eventCode)}>
        {(tickets) =>
          tickets.length ? (
            <div className="space-y-3">
              {tickets.map((t) => (
                <Panel key={t.id}>
                  <div className="flex flex-wrap justify-between gap-3">
                    <span className="font-mono">{t.ticketCode}</span>
                    <span>{t.userEmail}</span>
                    <span>{t.type}</span>
                    <Badge status={t.status} />
                  </div>
                </Panel>
              ))}
            </div>
          ) : (
            <EstadoVacio mensaje="No hay tickets pagados para este evento" />
          )
        }
      </Remote>
    </>
  );
}
