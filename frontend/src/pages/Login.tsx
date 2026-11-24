import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useLocation, Link } from "react-router";
import { z } from "zod";
import { loginSchema } from "../lib/schemas";
import { useAuth } from "../context/AuthContext";
import { RUTAS } from "../lib/rutas";
import { Boton, Heading, Panel } from "../componentes/UI";
import { authFormError, loginState } from "../lib/authForm";
import { field, Alert } from "./shared";
export default function Login() {
  const { login } = useAuth();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const state = loginState.safeParse(location.state).data;
  const {
    register,
    handleSubmit,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
  });
  return (
    <div className="max-w-lg mx-auto">
      <Heading title="Iniciar sesión" subtitle="Accede a tu cuenta PulsePass" />
      <Panel>
        {state?.registrado && (
          <p role="status" className="mb-4 text-emerald-700 dark:text-emerald-300">
            Cuenta creada, inicia sesión.
          </p>
        )}
        {state?.motivo === "expirada" && (
          <p role="status" className="mb-4">
            Tu sesión expiró. Vuelve a iniciar sesión.
          </p>
        )}
        <form
          noValidate
          className="space-y-4"
          onSubmit={handleSubmit(async (data) => {
            try {
              setError(null);
              await login(data.identifier, data.password, state?.desde);
            } catch (e) {
              setError(authFormError(e, loginSchema.shape, setFieldError, "login"));
            }
          })}
        >
          <div>
            <label className="block" htmlFor="login-identifier">
              Usuario o correo
            </label>
            <input
              id="login-identifier"
              className={field}
              autoComplete="username"
              aria-invalid={!!errors.identifier}
              aria-describedby={errors.identifier ? "login-identifier-error" : undefined}
              {...register("identifier")}
            />
            <span id="login-identifier-error" aria-live="polite" className="text-red-600 dark:text-red-400 text-sm">
              {errors.identifier?.message}
            </span>
          </div>
          <div>
            <label className="block" htmlFor="login-password">Contraseña</label>
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              aria-invalid={!!errors.password}
              aria-describedby={errors.password ? "login-password-error" : undefined}
              className={field}
              {...register("password")}
            />
            <span id="login-password-error" aria-live="polite" className="text-red-600 dark:text-red-400 text-sm">
              {errors.password?.message}
            </span>
          </div>
          <Alert error={error} />
          <Boton type="submit" cargando={isSubmitting} className="w-full">
            Ingresar
          </Boton>
        </form>
        <p className="mt-5">
          ¿No tienes cuenta?{" "}
          <Link
            className="text-marca-600 dark:text-indigo-300"
            to={RUTAS.registro}
            state={state?.desde ? { desde: state.desde } : undefined}
          >
            Regístrate
          </Link>
        </p>
      </Panel>
    </div>
  );
}
