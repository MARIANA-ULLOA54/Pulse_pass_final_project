import { Link } from "react-router";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { EventCard, Heading, EstadoVacio } from "../componentes/UI";
import { Remote } from "./shared";
import { useAuth } from "../context/AuthContext";
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
        <Heading title="Próximos eventos" />
        <Link to={RUTAS.eventos}>Ver todos →</Link>
      </div>
      <Remote keyName={["events"]} fetcher={api.events}>
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
        <Heading title="Artistas destacados" />
        <Link to={RUTAS.artistas}>Ver todos →</Link>
      </div>
      <Remote keyName={["artists"]} fetcher={api.artists}>
        {(artists) =>
          artists.length ? (
            <div className="flex flex-wrap gap-3">
              {artists.slice(0, 6).map((a) => (
                <Link
                  className="rounded-full bg-indigo-100 text-indigo-800 px-4 py-2"
                  key={a.id}
                  to={RUTAS.artista(a.id)}
                >
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
