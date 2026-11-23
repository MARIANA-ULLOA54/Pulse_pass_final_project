import { Link, useNavigate } from "react-router";
import { useMutation } from "@tanstack/react-query";
import type { FormEvent } from "react";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { AvatarArtista, Boton, Heading, Panel, EstadoVacio } from "../componentes/UI";
import { ApiError } from "../lib/http";
import { Alert, Remote, field } from "./shared";
export default function ListaArtistas() {
  const navigate = useNavigate();
  const buscar = useMutation({
    mutationFn: api.artistByName,
    onSuccess: (artista) => navigate(RUTAS.artista(artista.id)),
  });
  function buscarArtista(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const nombre = String(new FormData(e.currentTarget).get("stageName") ?? "").trim();
    if (nombre && !buscar.isPending) buscar.mutate(nombre);
  }

  return (
    <>
      <Heading title="Artistas" subtitle="Conoce a los protagonistas" />
      <form className="flex flex-wrap items-end gap-3 mb-4" onSubmit={buscarArtista}>
        <label className="flex-1 max-w-md">
          Buscar por nombre artístico
          <input name="stageName" type="search" className={field} required onChange={() => buscar.reset()} disabled={buscar.isPending} />
        </label>
        <Boton type="submit" cargando={buscar.isPending}>Buscar</Boton>
      </form>
      <Alert error={buscar.error instanceof ApiError && buscar.error.status === 404 ? "No encontramos ese artista." : buscar.error} />
      <Remote keyName={["artists"]} fetcher={api.artists}>
        {(data) =>
          data.length ? (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {data.map((a) => (
                <Link key={a.id} to={RUTAS.artista(a.id)}>
                  <Panel>
                    <AvatarArtista nombre={a.stageName} />
                    <h2 className="font-bold text-xl mt-3">{a.stageName}</h2>
                    <p className="text-slate-500">
                      {[a.genre, a.country].filter(Boolean).join(" · ")}
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
