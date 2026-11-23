import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { RUTAS } from "../lib/rutas";
import { Heading, Boton, Panel } from "../componentes/UI";
import { field } from "./shared";
export default function AdminEventos() {
  const [code, setCode] = useState("");
  const navigate = useNavigate();
  const saved = (() => {
    try {
      return JSON.parse(
        sessionStorage.getItem("pulsepass_admin_codes") ?? "[]",
      ) as string[];
    } catch {
      return [] as string[];
    }
  })();
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
            navigate(RUTAS.gestionar(code));
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
          {saved.map((code) => (
            <Link
              className="block text-marca-600 py-2"
              key={code}
              to={RUTAS.gestionar(code)}
            >
              {code}
            </Link>
          ))}
        </Panel>
      )}
    </>
  );
}
