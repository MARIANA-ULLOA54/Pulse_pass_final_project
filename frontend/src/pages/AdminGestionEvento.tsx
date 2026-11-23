import { useState } from "react";
import { useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { Badge, Boton, Heading, Panel } from "../componentes/UI";
import { Remote, Alert, field } from "./shared";
export default function AdminGestionEvento() {
  const { eventCode = "" } = useParams();
  const [artistId, setArtistId] = useState("");
  const client = useQueryClient();
  const refresh = () =>
    void client.invalidateQueries({ queryKey: ["event", eventCode] });
  const publish = useMutation({
    mutationFn: () => api.publish(eventCode),
    onSuccess: refresh,
  });
  const associate = useMutation({
    mutationFn: () => api.addArtist(eventCode, Number(artistId)),
    onSuccess: refresh,
  });
  return (
    <Remote keyName={["event", eventCode]} fetcher={() => api.event(eventCode)}>
      {(event) => (
        <>
          <Heading title={event.name} subtitle={event.eventCode} />
          <Panel className="max-w-3xl space-y-5">
            <Badge status={event.status} />
            <p>{event.description}</p>
            <p>Sede: {event.venueName}</p>
            <p>
              Artistas:{" "}
              {event.artists.map((a) => a.stageName).join(", ") || "Ninguno"}
            </p>
            <Alert error={publish.error} />
            <Boton
              disabled={event.status !== "DRAFT"}
              cargando={publish.isPending}
              onClick={() => {
                if (window.confirm("¿Publicar este evento?")) publish.mutate();
              }}
            >
              Publicar evento
            </Boton>
            <div className="border-t pt-5 dark:border-slate-700">
              <h2 className="font-bold mb-3">Asociar artista</h2>
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
                      {artists.map((a) => (
                        <option value={a.id} key={a.id}>
                          {a.stageName}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
              </Remote>
              <Alert error={associate.error} />
              <Boton
                className="mt-3"
                disabled={!artistId}
                cargando={associate.isPending}
                onClick={() => associate.mutate()}
              >
                Asociar artista
              </Boton>
            </div>
          </Panel>
        </>
      )}
    </Remote>
  );
}
