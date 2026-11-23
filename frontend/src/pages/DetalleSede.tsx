import { useParams } from "react-router";
import { api } from "../lib/api";
import { Heading, Panel } from "../componentes/UI";
import { Remote } from "./shared";
export default function DetalleSede() {
  const { code = "" } = useParams();
  return (
    <Remote keyName={["venue", code]} fetcher={() => api.venue(code)}>
      {(v) => (
        <>
          <Heading title={v.name} subtitle={v.city} />
          <Panel>
            <p>
              <strong>Código:</strong> {v.code}
            </p>
            <p>
              <strong>Dirección:</strong> {v.address}
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
