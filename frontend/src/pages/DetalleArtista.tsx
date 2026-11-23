import { Link, useParams } from "react-router";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { Heading, EventCard, EstadoVacio, AvatarArtista } from "../componentes/UI";
import { Remote } from "./shared";
import NoEncontrada from "./NoEncontrada";
export default function DetalleArtista() {
  const { id = "" } = useParams();
  const artistId = Number(id);
  if (!/^\d+$/.test(id) || !Number.isSafeInteger(artistId) || artistId < 1)
    return <NoEncontrada />;
  return (
    <Remote keyName={["artist", artistId]} fetcher={() => api.artist(String(artistId))} notFound={<NoEncontrada />}>
      {(a) => (
        <>
          <Link className="inline-block mb-5 underline" to={RUTAS.artistas}>Volver a artistas</Link>
          <div className="flex items-start gap-4">
            <AvatarArtista nombre={a.stageName} />
            <Heading title={a.stageName} subtitle={[a.genre, a.country].filter(Boolean).join(" · ")} />
          </div>
          <h2 className="text-xl font-bold mb-5">Próximos eventos de {a.stageName}</h2>
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
                <EstadoVacio mensaje="Sin eventos próximos" />
              )
            }
          </Remote>
        </>
      )}
    </Remote>
  );
}
