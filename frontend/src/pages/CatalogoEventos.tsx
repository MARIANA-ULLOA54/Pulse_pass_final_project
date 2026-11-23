import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router";
import { api } from "../lib/api";
import { category } from "../lib/schemas";
import { Boton, EventCard, EstadoVacio, Heading } from "../componentes/UI";
import { Remote, field } from "./shared";

const filtros = ["q", "categoria", "artista"] as const;

export default function CatalogoEventos() {
  const [params, setParams] = useSearchParams();
  const q = params.get("q") ?? "";
  const cat = params.get("categoria") ?? "";
  const artista = params.get("artista") ?? "";
  const artistas = useQuery({ queryKey: ["artists"], queryFn: api.artists });
  const opciones = artistas.data?.map((a) => a.stageName) ?? [];

  function actualizar(filtro: typeof filtros[number], valor: string) {
    const next = new URLSearchParams(params);
    if (valor) next.set(filtro, valor);
    else next.delete(filtro);
    setParams(next, { replace: filtro === "q" });
  }

  function limpiar() {
    const next = new URLSearchParams(params);
    filtros.forEach((filtro) => next.delete(filtro));
    setParams(next);
  }

  return (
    <>
      <Heading title="Cartelera de eventos" />
      <div className="grid gap-4 mb-7 sm:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr_auto] lg:items-end">
        <label>
          Buscar evento
          <input
            type="search"
            className={field}
            value={q}
            onChange={(e) => actualizar("q", e.target.value)}
            placeholder="Buscar por nombre"
          />
        </label>
        <label>
          Categoría
          <select className={field} value={cat} onChange={(e) => actualizar("categoria", e.target.value)}>
            <option value="">Todas</option>
            {cat && !category.safeParse(cat).success && <option>{cat}</option>}
            {category.options.map((c) => <option key={c}>{c}</option>)}
          </select>
        </label>
        <label>
          Artista
          <select className={field} value={artista} onChange={(e) => actualizar("artista", e.target.value)}>
            <option value="">Todos</option>
            {artista && !opciones.includes(artista) && <option>{artista}</option>}
            {opciones.map((nombre) => <option key={nombre}>{nombre}</option>)}
          </select>
        </label>
        <Boton variante="secundario" disabled={!q && !cat && !artista} onClick={limpiar}>
          Limpiar
        </Boton>
      </div>
      {artistas.isError && (
        <p role="status" className="mb-4">
          No se pudo cargar el filtro de artistas.{" "}
          <button type="button" className="underline" onClick={() => void artistas.refetch()}>
            Reintentar
          </button>
        </p>
      )}
      <Remote
        keyName={artista ? ["artist-events", artista] : ["events"]}
        fetcher={artista ? () => api.byArtist(artista) : api.events}
      >
        {(events) => {
          const texto = q.trim().toLocaleLowerCase("es-CO");
          const filtered = events.filter(
            (e) => e.name.toLocaleLowerCase("es-CO").includes(texto) && (!cat || e.category === cat),
          );
          return (
            <>
              <p role="status" className="mb-4">
                {filtered.length} {filtered.length === 1 ? "evento" : "eventos"}
              </p>
              {filtered.length ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                  {filtered.map((e) => <EventCard key={e.id} event={e} />)}
                </div>
              ) : (
                <EstadoVacio mensaje={q || cat || artista ? "Sin resultados para tu búsqueda" : "Aún no hay eventos publicados"} />
              )}
            </>
          );
        }}
      </Remote>
    </>
  );
}
