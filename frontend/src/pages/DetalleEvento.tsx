import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { api } from "../lib/api";
import { ticketType } from "../lib/schemas";
import { useAuth, hasRole } from "../context/AuthContext";
import { RUTAS } from "../lib/rutas";
import { Badge, Boton, Heading, Panel, fecha } from "../componentes/UI";
import { Remote, Alert, field } from "./shared";
export default function DetalleEvento() {
  const { eventCode = "" } = useParams();
  const navigate = useNavigate();
  const { session } = useAuth();
  const [type, setType] =
    useState<(typeof ticketType.options)[number]>("GENERAL");
  const [open, setOpen] = useState(false);
  const buy = useMutation({
    mutationFn: api.buy,
    onSuccess: (t) => navigate(RUTAS.confirmacion(t.ticketCode)),
  });
  return (
    <Remote keyName={["event", eventCode]} fetcher={() => api.event(eventCode)}>
      {(e) => (
        <>
          <Heading title={e.name} subtitle={fecha(e.eventDate)} />
          <div className="grid md:grid-cols-3 gap-6">
            <Panel className="md:col-span-2">
              <Badge status={e.status} />
              <p className="my-5">{e.description || "Evento en PulsePass"}</p>
              <p>
                <strong>Categoría:</strong> {e.category}
              </p>
              <p>
                <strong>Sede:</strong> {e.venueName}
              </p>
              <p>
                <strong>Edad mínima:</strong> {e.minimumAge}
              </p>
              <p>
                <strong>Artistas:</strong>{" "}
                {e.artists.map((a) => a.stageName).join(", ") || "Por anunciar"}
              </p>
            </Panel>
            <Panel>
              <h2 className="font-bold text-xl mb-4">Entradas</h2>
              <p className="text-slate-500 mb-4">
                El precio se confirma después de la compra.
              </p>
              <Boton
                disabled={
                  e.status !== "PUBLISHED" ||
                  (session.status === "authenticated" &&
                    !hasRole(session.user, ["USER"]))
                }
                onClick={() =>
                  session.status === "authenticated"
                    ? setOpen(true)
                    : navigate(RUTAS.login, {
                        state: { desde: RUTAS.evento(e.eventCode) },
                      })
                }
              >
                Comprar entrada
              </Boton>
            </Panel>
          </div>
          {open && (
            <div
              className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
              role="presentation"
              onMouseDown={(event) => {
                if (event.target === event.currentTarget) setOpen(false);
              }}
            >
              <div
                role="dialog"
                aria-modal="true"
                aria-label="Comprar entrada"
                className="bg-white dark:bg-slate-900 rounded-xl p-6 max-w-md w-full"
              >
                <h2 className="text-xl font-bold mb-4">Confirmar compra</h2>
                <label>
                  Tipo de entrada
                  <select
                    className={field}
                    value={type}
                    onChange={(ev) => setType(ev.target.value as typeof type)}
                  >
                    {ticketType.options.map((t) => (
                      <option key={t}>{t}</option>
                    ))}
                  </select>
                </label>
                <Alert error={buy.error} />
                <div className="flex gap-2 mt-5">
                  <Boton
                    cargando={buy.isPending}
                    onClick={() => {
                      if (session.status === "authenticated")
                        buy.mutate({
                          userEmail: session.user.email,
                          eventCode: e.eventCode,
                          type,
                        });
                    }}
                  >
                    Confirmar
                  </Boton>
                  <Boton variante="secundario" onClick={() => setOpen(false)}>
                    Cerrar
                  </Boton>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </Remote>
  );
}
