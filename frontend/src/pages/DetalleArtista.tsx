import { useParams } from "react-router";
import { api } from "../lib/api";
import { Heading, EventCard, EstadoVacio, Panel } from "../componentes/UI";
import { Remote } from "./shared";
export default function DetalleArtista() {
  const { id = "" } = useParams();
  return (
    <Remote keyName={["artist", id]} fetcher={() => api.artist(id)}>
      {(a) => (
        <>
          <Heading
            title={a.stageName}
            subtitle={`${a.genre ?? ""} · ${a.country ?? ""}`}
          />
          <Panel className="mb-8">
            Artista {a.active ? "activo" : "inactivo"} en PulsePass.
          </Panel>
          <h2 className="text-xl font-bold mb-5">Eventos de {a.stageName}</h2>
          <Remote
            keyName={["artist-events", a.stageName]}
            fetcher={() => api.byArtist(a.stageName)}
          >
            {(events) =>
              events.length ? (
                <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
                  {events.map((e) => (
                    <EventCard key={e.id} event={e} />
                  ))}
                </div>
              ) : (
                <EstadoVacio mensaje="No hay eventos publicados para este artista" />
              )
            }
          </Remote>
        </>
      )}
    </Remote>
  );
}
