import { Link } from "react-router";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { Heading, Panel, EstadoVacio } from "../componentes/UI";
import { Remote } from "./shared";
export default function ListaArtistas() {
  return (
    <>
      <Heading title="Artistas" subtitle="Conoce a los protagonistas" />
      <Remote keyName={["artists"]} fetcher={api.artists}>
        {(data) =>
          data.length ? (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {data.map((a) => (
                <Link key={a.id} to={RUTAS.artista(a.id)}>
                  <Panel>
                    <div className="w-14 h-14 rounded-full bg-indigo-100 text-marca-700 flex items-center justify-center font-bold text-xl mb-4">
                      {a.stageName.slice(0, 2).toUpperCase()}
                    </div>
                    <h2 className="font-bold text-xl">{a.stageName}</h2>
                    <p className="text-slate-500">
                      {a.genre} · {a.country}
                    </p>
                  </Panel>
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
