import { Link } from "react-router";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { AvatarArtista, EventCard, EstadoVacio } from "../componentes/UI";
import { Remote } from "./shared";
import { useAuth } from "../context/AuthContext";

function Esqueletos({ cantidad, className, itemClassName }: {
  cantidad: number;
  className: string;
  itemClassName: string;
}) {
  return (
    <div role="status" className={className}>
      <span className="sr-only">Cargando información…</span>
      {Array.from({ length: cantidad }, (_, index) => (
        <div key={index} aria-hidden="true" className={itemClassName + " bg-slate-200 dark:bg-slate-800 motion-safe:animate-pulse"} />
      ))}
    </div>
  );
}

export default function Inicio() {
  const { session } = useAuth();
  return (
    <>
      <section className="bg-linear-to-r from-marca-600 to-acento-500 text-white rounded-xl px-8 py-14 md:px-12 mb-10">
        <h1 className="font-titulo font-extrabold text-4xl md:text-5xl max-w-lg">
          Descubre los mejores eventos
        </h1>
        <p className="my-4">
          Música, cultura, deporte y tecnología en un solo lugar.
        </p>
        <div className="flex flex-wrap gap-3">
          <Link
            className="bg-white text-marca-700 rounded-lg px-5 py-3 font-semibold"
            to={RUTAS.eventos}
          >
            Ver cartelera
          </Link>
          {session.status === "anonymous" && (
            <Link
              className="border border-white rounded-lg px-5 py-3"
              to={RUTAS.registro}
            >
              Crear cuenta
            </Link>
          )}
        </div>
      </section>
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold mb-5">Próximos eventos</h2>
        <Link to={RUTAS.eventos}>Ver todos →</Link>
      </div>
      <Remote
        keyName={["events"]}
        fetcher={api.events}
        loading={<Esqueletos cantidad={3} className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5" itemClassName="h-64 rounded-xl" />}
      >
        {(events) =>
          events.length ? (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {events.slice(0, 3).map((e) => (
                <EventCard key={e.id} event={e} />
              ))}
            </div>
          ) : (
            <EstadoVacio mensaje="Aún no hay eventos publicados" />
          )
        }
      </Remote>
      <div className="flex justify-between items-center mt-10">
        <h2 className="text-2xl font-bold mb-5">Artistas destacados</h2>
        <Link to={RUTAS.artistas}>Ver todos →</Link>
      </div>
      <Remote
        keyName={["artists"]}
        fetcher={api.artists}
        loading={<Esqueletos cantidad={6} className="flex flex-wrap gap-3" itemClassName="h-14 w-36 rounded-full" />}
      >
        {(artists) =>
          artists.length ? (
            <div className="flex flex-wrap gap-3">
              {artists.slice(0, 6).map((a) => (
                <Link
                  className="flex items-center gap-2 rounded-full border border-slate-300 p-1 pr-4"
                  key={a.id}
                  to={RUTAS.artista(a.id)}
                >
                  <AvatarArtista nombre={a.stageName} />
                  {a.stageName}
                </Link>
              ))}
            </div>
          ) : (
            <EstadoVacio mensaje="No hay artistas activos" />
          )
        }
      </Remote>
    </>
  );
}
