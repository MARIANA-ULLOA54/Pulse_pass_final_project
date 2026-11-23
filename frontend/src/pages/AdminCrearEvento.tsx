import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router";
import { api } from "../lib/api";
import { category, createEventSchema } from "../lib/schemas";
import { RUTAS } from "../lib/rutas";
import { Boton, Heading, Panel } from "../componentes/UI";
import { Remote, Alert, field } from "./shared";
export default function AdminCrearEvento() {
  const navigate = useNavigate();
  const [error, setError] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<z.input<typeof createEventSchema>>({
    resolver: zodResolver(createEventSchema),
  });
  return (
    <div className="max-w-3xl mx-auto">
      <Heading
        title="Crear evento"
        subtitle="Completa los datos del nuevo evento"
      />
      <Panel>
        <form
          className="space-y-4"
          onSubmit={handleSubmit(async (data) => {
            try {
              setError(null);
              const parsed = createEventSchema.parse(data);
              const created = await api.createEvent({
                ...parsed,
                eventDate:
                  parsed.eventDate.length === 16
                    ? `${parsed.eventDate}:00`
                    : parsed.eventDate,
              });
              const codes = JSON.parse(
                sessionStorage.getItem("pulsepass_admin_codes") ?? "[]",
              ) as string[];
              sessionStorage.setItem(
                "pulsepass_admin_codes",
                JSON.stringify([...new Set([...codes, created.eventCode])]),
              );
              navigate(RUTAS.gestionar(created.eventCode));
            } catch (e) {
              setError(e);
            }
          })}
        >
          <label className="block">
            Código
            <input className={field} {...register("eventCode")} />
            <span className="text-red-600">{errors.eventCode?.message}</span>
          </label>
          <label className="block">
            Nombre
            <input className={field} {...register("name")} />
            <span className="text-red-600">{errors.name?.message}</span>
          </label>
          <label className="block">
            Descripción
            <textarea className={field} {...register("description")} />
          </label>
          <label className="block">
            Categoría
            <select className={field} {...register("category")}>
              <option value="">Selecciona categoría</option>
              {category.options.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
            <span className="text-red-600">{errors.category?.message}</span>
          </label>
          <label className="block">
            Fecha
            <input
              type="datetime-local"
              className={field}
              {...register("eventDate")}
            />
            <span className="text-red-600">{errors.eventDate?.message}</span>
          </label>
          <label className="block">
            Edad mínima
            <input
              type="number"
              className={field}
              {...register("minimumAge", { valueAsNumber: true })}
            />
            <span className="text-red-600">{errors.minimumAge?.message}</span>
          </label>
          <Remote keyName={["venues"]} fetcher={api.venues}>
            {(venues) => (
              <label className="block">
                Sede
                <select className={field} {...register("venueCode")}>
                  <option value="">Selecciona sede</option>
                  {venues.map((v) => (
                    <option key={v.code} value={v.code}>
                      {v.name} — {v.city}
                    </option>
                  ))}
                </select>
                <span className="text-red-600">
                  {errors.venueCode?.message}
                </span>
              </label>
            )}
          </Remote>
          <Alert error={error} />
          <Boton type="submit" cargando={isSubmitting}>
            Crear evento
          </Boton>
        </form>
      </Panel>
    </div>
  );
}
