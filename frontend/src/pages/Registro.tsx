import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate } from "react-router";
import { z } from "zod";
import { registerSchema } from "../lib/schemas";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { Boton, Heading, Panel } from "../componentes/UI";
import { field, Alert } from "./shared";
export default function Registro() {
  const navigate = useNavigate();
  const [error, setError] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<z.infer<typeof registerSchema>>({
    resolver: zodResolver(registerSchema),
  });
  return (
    <div className="max-w-xl mx-auto">
      <Heading title="Crear cuenta" subtitle="Únete a PulsePass" />
      <Panel>
        <form
          className="space-y-4"
          onSubmit={handleSubmit(async (data) => {
            try {
              setError(null);
              await api.register(data);
              navigate(RUTAS.login, { state: { registered: true } });
            } catch (e) {
              setError(e);
            }
          })}
        >
          {(
            [
              { name: "username", label: "Usuario" },
              { name: "email", label: "Correo electrónico", type: "email" },
              { name: "firstName", label: "Nombres" },
              { name: "lastName", label: "Apellidos" },
              { name: "birthDate", label: "Fecha de nacimiento", type: "date" },
              {
                name: "password",
                label: "Contraseña (mínimo 12 caracteres)",
                type: "password",
              },
            ] as const
          ).map((item) => (
            <label className="block" key={item.name}>
              {item.label}
              <input
                className={field}
                type={"type" in item ? item.type : "text"}
                {...register(item.name)}
              />
              <span className="text-red-600 text-sm">
                {errors[item.name]?.message}
              </span>
            </label>
          ))}
          <Alert error={error} />
          <Boton className="w-full" type="submit" cargando={isSubmitting}>
            Registrarme
          </Boton>
        </form>
      </Panel>
    </div>
  );
}
