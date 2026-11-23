import { useSearchParams } from "react-router";
import { api } from "../lib/api";
import { category } from "../lib/schemas";
import { EventCard, EstadoVacio, Heading } from "../componentes/UI";
import { Remote, field } from "./shared";
export default function CatalogoEventos() {
  const [params, setParams] = useSearchParams();
  const q = params.get("q") ?? "";
  const cat = params.get("categoria") ?? "";
  return (
    <>
      <Heading
        title="Cartelera de eventos"
        subtitle="Encuentra tu próximo plan"
      />
      <div className="grid md:grid-cols-2 gap-4 mb-7">
        <label>
          Buscar evento
          <input
            className={field}
            value={q}
            onChange={(e) =>
              setParams((p) => {
                const n = new URLSearchParams(p);
                e.target.value ? n.set("q", e.target.value) : n.delete("q");
                return n;
              })
            }
            placeholder="Buscar por nombre"
          />
        </label>
        <label>
          Categoría
          <select
            className={field}
            value={cat}
            onChange={(e) =>
              setParams((p) => {
                const n = new URLSearchParams(p);
                e.target.value
                  ? n.set("categoria", e.target.value)
                  : n.delete("categoria");
                return n;
              })
            }
          >
            <option value="">Todas</option>
            {category.options.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </label>
      </div>
      <Remote keyName={["events"]} fetcher={api.events}>
        {(events) => {
          const filtered = events.filter(
            (e) =>
              e.name.toLowerCase().includes(q.toLowerCase()) &&
              (!cat || e.category === cat),
          );
          return filtered.length ? (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {filtered.map((e) => (
                <EventCard key={e.id} event={e} />
              ))}
            </div>
          ) : (
            <EstadoVacio mensaje="No encontramos eventos con esos filtros" />
          );
        }}
      </Remote>
    </>
  );
}
