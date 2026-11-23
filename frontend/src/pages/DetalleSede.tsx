import { Link, useParams } from "react-router";
import { api } from "../lib/api";
import { Badge, Heading, Panel } from "../componentes/UI";
import { RUTAS } from "../lib/rutas";
import { Remote } from "./shared";
import NoEncontrada from "./NoEncontrada";
export default function DetalleSede() {
  const { code = "" } = useParams();
  return (
    <Remote keyName={["venue", code]} fetcher={() => api.venue(code)} notFound={<NoEncontrada />}>
      {(v) => (
        <>
          <Link className="inline-block mb-5 underline" to={RUTAS.sedes}>Volver a sedes</Link>
          <Heading title={v.name} subtitle={v.city} />
          <Panel>
            <Badge status={v.active ? "Activa" : "Inactiva"} />
            <p>
              <strong>Código:</strong> {v.code}
            </p>
            <p>
              <strong>Dirección:</strong> {v.address || "Por confirmar"}
            </p>
            <p>
              <strong>Capacidad:</strong> {v.capacity}
            </p>
          </Panel>
        </>
      )}
    </Remote>
  );
}
