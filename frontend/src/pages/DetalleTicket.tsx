import { useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "../lib/api";
import { Badge, Boton, Heading, Panel, fecha, precio } from "../componentes/UI";
import { Remote, Alert } from "./shared";
export default function DetalleTicket() {
  const { ticketCode = "" } = useParams();
  const client = useQueryClient();
  const [confirm, setConfirm] = useState(false);
  const cancel = useMutation({
    mutationFn: () => api.cancel(ticketCode),
    onSuccess: () => {
      setConfirm(false);
      void client.invalidateQueries({ queryKey: ["ticket", ticketCode] });
      void client.invalidateQueries({ queryKey: ["tickets"] });
    },
  });
  return (
    <Remote
      keyName={["ticket", ticketCode]}
      fetcher={() => api.ticket(ticketCode)}
    >
      {(t) => (
        <>
          <Heading title={t.eventName} subtitle="Detalle de entrada" />
          <Panel className="max-w-xl space-y-3">
            <p className="font-mono">{t.ticketCode}</p>
            <Badge status={t.status} />
            <p>Tipo: {t.type}</p>
            <p>Precio: {precio(t.price)}</p>
            <p>Compra: {fecha(t.purchaseDate)}</p>
            {t.status === "PAID" && (
              <Boton variante="peligro" onClick={() => setConfirm(true)}>
                Cancelar ticket
              </Boton>
            )}
            {confirm && (
              <div
                role="dialog"
                aria-modal="true"
                aria-label="Confirmar cancelación"
                className="border rounded-lg p-4"
              >
                <p>¿Seguro que deseas cancelar este ticket?</p>
                <Alert error={cancel.error} />
                <div className="flex gap-2 mt-3">
                  <Boton
                    variante="peligro"
                    cargando={cancel.isPending}
                    onClick={() => cancel.mutate()}
                  >
                    Sí, cancelar
                  </Boton>
                  <Boton
                    variante="secundario"
                    onClick={() => setConfirm(false)}
                  >
                    Volver
                  </Boton>
                </div>
              </div>
            )}
          </Panel>
        </>
      )}
    </Remote>
  );
}
