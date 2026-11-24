import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { Badge, Heading, Boton, Panel } from "../componentes/UI";
import { leerCodigosDeEventosCreados } from "../lib/adminEventos";
import { Remote, field } from "./shared";

function EventoReciente({ code }: { code: string }) {
  return (
    <Remote
      keyName={["admin-event", code]}
      fetcher={() => api.event(code)}
      notFound={<Link className="block py-3 font-mono" to={RUTAS.gestionar(code)}>{code}</Link>}
    >
      {(event) => (
        <Link className="flex flex-wrap items-center justify-between gap-3 py-3" to={RUTAS.gestionar(event.eventCode)}>
          <span>
            <span className="block font-semibold">{event.name}</span>
            <span className="font-mono text-sm text-slate-500">{event.eventCode}</span>
          </span>
          <Badge status={event.status} />
        </Link>
      )}
    </Remote>
  );
}

export default function AdminEventos() {
  const [code, setCode] = useState("");
  const [saved] = useState(leerCodigosDeEventosCreados);
  const navigate = useNavigate();
  return (
    <>
      <Heading
        title="Administrar eventos"
        subtitle="Crea y gestiona eventos por código"
      />
      <Panel className="max-w-2xl mb-6">
        <Link
          className="inline-block rounded-lg bg-marca-600 text-white px-5 py-3 mb-5"
          to={RUTAS.nuevo}
        >
          Crear evento
        </Link>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const normalizedCode = code.trim();
            if (normalizedCode) navigate(RUTAS.gestionar(normalizedCode));
          }}
        >
          <label>
            Código del evento
            <input
              className={field}
              value={code}
              onChange={(e) => setCode(e.target.value)}
              required
            />
          </label>
          <Boton className="mt-3" type="submit">
            Buscar evento
          </Boton>
        </form>
      </Panel>
      {saved.length > 0 && (
        <Panel>
          <h2 className="font-bold mb-3">Eventos creados en esta pestaña</h2>
          {saved.map((savedCode) => (
            <div className="border-t border-slate-200 dark:border-slate-700" key={savedCode}>
              <EventoReciente code={savedCode} />
            </div>
          ))}
        </Panel>
      )}
    </>
  );
}
