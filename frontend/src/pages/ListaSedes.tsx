import { Link } from "react-router";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { Heading, Panel, EstadoVacio } from "../componentes/UI";
import { Remote } from "./shared";
export default function ListaSedes() {
  return (
    <>
      <Heading
        title="Sedes"
        subtitle="Espacios donde suceden los mejores eventos"
      />
      <Remote keyName={["venues"]} fetcher={api.venues}>
        {(venues) =>
          venues.length ? (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {venues.map((v) => (
                <Link key={v.code} to={RUTAS.sede(v.code)}>
                  <Panel>
                    <h2 className="font-bold text-xl">{v.name}</h2>
                    <p>{v.city}</p>
                    <p className="text-slate-500">Capacidad: {v.capacity}</p>
                  </Panel>
                </Link>
              ))}
            </div>
          ) : (
            <EstadoVacio mensaje="No hay sedes activas" />
          )
        }
      </Remote>
    </>
  );
}
