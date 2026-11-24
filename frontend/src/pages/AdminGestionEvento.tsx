import { useState } from "react";
import { Link, useLocation, useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { api } from "../lib/api";
import { ApiError } from "../lib/http";
import { Badge, Boton, Heading, Panel, fecha } from "../componentes/UI";
import { RUTAS } from "../lib/rutas";
import { Remote, Alert, field } from "./shared";

function errorOperacion(error: unknown) {
  return error instanceof ApiError && error.status === 409
    ? new Error("No se pudo completar la acción. Revisa el estado del evento o la asociación del artista.")
    : error;
}

export default function AdminGestionEvento() {
  const { eventCode = "" } = useParams();
  const location = useLocation();
  const notice = z.object({ aviso: z.string() }).safeParse(location.state).data?.aviso;
  const [artistId, setArtistId] = useState("");
  const client = useQueryClient();
  const refresh = (event: Awaited<ReturnType<typeof api.event>>) => {
    client.setQueryData(["event", eventCode], event);
    client.setQueryData(["admin-event", eventCode], event);
    void client.invalidateQueries({ queryKey: ["events"] });
    void client.invalidateQueries({ queryKey: ["artist-events"] });
  };
  const publish = useMutation({
    mutationFn: () => api.publish(eventCode),
    onSuccess: refresh,
  });
  const associate = useMutation({
    mutationFn: () => api.addArtist(eventCode, Number(artistId)),
    onSuccess: (event) => {
      setArtistId("");
      refresh(event);
    },
  });
  return (
    <Remote keyName={["event", eventCode]} fetcher={() => api.event(eventCode)}>
      {(event) => (
        <>
          <Link className="mb-5 inline-block underline" to={RUTAS.admin}>Volver a eventos</Link>
          <Heading title={event.name} subtitle={event.eventCode} />
          {notice && <p role="status" className="mb-5 rounded-lg bg-emerald-50 p-4 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-100">{notice}</p>}
          <Panel className="max-w-3xl space-y-5">
            <Badge status={event.status} />
            <dl className="grid gap-3 sm:grid-cols-2">
              <div><dt className="text-sm text-slate-500">Categoría</dt><dd>{event.category}</dd></div>
              <div><dt className="text-sm text-slate-500">Fecha</dt><dd>{fecha(event.eventDate)}</dd></div>
              <div><dt className="text-sm text-slate-500">Edad mínima</dt><dd>{event.minimumAge} años</dd></div>
              <div><dt className="text-sm text-slate-500">Sede</dt><dd>{event.venueName} ({event.venueCode})</dd></div>
            </dl>
            {event.description && <p>{event.description}</p>}
            <Alert error={errorOperacion(publish.error)} />
            <Boton
              disabled={event.status !== "DRAFT"}
              cargando={publish.isPending}
              onClick={() => {
                if (window.confirm("¿Publicar este evento?")) publish.mutate();
              }}
            >
              Publicar evento
            </Boton>
            {publish.isSuccess && <p role="status" className="text-sm text-emerald-700 dark:text-emerald-300">Evento publicado correctamente.</p>}
            <div className="border-t pt-5 dark:border-slate-700">
              <h2 className="font-bold mb-3">Artistas asociados</h2>
              {event.artists.length ? (
                <ul className="mb-4 flex flex-wrap gap-2">
                  {event.artists.map((artist) => (
                    <li className="rounded-full bg-slate-100 px-3 py-2 text-sm dark:bg-slate-800" key={artist.id}>{artist.stageName}</li>
                  ))}
                </ul>
              ) : <p className="mb-4 text-slate-500">Aún no hay artistas asociados.</p>}
              <h3 className="mb-3 font-semibold">Asociar artista activo</h3>
              <Remote keyName={["artists"]} fetcher={api.artists}>
                {(artists) => (
                  <label>
                    Artista
                    <select
                      className={field}
                      value={artistId}
                      onChange={(e) => setArtistId(e.target.value)}
                    >
                      <option value="">Selecciona artista</option>
                      {artists.filter((artist) => !event.artists.some((associated) => associated.id === artist.id)).map((a) => (
                        <option value={a.id} key={a.id}>
                          {a.stageName}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
              </Remote>
              <Alert error={errorOperacion(associate.error)} />
              <Boton
                className="mt-3"
                disabled={!artistId}
                cargando={associate.isPending}
                onClick={() => associate.mutate()}
              >
                Asociar artista
              </Boton>
              {associate.isSuccess && <p role="status" className="mt-3 text-sm text-emerald-700 dark:text-emerald-300">Artista asociado correctamente.</p>}
            </div>
          </Panel>
        </>
      )}
    </Remote>
  );
}
