import { Link, useParams } from "react-router";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { Heading, Panel, precio, Badge } from "../componentes/UI";
import { Remote } from "./shared";
export default function ConfirmacionCompra() {
  const { ticketCode = "" } = useParams();
  return (
    <Remote
      keyName={["ticket", ticketCode]}
      fetcher={() => api.ticket(ticketCode)}
    >
      {(t) => (
        <>
          <Heading
            title="¡Compra confirmada!"
            subtitle="Tu entrada está registrada"
          />
          <Panel className="max-w-xl space-y-3">
            <h2 className="font-bold text-xl">{t.eventName}</h2>
            <p className="font-mono">{t.ticketCode}</p>
            <Badge status={t.status} />
            <p>Tipo: {t.type}</p>
            <p>Precio: {precio(t.price)}</p>
            <Link className="block text-marca-600" to={RUTAS.tickets}>
              Ver mis tickets →
            </Link>
          </Panel>
        </>
      )}
    </Remote>
  );
}
