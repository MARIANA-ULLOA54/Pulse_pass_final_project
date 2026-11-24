import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router";
import { api } from "../lib/api";
import { ApiError } from "../lib/http";
import { category, createEventSchema } from "../lib/schemas";
import { RUTAS } from "../lib/rutas";
import { recordarEventoCreado } from "../lib/adminEventos";
import { Boton, Heading, Panel } from "../componentes/UI";
import { Remote, Alert, field } from "./shared";
export default function AdminCrearEvento() {
  const navigate = useNavigate();
  const [error, setError] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    setError: setFieldError,
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
              recordarEventoCreado(created.eventCode);
              navigate(RUTAS.gestionar(created.eventCode), {
                state: { aviso: "Evento creado como borrador. Ya puedes asociar artistas y publicarlo." },
              });
            } catch (e) {
              if (e instanceof ApiError && e.status === 409) {
                setError(new Error("No se pudo crear el evento. Revisa que el código no exista, la fecha sea futura y la sede esté activa."));
              } else if (e instanceof ApiError && e.status === 400) {
                const fields: Record<string, "eventCode" | "name" | "description" | "category" | "eventDate" | "minimumAge" | "venueCode"> = {
                  eventCode: "eventCode",
                  name: "name",
                  description: "description",
                  category: "category",
                  eventDate: "eventDate",
                  minimumAge: "minimumAge",
                  venueCode: "venueCode",
                };
                let mapped = false;
                for (const [key, message] of Object.entries(e.details)) {
                  const fieldName = fields[key];
                  if (!fieldName) continue;
                  setFieldError(fieldName, { type: "server", message });
                  mapped = true;
                }
                setError(mapped ? null : e);
              } else {
                setError(e);
              }
            }
          })}
        >
          <label className="block">
            Código
            <input className={field} maxLength={30} aria-invalid={!!errors.eventCode} aria-describedby={errors.eventCode ? "evento-codigo-error" : undefined} {...register("eventCode")} />
            <span id="evento-codigo-error" aria-live="polite" className="text-red-600">{errors.eventCode?.message}</span>
          </label>
          <label className="block">
            Nombre
            <input className={field} maxLength={150} aria-invalid={!!errors.name} aria-describedby={errors.name ? "evento-nombre-error" : undefined} {...register("name")} />
            <span id="evento-nombre-error" aria-live="polite" className="text-red-600">{errors.name?.message}</span>
          </label>
          <label className="block">
            Descripción
            <textarea className={field} maxLength={2000} rows={4} aria-invalid={!!errors.description} aria-describedby={errors.description ? "evento-descripcion-error" : "evento-descripcion-ayuda"} {...register("description")} />
            <span id="evento-descripcion-ayuda" className="text-sm text-slate-500">Máximo 2000 caracteres.</span>
            <span id="evento-descripcion-error" aria-live="polite" className="text-red-600">{errors.description?.message}</span>
          </label>
          <label className="block">
            Categoría
            <select className={field} aria-invalid={!!errors.category} aria-describedby={errors.category ? "evento-categoria-error" : undefined} {...register("category")}>
              <option value="">Selecciona categoría</option>
              {category.options.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
            <span id="evento-categoria-error" aria-live="polite" className="text-red-600">{errors.category?.message}</span>
          </label>
          <label className="block">
            Fecha
            <input
              type="datetime-local"
              className={field}
              aria-invalid={!!errors.eventDate}
              aria-describedby={errors.eventDate ? "evento-fecha-error" : undefined}
              {...register("eventDate")}
            />
            <span id="evento-fecha-error" aria-live="polite" className="text-red-600">{errors.eventDate?.message}</span>
          </label>
          <label className="block">
            Edad mínima
            <input
              type="number"
              className={field}
              min={0}
              aria-invalid={!!errors.minimumAge}
              aria-describedby={errors.minimumAge ? "evento-edad-error" : undefined}
              {...register("minimumAge", { valueAsNumber: true })}
            />
            <span id="evento-edad-error" aria-live="polite" className="text-red-600">{errors.minimumAge?.message}</span>
          </label>
          <Remote keyName={["venues"]} fetcher={api.venues}>
            {(venues) => (
              <label className="block">
                Sede
                <select className={field} aria-invalid={!!errors.venueCode} aria-describedby={errors.venueCode ? "evento-sede-error" : undefined} {...register("venueCode")}>
                  <option value="">Selecciona sede</option>
                  {venues.filter((v) => v.active).map((v) => (
                    <option key={v.code} value={v.code}>
                      {v.name} — {v.city}
                    </option>
                  ))}
                </select>
                <span className="text-red-600">
                  <span id="evento-sede-error" aria-live="polite">{errors.venueCode?.message}</span>
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
