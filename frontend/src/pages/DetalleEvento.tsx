import { useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { ApiError } from "../lib/http";
import { ticketType, type EventDetail, type TicketType } from "../lib/schemas";
import { useAuth, hasRole } from "../context/AuthContext";
import { RUTAS } from "../lib/rutas";
import { AvatarArtista, Badge, Boton, EventBanner, Heading, Panel, fecha } from "../componentes/UI";
import { Remote, Alert } from "./shared";

function CompraEvento({ event }: { event: EventDetail }) {
  const navigate = useNavigate();
  const client = useQueryClient();
  const { session } = useAuth();
  const [type, setType] = useState<TicketType>("GENERAL");
  const dialog = useRef<HTMLDialogElement>(null);
  const desde = RUTAS.evento(event.eventCode);
  const buy = useMutation({
    mutationFn: api.buy,
    onSuccess: (ticket, { userEmail }) => {
      void client.invalidateQueries({ queryKey: ["tickets", userEmail] });
      void client.invalidateQueries({ queryKey: ["event", event.eventCode] });
      void client.invalidateQueries({ queryKey: ["events"] });
      void client.invalidateQueries({ queryKey: ["artist-events"] });
      navigate(RUTAS.confirmacion(ticket.ticketCode));
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401)
        navigate(RUTAS.login, { state: { desde } });
    },
  });
  if (session.status === "authenticated" && (!hasRole(session.user, ["USER"]) || hasRole(session.user, ["STAFF", "ADMIN"])))
    return null;

  const available = event.status === "PUBLISHED";
  const email = session.status === "authenticated" ? session.user.email : null;

  function iniciarCompra() {
    if (session.status === "anonymous") {
      navigate(RUTAS.login, { state: { desde } });
      return;
    }
    buy.reset();
    dialog.current?.showModal();
  }

  return (
    <Panel>
      <h2 className="font-bold text-xl mb-4">Entradas</h2>
      <fieldset className="grid gap-2 mb-4" disabled={!available}>
        <legend className="font-semibold mb-2">Tipo de entrada</legend>
        {ticketType.options.map((option) => (
          <label key={option} className="flex items-center gap-2">
            <input type="radio" name="tipoEntrada" value={option} checked={type === option} onChange={() => setType(option)} />
            {option}
          </label>
        ))}
      </fieldset>
      <p className="text-slate-500 mb-4">
        {available
          ? "El precio se confirma al realizar la compra."
          : event.status === "SOLD_OUT"
            ? "Entradas agotadas."
            : "La compra no está disponible para este evento."}
      </p>
      <div className={available ? "max-md:fixed max-md:inset-x-0 max-md:bottom-0 max-md:z-20 max-md:bg-white max-md:dark:bg-slate-900 max-md:p-3 max-md:shadow-lg" : undefined}>
        <Boton className="w-full" disabled={!available || session.status === "loading"} onClick={iniciarCompra}>
          {available && session.status === "anonymous" ? "Inicia sesión para comprar" : "Comprar entrada"}
        </Boton>
      </div>
      <dialog
        ref={dialog}
        aria-labelledby="titulo-compra"
        className="m-auto w-[calc(100%-2rem)] max-w-md rounded-xl bg-white p-6 text-slate-900 backdrop:bg-black/60 dark:bg-slate-900 dark:text-white"
        onCancel={(e) => { if (buy.isPending) e.preventDefault(); }}
      >
        <h2 id="titulo-compra" className="text-xl font-bold mb-4">Confirmar compra</h2>
        <p><strong>Evento:</strong> {event.name}</p>
        <p><strong>Tipo:</strong> {type}</p>
        <p><strong>Cuenta:</strong> {email}</p>
        <Alert error={buy.error} />
        <div className="flex flex-wrap gap-2 mt-5">
          <Boton variante="secundario" disabled={buy.isPending} onClick={() => dialog.current?.close()}>
            Cancelar
          </Boton>
          <Boton
            cargando={buy.isPending}
            disabled={!email || !available}
            onClick={() => {
              const tipo = ticketType.safeParse(type);
              if (email && available && tipo.success && !buy.isPending)
                buy.mutate({ userEmail: email, eventCode: event.eventCode, type: tipo.data });
            }}
          >
            Confirmar
          </Boton>
        </div>
      </dialog>
    </Panel>
  );
}

export default function DetalleEvento() {
  const { eventCode = "" } = useParams();
  return (
    <>
      <Link className="inline-block mb-5 underline" to={RUTAS.eventos}>Volver a la cartelera</Link>
      <Remote
        keyName={["event", eventCode]}
        fetcher={() => api.event(eventCode)}
        notFound={<Heading title="Evento no disponible" />}
      >
        {(event) => (
          <div className="grid items-start gap-6 pb-16 md:grid-cols-3 md:pb-0">
            <div className="space-y-5 md:col-span-2">
              <Panel>
                <div className="mb-4 overflow-hidden rounded-lg"><EventBanner category={event.category} /></div>
                <div className="mb-3 flex items-center gap-3">
                  <span className="font-semibold">{event.category}</span>
                  <Badge status={event.status} />
                </div>
                <Heading title={event.name} subtitle={fecha(event.eventDate)} />
                <p><strong>Edad mínima:</strong> {event.minimumAge}</p>
                {event.description && <p className="mt-4">{event.description}</p>}
              </Panel>
              <Panel>
                <h2 className="text-xl font-bold mb-3">Artistas</h2>
                {event.artists.length ? (
                  <ul className="flex flex-wrap gap-4">
                    {event.artists.map((artist) => (
                      <li key={artist.id}>
                        <Link className="flex items-center gap-2 underline" to={RUTAS.artista(artist.id)}>
                          <AvatarArtista nombre={artist.stageName} />
                          {artist.stageName}
                        </Link>
                      </li>
                    ))}
                  </ul>
                ) : <p>Por anunciar</p>}
              </Panel>
              <Panel>
                <h2 className="text-xl font-bold mb-3">Sede</h2>
                <Link className="underline" to={RUTAS.sede(event.venueCode)}>{event.venueName}</Link>
                <Remote keyName={["venue", event.venueCode]} fetcher={() => api.venue(event.venueCode)} notFound={null}>
                  {(venue) => <p className="mt-2">{venue.city} · Capacidad: {venue.capacity}</p>}
                </Remote>
              </Panel>
            </div>
            <CompraEvento key={event.eventCode} event={event} />
          </div>
        )}
      </Remote>
    </>
  );
}
