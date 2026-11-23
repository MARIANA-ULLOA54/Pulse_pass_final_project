import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useLocation, useNavigate, Link } from "react-router";
import { z } from "zod";
import { loginSchema } from "../lib/schemas";
import { useAuth } from "../context/AuthContext";
import { RUTAS } from "../lib/rutas";
import { Boton, Heading, Panel } from "../componentes/UI";
import { field, Alert } from "./shared";
export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
  });
  return (
    <div className="max-w-lg mx-auto">
      <Heading title="Iniciar sesión" subtitle="Accede a tu cuenta PulsePass" />
      <Panel>
        <form
          className="space-y-4"
          onSubmit={handleSubmit(async (data) => {
            try {
              setError(null);
              await login(data.identifier, data.password);
              const state = location.state as { desde?: string } | null;
              navigate(state?.desde ?? RUTAS.cuenta, { replace: true });
            } catch (e) {
              setError(e);
            }
          })}
        >
          <label className="block">
            Usuario o correo
            <input className={field} {...register("identifier")} />
            <span className="text-red-600 text-sm">
              {errors.identifier?.message}
            </span>
          </label>
          <label className="block">
            Contraseña
            <input
              type="password"
              className={field}
              {...register("password")}
            />
            <span className="text-red-600 text-sm">
              {errors.password?.message}
            </span>
          </label>
          <Alert error={error} />
          <Boton type="submit" cargando={isSubmitting} className="w-full">
            Ingresar
          </Boton>
        </form>
        <p className="mt-5">
          ¿No tienes cuenta?{" "}
          <Link className="text-marca-600" to={RUTAS.registro}>
            Regístrate
          </Link>
        </p>
      </Panel>
    </div>
  );
}
