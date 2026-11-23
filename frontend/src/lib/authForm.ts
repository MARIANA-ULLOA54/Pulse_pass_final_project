import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { z } from "zod";
import { ApiError } from "./http";

export const loginState = z.object({
  desde: z.string().regex(/^\/(?![\/\\])[^\\\s]*$/).optional().catch(undefined),
  registrado: z.boolean().optional(),
  motivo: z.literal("expirada").optional(),
});

export function authFormError<T extends FieldValues>(
  error: unknown,
  fields: Record<keyof T, unknown>,
  setFieldError: UseFormSetError<T>,
  form: "login" | "registro",
) {
  if (error instanceof ApiError) {
    if (form === "login" && error.status === 401) return "Credenciales inválidas";
    if (form === "registro" && error.status === 409) return "El usuario o correo ya está registrado";
    if (error.status === 400) {
      const details = Object.entries(error.details);
      let general = details.length === 0;
      for (const [name, message] of details) {
        if (Object.hasOwn(fields, name)) {
          setFieldError(name as Path<T>, { type: "server", message });
        } else {
          general = true;
        }
      }
      return general ? "Revisa los datos enviados." : null;
    }
  }
  return "Inténtalo más tarde";
}
