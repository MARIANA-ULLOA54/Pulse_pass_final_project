import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useLocation, useNavigate } from "react-router";
import { z } from "zod";
import { registerSchema } from "../lib/schemas";
import { api } from "../lib/api";
import { RUTAS } from "../lib/rutas";
import { Boton, Heading, Panel } from "../componentes/UI";
import { authFormError, loginState } from "../lib/authForm";
import { field, Alert } from "./shared";

const fields = [
  { name: "username", label: "Nombre de usuario", type: "text", autocomplete: "username" },
  { name: "email", label: "Correo electrónico", type: "email", autocomplete: "email" },
  { name: "firstName", label: "Nombres", type: "text", autocomplete: "given-name" },
  { name: "lastName", label: "Apellidos", type: "text", autocomplete: "family-name" },
  { name: "birthDate", label: "Fecha de nacimiento", type: "date", autocomplete: "bday" },
  { name: "password", label: "Contraseña (mínimo 12 caracteres)", type: "password", autocomplete: "new-password" },
] as const;

export default function Registro() {
  const navigate = useNavigate();
  const location = useLocation();
  const desde = loginState.safeParse(location.state).data?.desde;
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    watch,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<z.infer<typeof registerSchema>>({
    resolver: zodResolver(registerSchema),
  });
  const password = watch("password", "");
  const strength =
    password.length < 12 ? "baja" : password.length < 16 ? "media" : "alta";
  return (
    <div className="max-w-xl mx-auto">
      <Heading title="Crear cuenta" subtitle="Únete a PulsePass" />
      <Panel>
        <form
          noValidate
          className="space-y-4"
          onSubmit={handleSubmit(async (data) => {
            try {
              setError(null);
              await api.register(data);
              void navigate(RUTAS.login, {
                replace: true,
                state: { registrado: true, desde },
              });
            } catch (e) {
              setError(authFormError(e, registerSchema.shape, setFieldError, "registro"));
            }
          })}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            {fields.map((item) => (
              <div
                key={item.name}
                className={item.name === "firstName" || item.name === "lastName" ? "" : "sm:col-span-2"}
              >
                <label className="block" htmlFor={`registro-${item.name}`}>
                  {item.label}
                </label>
                <input
                  id={`registro-${item.name}`}
                  className={field}
                  type={item.type}
                  autoComplete={item.autocomplete}
                  aria-invalid={!!errors[item.name]}
                  aria-describedby={errors[item.name] ? `registro-${item.name}-error` : undefined}
                  {...register(item.name)}
                />
                <span
                  id={`registro-${item.name}-error`}
                  aria-live="polite"
                  className="text-red-600 dark:text-red-400 text-sm"
                >
                  {errors[item.name]?.message}
                </span>
              </div>
            ))}
          </div>
          {password && (
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Fortaleza: {strength}
            </p>
          )}
          <Alert error={error} />
          <Boton className="w-full" type="submit" cargando={isSubmitting}>
            Crear cuenta
          </Boton>
        </form>
        <p className="mt-5">
          ¿Ya tienes cuenta?{" "}
          <Link
            className="text-marca-600 dark:text-indigo-300"
            to={RUTAS.login}
            state={desde ? { desde } : undefined}
          >
            Inicia sesión
          </Link>
        </p>
      </Panel>
    </div>
  );
}
